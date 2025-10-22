package com.gemini.phoneai

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gemini.phoneai.databinding.ActivityMainBinding
import com.gemini.phoneai.gemini.GeminiLiveClient
import com.gemini.phoneai.services.GeminiConnectionService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "gemini_config"
        private const val KEY_API_KEY = "api_key"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var telecomManager: TelecomManager
    private lateinit var sharedPreferences: SharedPreferences
    private var phoneAccountHandle: PhoneAccountHandle? = null

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            updateStatus("All permissions granted ✓")
            binding.permissionCard.setCardBackgroundColor(getColor(R.color.success_light))
        } else {
            updateStatus("Some permissions denied")
            binding.permissionCard.setCardBackgroundColor(getColor(R.color.error_light))
            showPermissionsDeniedDialog()
        }
        updateUI()
    }

    private val setDefaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkDefaultDialerStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupUI()
        checkPermissions()
        registerPhoneAccount()
        loadApiKey()
        checkDefaultDialerStatus()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        checkDefaultDialerStatus()
        updateUI()
    }

    private fun setupUI() {
        // Setup click listeners
        binding.requestPermissionsButton.setOnClickListener {
            requestPermissions()
        }

        binding.setDefaultDialerButton.setOnClickListener {
            requestDefaultDialerRole()
        }

        binding.callButton.setOnClickListener {
            makeCall()
        }

        binding.apiKeyButton.setOnClickListener {
            showApiKeyDialog()
        }

        binding.testConnectionButton.setOnClickListener {
            testGeminiConnection()
        }

        binding.settingsButton.setOnClickListener {
            openAppSettings()
        }

        binding.helpButton.setOnClickListener {
            showHelpDialog()
        }

        // Set initial states
        binding.phoneNumberInput.setText("")
        updateUI()
    }

    private fun checkPermissions() {
        val allPermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            binding.permissionStatus.text = "✓ All permissions granted"
            binding.permissionCard.setCardBackgroundColor(getColor(R.color.success_light))
            binding.requestPermissionsButton.isEnabled = false
        } else {
            binding.permissionStatus.text = "⚠ Permissions required"
            binding.permissionCard.setCardBackgroundColor(getColor(R.color.warning_light))
            binding.requestPermissionsButton.isEnabled = true
        }
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(requiredPermissions)
    }

    private fun registerPhoneAccount() {
        phoneAccountHandle = PhoneAccountHandle(
            ComponentName(this, GeminiConnectionService::class.java),
            "GeminiPhoneAI"
        )

        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "Gemini AI Phone")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.MANAGE_OWN_CALLS) 
            == PackageManager.PERMISSION_GRANTED) {
            telecomManager.registerPhoneAccount(phoneAccount)
            Log.d(TAG, "Phone account registered")
        }
    }

    private fun checkDefaultDialerStatus() {
        val isDefaultDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            packageName == telecomManager.defaultDialerPackage
        }

        if (isDefaultDialer) {
            binding.dialerStatus.text = "✓ Set as default dialer"
            binding.dialerCard.setCardBackgroundColor(getColor(R.color.success_light))
            binding.setDefaultDialerButton.isEnabled = false
        } else {
            binding.dialerStatus.text = "⚠ Not default dialer"
            binding.dialerCard.setCardBackgroundColor(getColor(R.color.warning_light))
            binding.setDefaultDialerButton.isEnabled = true
        }
    }

    private fun requestDefaultDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                setDefaultDialerLauncher.launch(intent)
            } else {
                updateStatus("Already default dialer")
            }
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            startActivity(intent)
        }
    }

    private fun makeCall() {
        val phoneNumber = binding.phoneNumberInput.text.toString().trim()

        if (phoneNumber.isEmpty()) {
            Snackbar.make(binding.root, "Please enter a phone number", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (!hasCallPermission()) {
            Snackbar.make(binding.root, "Call permission not granted", Snackbar.LENGTH_SHORT).show()
            requestPermissions()
            return
        }

        val apiKey = sharedPreferences.getString(KEY_API_KEY, null)
        if (apiKey.isNullOrEmpty()) {
            showApiKeyDialog()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
            updateStatus("Calling: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Error making call", e)
            Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == 
                PackageManager.PERMISSION_GRANTED
    }

    private fun showApiKeyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_api_key, null)
        val apiKeyInput = dialogView.findViewById<TextInputEditText>(R.id.apiKeyInput)
        
        val currentApiKey = sharedPreferences.getString(KEY_API_KEY, "")
        apiKeyInput.setText(currentApiKey)

        MaterialAlertDialogBuilder(this)
            .setTitle("Gemini API Key")
            .setMessage("Enter your Gemini API key. Get one from https://aistudio.google.com/apikey")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val apiKey = apiKeyInput.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    saveApiKey(apiKey)
                    updateStatus("API key saved")
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Get API Key") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                startActivity(intent)
            }
            .show()
    }

    private fun saveApiKey(apiKey: String) {
        sharedPreferences.edit()
            .putString(KEY_API_KEY, apiKey)
            .apply()
        
        binding.apiKeyStatus.text = "✓ API key configured"
        binding.apiKeyCard.setCardBackgroundColor(getColor(R.color.success_light))
    }

    private fun loadApiKey() {
        val apiKey = sharedPreferences.getString(KEY_API_KEY, null)
        if (!apiKey.isNullOrEmpty()) {
            binding.apiKeyStatus.text = "✓ API key configured"
            binding.apiKeyCard.setCardBackgroundColor(getColor(R.color.success_light))
        } else {
            binding.apiKeyStatus.text = "⚠ API key required"
            binding.apiKeyCard.setCardBackgroundColor(getColor(R.color.warning_light))
        }
    }

    private fun testGeminiConnection() {
        val apiKey = sharedPreferences.getString(KEY_API_KEY, null)
        if (apiKey.isNullOrEmpty()) {
            showApiKeyDialog()
            return
        }

        updateStatus("Testing Gemini connection...")
        
        val client = GeminiLiveClient()
        client.setApiKey(apiKey)
        client.setCallback(object : GeminiLiveClient.Callback {
            override fun onConnected() {
                runOnUiThread {
                    updateStatus("✓ Connected to Gemini successfully!")
                    Snackbar.make(binding.root, "Connection successful!", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.success))
                        .show()
                }
                client.disconnect()
            }

            override fun onError(error: String) {
                runOnUiThread {
                    updateStatus("✗ Connection failed: $error")
                    Snackbar.make(binding.root, "Connection failed: $error", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.error))
                        .show()
                }
            }

            override fun onDisconnected() {
                Log.d(TAG, "Test connection closed")
            }

            override fun onAudioResponse(audioData: ByteArray) {}
            override fun onTextResponse(text: String) {}
        })
        
        client.connect()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("How to Use")
            .setMessage("""
                1. Grant all required permissions
                2. Set the app as default dialer
                3. Configure your Gemini API key
                4. Test the connection to Gemini
                5. Enter a phone number and tap 'Call with AI'
                
                The AI assistant will:
                • Auto-answer incoming calls
                • Handle conversations naturally
                • Process voice in real-time
                
                For support, check the README or project documentation.
            """.trimIndent())
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun showPermissionsDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs all permissions to function properly. Please grant them in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStatus(message: String) {
        Log.d(TAG, message)
        binding.statusText.text = message
    }

    private fun updateUI() {
        val allPermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        val hasApiKey = !sharedPreferences.getString(KEY_API_KEY, null).isNullOrEmpty()
        
        binding.callButton.isEnabled = allPermissionsGranted && hasApiKey
        binding.testConnectionButton.isEnabled = hasApiKey
    }
}

package com.gemini.phoneai.services

import android.net.Uri
import android.telecom.*
import android.util.Log

class GeminiConnectionService : ConnectionService() {
    companion object {
        private const val TAG = "GeminiConnectionService"
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        Log.d(TAG, "Creating outgoing connection")
        
        val connection = GeminiConnection()
        connection.setConnectionCapabilities(
            Connection.CAPABILITY_SUPPORT_HOLD or
            Connection.CAPABILITY_HOLD or
            Connection.CAPABILITY_MUTE
        )
        
        // Set initial state
        connection.setDialing()
        
        // Get phone number from request
        val address = request.address
        connection.setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
        
        // Start call process
        address?.schemeSpecificPart?.let { phoneNumber ->
            connection.startOutgoingCall(phoneNumber)
        }
        
        return connection
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        Log.d(TAG, "Creating incoming connection")
        
        val connection = GeminiConnection()
        connection.setConnectionCapabilities(
            Connection.CAPABILITY_SUPPORT_HOLD or
            Connection.CAPABILITY_HOLD or
            Connection.CAPABILITY_MUTE
        )
        
        // Set as incoming call
        connection.setRinging()
        
        // Get caller information
        val address = request.address
        connection.setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
        
        // Auto-answer for AI handling
        connection.setActive()
        connection.startIncomingCallHandling()
        
        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ) {
        Log.e(TAG, "Failed to create incoming connection")
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ) {
        Log.e(TAG, "Failed to create outgoing connection")
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request)
    }
}

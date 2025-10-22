# Quick Setup Guide - Gemini Phone AI

## 📱 Project Overview
This Android app integrates Gemini AI to handle phone calls with real-time voice conversation capabilities.

## 🚀 Quick Start

### Step 1: Open in Android Studio
1. Open Android Studio
2. Click "Open" and select the `GeminiPhoneAI` folder
3. Wait for Gradle sync to complete

### Step 2: Configure Gemini API Key
1. Get your API key from [Google AI Studio](https://aistudio.google.com/apikey)
2. Open `gradle.properties` in the project root
3. Replace `YOUR_GEMINI_API_KEY_HERE` with your actual API key:
   ```
   GEMINI_API_KEY=your_actual_api_key_here
   ```

### Step 3: Build and Run
1. Connect an Android device (API 23+) or start an emulator
2. Click the "Run" button in Android Studio (green play icon)
3. The app will build and install automatically

## 📋 First Time Setup on Device

### 1. Grant Permissions
When the app launches:
- Tap "Grant" for each permission card
- Allow all requested permissions in the system dialogs

### 2. Set as Default Dialer
- Tap "Set" on the Default Dialer card
- Confirm in the system dialog to make this your default phone app

### 3. Configure API Key (if not done in gradle.properties)
- Tap "Configure" on the Gemini API card
- Enter your API key
- Tap "Save"

### 4. Test Connection
- Tap "Test Gemini Connection"
- Wait for "Connected successfully!" message

## 📞 Using the App

### Making Calls
1. Enter a phone number in the text field
2. Tap "Call with AI Assistant"
3. The AI will handle the conversation

### Receiving Calls
- Incoming calls are automatically answered
- The AI processes and responds to the caller
- Monitor the call status in the app

## ⚠️ Important Notes

1. **API Key Security**: Never commit your API key to version control
2. **Testing**: Use real devices for best results (emulator telephony is limited)
3. **Permissions**: All permissions are required for proper functionality
4. **Network**: Ensure stable internet connection for AI features

## 🔧 Troubleshooting

### App Won't Build
- Ensure Android Studio is up to date
- Check that Gradle sync completed successfully
- Verify API level 23+ in SDK Manager

### API Connection Failed
- Verify your API key is correct
- Check internet connectivity
- Ensure the Gemini API is enabled in your Google Cloud project

### Calls Not Working
- Confirm all permissions are granted
- Set the app as default dialer
- Check device telephony capabilities

## 📂 Project Structure
```
GeminiPhoneAI/
├── app/src/main/java/com/gemini/phoneai/
│   ├── MainActivity.kt              # Main UI
│   ├── services/                    # Telephony services
│   ├── gemini/GeminiLiveClient.kt  # AI integration
│   └── audio/CallAudioProcessor.kt  # Audio handling
├── app/src/main/res/                # Resources (layouts, colors, etc.)
├── gradle.properties                 # API key configuration
└── README.md                        # Full documentation
```

## 🎯 Next Steps

1. **Customize AI Behavior**: Edit the system instruction in `GeminiLiveClient.kt`
2. **Modify UI**: Update layouts in `res/layout/`
3. **Add Features**: Extend functionality in the service classes
4. **Production Build**: Configure ProGuard and create signed APK

## 📚 Resources

- [Gemini API Documentation](https://ai.google.dev/gemini-api/docs)
- [Android Telecom Guide](https://developer.android.com/develop/connectivity/telecom)
- [Project README](README.md) - Full documentation

## ✅ Checklist

- [ ] Android Studio installed
- [ ] API key obtained
- [ ] Project opened and synced
- [ ] API key configured
- [ ] App built successfully
- [ ] Permissions granted
- [ ] Default dialer set
- [ ] Connection tested
- [ ] First call made

---

**Ready to go!** Your Gemini Phone AI app is now set up and ready to use. 🎉

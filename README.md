# Gemini Phone AI - Android App

An Android application that integrates Google's Gemini AI to handle phone calls with real-time voice conversation capabilities. The app automatically processes incoming and outgoing calls using AI-powered natural language processing.

## Features

- **AI-Powered Call Handling**: Automatically answer and handle phone conversations using Gemini AI
- **Real-time Voice Processing**: Convert speech to text and generate natural voice responses
- **Incoming Call Management**: Auto-answer incoming calls with AI assistance
- **Outgoing Call Support**: Make calls with AI conversation capabilities
- **Call Screening**: Smart call filtering to block spam
- **Natural Conversation**: Gemini AI provides contextual, human-like responses

## Prerequisites

- Android Studio (latest stable version)
- Android SDK with minimum API level 23 (Android 6.0)
- Android device or emulator for testing
- Gemini API key from Google AI Studio

## Setup Instructions

### 1. Get Gemini API Key

1. Visit [Google AI Studio](https://aistudio.google.com/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the generated API key (save it securely)

### 2. Clone and Open Project

1. Clone this repository or download the project files
2. Open Android Studio
3. Select "Open" and navigate to the GeminiPhoneAI folder
4. Wait for Gradle sync to complete

### 3. Configure API Key

#### Option 1: Build-time Configuration (Recommended for Development)

Edit `gradle.properties` in the project root:
```properties
GEMINI_API_KEY=your_actual_api_key_here
```

#### Option 2: Runtime Configuration

The app will prompt you to enter the API key on first launch. The key is stored securely in SharedPreferences.

### 4. Build and Run

1. Connect your Android device or start an emulator
2. Click "Run" in Android Studio or use:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install the APK on your device

## Required Permissions

The app requires the following permissions:

- **Phone Permissions**: CALL_PHONE, ANSWER_PHONE_CALLS, READ_PHONE_STATE
- **Audio Permissions**: RECORD_AUDIO, MODIFY_AUDIO_SETTINGS
- **Network Permissions**: INTERNET, ACCESS_NETWORK_STATE
- **System Permissions**: WAKE_LOCK, FOREGROUND_SERVICE

## Usage Guide

### Initial Setup

1. **Grant Permissions**: When prompted, grant all required permissions
2. **Set as Default Dialer**: Make the app your default dialer for full functionality
3. **Configure API Key**: Enter your Gemini API key in the settings
4. **Test Connection**: Use the "Test Gemini Connection" button to verify setup

### Making Calls

1. Enter a phone number in the main screen
2. Tap "Call with AI Assistant"
3. The AI will handle the conversation automatically

### Receiving Calls

1. Incoming calls are automatically answered by the AI
2. The AI processes the conversation in real-time
3. You can monitor the call status in the app

## Project Structure

```
GeminiPhoneAI/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/gemini/phoneai/
│   │   │   │   ├── MainActivity.kt           # Main app interface
│   │   │   │   ├── services/
│   │   │   │   │   ├── GeminiConnectionService.kt
│   │   │   │   │   ├── GeminiConnection.kt
│   │   │   │   │   ├── GeminiInCallService.kt
│   │   │   │   │   └── GeminiCallScreeningService.kt
│   │   │   │   ├── gemini/
│   │   │   │   │   └── GeminiLiveClient.kt  # Gemini API integration
│   │   │   │   └── audio/
│   │   │   │       └── CallAudioProcessor.kt # Audio processing
│   │   │   ├── res/
│   │   │   │   ├── layout/                  # UI layouts
│   │   │   │   ├── values/                  # Colors, strings, themes
│   │   │   │   └── drawable/                # Icons and graphics
│   │   │   └── AndroidManifest.xml         # App configuration
│   │   └── build.gradle                    # Module dependencies
│   └── proguard-rules.pro                 # ProGuard configuration
├── build.gradle                            # Project configuration
├── settings.gradle                         # Project settings
└── gradle.properties                       # Build properties
```

## Key Components

### GeminiConnectionService
Handles the telecommunication connection lifecycle for both incoming and outgoing calls.

### GeminiLiveClient
Manages WebSocket connection to Gemini API for real-time audio/text processing.

### CallAudioProcessor
Captures call audio, sends it to Gemini, and plays back AI-generated responses.

### GeminiInCallService
Manages the in-call UI and call state changes.

## Troubleshooting

### Common Issues

1. **API Key Not Working**
   - Verify the key is correct and active
   - Check your Google Cloud project has the Gemini API enabled
   - Ensure you have sufficient API quota

2. **Permissions Denied**
   - Go to Settings > Apps > Gemini Phone AI > Permissions
   - Enable all required permissions manually

3. **Not Set as Default Dialer**
   - Go to Settings > Apps > Default apps > Phone app
   - Select Gemini Phone AI

4. **Audio Not Working**
   - Check device volume settings
   - Ensure microphone is not muted
   - Verify audio permissions are granted

5. **Connection Failures**
   - Check internet connectivity
   - Verify firewall/proxy settings
   - Ensure WebSocket connections are not blocked

## Development Notes

### Testing

- Test on real devices for best results (emulator telephony can be limited)
- Use two devices to test incoming/outgoing calls
- Monitor Logcat for debugging information

### Security Considerations

- Never commit API keys to version control
- Use ProGuard for release builds
- Implement proper error handling for production
- Consider implementing call recording consent

### Future Enhancements

- Add conversation history storage
- Implement custom AI personalities
- Add multi-language support
- Create call transcription features
- Add voice customization options
- Implement advanced call screening rules

## API Reference

### Gemini Live API

The app uses the Gemini Live API for real-time conversation:
- WebSocket endpoint: `wss://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:bidiGenerateContent`
- Audio format: PCM 16-bit, 16kHz, mono
- Response modalities: Audio and text

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is provided as-is for educational purposes. Ensure you comply with:
- Google's Gemini API Terms of Service
- Android platform policies
- Local telecommunications regulations
- Privacy and data protection laws

## Support

For issues, questions, or suggestions:
- Open an issue in the repository
- Check the documentation
- Review Google's Gemini API documentation

## Acknowledgments

- Google Gemini API for AI capabilities
- Android Telecom framework
- OkHttp for WebSocket communication
- Material Design components

---

**Note**: This app is a demonstration of AI integration with Android telephony. Always ensure compliance with local laws regarding call recording and automated calling systems.

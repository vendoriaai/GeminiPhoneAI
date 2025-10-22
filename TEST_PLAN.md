# Gemini Phone AI - Complete Test Plan

## 📋 Overview
This document provides a comprehensive test plan to verify that all components of the Gemini Phone AI application are working correctly.

---

## 🔧 Pre-Test Setup

### 1. Environment Requirements
- [ ] Android device (physical preferred) or emulator running Android 6.0+
- [ ] Active internet connection (WiFi or mobile data)
- [ ] Valid Gemini API key configured
- [ ] Two phones for testing (one for the app, one to call from)
- [ ] Android Studio with Logcat open for monitoring

### 2. Installation Verification
```bash
# Check if app is installed
adb shell pm list packages | grep com.gemini.phoneai
```

---

## ✅ Test Suite

### Test 1: Initial App Launch
**Objective**: Verify app launches without crashes

**Steps**:
1. Launch the Gemini Phone AI app
2. Check for any crash dialogs
3. Verify main screen loads

**Expected Results**:
- ✅ App launches successfully
- ✅ Main activity displays
- ✅ No force close errors

**Verification Command**:
```bash
adb logcat -d | grep "MainActivity"
```

---

### Test 2: Permission Management
**Objective**: Verify all permissions are properly requested and handled

**Steps**:
1. Tap "Grant" on permissions card
2. Grant each permission when prompted:
   - Phone calls
   - Microphone
   - Contacts (if applicable)
   - Notifications

**Expected Results**:
- ✅ Each permission dialog appears
- ✅ Permission card shows "✓ All permissions granted"
- ✅ Card background turns green

**Verification Command**:
```bash
adb shell dumpsys package com.gemini.phoneai | grep permission
```

---

### Test 3: Default Dialer Setup
**Objective**: Verify app can be set as default dialer

**Steps**:
1. Tap "Set" on Default Dialer card
2. Confirm in system dialog
3. Return to app

**Expected Results**:
- ✅ System dialog appears
- ✅ App becomes default dialer
- ✅ Card shows "✓ Set as default dialer"

**Verification Command**:
```bash
adb shell settings get secure dialer_default_application
# Should return: com.gemini.phoneai
```

---

### Test 4: API Key Configuration
**Objective**: Verify API key setup and validation

**Steps**:
1. Tap "Configure" on API key card
2. Enter your Gemini API key
3. Tap "Save"

**Expected Results**:
- ✅ Dialog appears with input field
- ✅ API key saves successfully
- ✅ Card shows "✓ API key configured"

**Verification**:
Check SharedPreferences (requires root or debug build):
```bash
adb shell run-as com.gemini.phoneai cat /data/data/com.gemini.phoneai/shared_prefs/gemini_config.xml
```

---

### Test 5: Gemini Connection Test
**Objective**: Verify WebSocket connection to Gemini API

**Steps**:
1. Tap "Test Gemini Connection" button
2. Wait for connection response

**Expected Results**:
- ✅ Status shows "Testing Gemini connection..."
- ✅ Success message: "✓ Connected to Gemini successfully!"
- ✅ Green snackbar appears

**Logcat Verification**:
```bash
adb logcat -d | grep "GeminiLiveClient"
# Look for:
# - "WebSocket connected"
# - "Setup complete"
```

**Error Cases to Check**:
- Invalid API key → Should show "Connection failed: 403"
- No internet → Should show "Connection failed: Network error"

---

### Test 6: Outgoing Call with AI
**Objective**: Verify AI handles outgoing calls

**Steps**:
1. Enter a test phone number (use voicemail or test line)
2. Tap "Call with AI Assistant"
3. Let the call connect
4. Speak to test AI response
5. End the call

**Expected Results**:
- ✅ Call initiates successfully
- ✅ AI processes speech
- ✅ AI generates responses
- ✅ Audio plays through earpiece/speaker

**Logcat Monitoring**:
```bash
# Monitor in real-time
adb logcat | grep -E "GeminiConnection|CallAudioProcessor|GeminiLiveClient"

# Key logs to look for:
# - "Starting outgoing call to: [number]"
# - "Starting Gemini integration"
# - "Connected to Gemini"
# - "Audio level: [X] dB"
# - "Received audio response: [X] bytes"
```

---

### Test 7: Incoming Call Handling
**Objective**: Verify AI auto-answers and handles incoming calls

**Steps**:
1. From another phone, call the test device
2. Observe auto-answer behavior
3. Speak from calling phone
4. Listen for AI responses
5. End the call

**Expected Results**:
- ✅ Call auto-answers within 2-3 seconds
- ✅ No ringing on device (silent answer)
- ✅ AI processes caller's speech
- ✅ AI responds naturally
- ✅ Two-way conversation works

**Service Verification**:
```bash
adb shell dumpsys activity services | grep -E "GeminiInCallService|GeminiConnectionService"
```

---

### Test 8: Audio Processing
**Objective**: Verify audio capture and playback

**Test Audio Capture**:
```bash
# During a call, monitor audio levels
adb logcat | grep "Audio level"
# Should see varying dB levels when speaking
```

**Test Audio Playback**:
```bash
# Check for audio response processing
adb logcat | grep "Received audio response"
# Should see byte counts for AI audio
```

**Expected Results**:
- ✅ Audio levels change when speaking (not silent)
- ✅ Audio responses received from Gemini
- ✅ No audio feedback or echo
- ✅ Clear audio quality

---

### Test 9: Call State Management
**Objective**: Verify call states are handled correctly

**Test States**:
1. **DIALING**: Initiate call → Check "Call is dialing" log
2. **ACTIVE**: Call connects → Check "Call is now active" log
3. **HOLDING**: Put call on hold → Check "Call is on hold" log
4. **DISCONNECTED**: End call → Check "Call disconnected" log

```bash
# Monitor call states
adb logcat | grep "Call state changed"
```

---

### Test 10: Error Recovery
**Objective**: Verify app handles errors gracefully

**Test Scenarios**:

1. **No Internet During Call**:
   - Start call with internet
   - Disable WiFi/data mid-call
   - Expected: Call continues, AI features pause

2. **Invalid API Key**:
   - Enter wrong API key
   - Test connection
   - Expected: Clear error message

3. **Permission Denied**:
   - Deny microphone permission
   - Try to make call
   - Expected: Prompts for permission

4. **Call Interrupted**:
   - Receive regular call during AI call
   - Expected: Handles call waiting properly

---

### Test 11: Memory and Performance
**Objective**: Verify app doesn't leak memory or drain battery

**Memory Test**:
```bash
# Before test
adb shell dumpsys meminfo com.gemini.phoneai

# Make 5 calls, then check again
adb shell dumpsys meminfo com.gemini.phoneai

# Memory should not increase significantly
```

**CPU Usage**:
```bash
# During call
adb shell top -n 1 | grep com.gemini.phoneai
# CPU usage should be < 20%
```

**Battery Drain**:
```bash
# Check battery stats
adb shell dumpsys batterystats | grep com.gemini.phoneai
```

---

### Test 12: WebSocket Stability
**Objective**: Verify WebSocket connection remains stable

**Long Connection Test**:
1. Open app and connect to Gemini
2. Leave app open for 10 minutes
3. Make a call

```bash
# Monitor WebSocket
adb logcat | grep -E "WebSocket|onClosing|onFailure"
# Should not see disconnections
```

---

## 🔍 Debug Commands

### Complete Diagnostics Script
Create `diagnose.bat`:
```batch
@echo off
echo === Gemini Phone AI Diagnostics ===
echo.
echo 1. App Installation:
adb shell pm list packages | findstr com.gemini.phoneai
echo.
echo 2. Default Dialer:
adb shell settings get secure dialer_default_application
echo.
echo 3. Permissions:
adb shell dumpsys package com.gemini.phoneai | findstr permission
echo.
echo 4. Running Services:
adb shell dumpsys activity services | findstr Gemini
echo.
echo 5. Recent Logs:
adb logcat -d -t 100 | findstr Gemini
pause
```

### Live Monitoring
```bash
# Full debug monitoring
adb logcat -c && adb logcat | grep -E "Gemini|Call|Audio|WebSocket" --color=always
```

---

## 📊 Test Report Template

```markdown
## Test Execution Report

**Date**: [Date]
**Tester**: [Name]
**Device**: [Model, Android Version]
**App Version**: 1.0.0

### Test Results Summary

| Test | Result | Notes |
|------|--------|-------|
| App Launch | ✅ PASS | No crashes |
| Permissions | ✅ PASS | All granted |
| Default Dialer | ✅ PASS | Set successfully |
| API Key Config | ✅ PASS | Key saved |
| Gemini Connection | ✅ PASS | Connected in 2s |
| Outgoing Call | ✅ PASS | AI responded |
| Incoming Call | ✅ PASS | Auto-answered |
| Audio Processing | ✅ PASS | Clear audio |
| Call States | ✅ PASS | All states work |
| Error Recovery | ✅ PASS | Graceful handling |
| Performance | ✅ PASS | CPU < 15% |
| WebSocket Stability | ✅ PASS | No disconnects |

### Issues Found
- None / [List any issues]

### Overall Status: PASS ✅
```

---

## 🚨 Common Issues & Solutions

### Issue: "API key not working"
**Check**:
```bash
curl -X POST "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=YOUR_API_KEY" \
-H "Content-Type: application/json" \
-d '{"contents":[{"parts":[{"text":"Hello"}]}]}'
```

### Issue: "No audio during calls"
**Check**:
- Microphone permission granted
- Audio routing: `adb shell dumpsys audio`
- AudioRecord state: Check logcat for "AudioRecord"

### Issue: "Calls not auto-answering"
**Check**:
- Is app default dialer?
- InCallService running?
- Check: `adb shell dumpsys telecom`

### Issue: "WebSocket disconnecting"
**Check**:
- Network stability
- API quota limits
- Firewall/proxy settings

---

## ✅ Certification Checklist

Before declaring the app production-ready:

- [ ] All 12 tests pass
- [ ] No crashes in 1 hour of testing
- [ ] Memory usage stable
- [ ] Battery drain acceptable
- [ ] Audio quality clear
- [ ] AI responses appropriate
- [ ] Error messages user-friendly
- [ ] WebSocket stays connected
- [ ] Call states handled properly
- [ ] Permissions flow smooth

---

## 📝 Notes

- Test on multiple devices if possible
- Test with different network conditions (WiFi, 4G, 5G)
- Test with different audio accessories (Bluetooth, wired headphones)
- Record test sessions for debugging
- Keep Logcat running during all tests

**Test Duration**: Complete test suite takes approximately 30-45 minutes

---

**Last Updated**: October 22, 2025

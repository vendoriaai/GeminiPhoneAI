@echo off
echo =============================================
echo     Gemini Phone AI - Quick Test Suite
echo =============================================
echo.

echo [1/7] Checking if device is connected...
adb devices
echo.

echo [2/7] Checking if app is installed...
adb shell pm list packages | findstr com.gemini.phoneai
if %errorlevel% neq 0 (
    echo ERROR: App not installed!
    goto :error
)
echo SUCCESS: App is installed
echo.

echo [3/7] Checking default dialer status...
adb shell settings get secure dialer_default_application | findstr com.gemini.phoneai
if %errorlevel% neq 0 (
    echo WARNING: App is not the default dialer
) else (
    echo SUCCESS: App is set as default dialer
)
echo.

echo [4/7] Checking granted permissions...
echo Phone permission:
adb shell dumpsys package com.gemini.phoneai | findstr CALL_PHONE
echo Audio permission:
adb shell dumpsys package com.gemini.phoneai | findstr RECORD_AUDIO
echo.

echo [5/7] Checking if services are running...
adb shell dumpsys activity services | findstr Gemini
echo.

echo [6/7] Launching the app...
adb shell am start -n com.gemini.phoneai/.MainActivity
timeout /t 3 >nul
echo.

echo [7/7] Capturing recent logs for Gemini activity...
echo Recent Gemini-related logs:
adb logcat -d -t 50 | findstr "Gemini"
echo.

echo =============================================
echo              TEST SUMMARY
echo =============================================
echo.
echo Manual verification needed:
echo 1. Check if app UI is displayed on device
echo 2. Tap "Test Gemini Connection" button
echo 3. Try making a test call
echo.
echo For detailed testing, see TEST_PLAN.md
echo.
pause
goto :end

:error
echo.
echo TEST FAILED! Please check the error above.
pause

:end

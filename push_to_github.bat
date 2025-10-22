@echo off
echo Committing files...
git commit -m "Initial commit: Gemini Phone AI - AI-powered Android phone assistant"

echo.
echo Setting up main branch...
git branch -M main

echo.
echo Pushing to GitHub...
git push -u origin main

echo.
echo Done! Repository pushed to https://github.com/vendoriaai/GeminiPhoneAI
pause

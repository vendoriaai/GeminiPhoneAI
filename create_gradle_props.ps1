$content = @"
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true

GEMINI_API_KEY=AIzaSyDoem5Pe7xkUWipkU1ARh1ED71yWO7FJZA
"@

Set-Content -Path "gradle.properties" -Value $content -Encoding UTF8
Write-Host "gradle.properties file created successfully!"

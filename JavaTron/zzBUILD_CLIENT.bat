@echo off
call gradlew.bat lwjgl3:run
echo.
echo Gradle exited with code %ERRORLEVEL%
pause

@echo off
call gradlew.bat server:run
echo.
echo Gradle exited with code %ERRORLEVEL%
pause

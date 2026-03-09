@echo off
setlocal enableextensions
pushd "%~dp0"

REM Ensure logs folder exists
if not exist "logs" mkdir "logs"

REM Timestamp for filename
for /f "usebackq delims=" %%t in (`powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"`) do set "ts=%%t"
set "LOGFILE=%~dp0logs\zzBUILD_SERVER_%ts%.log"

echo Writing log to: "%LOGFILE%"
echo ==== zzBUILD_SERVER ====>> "%LOGFILE%"
echo StartTime: %DATE% %TIME%>> "%LOGFILE%"
echo WorkingDir: %CD%>> "%LOGFILE%"
echo Command: call .\gradlew.bat :server:build  --console=plain --info --stacktrace>> "%LOGFILE%"
echo ---- output ---->> "%LOGFILE%"

call .\gradlew.bat :server:build  --console=plain --info --stacktrace >> "%LOGFILE%" 2>&1
set "exitCode=%ERRORLEVEL%"

echo.>> "%LOGFILE%"
echo EndTime: %DATE% %TIME%>> "%LOGFILE%"
echo ExitCode: %exitCode%>> "%LOGFILE%"

echo.
echo Gradle exited with code %exitCode%
echo Log saved to: "%LOGFILE%"
popd
pause
exit /b %exitCode%

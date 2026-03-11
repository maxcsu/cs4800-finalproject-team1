@echo off
setlocal enableextensions
pushd "%~dp0"

echo.
echo ==================================================
echo JavaTron Server Build Helper
echo This will compile and build the server project.
echo ==================================================
echo.
echo Step 1 of 5: Moving into the project folder...

REM Ensure logs folder exists
if not exist "logs" mkdir "logs"
echo Step 2 of 5: Ensured the logs folder exists.

REM Timestamp for filename
for /f "usebackq delims=" %%t in (`powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"`) do set "ts=%%t"
set "LOGFILE=%~dp0logs\zzBUILD_SERVER_%ts%.log"

echo Step 3 of 5: Preparing a build log file.
echo Writing log to: "%LOGFILE%"
echo ==== zzBUILD_SERVER ====>> "%LOGFILE%"
echo StartTime: %DATE% %TIME%>> "%LOGFILE%"
echo WorkingDir: %CD%>> "%LOGFILE%"
echo Command: call .\gradlew.bat :server:build  --console=plain --info --stacktrace>> "%LOGFILE%"
echo ---- output ---->> "%LOGFILE%"

echo Step 4 of 5: Running Gradle to build the server...
call .\gradlew.bat :server:build  --console=plain --info --stacktrace >> "%LOGFILE%" 2>&1
set "exitCode=%ERRORLEVEL%"

echo.>> "%LOGFILE%"
echo EndTime: %DATE% %TIME%>> "%LOGFILE%"
echo ExitCode: %exitCode%>> "%LOGFILE%"

echo.
if not "%exitCode%"=="0" (
    echo Step 5 of 6: Build finished. Reviewing result...
    set "reason=See the log for details."
    for /f "usebackq delims=" %%r in (`powershell -NoProfile -Command "$lines = Get-Content -LiteralPath '%LOGFILE%'; $match = $lines ^| Select-String -Pattern '^\* What went wrong:|^FAILURE:|^BUILD FAILED|^Execution failed for task|^> ' ^| Select-Object -First 1; if ($match) { $match.Line.Trim() }"`) do set "reason=%%r"
    echo BUILD FAILED: %reason%
    echo Gradle exited with code %exitCode%
    echo Log saved to: "%LOGFILE%"
    popd
    pause
    exit /b %exitCode%
)

echo Step 5 of 6: Build succeeded.
echo BUILD SUCCEEDED: The server was compiled successfully.
echo.
echo Step 6 of 6: Launching the server now...
echo ---- run output ---->> "%LOGFILE%"
echo LaunchCommand: call .\gradlew.bat :server:run  --console=plain --info --stacktrace>> "%LOGFILE%"
call .\gradlew.bat :server:run  --console=plain --info --stacktrace >> "%LOGFILE%" 2>&1
set "runExitCode=%ERRORLEVEL%"

if "%runExitCode%"=="0" (
    echo SERVER CLOSED NORMALLY: The built server ran and exited cleanly.
) else (
    set "runReason=See the log for details."
    for /f "usebackq delims=" %%r in (`powershell -NoProfile -Command "$lines = Get-Content -LiteralPath '%LOGFILE%'; $match = $lines ^| Select-String -Pattern '^\* What went wrong:|^FAILURE:|^BUILD FAILED|^Execution failed for task|^> ' ^| Select-Object -Last 1; if ($match) { $match.Line.Trim() }"`) do set "runReason=%%r"
    echo SERVER FAILED TO RUN: %runReason%
)
echo Final exit code: %runExitCode%
echo Log saved to: "%LOGFILE%"
popd
pause
exit /b %runExitCode%

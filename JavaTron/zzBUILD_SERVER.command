#!/usr/bin/env bash
set -u

scriptDir="$(cd "$(dirname "$0")" && pwd)"
cd "$scriptDir" || exit 1

echo
echo "=================================================="
echo "JavaTron Server Build Helper"
echo "This will compile and build the server project."
echo "=================================================="
echo
echo "Step 1 of 5: Moving into the project folder..."

mkdir -p "logs"
echo "Step 2 of 5: Ensured the logs folder exists."

ts="$(date +"%Y%m%d_%H%M%S")"
logFile="$scriptDir/logs/zzBUILD_SERVER_${ts}.log"

gradleCmd=( "./gradlew" ":server:build" "--console=plain" "--info" "--stacktrace" )

echo "Step 3 of 5: Preparing a build log file."
echo "Writing log to: $logFile"
{
  echo "==== zzBUILD_SERVER ===="
  echo "StartTime: $(date)"
  echo "WorkingDir: $(pwd)"
  echo -n "Command: "
  printf '%q ' "${gradleCmd[@]}"
  echo
  echo "---- output ----"
} >>"$logFile"

echo "Step 4 of 5: Running Gradle to build the server..."
"${gradleCmd[@]}" >>"$logFile" 2>&1
exitCode=$?

{
  echo
  echo "EndTime: $(date)"
  echo "ExitCode: $exitCode"
} >>"$logFile"

echo
if [ "$exitCode" -ne 0 ]; then
  echo "Step 5 of 6: Build finished. Reviewing result..."
  reason="$(grep -m 1 -E '^\* What went wrong:|^FAILURE:|^BUILD FAILED|^Execution failed for task|^> ' "$logFile" || true)"
  if [ -z "$reason" ]; then
    reason="See the log for details."
  fi
  echo "BUILD FAILED: $reason"
  echo "Gradle exited with code $exitCode"
  echo "Log saved to: $logFile"
  read -r -n 1 -s -p "Press any key to close..." || true
  echo
  exit "$exitCode"
fi

echo "Step 5 of 6: Build succeeded."
echo "BUILD SUCCEEDED: The server was compiled successfully."
echo
echo "Step 6 of 6: Launching the server now..."
{
  echo "---- run output ----"
  echo "LaunchCommand: ./gradlew :server:run --console=plain --info --stacktrace"
} >>"$logFile"
"./gradlew" ":server:run" "--console=plain" "--info" "--stacktrace" >>"$logFile" 2>&1
runExitCode=$?
if [ "$runExitCode" -eq 0 ]; then
  echo "SERVER CLOSED NORMALLY: The built server ran and exited cleanly."
else
  reason="$(grep -E '^\* What went wrong:|^FAILURE:|^BUILD FAILED|^Execution failed for task|^> ' "$logFile" | tail -n 1 || true)"
  if [ -z "$reason" ]; then
    reason="See the log for details."
  fi
  echo "SERVER FAILED TO RUN: $reason"
fi
echo "Final exit code: $runExitCode"
echo "Log saved to: $logFile"
read -r -n 1 -s -p "Press any key to close..." || true
echo
exit "$runExitCode"

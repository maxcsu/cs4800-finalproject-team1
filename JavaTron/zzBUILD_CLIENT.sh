#!/usr/bin/env bash
set -u

scriptDir="$(cd "$(dirname "$0")" && pwd)"
cd "$scriptDir" || exit 1

echo
echo "=================================================="
echo "JavaTron Client Build Helper"
echo "This will compile and package the desktop client."
echo "=================================================="
echo
echo "Step 1 of 5: Moving into the project folder..."

mkdir -p "logs"
echo "Step 2 of 5: Ensured the logs folder exists."

ts="$(date +"%Y%m%d_%H%M%S")"
logFile="$scriptDir/logs/zzBUILD_CLIENT_${ts}.log"

gradleCmd=( "./gradlew" ":lwjgl3:dist" "--console=plain" "--info" "--stacktrace" )

echo "Step 3 of 5: Preparing a build log file."
echo "Writing log to: $logFile"
{
  echo "==== zzBUILD_CLIENT ===="
  echo "StartTime: $(date)"
  echo "WorkingDir: $(pwd)"
  echo -n "Command: "
  printf '%q ' "${gradleCmd[@]}"
  echo
  echo "---- output ----"
} >>"$logFile"

echo "Step 4 of 5: Running Gradle to build the desktop client..."
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
echo "BUILD SUCCEEDED: The client was compiled and packaged successfully."
echo
echo "Step 6 of 6: Launching the client now..."
{
  echo "---- run output ----"
  echo "LaunchCommand: ./gradlew :lwjgl3:run --console=plain --info --stacktrace"
} >>"$logFile"
"./gradlew" ":lwjgl3:run" "--console=plain" "--info" "--stacktrace" >>"$logFile" 2>&1
runExitCode=$?
if [ "$runExitCode" -eq 0 ]; then
  echo "CLIENT CLOSED NORMALLY: The built client ran and exited cleanly."
else
  reason="$(grep -E '^\* What went wrong:|^FAILURE:|^BUILD FAILED|^Execution failed for task|^> ' "$logFile" | tail -n 1 || true)"
  if [ -z "$reason" ]; then
    reason="See the log for details."
  fi
  echo "CLIENT FAILED TO RUN: $reason"
fi
echo "Final exit code: $runExitCode"
echo "Log saved to: $logFile"
read -r -n 1 -s -p "Press any key to close..." || true
echo
exit "$runExitCode"

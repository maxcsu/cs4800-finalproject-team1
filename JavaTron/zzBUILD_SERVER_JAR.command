#!/usr/bin/env bash
set -u

scriptDir="$(cd "$(dirname "$0")" && pwd)"
cd "$scriptDir" || exit 1

echo
echo "=================================================="
echo "JavaTron Server JAR Build Helper"
echo "This will compile the server and assemble the JAR file."
echo "=================================================="
echo
echo "Step 1 of 5: Moving into the project folder..."

mkdir -p "logs"
echo "Step 2 of 5: Ensured the logs folder exists."

ts="$(date +"%Y%m%d_%H%M%S")"
logFile="$scriptDir/logs/zzBUILD_SERVER_JAR_${ts}.log"

gradleCmd=( "./gradlew" ":server:jar" "--console=plain" "--info" "--stacktrace" )

echo "Step 3 of 5: Preparing a build log file."
echo "Writing log to: $logFile"
{
  echo "==== zzBUILD_SERVER_JAR ===="
  echo "StartTime: $(date)"
  echo "WorkingDir: $(pwd)"
  echo -n "Command: "
  printf '%q ' "${gradleCmd[@]}"
  echo
  echo "---- output ----"
} >>"$logFile"

echo "Step 4 of 5: Running Gradle to create the server JAR..."
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

jarFile="$(find "$scriptDir/server/build/libs" -maxdepth 1 -type f -name '*.jar' | head -n 1)"
if [ -z "$jarFile" ]; then
  echo "Step 5 of 6: Build succeeded, but the JAR file could not be found."
  echo "BUILD FAILED: No server JAR was found in server/build/libs."
  echo "Log saved to: $logFile"
  read -r -n 1 -s -p "Press any key to close..." || true
  echo
  exit 1
fi

echo "Step 5 of 6: Build succeeded."
echo "BUILD SUCCEEDED: The server JAR was created successfully."
echo
echo "Step 6 of 6: Launching the server JAR in a new window..."
{
  echo "---- run output ----"
  printf 'LaunchCommand: java -jar %q\n' "$jarFile"
} >>"$logFile"

osascript <<EOF
tell application "Terminal"
  activate
  do script "cd \"$scriptDir\"; java -jar \"$jarFile\""
end tell
EOF
runExitCode=$?
if [ "$runExitCode" -eq 0 ]; then
  echo "SERVER LAUNCHED: The built server JAR was started in a new window."
else
  reason="macOS Terminal could not be opened to launch the server JAR."
  echo "SERVER FAILED TO RUN: $reason"
fi
echo "Final exit code: $runExitCode"
echo "Log saved to: $logFile"
read -r -n 1 -s -p "Press any key to close..." || true
echo
exit "$runExitCode"

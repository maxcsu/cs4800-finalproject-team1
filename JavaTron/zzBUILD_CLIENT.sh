#!/usr/bin/env bash
set -u

scriptDir="$(cd "$(dirname "$0")" && pwd)"
cd "$scriptDir" || exit 1

mkdir -p "logs"

ts="$(date +"%Y%m%d_%H%M%S")"
logFile="$scriptDir/logs/zzBUILD_CLIENT_${ts}.log"

gradleCmd=( "./gradlew" ":lwjgl3:dist" "--console=plain" "--info" "--stacktrace" )

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

"${gradleCmd[@]}" >>"$logFile" 2>&1
exitCode=$?

{
  echo
  echo "EndTime: $(date)"
  echo "ExitCode: $exitCode"
} >>"$logFile"

echo
echo "Gradle exited with code $exitCode"
echo "Log saved to: $logFile"
read -r -n 1 -s -p "Press any key to close..." || true
echo
exit "$exitCode"
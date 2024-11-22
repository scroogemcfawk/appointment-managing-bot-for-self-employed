#!/bin/bash

stop() {
  # stop application
  echo "Terminating application with PID: $PID"
  bash "./stop.sh"
  exit 0
}

# set current directory to script's parent directory
cd "$(dirname "$0")" || { echo "Failed to set workdir"; exit 1; }

# check if process is running
if [[ -f ".pid" ]]; then
    echo "Process with $(cat .pid) is already running"
    exit 2
fi

# locate jar file
JAR="$(find . -name "samurai-1*.jar")"

# check if jar file exists
if [[ ! -f "$JAR" ]]; then
  echo "Failed to locate jar file"
  exit 3
fi

# set argument if it is not provided
if [[ ! $1 ]]; then
  echo "Config name is not provided, using default"
  set -- "./config/config.json"
fi

# set signal handling
trap "echo Ignoring the signal" HUP QUIT
trap stop INT TERM

# run application in background detached from terminal
nohup java -jar "$JAR" "$1" &

PID="$!"

# save pid to file
echo "$PID" > ".pid"

# echo application PID
echo "Application started with PID: $PID"

wait $PID

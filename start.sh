#!/bin/bash

# set current directory to script's parent directory
cd "$(dirname "$0")" || (echo "Failed to set workdir" | exit 1)

# locate jar file
JAR="$(find . -name "samurai-1*.jar")"

# check if jar file exists
if [ ! -f "$JAR" ]; then
    echo "Failed to locate jar file" | exit 2
fi

# run application
nohup java -jar "$JAR" "$1" &

PID="$!"

# save pid to file
echo "$PID" > ".pid"

# echo application PID
echo "Application started with PID: $PID"

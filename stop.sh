#!/bin/bash

# set current directory to script's parent directory
cd "$(dirname "$0")" || (echo "Failed to set workdir" | exit 1)

# check if jar file exists
if [ ! -f ".pid" ]; then
    echo ".pid file not found" | exit 2
fi

# read pid form file
PID="$(cat ".pid")"

# stop application
kill "$PID"

rm ".pid"

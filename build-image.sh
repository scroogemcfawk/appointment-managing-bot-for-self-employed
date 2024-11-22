#!/bin/bash

APP_ID="$(./gradlew appId | head -n 1)"

docker build --tag smf/"$APP_ID" .

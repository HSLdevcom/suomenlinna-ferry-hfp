#!/bin/sh

echo "Downloading HSL GTFS data..."
wget https://dev.hsl.fi/gtfs/hsl.zip
ls -la hsl.zip

echo "Starting application..."
if [[ "${DEBUG_ENABLED}" = true ]]; then
  java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -XX:InitialRAMPercentage=10.0 -XX:MaxRAMPercentage=95.0 -jar /app/application.jar
else
  java -XX:InitialRAMPercentage=10.0 -XX:MaxRAMPercentage=95.0 -jar /app/application.jar
fi

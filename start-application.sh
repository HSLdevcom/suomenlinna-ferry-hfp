#!/bin/bash

if [[ "${DEBUG_ENABLED}" = true ]]; then
  java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 -jar /app/application.jar
else
  java -jar /app/application.jar
fi

FROM gradle:5.2.1-jdk11-slim AS build
COPY --chown=gradle:gradle . /gradle
WORKDIR /gradle
RUN gradle shadowJar --no-daemon 

#Use jdk instead of jre to get access to remote debug tools
FROM openjdk:11-jdk-slim

RUN mkdir /app

COPY --from=build /gradle/build/libs/*-all.jar /app/application.jar

#curl for health check
RUN apt-get update && apt-get install -y --no-install-recommends curl

COPY start-application.sh /
RUN chmod +x /start-application.sh
CMD ["/start-application.sh"]

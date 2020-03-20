FROM gradle:5.2.1-jdk11-slim AS build
COPY --chown=gradle:gradle . /gradle
WORKDIR /gradle
RUN gradle shadowJar --no-daemon 

FROM openjdk:11-jre-slim

RUN mkdir /app

COPY --from=build /gradle/build/libs/*-all.jar /app/application.jar

ENTRYPOINT ["java", "-Xms2g", "-jar", "/app/application.jar"]

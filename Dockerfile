FROM gradle:5.2.1-jdk8-alpine AS build
COPY --chown=gradle:gradle . /gradle
WORKDIR /gradle
RUN gradle shadowJar --no-daemon 

FROM openjdk:8-jre-slim

RUN mkdir /app

COPY --from=build /gradle/build/libs/*-all.jar /app/application.jar

ENTRYPOINT ["java", "-Xms2560m", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-jar", "/app/application.jar"]

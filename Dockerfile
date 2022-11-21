FROM gradle:7-jdk11-alpine AS app-build
COPY --chown=gradle:gradle . /gradle
WORKDIR /gradle
RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:11-alpine AS jre-build
COPY --from=app-build /gradle/build/libs/*-all.jar /app/application.jar
RUN unzip /app/application.jar -d /app/unpacked
RUN jdeps --print-module-deps --ignore-missing-deps -q --recursive --multi-release 11 --class-path="/app/unpacked/BOOT-INF/lib/*" --module-path="/app/unpacked/BOOT-INF/lib/*" /app/application.jar > /java-modules.txt
RUN apk add --no-cache binutils
RUN jlink --verbose --bind-services --add-modules $(cat /java-modules.txt) --strip-debug --no-man-pages --no-header-files --compress=2 --output /customjre

FROM alpine:3
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=jre-build /customjre $JAVA_HOME

RUN apk add --no-cache java-cacerts && rm -rf ${JAVA_HOME}/lib/security/cacerts && ln -s /etc/ssl/certs/java/cacerts ${JAVA_HOME}/lib/security/cacerts

RUN mkdir /app

COPY --from=app-build /gradle/build/libs/*-all.jar /app/application.jar

#curl for health check
RUN apk add --no-cache curl

COPY start-application.sh /
RUN chmod +x /start-application.sh
CMD ["/start-application.sh"]

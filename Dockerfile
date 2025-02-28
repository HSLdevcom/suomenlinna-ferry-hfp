FROM eclipse-temurin:11-alpine

RUN mkdir /app
COPY build/libs/suomenlinna-ferry-hfp.jar /app/application.jar

#curl for health check
RUN apk add --no-cache curl

COPY start-application.sh /
COPY download-zip.sh /
RUN chmod +x /start-application.sh
RUN chmod +x /download-zip.sh
RUN ls -la /
CMD ["/start-application.sh"]
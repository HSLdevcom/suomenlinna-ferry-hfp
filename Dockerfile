FROM eclipse-temurin:11-alpine

RUN mkdir /app
COPY build/libs/suomenlinna-ferry-hfp.jar /app/application.jar

#curl for health check
RUN apk add --no-cache curl

RUN chmod 777 /tmp

COPY start-application.sh /
RUN chmod +x /start-application.sh
RUN ls -la /
CMD ["/start-application.sh"]
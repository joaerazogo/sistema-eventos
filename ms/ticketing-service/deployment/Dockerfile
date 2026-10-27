FROM eclipse-temurin:25-jdk-alpine
VOLUME /tmp

RUN apk update && apk add --no-cache curl

COPY applications/app-service/build/libs/TicketingService.jar app.jar
RUN sh -c 'touch /app.jar'

ENV TZ=America/Bogota
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT [ "/bin/sh", "-c", "java $JAVA_OPTS -jar /app.jar" ]

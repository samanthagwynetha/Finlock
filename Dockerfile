FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S finlock && adduser -S finlock -G finlock
USER finlock

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
RUN mvn -q -f services/notification-service/pom.xml dependency:go-offline

COPY services/notification-service/src services/notification-service/src
RUN mvn -q -f services/notification-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home spring \
    && chown -R spring:spring /app
COPY --from=build /workspace/services/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8087
USER spring
ENTRYPOINT ["java","-XX:+ExitOnOutOfMemoryError","-jar","/app/app.jar"]

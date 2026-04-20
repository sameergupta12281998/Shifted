FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY services/tracking-service/pom.xml services/tracking-service/pom.xml
RUN mvn -q -f services/tracking-service/pom.xml dependency:go-offline

COPY services/tracking-service/src services/tracking-service/src
RUN mvn -q -f services/tracking-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
	&& useradd --system --create-home spring \
	&& chown -R spring:spring /app
COPY --from=build /workspace/services/tracking-service/target/tracking-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
USER spring
ENTRYPOINT ["java","-XX:+ExitOnOutOfMemoryError","-jar","/app/app.jar"]

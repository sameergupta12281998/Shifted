FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY platform/api-gateway/pom.xml platform/api-gateway/pom.xml
RUN mvn -q -f platform/api-gateway/pom.xml dependency:go-offline

COPY platform/api-gateway/src platform/api-gateway/src
RUN mvn -q -f platform/api-gateway/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
	&& useradd --system --create-home spring \
	&& chown -R spring:spring /app
COPY --from=build /workspace/platform/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
USER spring
ENTRYPOINT ["java","-XX:+ExitOnOutOfMemoryError","-jar","/app/app.jar"]

# Use Maven to build the Spring Boot application
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

# Run the packaged jar in a lightweight JRE image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

EXPOSE 8080

COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

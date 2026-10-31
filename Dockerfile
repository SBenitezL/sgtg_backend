# Use Maven to build the Spring Boot application
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

# Run the packaged jar in a lightweight JRE image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# La aplicación corre con un usuario sin privilegios: si alguien llegara a ejecutar código en el contenedor,
# no sería root. Solo puede escribir en la carpeta de los archivos (el volumen la toma con este dueño al crearse).
RUN groupadd --system --gid 10001 sgtg \
    && useradd --system --uid 10001 --gid sgtg --no-create-home --shell /usr/sbin/nologin sgtg \
    && mkdir -p /app/archivos && chown sgtg:sgtg /app/archivos

EXPOSE 8080

COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

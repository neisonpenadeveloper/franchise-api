# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilacion ----------
# Se compila dentro de la imagen para no depender del JDK de la maquina.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# Las dependencias se resuelven en una capa aparte: mientras el pom no cambie,
# Docker reutiliza la cache y no vuelve a descargarlas en cada build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: ejecucion ----------
# Solo el JRE y el jar: la imagen final no lleva Maven ni el codigo fuente.
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuario sin privilegios: el contenedor no corre como root.
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /build/target/franchise-api-*.jar app.jar
RUN chown spring:spring app.jar

USER spring
EXPOSE 8080

# wget viene en la imagen alpine y evita instalar curl solo para esto.
# El puerto se resuelve igual que en la aplicacion, para que el chequeo siga
# siendo valido cuando la plataforma inyecta PORT.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q --spider "http://localhost:${PORT:-${SERVER_PORT:-8080}}/actuator/health" || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

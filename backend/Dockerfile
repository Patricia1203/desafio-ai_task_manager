# syntax=docker/dockerfile:1

# ---- Build stage -------------------------------------------------------------
# Imagem oficial do Maven com JDK 21 (o eclipse-temurin puro nao traz mvn).
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Resolve as dependencias em camada propria para reaproveitar o cache do Docker
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests

# ---- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# usuario nao-root
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
# o jar ja embute src/main/resources (prompts e migrations do Flyway)
COPY --from=build /workspace/target/*.jar app.jar

RUN chown -R app:app /app
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
# syntax=docker/dockerfile:1
# Claim Surveyor Assignment System - container image.
# Stage 1 builds the WAR with Maven; stage 2 runs it on a small JRE image (the WAR is a
# Spring Boot executable, so it carries its own embedded Tomcat).

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
ARG APP_ENV=dev
COPY pom.xml .
COPY src ./src
# Cache mount keeps downloaded dependencies between builds (needs BuildKit, default in Docker Desktop)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -Dmaven.test.skip=true -Dapp.environment=${APP_ENV}

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
ARG APP_VERSION=0.1.0-SNAPSHOT
LABEL org.opencontainers.image.title="claim-surveyor" \
      org.opencontainers.image.description="Claim Surveyor Assignment System" \
      org.opencontainers.image.version="${APP_VERSION}"

# Run as a non-root user
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
RUN mkdir -p /app/data && chown -R app:app /app
COPY --from=build --chown=app:app /build/target/claim-surveyor.war /app/claim-surveyor.war

USER app

# Runtime configuration (all can be overridden with docker run -e ...)
ENV PORT=8080 \
    DB_PATH=/app/data/claimdb \
    APP_ENV=dev

# The H2 database lives here; mount a volume so data survives container removal
VOLUME ["/app/data"]
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=3 \
    CMD wget -qO- http://localhost:${PORT}/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/claim-surveyor.war"]

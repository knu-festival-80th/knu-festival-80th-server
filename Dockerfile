# syntax=docker/dockerfile:1.7

# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Cache Gradle dependencies first
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew \
 && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Build bootJar
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app \
 && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
 && mkdir -p /app/uploads/images \
 && chown -R app:app /app/uploads

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

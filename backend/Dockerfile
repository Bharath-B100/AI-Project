# ─────────────────────────────────────────────
# Stage 1: Build — Maven + JDK 17
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom first (layer caching)
COPY pom.xml .
COPY apache-maven-3.9.6 ./apache-maven-3.9.6

# Download dependencies (cached unless pom.xml changes)
RUN ./apache-maven-3.9.6/bin/mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN ./apache-maven-3.9.6/bin/mvn clean package -DskipTests -B

# ─────────────────────────────────────────────
# Stage 2: Run — slim JRE only
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/ai-project-manager-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose the port (Render injects $PORT at runtime)
EXPOSE 8080

# Start the app
ENTRYPOINT ["java", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]

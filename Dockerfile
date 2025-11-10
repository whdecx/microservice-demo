# Multi-stage build for optimized image size

# Build arguments for flexible source selection
ARG BUILD_SOURCE=local
ARG GIT_REPO=https://github.com/whdecx/microservice-demo.git
ARG GIT_BRANCH=main

# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Install git for GitHub cloning
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Use build argument to determine source
ARG BUILD_SOURCE
ARG GIT_REPO
ARG GIT_BRANCH

# For GitHub builds: Clone the repository
# For local builds: This step is skipped
RUN if [ "$BUILD_SOURCE" = "github" ]; then \
        echo "Building from GitHub: $GIT_REPO (branch: $GIT_BRANCH)"; \
        git clone --depth 1 --branch $GIT_BRANCH $GIT_REPO . ; \
    else \
        echo "Building from local source files"; \
    fi

# For local builds: Copy pom.xml first (enables dependency caching)
# For GitHub builds: Files already present, COPY will overwrite with same content
COPY pom.xml .

# Download dependencies (cached layer for both build types)
RUN mvn dependency:go-offline -B

# For local builds: Copy source code
# For GitHub builds: Files already present, COPY will overwrite with same content
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port 8080
EXPOSE 8080

# Health check for container orchestration (ECS)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Optional JVM tuning for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

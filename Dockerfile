FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Install Gradle
RUN apk add --no-cache gradle

# Copy build files first (better layer caching)
COPY settings.gradle.kts build.gradle.kts ./

# Download dependencies
RUN gradle dependencies --no-daemon -q || echo "Dependencies download completed"

# Copy source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

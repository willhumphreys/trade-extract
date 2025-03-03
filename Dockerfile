# Stage 1: Build the application using Gradle
FROM gradle:8-jdk21 AS builder
WORKDIR /app

# Copy the Gradle wrapper scripts and the entire gradle directory
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/

# Copy the build configuration files
COPY build.gradle.kts settings.gradle.kts ./

# Copy the source code
COPY src/ src/

# Ensure the Gradle wrapper is executable and build the project
RUN chmod +x gradlew && ./gradlew clean shadowJar --no-daemon

# Stage 2: Create a lightweight image to run the application
FROM openjdk:21-jdk-slim
WORKDIR /app

RUN apt-get update && apt-get install -y lzop

# Copy the built JAR file from the builder stage (adjust the pattern if needed)
COPY --from=builder /app/build/libs/*.jar app.jar

# If your application listens on a specific port, uncomment and adjust the following line:
# EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM gradle:8-jdk21 AS builder
WORKDIR /app

ARG REPSY_USERNAME
ARG REPSY_PASSWORD

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY src/ src/

RUN chmod +x gradlew && \
    REPSY_USERNAME=${REPSY_USERNAME} \
    REPSY_PASSWORD=${REPSY_PASSWORD} \
    ./gradlew clean shadowJar --no-daemon

FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y \
    liblzo2-2 \
    && rm -rf /var/lib/apt/lists/*

ENV HADOOP_HOME=/app
ENV JAVA_LIBRARY_PATH=/usr/lib/jni

WORKDIR /app
COPY --from=builder /app/build/libs/*-all.jar /app/app.jar

# Copy the built JAR file from the builder stage (adjust the pattern if needed)
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-Djava.library.path=/usr/lib/jni", "-jar", "/app/app.jar"]

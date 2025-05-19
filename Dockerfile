# Versions
ARG GRADLE_VERSION=8.13.0-jdk21
ARG TEMURIN_VERSION=21-alpine

# Stage 1: Cache Gradle dependencies
FROM gradle:${GRADLE_VERSION} AS cache
RUN mkdir -p /home/gradle/cache_home/
ENV GRADLE_USER_HOME=/home/gradle/cache_home/
COPY /app/build.gradle.* gradle.properties /home/gradle/app/
COPY gradle /home/gradle/app/gradle/
WORKDIR /home/gradle/app/
RUN gradle clean build -i --stacktrace

# Stage 2: Build Application
FROM gradle:${GRADLE_VERSION} AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN gradle shadowJar --no-daemon

# Stage 3: Create the Runtime Image
FROM eclipse-temurin:${TEMURIN_VERSION} AS runtime
COPY --from=build /home/gradle/src/app/build/libs/app-all.jar app/
ENTRYPOINT ["java", "-jar", "/app/app-all.jar"]

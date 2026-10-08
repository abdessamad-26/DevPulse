# Multi-stage Dockerfile for backend
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
# The BuildKit cache mount keeps downloaded Maven dependencies between builds,
# so a code change no longer re-downloads the whole dependency tree.
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app
COPY --from=build --chown=app:app /workspace/target/*.jar /app/app.jar
# Run as an unprivileged user: a compromised process must not be root in the container.
USER app
EXPOSE 8080
# Size the heap from the container memory limit instead of the host's RAM.
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]

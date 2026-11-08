FROM gradle:8.7-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle :tars-connector:installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/tars-connector/build/install/tars-connector/ ./
USER 10001
ENTRYPOINT ["./bin/tars-connector"]

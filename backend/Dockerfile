FROM gradle:9.9-jdk27 AS build
WORKDIR /workspace
COPY . .
RUN gradle :backend:installDist --no-daemon

FROM eclipse-temurin:27-jre
WORKDIR /app
COPY --from=build /workspace/backend/build/install/backend/ ./
EXPOSE 8081
USER 10001
ENTRYPOINT ["./bin/backend"]

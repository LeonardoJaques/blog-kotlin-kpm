FROM eclipse-temurin:27-jre
WORKDIR /app
COPY --chown=10001:10001 install/backend/ ./
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["./bin/backend"]

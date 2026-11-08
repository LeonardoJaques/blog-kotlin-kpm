FROM eclipse-temurin:27-jre
WORKDIR /app
COPY --chown=10001:10001 .kobweb/conf.yaml .kobweb/conf.yaml
COPY --chown=10001:10001 .kobweb/site/ .kobweb/site/
COPY --chown=10001:10001 .kobweb/server/server.jar .kobweb/server/server.jar
RUN mkdir -p .kobweb/server/logs && chown -R 10001:10001 .kobweb
USER 10001:10001
EXPOSE 3000
CMD ["java", "-Dkobweb.server.environment=PROD", "-Dkobweb.site.layout=FULLSTACK", "-Dio.ktor.development=false", "-jar", ".kobweb/server/server.jar"]

# Verify - background (JobRunR) service. Runs the prebuilt Spring Boot 3.4 jar.
# The jar is Java 21 bytecode (built source=21) — needs JRE 21, NOT 11 (the idp's JDK).
# DB config comes from SPRING_DATASOURCE_* env (see .env.example / docker-compose.yml).
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# build_2.6.9/ holds ezto-bg.jar, application.properties, and providers/
# (application.properties sets library.directory=./providers).
COPY build_2.6.9/ /app/
EXPOSE 8070
CMD ["java", "-Dspring.config.location=application.properties", "-jar", "ezto-bg.jar"]

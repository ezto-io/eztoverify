# Verify - background (JobRunR) service.
#
# Builds the Spring Boot 3.4 app FROM SOURCE (Java 21) — no prebuilt jar shipped in git
# (build_*/ is .gitignored). Stage 1 mirrors build.sh: compile the manager into
# ezto-bg.jar plus each provider module into providers/. Stage 2 is a slim JRE 21 runtime.
#
# application.properties sets library.directory=./providers, so the provider jars must land
# next to the app jar under /app/providers.
#
#   docker compose build background     (or: docker build -t ezto-background services/background)
ARG VERSION=2.6.9
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy

# ---- Stage 1: build the app + provider plugins from source -----------------
FROM ${MAVEN_IMAGE} AS build
ARG VERSION
ENV MAVEN_OPTS="-Xmx2g"
WORKDIR /src
# Full background source. .dockerignore trims build_*/ and per-module target/.
COPY . .

# Assemble the dist layout under /out (mirrors build_${VERSION}/).
# A shared local .m2 cache lets each module resolve the ones installed before it.
RUN --mount=type=cache,target=/root/.m2 set -eux; \
    mkdir -p /out/providers; \
    # main app -> ezto-bg.jar
    (cd ezto-bg-manager && mvn -B clean install -U -Dmaven.test.skip package spring-boot:repackage); \
    cp ezto-bg-manager/ezto-bg/src/main/resources/application.properties /out/; \
    cp ezto-bg-manager/ezto-bg/target/ezto-bg-${VERSION}.jar /out/ezto-bg.jar; \
    # provider plugins -> providers/
    for m in helpers extensions email-services license-services sync-services hook-notifier-services scheduler-services; do \
      (cd ezto-bg-${m} && mvn -B clean install -U -Dmaven.test.skip package); \
      cp ezto-bg-${m}/target/ezto-bg-${m}-${VERSION}.jar /out/providers/; \
    done

# ---- Stage 2: slim runtime -------------------------------------------------
FROM ${RUNTIME_IMAGE}
WORKDIR /app
COPY --from=build /out/ /app/
EXPOSE 8070
CMD ["java", "-Dspring.config.location=application.properties", "-jar", "ezto-bg.jar"]

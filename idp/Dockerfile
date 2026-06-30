# Verify IdP (Keycloak 18 fork) — OSS image.
#
# Builds the whole distribution FROM SOURCE (no prebuilt dist shipped in git), then
# containerizes the augmented runtime. The build runs on JDK 11 + Maven — JDK 11 is a
# hard constraint (JDK 21 breaks Byte Buddy / Quarkus augmentation). build.sh sources
# eztoverify.sh, which bakes the build-time options OFFERING=onpremise, APP_PUSH_VERSION=9.3.1,
# MODE=bo, MULTITENANT=true (these MUST be set at build time or version URLs / migrators break).
#
# Heavy build: compiles Keycloak + all qlik-verify modules + themes (the theme Maven build
# pulls its own Node via the frontend plugin). Give Docker plenty of RAM/disk. To rebuild:
#   docker compose build idp     (or: docker build -t ezto-idp idp/)
ARG MAVEN_IMAGE=maven:3.8-eclipse-temurin-11
ARG RUNTIME_IMAGE=eclipse-temurin:11-jre-jammy

# ---- Stage 1: build the QV/Keycloak distribution from source --------------
FROM ${MAVEN_IMAGE} AS build
ENV LANG=en_US.UTF-8 \
    MAVEN_OPTS="-Xmx2g"
WORKDIR /src
# Full idp source (themes + keycloak + qlik-verify-addons + build-tools + build.sh).
# .dockerignore trims build outputs (build_*/target/node_modules) and dev cruft.
COPY . .
# build.sh -p true -> ./docker-keycloak (the assembled dist). Then kc.sh build augments
# it (jlink needs the JDK present in this stage); http-relative-path=/auth lives in the
# dist conf and is preserved by the augmentation.
RUN chmod +x build.sh eztoverify.sh build-tools/*.sh && \
    ./build.sh -v 0.0.0 -c eztoverify -p true && \
    mv docker-keycloak /opt/keycloak && \
    /opt/keycloak/bin/kc.sh build

# ---- Stage 2: slim runtime -------------------------------------------------
FROM ${RUNTIME_IMAGE}
ENV LANG=en_US.UTF-8 \
    QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE=100M
USER 0
COPY --chown=1001:0 --from=build /opt/keycloak /opt/keycloak
RUN chmod -R g=u /opt/keycloak
USER 1001
EXPOSE 8443 8787 7800 8080
CMD ["/opt/keycloak/bin/kc.sh", "start", "--auto-build"]

set -e

# pre-build
cd qlik-verify-extensions

# build
mvn install

# post-build
rm -rf ../../../build_$1/keycloak/keycloak-*/modules/com/qlikverify/extensions/main/
../../../build_$1/keycloak/keycloak-*/bin/jboss-cli.sh --command="module add --name=com.qlikverify.extensions --resources=target/qlik-verify-extensions.jar --dependencies=org.keycloak.keycloak-core,org.keycloak.keycloak-services,org.keycloak.keycloak-model-jpa,org.keycloak.keycloak-server-spi,org.keycloak.keycloak-server-spi-private,javax.api,javax.ws.rs.api,javax.persistence.api,com.fasterxml.jackson.core.jackson-databind,com.fasterxml.jackson.core.jackson-core,org.keycloak.keycloak-common,org.jboss.logging,org.jboss.resteasy.resteasy-jaxrs,org.jboss.resteasy.resteasy-multipart-provider,org.apache.commons.io,org.apache.httpcomponents,com.google.code.gson,com.squareup.okhttp3,com.fasterxml.jackson.core.jackson-annotations,org.hibernate,org.javassist,org.liquibase,org.infinispan,org.infinispan.commons"


echo "========================================================================="
echo " qlik-verify-extensions BUILD COMPLETED"
echo "========================================================================="

cd ../../../
cd build_$1/keycloak/keycloak-*/bin 

./standalone.sh --debug
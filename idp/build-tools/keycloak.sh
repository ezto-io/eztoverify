#!/bin/bash
set -e

## KEYCLOAK
echo "Keycloak Build Started"
mkdir -p ../build_$1/keycloak
cp -r ../build-tools/docker-keycloak/ ../build_$1/keycloak/docker-keycloak

pushd ../keycloak
pwd
cd quarkus
mvn -f ../pom.xml clean install -DskipTestsuite -DskipExamples -Dmaven.test.skip=true
mvn clean install -Dmaven.test.skip=true

cp dist/target/keycloak-*.tar.gz ../../build_$1/keycloak/
tar -zxf ../../build_$1/keycloak/keycloak-*.tar.gz -C ../../build_$1/keycloak
rm -rf ../../build_$1/keycloak/keycloak-*.tar.gz
echo -e "Keycloak Build Completed\n"
popd
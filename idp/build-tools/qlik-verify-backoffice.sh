#!/bin/bash
set -e

## QV SERVICE
echo "QV Backoffice Build Started"
pushd ../qlik-verify-addons/services/qlik-verify-backoffice

mvn clean install

cp target/qlik-verify-backoffice.jar ../../../build_$1/keycloak/keycloak-*/providers/

cp -a theme/login/* ../../../build_$1/keycloak/keycloak-*/themes/qlik_verify/login/
#cp -a theme/js/* ../../../build_$1/keycloak/keycloak-*/themes/qlik_verify/login/resources/js
#cp -a theme/img/* ../../../build_$1/keycloak/keycloak-*/themes/qlik_verify/login/resources/img
echo -e "QV Backoffice Build Completed\n"
popd


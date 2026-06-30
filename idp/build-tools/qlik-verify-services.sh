#!/bin/bash
set -e

## QV SERVICE
echo "QV Service Build Started"
pushd ../qlik-verify-addons/services/qlik-verify-services

mvn clean install

cp target/qlik-verify-services.jar ../../../build_$1/keycloak/keycloak-*/providers/
cp -a theme/admin/img/* ../../../build_$1/keycloak/keycloak-*/themes/qlik_verify/admin/resources/themes/img/
echo -e "QV Service Build Completed\n"
popd


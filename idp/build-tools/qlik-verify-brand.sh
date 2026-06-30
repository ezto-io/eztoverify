#!/bin/bash
set -e

## QlikVerify Brand
echo "QlikVerify Brand Build started"
pushd ../qlik-verify-addons/services/qlik-verify-brand

mvn clean install

cp target/qlik-verify-brand.jar ../../../build_$1/keycloak/keycloak-*/providers/

echo -e "QlikVerify Brand Build Completed\n"
popd


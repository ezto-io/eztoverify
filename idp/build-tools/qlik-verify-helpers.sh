#!/bin/bash
set -e

## QV HELPERS
echo "QV helpers Build Started"
pushd ../qlik-verify-addons/services/qlik-verify-helpers

mvn clean install

cp target/qlik-verify-helpers.jar ../../../build_$1/keycloak/keycloak-*/providers/
echo -e "QV helpers Build Completed\n"
popd

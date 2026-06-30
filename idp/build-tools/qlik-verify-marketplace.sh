#!/bin/bash
set -e

## QV MARKETPLACE
echo "QV Marketplace Build Started"
pushd ../qlik-verify-addons/services/qlik-verify-marketplace

mvn clean install

cp target/qlik-verify-marketplace.jar ../../../build_$1/keycloak/keycloak-*/providers/
echo -e "QV Marketplace Build Completed\n"
popd

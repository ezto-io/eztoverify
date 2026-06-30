#!/bin/bash
set -e

## QV EXTENSION
echo "QV Extension Build Started"
pushd ../qlik-verify-addons/services/qlik-verify-extensions

mvn install

cp target/qlik-verify-extensions.jar ../../../build_$1/keycloak/keycloak-*/providers/
echo -e "QV Extensions Build Completed\n"

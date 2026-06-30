#! /bin/sh
set -e

export MODE=bo
export MULTITENANT=true
export OFFERING=onpremise
# Maven filters ${env.APP_PUSH_VERSION} into keycloak-version.properties
# (qv-version) -> Version.QV_VERSION -> admin-console serverVersion/resourceVersion
# and the migration model version. Must be a valid x.y.z at BUILD time, else the
# literal "${env.APP_PUSH_VERSION}" leaks into ?version= URLs (400) and version parsing.
export APP_PUSH_VERSION=9.3.1
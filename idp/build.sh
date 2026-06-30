#!/bin/bash
set -e

export VER="0.0.0"
export BUILD_FOR=""
export PACKAGING="false"

while test $# -gt 0; do
  case "$1" in
    -h|--help)
      echo " "
      echo "./build.sh [options]"
      echo " "
      echo "options:"
      echo "-h,--help                                 show brief help"
      echo "-v,  --ver                                version of the package, default: 1.1.0"
      exit 0
      ;;
    -v|--ver)
      shift
      if test $# -gt 0; then
        export VER="$1"
      else
        echo "no version specified"
        exit 1
      fi
      shift
      ;;
    -c|--customer)
      shift
      if test $# -gt 0; then
        export BUILD_FOR="$1"
      else
        echo "no customer specified. Using this script for building eztoverify"
        export BUILD_FOR="eztoverify"
        exit 1
      fi
      shift
      ;;
    -p|--package-build)
      shift
      if test $# -gt 0; then
        export PACKAGING="$1"
      else
        echo "no packaging specified"
        exit 1
      fi
      shift
      ;;
    *)
      break
      ;;
  esac
done
source eztoverify.sh

echo "Building EZTO Verify (OSS)"
echo "-----------------------------"

rm -rf build_* qlik_verify_* docker-keycloak.tar.gz docker-keycloak

echo "QV Theme Build..."
pushd keycloak/themes
mvn clean install -Pnpm-update
popd
echo -e "QV Theme Build Completed\n"

echo "QV Build..."
pushd build-tools
./qv-init.sh
./keycloak.sh ${VER}
./qlik-verify-theme.sh ${VER}
./qlik-verify-services.sh ${VER}
./qlik-verify-extensions.sh ${VER}
./qlik-verify-helpers.sh ${VER}
./qlik-verify-brand.sh ${VER}
./qlik-verify-backoffice.sh ${VER}
./qlik-verify-marketplace.sh ${VER}
popd
echo -e "QV Build Completed\n"

# Proprietary vendor connectors (Sekura, Finema, iProov, UAE Pass, DigiLocker,
# SurePass, Twilio IVR, EZTO Pass, Keyless, Pindrop, aShield, TOTM) are NOT part
# of the OSS distribution. They live outside this repo under ../private-connectors/.
# See ../private-connectors/README.md for the roadmap to re-add them.

if [ "$PACKAGING" == "true" ];
then
  echo "Packaging QV For Docker Build..."
  mv build_${VER}/keycloak/keycloak-18.0.2 docker-keycloak
else
  echo "Skipping build packaging step"
fi

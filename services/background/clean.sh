#!/bin/bash
set -e

VERSION=2.6.7

while test $# -gt 0; do
  case "$1" in
    -h|--help)
      echo " "
      echo "./build.sh [options]"
      echo " "
      echo "options:"
      echo "-h,  --help                     show brief help"
      echo "-v,  --ver                      version of the package, default: 1.3.0"
      exit 0
      ;;
    -v|--ver)
      shift
      if test $# -gt 0; then
        export VERSION=$1
      else
        echo "no version specified"
        exit 1
      fi
      shift
      ;;

    *)
      break
      ;;
  esac
done

rm -rf build_${VERSION}

# ezto BG MANAGER
pushd ezto-bg-manager
mvn clean
popd

# ezto HOOK SERVICES
pushd ezto-bg-hook-notifier-services
mvn clean
popd

# ezto EMAIL SERVICES
pushd ezto-bg-email-services
mvn clean
popd

# ezto LICENSE SERVICES
pushd ezto-bg-license-services
mvn clean
popd

# ezto SYNC SERVICES
pushd ezto-bg-sync-services
mvn clean
popd

#mv build_${VERSION} build
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
      echo "-p,  --package-build            Creates package ready to dockerize"
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

rm -rf build_${VERSION}

mkdir -p build_${VERSION}/providers

pushd ezto-bg-manager
mvn clean install -U -Dmaven.test.skip package spring-boot:repackage
cp -i ezto-bg/src/main/resources/application.properties ../build_${VERSION}/
mv ezto-bg/target/ezto-bg-${VERSION}.jar ../build_${VERSION}/ezto-bg.jar
popd

if [ "$PACKAGING" == "true" ];
then
  echo "Packaging Verify Background For Docker Build..."
  mv build_${VERSION} build
else
  echo "Skipping build packaging step"
fi

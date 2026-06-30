#!/bin/bash
set -e

VERSION=2.6.9

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

# Verify HELPER SERVICES
pushd ezto-bg-helpers
mvn clean
mvn clean install -U -Dmaven.test.skip package
mv target/ezto-bg-helpers-${VERSION}.jar ../build_${VERSION}/providers/ezto-bg-helpers-${VERSION}.jar
popd

# Verify EXTENSIONS SERVICES
pushd ezto-bg-extensions
mvn clean install -U -Dmaven.test.skip package
mv target/ezto-bg-extensions-${VERSION}.jar ../build_${VERSION}/providers/ezto-bg-extensions-${VERSION}.jar
popd

# Verify EMAIL SERVICES
pushd ezto-bg-email-services
mvn clean install -U -Dmaven.test.skip package
mv target/ezto-bg-email-services-${VERSION}.jar ../build_${VERSION}/providers/ezto-bg-email-services-${VERSION}.jar
popd

# Verify LICENSE SERVICES
pushd ezto-bg-license-services
mvn clean install -U -Dmaven.test.skip package
mv target/ezto-bg-license-services-${VERSION}.jar ../build_${VERSION}/providers/ezto-bg-license-services-${VERSION}.jar
popd

# Verify SYNC SERVICES
pushd ezto-bg-sync-services
mvn clean
mvn clean install package
mv target/ezto-bg-sync-services-${VERSION}.jar ../build_${VERSION}/providers/ezto-bg-sync-services-${VERSION}.jar
popd

# Verify HOOK NOTIFIER SERVICES
pushd ezto-bg-hook-notifier-services
mvn clean
mvn clean install package
mv target/ezto-bg-hook-notifier-services-${VERSION}.jar ../build_${VERSION}/providers/ezto-bg-hook-notifier-services-${VERSION}.jar
popd

# Verify SCHEDULER SERVICES
pushd ezto-bg-scheduler-services
mvn -U -Dmaven.test.skip clean install
mv target/ezto-bg-scheduler-services-${VERSION}.jar ../build_${VERSION}/providers/ezto-bg-scheduler-services-${VERSION}.jar
popd

if [ "$PACKAGING" == "true" ];
then
  echo "Packaging QV Background For Docker Build..."
  mv build_${VERSION} build
else
  echo "Skipping build packaging step"
fi

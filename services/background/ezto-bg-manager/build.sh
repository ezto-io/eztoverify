#!/bin/bash
set -e

VERSION=2.6.6

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

rm -rf ezto-bg.jar

mvn clean

mvn -Dmaven.test.skip package spring-boot:repackage

mv ezto-bg/target/ezto-bg-${VERSION}.jar ezto-bg/target/ezto-bg.jar

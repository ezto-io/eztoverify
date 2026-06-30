#!/bin/bash
set -e

export VER="0.0.0"

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
    *)
      break
      ;;
  esac
done

docker-compose  -f docker-compose-local.yml down || true

echo "Building QV docker image..."
docker build . --build-arg VERSION=${VER} --tag "qlik-verify:local-${VER}"
echo -e "Docker Image Build Completed\n"

echo "Preparing QV docker-compose..."
sed 's#{version}#'"local-${VER}"'#' docker-compose-dev.yml > docker-compose-local.yml
echo -e "Docker Compose QV image replaced with tag ${VER}\n"

echo "Deploying Docker Compose in you local machine..."
docker-compose -f docker-compose-local.yml up -d
echo -e "Docker Compose Deployed\n"

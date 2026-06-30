set -e

#source env.sh

export VER="2.6.9"

while test $# -gt 0; do
  case "$1" in
    -h|--help)
      echo " "
      echo "./smart-builder.sh [options]"
      echo " "
      echo "options:"
      echo "-h,--help                                 show brief help"
      echo "-v,  --ver                                version of the package, default: 1.3.0"
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

cd build_${VER}
java -Dspring.config.location=application.properties -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8777 -jar ezto-bg.jar

#!/bin/bash
set -e

export VER="2.6.7"


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

DIFF_LIST=`git diff --name-only`

ezto_bg_email_services="false"
ezto_bg_license_services="false"
ezto_bg_hook_notifier_services="false"
ezto_bg_manager="false"
ezto_bg_sync_services="false"
ezto_bg_scheduler_services="false"

for n in $DIFF_LIST
do
    echo "Working on $n file name now"
    case $n in
        *ezto-bg-email-services/*)
           if [[ $ezto_bg_email_services == "false" ]]; then
            ezto_bg_email_services="true"
        fi
        ;;
        *ezto-bg-hook-notifier-services/*)
           if [[ $ezto_bg_hook_notifier_services == "false" ]]; then
            ezto_bg_hook_notifier_services="true"
        fi
        ;;
        *ezto-bg-license-services/*)
           if [[ $ezto_bg_license_services == "false" ]]; then
            ezto_bg_license_services="true"
        fi
        ;;
        *ezto-bg-manager/*)
           if [[ $ezto_bg_manager == "false" ]]; then
            ezto_bg_manager="true"
        fi
        ;;
        *ezto-bg-sync-services/*)
           if [[ $ezto_bg_sync_services == "false" ]]; then
            ezto_bg_sync_services="true"
        fi
        ;;
        *ezto-bg-scheduler-services/*)
           if [[ $ezto_bg_scheduler_services == "false" ]]; then
            ezto_bg_scheduler_services="true"
        fi
        ;;
    esac
done

if [[ $ezto_bg_email_services == "true" ]]; then
    pushd ezto-bg-email-services
        mvn -U -Dmaven.test.skip clean install
        mv target/ezto-bg-email-services-${VER}.jar ../build_${VER}/providers/
    popd
fi

if [[ $ezto_bg_hook_notifier_services == "true" ]]; then
    pushd ezto-bg-hook-notifier-services
        mvn -U -Dmaven.test.skip clean install
        mv target/ezto-bg-hook-notifier-services-${VER}.jar ../build_${VER}/providers/
    popd
fi

if [[ $ezto_bg_license_services == "true" ]]; then
    pushd ezto-bg-license-services
        mvn -U -Dmaven.test.skip clean install
        mv target/ezto-bg-license-services-${VER}.jar ../build_${VER}/providers/
    popd
fi

if [[ $ezto_bg_manager == "true" ]]; then
    pushd ezto-bg-manager
        mvn clean install -U -Dmaven.test.skip package spring-boot:repackage
        cp -f ezto-bg/src/main/resources/application.properties ../build_${VER}/
        mv ezto-bg/target/ezto-bg-${VER}.jar ../build_${VER}/ezto-bg.jar
        mv ezto-core/target/ezto-core-${VER}.jar ../build_${VER}/ezto-core.jar
    popd
fi

if [[ $ezto_bg_sync_services == "true" ]]; then
    pushd ezto-bg-sync-services
        mvn -U -Dmaven.test.skip clean install
        mv target/ezto-bg-sync-services-${VER}.jar ../build_${VER}/providers/
    popd
fi

if [[ $ezto_bg_scheduler_services == "true" ]]; then
    pushd ezto-bg-scheduler-services
        mvn -U -Dmaven.test.skip clean install
        mv target/ezto-bg-scheduler-services-${VER}.jar ../build_${VER}/providers/
    popd
fi
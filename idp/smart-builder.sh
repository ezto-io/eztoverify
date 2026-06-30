#!/bin/bash
set -e

export VER="0.0.0"


while test $# -gt 0; do
  case "$1" in
    -h|--help)
      echo " "
      echo "./smart-builder.sh [options]"
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

## Check if the shell bash or zsh
#if [ -n "$BASH_VERSION" ]; then
#    source ~/.bash_profile 2>/dev/null || source ~/.zshrc || true
#elif [ -n "$ZSH_VERSION" ]; then
#    source ~/.zshrc
#fi

DIFF_LIST=`git diff --name-only`
common="false"
server_spi_private="false"
server_spi="false"
services="false"
model_jpa="false"
core="false"
qv_brand="false"
qv_theme="false"
model_infinispan="false"
qv_services="false"
qv_backoffice="false"
qv_marketplace="false"
qv_extension="false"
keyless="false"
surepass="false"
pindrop="false"
totm="false"
iproov="false"
qv_private="false"
twilio="false"
sekura="false"
uae_pass="false"
ezto_pass="false"
digilocker="false"
#ashield="false"
for n in $DIFF_LIST
do
    echo "Working on $n file name now"
    case $n in
        *keycloak/common*)
        if [[ $common == "false" ]]; then
            common="true"
        fi
        ;;
        *keycloak/server-spi-private*)
        if [[ $server_spi_private == "false" ]]; then
            server_spi_private="true"
        fi
        ;;
        *keycloak/server-spi*)
        if [[ $server_spi == "false" ]]; then
            server_spi="true"
        fi
        ;;
        *keycloak/services*)
        if [[ $services == "false" ]]; then
            services="true"
        fi
        ;;
        *keycloak/model/jpa*)
        if [[ $model_jpa == "false" ]]; then
            model_jpa="true"
        fi
        ;;
        *keycloak/model/infinispan*)
        if [[ $model_infinispan == "false" ]]; then
            model_infinispan="true"
        fi
        ;;
        *keycloak/core*)
        if [[ $core == "false" ]]; then
            core="true"
        fi
        ;;
        *qlik-verify-addons/services/qlik-verify-brand*)
        if [[ $qv_brand == "false" ]]; then
            qv_brand="true"
        fi
        ;;
        *qlik-verify-addons/services/qlik-verify-services*)
        if [[ $qv_services == "false" ]]; then
            qv_services="true"
        fi
        ;;
        *qlik-verify-addons/services/qlik-verify-backoffice*)
        if [[ $qv_backoffice == "false" ]]; then
            qv_backoffice="true"
        fi
        ;;
        *qlik-verify-addons/themes/qlik_verify*)
        if [[ $qv_theme == "false" ]]; then
            qv_theme="true"
        fi
        ;;
        *qlik-verify-addons/services/qlik-verify-marketplace*)
        if [[ $qv_marketplace == "false" ]]; then
            qv_marketplace="true"
        fi
        ;;
        *qlik-verify-addons/services/qlik-verify-extensions*)
        if [[ $qv_extension == "false" ]]; then
            qv_extension="true"
        fi
        ;;
        *qlik-verify-addons/services/keyless-services*)
        if [[ $keyless == "false" ]]; then
            keyless="true"
        fi
        ;;
        *qlik-verify-addons/services/surepass-services*)
        if [[ $surepass == "false" ]]; then
            surepass="true"
        fi
        ;;
        *qlik-verify-addons/services/pindrop-services*)
        if [[ $pindrop == "false" ]]; then
            pindrop="true"
        fi
        ;;
        *qlik-verify-addons/services/totm-services*)
        if [[ $totm == "false" ]]; then
            totm="true"
        fi
        ;;
        *qlik-verify-addons/services/iproov-services*)
        if [[ $iproov == "false" ]]; then
            iproov="true"
        fi
        ;;
        *keycloak/qlikverify-private/*)
        if [[ $qv_private == "false" ]]; then
            qv_private="true"
        fi
        ;;
        *qlik-verify-addons/services/finema-services*)
        if [[ $finema == "false" ]]; then
            finema="true"
        fi
        ;;
        *qlik-verify-addons/services/twilio-ivr-services*)
        if [[ $twilio == "false" ]]; then
            twilio="true"
        fi
        ;;
#        *qlik-verify-addons/services/ashield-services*)
#        if [[ $ashield == "false" ]]; then
#            ashield="true"
#        fi
#        ;;
       *qlik-verify-addons/services/sekura-services*)
       if [[ $sekura == "false" ]]; then
           sekura="true"
       fi
       ;;
       *qlik-verify-addons/services/uae-pass-services*)
       if [[ $uae_pass == "false" ]]; then
           uae_pass="true"
       fi
       ;;
       *qlik-verify-addons/services/ezto-pass-services*)
       if [[ $ezto_pass == "false" ]]; then
          ezto_pass="true"
       fi
      ;;
      *qlik-verify-addons/services/digilocker-services*)
       if [[ $digilocker == "false" ]]; then
          digilocker="true"
       fi
      ;;
    esac
done



if [[ $common == "true" ]]; then
    pushd keycloak/common
    mvn -Dmaven.test.skip clean install
    mv target/keycloak-common-18.0.2.jar target/org.keycloak.keycloak-common-18.0.2.jar
    cp -fr target/org.keycloak.keycloak-common-18.0.2.jar ../../build_$VER/keycloak/keycloak-18.0.2/lib/lib/main
    popd
fi

if [[ $core == "true" ]]; then
    pushd keycloak/core
    mvn -Dmaven.test.skip clean install
    mv target/keycloak-core-18.0.2.jar target/org.keycloak.keycloak-core-18.0.2.jar
    cp -fr target/org.keycloak.keycloak-core-18.0.2.jar ../../build_$VER/keycloak/keycloak-18.0.2/lib/lib/main
    popd
    echo 'COMPLETED BUILDING CORE'
fi

if [[ $server_spi == "true" ]]; then
    pushd keycloak/server-spi
    mvn -Dmaven.test.skip clean install
    mv target/keycloak-server-spi-18.0.2.jar target/org.keycloak.keycloak-server-spi-18.0.2.jar
    cp -fr target/org.keycloak.keycloak-server-spi-18.0.2.jar ../../build_$VER/keycloak/keycloak-18.0.2/lib/lib/main
    popd
    echo 'COMPLETED BUILDING SERVER-SPI'
fi



if [[ $model_jpa == "true" ]]; then
    pushd keycloak/model/jpa
    mvn -Dmaven.test.skip clean install
    mv target/keycloak-model-jpa-18.0.2.jar target/org.keycloak.keycloak-model-jpa-18.0.2.jar
    cp -fr target/org.keycloak.keycloak-model-jpa-18.0.2.jar ../../../build_$VER/keycloak/keycloak-18.0.2/lib/lib/main
    popd
    echo 'COMPLETED BUILDING MODEL/JPA'
fi


if [[ $model_infinispan == "true" ]]; then
    pushd keycloak/model/infinispan
    mvn -Dmaven.test.skip clean install
     mv target/keycloak-model-infinispan-18.0.2.jar  target/org.keycloak.keycloak-model-infinispan-18.0.2.jar
    cp -fr target/org.keycloak.keycloak-model-infinispan-18.0.2.jar  ../../../build_$VER/keycloak/keycloak-18.0.2/lib/lib/main
    popd
    echo 'COMPLETED BUILDING MODEL/INFINISPAN'
fi

if [[ $server_spi_private == "true" ]]; then
    pushd keycloak/server-spi-private
    mvn -Dmaven.test.skip clean install
    mv target/keycloak-server-spi-private-18.0.2.jar target/org.keycloak.keycloak-server-spi-private-18.0.2.jar
    cp -fr target/org.keycloak.keycloak-server-spi-private-18.0.2.jar ../../build_$VER/keycloak/keycloak-18.0.2/lib/lib/main
    popd
    echo 'COMPLETED BUILDING SERVER-SPI-PRIVATE'
fi

if [[ $services == "true" ]]; then
    pushd keycloak/services
    mvn -Dmaven.test.skip clean install
    mv target/keycloak-services-18.0.2.jar target/org.keycloak.keycloak-services-18.0.2.jar
    yes | cp -fr target/org.keycloak.keycloak-services-18.0.2.jar ../../build_$VER/keycloak/keycloak-18.0.2/lib/lib/main
    popd
    echo 'COMPLETED BUILDING SERVICES'
fi

if [[ $qv_private == "true" ]]; then
    pushd keycloak/qlikverify-private
    mvn -Dmaven.test.skip clean install
    mv target/qlikverify-private-18.0.2.jar target/org.keycloak.qlikverify-private-18.0.2.jar
    yes | cp -fr target/org.keycloak.qlikverify-private-18.0.2.jar ../../build_$VER/keycloak/keycloak-18.0.2/lib/lib/main
    popd
    echo 'COMPLETED BUILDING QLIKVERIFY PRIVATE SPI'
fi

if [[ $common == "true" ]]; then
    pushd keycloak/common
    mvn -Dmaven.test.skip clean install
    mv target/keycloak-common-18.0.2.jar target/org.keycloak.keycloak-common-18.0.2.jar
    cp -fr target/org.keycloak.keycloak-common-18.0.2.jar ../../build_$VER/keycloak/keycloak-18.0.2/lib/lib/main
    popd
fi

if [[ $qv_brand == "true" ]]; then
    pushd qlik-verify-addons/services/qlik-verify-brand
    mvn -Dmaven.test.skip clean install
    cp -fr target/qlik-verify-brand.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    popd
    echo 'COMPLETED BUILDING QLIKVERIFY BRAND'
fi

if [[ $qv_services == "true" ]]; then
    pushd qlik-verify-addons/services/qlik-verify-services
    mvn clean install
    cp -fr target/qlik-verify-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    popd
    echo 'COMPLETED BUILDING QLIKVERIFY SERVICES'
fi

if [[ $qv_backoffice == "true" ]]; then
    pushd qlik-verify-addons/services/qlik-verify-backoffice
    mvn -Dmaven.test.skip clean install
    cp -fr target/qlik-verify-backoffice.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    cp -r "theme/login/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/"
    popd
    echo 'COMPLETED BUILDING QLIKVERIFY BACKOFFICE'
fi

if [[ $qv_theme == "true" ]]; then
    pushd build-tools
    ./qlik-verify-theme.sh ${VER}
    popd
    echo 'COMPLETED QLIKVERIFY THEME'
fi

if [[ $qv_marketplace == "true" ]]; then
    pushd qlik-verify-addons/services/qlik-verify-marketplace
    mvn -Dmaven.test.skip clean install
    cp -fr target/qlik-verify-marketplace.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    popd
    echo 'COMPLETED BUILDING QLIKVERIFY MARKETPLACE'
fi

if [[ $qv_extension == "true" ]]; then
    pushd qlik-verify-addons/services/qlik-verify-extensions
    mvn -Dmaven.test.skip clean install
    cp -fr target/qlik-verify-extensions.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    popd
    echo 'COMPLETED BUILDING QLIKVERIFY MARKETPLACE'
fi

if [[ $keyless == "true" ]]; then
    pushd qlik-verify-addons/services/keyless-services
    mvn -Dmaven.test.skip clean install
    cp -fr target/keyless-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    cp -r "theme/login/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/"
    popd
    echo 'COMPLETED BUILDING KEYLESS'
fi
if [[ $surepass == "true" ]]; then
    pushd qlik-verify-addons/services/surepass-services
    mvn -Dmaven.test.skip clean install
    cp -fr target/surepass-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    popd
    echo 'COMPLETED BUILDING SUREPASS'
fi

if [[ $pindrop == "true" ]]; then
    pushd qlik-verify-addons/services/pindrop-services
    mvn -Dmaven.test.skip clean install
    cp -fr target/pindrop-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    cp -r "theme/login/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/"
    cp -r "theme/js/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/resources/js/"
    cp -r "theme/img/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/resources/img/"
    popd
    echo 'COMPLETED BUILDING PINDROP'
fi

if [[ $totm == "true" ]]; then
    pushd qlik-verify-addons/services/totm-services
    mvn -Dmaven.test.skip clean install
    cp -fr target/totm-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    cp -r "theme/login/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/"
    cp -r "theme/img/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/resources/img/"
    popd
    echo 'COMPLETED BUILDING TOTM'
fi

if [[ $iproov == "true" ]]; then
    pushd qlik-verify-addons/services/iproov-services
    mvn -Dmaven.test.skip clean install
    cp -fr target/iproov-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    cp -r "theme/login/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/"
    cp -r "theme/img/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/resources/img/"
    popd
    echo 'COMPLETED BUILDING IPROOV'
fi

if [[ $twilio == "true" ]]; then
    pushd qlik-verify-addons/services/twilio-ivr-services
    mvn -Dmaven.test.skip clean install
    cp -fr target/twilio-ivr-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
    popd
    echo 'COMPLETED BUILDING TWILIO REALM ATTRIBUTES'
fi

#if [[ $ashield == "true" ]]; then
#    pushd qlik-verify-addons/services/ashield-services
#    mvn -Dmaven.test.skip clean install
#    cp -fr target/ashield-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
#    popd
#    echo 'COMPLETED BUILDING ASHIELD REALM ATTRIBUTES'
# fi

if [[ $sekura == "true" ]]; then
   pushd qlik-verify-addons/services/sekura-services
   mvn -Dmaven.test.skip clean install
   cp -fr target/sekura-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
   popd
   echo 'COMPLETED BUILDING SEKURA SERVICES'
fi

if [[ $uae_pass == "true" ]]; then
   pushd qlik-verify-addons/services/uae-pass-services
   mvn -Dmaven.test.skip clean install
   cp -fr target/uae-pass-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
   cp -r "theme/login/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/"
   popd
   echo 'COMPLETED BUILDING UAE PASS SERVICES'
fi


if [[ $ezto_pass == "true" ]]; then
   pushd qlik-verify-addons/services/ezto-pass-services
   mvn -Dmaven.test.skip clean install
   cp -fr target/ezto-pass-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
   cp -r "theme/login/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/"
   popd
   echo 'COMPLETED BUILDING EZTO PASS SERVICES'
fi

if [[ $digilocker == "true" ]]; then
   pushd qlik-verify-addons/services/digilocker-services
   mvn -Dmaven.test.skip clean install
   cp -fr target/digilocker-services.jar ../../../build_$VER/keycloak/keycloak-18.0.2/providers
   cp -r "theme/login/"* "../../../build_$VER/keycloak/keycloak-18.0.2/themes/qlik_verify/login/"
   popd
   echo 'COMPLETED BUILDING DIGILOCKER SERVICES'
fi

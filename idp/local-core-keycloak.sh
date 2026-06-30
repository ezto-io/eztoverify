if [ $1 == "services" ]; then  
    echo "========================================================================="
    echo "BUILDING KEYCLOAK - SERVICES"
    echo "========================================================================="
    echo ""

    cd keycloak/services
    mvn -Dmaven.test.skip clean install  
    cp -a target/keycloak-services-12.0.2.jar ../../build_*/keycloak/keycloak-*/modules/system/layers/keycloak/org/keycloak/keycloak-services/main

    cd ../../
fi

if [ $1 == "server-spi" ]; then  
    echo "========================================================================="
    echo "BUILDING KEYCLOAK - SERVER-SPI"
    echo "========================================================================="
    echo ""

    cd keycloak/server-spi
    mvn clean install
    cp -a target/keycloak-server-spi-12.0.2.jar ../../build_*/keycloak/keycloak-*/modules/system/layers/keycloak/org/keycloak/keycloak-server-spi/main

    cd ../../
fi

if [ $1 == "server-spi-private" ]; then  
    echo "========================================================================="
    echo "BUILDING KEYCLOAK - SERVER-SPI-PRIVATE"
    echo "========================================================================="
    echo ""

    cd keycloak/server-spi-private
    mvn clean install 
    cp -a target/keycloak-server-spi-private-12.0.2.jar ../../build_*/keycloak/keycloak-*/modules/system/layers/keycloak/org/keycloak/keycloak-server-spi-private/main

    cd ../../
fi

if [ $1 == "jpa" ]; then 
    echo "========================================================================="
    echo "BUILDING KEYCLOAK - MODEL-JPA"
    echo "========================================================================="
    echo ""
    
    cd keycloak/model/jpa
    mvn clean install 
    cp -a target/keycloak-model-jpa-12.0.2.jar ../../build_*/keycloak/keycloak-*/modules/system/layers/keycloak/org/keycloak/keycloak-model-jpa/main

    cd ../../ 
fi

if [ $1 == "infinispan" ]; then  
    echo "========================================================================="
    echo "BUILDING KEYCLOAK - MODEL-INFINISPAN"
    echo "========================================================================="
    echo ""
    
    cd keycloak/model/infinispan
    mvn clean install 
    cp -a target/keycloak-model-infinispan-12.0.2.jar ../../build_*/keycloak/keycloak-*/modules/system/layers/keycloak/org/keycloak/keycloak-model-infinispan/main

    cd ../../
fi

if [ $1 == "core" ]; then  
    echo "========================================================================="
    echo "BUILDING KEYCLOAK - CORE"
    echo "========================================================================="
    echo ""

    cd keycloak/core
    mvn clean install 
    cp -a target/keycloak-core-12.0.2.jar ../../build_*/keycloak/keycloak-*/modules/system/layers/keycloak/org/keycloak/keycloak-core/main

    cd ../../
fi
#!/bin/bash -e

###########################
# Build/download Keycloak #
###########################

pushd /opt/jboss/
tar -zxvf keycloak-$VERSION.tar.gz
rm -rf keycloak-$VERSION.tar.gz
mkdir /opt/jboss/keycloak
mv /opt/jboss/keycloak-$VERSION/* /opt/jboss/keycloak/
rm -rf /opt/jboss/keycloak-$VERSION
popd

#####################
# Create DB modules #
#####################

mkdir -p /opt/jboss/keycloak/modules/system/layers/base/com/mysql/jdbc/main
pushd /opt/jboss/keycloak/modules/system/layers/base/com/mysql/jdbc/main
curl -O https://repo1.maven.org/maven2/mysql/mysql-connector-java/$JDBC_MYSQL_VERSION/mysql-connector-java-$JDBC_MYSQL_VERSION.jar
cp /opt/jboss/tools/databases/mysql/module.xml .
sed "s/JDBC_MYSQL_VERSION/$JDBC_MYSQL_VERSION/" /opt/jboss/tools/databases/mysql/module.xml > module.xml
popd

mkdir -p /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main
pushd /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main
curl -L https://repo1.maven.org/maven2/org/postgresql/postgresql/$JDBC_POSTGRES_VERSION/postgresql-$JDBC_POSTGRES_VERSION.jar > postgres-jdbc.jar
cp /opt/jboss/tools/databases/postgres/module.xml .
popd

mkdir -p /opt/jboss/keycloak/modules/system/layers/base/org/mariadb/jdbc/main
pushd /opt/jboss/keycloak/modules/system/layers/base/org/mariadb/jdbc/main
curl -L https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/$JDBC_MARIADB_VERSION/mariadb-java-client-$JDBC_MARIADB_VERSION.jar > mariadb-jdbc.jar
cp /opt/jboss/tools/databases/mariadb/module.xml .
popd

mkdir -p /opt/jboss/keycloak/modules/system/layers/base/com/oracle/jdbc/main
pushd /opt/jboss/keycloak/modules/system/layers/base/com/oracle/jdbc/main
cp /opt/jboss/tools/databases/oracle/module.xml .
popd

mkdir -p /opt/jboss/keycloak/modules/system/layers/keycloak/com/microsoft/sqlserver/jdbc/main
pushd /opt/jboss/keycloak/modules/system/layers/keycloak/com/microsoft/sqlserver/jdbc/main
curl -L https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/$JDBC_MSSQL_VERSION/mssql-jdbc-$JDBC_MSSQL_VERSION.jar > mssql-jdbc.jar
cp /opt/jboss/tools/databases/mssql/module.xml .
popd

######################
# Configure Keycloak #
######################

pushd /opt/jboss/keycloak

bin/jboss-cli.sh --file=/opt/jboss/tools/cli/standalone-configuration.cli
rm -rf /opt/jboss/keycloak/standalone/configuration/standalone_xml_history

bin/jboss-cli.sh --file=/opt/jboss/tools/cli/standalone-ha-configuration.cli
rm -rf /opt/jboss/keycloak/standalone/configuration/standalone_xml_history

###########
# Garbage #
###########

rm -rf /opt/jboss/keycloak/standalone/tmp/auth
rm -rf /opt/jboss/keycloak/domain/tmp/auth

###################
# Set permissions #
###################

chgrp -R 0 /opt/jboss
chmod -R g=u /opt/jboss

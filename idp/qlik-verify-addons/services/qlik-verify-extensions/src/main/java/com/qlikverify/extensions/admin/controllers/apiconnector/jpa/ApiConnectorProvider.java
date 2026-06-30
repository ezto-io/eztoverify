package com.qlikverify.extensions.admin.controllers.apiconnector.jpa;

import com.qlikverify.extensions.admin.controllers.apiconnector.entity.ApiConnectorConfigEntity;
import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.Collections;
import java.util.List;

public class ApiConnectorProvider implements JpaEntityProvider, JpaEntityProviderFactory {
    protected static final String PROVIDER_FACTORY_ID = "api-connector-entity-provider";
    @Override
    public List<Class<?>> getEntities() {
        return Collections.singletonList(ApiConnectorConfigEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/api-connector-config-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return PROVIDER_FACTORY_ID;
    }

    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_FACTORY_ID;
    }
}

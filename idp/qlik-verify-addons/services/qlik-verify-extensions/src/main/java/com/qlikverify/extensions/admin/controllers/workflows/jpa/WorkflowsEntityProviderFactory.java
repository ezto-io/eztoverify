package com.qlikverify.extensions.admin.controllers.workflows.jpa;

import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class WorkflowsEntityProviderFactory implements JpaEntityProviderFactory {
    protected static final String PROVIDER_FACTORY_ID = "workflows-entity-provider";
    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new WorkflowsEntityProvider();
    }

    @Override
    public void init(Config.Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOP
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return PROVIDER_FACTORY_ID;
    }
}

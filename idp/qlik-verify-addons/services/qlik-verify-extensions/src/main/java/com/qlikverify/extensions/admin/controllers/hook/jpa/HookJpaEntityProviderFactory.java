
package com.qlikverify.extensions.admin.controllers.hook.jpa;

import org.keycloak.Config.Scope;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class HookJpaEntityProviderFactory implements JpaEntityProviderFactory {

	protected static final String ID = "hook-entity-provider";
	
    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new HookJpaEntityProvider();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

}

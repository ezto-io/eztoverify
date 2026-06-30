package com.qlikverify.extensions.admin.controllers.ssodashboard.jpa;

import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class SSODashboardEntityProviderFactory implements JpaEntityProviderFactory {

    //~ Static fields/initializers =========================================================================================================

    protected static final String PROVIDER_FACTORY_ID = "sso-dashboard-entity-provider";

    //~ Methods ============================================================================================================================

    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new SSODashboardEntityProvider();
    }

    //~ ====================================================================================================================================

    @Override
    public void init(Config.Scope config) {
        // No implementation required
    }

    //~ ====================================================================================================================================

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No implementation required
    }

    //~ ====================================================================================================================================

    @Override
    public String getId() {
        return PROVIDER_FACTORY_ID;
    }

    //~ ====================================================================================================================================

    @Override
    public void close() {
        // No implementation required
    }
}

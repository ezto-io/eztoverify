package com.qlikverify.extensions.admin.controllers.ssodashboard.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class SSODashboardProviderFactory implements RealmResourceProviderFactory {

    //~ Static fields/initializers =========================================================================================================

    public static final String API_PROVIDER_ID = "sso_dashboard";

    //~ Methods ============================================================================================================================

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new SSODashboardResource(session);
    }

    //~ ====================================================================================================================================

    @Override
    public String getId() {
        return API_PROVIDER_ID;
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
    public void close() {
        // No implementation required
    }
}

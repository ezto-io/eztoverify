package com.qlikverify.backoffice.extensions;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProviderFactory;

public class APIConnectorSyncProviderFactory implements EztoPublicRealmResourceProviderFactory {
    @Override
    public EztoPublicRealmResourceProvider create(KeycloakSession session) {
        return new APIConnectorSyncProvider(session);
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
        return "api-connector";
    }
}

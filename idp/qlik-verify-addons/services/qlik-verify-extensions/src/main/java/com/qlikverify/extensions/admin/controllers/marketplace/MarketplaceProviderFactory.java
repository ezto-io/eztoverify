package com.qlikverify.extensions.admin.controllers.marketplace;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProviderFactory;

public class MarketplaceProviderFactory implements EztoPublicRealmResourceProviderFactory {
    public static final String ID = "qv_marketplace";

    @Override
    public EztoPublicRealmResourceProvider create(KeycloakSession session) {
        return new MarketplaceProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
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

    @Override
    public String getId() {
        return ID;
    }
}

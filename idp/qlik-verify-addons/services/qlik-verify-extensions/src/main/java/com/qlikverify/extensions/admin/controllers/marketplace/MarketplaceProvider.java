package com.qlikverify.extensions.admin.controllers.marketplace;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class MarketplaceProvider implements EztoPublicRealmResourceProvider {
    private final KeycloakSession session;

    public MarketplaceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new MarketplaceAuthCheckResource(session);
    }

    protected RealmModel realm;

    @Override
    public void close() {
        // NOP
    }
}

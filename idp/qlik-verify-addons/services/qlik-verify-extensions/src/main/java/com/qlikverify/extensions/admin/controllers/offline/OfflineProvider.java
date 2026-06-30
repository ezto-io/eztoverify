package com.qlikverify.extensions.admin.controllers.offline;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class OfflineProvider implements EztoPublicRealmResourceProvider {

    private final KeycloakSession session;

    public OfflineProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new OfflineAuthCheckResource(session);
    }

    @Override
    public void close() {
        // NOP
    }
}

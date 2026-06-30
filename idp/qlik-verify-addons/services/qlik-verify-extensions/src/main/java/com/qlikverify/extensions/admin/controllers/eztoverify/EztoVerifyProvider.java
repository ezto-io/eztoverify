package com.qlikverify.extensions.admin.controllers.eztoverify;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class EztoVerifyProvider implements EztoPublicRealmResourceProvider {

    private final KeycloakSession session;

    public EztoVerifyProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new EztoVerifyResource(session);
    }

    @Override
    public void close() {
        // NOP
    }
}

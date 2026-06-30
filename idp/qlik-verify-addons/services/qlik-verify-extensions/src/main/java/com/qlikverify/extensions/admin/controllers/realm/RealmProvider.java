package com.qlikverify.extensions.admin.controllers.realm;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;


public class RealmProvider implements EztoPublicRealmResourceProvider {

    private final KeycloakSession session;

    public RealmProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new RealmAuthCheckResource(session);
    }

    protected RealmModel realm;

    @Override
    public void close() {
        // NOP
    }
}

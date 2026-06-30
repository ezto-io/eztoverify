package com.qlikverify.extensions.admin.controllers.webPush;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class WebPushProvider implements EztoPublicRealmResourceProvider {
    private final KeycloakSession session;

    public WebPushProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new WebPushAuthCheckResource(session);
    }

    protected RealmModel realm;

    @Override
    public void close() {

    }
}

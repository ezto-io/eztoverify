package com.qlikverify.extensions.admin.controllers.notification;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class NotificationProvider implements EztoPublicRealmResourceProvider {

    private final KeycloakSession session;

    public NotificationProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new NotificationAuthCheckResource(session);
    }

    @Override
    public void close() {
        // NOP
    }
}

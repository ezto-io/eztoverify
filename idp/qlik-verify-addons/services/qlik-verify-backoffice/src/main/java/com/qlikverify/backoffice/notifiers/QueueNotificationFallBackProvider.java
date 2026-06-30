package com.qlikverify.backoffice.notifiers;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class QueueNotificationFallBackProvider implements RealmResourceProvider {
    private final KeycloakSession session;
    public QueueNotificationFallBackProvider(KeycloakSession session) {
        this.session = session;
    }
    @Override
    public Object getResource() {
        return new HookNotificationFallBackAuthCheckResource(session);
    }

    @Override
    public void close() {
        //NOP
    }
}

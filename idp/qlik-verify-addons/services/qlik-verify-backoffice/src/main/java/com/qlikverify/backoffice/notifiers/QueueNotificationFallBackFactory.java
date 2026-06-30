package com.qlikverify.backoffice.notifiers;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class QueueNotificationFallBackFactory implements RealmResourceProviderFactory {
    public static final String ID = "queue-fallback-notification";
    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new QueueNotificationFallBackProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOP
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return ID;
    }
}

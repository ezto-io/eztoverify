package com.qlikverify.backoffice.notifiers;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProviderFactory;

public class HookNotificationFallBackFactory implements EztoPublicRealmResourceProviderFactory {
    public static final String ID = "hook-fallback-notification";
    @Override
    public EztoPublicRealmResourceProvider create(KeycloakSession session) {
        return new HookNotificationFallBackProvider(session);
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

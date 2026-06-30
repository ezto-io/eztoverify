package com.qlikverify.backoffice.notifiers;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class HookNotificationFallBackProvider implements EztoPublicRealmResourceProvider {

    private final KeycloakSession session;

    public HookNotificationFallBackProvider(KeycloakSession session) {
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

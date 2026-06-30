package com.qlikverify.extensions.admin.controllers.hook;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class HookExtensionProvider implements EztoPublicRealmResourceProvider {

    private final KeycloakSession session;

    public HookExtensionProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new HookAuthCheckResource(session);
    }

    @Override
    public void close() {
        // NOP
    }
}

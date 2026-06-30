package com.qlikverify.extensions.admin.controllers.extension;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class BPluginExtensionProvider implements EztoPublicRealmResourceProvider {

    private final KeycloakSession session;

    public BPluginExtensionProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new BPluginExtensionAuthCheckResource(session);
    }

    @Override
    public void close() {
        // NOP
    }
}

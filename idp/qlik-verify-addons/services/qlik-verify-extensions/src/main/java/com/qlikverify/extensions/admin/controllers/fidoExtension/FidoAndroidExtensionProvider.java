package com.qlikverify.extensions.admin.controllers.fidoExtension;

import org.keycloak.models.KeycloakSession;
import org.keycloak.wellknown.WellKnownProvider;

import java.util.HashMap;

public class FidoAndroidExtensionProvider implements WellKnownProvider {
    private final KeycloakSession session;

    public FidoAndroidExtensionProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public Object getConfig() {
        return session.getContext().getRealm().getAttribute("fido-asset-links-android", new HashMap<>());
    }
}
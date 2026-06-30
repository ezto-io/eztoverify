package com.qlikverify.extensions.admin.controllers.fidoExtension;

import liquibase.pro.packaged.S;
import org.keycloak.models.KeycloakSession;
import org.keycloak.wellknown.WellKnownProvider;

import java.util.HashMap;

public class FidoIosExtensionProvider implements WellKnownProvider {

    private final KeycloakSession session;

    public FidoIosExtensionProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public Object getConfig() {
        return session.getContext().getRealm().getAttribute("fido-asset-links-ios", new HashMap<>());
    }
}

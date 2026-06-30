package com.qlikverify.extensions.admin.controllers.fidoExtension;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

public class FidoIosExtensionProviderFactory implements WellKnownProviderFactory {

    public static final String ID = "fido-ios";
    @Override
    public WellKnownProvider create(KeycloakSession session) {
        return new FidoIosExtensionProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return ID;
    }
}

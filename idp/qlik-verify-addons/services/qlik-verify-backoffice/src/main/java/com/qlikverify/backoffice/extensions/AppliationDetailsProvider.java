package com.qlikverify.backoffice.extensions;

import com.qlikverify.backoffice.controllers.ApplicationDetailsResource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class AppliationDetailsProvider implements EztoPublicRealmResourceProvider {
    private final KeycloakSession session;
    public AppliationDetailsProvider(KeycloakSession session) {
        this.session = session;
    }
    @Override
    public Object getResource() {
        return new ApplicationDetailsResource(session);
    }

    @Override
    public void close() {
        // NOP
    }
}

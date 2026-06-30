package com.qlikverify.backoffice.extensions;

import com.qlikverify.backoffice.controllers.DeepLinkingResource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class DeepLinkingProvider implements EztoPublicRealmResourceProvider {
    private final KeycloakSession session;
    public DeepLinkingProvider(KeycloakSession session) {
        this.session = session;
    }
    @Override
    public Object getResource() {
        return new DeepLinkingResource(session);
    }

    @Override
    public void close() {
        // NOP
    }
}

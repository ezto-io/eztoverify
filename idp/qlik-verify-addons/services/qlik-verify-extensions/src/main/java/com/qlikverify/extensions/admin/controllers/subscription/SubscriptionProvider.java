package com.qlikverify.extensions.admin.controllers.subscription;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProviderFactory;

public class SubscriptionProvider implements EztoPublicRealmResourceProvider {

    private final KeycloakSession session;

    public SubscriptionProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new SubscriptionAuthCheckResource(session);
    }

    @Override
    public void close() {
        // NOP
    }
}

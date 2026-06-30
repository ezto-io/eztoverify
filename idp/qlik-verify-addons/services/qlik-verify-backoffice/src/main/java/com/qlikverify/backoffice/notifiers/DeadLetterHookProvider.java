package com.qlikverify.backoffice.notifiers;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class DeadLetterHookProvider implements EztoPublicRealmResourceProvider {

    private final KeycloakSession session;

    public DeadLetterHookProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource()  {
        return new DeadLetterHookNotificationAuthCheckResource(session);
    }

    @Override
    public void close() {

    }
}

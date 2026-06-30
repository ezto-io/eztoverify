package com.qlikverify.backoffice.extensions;

import com.qlikverify.backoffice.controllers.AddMoneyResource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class AddMoneyProvider implements EztoPublicRealmResourceProvider {
    private final KeycloakSession session;
    public AddMoneyProvider(KeycloakSession session) {
        this.session = session;
    }
    @Override
    public Object getResource() {
        return new AddMoneyResource(session);
    }

    @Override
    public void close() {

    }
}

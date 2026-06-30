package com.qlikverify.backoffice.extensions;

import com.qlikverify.backoffice.apiconnector.controllers.APIConnectorController;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;

public class APIConnectorSyncProvider implements EztoPublicRealmResourceProvider {

    private final KeycloakSession session;

    public APIConnectorSyncProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new APIConnectorController(session);
    }

    @Override
    public void close() {

    }
}

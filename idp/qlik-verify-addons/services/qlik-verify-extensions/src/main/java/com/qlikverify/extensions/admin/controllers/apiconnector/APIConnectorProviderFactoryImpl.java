package com.qlikverify.extensions.admin.controllers.apiconnector;

import com.qlikverify.apiConnector.spi.APIConnectorProvider;
import com.qlikverify.apiConnector.spi.APIConnectorProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class APIConnectorProviderFactoryImpl implements APIConnectorProviderFactory {
    @Override
    public APIConnectorProvider create(KeycloakSession session) {
        return new APIConnectorProviderImpl(session);
    }

    @Override
    public void init(Config.Scope config) {
        //
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //
    }

    @Override
    public void close() {
        //
    }

    @Override
    public String getId() {
        return "APIConnectorProviderFactoryImpl";
    }
}

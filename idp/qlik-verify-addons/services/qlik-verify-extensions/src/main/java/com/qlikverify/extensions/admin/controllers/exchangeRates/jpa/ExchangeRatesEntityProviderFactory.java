package com.qlikverify.extensions.admin.controllers.exchangeRates.jpa;

import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class ExchangeRatesEntityProviderFactory implements JpaEntityProviderFactory {
    protected static final String ID = "exchange-rates-entity-provider";

    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new ExchangeRatesEntityProvider();
    }

    @Override
    public void init(Config.Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOP
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return ID;
    }
}

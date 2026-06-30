package com.qlikverify.extensions.admin.controllers.exchangeRates.spi;


import com.qlikverify.exchangeRates.spi.ExchangeRatesService;
import com.qlikverify.exchangeRates.spi.ExchangeRatesServiceProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class ExchangeRatesServiceProviderFactoryImpl implements ExchangeRatesServiceProviderFactory {
    @Override
    public ExchangeRatesService create(KeycloakSession session) {
        return new ExchangeRatesServiceImpl(session);
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
        return "ExchangeRatesServiceImpl";
    }
}

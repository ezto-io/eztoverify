package com.qlikverify.extensions.admin.controllers.exchangeRates.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

public class ExchangeRatesEntityProvider implements JpaEntityProvider {
    @Override
    public List<Class<?>> getEntities() {
        return Collections.singletonList(ExchangeRatesEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/exchange-rates-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return ExchangeRatesEntityProviderFactory.ID;
    }

    @Override
    public void close() {
        //NOP
    }
}

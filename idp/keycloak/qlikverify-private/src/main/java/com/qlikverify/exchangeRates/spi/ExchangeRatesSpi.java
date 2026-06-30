package com.qlikverify.exchangeRates.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ExchangeRatesSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "ExchangeRates";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ExchangeRatesService.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ExchangeRatesServiceProviderFactory.class;
    }
}

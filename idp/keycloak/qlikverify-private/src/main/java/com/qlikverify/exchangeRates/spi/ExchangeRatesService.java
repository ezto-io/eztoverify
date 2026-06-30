package com.qlikverify.exchangeRates.spi;

import org.keycloak.provider.Provider;

import java.util.Map;
import java.util.Optional;

public interface ExchangeRatesService extends Provider {

    boolean isExchangeRatesAvailable();

    Optional<Double> getRate(String currencyCode);

    void updateRates(Map<String, Double> newRates);
}

package com.qlikverify.transactionState.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class TransactionStateProviderSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "TransactionStateProviderSpi";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return TransactionStateProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return TransactionStateProviderFactory.class;
    }
}

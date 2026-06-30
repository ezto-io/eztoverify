package com.qlikverify.apiConnector.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class APIConnectorSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "APIConnectorSpi";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return APIConnectorProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return APIConnectorProviderFactory.class;
    }
}

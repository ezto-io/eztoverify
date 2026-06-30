package org.keycloak.marketplace;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class MarketplaceSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "marketplace";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return MarketplaceProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return MarketplaceProviderFactory.class;
    }
}

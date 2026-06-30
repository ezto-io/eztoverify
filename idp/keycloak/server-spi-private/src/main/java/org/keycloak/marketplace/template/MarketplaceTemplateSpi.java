package org.keycloak.marketplace.template;

import org.keycloak.marketplace.MarketplaceProvider;
import org.keycloak.marketplace.MarketplaceProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class MarketplaceTemplateSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "marketplacetemplate";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return MarketplaceTemplateProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return MarketplaceTemplateProviderFactory.class;
    }
}

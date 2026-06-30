package org.keycloak.ciba;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class CibaTutorialSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "ciba-tutorial";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return CibaTutorialProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return CibaTutorialProviderFactory.class;
    }
}

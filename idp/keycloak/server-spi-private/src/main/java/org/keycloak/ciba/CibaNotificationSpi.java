package org.keycloak.ciba;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class CibaNotificationSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "ciba-notify";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return CibaNotificationProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return CibaNotificationProviderFactory.class;
    }
}

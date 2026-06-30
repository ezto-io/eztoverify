package org.keycloak.ciba;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class CibaResultHookSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "ciba-result-hook";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return CibaDeadLetterHookNotificationProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return CibaDeadLetterHookNotificationProviderFactory.class;
    }
}

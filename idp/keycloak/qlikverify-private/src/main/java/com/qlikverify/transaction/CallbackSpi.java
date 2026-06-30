package com.qlikverify.transaction;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class CallbackSpi  implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "callback";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return CallBackService.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return CallBackServiceProviderFactory.class;
    }
}

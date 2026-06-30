package org.keycloak.mfa;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class MFASpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "mfa";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return MFAProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return MFAProviderFactory.class;
    }
}

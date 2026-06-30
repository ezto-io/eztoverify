package org.keycloak.branding;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class LoginBrandingSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "login-branding";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return LoginBrandingProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return LoginBrandingProviderFactory.class;
    }
}

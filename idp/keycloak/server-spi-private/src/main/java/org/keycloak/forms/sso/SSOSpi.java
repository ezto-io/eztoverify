package org.keycloak.forms.sso;

import org.keycloak.forms.account.AccountProvider;
import org.keycloak.forms.account.AccountProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class SSOSpi  implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "sso";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return SSOProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return SSOProviderFactory.class;
    }

}

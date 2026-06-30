package org.keycloak.services.resource.ezto;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class EztoPublicRealmResourceSPI implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "ez-realm-restapi-extension";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return EztoPublicRealmResourceProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return EztoPublicRealmResourceProviderFactory.class;
    }
}

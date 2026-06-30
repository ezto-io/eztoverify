package org.keycloak.attributes;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RealmAttributeSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "realm-attribute";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return RealmAttributeProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return RealmAttributeProviderFactory.class;
    }
}

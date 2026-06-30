package org.keycloak.models;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RealmPolicySpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "realmPolicy";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return RealmPolicyProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return RealmPolicyProviderFactory.class;
    }
}

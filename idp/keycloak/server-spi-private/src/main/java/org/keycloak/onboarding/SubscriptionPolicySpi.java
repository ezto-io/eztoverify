package org.keycloak.onboarding;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class SubscriptionPolicySpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "subscription-policy";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return SubscriptionPolicyProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return SubscriptionPolicyProviderFactory.class;
    }
}

package org.keycloak.onboarding;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class OnboardingSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "onboarding";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return OnboardingProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return OnboardingProviderFactory.class;
    }
}

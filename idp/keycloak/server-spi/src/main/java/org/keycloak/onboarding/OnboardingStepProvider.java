package org.keycloak.onboarding;

import org.keycloak.provider.Provider;

public interface OnboardingStepProvider extends Provider {

    int priority();

    String getTitle();

    String getDescription();

    String getProviderId();

    boolean isSkippable();

    default boolean shouldIncludeBasedOnMode(){
        return true;
    }
}

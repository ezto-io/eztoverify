package org.keycloak.onboarding;

import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class OnboardingStep {
    public String title;
    public String description;
    public List<ProviderConfigProperty> properties;
    public String stepProviderId;
    boolean isStepCompleted;
    boolean isLastStep;
    boolean isFirstStep;
    boolean isSkippable;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ProviderConfigProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<ProviderConfigProperty> properties) {
        this.properties = properties;
    }

    public String getStepProviderId() {
        return stepProviderId;
    }

    public void setStepProviderId(String stepProviderId) {
        this.stepProviderId = stepProviderId;
    }

    public boolean isLastStep() {
        return isLastStep;
    }

    public void setLastStep(boolean lastStep) {
        isLastStep = lastStep;
    }

    public boolean isFirstStep() {
        return isFirstStep;
    }

    public void setFirstStep(boolean firstStep) {
        isFirstStep = firstStep;
    }

    public boolean getStepCompleted() {
        return isStepCompleted;
    }

    public void setStepCompleted(boolean stepCompleted) {
        this.isStepCompleted = stepCompleted;
    }

    public boolean isStepCompleted() {
        return isStepCompleted;
    }

    public boolean isSkippable() {
        return isSkippable;
    }

    public void setSkippable(boolean skippable) {
        isSkippable = skippable;
    }
}

package org.keycloak.onboarding;

import java.util.ArrayList;
import java.util.List;

public class OnboardingRepresentation {
    List<OnboardingStep> steps;
    int currentStep;

    public OnboardingRepresentation(){
        steps = new ArrayList<>();
        currentStep = -1;
    }

    public List<OnboardingStep> getSteps() {
        return steps;
    }

    public void setSteps(List<OnboardingStep> steps) {
        this.steps = steps;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }
}

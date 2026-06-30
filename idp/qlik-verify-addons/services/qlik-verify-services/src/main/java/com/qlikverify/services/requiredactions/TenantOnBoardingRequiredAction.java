package com.qlikverify.services.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.onboarding.OnboardingRepresentation;
import org.keycloak.onboarding.OnboardingStep;
import org.keycloak.onboarding.OnboardingStepProvider;
import org.keycloak.onboarding.OnboardingStepProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.services.resources.WelcomeResource;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.services.constants.Constants.CUSTOM_SUBSCRIPTION;


public class TenantOnBoardingRequiredAction implements RequiredActionProvider, RequiredActionFactory {
    private static final Logger logger = Logger.getLogger(TenantOnBoardingRequiredAction.class);

    KeycloakSession session;

    @Override
    public String getDisplayText() {
        return "tenant-onboarding-ra";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.TENANT_ONBOARDING;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public String[] supportedModes() {
        return new String[] {"ciam","wiam","bo"};
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getPolicyId() != null) {
            if ((session.users().getUsersStream(session.getContext().getRealm()).count() == 1) || context.getUser().getFirstAttribute("qv_is_account_creator") != null && context.getUser().getFirstAttribute("qv_is_account_creator").equalsIgnoreCase("true")) {
                boolean result;
                String policyType = context.getPolicyType();
                result = session.onBoarding().isOnboardingCompleted();
                if (!result) {
                    addAction(context);
                } else {
                    context.getUser().removeRequiredAction(getId());
                    context.getUser().setMfaEnabled(true);
                }
            }
        }
    }

    private void addAction(RequiredActionContext context) {
        if (!context.getUser().getRequiredActionsStream().filter(x -> x.equalsIgnoreCase(getId())).findFirst().isPresent()) {
            context.getUser().addRequiredAction(getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        loadWelcomeResource(context, null);
    }

    @Override
    public void processAction(RequiredActionContext context) {
            final MultivaluedMap<String, String> decodedFormParameters =
                context.getHttpRequest().getDecodedFormParameters();
        boolean isSwitch = Boolean.parseBoolean(decodedFormParameters.getFirst("isSwitch"));
        if(isSwitch){
            String steps = session.getContext().getRealm().getAttribute(Constants.ONBOARDING_COMPLETED_STEPS);
            List<String> completedSteps = null;
            List<String> newList = new ArrayList<>();
            List<String> newCompletedList = new ArrayList<>();
            List<OnboardingStepProvider> unchanged = new ArrayList<>();
            List<OnboardingStepProvider> unchangedList = new ArrayList<>();
            String unChangedLastProviderId = "";
            List<String>completedListId = new ArrayList<>();

            Set<OnboardingStepProvider> onboardingStepProvider = session.getAllProviders(OnboardingStepProvider.class);
            List<OnboardingStepProvider> sorted = onboardingStepProvider.stream().sorted(Comparator.comparingInt(OnboardingStepProvider::priority)).collect(Collectors.toList());

            final String lastStepProviderId = sorted.get(sorted.size() - 1).getProviderId();
            final OnboardingStepProviderFactory lastProviderFactory =
                    (OnboardingStepProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(OnboardingStepProvider.class, lastStepProviderId);
            boolean islastStepCompleted = lastProviderFactory.isCompleted(session.getContext().getRealm());


            if(session.getContext().getRealm().getAttribute("obSubscriptionChange") != null && session.getContext().getRealm().getAttribute("obSubscriptionChange").equals("true") && session.getContext().getRealm().getAttribute(Constants.ONBOARDING_COMPLETED_STEPS).contains("ob-admin-security-defenses-setup")){
                completedSteps = Arrays.stream(session.getContext().getRealm().getAttribute(CUSTOM_SUBSCRIPTION).split(",")).filter(x -> !x.isEmpty()).sorted().collect(Collectors.toList());
            }
            else{
                completedSteps = Arrays.stream(steps.split(",")).filter(x -> !x.isEmpty()).collect(Collectors.toList());
            }
            int switchStep = Integer.parseInt(decodedFormParameters.getFirst("switchStep"));

            if(switchStep > completedSteps.size()){
                //skip
                final String stepProviderId =
                        session.onBoarding().welcomeOnboardingSteps(session, null).getSteps().get(switchStep -1 ).stepProviderId;
                completedSteps.add(stepProviderId);
                session.getContext().getRealm().setAttribute(Constants.ONBOARDING_COMPLETED_STEPS, String.join(",", completedSteps));
            } else {
                //back
                if(session.getContext().getRealm().getAttribute(CUSTOM_SUBSCRIPTION) != null){
                    if( Integer.parseInt(decodedFormParameters.getFirst( "switchStep")) != completedSteps.size()-1 || Integer.parseInt(decodedFormParameters.getFirst( "switchStep")) !=onboardingStepProvider.size()-1){
                        switchStep = switchStep + 1;
                    }
                    completedSteps = completedSteps.subList(0, switchStep);

                    completedSteps.forEach(x ->
                        sorted.forEach(y -> {
                            if(x.equals(y.getProviderId())){
                                unchanged.add(y);
                            }
                        })
                    );

                    unchangedList = unchanged.stream().sorted(Comparator.comparingInt(OnboardingStepProvider::priority)).collect(Collectors.toList());
                    String previouStep = unchanged.get(unchanged.size() - 1).getProviderId();
                    unchangedList.remove(unchangedList.size()-1);
                    unchangedList.forEach(x ->
                        completedListId.add(x.getProviderId())
                    );
                    completedSteps = completedListId;


                    newList = Arrays.asList(session.getContext().getRealm().getAttribute(Constants.ONBOARDING_COMPLETED_STEPS).split(","));
                        if(session.getContext().getRealm().getAttribute(Constants.ONBOARDING_COMPLETED_STEPS).contains(previouStep)){
                            newList.forEach(x -> {
                                if( !x.equals(previouStep)){
                                    newCompletedList.add(x);
                                }
                            });
                        }

                        session.getContext().getRealm().setAttribute(Constants.ONBOARDING_COMPLETED_STEPS,String.join(",", newCompletedList));
                }
                else{
                    completedSteps = completedSteps.subList(0, Integer.parseInt(decodedFormParameters.getFirst( "switchStep")));
                }
            }
            if( ( session.getContext().getRealm().getAttribute(CUSTOM_SUBSCRIPTION) == null) && !islastStepCompleted){
                session.getContext().getRealm().setAttribute(Constants.ONBOARDING_COMPLETED_STEPS, String.join(",", completedSteps));
            }
            loadWelcomeResource(context, null);
            return;
        }
        if (session.onBoarding().isOnboardingCompleted()){
            context.success();
        } else{
            OnboardingRepresentation onboardingRepresentation = session.onBoarding().welcomeOnboardingSteps(session,
                    null);
            final OnboardingStep onboardingStep =
                    onboardingRepresentation.getSteps().get(onboardingRepresentation.getCurrentStep());
           final OnboardingStepProviderFactory factory =
                   (OnboardingStepProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(OnboardingStepProvider.class,
                           onboardingStep.getStepProviderId());
            final Response process = factory.process(decodedFormParameters, session.getContext().getRealm());
            String errorMessage = null;
            if(process.getStatus() != 200){
             errorMessage = ((ErrorRepresentation) process.getEntity()).getErrorMessage();
            }
            loadWelcomeResource(context, errorMessage);
        }
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return UserModel.RequiredAction.TENANT_ONBOARDING.name();
    }

    private void loadWelcomeResource(RequiredActionContext context, String errorMessage) {
        WelcomeResource welcomeResource = new WelcomeResource(session);
        Response response = welcomeResource.createWelcomePage(null, errorMessage == null ? null: errorMessage);
        if(response != null) {
            context.challenge(response);
        }
    }
}

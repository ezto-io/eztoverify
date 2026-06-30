package com.qlikverify.services.authenticators.onboarding.email;

import com.qlikverify.services.constants.Constants;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.EmailConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;
import java.util.stream.Collectors;

public class EmailRegistrationAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "email-reg-authenticator";
    private static final EmailRegistrationAuthenticator SINGLETON = new EmailRegistrationAuthenticator();
    private static final String PROVIDER_NAME = "email-onboarding-title";
    private static KeycloakSession session;

    @Override
    public String getDisplayType() {
        return PROVIDER_NAME;
    }

    @Override
    public String getReferenceCategory() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return PROVIDER_NAME;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(Constants.EMAIL_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(Constants.EMAIL_SENDER);
        provider.setLabel(Constants.EMAIL_SENDER_LABEL);
        provider.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);
        Map<String, Object> emailSenderDependsOn = new HashMap<>();
        emailSenderDependsOn.put(Constants.EMAIL_OPTION_CONFIG, new String[]{Constants.OTP, Constants.MAGIC_LINK});
        provider.setDependsOn(emailSenderDependsOn);

        if(session.getContext().getRealm().isEnabledOwnEmailProvider()){
            List<EmailConfigModel> providers = session.getContext().getRealm().getEmailConfigsStream().collect(Collectors.toList());

            List<String> providerList = new ArrayList<>();
            if (!providers.isEmpty()) {
                providers.forEach(x -> providerList.add(x.getEmailProviderId()));
            }
            provider.setRequired(true);
            if (!providers.isEmpty()) {
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(providerList);
                provider.setHelpText("email-sender-hlp-txt");
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("No Email Sender are registered. Click <a href=' #/realms/" + session.getContext().getRealm().getName() + "/email/providers/create'> here </a> to create one");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All Emails are routed through default Email Provider from "+ org.keycloak.Config.getAdminRealm() +".If you want to use your email providers, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
        }

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
        session = keycloakSession;
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope scope) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {

        ProviderConfigProperty property;

        List<String> verificationTypes = new ArrayList<>();
        verificationTypes.add(Constants.OTP);
        verificationTypes.add(Constants.MAGIC_LINK);
        verificationTypes.add(Constants.WEBAUTHN_PASSWORDLESS);
        verificationTypes.add(Constants.USER_INVITE);
        verificationTypes.add(Constants.SKIP);
        property = new ProviderConfigProperty();
        property.setName(Constants.EMAIL_OPTION_CONFIG);
        property.setLabel("email-verification-type-label");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(verificationTypes);
        property.setRequired(true);
        property.setHelpText("email-verification-type-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(Constants.VERIFICATION_EXPIRY_CONFIG);
        property.setLabel("email-exp-label");
        property.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        property.setHelpText("email-exp-hlp-txt");
        Map<String, Object> verificationExpiryConfigDependsOn = new HashMap<>();
        verificationExpiryConfigDependsOn.put(Constants.EMAIL_OPTION_CONFIG,new String[]{ Constants.OTP, Constants.MAGIC_LINK});
        property.setDependsOn(verificationExpiryConfigDependsOn);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(Constants.EMAIL_VER_OTP_LENGTH);
        property.setLabel(Constants.EMAIL_VER_OTP_LENGTH_LABEL);
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("email-verify-otp-hlp-txt");
        Map<String, Object> emailVerOtpLenDependsOn = new HashMap<>();
        emailVerOtpLenDependsOn.put(Constants.EMAIL_OPTION_CONFIG,Constants.OTP);
        property.setDependsOn(emailVerOtpLenDependsOn);
        property.setMinVal(4);
        property.setMaxVal(8);
        property.setDefaultValue(6);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        property.setHelpText("email-onboarding-warn-txt");
        Map<String, Object> emailOnboardingWarnTxtDependsOn = new HashMap<>();
        emailOnboardingWarnTxtDependsOn.put(Constants.EMAIL_OPTION_CONFIG,Constants.WEBAUTHN_PASSWORDLESS);
        property.setDependsOn(emailOnboardingWarnTxtDependsOn);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        property.setHelpText("email-verify-skip-warn-txt");
        Map<String, Object> emailVerSkipWarnTxtDependsOn = new HashMap<>();
        emailVerSkipWarnTxtDependsOn.put(Constants.EMAIL_OPTION_CONFIG,Constants.SKIP);

        property.setDependsOn(emailVerSkipWarnTxtDependsOn);
        configProperties.add(property);
    }

    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.ONBOARDING;
    }

    @Override
    public String getArticleId() {
        return "84000117645";
    }
}

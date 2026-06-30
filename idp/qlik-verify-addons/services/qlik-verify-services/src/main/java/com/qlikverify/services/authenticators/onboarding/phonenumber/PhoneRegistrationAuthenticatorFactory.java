package com.qlikverify.services.authenticators.onboarding.phonenumber;

import com.qlikverify.services.constants.Constants;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.SmsConfigModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PhoneRegistrationAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "phone-registration-authenticator";
    private static final PhoneRegistrationAuthenticator SINGLETON = new PhoneRegistrationAuthenticator();
    private static final String PROVIDER_NAME = "phone-onboarding-title";

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
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(Constants.SMS_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(Constants.SMS_SENDER);
        provider.setLabel(Constants.SMS_SENDER_LABEL);

        if(session.getContext().getRealm().isEnabledOwnSmsProvider()){
            List<SmsConfigModel> providers = session.getContext().getRealm().getSmsConfigsStream().filter(x-> x.getSmsProviderId() !=null && !x.getSmsProviderId().equalsIgnoreCase("whatsapp")).collect(Collectors.toList());
            List<String> providerList = new ArrayList<>();
            if (!providers.isEmpty()) {
                providers.forEach(x -> providerList.add(x.getSmsProviderId()));
            }

            provider.setRequired(true);
            if(!providers.isEmpty()) {
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(providerList);
                provider.setHelpText("sms-sender-hlp-txt");
            }  else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("No SMS Sender are registered. Click <a href=' #/realms/" + session.getContext().getRealm().getName() + "/sms/providers/create'> here </a> to create one");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All SMSes are routed through default SMS Provider from "+ org.keycloak.Config.getAdminRealm() + ".If you want to use your SMS providers, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
        }
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
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
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.VERIFICATION_EXPIRY_CONFIG);
        configProperty.setLabel("mob-exp-label");
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        configProperty.setHelpText("mob-exp-hlp-txt");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.PHONE_VER_OTP_LENGTH);
        configProperty.setLabel(Constants.PHONE_VER_OTP_LENGTH_LABEL);
        configProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        configProperty.setMaxVal(8);
        configProperty.setMinVal(4);
        configProperty.setDefaultValue(6);
        configProperty.setHelpText("phone-verify-otp-hlp-txt");
        configProperty.setMinVal(4);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.PHONE_VER_OTP_TXT);
        configProperty.setLabel("sms-txt-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("sms-txt-hlp-txt");
        configProperties.add(configProperty);
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
        return "84000147276";
    }
}

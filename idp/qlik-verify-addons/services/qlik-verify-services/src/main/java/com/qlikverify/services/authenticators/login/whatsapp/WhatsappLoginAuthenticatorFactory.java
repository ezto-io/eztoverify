package com.qlikverify.services.authenticators.login.whatsapp;

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

import java.util.*;

import static com.qlikverify.services.constants.Constants.WHATSAPP_VERIFY_OPTIONS;

public class WhatsappLoginAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "whatsapp-login-authenticator";
    private static final WhatsappLoginAuthenticator SINGLETON = new WhatsappLoginAuthenticator();
    private static final String PROVIDER_NAME = "whatsapp-login-title";



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
        configProperty.setName(WHATSAPP_VERIFY_OPTIONS);
        configProperty.setLabel("verify-way-label");
        configProperty.setType(ProviderConfigProperty.LIST_TYPE);
        configProperty.setRequired(true);
        List<String> whatsappVerifyOptions = new ArrayList<>();
        whatsappVerifyOptions.add("OTP");
        whatsappVerifyOptions.add("PUSH");
        whatsappVerifyOptions.add("QR");
        configProperty.setOptions(whatsappVerifyOptions);
        configProperty.setHelpText("verify-way-hlp-txt");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(com.qlikverify.services.constants.Constants.VERIFICATION_EXPIRY_CONFIG);
        configProperty.setLabel(com.qlikverify.services.constants.Constants.OTP_VER_EXPIRY_LABEL);
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        configProperty.setRequired(false);
        configProperty.setHelpText("otp-ver-expiry-hlp-txt");
        Map<String, Object> verificationExpiryConfigDependsOn = new HashMap<>();
        verificationExpiryConfigDependsOn.put(WHATSAPP_VERIFY_OPTIONS, "push-config");
        configProperty.setDependsOn(verificationExpiryConfigDependsOn);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(com.qlikverify.services.constants.Constants.PHONE_VER_OTP_LENGTH);
        configProperty.setLabel(com.qlikverify.services.constants.Constants.WHAT_VER_OTP_LENGTH_LABEL);
        configProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        configProperty.setRequired(false);
        configProperty.setHelpText("what-ver-otp-length-hlp-txt");
        Map<String, Object> phoneVerOtpLenDependsOn = new HashMap<>();
        phoneVerOtpLenDependsOn.put(WHATSAPP_VERIFY_OPTIONS, "otp-config");
        configProperty.setDependsOn(phoneVerOtpLenDependsOn);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SESSION_EXPIRY");
        configProperty.setLabel("waiting-duration-rc-label");
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        Map<String, Object> sessionExpiryDependsOn = new HashMap<>();
        sessionExpiryDependsOn.put(WHATSAPP_VERIFY_OPTIONS, "qr-config");
        configProperty.setDependsOn(sessionExpiryDependsOn);
        configProperty.setHelpText("waiting-duration-hlp-txt");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SESSION_EXPIRY_PUSH");
        configProperty.setLabel("waiting-duration-rr-label");
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        Map<String, Object> sessionExpiryPushDependsOn = new HashMap<>();
        sessionExpiryPushDependsOn.put(WHATSAPP_VERIFY_OPTIONS, "push-config");

        configProperty.setDependsOn(sessionExpiryPushDependsOn);
        configProperty.setHelpText("waiting-duration-hlp-txt");
        configProperties.add(configProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        setupSmsSenders(session);
        return configProperties;
    }

    private void setupSmsSenders(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(Constants.WHATSAPP_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(Constants.WHATSAPP_SENDER);
        provider.setLabel(Constants.WHATSAPP_SENDER_LABEL);

        if(session.getContext().getRealm().isEnabledOwnWhatsappProvider()){
            Optional<SmsConfigModel> hasWhatsAppProvider = session.getContext().getRealm().getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();

            if (hasWhatsAppProvider.isPresent()) {
                configProperties.removeIf(x -> x.getName() != null && x.getName().equals(Constants.WHATSAPP_SENDER));
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("No Whatsapp Sender is registered. Click <a href=' #/realms/" + session.getContext().getRealm().getName() + "/sms/providers/create/whatsapp'> here </a> to create one");
                if (!providerConfigProperty.isPresent()) {
                    configProperties.add(provider);
                }
            }
        } else {
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All your Whatsapp messages are are routed through default Whatsapp Provider from "+org.keycloak.Config.getAdminRealm()+".If you want to use your own Whatsapp provider, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
            if (!providerConfigProperty.isPresent()) {
                configProperties.add(provider);
            }
        }
    }


    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.ONBOARDING_LOGIN;
    }

    @Override
    public String getArticleId() {
        return "84000304270";
    }
}

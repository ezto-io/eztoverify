package com.qlikverify.services.authenticators.recovery.resetpassword;

import com.qlikverify.services.constants.Constants;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;
import java.util.stream.Collectors;

public class ResetPasswordAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "reset-password-authenticator";
    private static final ResetPasswordAuthenticator SINGLETON = new ResetPasswordAuthenticator();
    private static final String PROVIDER_NAME = "password-recovery-title";

    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.RECOVERY;
    }

    @Override
    public String getArticleId() {
        return "84000147347";
    }

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


    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.RP_USING_EMAIL_OTP);
        configProperty.setLabel(Constants.RP_USING_EMAIL_OTP_LABEL);
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText(Constants.RP_USING_EMAIL_OTP_HELP_TEXT);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.RP_USING_EMAIL_LINK);
        configProperty.setLabel(Constants.RP_USING_EMAIL_LINK_LABEL);
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText(Constants.RP_USING_EMAIL_LINK_HELP_TEXT);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.RP_USING_MOBILE_NUM);
        configProperty.setLabel(Constants.RP_USING_MOBILE_NUM_LABEL);
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText(Constants.RP_USING_MOBILE_NUM_HELP_TEXT);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.VERIFICATION_EXPIRY_CONFIG);
        configProperty.setLabel(Constants.RP_USING_EMAIL_OR_MOB_EXP_LABEL);
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        configProperty.setHelpText("mob-exp-hlp-txt");
        Map<String, Object> verificationExpiryConfigDependsOn = new HashMap<>();
        verificationExpiryConfigDependsOn.put(Constants.RP_USING_EMAIL_OTP,true);
        verificationExpiryConfigDependsOn.put(Constants.RP_USING_MOBILE_NUM,true);
        configProperty.setDependsOn(verificationExpiryConfigDependsOn);
        configProperties.add(configProperty);

        ProviderConfigProperty emailOtplenConfig = new ProviderConfigProperty();
        emailOtplenConfig.setName(Constants.EMAIL_VER_OTP_LENGTH);
        emailOtplenConfig.setLabel(Constants.EMAIL_VER_OTP_LENGTH_LABEL);
        emailOtplenConfig.setType(ProviderConfigProperty.NUMBER_TYPE);
        emailOtplenConfig.setHelpText("email-verify-otp-hlp-txt");
        Map<String, Object> emailVerOtpLenDependsOn = new HashMap<>();
        emailVerOtpLenDependsOn.put(Constants.RP_USING_EMAIL_OTP,true);
        emailOtplenConfig.setDependsOn(emailVerOtpLenDependsOn);
        emailOtplenConfig.setMinVal(4);
        emailOtplenConfig.setMaxVal(8);
        emailOtplenConfig.setDefaultValue(6);
        configProperties.add(emailOtplenConfig);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.PHONE_VER_OTP_LENGTH);
        configProperty.setLabel(Constants.PHONE_VER_OTP_LENGTH_LABEL);
        configProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        configProperty.setHelpText("phone-verify-otp-hlp-txt");
        Map<String, Object> phoneVerOtpLenDependsOn = new HashMap<>();
        phoneVerOtpLenDependsOn.put(Constants.RP_USING_MOBILE_NUM,true);
        configProperty.setDependsOn(phoneVerOtpLenDependsOn);
        configProperty.setMinVal(4);
        configProperty.setMaxVal(8);
        configProperty.setDefaultValue(6);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.PHONE_VER_OTP_TXT);
        configProperty.setLabel("sms-txt-label");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setHelpText("sms-txt-hlp-txt");
        Map<String, Object> phoneVerOtpDependsOn = new HashMap<>();
        phoneVerOtpDependsOn.put(Constants.RP_USING_MOBILE_NUM,true);
        configProperty.setDependsOn(phoneVerOtpDependsOn);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("passwordPolicy");
        configProperty.setLabel("password-policy-label");
        configProperty.setType(ProviderConfigProperty.POLICY_LIST_TYPE);
        configProperty.setDefaultValue("default_password_policy");
        configProperty.setHelpText("password-policy-hlp-txt");
        configProperties.add(configProperty);
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        setupEmailSenders(session);
        setupSmsSenders(session);
        return configProperties;
    }

    private void setupEmailSenders(KeycloakSession session) {

        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName().equals(Constants.EMAIL_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(Constants.EMAIL_SENDER);
        provider.setLabel(Constants.EMAIL_SENDER_LABEL);
        Map<String, Object> emailSenderDependsOn = new HashMap<>();
        emailSenderDependsOn.put(Constants.RP_USING_EMAIL_OTP, true);
        emailSenderDependsOn.put(Constants.RP_USING_EMAIL_LINK, true);
        provider.setDependsOn(emailSenderDependsOn);
        provider.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);

        if(session.getContext().getRealm().isEnabledOwnEmailProvider()){
            List<EmailConfigModel> providers = session.getContext().getRealm().getEmailConfigsStream().collect(Collectors.toList());
            List<String> providerList = new ArrayList<>();
            if (!providers.isEmpty()) {
                providers.forEach(x -> providerList.add(x.getEmailProviderId()));
            }

            provider.setRequired(true);
            if(!providers.isEmpty()) {
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(providerList);
                provider.setHelpText("email-sender-hlp-txt");
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("No Email Sender are registered. Click <a href=' #/realms/"+session.getContext().getRealm().getName()+"/email/providers/create'> here </a> to create one");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All Emails are routed through default Email Provider from "+org.keycloak.Config.getAdminRealm()+".If you want to use your email providers, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
        }
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    private void setupSmsSenders(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(Constants.SMS_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(Constants.SMS_SENDER);
        provider.setLabel(Constants.SMS_SENDER_LABEL);
        Map<String, Object> smsSenderDependsOn = new HashMap<>();
        smsSenderDependsOn.put(Constants.RP_USING_MOBILE_NUM,true);
        provider.setDependsOn(smsSenderDependsOn);

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
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("No SMS Sender are registered. Click <a href=' #/realms/"+session.getContext().getRealm().getName()+"/sms/providers/create'> here </a> to create one");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All SMSes are routed through default SMS Provider from "+org.keycloak.Config.getAdminRealm()+".If you want to use your SMS providers, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
        }
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
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
        return PROVIDER_ID;
    }
}

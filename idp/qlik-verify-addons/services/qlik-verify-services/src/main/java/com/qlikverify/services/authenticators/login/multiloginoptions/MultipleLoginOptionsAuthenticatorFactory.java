package com.qlikverify.services.authenticators.login.multiloginoptions;

import com.qlikverify.services.constants.Constants;
import org.apache.kafka.common.protocol.types.Field;
import org.bouncycastle.i18n.MessageBundle;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;
import org.keycloak.theme.beans.MessageBean;

import java.util.*;
import java.util.stream.Collectors;

public class MultipleLoginOptionsAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "multiple-login-options-authenticator";
    private static final MultipleLoginOptionsAuthenticator SINGLETON = new MultipleLoginOptionsAuthenticator();
    private static final String PROVIDER_NAME = "multiple-login-options-title";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
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
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {

        setupSmsSenders(session);
        setupEmailSenders(session);

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
        emailSenderDependsOn.put(Constants.EMAIL_OTP, true);
        emailSenderDependsOn.put(Constants.EMAIL_ML, true);
        emailSenderDependsOn.put(Constants.EMAIL_PHONE_OTP, true);
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
        smsSenderDependsOn.put(Constants.MOB_OTP,true);
        smsSenderDependsOn.put(Constants.EMAIL_PHONE_OTP,true);
        provider.setDependsOn(smsSenderDependsOn);
        provider.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);

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
                provider.setOptions(new ArrayList<>());
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("No SMS Sender are registered. Click <a href=' #/realms/"+session.getContext().getRealm().getName()+"/sms/providers/create'> here </a> to create one");
            }
        } else {
            provider.setRequired(false);
            provider.setOptions(new ArrayList<>());
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All SMS are routed through default SMS Provider from "+org.keycloak.Config.getAdminRealm()+".If you want to use your SMS providers, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
        }

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public String getHelpText() {
        return PROVIDER_NAME;
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

    static {

        // for a dependant boolean vale, the name has to start with dependant_ ...
        ProviderConfigProperty emailOtpConfig = new ProviderConfigProperty();
        emailOtpConfig.setName(Constants.EMAIL_OTP);
        emailOtpConfig.setLabel(Constants.EMAIL_OTP_LABEL);
        emailOtpConfig.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        emailOtpConfig.setHelpText("use-email-otp-hlp-txt");
        configProperties.add(emailOtpConfig);

        ProviderConfigProperty magiclinkConfig = new ProviderConfigProperty();
        magiclinkConfig.setName(Constants.EMAIL_ML);
        magiclinkConfig.setLabel(Constants.EMAIL_ML_LABEL);
        magiclinkConfig.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        magiclinkConfig.setHelpText("use-email-ml-hlp-txt");
        configProperties.add(magiclinkConfig);

        ProviderConfigProperty mobileOtp = new ProviderConfigProperty();
        mobileOtp.setName(Constants.MOB_OTP);
        mobileOtp.setLabel(Constants.MOB_OTP_LABEL);
        mobileOtp.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        mobileOtp.setHelpText("use-phone-otp-hlp-txt");
        configProperties.add(mobileOtp);


        ProviderConfigProperty emailMobileOtpConfig = new ProviderConfigProperty();
        emailMobileOtpConfig.setName(Constants.EMAIL_PHONE_OTP);
        emailMobileOtpConfig.setLabel(Constants.EMAIL_PHONE_OTP_LABEL);
        emailMobileOtpConfig.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        emailMobileOtpConfig.setHelpText(Constants.EMAIL_PHONE_OTP_LOGIN);
        configProperties.add(emailMobileOtpConfig);

        ProviderConfigProperty passwordConfig = new ProviderConfigProperty();
        passwordConfig.setName(Constants.PASSWORD);
        passwordConfig.setLabel(Constants.PASSWORD_LABEL);
        passwordConfig.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        passwordConfig.setHelpText("use-password-hlp-txt");
        configProperties.add(passwordConfig);

        ProviderConfigProperty emailSmsConfig = new ProviderConfigProperty();
        emailSmsConfig.setName("dependant_sameOtp");
        emailSmsConfig.setLabel(Constants.SAME_OTP_EMAIL_PHONE);
        emailSmsConfig.setHelpText(Constants.ENABLE_SAME_OTP_EMAIL_PHONE);
        emailSmsConfig.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        Map<String, Object> sameOtpEmailPoneDependsOn = new HashMap<>();
        sameOtpEmailPoneDependsOn.put(Constants.EMAIL_PHONE_OTP,true);
        emailSmsConfig.setDependsOn(sameOtpEmailPoneDependsOn);
        configProperties.add(emailSmsConfig);

        ProviderConfigProperty emailExpiryTime = new ProviderConfigProperty();
        emailExpiryTime.setName(Constants.EMAIL_VER_EXPIRY_CONFIG);
        emailExpiryTime.setLabel(Constants.EMAIL_EXP_LABEL);
        emailExpiryTime.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        emailExpiryTime.setHelpText("email-exp-hlp-txt");
        Map<String, Object> emailVerExpiryConfigDependsOn = new HashMap<>();
        emailVerExpiryConfigDependsOn.put(Constants.EMAIL_OTP,true);
        emailVerExpiryConfigDependsOn.put(Constants.EMAIL_ML,true);
        emailExpiryTime.setDependsOn(emailVerExpiryConfigDependsOn);
        emailExpiryTime.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);
        configProperties.add(emailExpiryTime);

        ProviderConfigProperty mobExpiryTime = new ProviderConfigProperty();
        mobExpiryTime.setName(Constants.MOB_VER_EXPIRY_CONFIG);
        mobExpiryTime.setLabel(Constants.MOB_EXP_LABEL);
        mobExpiryTime.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        mobExpiryTime.setHelpText("mob-exp-hlp-txt");
        Map<String, Object> mobVerExpConfigDependsOn = new HashMap<>();
        mobVerExpConfigDependsOn.put(Constants.MOB_OTP,true);
        mobExpiryTime.setDependsOn(mobVerExpConfigDependsOn);
        mobExpiryTime.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);
        configProperties.add(mobExpiryTime);


        ProviderConfigProperty emailOtplenConfig = new ProviderConfigProperty();
        emailOtplenConfig.setName(Constants.EMAIL_VER_OTP_LENGTH);
        emailOtplenConfig.setLabel(Constants.EMAIL_VER_OTP_LENGTH_LABEL);
        emailOtplenConfig.setType(ProviderConfigProperty.NUMBER_TYPE);
        emailOtplenConfig.setHelpText("email-verify-otp-hlp-txt");
        Map<String, Object> emailVerOtpLenDependsOn = new HashMap<>();
        emailVerOtpLenDependsOn.put(Constants.EMAIL_OTP,true);
        emailOtplenConfig.setDependsOn(emailVerOtpLenDependsOn);
        emailOtplenConfig.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);
        emailOtplenConfig.setDefaultValue(6);
        emailOtplenConfig.setMaxVal(8);
        emailOtplenConfig.setMinVal(4);
        configProperties.add(emailOtplenConfig);

        ProviderConfigProperty smsOtplenConfig = new ProviderConfigProperty();
        smsOtplenConfig.setName(Constants.PHONE_VER_OTP_LENGTH);
        smsOtplenConfig.setLabel(Constants.PHONE_VER_OTP_LENGTH_LABEL);
        smsOtplenConfig.setType(ProviderConfigProperty.NUMBER_TYPE);
        smsOtplenConfig.setHelpText("phone-verify-otp-hlp-txt");
        smsOtplenConfig.setMaxVal(8);
        smsOtplenConfig.setMinVal(4);
        smsOtplenConfig.setDefaultValue(6);
        Map<String, Object> phoneVerOtpLenDependsOn = new HashMap<>();
        phoneVerOtpLenDependsOn.put(Constants.MOB_OTP,true);
        smsOtplenConfig.setDependsOn(phoneVerOtpLenDependsOn);
        smsOtplenConfig.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);
        configProperties.add(smsOtplenConfig);

        ProviderConfigProperty smstxtConfig = new ProviderConfigProperty();
        smstxtConfig.setName(Constants.PHONE_VER_OTP_TXT);
        smstxtConfig.setLabel("sms-txt-label");
        smstxtConfig.setType(ProviderConfigProperty.STRING_TYPE);
        smstxtConfig.setHelpText("sms-txt-hlp-txt");
        Map<String, Object> phoneVerOtpDependsOn = new HashMap<>();
        phoneVerOtpDependsOn.put(Constants.MOB_OTP,true);
        smstxtConfig.setDependsOn(phoneVerOtpDependsOn);
        smstxtConfig.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);
        configProperties.add(smstxtConfig);

        ProviderConfigProperty emailMobOtpLenConfig = new ProviderConfigProperty();
        emailMobOtpLenConfig.setName(Constants.EMAIL_PHONE_OTP_LEN);
        emailMobOtpLenConfig.setLabel(Constants.OTP_LENGTH);
        emailMobOtpLenConfig.setType(ProviderConfigProperty.NUMBER_TYPE);
        emailMobOtpLenConfig.setHelpText("email-verify-otp-hlp-txt");
        Map<String, Object> emailPhoneOtpLenDependsOn = new HashMap<>();
        emailPhoneOtpLenDependsOn.put(Constants.EMAIL_PHONE_OTP,true);
        emailMobOtpLenConfig.setDependsOn(emailPhoneOtpLenDependsOn);
        emailMobOtpLenConfig.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);
        emailMobOtpLenConfig.setMinVal(4);
        emailMobOtpLenConfig.setMaxVal(8);
        emailMobOtpLenConfig.setDefaultValue(6);
        configProperties.add(emailMobOtpLenConfig);

        ProviderConfigProperty emailMobExpiryTime = new ProviderConfigProperty();
        emailMobExpiryTime.setName(Constants.EMAIL_PHONE_EXP_TIME);
        emailMobExpiryTime.setLabel(Constants.OTP_EXPIRY);
        emailMobExpiryTime.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        emailMobExpiryTime.setHelpText("email-exp-hlp-txt");
        Map<String, Object> emailPhoneExpTimeDependsOn = new HashMap<>();
        emailPhoneExpTimeDependsOn.put(Constants.EMAIL_PHONE_OTP,true);
        emailMobExpiryTime.setDependsOn(emailPhoneExpTimeDependsOn);
        emailMobExpiryTime.setValidationType(ProviderConfigProperty.VALIDATION_ANY_ONE_TRUE);
        configProperties.add(emailMobExpiryTime);

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
        return "84000147339";
    }
}

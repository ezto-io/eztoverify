package com.qlikverify.services.mfa;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.helpers.GeneralHelper;
import com.qlikverify.services.mfa.whatsapp.WhatsappOTP;
import com.qlikverify.services.mfa.whatsapp.WhatsappPush;
import com.qlikverify.services.mfa.whatsapp.WhatsappQR;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.MFAProviderFactory;
import org.keycloak.mfa.recovery.MFARecovery;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;

import static com.qlikverify.services.constants.Constants.WHATAPP_MFA_HELP_TEXT;
import static com.qlikverify.services.constants.Constants.WHATSAPP_VERIFY_OPTIONS;

public class WhatsappMFAProvider extends MFARecovery implements MFAProvider, MFAProviderFactory {

    private WhatsappQR whatsappQR;
    private WhatsappOTP whatsappOTP;
    private WhatsappPush whatsappPush;

    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        String verifyWay = GeneralHelper.getConfigValue(configModel, WHATSAPP_VERIFY_OPTIONS);
        if (verifyWay.equals("OTP")) {
            getWhatsappOTP().onPreLogin(configModel, context);
        } else if (verifyWay.equals("PUSH")) {
            getWhatsappPush().onPreLogin(configModel, context);
        } else if (verifyWay.equals("QR")) {
            getWhatsappQR().onPreLogin(configModel, context);
        }
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel, RequiredActionContext context) {
        String verifyWay = GeneralHelper.getConfigValue(configModel, WHATSAPP_VERIFY_OPTIONS);
        if (verifyWay.equals("OTP")) {
            return getWhatsappOTP().onPostLogin(configModel, context);
        } else if (verifyWay.equals("PUSH")) {
            return getWhatsappPush().onPostLogin(configModel, context);
        } else if (verifyWay.equals("QR")) {
            return getWhatsappQR().onPostLogin(configModel, context);
        }
        return false;
    }

    @Override
    public boolean onPostRegistration(MFAConfigModel configModel, RequiredActionContext context) {
        String verifyWay = GeneralHelper.getConfigValue(configModel, WHATSAPP_VERIFY_OPTIONS);
        if (verifyWay.equals("OTP")) {
            return getWhatsappOTP().onPostRegistration(configModel, context);
        } else if (verifyWay.equals("PUSH")) {
            return getWhatsappPush().onPostRegistration(configModel, context);
        } else if (verifyWay.equals("QR")) {
            return getWhatsappQR().onPostRegistration(configModel, context);
        }
        return false;
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel, RequiredActionContext context, boolean canSkip) {
        String verifyWay = GeneralHelper.getConfigValue(configModel, WHATSAPP_VERIFY_OPTIONS);
        if (verifyWay.equals("OTP")) {
            getWhatsappOTP().onPreRegistration(configModel, context, canSkip);
        } else if (verifyWay.equals("PUSH")) {
            getWhatsappPush().onPreRegistration(configModel, context, canSkip);
        } else if (verifyWay.equals("QR")) {
            getWhatsappQR().onPreRegistration(configModel, context, canSkip);
        }
    }

    @Override
    public String getDisplayName() {
        return "Whatsapp";
    }

    @Override
    public String getProviderClass() {
        return (MFAProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "whatsapp.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/MFA/Whatsapp-Factor.md";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config) {
        List<String> validations = GeneralHelper.validateMFAConfig(getConfigProperties(session), config);

        List<String> errors = new ArrayList<>();
        Optional<SmsConfigModel> hasWhatsAppProvider = session.getContext().getRealm().getSmsConfigsStream().filter(x -> x.getSmsProviderId() != null && x.getSmsProviderId().equalsIgnoreCase("whatsapp")).findFirst();
        if(session.getContext().getRealm().isEnabledOwnWhatsappProvider() && !hasWhatsAppProvider.isPresent()) {
            errors.add("mfa-whatsapp-helptext");
        }

        if(validations != null && !validations.isEmpty()){
            errors.addAll(validations);
        }

        return errors;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName(WHATSAPP_VERIFY_OPTIONS);
        configProperty.setLabel("mfa-whatsapp-verify-way");
        configProperty.setType(ProviderConfigProperty.LIST_TYPE);
        configProperty.setRequired(true);
        List<String> whatsappVerifyOptions = new ArrayList<>();
        whatsappVerifyOptions.add("OTP");
        whatsappVerifyOptions.add("PUSH");
        whatsappVerifyOptions.add("QR");
        configProperty.setOptions(whatsappVerifyOptions);
        configProperty.setHelpText("mfa-whatsapp-verify-way-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(com.qlikverify.services.constants.Constants.VERIFICATION_EXPIRY_CONFIG);
        configProperty.setLabel(com.qlikverify.services.constants.Constants.OTP_VER_EXPIRY_LABEL);
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        configProperty.setHelpText("mfa-whatsapp-otp-expiry-helptext");
        Map<String, Object> verificationExpiryConfigDependsOn = new HashMap<>();
        verificationExpiryConfigDependsOn.put(WHATSAPP_VERIFY_OPTIONS, "OTP");
        configProperty.setDependsOn(verificationExpiryConfigDependsOn);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(com.qlikverify.services.constants.Constants.PHONE_VER_OTP_LENGTH);
        configProperty.setLabel(com.qlikverify.services.constants.Constants.WHAT_VER_OTP_LENGTH_LABEL);
        configProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        configProperty.setHelpText("mfa-whatsapp-verify-otp-len-helptext");
        configProperty.setMinVal(4);
        configProperty.setMaxVal(8);
        configProperty.setDefaultValue(6);
        Map<String, Object> phoneVerOtpLenDependsOn = new HashMap<>();
        phoneVerOtpLenDependsOn.put(WHATSAPP_VERIFY_OPTIONS, "OTP");
        configProperty.setDependsOn(phoneVerOtpLenDependsOn);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SESSION_EXPIRY");
        configProperty.setLabel("mfa-whatsapp-waiting-duration-for-code");
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        Map<String, Object> sessionExpiryDependsOn = new HashMap<>();
        sessionExpiryDependsOn.put(WHATSAPP_VERIFY_OPTIONS, "QR");
        configProperty.setDependsOn(sessionExpiryDependsOn);
        configProperty.setHelpText("mfa-whatsapp-waiting-duration-for-code-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SESSION_EXPIRY_PUSH");
        configProperty.setLabel("mfa-whatsapp-waiting-duration-for-response");
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        Map<String, Object> sessionExpiryPushDependsOn = new HashMap<>();
        sessionExpiryPushDependsOn.put(WHATSAPP_VERIFY_OPTIONS, "PUSH");
        configProperty.setDependsOn(sessionExpiryPushDependsOn);
        configProperty.setHelpText("mfa-whatsapp-waiting-duration-for-response-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("enable_mfa_recovery");
        configProperty.setLabel("mfa-whatsapp-recovery");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText("mfa-whatsapp-recovery-helptext");
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
            provider.setHelpText("All your Whatsapp messages are routed through default Whatsapp Provider from "+ org.keycloak.Config.getAdminRealm() +".If you want to use your own Whatsapp provider, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
            if (!providerConfigProperty.isPresent()) {
                configProperties.add(provider);
            }
        }
    }

    @Override
    public OTPPolicy getOtpPolicy(RealmModel realmModel) {
        return null;
    }

    @Override
    public String getHelpText() {
        return WHATAPP_MFA_HELP_TEXT;
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public MFAProvider create(KeycloakSession session) {
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
        return "whatsapp-mfa";
    }

    private WhatsappQR getWhatsappQR() {
        if (whatsappQR == null) {
            whatsappQR = new WhatsappQR(getId());
        }
        return whatsappQR;
    }

    public WhatsappOTP getWhatsappOTP() {
        if (whatsappOTP == null) {
            whatsappOTP = new WhatsappOTP(getId());
        }
        return whatsappOTP;
    }

    public WhatsappPush getWhatsappPush() {
        if (whatsappPush == null) {
            whatsappPush = new WhatsappPush(getId());
        }
        return whatsappPush;
    }

}

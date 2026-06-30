package com.qlikverify.services.mfa;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.MFAProviderFactory;
import org.keycloak.mfa.recovery.MFARecovery;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sms.SmsSenderProvider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.qlikverify.services.constants.Constants.ERROR_MESSAGE;
import static com.qlikverify.services.constants.Constants.MFA_PROVIDER_ID;
import static com.qlikverify.services.helpers.GeneralHelper.getProtector;


public class PhoneOTPMFAProvider extends MFARecovery implements MFAProvider, MFAProviderFactory {
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);
    private static final Logger logger = Logger.getLogger(PhoneOTPMFAProvider.class);

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:PhoneOTP:PreLogin");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return;
        verifyOtp(configModel, context,false,true);
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:PhoneOTP:PostLogin");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return false;
        return verifyOtp(configModel, context,false,true);
    }

    private boolean verifyOtp(MFAConfigModel configModel, RequiredActionContext context, boolean isFromRegistration, boolean showTroubleShoot) {
        SmsSenderProvider smsSenderProvider = context.getSession().getProvider(SmsSenderProvider.class, "default");
        LoginFormsProvider formsProvider = context.form();

        if(showTroubleShoot)
        {
            formsProvider
                    .setAttribute("showTroubleShoot",isMFARecoveryEnabled(context,configModel));
        }

        if (context.getUser() == null){
            org.keycloak.authentication.authenticators.utils.GeneralHelper.auditEvent(context,
                    EventType.PHONE_OTP_MFA_ERROR, "User not found.");
            showErrorView(context, "user-not-found");
            return false;
        }

        if (context.getUser().getMobileNumber() == null) {
            showErrorView(context, "no-mobile-number");
            return false;
        }

        if(context.getRealm().isEnabledOwnSmsProvider()){
            smsSenderProvider.setProviderId(GeneralHelper.getConfigValue(configModel, Constants.SMS_SENDER));
        } else {
            SmsConfigModel smsConfig = GeneralHelper.getDefaultSmsSender(context.getSession());
            if(smsConfig != null ){
                smsSenderProvider.setProviderId(smsConfig.getSmsProviderId());
            }
        }
        if (smsSenderProvider == null || !smsSenderProvider.isConfigured() || !smsSenderProvider.isSmsSupported()) {
            showErrorView(context, Constants.NO_SMS_CONFIG);
            return false;
        }

        if(isFormDataRequest(context.getHttpRequest())){
            MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

            if (Boolean.parseBoolean(formData.getFirst(Constants.FORM_RESEND))) {
                String expiryTime = configModel.getConfig().get(Constants.VERIFICATION_EXPIRY_CONFIG);
                if (!GeneralHelper.sendSmsOtp(configModel, context, expiryTime,GeneralHelper.getConfigValue(configModel, Constants.SMS_SENDER))) {
                    showErrorView(context, ErrorMappings.FAILED_TO_SEND_SMS_OTP);
                    return false;
                }
                Response challenge = formsProvider
                        .setAttribute(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE))
                        .setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY))
                        .setAttribute(MFA_PROVIDER_ID, getId())
                        .setAttribute("clientName",context.getAuthenticationSession().getClient().getName())
                        .createForm(Constants.MFA_OTP_VERIFICATION_FTL);
                context.challenge(challenge);
                return false;
            }
            else if (Boolean.parseBoolean(formData.getFirst(Constants.FORM_IS_OTP_VERIFICATION)))  {
                String otp = context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP);
                String otpExpiryTime = context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY);

                if (System.currentTimeMillis() > Long.parseLong(otpExpiryTime)) {
                    getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                    Response challenge = formsProvider
                            .setAttribute(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE))
                            .addError(new FormMessage(ErrorMappings.OTP_EXPIRED))
                            .setAttribute(MFA_PROVIDER_ID, getId())
                            .setAttribute("clientName",context.getAuthenticationSession().getClient().getName())
                            .createForm(Constants.MFA_OTP_VERIFICATION_FTL);
                    context.challenge(challenge);
                    return false;
                }

                if (formData.getFirst(Constants.FORM_OTP) == null || formData.getFirst(Constants.FORM_OTP).length() == 0) {
                    getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                    Response challenge = formsProvider
                            .setAttribute(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE))
                            .addError(new FormMessage(ErrorMappings.ENTER_OTP))
                            .setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                            .setAttribute(MFA_PROVIDER_ID, getId())
                            .setAttribute("clientName",context.getAuthenticationSession().getClient().getName())
                            .createForm(Constants.MFA_OTP_VERIFICATION_FTL);
                    context.challenge(challenge);
                    return false;
                } else if (formData.getFirst(Constants.FORM_OTP).equals(otp)) {
                    UserModel user = context.getUser();
                    user.setSingleAttribute(Constants.ATTRIBUTE_PHONE_VERIFIED, "TRUE");
                    if(isFromRegistration) {
                        OTPCredentialModel otpCredentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), "test",context.getAuthenticationSession().getClient().getClientId());
                        otpCredentialModel.setProviderId(getId());
                        OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
                        otpCredentialProvider.createCredential(context.getRealm(), context.getUser(), otpCredentialModel);
                    }
                    getProtector(context).successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
                    return true;
                }
                getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                Response challenge = formsProvider
                        .setAttribute(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE, context.getAuthenticationSession().getAuthNote(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE))
                        .addError(new FormMessage(ErrorMappings.WRONG_OTP_ERROR))
                        .setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, otpExpiryTime)
                        .setAttribute(MFA_PROVIDER_ID, getId())
                        .setAttribute("clientName",context.getAuthenticationSession().getClient().getName())
                        .createForm(Constants.MFA_OTP_VERIFICATION_FTL);
                context.challenge(challenge);
                return false;
            }
        }

            String expiryTime = GeneralHelper.getConfigValue(configModel, Constants.VERIFICATION_EXPIRY_CONFIG);
            if(!GeneralHelper.sendSmsOtp(configModel,context, expiryTime,GeneralHelper.getConfigValue(configModel, Constants.SMS_SENDER))){
                showErrorView(context, ErrorMappings.FAILED_TO_SEND_SMS_OTP);
                return false;
            }
            Response challenge = formsProvider
//                    .setAttribute(Constants.PHONE_NUMBER_WITH_COUNTRY_CODE, formData.getFirst("mobileNum"))
                    .setAttribute(Constants.EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(Constants.SESSION_MOBILE_OTP_EXPIRY))
                    .setAttribute(MFA_PROVIDER_ID, getId())
                    .setAttribute("clientName",context.getAuthenticationSession().getClient().getName())
                    .createForm(Constants.MFA_OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return false;
    }

    protected boolean isFormDataRequest(HttpRequest request) {
        MediaType mediaType = request.getHttpHeaders().getMediaType();
        return mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    }


    @Override
    public boolean onPostRegistration(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:PhoneOTP:PostRegistration");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return false;
        return verifyOtp(configModel, context,true,false);
    }

    private void showErrorView(RequiredActionContext context, String error) {
        LoginFormsProvider formsProvider = context.form();
        formsProvider.setAttribute(MFA_PROVIDER_ID, getId());
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }
        Response challenge = formsProvider.createForm(Constants.ERROR_FTL);
        context.challenge(challenge);
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel, RequiredActionContext context, boolean canSkip) {
        logger.info("QlikVerify:MFA:PhoneOTP:PreRegistration");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return;
        verifyOtp(configModel, context,false,false);
    }


    protected boolean isTemporarilyDisabledByBruteForce(RequiredActionContext context, UserModel user) {
        if (context.getRealm().isBruteForceProtected() && getProtector(context).isTemporarilyDisabled(context.getSession(), context.getRealm(), user)) {
                Response challengeResponse = context.form()
                        .setAttribute(ERROR_MESSAGE, ErrorMappings.MAXIMUM_ATTEMPTS_REACHED_USER_TEMP_DISABLED)
                        .createForm("error.ftl");
                context.challenge(challengeResponse);
                return true;
            }
        return false;
    }

    @Override
    public String getDisplayName() {
        return "mfa-phone-message";
    }

    @Override
    public String getProviderClass() {
        return (MFAProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "sms-prov.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/MFA/Phone-OTP-Factor.md";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config) {
        return GeneralHelper.validateMFAConfig(getConfigProperties(session), config);
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.VERIFICATION_EXPIRY_CONFIG);
        configProperty.setLabel(Constants.OTP_VER_EXPIRY_LABEL);
        configProperty.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("mfa-phone-message-verify-expiry-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.PHONE_VER_OTP_LENGTH);
        configProperty.setLabel(Constants.PHONE_VER_OTP_LENGTH_LABEL);
        configProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        configProperty.setRequired(false);
        configProperty.setHelpText("mfa-phone-message-verify-len-helptext");
        configProperty.setMinVal(4);
        configProperty.setMaxVal(8);
        configProperty.setDefaultValue(6);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(Constants.PHONE_VER_OTP_TXT);
        configProperty.setLabel("mfa-phone-message-txt-template");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("mfa-phone-message-txt-template-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("enable_mfa_recovery");
        configProperty.setLabel("mfa-phone-message-recovery");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText("mfa-phone-message-recovery-helptext");
        configProperties.add(configProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        setupSmsSenders(session);
        return configProperties;
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
    public OTPPolicy getOtpPolicy(RealmModel realmModel) {
        return null;
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
        return "phone-otp";
    }

    @Override
    public String getHelpText() {
        return Constants.PHONE_MFA_HELP_TEXT;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

}

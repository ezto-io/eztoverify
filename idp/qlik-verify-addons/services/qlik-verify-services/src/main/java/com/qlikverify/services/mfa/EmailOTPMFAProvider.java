package com.qlikverify.services.mfa;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.MFAProviderFactory;
import org.keycloak.mfa.recovery.MFARecovery;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.qlikverify.services.constants.Constants.*;
import static com.qlikverify.services.helpers.GeneralHelper.getProtector;

public class EmailOTPMFAProvider extends MFARecovery implements MFAProviderFactory, MFAProvider {

    private static final Logger logger = Logger.getLogger(EmailOTPMFAProvider.class);
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:EmailOTP:PreLogin");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return;
        sendOTP(configModel, context,false);
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:EmailOTP:PostLogin");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return false;
       return verifyOTP(configModel, context, false);
    }

    @Override
    public boolean onPostRegistration(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:EmailOTP:PreRegistration");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return false;
       return verifyOTP(configModel, context, true);
    }

    private boolean verifyOTP(MFAConfigModel configModel, RequiredActionContext context, boolean isFromRegistration) {
        UserModel userModel = context.getUser();
        String expiryTime = GeneralHelper.getConfigValue(configModel, VERIFICATION_EXPIRY_CONFIG);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String otp = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP);
        String otpExpiry = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY);
        LoginFormsProvider loginFormsProvider = context.form();

        if(!isFromRegistration)
        {
            loginFormsProvider.setAttribute("showTroubleShoot", isMFARecoveryEnabled(context, configModel));
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
            String formEmail = userModel.getEmail();
            boolean isEmailSent = GeneralHelper.sendEmailOTP(configModel, context, formEmail, expiryTime);
            if (!isEmailSent) {
                showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL);
                return false;
            }
            otpExpiry = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY);
            Response challenge = loginFormsProvider
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiry)
                    .setAttribute("mfaProviderId", getId())
                    .setAttribute("clientName",context.getAuthenticationSession().getClient().getName())
                    .createForm(EMAIL_MFA_OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return false;
        } else {
            if (System.currentTimeMillis() > Long.parseLong(otpExpiry)) {
                getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
                showView(context, ErrorMappings.OTP_EXPIRED);
                return false;
            }
            if (formData.getFirst(FORM_OTP).equals(otp)) {
                UserModel user = context.getUser();
                user.setEmailVerified(true);
                if (isFromRegistration) {
                    OTPCredentialModel otpCredentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), "test",context.getAuthenticationSession().getClient().getClientId());
                    otpCredentialModel.setProviderId(getId());
                    OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
                    otpCredentialProvider.createCredential(context.getRealm(), context.getUser(), otpCredentialModel);
                }
                getProtector(context).successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
                return true;
            }
            getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            Response challenge = loginFormsProvider
                    .addError(new FormMessage(ErrorMappings.WRONG_OTP_ERROR))
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiry)
                    .setAttribute("mfaProviderId", getId())
                    .setAttribute("clientName",context.getAuthenticationSession().getClient().getName())
                    .createForm(EMAIL_MFA_OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return false;
        }
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel, RequiredActionContext context, boolean canSkip) {
        logger.info("QlikVerify:MFA:EmailOTP:PreRegistration");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return;
        sendOTP(configModel, context,true);
    }

    private void sendOTP(MFAConfigModel configModel, RequiredActionContext context,boolean isReg) {
        UserModel userModel = context.getUser();
        String expiryTime = GeneralHelper.getConfigValue(configModel, VERIFICATION_EXPIRY_CONFIG);
        if (userModel == null || (userModel != null && userModel.getEmail() == null)) {
            showView(context, ErrorMappings.EMAIL_NOT_FOUND);
            return;
        }

        if (!GeneralHelper.sendEmailOTP(configModel, context, userModel.getEmail(), expiryTime)) {
            showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL);
            return;
        }

        LoginFormsProvider formsProvider = context.form();

        if(!isReg) {
            formsProvider.setAttribute("showTroubleShoot", isMFARecoveryEnabled(context, configModel));
        }

        Response challenge = formsProvider
                .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY))
                .setAttribute("mfaProviderId", getId())
                .setAttribute("clientName",context.getAuthenticationSession().getClient().getName())
                .createForm(EMAIL_MFA_OTP_VERIFICATION_FTL);
        context.challenge(challenge);
    }

    private void showView(RequiredActionContext context, String error) {
        LoginFormsProvider formsProvider = context.form();
        formsProvider.setAttribute("mfaProviderId", getId());
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }
        Response challenge = formsProvider.createForm(ERROR_FTL);
        context.challenge(challenge);
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
        return "Email";
    }

    @Override
    public String getProviderClass() {
        return (MFAProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "smtp.svg";
    }

    @Override
    public String getHowTodo() {
        return  DEFAULT_SERVER_URL + "/Setup-Guide/MFA/Email-OTP-Factor.md";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config) {
        return GeneralHelper.validateMFAConfig(getConfigProperties(session), config);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName().equals(Constants.EMAIL_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(Constants.EMAIL_SENDER);
        provider.setLabel(Constants.EMAIL_SENDER_LABEL);

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
                provider.setHelpText("mfa-email-sender-helptext");
            }  else {
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
        return configProperties;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();


    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(Constants.VERIFICATION_EXPIRY_CONFIG);
        property.setLabel(Constants.EMAIL_VER_EXPIRY_LABEL);
        property.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        property.setRequired(true);
        property.setHelpText("mfa-email-expiry-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(Constants.EMAIL_VER_OTP_LENGTH);
        property.setLabel(Constants.EMAIL_VER_OTP_LENGTH_LABEL);
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setRequired(false);
        property.setHelpText("mfa-email-otp-len-helptext");
        property.setMinVal(4);
        property.setMaxVal(8);
        property.setDefaultValue(6);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("enable_mfa_recovery");
        property.setLabel("mfa-email-recovery");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("mfa-email-recovery-helptext");
        configProperties.add(property);
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
        return "email-otp";
    }

    @Override
    public String getHelpText() {
        return EMAIL_MFA_HELP_TEXT;
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

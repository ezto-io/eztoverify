package com.qlikverify.services.mfa;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.credential.OTPCredentialProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.TotpBean;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.MFAProviderFactory;
import org.keycloak.mfa.recovery.MFARecovery;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.utils.CredentialHelper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.qlikverify.services.constants.Constants.*;
import static com.qlikverify.services.helpers.GeneralHelper.getProtector;

public class GoogleAuthenticationMFAProvider extends MFARecovery implements MFAProvider, MFAProviderFactory {

    public static final String SELECTED_OTP_CREDENTIAL_ID = "selectedOtpCredentialId";
    private static final Logger logger = Logger.getLogger(GoogleAuthenticationMFAProvider.class);
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    private static TotpBean totpBean = null;


    @Override
    public String getDisplayName() {
        return "mfa-google-authenticator";
    }

    @Override
    public String getProviderClass() {
        return (MFAProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "google-authenticator.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/MFA/Google-Authenticator.md";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config) {
        return new ArrayList<>();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
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
        return "google-authenticator";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName("look_ahead_window");
        property.setLabel("mfa-google-look-ahead-window");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("mfa-google-look-ahead-window-helptext");
        property.setDefaultValue("1");
        property.setMinVal(0);
        property.setMaxVal(5);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("enable_mfa_recovery");
        property.setLabel("mfa-google-recovery");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("mfa-google-recovery-helptext");
        configProperties.add(property);
    }

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:GoogleAuthenticator:PreLogin");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return;

        Response challenge = context.form()
                .setAttribute("showTroubleShoot",isMFARecoveryEnabled(context,configModel))
                .setAttribute("mfaProviderId",getId())
                .setAttribute("clientName",context.getAuthenticationSession().getClient().getName())
                .setAttribute(APPLICATION_NAME,GOOGLE_AUTHENTICATOR)
                .createLoginTotp();
        context.challenge(challenge);
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel,RequiredActionContext context) {
        logger.info("QlikVerify:MFA:GoogleAuthenticator:PostLogin");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return false;

        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();

        String otp = inputData.getFirst("otp");

        Optional<CredentialModel> credentialModel = context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(),context.getUser(),OTPCredentialModel.TYPE)
                .filter(x->x.getClientId() != null && x.getClientId().equals(context.getAuthenticationSession().getClient().getClientId()))
                .filter(x->x.getProviderId() != null && x.getProviderId().equals(getId()))
                .findFirst();

        String credentialId = null;

        if (!credentialModel.isPresent()) {
            OTPCredentialModel defaultOtpCredential = getCredentialProvider(context.getSession())
                    .getDefaultCredential(context.getSession(), context.getRealm(), context.getUser());
            credentialId = defaultOtpCredential==null ? "" : defaultOtpCredential.getId();
        }
        else
        {
            credentialId= credentialModel.get().getId();
        }

        context.getEvent().detail(Details.SELECTED_CREDENTIAL_ID, credentialId);

        context.form().setAttribute(SELECTED_OTP_CREDENTIAL_ID, credentialId);

        UserModel userModel = context.getUser();
        if (!userModel.isEnabled()) {
            // error in context is set in enabledUser/isTemporarilyDisabledByBruteForce
            return false;
        }

        if (otp == null) {
            Response challengeResponse = challenge(context,null);
            context.challenge(challengeResponse);
            return false;
        }
        boolean valid = context.getSession().userCredentialManager().isValid(context.getRealm(),context.getUser(),
                new UserCredentialModel(credentialId, getCredentialProvider(context.getSession()).getType(), otp));
        if (!valid) {
            getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            context.getEvent().user(userModel)
                    .error(Errors.INVALID_USER_CREDENTIALS);
            Response challenge = context.form()
                    .setAttribute("showTroubleShoot",isMFARecoveryEnabled(context,configModel))
                    .setAttribute("mfaProviderId",getId())
                    .setAttribute("clientName",context.getAuthenticationSession().getClient().getName())
                    .setAttribute(APPLICATION_NAME,GOOGLE_AUTHENTICATOR)
                    .setError( Messages.INVALID_TOTP)
                    .createLoginTotp();
            context.challenge(challenge);
            return false;
        }
        getProtector(context).successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
        return true;
    }

    @Override
    public boolean onPostRegistration(MFAConfigModel configModel,RequiredActionContext context) {
        logger.info("QlikVerify:MFA:GoogleAuthenticator:PostRegistration");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return false;
        EventBuilder event = context.getEvent();
        event.event(EventType.UPDATE_TOTP);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String challengeResponse = formData.getFirst("totp");
        String totpSecret = formData.getFirst("totpSecret");
        String mode = formData.getFirst("mode");
        String userLabel = formData.getFirst("userLabel");

        OTPPolicy policy = getOtpPolicy(configModel);
        OTPCredentialModel credentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), totpSecret, userLabel,policy,context.getAuthenticationSession().getClient().getClientId());
        credentialModel.setProviderId(getId());
        if (Validation.isBlank(challengeResponse)) {
            getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            Response challenge = getLoginFormsProvider(context,mode,policy)
                    .addError(new FormMessage(Validation.FIELD_OTP_CODE, Messages.MISSING_TOTP))
                    .setAttribute(APPLICATION_NAME,GOOGLE_AUTHENTICATOR)
                    .createForm(MFA_OTP_REG_FTL);
            context.challenge(challenge);
            return false;
        } else if (Validation.isBlank(userLabel)) {
            getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            Response challenge = getLoginFormsProvider(context,mode,policy)
                    .addError(new FormMessage(Validation.FIELD_OTP_LABEL, Messages.MISSING_TOTP_DEVICE_NAME))
                    .setAttribute(APPLICATION_NAME,GOOGLE_AUTHENTICATOR)
                    .createForm(MFA_OTP_REG_FTL);
            context.challenge(challenge);
            return false;
        } else if (!validateOTPCredential(context, challengeResponse, credentialModel, policy)) {
            getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            Response challenge = getLoginFormsProvider(context,mode,policy)
                    .addError(new FormMessage(Validation.FIELD_OTP_CODE, Messages.INVALID_TOTP))
                    .setAttribute(APPLICATION_NAME,GOOGLE_AUTHENTICATOR)
                    .createForm(MFA_OTP_REG_FTL);
            context.challenge(challenge);
            return false;
        }
        OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
        final Stream<CredentialModel> otpCredentials = (otpCredentialProvider.isConfiguredFor(context.getRealm(), context.getUser()))
                ? context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), OTPCredentialModel.TYPE)
                : Stream.empty();
        if (otpCredentials.count() >= 1 && Validation.isBlank(userLabel)) {
            getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            Response challenge = getLoginFormsProvider(context,mode,policy)
                    .addError(new FormMessage(Validation.FIELD_OTP_LABEL, Messages.MISSING_TOTP_DEVICE_NAME))
                    .setAttribute(APPLICATION_NAME,GOOGLE_AUTHENTICATOR)
                    .createForm(MFA_OTP_REG_FTL);
            context.challenge(challenge);
            return false;
        }

        if (!CredentialHelper.createOTPCredential(context.getSession(), context.getRealm(), context.getUser(), challengeResponse, credentialModel)) {
            getProtector(context).failedLogin(context.getRealm(), context.getUser(), context.getConnection());
            Response challenge = getLoginFormsProvider(context,mode,policy)
                    .addError(new FormMessage(Validation.FIELD_OTP_CODE, Messages.INVALID_TOTP))
                    .setAttribute(APPLICATION_NAME,GOOGLE_AUTHENTICATOR)
                    .createForm(MFA_OTP_REG_FTL);
            context.challenge(challenge);
            return false;
        }
        getProtector(context).successfulLogin(context.getRealm(), context.getUser(), context.getConnection());
        return true;
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel,RequiredActionContext context, boolean canSkip) {
        logger.info("QlikVerify:MFA:GoogleAuthenticator:PreRegistration");
        //Check user is disabled by brute force attack
        if (isTemporarilyDisabledByBruteForce(context, context.getUser()))
            return;
        OTPPolicy otpPolicy = getOtpPolicy(configModel);
        Response challenge = getRegistrationResponse(context,context.getUriInfo().getQueryParameters().getFirst("mode"),otpPolicy);
        context.challenge(challenge);
    }

    private Response getRegistrationResponse(RequiredActionContext context, String mode, OTPPolicy otpPolicy) {
        return getLoginFormsProvider(context, mode, otpPolicy)
                .setAttribute(APPLICATION_NAME,GOOGLE_AUTHENTICATOR)
                .createForm(MFA_OTP_REG_FTL);
    }

    private LoginFormsProvider getLoginFormsProvider(RequiredActionContext context, String mode,OTPPolicy otpPolicy) {
        if(context.getAuthenticationSession().getAuthNote("skipMfaQR") == null || (totpBean == null || totpBean.getTotpSecretQrCode() != null)){
            totpBean = new TotpBean(context.getSession(), context.getRealm(), context.getSession().getContext().getClient(), context.getUser(), context.getUriInfo().getRequestUriBuilder(),otpPolicy);
            context.getAuthenticationSession().setAuthNote("skipMfaQR","true");
        }

        return context.form()
                .setAttribute("totpMode", mode)
                .setAttribute("mfaProviderId", getId())
                .setAttribute("totp", totpBean)
                .setAttribute("appStore", "https://apps.apple.com/us/app/google-authenticator/id388497605")
                .setAttribute("playStore", "https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2&hl=en_IN&gl=US");
    }

    // Use separate method, so it's possible to override in the custom provider
    protected boolean validateOTPCredential(RequiredActionContext context, String token, OTPCredentialModel credentialModel, OTPPolicy policy) {
        return CredentialValidation.validOTP(token, credentialModel, policy.getLookAheadWindow());
    }

    private OTPPolicy getOtpPolicy(MFAConfigModel configModel) {
        String lookAheadWindow = GeneralHelper.getConfigValue(configModel,"look_ahead_window",String.valueOf(GeneralHelper.getConfigProp(configProperties,"look_ahead_window").getDefaultValue()));

        OTPPolicy policy = new OTPPolicy();
        policy.setAlgorithm(HmacOTP.HMAC_SHA1);
        policy.setType(OTPCredentialModel.TOTP);
        policy.setDigits(6);
        policy.setLookAheadWindow(Integer.parseInt(lookAheadWindow));
        policy.setPeriod(30);

        return policy;
    }

    private OTPCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (OTPCredentialProvider)session.getProvider(CredentialProvider.class, OTPCredentialProviderFactory.PROVIDER_ID);
    }

    protected Response challenge(RequiredActionContext context, String error) {
        return challenge(context, error, null);
    }

    protected Response challenge(RequiredActionContext context, String error, String field) {
        LoginFormsProvider form = context.form();
        if (error != null) {
            if (field != null) {
                form.addError(new FormMessage(field, error));
            } else {
                form.setError(error);
            }
        }
        form.setAttribute("mfaProviderId",getId());
        return form.createLoginTotp();
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
    public String getHelpText() {
        return Constants.GOOGLE_MFA_HELP_TEXT;
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

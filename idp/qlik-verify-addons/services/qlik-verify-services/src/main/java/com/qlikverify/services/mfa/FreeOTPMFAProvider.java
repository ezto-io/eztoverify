package com.qlikverify.services.mfa;

import com.qlikverify.services.constants.Constants;
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
import java.util.stream.Stream;

import static com.qlikverify.services.constants.Constants.APPLICATION_NAME;
import static com.qlikverify.services.constants.Constants.MFA_OTP_REG_FTL;


public class FreeOTPMFAProvider extends MFARecovery implements MFAProviderFactory, MFAProvider {
    public static final String SELECTED_OTP_CREDENTIAL_ID = "selectedOtpCredentialId";
    private static final Logger logger = Logger.getLogger(FreeOTPMFAProvider.class);
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public String getDisplayName() {
        return "mfa-free-otp";
    }

    @Override
    public String getProviderClass() {
        return (MFAProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "free-otp-logo.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/MFA/Free-OTP.md";
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
        MFAConfigModel configModel = realmModel.getMFAConfigByProviderId(getId());
        return getOtpPolicy(configModel);
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
        return "free-otp";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {

        ProviderConfigProperty property;

        List<String> otpTypes = new ArrayList<>();
        otpTypes.add("Time Based");
        otpTypes.add("Counter Based");
        property = new ProviderConfigProperty();
        property.setName("otp_type");
        property.setLabel("mfa-free-otp-type");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(otpTypes);
        property.setRequired(true);
        property.setHelpText("mfa-free-otp-type-helptext");
        configProperties.add(property);

        List<String> otpAlgorithms = new ArrayList<>();
        otpAlgorithms.add("SHA1");
        otpAlgorithms.add("SHA256");
        otpAlgorithms.add("SHA512");
        property = new ProviderConfigProperty();
        property.setName("otp_hash_algorithm");
        property.setLabel("mfa-free-otp-hash-algo");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(otpAlgorithms);
        property.setRequired(true);
        property.setHelpText("mfa-free-otp-hash-algo-helptext");
        configProperties.add(property);


        List<String> otpLength = new ArrayList<>();
        otpLength.add("6");
        otpLength.add("8");
        property = new ProviderConfigProperty();
        property.setName("otp_length");
        property.setLabel("mfa-free-otp-no-of-digits");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(otpLength);
        property.setRequired(true);
        property.setHelpText("mfa-free-otp-no-of-digits-helptext");
        configProperties.add(property);


        property = new ProviderConfigProperty();
        property.setName("look_ahead_window");
        property.setLabel("mfa-free-otp-look-ahead-window");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setRequired(false);
        property.setHelpText("mfa-free-otp-look-ahead-window-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("initial_counter");
        property.setLabel("mfa-free-otp-initial-counter");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("mfa-free-otp-initial-counter-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("otp_token_period");
        property.setLabel("mfa-free-otp-token-session");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("mfa-free-otp-token-session-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("enable_mfa_recovery");
        property.setLabel("mfa-free-otp-recovery");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("mfa-free-otp-recovery-helptext");
        configProperties.add(property);
    }

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:FreeOTP:PreLogin");
        Response challengeResponse = challenge(configModel,context,null);
        context.challenge(challengeResponse);
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:FreeOTP:PostLogin");
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();

        String otp = inputData.getFirst("otp");

        String credentialId = inputData.getFirst("selectedCredentialId");

        if (credentialId == null || credentialId.isEmpty()) {
            OTPCredentialModel defaultOtpCredential = getCredentialProvider(context.getSession())
                    .getDefaultCredential(context.getSession(), context.getRealm(), context.getUser());
            credentialId = defaultOtpCredential==null ? "" : defaultOtpCredential.getId();
        }
        context.getEvent().detail(Details.SELECTED_CREDENTIAL_ID, credentialId);

        context.form().setAttribute(SELECTED_OTP_CREDENTIAL_ID, credentialId);

        UserModel userModel = context.getUser();
        if (!userModel.isEnabled()) {
            // error in context is set in enabledUser/isTemporarilyDisabledByBruteForce
            return false;
        }

        if (otp == null) {
            Response challengeResponse = challenge(configModel,context,null);
            context.challenge(challengeResponse);
            return false;
        }
        boolean valid = context.getSession().userCredentialManager().isValid(context.getRealm(),context.getUser(),
                new UserCredentialModel(credentialId, getCredentialProvider(context.getSession()).getType(), otp));
        if (!valid) {
            context.getEvent().user(userModel)
                    .error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = challenge(configModel,context, Messages.INVALID_TOTP, Validation.FIELD_OTP_CODE);
            context.challenge(challengeResponse);
            return false;
        }
        return true;
    }

    @Override
    public boolean onPostRegistration(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:FreeOTP:PostRegistration");
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
            Response challenge = getLoginFormsProvider(context,mode,policy)
                    .addError(new FormMessage(Validation.FIELD_OTP_CODE, Messages.MISSING_TOTP))
                    .setAttribute(APPLICATION_NAME,"FreeOtp")
                    .createForm(MFA_OTP_REG_FTL);
            context.challenge(challenge);
            return false;
        } else if (!validateOTPCredential(context, challengeResponse, credentialModel, policy)) {
            Response challenge = getLoginFormsProvider(context,mode,policy)
                    .addError(new FormMessage(Validation.FIELD_OTP_CODE, Messages.INVALID_TOTP))
                    .setAttribute(APPLICATION_NAME,"FreeOtp")
                    .createForm(MFA_OTP_REG_FTL);
            context.challenge(challenge);
            return false;
        }
        OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
        final Stream<CredentialModel> otpCredentials = (otpCredentialProvider.isConfiguredFor(context.getRealm(), context.getUser()))
                ? context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), OTPCredentialModel.TYPE)
                : Stream.empty();
        if (otpCredentials.count() >= 1 && Validation.isBlank(userLabel)) {
            Response challenge = getLoginFormsProvider(context,mode,policy)
                    .addError(new FormMessage(Validation.FIELD_OTP_LABEL, Messages.MISSING_TOTP_DEVICE_NAME))
                    .setAttribute(APPLICATION_NAME,"FreeOtp")
                    .createForm(MFA_OTP_REG_FTL);
            context.challenge(challenge);
            return false;
        }

        if (!CredentialHelper.createOTPCredential(context.getSession(), context.getRealm(), context.getUser(), challengeResponse, credentialModel)) {
            Response challenge = getLoginFormsProvider(context,mode,policy)
                    .addError(new FormMessage(Validation.FIELD_OTP_CODE, Messages.INVALID_TOTP))
                    .setAttribute(APPLICATION_NAME,"FreeOtp")
                    .createForm(MFA_OTP_REG_FTL);
            context.challenge(challenge);
            return false;
        }

        return true;
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel, RequiredActionContext context, boolean canSkip) {
        logger.info("QlikVerify:MFA:FreeOTP:PreRegistration");
        OTPPolicy policy = getOtpPolicy(configModel);
        Response challenge = getRegistrationResponse(context,context.getUriInfo().getQueryParameters().getFirst("mode"),policy);
        context.challenge(challenge);
    }

    private Response getRegistrationResponse(RequiredActionContext context, String mode, OTPPolicy otpPolicy) {
        return getLoginFormsProvider(context, mode,otpPolicy)
                .setAttribute(APPLICATION_NAME,"FreeOtp")
                .createForm(MFA_OTP_REG_FTL);
    }

    private LoginFormsProvider getLoginFormsProvider(RequiredActionContext context, String mode, OTPPolicy otpPolicy) {
        return context.form()
                .setAttribute("totpMode", mode)
                .setAttribute("mfaProviderId", getId())
                .setAttribute("totp", new TotpBean(context.getSession(), context.getRealm(), context.getSession().getContext().getClient(), context.getUser(), context.getUriInfo().getRequestUriBuilder(),otpPolicy))
                .setAttribute("appStore", "https://apps.apple.com/us/app/freeotp-authenticator/id872559395")
                .setAttribute("playStore", "https://play.google.com/store/apps/details?id=org.fedorahosted.freeotp&hl=en");

    }

    // Use separate method, so it's possible to override in the custom provider
    protected boolean validateOTPCredential(RequiredActionContext context, String token, OTPCredentialModel credentialModel, OTPPolicy policy) {
        return CredentialValidation.validOTP(token, credentialModel, policy.getLookAheadWindow());
    }

    private OTPPolicy getOtpPolicy(MFAConfigModel configModel) {
        Map<String, String> config = configModel.getConfig();

        OTPPolicy policy = new OTPPolicy();
        policy.setAlgorithm(getAlgorithm(config.get("otp_hash_algorithm")));
        if (config.get("otp_type").equals("Time Based"))
        {
            policy.setType(OTPCredentialModel.TOTP);
        }
        else
        {
            policy.setType(OTPCredentialModel.HOTP);
        }
        policy.setDigits(Integer.parseInt(config.get("otp_length")));
        policy.setLookAheadWindow(Integer.parseInt(GeneralHelper.getConfigValue(configModel,"look_ahead_window","1")));
        if (config.get("otp_type").equals("Time Based")) {
            String value = config.get("otp_token_period");
            if(value == null || value.isEmpty())
            {
                value = "30";
            }
            policy.setPeriod(Integer.parseInt(value));
        } else {
            String counter = config.get("initial_counter");
            if(counter == null || counter.isEmpty())
            {
                counter = "1";
            }
            policy.setInitialCounter(Integer.parseInt(counter));
        }

        return policy;
    }

    private String getAlgorithm(String algorithm)
    {
        switch (algorithm)
        {
            default:
            case "SHA1":
                return HmacOTP.HMAC_SHA1;
            case "SHA256":
                return HmacOTP.HMAC_SHA256;
            case "SHA512":
                return HmacOTP.HMAC_SHA512;
        }
    }

    private OTPCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (OTPCredentialProvider)session.getProvider(CredentialProvider.class, OTPCredentialProviderFactory.PROVIDER_ID);
    }

    protected Response challenge(MFAConfigModel configModel,RequiredActionContext context, String error) {
        return challenge(configModel,context, error, null);
    }

    protected Response challenge(MFAConfigModel configModel,RequiredActionContext context, String error, String field) {
        LoginFormsProvider form = context.form();
        if (error != null) {
            if (field != null) {
                form.addError(new FormMessage(field, error));
            } else {
                form.setError(error);
            }
        }
        form.setAttribute("showTroubleShoot", isMFARecoveryEnabled(context,configModel));
        form.setAttribute("mfaProviderId",getId());
        form.setAttribute("clientName",context.getAuthenticationSession().getClient().getName());
        return form.createLoginTotp();
    }

    @Override
    public String getHelpText() {
        return Constants.FREEOTP_MFA_HELP_TEXT;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return false;
    }
}

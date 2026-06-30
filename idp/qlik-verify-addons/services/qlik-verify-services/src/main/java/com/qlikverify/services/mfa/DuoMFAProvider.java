package com.qlikverify.services.mfa;

import com.duosecurity.duoweb.DuoWeb;
import com.qlikverify.services.constants.Constants;
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
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.qlikverify.services.constants.Constants.SIG_RESPONSE;

public class DuoMFAProvider extends MFARecovery implements MFAProviderFactory, MFAProvider {

    private static final Logger logger = Logger.getLogger(DuoMFAProvider.class);
    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public void onPreLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:DuoSecurity:PreLogin");
        Response challenge = createDuoForm(configModel, context, null);
        context.challenge(challenge);
    }

    @Override
    public boolean onPostLogin(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:DuoSecurity:PostLogin");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.failure();
            return false;
        }
        if (!formData.containsKey(SIG_RESPONSE)) {
            context.challenge(createDuoForm(configModel, context, "missing sig_response"));
            return false;
        }
        String sig_response = formData.getFirst(SIG_RESPONSE);
        String authenticated_username = null;
        try {
            authenticated_username = DuoWeb.verifyResponse(duoIkey(configModel), duoSkey(configModel), duoAkey(configModel), sig_response);
        } catch (Exception ex) {
            context.challenge(createDuoForm(configModel, context, ex.getMessage()));
            return false;
        }
        if (!authenticated_username.equals(context.getUser().getUsername())) {
            String error = "Wrong DUO user returned: " + authenticated_username + " (expected " + context.getUser().getUsername() + ")";
            context.challenge(createDuoForm(configModel, context, error));
            return false;
        }
        return true;
    }

    @Override
    public boolean onPostRegistration(MFAConfigModel configModel, RequiredActionContext context) {
        logger.info("QlikVerify:MFA:DuoSecurity:PostRegistration");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.failure();
            return false;
        }
        if (!formData.containsKey(SIG_RESPONSE)) {
            context.challenge(createDuoForm(configModel, context, "missing sig_response"));
            return false;
        }
        String sig_response = formData.getFirst(SIG_RESPONSE);
        String authenticated_username = null;
        try {
            authenticated_username = DuoWeb.verifyResponse(duoIkey(configModel), duoSkey(configModel), duoAkey(configModel), sig_response);
        } catch (Exception ex) {
            context.challenge(createDuoForm(configModel, context, ex.getMessage()));
            return false;
        }
        if (!authenticated_username.equals(context.getUser().getUsername())) {
            String error = "Wrong DUO user returned: " + authenticated_username + " (expected " + context.getUser().getUsername() + ")";
            context.challenge(createDuoForm(configModel, context, error));
            return false;
        }

        OTPCredentialModel otpCredentialModel = OTPCredentialModel.createFromPolicy(context.getRealm(), "test",context.getAuthenticationSession().getClient().getClientId());
        otpCredentialModel.setProviderId(getId());
        OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) context.getSession().getProvider(CredentialProvider.class, "keycloak-otp");
        otpCredentialProvider.createCredential(context.getRealm(), context.getUser(), otpCredentialModel);

        return true;
    }

    @Override
    public void onPreRegistration(MFAConfigModel configModel, RequiredActionContext context, boolean canSkip) {
        logger.info("QlikVerify:MFA:DuoSecurity:PreRegistration");
        Response challenge = createDuoForm(configModel, context, null);
        context.challenge(challenge);
    }

    private Response createDuoForm(MFAConfigModel configModel, RequiredActionContext context, String error) {
        String sig_request = DuoWeb.signRequest(duoIkey(configModel), duoSkey(configModel), duoAkey(configModel), context.getUser().getUsername());
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("provider-id", getId());
        LoginFormsProvider form = context.form()
                .setFormData(formData)
                .setAttribute("sig_request", sig_request)
                .setAttribute("mfaProviderId", getId())
                .setAttribute("apihost", duoApihost(configModel));
        form.addScript("https://api.duosecurity.com/frame/hosted/Duo-Web-v2.js");
        if (error != null) {
            form.setError(error);
        } else if (sig_request.startsWith("ERR")) {
            form.setError("Did you configure Duo in Keycloak?\n" + sig_request);
        }
        form.setAttribute("showTroubleShoot",isMFARecoveryEnabled(context,configModel));
        form.setAttribute("clientName",context.getAuthenticationSession().getClient().getName());
        return form.createForm("duo-mfa.ftl");
    }

    @Override
    public String getDisplayName() {
        return "Duo Security";
    }

    @Override
    public String getProviderClass() {
        return (MFAProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "duo_mfa.png";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL +"/Setup-Guide/MFA/Duo-MFA.md";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config) {
        return null;
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
    public String getHelpText() {
        return Constants.DUO_MFA_HELP_TEXT;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return false;
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
        return "duo-mfa";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    protected static final String DUO_INTEGERATION_KEY = "integeration_key";

    protected static final String DUO_SECRET_KEY = "secret_key";

    protected static final String DUO_CHALLENGE_NOUNCE = "challenge_nounce";

    protected static final String DUO_API_HOST = "api_host";

    static {

        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(DUO_INTEGERATION_KEY);
        property.setLabel("mfa-duo-client-id");
        property.setType(ProviderConfigProperty.PASSWORD);
        property.setSecret(true);
        property.setRequired(true);
        property.setHelpText("mfa-duo-client-id-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(DUO_SECRET_KEY);
        property.setLabel("mfa-duo-client-secret");
        property.setType(ProviderConfigProperty.PASSWORD);
        property.setSecret(true);
        property.setRequired(true);
        property.setHelpText("mfa-duo-client-secret-helptext");
        configProperties.add(property);

        ProviderConfigProperty akey = new ProviderConfigProperty();
        akey.setName(DUO_CHALLENGE_NOUNCE);
        akey.setLabel("mfa-duo-challenge-nonce");
        akey.setType(ProviderConfigProperty.ALPHANUMERIC_TYPE);
        akey.setRequired(true);
        akey.setHelpText("mfa-duo-challenge-nonce-helptext");
        configProperties.add(akey);

        property = new ProviderConfigProperty();
        property.setName(DUO_API_HOST);
        property.setLabel("mfa-duo-api-host");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setHelpText("mfa-duo-api-host-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("enable_mfa_recovery");
        property.setLabel("mfa-duo-recovery");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("mfa-duo-recovery-helptext");
        configProperties.add(property);
    }

    private String duoIkey(MFAConfigModel config) {
        if (config == null) return "";
        return String.valueOf(config.getConfig().get(DUO_INTEGERATION_KEY));
    }

    private String duoSkey(MFAConfigModel config) {
        if (config == null) return "";
        return String.valueOf(config.getConfig().get(DUO_SECRET_KEY));
    }

    private String duoAkey(MFAConfigModel config) {
        if (config == null) return "";
        return String.valueOf(config.getConfig().get(DUO_CHALLENGE_NOUNCE));
    }

    private String duoApihost(MFAConfigModel config) {
        if (config == null) return "";
        return String.valueOf(config.getConfig().get(DUO_API_HOST));
    }

}

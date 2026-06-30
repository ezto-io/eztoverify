package com.qlikverify.services.requiredactions;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.apache.http.util.TextUtils;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.EmailConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.policy.ciba.CibaPolicyProvider;
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.qlikverify.services.constants.Constants.*;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;

public class UpdateEmailRequiredAction implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    @Override
    public String getDisplayText() {
        return "update-email-ra";
    }


    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }



    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getPolicyId() != null) {
            boolean result;
            String policyType = context.getPolicyType();
            if (policyType.equals("action")) {
                result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
            } else {
                result = context.getSession().getProvider(CibaPolicyProvider.class).validate(context.getSession(), context.getAuthenticationSession().getAuthNote(BINDING_MESSAGE), context.getPolicyId());
            }
            if (result) {
                addAction(context);
            } else {
                context.getUser().removeRequiredAction(getId());
            }
        }
    }
    private void addAction(RequiredActionContext context) {
        if (!context.getUser().getRequiredActionsStream().filter(x -> x.equalsIgnoreCase(getId())).findFirst().isPresent()) {
            context.getUser().addRequiredAction(getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        LoginFormsProvider loginFormsProvider = context.form();
        if(context.getUser().getEmail() != null){
            loginFormsProvider.setAttribute("oldEmail", context.getUser().getEmail());
        }
        context.challenge(loginFormsProvider.createForm(UPDATE_EMAIL_FTL));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String expiryTime   = GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG);
        if (Boolean.parseBoolean(formData.getFirst(FORM_EDIT))) {
            Response challenge = context.form()
                    .setAttribute(OLD_EMAIL, context.getAuthenticationSession().getAuthNote(EMAIL))
                    .createForm(UPDATE_EMAIL_FTL);
            context.challenge(challenge);
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
            if (!GeneralHelper.sendEmailOTP(context, formData.getFirst(EMAIL), expiryTime)) {
                showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, UPDATE_EMAIL_FTL);
                return;
            } else {
                Response challenge = context.form()
                        .setAttribute(EMAIL, context.getAuthenticationSession().getAuthNote(EMAIL))
                        .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY))
                        .createForm(OTP_VERIFICATION_FTL);
                context.challenge(challenge);
                return;
            }
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_EMAIL_UPDATE))) {
            String email = formData.getFirst(EMAIL);

            if(TextUtils.isEmpty(email)) {
                showView(context, ErrorMappings.EMAIL_REQUIRED, UPDATE_EMAIL_FTL);
                return;
            }
            if (!GeneralHelper.isValidEmail(email)) {
                Response challenge = context.form()
                        .addError(new FormMessage(ErrorMappings.EMAIL_INVALID))
                        .setAttribute(EMAIL, email)
                        .createForm(UPDATE_EMAIL_FTL);
                context.challenge(challenge);
                return;
            }

            UserModel user = context.getSession().users().getUserByEmail(context.getRealm(),email);
            if(user != null && context.getUser() != null && !context.getUser().getId().equals(user.getId())){
                showView(context, ErrorMappings.USER_EXISTS_SAME_EMAIL, UPDATE_EMAIL_FTL);
                return;
            }

            context.getAuthenticationSession().setAuthNote(EMAIL, email);

            if (!GeneralHelper.sendEmailOTP(context, email, expiryTime)) {
                showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, UPDATE_EMAIL_FTL);
                return;
            }
            Response challenge = context.form()
                    .setAttribute(EMAIL, email)
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY))
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return;


        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_OTP_VERIFICATION))) {
            String sessEmail = context.getAuthenticationSession().getAuthNote(EMAIL);
            if (sessEmail == null && sessEmail.length() == 0) {
                showView(context, ErrorMappings.EMAIL_REQUIRED, UPDATE_EMAIL_FTL);
                return;
            }
            UserModel user      = context.getSession().users().getUserByEmail(context.getRealm(),sessEmail);
            String otp          = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP);
            String otpExpiry    = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY);
            context.form().setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiry);

            if (user != null) {
                showView(context, ErrorMappings.EMAIL_REGISTERED, UPDATE_EMAIL_FTL);
                return;
            }

            if (System.currentTimeMillis() > Long.parseLong(otpExpiry)) {
                Response challenge = context.form()
                        .addError(new FormMessage(ErrorMappings.OTP_EXPIRED))
                        .setAttribute(EMAIL, context.getAuthenticationSession().getAuthNote(EMAIL))
                        .createForm(OTP_VERIFICATION_FTL);
                context.challenge(challenge);
                return;
            }

            if (!formData.getFirst(FORM_OTP).equals(otp)) {
                Response challenge = context.form()
                        .addError(new FormMessage(ErrorMappings.WRONG_OTP_ERROR))
                        .setAttribute(EMAIL, context.getAuthenticationSession().getAuthNote(EMAIL))
                        .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiry)
                        .createForm(OTP_VERIFICATION_FTL);
                context.challenge(challenge);
                return;
            }
            context.getUser().setEmail(sessEmail);
            context.getUser().setEmailVerified(true);
            context.success();
        }

    }

    private void showView(RequiredActionContext context, String error, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
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
        return "update-email";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(Constants.VERIFICATION_EXPIRY_CONFIG);
        property.setLabel("email-config-expiry-duration");
        property.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        property.setHelpText("email-config-expiry-duration-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(Constants.EMAIL_VER_OTP_LENGTH);
        property.setLabel(Constants.EMAIL_VER_OTP_LENGTH_LABEL);
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("email-ver-otp-length-helptext");
        property.setMinVal(4);
        property.setMaxVal(8);
        property.setDefaultValue(6);
        configProperties.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(Constants.EMAIL_SENDER)).findFirst();
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
                provider.setHelpText("email-config-set-sender");
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("No Email Sender are registered. Click <a href=' #/realms/" + session.getContext().getRealm().getName() + "/email/providers/create'> here </a> to create one");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("All Emails are routed through default Email Provider from "+ org.keycloak.Config.getAdminRealm() +".If you want to use your email providers, Click <a href=' #/realms/"+ session.getContext().getRealm().getName()+ "/realm-subscription'> here </a>");
        }

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
        return configProperties;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String[] supportedModes() {
        return new String[] {"ciam","wiam"};
    }
}

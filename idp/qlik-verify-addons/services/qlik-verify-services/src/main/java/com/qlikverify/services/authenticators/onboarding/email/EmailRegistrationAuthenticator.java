package com.qlikverify.services.authenticators.onboarding.email;

import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static com.qlikverify.services.constants.Constants.*;

public class EmailRegistrationAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(EmailRegistrationAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:EmailRegistrationAuthenticator:Authenticate");

        String nextAuth = context.getAuthenticationSession().getAuthNote("next_auth");
        if(nextAuth != null && !nextAuth.equalsIgnoreCase(EmailRegistrationAuthenticatorFactory.PROVIDER_ID)) {
            context.success();
            return;
        }
        context.getAuthenticationSession().removeAuthNote("next_auth");

        //Checking MagicLink is Clicked or not using authnote
        String isMagicLinkRegistration = context.getAuthenticationSession().getAuthNote(MAGIC_LINK_ACTION);
        if (Boolean.parseBoolean(isMagicLinkRegistration)) {
            magicLinkAuthentication(context);
            return;
        }
        showView(context, null, EMAIL_REGISTRATION_FTL, null);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:EmailRegistrationAuthenticator:Action");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String expiryTime   = GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG);
        String email        = formData.getFirst(EMAIL);
        if (Boolean.parseBoolean(formData.getFirst(FORM_EDIT))) {
            formEdit(context);
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_RESEND))) {
            formResend(context, email, expiryTime);
            return;
        }

        if (Boolean.parseBoolean(formData.getFirst(FORM_IS_EMAIL_REGISTRATION))) {
            formRegistration(context, email, expiryTime);
        } else if (Boolean.parseBoolean(formData.getFirst(FORM_IS_OTP_VERIFICATION))) {
            formOtpVerification(context, formData, email);
        }
    }

    private void formOtpVerification(AuthenticationFlowContext context, MultivaluedMap<String, String> formData, String email) {
        String sessEmail = context.getAuthenticationSession().getAuthNote(EMAIL);
        if (sessEmail == null && sessEmail.length() == 0) {
            showView(context, ErrorMappings.EMAIL_REQUIRED, EMAIL_REGISTRATION_FTL, null);
            return;
        }
        UserModel userWithEmail      = context.getSession().users().getUserByEmail(context.getRealm(),sessEmail);
        UserModel userWithUsername = context.getSession().users().getUserByUsername(context.getRealm(), sessEmail);
        String otp          = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP);
        String otpExpiry    = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY);
        context.form().setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiry);

        if (userWithEmail != null) {
             showView(context, ErrorMappings.EMAIL_REGISTERED, EMAIL_REGISTRATION_FTL, null);
            return;
        }

        if(userWithUsername != null){
            GeneralHelper.auditEvent(context, EventType.EMAIL_ONBOARDING_ERROR, "Conflict in user email and username");
            return;
        }

        if (System.currentTimeMillis() > Long.parseLong(otpExpiry)) {
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.OTP_EXPIRED))
                    .setAttribute(EMAIL, context.getAuthenticationSession().getAuthNote(EMAIL))
                    .setAttribute(OTP_LENGTH_ATTR, GeneralHelper.getConfigValue(context, EMAIL_VER_OTP_LENGTH))
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return;
        }

        if (!formData.getFirst(FORM_OTP).equals(otp)) {
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.WRONG_OTP_ERROR))
                    .setAttribute(EMAIL, context.getAuthenticationSession().getAuthNote(EMAIL))
                    .setAttribute(OTP_LENGTH_ATTR, GeneralHelper.getConfigValue(context, EMAIL_VER_OTP_LENGTH))
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, otpExpiry)
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
            return;
        }
        addUser(context, email);
    }

    private void formRegistration(AuthenticationFlowContext context, String email, String expiryTime) {
        if(TextUtils.isEmpty(email)) {
            showView(context, ErrorMappings.EMAIL_REQUIRED, EMAIL_REGISTRATION_FTL, null);
            return;
        }
        if (!GeneralHelper.isValidEmail(email)) {
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.EMAIL_INVALID))
                    .setAttribute(EMAIL, email)
                    .createForm(EMAIL_REGISTRATION_FTL);
            context.challenge(challenge);
            return;
        }

        UserModel user = context.getSession().users().getUserByEmail(context.getRealm(),email);
        if (user != null) {
            showView(context, ErrorMappings.EMAIL_REGISTERED, EMAIL_REGISTRATION_FTL, null);
            return;
        }

        context.getAuthenticationSession().setAuthNote(EMAIL, email);
        String type = GeneralHelper.getConfigValue(context, EMAIL_OPTION_CONFIG);
        if (type == null || type.length() == 0)
            type = OTP;

        if (type.equals(SKIP))
        {
            context.success();
            return;
        }
        if (type.equals(OTP)) {
            if(!GeneralHelper.checkEmailConfigured(context, true)){
                GeneralHelper.handleConfigurationError(context, EventType.EMAIL_ONBOARDING_ERROR, ERROR_FTL,EMAIL_CONFIG_MISSING_ERROR);
                return;
            }
            if (!GeneralHelper.sendEmailOTP(context, email, expiryTime)) {
                showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, EMAIL_REGISTRATION_FTL, null);
                return;
            }
            Response challenge = context.form()
                    .setAttribute(EMAIL, email)
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY))
                    .setAttribute(OTP_LENGTH_ATTR, GeneralHelper.getConfigValue(context, EMAIL_VER_OTP_LENGTH))
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
        } else if (type.equals(MAGIC_LINK)) {
            if(!GeneralHelper.checkEmailConfigured(context, true)){
                GeneralHelper.handleConfigurationError(context, EventType.EMAIL_ONBOARDING_ERROR, ERROR_FTL,EMAIL_CONFIG_MISSING_ERROR);
                return;
            }
            context.getAuthenticationSession().setAuthNote(MAGIC_LINK_ACTION, "true");
            if (!GeneralHelper.sendEmailMagicLink(context, email, GeneralHelper.getConfigValue(context, VERIFICATION_EXPIRY_CONFIG))) {
                showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, EMAIL_REGISTRATION_FTL, null);
            } else {
                context.forceChallenge(context.form()
                        .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_MAGIC_LINK_EXPIRY))
                        .createForm(VIEW_EMAIL_FTL));
            }
        } else if (type.equals(WEBAUTHN_PASSWORDLESS)) {
            addUser(context, email);
        }
        else if (type.equals(USER_INVITE)) {
            addUser(context, email);
        }
    }

    private void formEdit(AuthenticationFlowContext context) {
        Response challenge = context.form()
                .setAttribute(OLD_EMAIL, context.getAuthenticationSession().getAuthNote(EMAIL))
                .createForm(EMAIL_REGISTRATION_FTL);
        context.challenge(challenge);
    }

    private void formResend(AuthenticationFlowContext context, String email, String expiryTime) {
        if(!GeneralHelper.checkEmailConfigured(context, true)){
            GeneralHelper.handleConfigurationError(context, EventType.EMAIL_ONBOARDING_ERROR, ERROR_FTL,EMAIL_CONFIG_MISSING_ERROR);
            return;
        }
        if (!GeneralHelper.sendEmailOTP(context, email, expiryTime)) {
            showView(context, ErrorMappings.FAILED_TO_SEND_EMAIL, EMAIL_REGISTRATION_FTL, null);
        } else {
            Response challenge = context.form()
                    .setAttribute(EMAIL, context.getAuthenticationSession().getAuthNote(EMAIL))
                    .setAttribute(EXPIRY_TIME_ATTRIBUTE, context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_OTP_EXPIRY))
                    .setAttribute(OTP_LENGTH_ATTR, GeneralHelper.getConfigValue(context, EMAIL_VER_OTP_LENGTH))
                    .createForm(OTP_VERIFICATION_FTL);
            context.challenge(challenge);
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    private void showView(AuthenticationFlowContext context, String error, String ftl, String expiry) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
            logger.info("User Already onboarded. Try to Login.");
        }

        if (expiry != null && expiry.length() > 0)
            formsProvider.setAttribute(EXPIRY_TIME_ATTRIBUTE, expiry);

        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    private void magicLinkAuthentication(AuthenticationFlowContext context) {
        logger.info("QlikVerify:EmailRegistrationAuthenticator:MagicLinkAuthentication");

        String sessionKey = context.getAuthenticationSession().getAuthNote("email-key");
        if (sessionKey != null) {
            String requestKey = context.getUriInfo().getQueryParameters().getFirst("key");
            if (requestKey != null) {
                if (requestKey.equals(sessionKey)) {
                    String linkExpiry = context.getAuthenticationSession().getAuthNote(SESSION_EMAIL_MAGIC_LINK_EXPIRY);
                    if (System.currentTimeMillis() > Long.parseLong(linkExpiry)) {
                        GeneralHelper.redirectToError(context, ErrorMappings.MAGIC_LINK_EXPIRED);
                    } else {
                        UserModel userByEmail = null;
                        String email = context.getAuthenticationSession().getAuthNote(EMAIL);
                        if (email == null && email.length() == 0) {
                            GeneralHelper.redirectToError(context, ErrorMappings.EMAIL_REQUIRED);
                            return;
                        }

                        userByEmail = context.getSession().users().getUserByEmail(context.getRealm(),email);

                        //checking user already present or not
                        if (userByEmail != null) {
                            GeneralHelper.redirectToError(context, ErrorMappings.EMAIL_REGISTERED);
                            return;
                        }
                        //if not addUser is called
                        addUser(context, email);
                    }
                } else {
                    GeneralHelper.redirectToError(context, ErrorMappings.MAGIC_LINK_NOT_VALID);
                }
            } else {
                GeneralHelper.redirectToError(context, ErrorMappings.MAGIC_LINK_NOT_VALID);
            }
        } else {
            GeneralHelper.redirectToError(context, ErrorMappings.SOMETHING_WENT_WRONG);
        }
    }

    private void addUser(AuthenticationFlowContext context, String email) {
        addUserOnly(context, email);
        context.success();
    }

    private void addUserOnly(AuthenticationFlowContext context, String email) {
        UserModel isUserPresent = context.getSession().users().getUserByEmail(context.getRealm(),email);
        if (isUserPresent != null) {
            logger.error(ErrorMappings.USER_EXISTS_SAME_EMAIL + email);
            GeneralHelper.redirectToError(context, ErrorMappings.USER_EXISTS_SAME_EMAIL);
            return;
        }

        //if user is set on context, then we ignore the addUser
        if(context.getUser() == null){
            UserModel user = context.getSession().users().addUser(context.getRealm(), email);
            if(context.getAuthenticationSession().getAuthNote("username") != null){
                user.setUsername(context.getAuthenticationSession().getAuthNote("username"));
            }
            user.setEnabled(true);
            context.setUser(user);
        }

        context.getUser().setEmail(email);
        context.getUser().setEmailVerified(true);
    }

}

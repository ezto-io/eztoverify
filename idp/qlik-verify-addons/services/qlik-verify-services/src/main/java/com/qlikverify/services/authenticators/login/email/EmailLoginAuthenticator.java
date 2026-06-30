package com.qlikverify.services.authenticators.login.email;

import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static com.qlikverify.services.constants.Constants.*;


public class EmailLoginAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(EmailLoginAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:EmailFormAuthenticator:Authenticate");

        //Check user is disabled by brute force attack
        if (context.getUser() != null && GeneralHelper.isTemporarilyDisabledByBruteForce(context, context.getUser(), null))
            return;

        //Checking MagicLink is Clicked or not using authnote
        String isMagicLinkRegistration = context.getAuthenticationSession().getAuthNote(MAGIC_LINK_ACTION);
        if (Boolean.parseBoolean(isMagicLinkRegistration)) {
            magicLinkAuthentication(context);
            return;
        }

        showView(context, null, null, EMAIL_FORM_FTL);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:EmailFormAuthenticator:Action");

        //Check user is disabled by brute force attack
        if (context.getUser() != null && GeneralHelper.isTemporarilyDisabledByBruteForce(context, context.getUser(), null))
            return;

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String email = formData.getFirst(EMAIL);
        if(TextUtils.isEmpty(email)) {
            showView(context, ErrorMappings.EMAIL_REQUIRED, null, EMAIL_FORM_FTL);
            return;
        }
        if (!GeneralHelper.isValidEmail(email)) {
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.EMAIL_INVALID))
                    .setAttribute(OLD_EMAIL, email)
                    .createForm(EMAIL_FORM_FTL);
            context.challenge(challenge);
            return;
        }

        UserModel userModel = context.getSession().users().getUserByEmail(context.getRealm(), email);
        if (userModel == null) {
            logger.info("Email you entered is not registered,Please register before login");
            showView(context, ErrorMappings.EMAIL_NOT_REGISTERED, email, EMAIL_FORM_FTL);
            return;
        }

        boolean isUserEmailVerified = GeneralHelper.isUserEmailVerified(userModel, context.getRealm());

        if(!isUserEmailVerified){
                logger.info("Email is not verified. Make the email verified for user");
                GeneralHelper.redirectToErrorPage(context, ErrorMappings.EMAIL_NOT_VERIFIED);
                return;
        }

        //Here we check if there is user in context that should match the provided email
        if (context.getUser() != null && !context.getUser().getEmail().equals(email)) {
            Response challenge = context.form()
                    .addError(new FormMessage(ErrorMappings.EMAIL_INVALID))
                    .createForm(EMAIL_FORM_FTL);
            context.challenge(challenge);
            return;
        }



        String checkUserState = validateUser(context, userModel);
        if (!checkUserState.equals(OK)) {
            GeneralHelper.redirectToErrorPage(context, checkUserState);
            return;
        }

        context.setUser(userModel);
        context.getAuthenticationSession().setAuthNote(LOGIN_WITH, EMAIL);
        context.getAuthenticationSession().setAuthNote(LOGIN_VALUE, email);
        context.success();
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

    private void showView(AuthenticationFlowContext context, String error, String email, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }
        if (email != null && email.length() > 0) {
            formsProvider.setAttribute(EMAIL, email);
        }
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    private String validateUser(AuthenticationFlowContext context, UserModel user) {
        if (!user.isEnabled()) {
            context.getEvent().user(user);
            context.getEvent().error(USER_DISABLED);
            return USER_DISABLED;
        }
        if (context.getRealm().isBruteForceProtected() && context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user)) {
            context.getEvent().user(user);
            context.getEvent().error(USER_DISABLED_BRUTEFORCE);
            return USER_DISABLED_BRUTEFORCE;
        }
        return OK;
    }
}

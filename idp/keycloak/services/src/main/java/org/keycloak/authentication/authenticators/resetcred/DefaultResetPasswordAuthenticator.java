package org.keycloak.authentication.authenticators.resetcred;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.utils.Constants;
import org.keycloak.authentication.authenticators.utils.GeneralHelper;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.concurrent.TimeUnit;

public class DefaultResetPasswordAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(DefaultResetPasswordAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:DefaultResetPasswordAuthenticator:Authenticate");

        String existingUserId = context.getAuthenticationSession().getAuthNote(AbstractIdpAuthenticator.EXISTING_USER_INFO);
        if (existingUserId != null) {
            UserModel existingUser = AbstractIdpAuthenticator.getExistingUser(context.getSession(), context.getRealm(), context.getAuthenticationSession());

            logger.debugf("Forget-password triggered when reauthenticating user after first broker login. Prefilling reset-credential-choose-user screen with user '%s' ", existingUser.getUsername());
            context.setUser(existingUser);
            showView(context, null, Constants.LOGIN_RESET_PASSWORD_FTL);
            return;
        }

        String actionTokenUserId = context.getAuthenticationSession().getAuthNote(DefaultActionTokenKey.ACTION_TOKEN_USER_ID);
        if (actionTokenUserId != null) {
            UserModel existingUser = context.getSession().users().getUserById(actionTokenUserId, context.getRealm());

            // Action token logics handles checks for user ID validity and user being enabled
            logger.debugf("Forget-password triggered when reauthenticating user after authentication via action token. Skipping reset-credential-choose-user screen and using user '%s' ", existingUser.getUsername());
            context.setUser(existingUser);
            context.success();
            return;
        }

        showView(context, null, Constants.LOGIN_RESET_PASSWORD_FTL);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:DefaultResetPasswordAuthenticator:Action");

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String email = formData.getFirst("email");
        if (email == null || email.length() == 0) {
            showView(context, Constants.EMAIL_REQUIRED,Constants.LOGIN_RESET_PASSWORD_FTL);
            return;
        } else if (!GeneralHelper.isValidEmail(email)) {
            Response challenge = context.form()
                    .addError(new FormMessage(Constants.EMAIL_INVALID))
                    .createForm(Constants.LOGIN_RESET_PASSWORD_FTL);
            context.challenge(challenge);
            return;
        }

        UserModel userModel = context.getSession().users().getUserByEmail(email, context.getRealm());

        if (userModel == null) {
            context.forceChallenge(context.form()
                            .setAttribute("email", email)
                    .createForm(Constants.RESET_PASSWORD_EMAIL_LINK_SENT_FTL));
            return;
        } else {
            context.setUser(userModel);

            if (!sendResetPasswordEmail(context)){
                GeneralHelper.redirectToErrorPage(context, Constants.FAILED_TO_SEND_RESET_EMAIL);
                return;
            }
            context.clearUser();
            context.forceChallenge(context.form()
                    .setAttribute("email", email)
                    .createForm(Constants.RESET_PASSWORD_EMAIL_LINK_SENT_FTL));
            return;
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }

    private void showView(AuthenticationFlowContext context, String error, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }

        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    private boolean sendResetPasswordEmail(AuthenticationFlowContext context ){
        UserModel user = context.getUser();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();

        String username = authenticationSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);

        // we don't want people guessing usernames, so if there was a problem obtaining the user, the user will be null.
        // just reset login for with a success message
        if (user == null) {
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return false;
        }


        EventBuilder event = context.getEvent();
        // we don't want people guessing usernames, so if there is a problem, just continuously challenge
        if (user.getEmail() == null || user.getEmail().trim().length() == 0) {
            event.user(user)
                    .detail(Details.USERNAME, username)
                    .error(Errors.INVALID_EMAIL);

            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return false;
        }

        int validityInSecs = context.getRealm().getActionTokenGeneratedByUserLifespan(ResetCredentialsActionToken.TOKEN_TYPE);
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

        // We send the secret in the email in a link as a query param.
        String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authenticationSession).getEncodedId();
        ResetCredentialsActionToken token = new ResetCredentialsActionToken(user.getId(),user.getEmail(), absoluteExpirationInSecs, authSessionEncodedId, authenticationSession.getClient().getClientId());
        String link = UriBuilder
                .fromUri(context.getActionTokenUrl(token.serialize(context.getSession(), context.getRealm(), context.getUriInfo())))
                .build()
                .toString();
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes(validityInSecs);
        try {
            context.getSession().getProvider(EmailTemplateProvider.class)
                    .setRealm(context.getRealm()).setUser(user)
                    .setAuthenticationSession(authenticationSession)
                    .sendPasswordReset(link, expirationInMinutes);

            event.clone().event(EventType.SEND_RESET_PASSWORD)
                    .user(user)
                    .detail(Details.USERNAME, username)
                    .detail(Details.EMAIL, user.getEmail()).detail(Details.CODE_ID, authenticationSession.getParentSession().getId()).success();
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return true;
        } catch (EmailException e) {
            event.clone().event(EventType.SEND_RESET_PASSWORD)
                    .detail(Details.USERNAME, username)
                    .user(user)
                    .error(Errors.EMAIL_SEND_FAILED);
            ServicesLogger.LOGGER.failedToSendPwdResetEmail(e);
            return false;
        }
    }
}

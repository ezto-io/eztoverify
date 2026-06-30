package com.qlikverify.services.authenticators.login.username;

import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import com.qlikverify.services.helpers.GeneralHelper;
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

public class UsernameLoginAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(UsernameLoginAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:UsernameFormAuthenticator:Authenticate");
        showView(context, null);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("QlikVerify:UsernameFormAuthenticator:Action");

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String userId = formData.getFirst(USERNAME);
        if (userId == null || userId.isEmpty()) {
            showView(context, ErrorMappings.USERNAME_REQUIRED);
            return;
        }

        UserModel user = context.getSession().users().getUserByUsername(context.getRealm(),userId);
        if (user == null) {
            showView(context, Constants.USER_INVALID);
            return;
        }
        String checkUserState = validateUserName(context, user);
        if (!checkUserState.equals(Constants.OK)) {
            GeneralHelper.redirectToErrorPage(context, checkUserState);
            return;
        }

        context.setUser(user);
        context.getAuthenticationSession().setAuthNote(LOGIN_WITH, USERNAME);
        context.getAuthenticationSession().setAuthNote(LOGIN_VALUE, userId);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    private void showView(AuthenticationFlowContext context, String error) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
        }
        Response challenge = formsProvider.createForm(Constants.USERNAME_FTL);
        context.challenge(challenge);
    }

    private String validateUserName(AuthenticationFlowContext context, UserModel user) {
        if (!user.isEnabled()) {
            context.getEvent().user(user);
            context.getEvent().error(Constants.USER_DISABLED);
            return Constants.USER_DISABLED;
        }
        if (context.getRealm().isBruteForceProtected() && context.getProtector().isTemporarilyDisabled(context.getSession(), context.getRealm(), user)) {
            context.getEvent().user(user);
            context.getEvent().error(Constants.USER_DISABLED_BRUTEFORCE);
            return Constants.USER_DISABLED_BRUTEFORCE;
        }
        return Constants.OK;
    }


}

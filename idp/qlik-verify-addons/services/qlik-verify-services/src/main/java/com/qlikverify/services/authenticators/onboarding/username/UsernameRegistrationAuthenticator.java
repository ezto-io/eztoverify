package com.qlikverify.services.authenticators.onboarding.username;

import com.qlikverify.services.authenticators.onboarding.email.EmailRegistrationAuthenticator;
import com.qlikverify.services.authenticators.onboarding.email.EmailRegistrationAuthenticatorFactory;
import com.qlikverify.services.constants.Constants;
import com.qlikverify.services.constants.ErrorMappings;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class UsernameRegistrationAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(UsernameRegistrationAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("Entered UsernameRegistrationAuthenticator : authenticate");
        String nextAuth = context.getAuthenticationSession().getAuthNote("next_auth");
        if(nextAuth != null && !nextAuth.equalsIgnoreCase(UsernameRegistrationAuthenticatorFactory.PROVIDER_ID)) {
            context.success();
            return;
        }
        context.getAuthenticationSession().removeAuthNote("next_auth");

        Response challenge = context.form().createForm(Constants.USERNAME_REG_FTL);
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("Entered UsernameRegistrationAuthenticator : action");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String userName = formData.getFirst(Constants.USERNAME);
        UserModel userModel = context.getSession().users().getUserByUsername(userName, context.getRealm());

        if(userModel != null){
            Response challenge = context.form()
                    .setAttribute(ErrorMappings.ERROR, "usernameExistsMessage")
                    .setAttribute(Constants.USERNAME, userName)
                    .createForm(Constants.USERNAME_REG_FTL);
            context.challenge(challenge);
            return;
        }
        //if user is set on context, then we ignore the addUser
        if(context.getUser() == null){
            if(context.getAuthenticationSession().getAuthNote(EmailRegistrationAuthenticatorFactory.PROVIDER_ID) != null){
                // Username will be set to the auth note and user will be created only after email verification
                context.getAuthenticationSession().setAuthNote("username",userName);
            }
            else{
                userModel = context.getSession().users().addUser(context.getRealm(), userName);
                userModel.setEnabled(true);
                context.setUser(userModel);
            }

        } else {
            context.getUser().setUsername(userName);
        }

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
}

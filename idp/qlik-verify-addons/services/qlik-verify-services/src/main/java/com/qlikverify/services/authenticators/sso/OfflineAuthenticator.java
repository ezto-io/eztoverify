package com.qlikverify.services.authenticators.sso;

import com.qlikverify.services.actiontoken.OfflineActionTokenValidation;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OfflineAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(OfflineAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("QlikVerify:OfflineAuthenticator:Authenticate");

        String token = context.getUriInfo().getQueryParameters().getFirst("sso_off_token");
        if (token == null && context.getAuthenticationSession() != null) {
            String params = context.getAuthenticationSession().getAuthNote("QUERY_PARAMS");
            if (params != null) {
                try {
                    HashMap<String, List<String>> queryParams = (HashMap<String, List<String>>) JsonSerialization.readValue(params, Map.class);
                    List<String> ssoOffToken = queryParams.get("sso_off_token");
                    if (ssoOffToken != null && !ssoOffToken.isEmpty()) {
                        token = ssoOffToken.get(0);
                    }
                } catch (IOException e) {
                    logger.error("Cant Read the SSO Offline token: " + e.getMessage());
                }
            }
        }

        if (token == null) {
            context.attempted();
            return;
        }

        try {
            JsonWebToken jsonWebToken = OfflineActionTokenValidation.validateToken(context.getSession(), token);
            if (!jsonWebToken.getSubject().equals("sso-desktop")) {
                context.attempted();
                return;
            }

            if (jsonWebToken.isExpired()) {
                context.attempted();
                return;
            }
            UserModel userModel = context.getSession().users().getUserById(context.getRealm(), jsonWebToken.getAudience()[0]);
            if (userModel == null) {
                context.attempted();
                return;
            }

            context.setUser(userModel);

            context.success();
            return;

        } catch (Exception err) {

        }

        context.attempted();

    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // NOP
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

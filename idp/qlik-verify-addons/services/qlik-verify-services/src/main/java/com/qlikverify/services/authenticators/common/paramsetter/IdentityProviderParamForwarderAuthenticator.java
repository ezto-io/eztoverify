package com.qlikverify.services.authenticators.common.paramsetter;

import com.fasterxml.jackson.core.type.TypeReference;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;

public class IdentityProviderParamForwarderAuthenticator implements Authenticator {
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        try {
           HashMap<String,String>[] hashMaps = JsonSerialization.readValue(context.getAuthenticatorConfig().getConfig().get("params"),new TypeReference<HashMap<String,String>[]>(){});
           if(hashMaps != null && hashMaps.length > 0 && context.getAuthenticationSession() != null)
           {
               for (HashMap<String,String> hashKeys : hashMaps)
               {
                   context.getAuthenticationSession().setClientNote(AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX +hashKeys.get("key").trim(),getValue(context,hashKeys.get("value")));
               }
           }
        } catch (IOException e) {
            e.printStackTrace();
        }
        context.success();
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
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    private String getValue(AuthenticationFlowContext context, String value)
    {
        switch (value.toLowerCase())
        {
            case "user.email":
                return context.getUser() != null ? context.getUser().getEmail(): "";
            case "user.firstname":
                return context.getUser() != null ? context.getUser().getFirstName(): "";
            case "user.lastname":
                return context.getUser() != null ? context.getUser().getLastName(): "";
            case "user.phone":
                return context.getUser() != null ? context.getUser().getMobileNumber(): "";
            case "user.username":
                return context.getUser() != null ? context.getUser().getUsername(): "";
            default:
                return "";
        }
    }
}

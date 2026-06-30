package com.qlikverify.services.policies.action;

import org.keycloak.Config;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;

import java.util.List;
import java.util.stream.Collectors;

public class TempPasswordPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {

    public static final String ID = "temppassword";

    @Override
    public boolean validate(KeycloakSession session,ActionPolicy policy, UserModel user) {
        List<CredentialModel> creds = session.userCredentialManager()
                                                .getStoredCredentialsStream(session.getContext().getRealm(),user)
                                                .filter(x->x.isTemporary() && x.getType().equalsIgnoreCase("password"))
                                                .collect(Collectors.toList());

        //if credentialData was userInvited, then we need to change userInviteStatus
        if(creds.size() > 0){
            creds.forEach(cred -> {
                if(cred.getUserLabel() != null && cred.getUserLabel().equalsIgnoreCase("userInvited")){
                    user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Accepted");
                }
            });
        }

        return creds.size() > 0;
    }

    @Override
    public List<String> parseConfig(String value) {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "is-temp-password";
    }

    @Override
    public String getConfigType() {
        return null;
    }

    @Override
    public String getDefaultConfigValue() {
        return "";
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public ActionPolicyProvider create(KeycloakSession session) {
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
        return ID;
    }
}

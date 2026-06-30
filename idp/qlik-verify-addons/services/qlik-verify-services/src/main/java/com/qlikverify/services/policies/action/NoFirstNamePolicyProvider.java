package com.qlikverify.services.policies.action;

import org.keycloak.Config;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;

public class NoFirstNamePolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {
    public static final String ID = "nofirstname";

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        String userFirstName = user.getFirstName();
        return userFirstName == null || userFirstName.isEmpty();
    }

    @Override
    public Object parseConfig(String value) {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "no-firstname";
    }

    @Override
    public String getConfigType() {
        return null;
    }

    @Override
    public String getDefaultConfigValue() {
        return null;
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

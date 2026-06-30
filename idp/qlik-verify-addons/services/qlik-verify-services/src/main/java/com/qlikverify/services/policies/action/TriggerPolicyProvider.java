package com.qlikverify.services.policies.action;

import org.keycloak.Config;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;

public class TriggerPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {

    public static final String ID = "triggerpolicy";

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        Object policyConfig = policy.getPolicyConfig(ID);

        if (policyConfig == null)
            return true;

        return (boolean) policyConfig;
    }

    @Override
    public Object parseConfig(String value) {
        return Boolean.parseBoolean(value);
    }

    @Override
    public String getDisplayName() {
        return "trigger-policy";
    }

    @Override
    public String getConfigType() {
        return null;
    }

    @Override
    public String getDefaultConfigValue() {
        return "true";
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

package com.qlikverify.services.policies.action;

import org.keycloak.Config;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;

import java.util.List;

public class CustomUserMetadataPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {
    public static final String ID = "customusermetadata";

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        List<String> policyConfig = policy.getPolicyConfig(ID);
        if(policyConfig == null || policyConfig.isEmpty())
            return false;

        String attrValue = user.getFirstAttribute(policyConfig.get(0));
        if(attrValue == null || attrValue.isEmpty())
            return false;

        return attrValue.equals(policyConfig.get(1));
    }

    @Override
    public Object parseConfig(String value) {
        return value.split("=");
    }

    @Override
    public String getDisplayName() {
        return "custom-user-metadata";
    }

    @Override
    public String getConfigType() {
        return ActionPolicyProvider.STRING_CONFIG_TYPE;
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

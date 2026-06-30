package com.qlikverify.services.policies.action;

import org.keycloak.Config;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;


public class TenantOnboardingPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {
    private static final String QV_IS_ACCOUNT_CREATOR = "qv_is_account_creator";
    public static final String ID = "tenantOnboardingPolicy";

    @Override
    public String getDisplayName() {
        return "tenant-onboarding-policy";
    }

    @Override
    public String getConfigType() {
        return null;
    }

    @Override
    public String getDefaultConfigValue() {
        return "false";
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        Object policyConfig = policy.getPolicyConfig(ID);
        if (policyConfig == null)
            return false;
        return user.getFirstAttribute(QV_IS_ACCOUNT_CREATOR) != null &&
                user.getFirstAttribute(QV_IS_ACCOUNT_CREATOR).equalsIgnoreCase("true") && session.onBoarding().isOnboardingCompleted();

    }

    @Override
    public Object parseConfig(String value) {
        return Boolean.parseBoolean(value);
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

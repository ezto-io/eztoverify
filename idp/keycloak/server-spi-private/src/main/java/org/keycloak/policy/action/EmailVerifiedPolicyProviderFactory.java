package org.keycloak.policy.action;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.policy.username.UsernamePolicyProvider;
import org.keycloak.policy.username.UsernamePolicyProviderFactory;

public class EmailVerifiedPolicyProviderFactory implements ActionPolicyProviderFactory {

    public static final String ID = "isemailverified";
    @Override
    public ActionPolicyProvider create(KeycloakSession session) {
        return new EmailVerifiedPolicyProvider(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "email-verified-policy";
    }

    @Override
    public String getConfigType() {
        return ActionPolicyProvider.BOOLEAN_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return "false";
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }
}

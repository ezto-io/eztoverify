package com.qlikverify.services.policies.action;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;

import static com.qlikverify.services.constants.Constants.ATTRIBUTE_PHONE_VERIFIED;

public class VerifyPhoneNumberPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {
    public static final String ID = "phoneNumberverify";
    private static final Logger logger = Logger.getLogger(VerifyPhoneNumberPolicyProvider.class);
    @Override
    public String getDisplayName() {
        return "phone-number-verified";
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
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        return user.getMobileNumber() == null ||
                user.getFirstAttribute(ATTRIBUTE_PHONE_VERIFIED) == null ||
                !Boolean.parseBoolean(user.getFirstAttribute(ATTRIBUTE_PHONE_VERIFIED));
    }

    @Override
    public Object parseConfig(String value) {
        return null;
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

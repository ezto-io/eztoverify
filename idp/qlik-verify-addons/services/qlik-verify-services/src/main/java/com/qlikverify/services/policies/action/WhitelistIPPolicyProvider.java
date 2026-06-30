package com.qlikverify.services.policies.action;

import com.qlikverify.services.helpers.GeneralHelper;
import org.keycloak.Config;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;

import java.util.Arrays;
import java.util.List;

public class WhitelistIPPolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {

    public static final String ID = "ipwhitelist";

    @Override
    public boolean validate(KeycloakSession session,ActionPolicy policy, UserModel user) {
        List<String> policyConfig = policy.getPolicyConfig(ID);
        if(policyConfig == null || policyConfig.isEmpty())
            return false;

        String ipAddress = GeneralHelper.getInstance().getRemoteAddress(session);
        if(ipAddress == null || ipAddress.isEmpty())
            return false;

        return policyConfig.contains(ipAddress);
    }

    @Override
    public List<String> parseConfig(String value) {
        return Arrays.asList(value.split(";"));
    }

    @Override
    public String getDisplayName() {
        return "ip-whitelist";
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

    @Override
    public String[] supportedModes() {
        return new String[] {"wiam", "ciam", "bo"};
    }
}

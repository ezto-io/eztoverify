package com.qlikverify.services.policies.action;

import org.keycloak.Config;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;

import java.util.List;
import java.util.stream.Collectors;

public class HasRolePolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {

    public static final String ID = "hasrolepolicy";

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        String policyConfig = policy.getPolicyConfig(ID);

        if (policyConfig == null || policyConfig.isEmpty())
            return true;


        RoleModel roleModel=  session.getContext().getRealm().getRole(policyConfig);
        if(roleModel == null)
            return true;

        return user.hasRole(roleModel);
    }

    @Override
    public Object parseConfig(String value) {
        return value;
    }

    @Override
    public List<String> getList(KeycloakSession session) {
        return session.getContext().getRealm().getRolesStream().map(x->x.getName()).collect(Collectors.toList());
    }

    @Override
    public String getDisplayName() {
        return "has-role";
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

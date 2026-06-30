package org.keycloak.policy.username;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UniqueUsernamePolicyProviderFactory implements UsernamePolicyProviderFactory{

    public static final String ID = "uniqueusername";
    @Override
    public UsernamePolicyProvider create(KeycloakSession session) {
        return new UniqueUsernamePolicyProvider(session);
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
        return "kc-unique-username-policy";
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
}

package org.keycloak.authentication.requiredactions;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class DeleteCredentialActionFactory implements RequiredActionFactory {

    public static final String PROVIDER_ID = "delete_credential";

    @Override
    public String getDisplayText() {
        return "Delete Credential";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public String[] supportedModes() {
        return new String[] {"ciam","wiam"};
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new DeleteCredentialAction();
    }

    @Override
    public void init(Config.Scope config) {
        // NO OP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NO OP
    }

    @Override
    public void close() {
        // NO OP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

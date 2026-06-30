package com.qlikverify.backoffice.authenticators;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class CibaAuthenticatorFactory implements AuthenticatorFactory {
    @Override
    public boolean isInBuilt() {
        return true;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.LOGIN;
    }

    @Override
    public String getArticleId() {
        return null;
    }

    @Override
    public String getDisplayType() {
        return null;
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[0];
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return null;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new CibaAuthenticator(session);
    }

    @Override
    public void init(Config.Scope config) {
      // Nothing to be done as of now
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to be done as of now
    }

    @Override
    public void close() {
        // Nothing to be done as of now
    }

    @Override
    public String getId() {
        return "ciba-authenticator";
    }
}

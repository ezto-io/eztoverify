package org.keycloak.authentication.authenticators.resetcred;


import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class DefaultResetPasswordAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "default-reset-password-authenticator";
    public static final DefaultResetPasswordAuthenticator SINGLETON = new DefaultResetPasswordAuthenticator();

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
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
        return PROVIDER_ID;
    }

    @Override
    public String getReferenceCategory() {
        return "Default Reset Password";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "Default Reset Password Flow";
    }

    @Override
    public String getHelpText() {
        return "Default Reset Password Flow triggers here";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return null;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public boolean isInBuilt() {
        return true;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.RECOVERY;
    }
    
    @Override
    public String getArticleId() {
        return null;
    }
}

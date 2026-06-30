package org.keycloak.protocol.docker;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

import static org.keycloak.models.AuthenticationExecutionModel.Requirement;

public class DockerAuthenticatorFactory implements AuthenticatorFactory {

    @Override
    public String getHelpText() {
        return "docker-login-helptext";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return Collections.emptyList();
    }

    @Override
    public String getDisplayType() {
        return "docker-login";
    }

    @Override
    public String getReferenceCategory() {
        return "docker";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    private static final Requirement[] REQUIREMENT_CHOICES = {
            Requirement.REQUIRED,
    };

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }


    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new DockerAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return DockerAuthenticator.ID;
    }

    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.LOGIN;
    }

    @Override
    public String getArticleId() {
        return "84000147340";
    }
}

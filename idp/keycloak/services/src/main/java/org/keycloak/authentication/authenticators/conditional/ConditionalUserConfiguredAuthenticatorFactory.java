package org.keycloak.authentication.authenticators.conditional;

import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class ConditionalUserConfiguredAuthenticatorFactory implements ConditionalAuthenticatorFactory {
    public static final String PROVIDER_ID = "conditional-user-configured";
    protected static final String CONDITIONAL_USER_ROLE = "condUserConfigured";

    @Override
    public void init(Scope config) {
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
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Condition - user configured";
    }

    @Override
    public String getReferenceCategory() {
        return "condition";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    private static final Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
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
    public String getHelpText() {
        return "Executes the current flow only if authenticators are configured";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return null;
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return ConditionalUserConfiguredAuthenticator.SINGLETON;
    }

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
}

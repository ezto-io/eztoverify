package org.keycloak.authentication.authenticators.sessionlimits;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

public class UserSessionLimitsAuthenticatorFactory implements AuthenticatorFactory {
    public static final String USER_REALM_LIMIT = "userRealmLimit";
    public static final String USER_CLIENT_LIMIT = "userClientLimit";
    public static final String BEHAVIOR = "behavior";
    public static final String DENY_NEW_SESSION = "Deny new session";
    public static final String TERMINATE_OLDEST_SESSION = "Terminate oldest session";
    public static final String USER_SESSION_LIMITS = "user-session-limits";
    public static final String ERROR_MESSAGE = "errorMessage";

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getDisplayType() {
        return "user-session-count-limiter";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "user-session-count-limiter-helptext";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        ProviderConfigProperty userRealmLimit = new ProviderConfigProperty();
        userRealmLimit.setName(USER_REALM_LIMIT);
        userRealmLimit.setLabel("user-session-each-user-label");
        userRealmLimit.setHelpText("user-session-each-user-hlptxt");
        userRealmLimit.setType(ProviderConfigProperty.STRING_TYPE);
        userRealmLimit.setDefaultValue("3");

        ProviderConfigProperty userClientLimit = new ProviderConfigProperty();
        userClientLimit.setName(USER_CLIENT_LIMIT);
        userClientLimit.setLabel("user-session-each-user-keycloak-client-label");
        userClientLimit.setHelpText("user-session-each-user-keycloak-client-hlptxt");
        userClientLimit.setType(ProviderConfigProperty.STRING_TYPE);
        userClientLimit.setDefaultValue("0");
        
        ProviderConfigProperty behaviourProperty = new ProviderConfigProperty();
        behaviourProperty.setName(BEHAVIOR);
        behaviourProperty.setLabel("behavior-user-session-limit-exceeded-label");
        behaviourProperty.setType(ProviderConfigProperty.LIST_TYPE);
        behaviourProperty.setDefaultValue(DENY_NEW_SESSION);
        behaviourProperty.setOptions(Arrays.asList(DENY_NEW_SESSION, TERMINATE_OLDEST_SESSION));
        
        ProviderConfigProperty customErrorMessage = new ProviderConfigProperty();
        customErrorMessage.setName(ERROR_MESSAGE);
        customErrorMessage.setLabel("optional-custom-error-message-label");
        customErrorMessage.setHelpText("optional-custom-error-message-hlptxt");
        customErrorMessage.setType(ProviderConfigProperty.STRING_TYPE);

        return Arrays.asList(userRealmLimit, userClientLimit, behaviourProperty, customErrorMessage);
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
        return new UserSessionLimitsAuthenticator(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return USER_SESSION_LIMITS;
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
        return null;
    }
}

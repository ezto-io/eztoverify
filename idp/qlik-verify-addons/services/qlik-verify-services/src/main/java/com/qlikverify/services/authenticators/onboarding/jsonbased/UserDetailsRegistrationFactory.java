package com.qlikverify.services.authenticators.onboarding.jsonbased;

import com.qlikverify.services.constants.Constants;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserDetailsRegistrationFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {
    public static final String  ID = "user-details-reg-authenticator";
    private static final String NAME = "user-detail-registration";
    private static final UserDetailsRegistration  SINGLETON = new UserDetailsRegistration();


    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.ONBOARDING;
    }

    @Override
    public String getArticleId() {
        return null;
    }

    @Override
    public String getDisplayType() {
        return NAME;
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
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
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
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }


    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {

        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName("config_json");
        property.setLabel("config-json");
        property.setType(ProviderConfigProperty.TEXT_TYPE);
        property.setHelpText("config-json-helptext");

        configProperties.add(property);
    }
}

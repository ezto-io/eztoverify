package com.qlikverify.services.authenticators.onboarding.password;


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
import java.util.List;

public class PasswordRegisterAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "password-register-authenticator";
    private static final PasswordRegisterAuthenticator SINGLETON = new PasswordRegisterAuthenticator();
    private static final String PROVIDER_NAME = "password-register-title";

    @Override
    public String getDisplayType() {
        return PROVIDER_NAME;
    }

    @Override
    public String getReferenceCategory() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

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
        return PROVIDER_NAME;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope scope) {
        // NOP
     }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


   static {

       ProviderConfigProperty configProperty;
       configProperty = new ProviderConfigProperty();
       configProperty.setName(Constants.PASS_POLICY);
       configProperty.setLabel(Constants.PASS_POLICY_LABEL);
       configProperty.setType(ProviderConfigProperty.POLICY_LIST_TYPE);
       configProperty.setDefaultValue("default_password_policy");
       configProperty.setHelpText("password-policy-hlp-txt");
       configProperties.add(configProperty);
   }

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
        return "84000147273";
    }
}
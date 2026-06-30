package com.qlikverify.services.authenticators.common.recaptcha;

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
import java.util.Map;

public class RecaptchaAuthenticatorFactory  implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {
    public static final String CAPTCHA_VERSION = "captcha_version";
    public static final String SITE_KEY = "site.key";
    public static final String SITE_SECRET = "secret";
    public static final String USE_RECAPTCHA_NET = "useRecaptchaNet";
    public static final String SITE_SCORE = "site.score";
    public static final String SITE_ACTION = "recaptcha.action";
    private static final String PROVIDER_NAME = "google-recaptcha-title";

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.ALL;
    }

    @Override
    public String getArticleId() {
        return "84000147272";
    }

    @Override
    public String getDisplayType() {
        return PROVIDER_NAME;
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
        return "google-recaptcha-hlp-txt";
    }

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        List<String> versions = new ArrayList<>();
        versions.add("V2");
        //TODO: We have disabled V3 version of recaptcha as its not working as expected, this needs to be revisited later
       /* versions.add("V3");*/
        property.setName(CAPTCHA_VERSION);
        property.setRequired(true);
        property.setLabel("recaptcha-version-label");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(versions);
        property.setDefaultValue("V2");
        property.setHelpText("recaptcha-version-hlp-txt");
        CONFIG_PROPERTIES.add(property);

        property = new ProviderConfigProperty();
        property.setName(SITE_KEY);
        property.setRequired(true);
        property.setLabel("recaptcha-site-key-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("recaptcha-site-key-hlp-txt");
        CONFIG_PROPERTIES.add(property);

        property = new ProviderConfigProperty();
        property.setName(SITE_SECRET);
        property.setRequired(true);
        property.setLabel("recaptcha-secret-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("recaptcha-secret-hlp-txt");
        CONFIG_PROPERTIES.add(property);

        property = new ProviderConfigProperty();
        property.setName(SITE_SCORE);
        property.setLabel("recaptcha-score-label");
        property.setDefaultValue(0.5);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("recaptcha-score-hlp-txt");

        Map<String, Object> recaptchaScoreDependsOn = new HashMap<>();
        recaptchaScoreDependsOn.put(CAPTCHA_VERSION,"V3");

        property.setDependsOn(recaptchaScoreDependsOn);
        CONFIG_PROPERTIES.add(property);

        property = new ProviderConfigProperty();
        property.setName(SITE_ACTION);
        property.setLabel("recaptcha-action-label");
        property.setDefaultValue("kc_registration_page");
        property.setHelpText("recaptcha-action-hlp-txt");

        Map<String, Object> recaptchaActionDependsOn = new HashMap<>();
        recaptchaActionDependsOn.put(CAPTCHA_VERSION,"V3");
        property.setDependsOn(recaptchaActionDependsOn);
        CONFIG_PROPERTIES.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return CONFIG_PROPERTIES;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new RecaptchaAuthenticator(session);
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
        return "qv-recaptcha";
    }
}

package com.qlikverify.services.eventlisteners.blacklisting;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.qlikverify.services.constants.Constants.*;

public class BlacklistingEventListenerProviderFactory implements EventListenerProviderFactory {
    @Override
    public String getDisplayName() {
        return BLACKLISTING_PROVIDER_ID;
    }

    @Override
    public String getLogo() {
        return null;
    }

    @Override
    public String getHowTodo() {
        return null;
    }

    @Override
    public boolean isInBuilt() {
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isRequiredTestConfigDetails() {
        return false;
    }

    @Override
    public boolean testConfig(KeycloakSession session, Map<String, String> config) {
        return false;
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public String getHelpText() {
        return "black-listing-listener-hlp-txt";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"ciam", "wiam"};
    }

    private List<ProviderConfigProperty> configProperties = new ArrayList<>();
    {
        ProviderConfigProperty providerConfigProperty;

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("bl-enable");
        providerConfigProperty.setLabel("bl-enable-label");
        providerConfigProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("bl-enable-hlp-txt");
        providerConfigProperty.setDefaultValue(false);
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("ip-address");
        providerConfigProperty.setLabel("ip-address-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("ip-address-hlp-txt");
        providerConfigProperty.setDefaultValue(null);
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("email");
        providerConfigProperty.setLabel("email-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("email-hlp-txt");
        providerConfigProperty.setDefaultValue(null);
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("phone");
        providerConfigProperty.setLabel("phone-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("phone-hlp-txt");
        providerConfigProperty.setDefaultValue(null);
        configProperties.add(providerConfigProperty);

        providerConfigProperty = new ProviderConfigProperty();
        providerConfigProperty.setName("username");
        providerConfigProperty.setLabel("username-label");
        providerConfigProperty.setType(ProviderConfigProperty.STRING_TYPE);
        providerConfigProperty.setRequired(false);
        providerConfigProperty.setHelpText("username-hlp-txt");
        providerConfigProperty.setDefaultValue(null);
        configProperties.add(providerConfigProperty);
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new BlacklistingEventListenerProvider(session, this);
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
        return BLACKLISTING_PROVIDER_ID;
    }

}

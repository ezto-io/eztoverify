package com.qlikverify.services.emails.sendgrid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.services.helpers.GeneralHelper;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.email.*;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SendGridEmailSenderProviderFactory implements EmailSenderProviderFactory {

    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public String getDisplayName() {
        return "sendgrid-title";
    }

    @Override
    public String getProviderClass() {
        return (EmailSenderProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "sendgrid.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/Email-Provider/Sendgrid.md";
    }

    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isRequiredTestConfigDetails() {
        return true;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config) {
        return GeneralHelper.validateConfig(getConfigProperties(session), config);
    }

    @Override
    public ConfigTestResponse testConfig(KeycloakSession session, UserModel userModel, Map<String, String> config) {
        try {
            session.getProvider(EmailTemplateProvider.class).setRealm(session.getContext().getRealm()).setEmailProviderId(getId()).sendSmtpTestEmail(config, userModel, config.get("emailId"));
            return new ConfigTestResponse(true, null);
        } catch (EmailException e) {
            return new ConfigTestResponse(false, handleSendGridError(e.getMessage()));
        } catch (ConfigException e) {
            EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return new ConfigTestResponse(false, handleSendGridError(e.getMessage()));
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        return new SendGridEmailSenderProvider(session, this);
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
        return "sendgrid";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("FROM");
        configProperty.setLabel("from-label");
        configProperty.setRequired(true);
        configProperty.setType(ProviderConfigProperty.EMAIL);
        configProperty.setHelpText("sendgrid-sender-email-addr");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("SEND_GRID_API_KEY");
        configProperty.setLabel("api-key-label");
        configProperty.setSecret(true);
        configProperty.setRequired(true);
        configProperty.setType(ProviderConfigProperty.PASSWORD);
        configProperty.setHelpText("sendgrid-api-key-hlp-txt");

        configProperties.add(configProperty);

    }

    private String handleSendGridError(String error) {
        if (error == null || error.isEmpty()) {
            return "";
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            SendGridError sendGridError = objectMapper.readValue(error, SendGridError.class);
            return sendGridError.getErrors().get(0).get("message");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}

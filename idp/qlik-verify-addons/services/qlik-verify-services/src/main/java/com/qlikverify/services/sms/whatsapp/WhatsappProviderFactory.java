package com.qlikverify.services.sms.whatsapp;

import com.qlikverify.services.helpers.GeneralHelper;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.sms.SmsProvider;
import org.keycloak.sms.SmsProviderFactory;
import org.keycloak.sms.SmsType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WhatsappProviderFactory implements SmsProviderFactory {

    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public String getDisplayName() {
        return "Whatsapp";
    }

    @Override
    public String getProviderClass() {
        return (SmsProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "whatsapp.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL +"/Setup-Guide/SMS-Provider/Whatsapp.md";
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
        return GeneralHelper.validateConfig(getConfigProperties(session),config);
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("from");
        configProperty.setLabel("whatsapp-phone-num");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("accountsid");
        configProperty.setLabel("whatsapp-account-sid");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("authtoken");
        configProperty.setLabel("whatsapp-auth-token");
        configProperty.setType(ProviderConfigProperty.PASSWORD);
        configProperty.setRequired(true);
        configProperty.setSecret(true);
        configProperties.add(configProperty);


        configProperty = new ProviderConfigProperty();
        configProperty.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        configProperty.setHelpText("whatsapp-sid-warn");
        configProperties.add(configProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public String getHelpText() {
        return "whatsapp-powered-by";
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public SmsType getType() {
        return SmsType.PREMIUM;
    }


    @Override
    public SmsProvider create(KeycloakSession session) {
        return new WhatsappProvider(session);
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
        return "whatsapp";
    }
}

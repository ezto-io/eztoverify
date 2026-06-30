package com.qlikverify.services.sms.twilio;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwilioProviderFactory implements SmsProviderFactory{

    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public SmsProvider create(KeycloakSession session) {
        return new TwilioProvider(session);
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
        return "twilio";
    }


    @Override
    public String getDisplayName() {
        return "Twilio";
    }

    @Override
    public String getProviderClass() {
        return (SmsProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "Twilio.png";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/SMS-Provider/Twilio.md";
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

    public static final String ENABLEDISPLAYNAME = "enabledisplayname";

    public static final String DISPLAYNAME = "displayname";

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("from");
        configProperty.setLabel("twilio-phone-num");
        configProperty.setType(ProviderConfigProperty.FROM_MOBILE_NUMBER);
        configProperty.setRequired(true);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ENABLEDISPLAYNAME);
        configProperty.setLabel("enable-display-name");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setRequired(false);
        configProperty.setHelpText("enable-display-name-hlp-txt");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(DISPLAYNAME);
        configProperty.setLabel("display-name");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperty.setHelpText("display-name-hlp-txt");
        configProperty.setDependsOn(new HashMap<>(){{
            put(ENABLEDISPLAYNAME,true);
        }});
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("callurl");
        configProperty.setLabel("twilio-is-call-supported");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("accountsid");
        configProperty.setLabel("twilio-account-sid");
        configProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperty.setRequired(true);
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("authtoken");
        configProperty.setLabel("twilio-auth-token");
        configProperty.setType(ProviderConfigProperty.PASSWORD);
        configProperty.setRequired(true);
        configProperty.setSecret(true);
        configProperties.add(configProperty);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
       return configProperties;
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public SmsType getType() {
        return SmsType.NORMAL;
    }
}

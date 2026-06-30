package com.qlikverify.services.emails.amazonses;

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

public class AmazonSESEmailSenderProviderFactory implements EmailSenderProviderFactory {

    private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public String getDisplayName() {
        return "amazon-ses-title";
    }

    @Override
    public String getProviderClass() {
        return (EmailSenderProviderFactory.class).getSimpleName();
    }

    @Override
    public String getLogo() {
        return "aws.svg";
    }

    @Override
    public String getHowTodo() {
        return DEFAULT_SERVER_URL + "/Setup-Guide/Email-Provider/Amazon-SES.md";
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
    public List<String> validateConfig(KeycloakSession session,Map<String, String> config) {
        return GeneralHelper.validateConfig(getConfigProperties(session), config);
    }

    @Override
    public ConfigTestResponse testConfig(KeycloakSession session, UserModel userModel, Map<String, String> config) {
        try {
            session.getProvider(EmailTemplateProvider.class).setRealm(session.getContext().getRealm()).setEmailProviderId(getId()).sendSmtpTestEmail(config, userModel, config.get("emailId"));
            return new ConfigTestResponse(true,null);
        } catch (EmailException e) {
            return new ConfigTestResponse(false,e.getMessage());
        }
        catch (ConfigException e)
        {
            EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(),session,session.getContext().getConnection());
            eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
            eventBuilder.error(e.getMessage());
            return new ConfigTestResponse(false, e.getMessage());
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        return new AmazonSESEmailSenderProvider(session,this);
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
        return "amazon-ses";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName("FROM");
        configProperty.setLabel("from-label");
        configProperty.setRequired(true);
        configProperty.setType(ProviderConfigProperty.EMAIL);
        configProperty.setHelpText("amazonses-sender-email-addr");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("AWS_ACCESS_ID");
        configProperty.setLabel("aws-access-key-id-label");
        configProperty.setRequired(true);
        configProperty.setSecret(true);
        configProperty.setType(ProviderConfigProperty.PASSWORD);
        configProperty.setHelpText("aws-access-key-id-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("AWS_SECRET_ACCESS_KEY");
        configProperty.setLabel("aws-secret-access-key-label");
        configProperty.setRequired(true);
        configProperty.setSecret(true);
        configProperty.setType(ProviderConfigProperty.PASSWORD);
        configProperty.setHelpText("aws-secret-access-key-hlp-txt");

        configProperties.add(configProperty);

        List<String> regions = new ArrayList<>();
        regions.add("us-east-2");
        regions.add("us-east-1");
        regions.add("us-west-1");
        regions.add("us-west-2");
        regions.add("af-south-1");
        regions.add("ap-east-1");
        regions.add("ap-south-1");
        regions.add("ap-northeast-3");
        regions.add("ap-northeast-2");
        regions.add("ap-southeast-1");
        regions.add("ap-southeast-2");
        regions.add("ap-northeast-1");
        regions.add("ca-central-1");
        regions.add("cn-north-1");
        regions.add("cn-northwest-1");
        regions.add("eu-central-1");
        regions.add("eu-west-1");
        regions.add("eu-west-2");
        regions.add("eu-south-1");
        regions.add("eu-west-3");
        regions.add("eu-north-1");
        regions.add("me-south-1");
        regions.add("sa-east-1");
        regions.add("us-gov-east-1");
        regions.add("us-gov-west-1");
        configProperty = new ProviderConfigProperty();
        configProperty.setName("AWS_REGION");
        configProperty.setLabel("aws-region-label");
        configProperty.setRequired(true);
        configProperty.setOptions(regions);
        configProperty.setType(ProviderConfigProperty.LIST_TYPE);
        configProperty.setHelpText("aws-region-hlp-txt");


        configProperties.add(configProperty);
    }
}

package com.qlikverify.services.eventlisteners.anamoly;

import com.qlikverify.services.constants.Constants;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.models.ConstraintData;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.models.EmailConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.qlikverify.services.constants.Constants.*;

public class AnamolyEventListenerProviderFactory implements EventListenerProviderFactory {
    @Override
    public String getDisplayName() {
        return "anamoly-detection-title";
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
        return true;
    }

    @Override
    public boolean testConfig(KeycloakSession session, Map<String, String> config) {
        if(session.getContext().getRealm().isEnabledOwnEmailProvider() && config.get(Constants.EMAIL_SENDER) == null)
            return false;

        if (!(config.containsKey(DIFF_BROWSER_TRIGGER) || config.containsKey(DIFF_IP_TRIGGER) || config.containsKey(DIFF_OS_TRIGGER)))
            return false;

        if (!(Boolean.parseBoolean(config.get(DIFF_BROWSER_TRIGGER)) || Boolean.parseBoolean(config.get(DIFF_IP_TRIGGER)) || Boolean.parseBoolean(config.get(DIFF_OS_TRIGGER))))
            return false;

        return true;
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        List<ConstraintData> constraintData = new ArrayList<>();
        if (session.getContext().getRealm().isEnabledOwnEmailProvider() && session.getContext().getRealm().getEmailConfigsStream().count() == 0)
            constraintData.add(new ConstraintData("This Feature requires enabling of atleast one of the Email Providers", "#/realms/" + session.getContext().getRealm().getName() + "/email/providers"));

        if (!session.getContext().getRealm().isEventsEnabled())
            constraintData.add(new ConstraintData("This Feature requires enabling of Audit Logs", "#/realms/" + session.getContext().getRealm().getName() + "/logs"));

        if (constraintData.isEmpty()) {
            return null;
        }

        return new ConstraintResponse(false, constraintData);
    }

    @Override
    public String getHelpText() {
        return "anamoly-detection-hlp-txt";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(Constants.EMAIL_SENDER)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(Constants.EMAIL_SENDER);
        provider.setLabel(Constants.EMAIL_SENDER_LABEL);

        if (session.getContext().getRealm().isEnabledOwnEmailProvider()) {
            List<EmailConfigModel> providers = session.getContext().getRealm().getEmailConfigsStream().collect(Collectors.toList());
            List<String> providerList = new ArrayList<>();
            if (!providers.isEmpty()) {
                providers.forEach(x -> providerList.add(x.getEmailProviderId()));
            }
            provider.setRequired(true);
            if (!providers.isEmpty()) {
                provider.setType(ProviderConfigProperty.LIST_TYPE);
                provider.setOptions(providerList);
                provider.setHelpText("email-config-set-sender");
                provider.setHelperLinkText("");
                provider.setHelperLink("");
            } else {
                provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                provider.setHelpText("");
                provider.setHelperLinkText("no-email-reg");
                provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/email/providers/create");
            }
        } else {
            provider.setRequired(false);
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("");
            provider.setHelperLinkText("all-emails-routed-root-workspace");
            provider.setHelperLink("realms/" + session.getContext().getRealm().getName() + "/realm-subscription");
        }
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }

        Optional<ProviderConfigProperty> helpProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("HELP")).findFirst();

        if(!helpProperty.isPresent())
        {
            ProviderConfigProperty configProperty = new ProviderConfigProperty();
            configProperty.setName("HELP");
            configProperty.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            configProperty.setHelpText("enable-trigger-to-enable-anamoly-detection");
            configProperties.add(configProperty);
        }

        return configProperties;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;

        configProperty = new ProviderConfigProperty();
        configProperty.setName(ANOMALY_TRIIGER_COUNT);
        configProperty.setLabel("anomaly-trigger-count-label");
        configProperty.setType(ProviderConfigProperty.NUMBER_TYPE);
        configProperty.setDefaultValue(2);
        configProperty.setHelpText("anomaly-trigger-count-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(DIFF_BROWSER_TRIGGER);
        configProperty.setLabel("trigger-on-diff-brow-label");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText("trigger-on-diff-brow-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(DIFF_IP_TRIGGER);
        configProperty.setLabel("trigger-on-diff-ip-label ");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText("trigger-on-diff-ip-hlp-txt");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName(DIFF_OS_TRIGGER);
        configProperty.setLabel("trigger-on-diff-os-label");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText("trigger-on-diff-os-hlp-txt");

        configProperties.add(configProperty);
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new AnamolyEventListenerProvider(session, this);
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
        return "qv-anamoly-detection";
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"bo", "ciam", "wiam"};
    }
}

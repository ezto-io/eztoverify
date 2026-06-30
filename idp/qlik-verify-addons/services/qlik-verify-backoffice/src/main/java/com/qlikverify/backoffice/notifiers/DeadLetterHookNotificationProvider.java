package com.qlikverify.backoffice.notifiers;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jetbrains.annotations.Nullable;
import org.keycloak.Config;
import org.keycloak.ciba.CibaDeadLetterHookNotificationProvider;
import org.keycloak.ciba.CibaDeadLetterHookNotificationProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ComponentRepresentation;

import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;

public class DeadLetterHookNotificationProvider implements CibaDeadLetterHookNotificationProvider, CibaDeadLetterHookNotificationProviderFactory {

    private static final Logger logger = Logger.getLogger(DeadLetterHookNotificationProvider.class);
    KeycloakSession session;
    EventBuilder event;

    private static final Map<String, Object> encrytionDependencyMap = new HashMap<>();

    static {
        encrytionDependencyMap.put(NOTIFICATION_RESPONSE_WAY, ENCRYPTED);
        encrytionDependencyMap.put("enable", true);
    }

    @Override
    public CibaDeadLetterHookNotificationProvider setEventBuilder(EventBuilder eventBuilder) {
        this.event = eventBuilder;
        return this;
    }

    @Override
    public void sendNotification(KeycloakSession session, RealmModel realm, ClientModel client, CibaConfig cibaConfig, OAuth2DeviceCodeModel deviceModel, CibaDeadLetterHookConfigModel transactionResultHookConfigModel, HttpRequest httpRequest, Map<String, String> attrs) {

    }

    @Override
    public void sendError(KeycloakSession session, RealmModel realm, ClientModel client, CibaConfig cibaConfig, OAuth2DeviceCodeModel deviceModel, CibaDeadLetterHookConfigModel transactionResultHookConfigModel, HttpRequest httpRequest, Map<String, String> attrs) {

    }

    @Override
    public Map<String, Object> getResult(ClientModel client, CibaDeadLetterHookConfigModel transactionResultHookConfigModel, Map<String, Object> attrs) {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Hook";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName("enable");
        property.setLabel("Enable");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("dead-letter-enable-hlp-txt");
        configProperties.add(property);


        property = new ProviderConfigProperty();
        property.setName(NOTIFICATION_ENDPOINT);
        property.setLabel("notification-endpoint-label");
        property.setType(ProviderConfigProperty.URL);
        property.setRequired(true);
        property.setDependsOn(new HashMap<>(){{
            put("enable", true);
        }});
        property.setHelpText("dead-letter-notification-endpoint-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(NOTIFICATION_ENDPOINT_AUTH);
        property.setLabel("notification-endpoint-auth-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("notification-endpoint-auth-hlp-txt");
        property.setDependsOn(new HashMap<>(){{
            put("enable", true);
        }});
        configProperties.add(property);

        List<String> responseWays = new ArrayList<>();
        responseWays.add("plaintext");
        responseWays.add(ENCRYPTED);
        responseWays.add("token");

        property = new ProviderConfigProperty();
        property.setName(NOTIFICATION_RESPONSE_WAY);
        property.setLabel("notification-response-way-label");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setHelpText("notification-response-way-hlp-txt");
        property.setOptions(responseWays);
        property.setDefaultValue("plaintext");
        property.setDependsOn(new HashMap<>(){{
            put("enable", true);
        }});
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("hook-failure-threshold");
        property.setLabel("hook-failure-threshold-label");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("hook-failure-threshold-hlp-txt");
        property.setRequired(true);
        property.setDefaultValue(5);
        property.setDependsOn(new HashMap<>(){{
            put("enable", true);
        }});
        configProperties.add(property);

    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        List<Map> hmacKeys = session.keys().getKeysStream(session.getContext().getRealm(), KeyUse.ENC, "AES").filter(x -> x.getStatus().isEnabled() && x.getStatus().isActive())
                .map(x-> {
                    Map<String, String> key = new HashMap<>();
                    key.put("name",x.getName());
                    key.put("value", x.getProviderId());
                    return key;
                })
                .collect(Collectors.toList());
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(SECRET_KEY)).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName(SECRET_KEY);
        provider.setLabel("secret-keys-label");

        if (hmacKeys.isEmpty()) {
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("No AES Keys are registered. <a href=' #/create/keys/" + session.getContext().getRealm().getName() + "/providers/aes-generated'> Click here </a> to create one");
        } else {
            provider.setType(ProviderConfigProperty.MAP_LIST_TYPE);
            provider.setRequired(true);
            provider.setMapOptions(hmacKeys);
            provider.setOnDemand("/admin/realms/" + session.getContext().getRealm().getName() + "/clients/" + session.getContext().getClient().getClientId() + "/dead-letter-hook-notification-config/ondemand/" + getId());
            provider.setHelpText("Select key from the list of keys to enable encrytion of payload, key will be shown only for Account creator after saving the config. Note if its changed need to update at the receiving side. <a href=' #/realms/" + session.getContext().getRealm().getName() + "/defense/keys/'> Click here </a> to view keys");
        }


        provider.setDependsOn(encrytionDependencyMap);

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
        return configProperties;
    }

    @Override
    public CibaDeadLetterHookNotificationProvider create(KeycloakSession session) {
        this.session = session;
        return this;
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
        return "dead-letter-hook-notification-provider";
    }

    @Override
    public List<ProviderConfigProperty> getOnDemandConfig(KeycloakSession session, HashMap<String, Object> config) {
        ComponentRepresentation componentRepresentation = getEncryptionComponent(session, config);
        if (componentRepresentation == null) return new ArrayList<>();
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName("secret-key");
        property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        property.setHelpText("Use the following secret key to decrypt payload in hook result.\n KEY: <b>" + componentRepresentation.getConfig().get("secret").get(0));
        property.setLabel("secret-key-label");
        property.setDependsOn(encrytionDependencyMap);
        List<ProviderConfigProperty> providerConfigProperties = new ArrayList<>();
        providerConfigProperties.add(property);
        return providerConfigProperties;

    }

    @Nullable
    private ComponentRepresentation getEncryptionComponent(KeycloakSession session, HashMap<String, Object> config) {
        ComponentModel optionalComponentModel = session.getContext().getRealm().getComponent(String.valueOf(config.get(SECRET_KEY)));
        if (optionalComponentModel == null) {
            return null;
        }

        return ModelToRepresentation.toRepresentation(session, optionalComponentModel, true);
    }

    @Override
    public void onCleanUp(KeycloakSession session, RealmModel realm, ClientModel client) {
        CibaDeadLetterHookConfigModel deadLetterConfig = realm.getDeadLetterHookNotificationConfigByClientId(client.getClientId(), realm.getId());
        if(deadLetterConfig != null) {
            realm.removeDeadLetterHookNotificationConfig(deadLetterConfig);
        }
    }
}

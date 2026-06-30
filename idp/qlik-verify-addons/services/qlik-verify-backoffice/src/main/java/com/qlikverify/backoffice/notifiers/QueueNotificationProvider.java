package com.qlikverify.backoffice.notifiers;

import com.qlikverify.services.eventlisteners.kafka.ApacheKafkaProducer;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.Config;
import org.keycloak.ciba.CibaNotificationProvider;
import org.keycloak.ciba.CibaNotificationProviderFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.*;
import java.util.*;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.services.constants.Constants.AUTHENTICATION;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.TOKEN_DELIVERY_MODE;

public class QueueNotificationProvider implements CibaNotificationProvider, CibaNotificationProviderFactory {
    private static final Logger logger = Logger.getLogger(QueueNotificationProvider.class);
    KeycloakSession session;
    EventBuilder event;

    @Override
    public CibaNotificationProvider setEventBuilder(EventBuilder eventBuilder) {
        this.event = eventBuilder;
        return this;
    }

    @Override
    public void sendNotification(KeycloakSession session, RealmModel realm, ClientModel client, CibaConfig cibaConfig, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig, HttpRequest httpRequest, Map<String, String> attrs) {
        sendResultToQueue(realm, client, deviceModel, notificationConfig);
    }

    public void queueWayKafka(ClientModel client, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig, String queueWay, RealmModel realm) {
        String trxId = deviceModel.getClientNotificationToken();
        TrxModel trx = realm.getTrx(trxId);
        if (trx != null && trx.getAttributes() != null
                && trx.getAttributes().containsKey("liveness_completed")
                && !Boolean.parseBoolean(trx.getAttributes().get("liveness_completed"))) {
            realm.addAttribute(trxId, "ik@client_id", client.getClientId());
            realm.addAttribute(trxId, "ik@hook_way", "kafka");
            realm.addAttribute(trxId, "ik@device_code", deviceModel.getDeviceCode());
            return;
        }

        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle(AUTH_REQ_ID, deviceModel.getAuthReqId());
        params.putSingle("ref_id", deviceModel.getClientNotificationToken());
        TokenEndpoint tokenEndpoint = new TokenEndpoint(new TokenManager(), realm, event, "urn:openid:params:grant-type:ciba");

        CibaGrantType cibaGrantType = new CibaGrantType(params, client, session, tokenEndpoint, realm, event);
        Response response = null;
        Map<String, String> attributes = new HashMap<>();
        try {
            response = cibaGrantType.cibaGrant(deviceModel.getAuthReqId());
        } catch (ErrorResponseException | CorsErrorResponseException e) {
            OAuth2ErrorRepresentation error = (OAuth2ErrorRepresentation) e.getResponse().getEntity();
            attributes.put("error", error.getError());
            attributes.put("error_message", error.getErrorDescription());
        }

        try {
            if (Boolean.parseBoolean(notificationConfig.getConfig().get("AUTHENTICATION"))) {
                ApacheKafkaProducer.publishEvent(session, notificationConfig.getConfig().get("KAFKA_SERVER"), notificationConfig.getConfig().get("KAFKA_TOPIC"), JsonSerialization.writeValueAsString(response != null ? response.getEntity() : attributes), notificationConfig.getConfig().get("USER_NAME"), notificationConfig.getConfig().get("PASSWORD"), notificationConfig.getConfig().get("AUTHENTICATION"));
            } else {
                ApacheKafkaProducer.publishEvent(session, notificationConfig.getConfig().get("KAFKA_SERVER"), notificationConfig.getConfig().get("KAFKA_TOPIC"), JsonSerialization.writeValueAsString(response != null ? response.getEntity() : attributes), null, null, notificationConfig.getConfig().get("AUTHENTICATION"));
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    @Override
    public void sendError(KeycloakSession session, RealmModel realm, ClientModel client, CibaConfig cibaConfig, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig, HttpRequest httpRequest, Map<String, String> attrs) {
        sendResultToQueue(realm, client, deviceModel, notificationConfig);
    }

    private void sendResultToQueue(RealmModel realm, ClientModel client, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig) {
        String queueWay = notificationConfig.getConfig().get(QUEUE_WAY);
        switch (queueWay) {
            case KAFKA:
                queueWayKafka(client, deviceModel, notificationConfig, queueWay, realm);
                break;
        }
    }

    @Override
    public Map<String, Object> getResult(ClientModel client, CibaNotificationConfigModel notificationConfig, Map<String, Object> attrs) {
        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        UriBuilder backendUriBuilder = RealmsResource.protocolUrl(backendUriInfo);
        String responseNotificationWay = notificationConfig.getConfig().get(NOTIFICATION_RESPONSE_WAY);
        String queueWay = notificationConfig.getConfig().get(QUEUE_WAY);

        Map<String, Object> result = new HashMap<>();
        String version = (String) attrs.get("version");
        HashMap<String, Object> body = new HashMap<>();
        if (version.equals("0")) {
            body.put(NOTIFICATION_WAY, responseNotificationWay);
        }

        if (queueWay.equals(KAFKA)) {
            result.put(AUTH_REQ_ID, "Will receive in Kafka queue");
            if (version.equals("0")) {
                body.put(AUTH_REQ_ID, "Will receive in Kafka queue");
            }
        }

        if (!queueWay.equals("push")) {
            if (version.equals("0")) {
                body.put("app_id", client.getClientId());
                body.put("app_secret", "Take from dashboard or request your admin to provide");
                HashMap<String, Object> tokenAccessDetails = new HashMap<>();
                tokenAccessDetails.put("token_endpoint", backendUriBuilder.clone().path(OIDCLoginProtocolService.class, RESULT).build(session.getContext().getRealm().getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
                tokenAccessDetails.put("request_id", "POST");
                tokenAccessDetails.put("content_type", "application/x-www-form-urlencoded");
                tokenAccessDetails.put("body", body);
                result.put("token_access_details", tokenAccessDetails);
            }
        }

        result.put(TOKEN_DELIVERY_MODE, getDisplayName().toLowerCase(Locale.ROOT) + "@" + queueWay);
        return result;
    }

    @Override
    public Object getStep3(ClientModel clientModel, UserModel userModel, CibaNotificationConfigModel notificationConfig) {
        String queueWay = notificationConfig.getConfig().get(QUEUE_WAY);
        if (queueWay.equals(KAFKA)) {
            return "Token 'auth_req_id' will be delivered to Kafka server";
        }

        return "Invalid settings";
    }

    @Override
    public void onCleanUp(KeycloakSession session, RealmModel realm, ClientModel client) {
        CibaNotificationConfigModel notificationConfig = realm.getCibaNotificationConfigByClientId(client.getClientId(), realm.getId());
        if (notificationConfig != null) {
            realm.removeCibaNotificationConfig(notificationConfig);
        }
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Queue";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;

        List<String> verificationTypes = new ArrayList<>();
        verificationTypes.add(KAFKA);
        property = new ProviderConfigProperty();
        property.setName(QUEUE_WAY);
        property.setLabel("Queue");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(verificationTypes);
        property.setDefaultValue(KAFKA);
        property.setHelpText("queue-way-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("KAFKA_SERVER");
        property.setLabel("kafka-bootstrap-server-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setHelpText("kafka-bootstrap-server-hlp-txt");
        Map<String, Object> kafkaServerDependsOn = new HashMap<>();
        kafkaServerDependsOn.put(QUEUE_WAY, KAFKA);
        property.setDependsOn(kafkaServerDependsOn);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("KAFKA_TOPIC");
        property.setLabel("kafka-topic-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("kafka-topic-hlp-txt");
        property.setRequired(true);
        Map<String, Object> kafkaTopicDependsOn = new HashMap<>();
        kafkaTopicDependsOn.put(QUEUE_WAY, KAFKA);
        property.setDependsOn(kafkaTopicDependsOn);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(AUTHENTICATION);
        property.setLabel("authentication-label");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("authentication-hlp-txt");
        Map<String, Object> kafkaAuthenticationDependsOn = new HashMap<>();
        kafkaAuthenticationDependsOn.put(QUEUE_WAY, KAFKA);
        property.setDependsOn(kafkaAuthenticationDependsOn);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("USER_NAME");
        property.setLabel("username-or-api-key-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("username-or-api-key-hlp-txt");
        property.setRequired(true);
        Map<String, Object> usernameDependsOn = new HashMap<>();
        usernameDependsOn.put(AUTHENTICATION, true);
        property.setDependsOn(usernameDependsOn);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("PASSWORD");
        property.setLabel("password-or-api-secret-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("password-or-api-secret-hlp-txt");
        property.setRequired(true);
        Map<String, Object> passwordDependsOn = new HashMap<>();
        passwordDependsOn.put(AUTHENTICATION, true);
        property.setDependsOn(passwordDependsOn);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        property.setHelpText("save-enable-when-test-config-verify-label");
        configProperties.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        Optional<ProviderConfigProperty> resultVersionConfig = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("result-version")).findFirst();

        boolean hasResultVersion = session.getContext().getRealm().getAttributes().containsKey("result-version");
        ProviderConfigProperty property;

        if (resultVersionConfig.isPresent() && hasResultVersion) {
            property = resultVersionConfig.get();
            configProperties.remove(property);
        } else if (!resultVersionConfig.isPresent() && !hasResultVersion) {
            property = new ProviderConfigProperty();
            property.setName("result-version");
            property.setLabel("result-version");
            property.setHelpText("result-version-hlp-txt");
            property.setType(ProviderConfigProperty.LIST_TYPE);

            List<String> apiVersions = new ArrayList<>();
            apiVersions.add("v0");
            apiVersions.add("v1");
            property.setOptions(apiVersions);
            property.setDefaultValue("v1");
            configProperties.add(property);
        }

        return configProperties;
    }

    @Override
    public CibaNotificationProvider create(KeycloakSession session) {
        this.session = session;
        return this;
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
        return "queue-notifier";
    }
}

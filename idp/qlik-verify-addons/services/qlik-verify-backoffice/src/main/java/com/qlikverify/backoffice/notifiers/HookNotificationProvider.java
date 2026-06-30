package com.qlikverify.backoffice.notifiers;

import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.backgroundService.backgroundHookNotifierService.BackgroundHookNotifierRequestBody;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jetbrains.annotations.Nullable;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.ciba.CibaNotificationProvider;
import org.keycloak.ciba.CibaNotificationProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.ClientNotificationEndpointRequest;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.urls.UrlType;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.ErrorMappings.RESULT_NOTIFICATION_ERROR;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.TOKEN_DELIVERY_MODE;

public class HookNotificationProvider implements CibaNotificationProvider, CibaNotificationProviderFactory {
    private static final Logger logger = Logger.getLogger(HookNotificationProvider.class);
    KeycloakSession session;
    EventBuilder event;
    private static final Map<String, Object> encrytionDependencyMap = new HashMap<>();

    static {
        encrytionDependencyMap.put(NOTIFICATION_RESPONSE_WAY, ENCRYPTED);
    }

    @Override
    public CibaNotificationProvider setEventBuilder(EventBuilder eventBuilder) {
        this.event = eventBuilder;
        return this;
    }

    @Override
    public void sendNotification(KeycloakSession session, RealmModel realm, ClientModel client, CibaConfig cibaConfig, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig, HttpRequest httpRequest, Map<String, String> attrs) {
        sendResultToHook(realm, client, deviceModel, notificationConfig, attrs);
    }

    private void hookWayPing(RealmModel realm, String clientNotificationEndpoint, ClientModel client, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig, String hookWay) {
        if (clientNotificationEndpoint == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Client notification endpoint not set for the client with the ping mode",
                    Response.Status.BAD_REQUEST);
        }

        logger.debugf("Sending request to client notification endpoint '%s' for the client '%s'", clientNotificationEndpoint, client.getClientId());

        ClientNotificationEndpointRequest clientNotificationRequest = new ClientNotificationEndpointRequest();
        clientNotificationRequest.setAuthReqId(deviceModel.getAuthReqId());

        String token = notificationConfig.getConfig().get(NOTIFICATION_ENDPOINT_AUTH);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("auth_req_id", deviceModel.getAuthReqId());
        String response = null;
        try {
            response = JsonSerialization.writeValueAsString(responseMap);
        } catch (IOException e) {
            response = responseMap.toString();
        }
        try {
            Map<String, String> fallBackDetails = new HashMap<>();
            fallBackDetails.put("clientId", client.getClientId());
            fallBackDetails.put("tenantId", realm.getId());
            fallBackDetails.put("actionToken", BackgroundJobActionTokenVerifier.
                    generateToken(session, Long.parseLong(String.valueOf(GeneralHelper.getExpiryDuration("1h"))), ""));
            BackgroundHookNotifierRequestBody requestBody = new BackgroundHookNotifierRequestBody(
                    "ping",
                    clientNotificationEndpoint,
                    token != null && !token.isEmpty() ? token : null,
                    response,
                    JsonSerialization.writeValueAsString(fallBackDetails)
            );
            BackgroundServiceImpl.getInstance(session).createBackgroundHookNotifierJob(requestBody);
            event.detail(REQUEST_ID, deviceModel.getClientNotificationToken()).detail(NOTIFICATION_WAY, hookWay).event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER).success();
        } catch (Exception ioe) {
            logger.errorf(ioe, "Failed to send request to client notification endpoint '%s' of client '%s'", clientNotificationEndpoint, client.getClientId());
            event.detail(REQUEST_ID, deviceModel.getClientNotificationToken()).detail(NOTIFICATION_WAY, hookWay).event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER).error(ioe.getMessage());
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, SEND_REQUEST_FAILED,
                    Response.Status.BAD_REQUEST);
        }
    }

    private void hookWayPush(String clientNotificationEndpoint, ClientModel client, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig, String hookWay, RealmModel realm, String responseNotificationWay) {
        if (clientNotificationEndpoint == null) {
            event.detail(REQUEST_ID, deviceModel.getClientNotificationToken()).detail(NOTIFICATION_WAY, "push").event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER).error(Errors.INVALID_REQUEST);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Client notification endpoint not set for the client with the push mode",
                    Response.Status.BAD_REQUEST);
        }

        String trxId = deviceModel.getClientNotificationToken();
        TrxModel trx = realm.getTrx(trxId);
        if (trx.getAttributes().containsKey(VIDEO_KYC_COMPLETED))
            logger.info("hook-way" + trxId + trx.getAttributes().get(VIDEO_KYC_COMPLETED));
        if (trx != null && trx.getAttributes() != null
                && ((trx.getAttributes().containsKey("liveness_completed") &&
                !Boolean.parseBoolean(trx.getAttributes().get("liveness_completed")))
                || (trx.getAttributes().containsKey(VIDEO_KYC_COMPLETED) &&
                !Boolean.parseBoolean(trx.getAttributes().get(VIDEO_KYC_COMPLETED))))
        ) {
            realm.addAttribute(trxId, "ik@client_id", client.getClientId());
            realm.addAttribute(trxId, "ik@hook_way", "push");
            realm.addAttribute(trxId, "ik@device_code", deviceModel.getDeviceCode());
            return;
        }

        logger.debugf("Sending request to client notification endpoint '%s' for the client '%s'", clientNotificationEndpoint, client.getClientId());

        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle(AUTH_REQ_ID, deviceModel.getAuthReqId());
        params.putSingle("ref_id", deviceModel.getClientNotificationToken());
        TokenEndpoint tokenEndpoint = new TokenEndpoint(new TokenManager(), realm, event, "urn:openid:params:grant-type:ciba");


        CibaGrantType cibaGrantType = new CibaGrantType(params, client, session, tokenEndpoint, realm, event);
        Map<String, String> attributes = new HashMap<>();
        Response response = null;
        try {
            response = cibaGrantType.cibaGrant(deviceModel.getAuthReqId());
        } catch (ErrorResponseException | CorsErrorResponseException e) {
            OAuth2ErrorRepresentation error = (OAuth2ErrorRepresentation) e.getResponse().getEntity();
            attributes.put("error", error.getError());
            attributes.put("error_message", error.getErrorDescription());
        }

        String token = notificationConfig.getConfig().get(NOTIFICATION_ENDPOINT_AUTH);
        boolean isEncrypted = responseNotificationWay.equals(ENCRYPTED);
        try {
            Map<String, String> fallBackDetails = new HashMap<>();
            fallBackDetails.put("clientId", client.getClientId());
            fallBackDetails.put("tenantId", realm.getId());
            fallBackDetails.put("actionToken", BackgroundJobActionTokenVerifier.
                    generateToken(session, Long.parseLong(String.valueOf(GeneralHelper.getExpiryDuration("1h"))), ""));
            BackgroundHookNotifierRequestBody requestBody = new BackgroundHookNotifierRequestBody(
                    "push",
                    clientNotificationEndpoint,
                    token != null && !token.isEmpty() ? token : null,
                    isEncrypted && response.getEntity() != null ? (String) response.getEntity() :
                            JsonSerialization.writeValueAsString(attributes.isEmpty() ? response.getEntity() : attributes),
                    JsonSerialization.writeValueAsString(fallBackDetails)
            );
            BackgroundServiceImpl.getInstance(session).createBackgroundHookNotifierJob(requestBody);
            event.detail(REQUEST_ID, deviceModel.getClientNotificationToken()).detail(NOTIFICATION_WAY, hookWay).event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER).success();
        } catch (Exception ioe) {
            logger.errorf(ioe, "Failed to send request to client notification endpoint '%s' of client '%s'", clientNotificationEndpoint, client.getClientId());
            event.detail(REQUEST_ID, deviceModel.getClientNotificationToken()).detail(NOTIFICATION_WAY, hookWay).event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER).error(ioe.getMessage());
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, SEND_REQUEST_FAILED,
                    Response.Status.BAD_REQUEST);
        }
    }

    private void hookWayMobileOrWebSdk(RealmModel realm, OAuth2DeviceCodeModel deviceModel, String hookWay, ClientModel client, Map<String, String> attrs) {
        logger.debugf("Sending request to socket endpoint for the client '%s'", client.getClientId());
        if (hookWay.equals(WEB_SDK)) {
            UserModel userModel = null;
            if (attrs != null && !attrs.isEmpty()) {
                userModel = session.users().getUserById(realm, attrs.get("user_id"));
            }
            if (userModel != null) {
                session.sessions().removeUserSessionsExceptForClient(realm, userModel, "dashboard");
            }
        }

        String socketUrl = EnvironmentReader.getValue("SOCKET_URL");
        HttpUtility httpUtility = new HttpUtility();
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("auth_req_id", deviceModel.getAuthReqId());

        try (okhttp3.Response socketResponse = httpUtility.callRequest(
                "application/json",
                "POST",
                socketUrl + "/send?key=" + deviceModel.getClientNotificationToken(),
                JsonSerialization.writeValueAsString(reqBody),
                null, null, 5)) {
            if (!socketResponse.isSuccessful()) {
                event.error("Failed to send request id to socket");
                throw new Exception("Failed to send request id to socket");
            }
            event.detail(REQUEST_ID, deviceModel.getClientNotificationToken()).detail(NOTIFICATION_WAY, hookWay).event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER).success();
        } catch (Exception ioe) {
            logger.errorf(ioe, "Failed to send request to socket endpoint of client '%s'", client.getClientId());
            event.detail(REQUEST_ID, deviceModel.getClientNotificationToken()).detail(NOTIFICATION_WAY, hookWay).event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER_ERROR).error(RESULT_NOTIFICATION_ERROR);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, SEND_REQUEST_FAILED,
                    Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public void sendError(KeycloakSession session, RealmModel realm, ClientModel client, CibaConfig cibaConfig, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig, HttpRequest httpRequest, Map<String, String> attrs) {
        sendResultToHook(realm, client, deviceModel, notificationConfig, attrs);
    }

    private void sendResultToHook(RealmModel realm, ClientModel client, OAuth2DeviceCodeModel deviceModel, CibaNotificationConfigModel notificationConfig, Map<String, String> attrs) {
        String hookWay = notificationConfig.getConfig().get(HOOK_WAY);
        String responseNotificationWay = notificationConfig.getConfig().get(NOTIFICATION_RESPONSE_WAY);
        String clientNotificationEndpoint = notificationConfig.getConfig().get(NOTIFICATION_ENDPOINT);
        switch (hookWay) {
            case "ping":
                hookWayPing(realm, clientNotificationEndpoint, client, deviceModel, notificationConfig, hookWay);
                break;
            case "push":
                hookWayPush(clientNotificationEndpoint, client, deviceModel, notificationConfig, hookWay, realm, responseNotificationWay);
                break;
            case MOBILE_SDK:
            case DESKTOP_APP:
            case WEB_SDK:
                hookWayMobileOrWebSdk(realm, deviceModel, hookWay, client, attrs);
                break;
            default:
                break;
        }
    }

    @Override
    public Map<String, Object> getResult(ClientModel clientModel, CibaNotificationConfigModel notificationConfig, Map<String, Object> attrs) {


        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        UriBuilder backendUriBuilder = RealmsResource.protocolUrl(backendUriInfo);
        String hookWay = notificationConfig.getConfig().get(HOOK_WAY);

        if (hookWay == null) {
            hookWay = "poll";
        }

        Map<String, Object> result = new HashMap<>();

        String version = (String) attrs.get("version");

        result.put(TOKEN_DELIVERY_MODE, getDisplayName().toLowerCase(Locale.ROOT) + "@" + hookWay);

        HashMap<String, Object> body = new HashMap<>();
        if (hookWay.equals("poll")) {
            result.put(AUTH_REQ_ID, attrs.get(AUTH_REQ_ID));
            Integer pollInterval = Integer.parseInt(notificationConfig.getConfig().get(POLL_INTERVAL));
            result.put(OAuth2Constants.INTERVAL, pollInterval);
            if (version.equals("0")) {
                body.put(AUTH_REQ_ID, attrs.get(AUTH_REQ_ID));
            }
        } else if (hookWay.equals("ping")) {
            result.put(AUTH_REQ_ID, "Will receive in Subscribed Hook URL");
            if (version.equals("0")) {
                body.put(AUTH_REQ_ID, "Will receive in Subscribed Hook URL");
            }
        } else if (hookWay.equals("web_sdk")) {
            result.put(AUTH_REQ_ID, "Will receive in Web SDK event");
            if (version.equals("0")) {
                body.put(AUTH_REQ_ID, "Will receive in Web SDK event");
            }
        } else if (hookWay.equals("mobile_sdk")) {
            result.put(AUTH_REQ_ID, "Will receive in Mobile SDK event");
            if (version.equals("0")) {
                body.put(AUTH_REQ_ID, "Will receive in Mobile SDK event");
            }
        }

        if (!hookWay.equals("push") && !hookWay.equals("desktop_app")) {
            if (version.equals("0")) {
                HashMap<String, Object> tokenAccessDetails = new HashMap<>();
                body.put("app_id", clientModel.getClientId());
                body.put("app_secret", "Take from dashboard or request your admin to provide");
                tokenAccessDetails.put("token_endpoint", backendUriBuilder.clone().path(OIDCLoginProtocolService.class, RESULT).build(session.getContext().getRealm().getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
                tokenAccessDetails.put("request_id", "POST");
                tokenAccessDetails.put("content_type", "application/x-www-form-urlencoded");
                tokenAccessDetails.put("body", body);
                result.put("token_access_details", tokenAccessDetails);
            }
        }

        return result;
    }

    @Override
    public Object getStep3(ClientModel clientModel, UserModel userModel, CibaNotificationConfigModel configModel) {
        String hookWay = configModel.getConfig().get(HOOK_WAY);
        boolean clientSupportsV0 = com.qlikverify.extensions.admin.misc.helpers.GeneralHelper.isV0Supported(session.getContext().getRealm());
        HashMap<String, String> data = new HashMap<>();
        if (hookWay.equals("poll")) {
            String pollInterval = configModel.getConfig().get(POLL_INTERVAL);
            UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
            UriBuilder backendUriBuilder = RealmsResource.protocolUrl(backendUriInfo);
            String tokenUrl = backendUriBuilder.clone().path(OIDCLoginProtocolService.class, RESULT).build(session.getContext().getRealm().getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString();
            if (clientSupportsV0) {
                data.put("v0", "curl --location --request POST '" + tokenUrl + "' \n--header 'Content-Type:application/x-www-form-urlencoded' \n--data-urlencode 'app_id=" + clientModel.getClientId() + "' \n--data-urlencode 'app_secret=" + clientModel.getSecret() + "' \n--data-urlencode 'auth_req_id={auth_req_id from step1}'");
            }

            data.put("v1", "curl --location --request POST '" + tokenUrl + "' \n--header 'Content-Type:application/x-www-form-urlencoded' \n --header 'api-version: 1' \n --header 'Authorization: " + BasicAuthHelper.createHeader(clientModel.getClientId(), clientModel.getSecret()) + "' \n--data-urlencode 'auth_req_id={auth_req_id from step1}'");
            return data;
        }

        if (hookWay.equals("ping")) {
            UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
            UriBuilder backendUriBuilder = RealmsResource.protocolUrl(backendUriInfo);
            String tokenUrl = backendUriBuilder.clone().path(OIDCLoginProtocolService.class, RESULT).build(session.getContext().getRealm().getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString();
            if (clientSupportsV0) {
                data.put("v0", "curl --location --request POST '" + tokenUrl + "' \n--header 'Content-Type:application/x-www-form-urlencoded' \n--data-urlencode 'app_id=" + clientModel.getClientId() + "' \n--data-urlencode 'app_secret=" + clientModel.getSecret() + "' \n--data-urlencode 'auth_req_id={auth_req_id received to your hook url}'");
            }

            data.put("v1", "curl --location --request POST '" + tokenUrl + "' \n--header 'Content-Type:application/x-www-form-urlencoded' \n --header 'api-version: 1' \n --header 'Authorization: " + BasicAuthHelper.createHeader(clientModel.getClientId(), clientModel.getSecret()) + "' \n--data-urlencode 'auth_req_id={auth_req_id received to your hook url}'");
            return data;
        }

        if (hookWay.equals("push")) {
            return "Result will be sent your hook url '" + configModel.getConfig().get(NOTIFICATION_ENDPOINT) + "'";
        }

        if (hookWay.equals(MOBILE_SDK)) {
            return "Token 'auth_req_id' will be delivered to mobile sdk";
        }

        if (hookWay.equals(WEB_SDK)) {
            return "Token 'auth_req_id' will be delivered to web sdk";
        }

        if (hookWay.equals(DESKTOP_APP)) {
            return "Token 'auth_req_id will be delivered to desktop app";
        }

        return "Invalid settings";
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

        List<String> verificationTypes = new ArrayList<>();
        verificationTypes.add("poll");
        verificationTypes.add("ping");
        verificationTypes.add("push");
        verificationTypes.add(MOBILE_SDK);
        verificationTypes.add(DESKTOP_APP);
        verificationTypes.add(WEB_SDK);
        property = new ProviderConfigProperty();
        property.setName(HOOK_WAY);
        property.setLabel("hook-way-label");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(verificationTypes);
        property.setDefaultValue("push");
        property.setHelpText("hook-way-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(POLL_INTERVAL);
        property.setLabel("poll-interval-label");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setRequired(true);
        property.setHelpText("sms-template-hlp-txt");
        Map<String, Object> pollIntervalDependsOn = new HashMap<>();
        pollIntervalDependsOn.put(HOOK_WAY, "poll");
        property.setDependsOn(pollIntervalDependsOn);
        property.setMinVal(5);
        property.setDefaultValue(5);
        property.setHelpText("poll-interval-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(NOTIFICATION_ENDPOINT);
        property.setLabel("notification-endpoint-label");
        property.setType(ProviderConfigProperty.URL);
        property.setRequired(true);
        property.setHelpText("notification-endpoint-hlp-txt");
        Map<String, Object> notificationEndpointDependsOn = new HashMap<>();
        notificationEndpointDependsOn.put(HOOK_WAY, new String[]{"push", "ping"});
        property.setDependsOn(notificationEndpointDependsOn);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(NOTIFICATION_ENDPOINT_AUTH);
        property.setLabel("notification-endpoint-auth-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("notification-endpoint-auth-hlp-txt");
        property.setDependsOn(notificationEndpointDependsOn);
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

        Map<String, Object> dependsOn = new HashMap<>();
        dependsOn.put(HOOK_WAY, new String[]{"push", "ping", "poll"});
        property.setDependsOn(dependsOn);

        property.setHelpText("notification-response-way-hlp-txt");
        property.setOptions(responseWays);
        property.setDefaultValue("plaintext");
        configProperties.add(property);


        property = new ProviderConfigProperty();
        property.setName("notification-sdk-response-way");
        property.setLabel("notification-response-way-label");
        property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        property.setHelpText("notification-always-auth-request-id");
        dependsOn = new HashMap<>();
        dependsOn.put(HOOK_WAY, new String[]{MOBILE_SDK, WEB_SDK, DESKTOP_APP});
        property.setDependsOn(dependsOn);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("hook-failure-threshold");
        property.setLabel("hook-failure-threshold-label");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("hook-failure-threshold-hlp-txt");
        property.setRequired(true);
        property.setDefaultValue(5);
        dependsOn = new HashMap<>();
        dependsOn.put(HOOK_WAY, new String[]{"push", "ping"});
        property.setDependsOn(dependsOn);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        property.setDependsOn(new HashMap<String, Object>() {{
            put("hook-way", new String[]{"web_sdk"});
        }});
        property.setHelpText("web-sdk-notify-banner");
        configProperties.add(property);

    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        List<Map> hmacKeys = session.keys().getKeysStream(session.getContext().getRealm(), KeyUse.ENC, "AES").filter(x -> x.getStatus().isEnabled() && x.getStatus().isActive())
                .map(x -> {
                    Map<String, String> key = new HashMap<>();
                    key.put("name", x.getName());
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
            provider.setHelpText("No AES Keys are registered. Click <a href=' #/create/keys/" + session.getContext().getRealm().getName() + "/providers/aes-generated'> Click here </a> to create one");
        } else {
            provider.setType(ProviderConfigProperty.MAP_LIST_TYPE);
            provider.setRequired(true);
            provider.setMapOptions(hmacKeys);
            provider.setOnDemand("/admin/realms/" + session.getContext().getRealm().getName() + "/clients/" + session.getContext().getClient().getClientId() + "/trx-notification-config/ondemand/" + getId());
            provider.setHelpText("Select a key from the list of keys to enable encryption of the payload. The key will only be visible to the account creator after saving the configuration. Note: If the key is changed, it must be updated on the receiving side. <a href=' #/realms/\" + session.getContext().getRealm().getName() + \"/defense/keys/'> Click here </a> to view keys.");
        }

        provider.setDependsOn(encrytionDependencyMap);

        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }

        Optional<ProviderConfigProperty> resultVersionConfig = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("result-version")).findFirst();
        boolean hasResultVersion = session.getContext().getRealm().getAttributes().containsKey("result-version");

        if (resultVersionConfig.isPresent() && hasResultVersion) {
            provider = resultVersionConfig.get();
            configProperties.remove(provider);
        } else if (!resultVersionConfig.isPresent() && !hasResultVersion) {
            provider = new ProviderConfigProperty();
            provider.setName("result-version");
            provider.setLabel("result-version");
            provider.setHelpText("result-version-hlp-txt");
            provider.setType(ProviderConfigProperty.LIST_TYPE);

            List<String> apiVersions = new ArrayList<>();
            apiVersions.add("v0");
            apiVersions.add("v1");
            provider.setOptions(apiVersions);
            provider.setDefaultValue("v1");
            configProperties.add(provider);
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
        return "hook-notifier";
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
        CibaNotificationConfigModel notificationConfig = realm.getCibaNotificationConfigByClientId(client.getClientId(), realm.getId());
        if (notificationConfig != null) {
            realm.removeCibaNotificationConfig(notificationConfig);
        }
    }
}

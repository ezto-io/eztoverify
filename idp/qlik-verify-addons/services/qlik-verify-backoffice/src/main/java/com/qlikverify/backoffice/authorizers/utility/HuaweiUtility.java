package com.qlikverify.backoffice.authorizers.utility;

import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.CibaAuthConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.authorizers.MobileAuthorizer.INTEGRATION_TYPE;
import static com.qlikverify.backoffice.authorizers.MobileAuthorizer.SDK;

public class HuaweiUtility {
    private static HuaweiUtility instance;

    private HuaweiUtility() {
    }

    public static HuaweiUtility getInstance() {
        if (instance == null) {
            instance = new HuaweiUtility();
        }
        return instance;
    }

    public boolean sendPushNotification(KeycloakSession session, CibaAuthConfigModel cibaAuthConfigModel,
                                        String requestId, String token, HashMap<String, Object> payload, EventBuilder event) {
        String clientId;
        String clientSecret;
        String integrationType = cibaAuthConfigModel.getConfig().get(INTEGRATION_TYPE);
        if (integrationType.equalsIgnoreCase(SDK)) {
            clientId = cibaAuthConfigModel.getConfig().get(HUAWEI_CLIENT_ID);
            clientSecret = cibaAuthConfigModel.getConfig().get(HUAWEI_CLIENT_SECRET);
        } else {
            clientId = session.realms().getRealmByName(Config.getAdminRealm()).getAttribute(HUAWEI_CLIENT_ID);
            clientSecret = session.realms().getRealmByName(Config.getAdminRealm()).getAttribute(HUAWEI_CLIENT_SECRET);
        }
        Response response = null;
        try {
            HttpUtility httpUtility = new HttpUtility();
            String body = "grant_type=client_credentials&client_id=" + clientId
                    + "&client_secret=" + clientSecret;
            response = httpUtility.callRequest("application/x-www-form-urlencoded", "POST", "https://oauth-login.cloud.huawei.com/oauth2/v3/token", body, null);
            if (!response.isSuccessful()) {
                event.event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER_ERROR);
                event.detail(REQUEST_ID, requestId).error("Failed to get Huawei access token to send the notification");
                return false;
            }
            String res = response.body().string();
            Map<String, String> resObject = JsonSerialization.readValue(res, Map.class);

            return sendNotification(cibaAuthConfigModel, requestId, clientId, resObject.get("access_token"), token, payload, event);
        } catch (IOException e) {
            event.event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER_ERROR);
            event.detail(REQUEST_ID, requestId).error("Failed to parse token response: " + e.getMessage());
        } finally {
            if (Objects.nonNull(response)) response.close();
        }
        return false;
    }

    private boolean sendNotification(CibaAuthConfigModel cibaAuthConfigModel,String requestId, String clientId, String accessToken, String pushToken, HashMap<String, Object> payload, EventBuilder event) {
        Response response = null;
        try {

            HashMap<Object, Object> payloadObject = getPayloadObject(cibaAuthConfigModel, pushToken, payload);

            HttpUtility httpUtility = new HttpUtility();
            response = httpUtility.callRequest("application/json", "POST", "https://push-api.cloud.huawei.com/v1/" + clientId + "/messages:send",
                    JsonSerialization.writeValueAsString(payloadObject), accessToken);
            if (!response.isSuccessful()) {
                event.event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER_ERROR);
                event.detail(REQUEST_ID, requestId).error("Failed to send Huawei notification: " + response.code());
                return false;
            }
            String res = response.body().string();
            Map<String, String> resObject = JsonSerialization.readValue(res, Map.class);
            return resObject.get("msg") != null && resObject.get("msg").equalsIgnoreCase("Success");
        } catch (IOException e) {
            event.event(EventType.EZ_TRANSACTION_NOTIFICATION_TRIGGER_ERROR);
            event.detail(REQUEST_ID, requestId).error("Failed to send Huawei notification: " + e.getMessage());
        } finally {
            if (Objects.nonNull(response)) response.close();
        }
        return false;
    }

    @NotNull
    private static HashMap<Object, Object> getPayloadObject(CibaAuthConfigModel cibaAuthConfigModel, String pushToken, HashMap<String, Object> payload) throws IOException {
        HashMap<Object, Object> payloadObject = new HashMap<>();

        HashMap<Object, Object> notification = new HashMap<>();
        notification.put("foreground_show", false);
        notification.put("title", cibaAuthConfigModel.getConfig().get("notification-title"));
        notification.put("body", cibaAuthConfigModel.getConfig().get("notification-body"));
        notification.put("click_action", new HashMap<>() {{
            put("type", 3);
        }});

        HashMap<Object, Object> message = new HashMap<>();
        message.put("token", List.of(pushToken));
        message.put("data", JsonSerialization.writeValueAsString(new HashMap<>() {{
            put("payload", JsonSerialization.writeValueAsString(payload));
        }}));

        message.put("android", new HashMap<>() {{
            put("notification", notification);
        }});
        payloadObject.put("message", message);
        return payloadObject;
    }

}

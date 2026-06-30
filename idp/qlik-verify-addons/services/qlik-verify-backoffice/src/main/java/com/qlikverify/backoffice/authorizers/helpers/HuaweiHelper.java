package com.qlikverify.backoffice.authorizers.helpers;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.qlikverify.backoffice.authorizers.utility.HuaweiUtility;
import com.qlikverify.backoffice.models.HuaweiCredentialModel;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.CibaAuthConfigModel;
import org.keycloak.models.KeycloakSession;

import java.util.HashMap;

public class HuaweiHelper {
    static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private HuaweiHelper() {
        throw new UnsupportedOperationException("Huawei helper cannot be instantiated");
    }

    public static boolean sendHuaweiPushNotification(KeycloakSession session, CibaAuthConfigModel cibaAuthConfigModel,
                                                     String requestId, String token, HashMap<String, Object> payload, EventBuilder event) {
        return HuaweiUtility.getInstance().sendPushNotification(session, cibaAuthConfigModel, requestId, token, payload, event);
    }
}

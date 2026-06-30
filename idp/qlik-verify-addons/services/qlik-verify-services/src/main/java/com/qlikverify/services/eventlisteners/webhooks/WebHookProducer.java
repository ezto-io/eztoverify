package com.qlikverify.services.eventlisteners.webhooks;


import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.sms.utility.HttpUtility;

import javax.ws.rs.core.MediaType;

public class WebHookProducer {

    public static void publishEvent(KeycloakSession session, HttpUtility httpUtility, String server, String authorization, String payload) {
        try {
            if (server == null)
                return;

            resetThreadContext();

            okhttp3.Response response = httpUtility.callRequest(MediaType.APPLICATION_JSON, "POST", server, payload, authorization);
            response.close();

            if (response == null)
                throw new Exception("Url cannot be reached");

            if (response.code() == 400)
                throw new Exception(response.message());

            if (response.code() == 401)
                throw new Exception("Authentication failed");

            if (response.code() == 500)
                throw new Exception("Internal server error");
        } catch (Exception e) {
            RealmModel realm = session.getContext().getRealm();
            RealmEventsConfigRepresentation config = ModelToRepresentation.toEventsConfigReprensetation(realm);
            if (config.getEventsListeners().size() > 0) {
                config.getEventsListeners().remove(WebHookEventListenerProviderFactory.ID);
                new RealmManager(session).updateRealmEventsConfig(config, realm);
            }
        }
    }

    private static void resetThreadContext() {
        Thread.currentThread().setContextClassLoader(null);
    }

    public static boolean test(HttpUtility httpUtility, String url, String authorization, String payload) {
        try {
            if (url == null)
                return false;

            okhttp3.Response response = httpUtility.callRequest(MediaType.APPLICATION_JSON, "POST", url, payload, authorization);
            response.close();

            if (response == null || response.code() == 400 || response.code() == 401 || response.code() == 500)
                return false;
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}

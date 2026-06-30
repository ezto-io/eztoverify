package com.qlikverify.services.eventlisteners.splunk;


import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.sms.utility.HttpUtility;

import javax.ws.rs.core.MediaType;
import java.util.Map;

import static com.qlikverify.services.constants.Constants.SPLUNK_PORT;

public class SplunkProducer {

    public static void publishEvent(KeycloakSession session, HttpUtility httpUtility, Map<String, String> config, String payload) {
        try {
            resetThreadContext();

            String port = "8088";
            if (config.get(SPLUNK_PORT) != null) {
                port = config.get(SPLUNK_PORT);
            }

            String url = "https://" + config.get("SPLUNK_DOMAIN") + ":" + port + "/services/collector";

            okhttp3.Response response = httpUtility.callRequest(MediaType.APPLICATION_JSON, "POST", url, payload, "Splunk " + config.get("SPLUNK_TOKEN"));
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
            disableEventListener(session);
        }
    }

    private static void disableEventListener(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        RealmEventsConfigRepresentation rnconfig = ModelToRepresentation.toEventsConfigReprensetation(realm);
        if (rnconfig.getEventsListeners().size() > 0) {
            rnconfig.getEventsListeners().remove(SplunkEventListenerProviderFactory.ID);
            new RealmManager(session).updateRealmEventsConfig(rnconfig, realm);
        }
    }

    private static void resetThreadContext() {
        Thread.currentThread().setContextClassLoader(null);
    }

    public static boolean test(RealmModel realm, HttpUtility httpUtility, Map<String, String> config, String payload) {
        try {
            String port = "8088";
            if (config.get(SPLUNK_PORT) != null) {
                port = config.get(SPLUNK_PORT);
            }

            String url = "https://" + config.get("SPLUNK_DOMAIN") + ":" + port + "/services/collector";

            okhttp3.Response response = httpUtility.callRequest(MediaType.APPLICATION_JSON, "POST", url, payload, "Splunk " + config.get("SPLUNK_TOKEN"));
            response.close();


            if (response == null)
                throw new Exception("Url cannot be reached");

            if (response.code() == 400)
                throw new Exception(response.message());

            if (response.code() == 401)
                throw new Exception("Authentication failed");

            if (response.code() == 500)
                throw new Exception("Internal server error");

            return true;
        } catch (Exception err) {
            return false;
        }
    }
}

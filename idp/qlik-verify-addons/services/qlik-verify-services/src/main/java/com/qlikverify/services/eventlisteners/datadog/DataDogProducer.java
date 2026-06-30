package com.qlikverify.services.eventlisteners.datadog;


//import com.datadog.api.client.ApiClient;
//import com.datadog.api.client.v2.api.LogsApi;
//import com.datadog.api.client.v2.model.ContentEncoding;
//import com.datadog.api.client.v2.model.HTTPLogItem;
import com.datadog.api.v1.client.Configuration;
import com.datadog.api.v1.client.model.ContentEncoding;
import com.datadog.api.v1.client.model.HTTPLogItem;
import com.datadog.api.v1.client.ApiClient;
import com.datadog.api.v1.client.api.LogsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.services.managers.RealmManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataDogProducer {

    public static void publishEvent(KeycloakSession session, String region, String authorization, String payload, String eventType) {
        try {
            if (authorization == null)
                return;

            resetThreadContext();
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            HashMap<String, String> serverVariables = new HashMap<>();
            serverVariables.put("site", getServer(region));
            defaultClient.setServerVariables(serverVariables);

            HashMap<String, String> secrets = new HashMap<>();
            secrets.put("apiKeyAuth", authorization);
            defaultClient.configureApiKeys(secrets);

            LogsApi apiInstance = new LogsApi(defaultClient);
            List<HTTPLogItem> body = new ArrayList<>();
            HTTPLogItem httpLogItem = new HTTPLogItem();
            httpLogItem.setDdsource(session.getContext().getRealm().getName() + ".qlikverify.com");
            httpLogItem.setDdtags("event_type:" + eventType);
            httpLogItem.setMessage(payload);
            body.add(httpLogItem);
            ContentEncoding contentEncoding = ContentEncoding.fromValue("gzip"); // ContentEncoding | HTTP header used to compress the media-type.

            apiInstance.submitLog(body, new LogsApi.SubmitLogOptionalParameters()
                    .contentEncoding(contentEncoding));
        } catch (Exception e) {
            RealmModel realm = session.getContext().getRealm();
            RealmEventsConfigRepresentation config = ModelToRepresentation.toEventsConfigReprensetation(realm);
            if (config.getEventsListeners().size() > 0) {
                config.getEventsListeners().remove(DataDogEventListenerProviderFactory.ID);
                new RealmManager(session).updateRealmEventsConfig(config, realm);
            }
        }
    }

    private static String getServer(String region) {
        switch (region) {
            case "EU":
                return "datadoghq.eu";
            case "US3":
                return "us3.datadoghq.com";
            case "US1-FED":
                return "ddog-gov.com";
            case "US":
            default:
                return "datadoghq.com";
        }
    }

    private static void resetThreadContext() {
        Thread.currentThread().setContextClassLoader(null);
    }

    public static boolean test(RealmModel realm, String region, String apiKey) {
        try {
            if (apiKey == null)
                return false;

            resetThreadContext();
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            HashMap<String, String> serverVariables = new HashMap<>();
            serverVariables.put("site", getServer(region));
            defaultClient.setServerVariables(serverVariables);

            HashMap<String, String> secrets = new HashMap<>();
            secrets.put("apiKeyAuth", apiKey);
            defaultClient.configureApiKeys(secrets);

            LogsApi apiInstance = new LogsApi(defaultClient);
            List<HTTPLogItem> body = new ArrayList<>();
            HTTPLogItem httpLogItem = new HTTPLogItem();
            httpLogItem.setDdsource(realm.getName() + ".qlikverify.com");
            httpLogItem.setDdtags("event_type:test_event");
            httpLogItem.setMessage("{ \"tenant\": "+realm.getName()+", \"type\": \"test\" , \"message\":\"ezto auth Test Event\"}");
            body.add(httpLogItem);
            ContentEncoding contentEncoding = ContentEncoding.fromValue("gzip"); // ContentEncoding | HTTP header used to compress the media-type.

            Object res = apiInstance.submitLog(body, new LogsApi.SubmitLogOptionalParameters()
                    .contentEncoding(contentEncoding));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

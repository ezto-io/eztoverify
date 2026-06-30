package com.qlikverify.services.backgroundService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qlikverify.services.backgroundService.backgroundEmailService.BackgroundEmailRequestBody;
import com.qlikverify.services.backgroundService.backgroundHookNotifierService.BackgroundHookNotifierRequestBody;
import com.qlikverify.services.backgroundService.backgroundSyncService.BackgroundSyncCancelRequestBody;
import com.qlikverify.services.backgroundService.backgroundSyncService.BackgroundSyncRequestBody;
import com.qlikverify.services.scheduleresult.backgroundScheduledJob.BackgroundScheduledJobBody;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.validation.Validation;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class BackgroundServiceImpl implements BackgroundService {

    private static final Logger logger = Logger.getLogger(BackgroundServiceImpl.class);
    private static final String LOG_TAG = "BACKGROUND_SERVICE:: ";
    private static final String BACKGROUND_JOB_URL = EnvironmentReader.getValue("BACKGROUND_JOB_URL");
    private static KeycloakSession session = null;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected BackgroundServiceImpl() {
    }

    public static BackgroundServiceImpl getInstance(KeycloakSession session) {
        if (!Validation.isBlank(BACKGROUND_JOB_URL)) {
            BackgroundServiceImpl.session = session;
            return BackgroundServiceInstanceHolder.INSTANCE;
        } else {
            throw new RuntimeException("Failed to connect to Background job service. Background job URL is null");
        }
    }

    @Override
    public void createBackgroundEmailJob(BackgroundEmailRequestBody requestBody) {
        sendJobRequest(requestBody, "email");
    }

    @Override
    public void createBackgroundSyncJob(BackgroundSyncRequestBody requestBody, String providerId) {
        sendJobRequest(requestBody, providerId);
    }

    @Override
    public void createBackgroundTriggerJob(BackgroundJobRequestBody requestBody, String providerId) {
        sendJobRequest(requestBody, providerId);
    }

    @Override
    public void createBackgroundSyncCancelJob(BackgroundSyncCancelRequestBody requestBody, String jobId, String connectionId) {
        sendJobRequest(requestBody, jobId, connectionId);
    }

    @Override
    public void createBackgroundHookNotifierJob(BackgroundHookNotifierRequestBody requestBody) {
        sendJobRequest(requestBody, "hook-notifier");
    }

    @Override
    public void createBackgroundRecurringScheduledJob(BackgroundScheduledJobBody requestBody, String providerId) {
        sendRecurringScheduledJobRequest(requestBody, providerId);
    }

    @Override
    public void createBackgroundScheduledJob(BackgroundJobRequestBody requestBody, String providerId) {
        sendScheduledJobRequest(requestBody, providerId);
    }

    @Override
    public boolean getRecurringJobStatus(String jobId) {
        HttpUtility httpUtility = new HttpUtility();
        try (Response response = httpUtility.callRequest(javax.ws.rs.core.MediaType.APPLICATION_JSON, "GET", BACKGROUND_JOB_URL + "/jobs/recurring/" + jobId,
                null, null, null, 1)) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                Map<?, ?> responseData = JsonSerialization.readValue(responseBody, Map.class);
                Object status = responseData.getOrDefault("status", null);
                return status == null || !status.equals(404);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean getJobStatus(String jobId) {
        HttpUtility httpUtility = new HttpUtility();
        try (Response response = httpUtility.callRequest(javax.ws.rs.core.MediaType.APPLICATION_JSON, "GET", BACKGROUND_JOB_URL + "/jobs/status/" + jobId,
                null, null, null, 1)) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                Map<?, ?> responseData = JsonSerialization.readValue(responseBody, Map.class);
                Object status = responseData.getOrDefault("status", null);
                return status == null || !status.equals(404);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendJobRequest(BackgroundJobRequestBody requestBody, String provider) {
        Executors.newCachedThreadPool().execute(() -> KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), innerSession -> {
            Response response;
            HttpUtility httpUtility = new HttpUtility();
            response = httpUtility.callRequest("application/json", "POST", BACKGROUND_JOB_URL + "/jobs/" + provider + "/queue",
                    objectMapper.writeValueAsString(requestBody), null);
            try {
                Map<?, ?> responseBody = JsonSerialization.readValue(response.body().string(), Map.class);
                String jobId = responseBody.get("body").toString();
                if ((provider.equals("duo-ws-sync")) || (provider.equals("azure-sync")) || (provider.equals("google-ws-sync")) || (provider.equals("okta-ws-sync"))) {
                    BackgroundSyncRequestBody backgroundSyncRequestBody = (BackgroundSyncRequestBody) requestBody;
                    String realmId = ((BackgroundSyncRequestBody) requestBody).getRealmId();
                    Map<?, ?> map = JsonSerialization.readValue(backgroundSyncRequestBody.getRepresentation(), Map.class);
                    if (innerSession.realms().getRealm(realmId).getAttribute(map.get("connectionId").toString()) != null) {
                        innerSession.realms().getRealm(realmId).setAttribute(map.get("connectionId").toString() + "-jobId", jobId);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process the response from Jobrunr");
            }
            if (!response.isSuccessful()) {
                try {
                    throw new RuntimeException(response.body().string());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to connect to Jobrunr");
                }
            } else {
                logger.info(LOG_TAG + "Request sent to Jobrunr");
                response.close();
            }
        }));
    }

    private void sendScheduledJobRequest(BackgroundJobRequestBody requestBody, String provider) {
        Executors.newCachedThreadPool().execute(() -> KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), innerSession -> {
            Response response;
            HttpUtility httpUtility = new HttpUtility();
            response = httpUtility.callRequest("application/json", "POST", BACKGROUND_JOB_URL + "/jobs/" + provider + "/scheduleJob",
                    objectMapper.writeValueAsString(requestBody), null);
            if (!response.isSuccessful()) {
                try {
                    throw new RuntimeException(response.body().string());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to connect to Jobrunr");
                }
            } else {
                logger.info(LOG_TAG + "Scheduled job created");
                response.close();
            }
        }));
    }

    private void sendRecurringScheduledJobRequest(BackgroundJobRequestBody requestBody, String provider) {
        Executors.newCachedThreadPool().execute(() -> KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), innerSession -> {
            Response response;
            HttpUtility httpUtility = new HttpUtility();
            response = httpUtility.callRequest("application/json", "POST", BACKGROUND_JOB_URL + "/jobs/" + provider + "/schedule",
                    objectMapper.writeValueAsString(requestBody), null);
            if (!response.isSuccessful()) {
                try {
                    throw new RuntimeException(response.body().string());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to connect to Jobrunr");
                }
            } else {
                logger.info(LOG_TAG + "Scheduled job created");
                response.close();
            }
        }));
    }

    private void sendJobRequest(BackgroundSyncCancelRequestBody requestBody, String jobId, String connectionId) {
        Executors.newCachedThreadPool().execute(() -> KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), innerSession -> {
            Response response;
            HttpUtility httpUtility = new HttpUtility();
            response = httpUtility.callRequest("application/json", "DELETE", BACKGROUND_JOB_URL + "/jobs/" + jobId + "/deleteScheduledJob",
                    objectMapper.writeValueAsString(requestBody), null);
            if (!response.isSuccessful()) {
                try {
                    throw new RuntimeException(response.body().string());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to connect to Jobrunr");
                }
            } else {
                List<String> eventListeners = innerSession.getContext().getRealm().getEventsListenersStream().collect(Collectors.toList());
                innerSession.getContext().getRealm().setEventsListeners(eventListeners.stream().filter(x -> !x.contains("sync-cancel")).collect(Collectors.toSet()));
                if (innerSession.getContext().getRealm().getAttribute(connectionId + "-jobId") != null) {
                    innerSession.getContext().getRealm().removeAttribute(connectionId + "-jobId");
                }
                response.close();
            }
        }));
    }

    //~ Inner Classes ======================================================================================================================

    private static class BackgroundServiceInstanceHolder {

        //~ Static fields/initializers =====================================================================================================

        protected static final BackgroundServiceImpl INSTANCE = new BackgroundServiceImpl();

        static {
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }
}

package com.qlikverify.backoffice.extensions;

import com.qlikverify.constants.DevConstants;
import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.scheduleresult.backgroundScheduledJob.BackgroundScheduledJobBody;
import com.qlikverify.services.scheduleresult.backgroundScheduledJob.RestPayload;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProviderFactory;

import javax.ws.rs.core.MediaType;

public class EVSchedulerProviderFactory implements EztoPublicRealmResourceProviderFactory {
    Logger logger = Logger.getLogger(EVSchedulerProviderFactory.class);

    private static final String SCHEDULED_MANAGEMENT_KEY_JOB_ID = "63b167a5-e968-4e13-8edf-77bd348f8cd5";

    private static final String JOB_ID = "trigger-job";

    @Override
    public EztoPublicRealmResourceProvider create(KeycloakSession session) {
        return new EVSchedulerProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        String variant = EnvironmentReader.getValue("VARIANT");
        if (variant == null) return;

        factory.register(event -> {
            if (event instanceof PostMigrationEvent) {
                if (variant.equalsIgnoreCase("ezto verify")) {
                    KeycloakModelUtils.runJobInTransaction(factory, this::checkForScheduledJobForManagementKeys);
                }
            }
        });
    }

    private void checkForScheduledJobForManagementKeys(KeycloakSession session) {
        boolean isJobAvailable = BackgroundServiceImpl.getInstance(session).getJobStatus(SCHEDULED_MANAGEMENT_KEY_JOB_ID);
        if (!isJobAvailable) {
            RestPayload restPayload = RestPayload
                    .builder()
                    .auth(null)
                    .url(EnvironmentReader.getValue("EZTO_CORE_SERVICE", DevConstants.DEFAULT_HOST) + "/auth/realms/api/ev-scheduler/validate-management-keys")
                    .method("GET")
                    .mediaType(MediaType.APPLICATION_JSON)
                    .headers(null)
                    .body(null)
                    .build();

            BackgroundScheduledJobBody backgroundScheduledJobBody = BackgroundScheduledJobBody
                    .builder()
                    .jobId(SCHEDULED_MANAGEMENT_KEY_JOB_ID)
                    .interval(Long.parseLong(EnvironmentReader.getValue("ACCESS_KEY_VALIDATION_INTERVAL", "86400")))
                    .payload(restPayload)
                    .build();
            BackgroundServiceImpl.getInstance(session).createBackgroundRecurringScheduledJob(backgroundScheduledJobBody, JOB_ID);
        }
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return "ev-scheduler";
    }
}

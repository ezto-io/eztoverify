package com.qlikverify.services.scheduleresult;

import com.qlikverify.constants.DevConstants;
import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.scheduleresult.backgroundScheduledJob.BackgroundScheduledJobBody;
import com.qlikverify.services.scheduleresult.backgroundScheduledJob.RestPayload;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.events.*;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DeadLetterHookListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(DeadLetterHookListenerProvider.class);
    private KeycloakSession session;
    private RealmProvider model;
    private DeadLetterHookListenerProviderFactory factory;

    public DeadLetterHookListenerProvider(KeycloakSession session, DeadLetterHookListenerProviderFactory factory) {
        this.session = session;
        this.model = session.realms();
        this.factory = factory;
    }

    @Override
    public void onEvent(Event event, Map<String, String> config) {
        if (event.getType() == EventType.EZ_DEAD_LETTER_HOOK) {
            triggerEvent(event, config, false);
        }
    }

    @Override
    public void onEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {

    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return factory;
    }

    @Override
    public void close() {

    }

    private void triggerEvent(Event event, Map<String, String> config, boolean includeRepresentation) {
        if (event.getType() == EventType.EZ_DEAD_LETTER_HOOK) {

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession innerSession) {
                    EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session, null);
                    try {
                        session.getContext().setRealm(innerSession.realms().getRealm(event.getRealm().getId()), innerSession);
                        HashMap<String, Object> representation = new HashMap<>();
                        representation.put("requestId", event.getDetails().get("requestId"));
                        representation.put("clientId", event.getClient().getClientId());
                        representation.put("realmId", event.getRealm().getId());
                        representation.put("userId", event.getUser().getUsername());

                        String requestId = event.getDetails().get("requestId");
                        CibaAuthConfigModel cibaAuthConfigModel;
                        if(session.getContext().getClient().isSandboxMode()) {
                            ClientModel clientBySandboxClientId = session.realms().getRealm(event.getRealm().getId()).getClientBySandboxClientId(event.getClient().getClientId());
                            cibaAuthConfigModel = session.realms().getRealm(event.getRealm().getId()).getCibaAuthConfigByClientId(clientBySandboxClientId.getClientId(), event.getRealm().getId());
                        } else {
                            cibaAuthConfigModel = session.realms().getRealm(event.getRealm().getId()).getCibaAuthConfigByClientId(event.getClient().getClientId(), event.getRealm().getId());
                        }

                        Long exp_time = getExpiryDurationInRawSec(cibaAuthConfigModel.getConfig().get("verify-expiry-time"));
                        String actionToken = BackgroundJobActionTokenVerifier.generateToken(session, exp_time + 30, "trigger-job");

                        RestPayload restPayload = RestPayload
                                .builder()
                                .auth(actionToken)
                                .url( EnvironmentReader.getValue("EZTO_CORE_SERVICE", DevConstants.DEFAULT_HOST) + "/auth/realms/api/dead-letter-hook-notification/send-result")
                                .method("POST")
                                .mediaType(MediaType.APPLICATION_JSON)
                                .headers(null)
                                .body(representation)
                                .build();

                        BackgroundScheduledJobBody backgroundScheduledJobBody = BackgroundScheduledJobBody
                                .builder()
                                .jobId(requestId)
                                .scheduled_time(exp_time)
                                .payload(restPayload)
                                .build();

                        BackgroundServiceImpl.getInstance(session).createBackgroundScheduledJob(backgroundScheduledJobBody, "trigger-job");
                    } catch (Exception e) {
                        HashMap<String, String> eventDetails = new HashMap<>();
                        eventDetails.put("realm", session.getContext().getRealm().getId());
                        eventDetails.put("clientId", session.getContext().getClient().getClientId());
                        eventBuilder.event(EventType.EZ_DEAD_LETTER_HOOK_ERROR);
                        eventBuilder.ipAddress(event.getIpAddress());
                        eventBuilder.getEvent().setDetails(eventDetails);
                        eventBuilder.error("Unable to send response to hook");
                        log.error(e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private Long getExpiryDurationInRawSec(String ttl) {
        boolean validate = validateTTL(ttl);
        if (!validate)
            ttl = "1h";
        String timeUnit = "";
        if (Character.isAlphabetic(ttl.charAt(ttl.length() - 1))) {
            timeUnit = ttl.substring(ttl.length() - 1);
            ttl = ttl.substring(0, ttl.length() - 1);
        } else {
            timeUnit = "h";
        }
        long duration = Long.parseLong(ttl);
        long expiry = 120;

        if (timeUnit.equals("d")) {
            expiry = TimeUnit.DAYS.toSeconds(duration);
        } else if (timeUnit.equals("h")) {
            expiry = TimeUnit.HOURS.toSeconds(duration);
        } else if (timeUnit.equals("s")) {
            expiry = duration;
        } else if(timeUnit.equals("m")) {
            expiry = TimeUnit.MINUTES.toSeconds(duration);
        }
        return expiry;
    }

    private static boolean validateTTL(String ttl) {
        if (ttl == null || ttl.length() == 0) {
            return false;
        }
        try {
            ttl = ttl.substring(0, ttl.length() - 1);
            Long.parseLong(ttl);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}


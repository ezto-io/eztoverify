package com.qlikverify.services.eventlisteners.googlewssync;

import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.backgroundService.backgroundSyncService.BackgroundSyncRequestBody;
import com.qlikverify.services.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.events.*;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.JsonSerialization;

import java.util.HashMap;
import java.util.Map;

public class GoogleWSSyncListenerProvider implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(GoogleWSSyncListenerProvider.class);

    private KeycloakSession session;
    private RealmProvider model;
    private GoogleWSSyncListenerProviderFactory factory;
    private final EventListenerTransaction tx = new EventListenerTransaction(this::triggerAdminEvent, null);

    public GoogleWSSyncListenerProvider(KeycloakSession session, GoogleWSSyncListenerProviderFactory factory) {
        this.session = session;
        this.model = session.realms();
        this.factory = factory;
        this.session.getTransactionManager().enlistAfterCompletion(tx);
    }

    @Override
    public void close() {
    }

    @Override
    public void onEvent(Event event, Map<String, String> config) {

    }

    private void triggerAdminEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        if (event.getOperationType() == OperationType.TRIGGER && event.getResourceType() == ResourceType.GOOGLE_WS_SYNC) {

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession innerSession) {
                    try {
                        String connId = (String) JsonSerialization.readValue(event.getRepresentation(), HashMap.class).get("connectionId");
                        String credentails = innerSession.realms().getRealm(event.getRealmId()).getUserSyncConfigByProviderId(connId).getConfig().get("access_token");
                        innerSession.getContext().setRealm(innerSession.realms().getRealm(event.getRealmId()));
                        BackgroundServiceImpl.getInstance(session).createBackgroundSyncJob(new BackgroundSyncRequestBody(
                                factory.getId(),
                                credentails,
                                event.getRealmId(),
                                event.getRepresentation(),
                                event.getAuthDetails().getUser().getId(),
                                BackgroundJobActionTokenVerifier.generateToken(session,Long.parseLong(String.valueOf(GeneralHelper.getExpiryDuration("1h"))), "google-sync")
                        ), factory.getId()) ;
                    } catch (Exception e) {
                        EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session, null);
                        eventBuilder.event(EventType.GOOGLE_WS_SYNC_ERROR);
                        eventBuilder.realm(session.getContext().getRealm());
                        eventBuilder.error(e.getMessage());
                        log.error(e.getMessage());
                    }
                }
            });
        }
    }

    @Override
    public void onEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        if (event.getOperationType() == OperationType.TRIGGER && event.getResourceType() == ResourceType.GOOGLE_WS_SYNC) {
            tx.addAdminEvent(event,config,includeRepresentation);
        }
    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return factory;
    }
}

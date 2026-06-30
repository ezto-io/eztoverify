package com.qlikverify.services.userSync.syncCancel;

import com.qlikverify.services.backgroundService.BackgroundServiceImpl;
import com.qlikverify.services.backgroundService.actionToken.BackgroundJobActionTokenVerifier;
import com.qlikverify.services.backgroundService.backgroundSyncService.BackgroundSyncCancelRequestBody;
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

import java.util.Map;

public class SyncCancelListenerProvider implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(SyncCancelListenerProvider.class);
    private KeycloakSession session;
    private RealmProvider model;
    private SyncCancelListenerProviderFactory factory;
    private final EventListenerTransaction tx = new EventListenerTransaction(this::triggerAdminEvent, null);
    public SyncCancelListenerProvider(KeycloakSession session, SyncCancelListenerProviderFactory factory) {
        this.session = session;
        this.model = session.realms();
        this.factory = factory;
        this.session.getTransactionManager().enlistAfterCompletion(tx);
    }
    @Override
    public void onEvent(Event event, Map<String, String> config) {

    }

    @Override
    public void onEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        if (event.getOperationType() == OperationType.TRIGGER && event.getResourceType() == ResourceType.SYNC_CANCEL) {
            tx.addAdminEvent(event,config,includeRepresentation);
        }
    }

    private void triggerAdminEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        if (event.getOperationType() == OperationType.TRIGGER && event.getResourceType() == ResourceType.SYNC_CANCEL) {

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession innerSession) {
                    EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session, null);
                    try {
                        session.getContext().setRealm(innerSession.realms().getRealm(event.getRealmId()), innerSession);
                        Map<String, Object> representation = JsonSerialization.readValue(event.getRepresentation().toString(), Map.class);
                        BackgroundServiceImpl.getInstance(innerSession).createBackgroundSyncCancelJob(new BackgroundSyncCancelRequestBody(
                                event.getRealmId(),
                                representation.get("connectionId").toString(),
                                representation.get("jobId").toString(),
                                BackgroundJobActionTokenVerifier.generateToken(session,Long.parseLong(String.valueOf(GeneralHelper.getExpiryDuration("1h"))), "sync-cancel")
                        ), representation.get("jobId").toString(), representation.get("connectionId").toString());
                    } catch (Exception e) {
                        eventBuilder.event(EventType.USER_SYNC_CANCEL_ERROR);
                        eventBuilder.realm(session.getContext().getRealm());
                        eventBuilder.error(e.getMessage());
                        log.error(e.getMessage());
                    }
                }
            });
        }
    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return factory;
    }

    @Override
    public void close() {

    }
}

package com.qlikverify.services.eventlisteners.azureadsync;

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
import org.keycloak.models.RealmProvider;
import org.keycloak.util.JsonSerialization;

import java.util.Map;

public class AzureADSyncListenerProvider implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(AzureADSyncListenerProvider.class);

    private KeycloakSession session;
    private RealmProvider model;
    private AzureADSyncListenerProviderFactory factory;
    private final EventListenerTransaction tx = new EventListenerTransaction(this::triggerAdminEvent, null);

    public AzureADSyncListenerProvider(KeycloakSession session,AzureADSyncListenerProviderFactory factory) {
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
        if (event.getOperationType() == OperationType.TRIGGER && event.getResourceType() == ResourceType.AZURE_SYNC) {
            EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session, null);
            try {
                if (config == null) {
                    String connectionId = JsonSerialization.readValue(event.getRepresentation(), Map.class).get("connectionId").toString();
                    if(connectionId != null){
                        config = session.getContext().getRealm().getUserSyncConfigByProviderId(connectionId).getConfig();
                    }
                }

                BackgroundServiceImpl.getInstance(session).createBackgroundSyncJob(new BackgroundSyncRequestBody(
                        factory.getId(),
                        JsonSerialization.writeValueAsString(config),
                        event.getRealmId(),
                        event.getRepresentation(),
                        event.getAuthDetails().getUser().getId(),
                        BackgroundJobActionTokenVerifier.generateToken(session, Long.parseLong(String.valueOf(GeneralHelper.getExpiryDuration("1h"))), "azure-sync")
                ), factory.getId());
            } catch (Exception e) {
                eventBuilder.event(EventType.AZURE_SYNC_ERROR);
                eventBuilder.realm(session.getContext().getRealm());
                eventBuilder.error(e.getMessage());
                log.error(e.getMessage());
            }
        }
    }

    @Override
    public void onEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        if (event.getOperationType() == OperationType.TRIGGER && event.getResourceType() == ResourceType.AZURE_SYNC) {
            tx.addAdminEvent(event,config,includeRepresentation);
        }
    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return factory;
    }
}

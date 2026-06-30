package com.qlikverify.services.eventlisteners.reports;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

public class ReportEventListenerProvider implements EventListenerProvider {

    KeycloakSession session;
    ReportEventListenerProviderFactory factory;
    private final EventListenerTransaction tx = new EventListenerTransaction(this::triggerAdminEvent, null);
    public ReportEventListenerProvider(KeycloakSession session,ReportEventListenerProviderFactory factory) {
        this.session = session;
        this.factory = factory;
        this.session.getTransactionManager().enlistAfterCompletion(tx);
    }

    @Override
    public void onEvent(Event event, Map<String, String> config) {
        // NOP
    }

    @Override
    public void onEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        switch (event.getOperationType()) {
            case TRIGGER:
                if (event.getResourceType() == ResourceType.REPORT) {
                    tx.addAdminEvent(event, config, includeRepresentation);
                }
                break;
        }
    }

    private void triggerAdminEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        switch (event.getOperationType()) {
            case TRIGGER:
                switch (event.getResourceType()) {
                    case REPORT:
                        ReportProducer.triggerReportCreation(session, event, config, includeRepresentation);
                        break;
                }
                break;
        }

    }


    @Override
    public EventListenerProviderFactory getFactory() {
        return factory;
    }

    @Override
    public void close() {
        // NOP
    }
}

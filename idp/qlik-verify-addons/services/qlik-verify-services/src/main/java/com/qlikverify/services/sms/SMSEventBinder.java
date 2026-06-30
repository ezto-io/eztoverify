package com.qlikverify.services.sms;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

public class SMSEventBinder {
    EventBuilder eventBuilder;
    KeycloakSession session;

    public SMSEventBinder(KeycloakSession session) {
        this.session = session;
        this.eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
    }

    protected void sendSuccess(EventType eventType, String message) {
        updateEvent(eventType);
        eventBuilder.detail("Detail", message).success();
    }

    protected void sendError(EventType eventType, String message) {
        updateEvent(eventType);
        eventBuilder.error(message);
    }

    private void updateEvent(EventType eventType) {
        eventBuilder.event(eventType);
        if (session != null && session.getContext() != null) {
            if (session.getContext().getClient() != null) {
                eventBuilder.client(session.getContext().getClient().getClientId());
            }

            if (session.getContext().getAuthenticationSession() != null && session.getContext().getAuthenticationSession().getAuthenticatedUser() != null) {
                eventBuilder.user(session.getContext().getAuthenticationSession().getAuthenticatedUser());
            }
        }
    }
}

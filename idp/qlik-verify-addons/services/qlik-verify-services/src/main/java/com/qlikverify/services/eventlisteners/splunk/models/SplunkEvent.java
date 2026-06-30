package com.qlikverify.services.eventlisteners.splunk.models;

import org.keycloak.events.Event;

public class SplunkEvent {
    private Event event;

    public SplunkEvent(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }
}

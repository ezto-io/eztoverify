package com.qlikverify.services.events.models;

import org.keycloak.events.Event;

public class ElasticEventModel implements  ElasticModel {
    private Event eventData;
    private String eventType;

    public ElasticEventModel(){}

    public ElasticEventModel(Event eventData) {
        this.eventData = eventData;
        this.eventType = "log";
    }

    public String getEventType() {
        return eventType;
    }

    public Event getEventData() {
        return eventData;
    }
}

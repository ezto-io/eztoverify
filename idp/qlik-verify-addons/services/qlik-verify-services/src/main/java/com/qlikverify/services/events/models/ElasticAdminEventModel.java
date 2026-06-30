package com.qlikverify.services.events.models;

import org.keycloak.events.admin.AdminEvent;

public class ElasticAdminEventModel implements  ElasticModel {
    private AdminEvent eventData;
    private String eventType;

    public ElasticAdminEventModel(){

    }

    public ElasticAdminEventModel(AdminEvent eventData) {
        this.eventData = eventData;
        this.eventType = "admin_log";
    }

    public String getEventType() {
        return eventType;
    }

    public AdminEvent getEventData() {
        return eventData;
    }
}

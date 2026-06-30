package com.qlikverify.services.eventlisteners.splunk.models;

import org.keycloak.events.admin.AdminEvent;

public class SplunkAdminEvent {
    private AdminEvent event;

    public SplunkAdminEvent(AdminEvent event) {
        this.event = event;
    }

    public AdminEvent getEvent() {
        return event;
    }
}

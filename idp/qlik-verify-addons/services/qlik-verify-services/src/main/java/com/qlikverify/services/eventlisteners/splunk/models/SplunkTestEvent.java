package com.qlikverify.services.eventlisteners.splunk.models;

public class SplunkTestEvent {
    private String message;
    private String type;
    private String tenant;

    public SplunkTestEvent(String tenant, String message) {
        this.tenant = tenant;
        this.message = message;
        this.type = "test";
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getTenant() {
        return tenant;
    }
}

package com.qlikverify.extensions.admin.controllers.workflows.misc.models;

public class EventMessageData {
    private String message;

    public EventMessageData(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

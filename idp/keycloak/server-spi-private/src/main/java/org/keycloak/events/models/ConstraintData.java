package org.keycloak.events.models;

public class ConstraintData {

    private String message;
    private String redirectUrl;

    public ConstraintData(String message, String redirectUrl) {
        this.message = message;
        this.redirectUrl = redirectUrl;
    }

    public String getMessage() {
        return message;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }
}

package org.keycloak.services;

public class AuditTestResponse {

    boolean isSuccess;
    String message;

    public AuditTestResponse(boolean isSuccess, String message) {
        this.isSuccess = isSuccess;
        this.message = message;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getMessage() {
        return message;
    }
}

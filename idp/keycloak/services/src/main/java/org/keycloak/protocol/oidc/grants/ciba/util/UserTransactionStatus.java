package org.keycloak.protocol.oidc.grants.ciba.util;


public enum UserTransactionStatus {

    SUCCESS("SUCCESS"),
    FAILURE("FAILURE"),
    ACCESS_DENIED("ACCESS DENIED"),
    USER_CANCELLED("USER CANCELLED"),
    PENDING("PENDING"),
    EXPIRED("EXPIRED");

    private final String displayName;

    UserTransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}

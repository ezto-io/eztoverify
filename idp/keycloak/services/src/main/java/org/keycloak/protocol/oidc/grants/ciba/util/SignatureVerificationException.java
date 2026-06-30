package org.keycloak.protocol.oidc.grants.ciba.util;

public class SignatureVerificationException extends Exception {
    private static final long serialVersionUID = 2L;
    private final String sigHeader;

    public SignatureVerificationException(String message, String sigHeader) {
        super(message);
        this.sigHeader = sigHeader;
    }

    public String getSigHeader() {
        return this.sigHeader;
    }
}
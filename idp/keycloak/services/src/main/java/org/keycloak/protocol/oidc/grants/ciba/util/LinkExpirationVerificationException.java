package org.keycloak.protocol.oidc.grants.ciba.util;

public class LinkExpirationVerificationException extends Exception{
    private static final long serialVersionUID = 2L;
    private final String sigHeader;

    public LinkExpirationVerificationException(String message, String sigHeader) {
        super(message);
        this.sigHeader = sigHeader;
    }

    public String getSigHeader() {
        return this.sigHeader;
    }
}

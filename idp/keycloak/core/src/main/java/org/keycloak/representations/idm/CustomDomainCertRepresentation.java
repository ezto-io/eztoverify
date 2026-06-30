package org.keycloak.representations.idm;

public class CustomDomainCertRepresentation {

    private boolean customCert;
    private String crt;
    private String key;

    public boolean isCustomCert() {
        return customCert;
    }

    public void setCustomCert(boolean customCert) {
        this.customCert = customCert;
    }

    public String getCrt() {
        return crt;
    }

    public void setCrt(String crt) {
        this.crt = crt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

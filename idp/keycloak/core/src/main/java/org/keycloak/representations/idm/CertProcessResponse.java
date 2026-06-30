package org.keycloak.representations.idm;

public class CertProcessResponse {
    private String certReady;
    private String tenant;
    private String domain;
    private String userEmail;

    public String certReady() {
        return certReady;
    }

    public void setCertReady(String certReady) {
        this.certReady = certReady;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}

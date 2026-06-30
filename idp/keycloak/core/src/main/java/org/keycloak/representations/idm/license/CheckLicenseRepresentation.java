package org.keycloak.representations.idm.license;

public class CheckLicenseRepresentation {

    private Integer status;
    private CheckLicenseBodyRepresentation body;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public CheckLicenseBodyRepresentation getBody() {
        return body;
    }

    public void setBody(CheckLicenseBodyRepresentation body) {
        this.body = body;
    }

}

package org.keycloak.marketplace;

import java.io.Serializable;

public class InstallResponse implements Serializable {

    private boolean isSuccess;
    private String message;
    private String redirectLink;

    public InstallResponse(boolean isSuccess, String message,String redirectLink) {
        this.isSuccess = isSuccess;
        this.message = message;
        this.redirectLink = redirectLink;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getMessage() {
        return message;
    }

    public String getRedirectLink() {
        return redirectLink;
    }
}

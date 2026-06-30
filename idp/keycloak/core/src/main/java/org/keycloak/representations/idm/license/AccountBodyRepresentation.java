
package org.keycloak.representations.idm.license;


public class AccountBodyRepresentation {

    private String tenantId;
    private Boolean markedToDelete;
    private Boolean hardDelete;
    private String actionToken;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Boolean getMarkedToDelete() {
        return markedToDelete;
    }

    public void setMarkedToDelete(Boolean markedToDelete) {
        this.markedToDelete = markedToDelete;
    }

    public Boolean getHardDelete() {
        return hardDelete;
    }

    public void setHardDelete(Boolean hardDelete) {
        this.hardDelete = hardDelete;
    }

    public String getActionToken() {
        return actionToken;
    }

    public void setActionToken(String actionToken) {
        this.actionToken = actionToken;
    }
}

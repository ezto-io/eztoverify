package io.ezto.license.job.models;

public class RemoveLicenseRequestModel {
    private String tenantId;
    private boolean markedToDelete;
    private boolean hardDelete;
    private String actionToken;

    public RemoveLicenseRequestModel() {
        // NOP
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isMarkedToDelete() {
        return markedToDelete;
    }

    public void setMarkedToDelete(boolean markedToDelete) {
        this.markedToDelete = markedToDelete;
    }

    public boolean isHardDelete() {
        return hardDelete;
    }

    public void setHardDelete(boolean hardDelete) {
        this.hardDelete = hardDelete;
    }

    public String getActionToken() {
        return actionToken;
    }

    public void setActionToken(String actionToken) {
        this.actionToken = actionToken;
    }
}

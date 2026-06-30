package org.keycloak.models;

import java.io.Serializable;

public class UserTransactionModel implements Serializable {

    private String trxId;
    private String userId;
    private String status;
    private Long createdAt;
    private Long expiresAt;

    public String getTrxId() {
        return trxId;
    }

    public void setTrxId(String trxId) {
        this.trxId = trxId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
    public Long getExpiresAt() { return expiresAt; }

}
package org.keycloak.representations.idm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
@JsonInclude(JsonInclude.Include.NON_NULL)

public class MgmtAccessRepresentation implements Cloneable {

    private String id;
    private String name;
    private long expiryDate;
    private UserRepresentation requestedBy;
    private long createdDate;
    private long lastUpdatedDate;
    private long lastUsedDate;
    private String secret;
    private boolean enabled;
    protected Map<String, Boolean> permissions;
    private boolean sandboxMode;
    private String sandboxSecret;
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserRepresentation getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UserRepresentation requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public long getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(long lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions = permissions;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean isSandboxMode() {
        return sandboxMode;
    }

    public void setSandboxMode(boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    public String getSandboxSecret() {
        return sandboxSecret;
    }

    public void setSandboxSecret(String sandboxSecret) {
        this.sandboxSecret = sandboxSecret;
    }
}

package org.keycloak.representations.idm;

public class CibaDeadLetterHookNotificationProviderRepresentation {
    private String id;
    private String displayName;
    private String description;
    private boolean isInBuilt;
    private boolean hasConfig;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isInBuilt() {
        return isInBuilt;
    }

    public void setInBuilt(boolean inBuilt) {
        isInBuilt = inBuilt;
    }

    public boolean isHasConfig() {
        return hasConfig;
    }

    public void setHasConfig(boolean hasConfig) {
        this.hasConfig = hasConfig;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

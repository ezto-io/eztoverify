package org.keycloak.models;

import java.io.Serializable;

public class TermsAndConditionsModel implements Serializable {
    private String requiredActionId;
    private String realmId;
    private String id;
    private String version;
    private String type;
    private String content;

    public String getRequiredActionId() {
        return requiredActionId;
    }

    public void setRequiredActionId(String requiredActionId) {
        this.requiredActionId = requiredActionId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

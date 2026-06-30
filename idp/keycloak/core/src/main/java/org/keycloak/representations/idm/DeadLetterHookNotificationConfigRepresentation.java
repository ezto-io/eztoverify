package org.keycloak.representations.idm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DeadLetterHookNotificationConfigRepresentation implements Serializable {
    private String id;
    private String providerId;
    private Map<String, String> config = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}

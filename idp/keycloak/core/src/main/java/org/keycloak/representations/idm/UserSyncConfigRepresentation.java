package org.keycloak.representations.idm;

import java.util.HashMap;
import java.util.Map;

public class UserSyncConfigRepresentation {
    private String id;
    private String name;
    private String providerId;
    private String enrollmentOption;
    private boolean tokenPresents;
    private Map<String, String> config = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getEnrollmentOption() {
        return enrollmentOption;
    }

    public void setEnrollmentOption(String enrollmentOption) {
        this.enrollmentOption = enrollmentOption;
    }

    public boolean isTokenPresents() {
        return tokenPresents;
    }

    public void setTokenPresents(boolean tokenPresents) {
        this.tokenPresents = tokenPresents;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}

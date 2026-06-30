package org.keycloak.representations;

import java.util.Map;

public class UserSyncConnectionRepresentation {

    private String name;

    private String providerId;

    private String enrollmentOption;

    private Map<String,String> syncConfig;

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

    public Map<String, String> getSyncConfig() {
        return syncConfig;
    }

    public void setSyncConfig(Map<String, String> syncConfig) {
        this.syncConfig = syncConfig;
    }
}

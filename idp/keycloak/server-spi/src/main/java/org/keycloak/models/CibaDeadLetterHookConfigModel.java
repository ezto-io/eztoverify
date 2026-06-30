package org.keycloak.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CibaDeadLetterHookConfigModel implements Serializable {

    private String id;
    private String cibaResultHookProviderId;
    private String clientId;
    private String realmId;
    private Map<String, String> config = new HashMap<>();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public String getCibaResultHookProviderId() {
        return cibaResultHookProviderId;
    }

    public void setCibaResultHookProviderId(String cibaResultHookProviderId) {
        this.cibaResultHookProviderId = cibaResultHookProviderId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }
}

package org.keycloak.representations.marketplace;

import java.io.Serializable;
import java.util.Map;

public class MarketplaceTemplateRepresentation implements Serializable {
    private Map<String,String> config;

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

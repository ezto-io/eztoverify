package org.keycloak.models;

import java.io.Serializable;

public class EztovrfyMarketplaceCategoryModel implements Serializable {
    private String id;
    private String realmId;
    private String category;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

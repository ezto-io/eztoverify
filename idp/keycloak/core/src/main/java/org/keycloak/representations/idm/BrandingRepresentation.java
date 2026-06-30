package org.keycloak.representations.idm;

import java.util.Map;

public class BrandingRepresentation {

    String id;
    String name;
    boolean isRealmBrand;
    Map<String, String> attributes;
    long createdTimeStamp;
    String realmId;

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

    public boolean isRealmBrand() {
        return isRealmBrand;
    }

    public void setRealmBrand(boolean realmBrand) {
        isRealmBrand = realmBrand;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public long getCreatedTimeStamp() {
        return createdTimeStamp;
    }

    public void setCreatedTimeStamp(long createdTimeStamp) {
        this.createdTimeStamp = createdTimeStamp;
    }
}

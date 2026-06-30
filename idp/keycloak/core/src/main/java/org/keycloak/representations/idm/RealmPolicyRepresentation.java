package org.keycloak.representations.idm;

public class RealmPolicyRepresentation {

    private String id;
    private String realmId;
    private String policyName;
    private String policyValue;
    private String policyType;
    private boolean isRealmPolicy;
    private Long createdTimeStamp;

    public Long getCreatedTimeStamp() {
        return createdTimeStamp;
    }

    public void setCreatedTimeStamp(Long createdTimeStamp) {
        this.createdTimeStamp = createdTimeStamp;
    }

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

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getPolicyValue() {
        return policyValue;
    }

    public void setPolicyValue(String policyValue) {
        this.policyValue = policyValue;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public boolean isRealmPolicy() {
        return isRealmPolicy;
    }

    public void setRealmPolicy(boolean realmPolicy) {
        isRealmPolicy = realmPolicy;
    }
}

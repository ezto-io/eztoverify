package org.keycloak.models.jpa.entities;


import javax.persistence.*;

@Table(name="REALM_POLICY")
@Entity
@NamedQueries({
        @NamedQuery(name="getPolicyByType", query="select realmpol from RealmPolicyEntity realmpol where realmpol.realmId = :realm and realmpol.policyType = :policyType and realmpol.inBuilt = FALSE"),
        @NamedQuery(name="getRealmPolicy", query="select realmpol from RealmPolicyEntity realmpol where realmpol.isRealmPolicy = TRUE and realmpol.realmId = :realm and realmpol.policyType = :policyType and realmpol.inBuilt = FALSE"),
        @NamedQuery(name="getPolicyIdsByRealm", query="select realmpol.id from RealmPolicyEntity realmpol where realmpol.realmId = :realm and realmpol.inBuilt = FALSE order by realmpol.id "),
        @NamedQuery(name="getNonRealmPolicyIdsByRealm", query="select realmpol.id from RealmPolicyEntity realmpol " +
                "where realmpol.realmId = :realm and realmpol.inBuilt = FALSE and realmpol.isRealmPolicy = FALSE " +
                "order by " +
                "realmpol.id "),
        @NamedQuery(name="getCibaPolicyIdsByRealm", query="select realmpol.id from RealmPolicyEntity realmpol " +
                "where realmpol.realmId = :realm and realmpol.inBuilt = FALSE and realmpol.isRealmPolicy = FALSE and realmpol.policyType = :policyType " +
                "and (:policyName is null or :policyName = '' or lower(realmpol.policyName) like lower(concat('%', :policyName, '%')))" +
                "order by " +
                "realmpol.id "),
        @NamedQuery(name="getAllPolicyIdsOfRealm",query = "select realmpol.id from RealmPolicyEntity realmpol where realmpol.realmId = :realm order by realmpol.id"),
        @NamedQuery(name="searchPoliciesByPolicyName", query="select realmpol from RealmPolicyEntity realmpol where lower(realmpol.policyName) like lower(concat('%',:policyName,'%')) and realmpol.realmId = :realm and realmpol.inBuilt = FALSE order by realmpol.policyName"),
})
public class RealmPolicyEntity {

    @Column(name = "REALM_ID")
    protected String realmId;

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name="POL_NAME", unique = true)
    protected String policyName;

    @Column(name="POL_VALUE")
    protected String policyValue;

    @Column(name="IS_REALM_POL")
    protected boolean isRealmPolicy;

    @Column(name="POL_TYPE")
    protected String policyType;

    @Column(name = "CREATED_TIMESTAMP")
    private Long createdTimestamp;

    @Column(name = "IN_BUILT")
    private boolean inBuilt;

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
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

    public boolean isRealmPolicy() {
        return isRealmPolicy;
    }

    public void setRealmPolicy(boolean realmPolicy) {
        isRealmPolicy = realmPolicy;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public boolean isInBuilt() {
        return inBuilt;
    }

    public void setInBuilt(boolean inBuilt) {
        this.inBuilt = inBuilt;
    }
}

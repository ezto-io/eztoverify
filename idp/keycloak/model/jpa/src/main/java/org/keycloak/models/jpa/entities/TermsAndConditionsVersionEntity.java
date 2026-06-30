package org.keycloak.models.jpa.entities;


import javax.persistence.*;

@NamedQueries({
        @NamedQuery(name = "findTermsAndConditionsByVersion", query = "select t from TermsAndConditionsVersionEntity t where t.requiredActionId = :termsAndConditionId and t.version = :version"),
        @NamedQuery(name = "findTermsAndConditionsVersion", query = "select t from TermsAndConditionsVersionEntity t where t.requiredActionId = :termsAndConditionId"),
        @NamedQuery(name = "removeTermsAndConditionsVersionByVersion", query = "delete from TermsAndConditionsVersionEntity t where t.version = :version and t.requiredActionId = :termsAndConditionId"),
        @NamedQuery(name = "removeTermsAndConditionsVersions", query = "delete from TermsAndConditionsVersionEntity t where t.requiredActionId = :termsAndConditionId"),
        @NamedQuery(name = "updateTermsAndConditionsVersions", query = "update TermsAndConditionsVersionEntity t set t.id = :id, t.type = :type, t.content = :content  where t.requiredActionId = :termsAndConditionId and t.version = :version"),
        @NamedQuery(name = "findTermsAndConditions", query = "select t from TermsAndConditionsVersionEntity t where realmId = :realmId")
})

@Table(name="TERMS_AND_CONDITIONS_VERSION")
@Entity
public class TermsAndConditionsVersionEntity {

    @Id
    @Column(name = "ID")
    protected String id;

    @Column(name="REQUIRED_ACTION_ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String requiredActionId;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "VERSION")
    protected String version;

    @Column(name = "TYPE")
    protected String type;

    @Column(name = "CONTENT", length = 3000)
    protected String content;

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

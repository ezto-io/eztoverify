package org.keycloak.models.jpa.entities;

import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.*;

@Entity
@Table(name = "CUSTOM_DOMAIN_MAPPINGS")
public class CustomDomainMappingsEntity {
    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "REALM")
    private RealmEntity realm;

    @Column(name = "CUSTOM_DOMAIN")
    private String customDomain;
    @Column(name = "TXT_RECORD_NAME")
    private String txtRecordName;
    @Column(name = "TXT_RECORD_VALUE")
    private String txtRecordValue;
    @Column(name = "A_RECORD_NAME")
    private String aRecordName;
    @Column(name = "A_RECORD_VALUE")
    private String aRecordValue;
    @Column(name = "TXT_RECORD_VERIFIED")
    private Boolean txtRecordVerified;
    @Column(name = "A_RECORD_VERIFIED")
    private Boolean aRecordVerified;
    @Column(name = "IS_SETUP_COMPLETED")
    private Boolean isSetupCompleted;
    @Column(name = "IS_ACTIVE")
    private Boolean isActive;

    @Column(name = "IS_PRIMARY")
    private Boolean isPrimary;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(String customDomain) {
        this.customDomain = customDomain;
    }

    public String getTxtRecordName() {
        return txtRecordName;
    }

    public void setTxtRecordName(String txtRecordName) {
        this.txtRecordName = txtRecordName;
    }

    public String getTxtRecordValue() {
        return txtRecordValue;
    }

    public void setTxtRecordValue(String txtRecordValue) {
        this.txtRecordValue = txtRecordValue;
    }

    public String getaRecordName() {
        return aRecordName;
    }

    public void setARecordName(String aRecordName) {
        this.aRecordName = aRecordName;
    }

    public String getARecordValue() {
        return aRecordValue;
    }

    public void setARecordValue(String aRecordValue) {
        this.aRecordValue = aRecordValue;
    }

    public Boolean isTxtRecordVerified() {
        return txtRecordVerified;
    }

    public void setTxtRecordVerified(Boolean txtRecordVerified) {
        this.txtRecordVerified = txtRecordVerified;
    }

    public Boolean isARecordVerified() {
        return aRecordVerified;
    }

    public void setARecordVerified(Boolean aRecordVerified) {
        this.aRecordVerified = aRecordVerified;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public Boolean isSetupCompleted() {
        return isSetupCompleted;
    }

    public void setSetupCompleted(Boolean setupCompleted) {
        isSetupCompleted = setupCompleted;
    }

    public Boolean isActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public void setIsPrimary(Boolean primary) {
        isPrimary = primary;
    }

    public Boolean isPrimary() {
        return isPrimary;
    }

    @PrePersist
    public void onCreate() {
        if (this.id == null) {
            this.id = KeycloakModelUtils.generateId();
        }
    }
}

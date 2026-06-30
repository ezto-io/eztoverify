package org.keycloak.models;


import java.util.Objects;

public class CustomDomainMappingModel {
    private String id;
    private String customDomain;
    private String txtRecordName;
    private String txtRecordValue;
    private String aRecordName;
    private String aRecordValue;
    private Boolean txtRecordVerified;
    private Boolean aRecordVerified;
    private Boolean isSetupCompleted;
    private Boolean isActive;
    private Boolean isPrimary;

    // Constructors
    public CustomDomainMappingModel() {
    }

    // Getters and Setters
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

    public String getARecordName() {
        return aRecordName;
    }

    public void setARecord(String aRecord) {
        this.aRecordName = aRecord;
    }

    public Boolean getTxtRecordVerified() {
        return txtRecordVerified;
    }

    public void setTxtRecordVerified(Boolean txtRecordVerified) {
        this.txtRecordVerified = txtRecordVerified;
    }

    public Boolean getARecordVerified() {
        return aRecordVerified;
    }

    public void setARecordVerified(Boolean aRecordVerified) {
        this.aRecordVerified = aRecordVerified;
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

    public String getARecordValue() {
        return aRecordValue;
    }

    public void setARecordValue(String aRecordValue) {
        this.aRecordValue = aRecordValue;
    }

    public String getTxtRecordValue() {
        return txtRecordValue;
    }

    public void setTxtRecordValue(String txtRecordValue) {
        this.txtRecordValue = txtRecordValue;
    }
    public void setIsPrimary(Boolean primary) {
        isPrimary = primary;
    }
    public Boolean isPrimary() {
        return isPrimary;
    }

    // Override equals and hashCode for comparison and collections
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomDomainMappingModel)) return false;
        CustomDomainMappingModel that = (CustomDomainMappingModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

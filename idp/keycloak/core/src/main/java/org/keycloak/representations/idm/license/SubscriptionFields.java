package org.keycloak.representations.idm.license;

import java.util.Map;

public class SubscriptionFields {
    private String tenantId;
    private String adminEmailId;
    private Map<String, Object> subscriptions;
    private String mode;
    private String pricingType;
    private String gstNumber;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAdminEmailId() {
        return adminEmailId;
    }

    public void setAdminEmailId(String adminEmailId) {
        this.adminEmailId = adminEmailId;
    }

    public Map<String, Object> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Map<String, Object> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPricingType() {
        return pricingType;
    }

    public void setPricingType(String pricingType) {
        this.pricingType = pricingType;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }
}

package org.keycloak.representations.idm.license;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriceRequest {
    private String name;
    private String tenantId;
    private String consider;
    private String priceId;
    private String pricingType;
    private String billingScheme;
    private String productId;
    private List<CurrencyOptionModel> currencyOptions;
    private List<Map<String, String>> metadata;
    private String provider;
    private long updatedAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getConsider() {
        return consider;
    }

    public void setConsider(String consider) {
        this.consider = consider;
    }

    public String getPriceId() {
        return priceId;
    }

    public void setPriceId(String priceId) {
        this.priceId = priceId;
    }

    public String getPricingType() {
        return pricingType;
    }

    public void setPricingType(String pricingType) {
        this.pricingType = pricingType;
    }

    public String getBillingScheme() {
        return billingScheme;
    }

    public void setBillingScheme(String billingScheme) {
        this.billingScheme = billingScheme;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public List<CurrencyOptionModel> getCurrencyOptions() {
        return currencyOptions;
    }

    public void setCurrencyOptions(List<CurrencyOptionModel> currencyOptions) {
        this.currencyOptions = currencyOptions;
    }

    public List<Map<String, String>> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<Map<String, String>> metadata) {
        this.metadata = metadata;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}

package org.keycloak.representations.idm.license;

import java.util.List;
import java.util.Map;

public class CustomPricingModel {
    private String name;
    private String priceId;
    private String consider;
    private String usageType;
    private String billingScheme;
    private List<CurrencyOptionModel> currencyOptions;
    private List<Map<String,String>> metadata;
    private Long updatedAt;

    public String getPriceId() {
        return priceId;
    }

    public void setPriceId(String priceId) {
        this.priceId = priceId;
    }

    public String getConsider() {
        return consider;
    }

    public void setConsider(String consider) {
        this.consider = consider;
    }

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public String getBillingScheme() {
        return billingScheme;
    }

    public void setBillingScheme(String billingScheme) {
        this.billingScheme = billingScheme;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

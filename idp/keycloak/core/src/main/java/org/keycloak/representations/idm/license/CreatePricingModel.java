package org.keycloak.representations.idm.license;

import java.util.List;
import java.util.Map;

public class CreatePricingModel {
    private String productId;
    private String tenantId;
    private String pricingType;
    private List<CurrencyOptionModel> currencyOptions;
    private List<Map<String,String>> metadata;
    private String name;
    private String consider;
    private String priceId;
    private String provider;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPricingType() {
        return pricingType;
    }

    public void setPricingType(String pricingType) {
        this.pricingType = pricingType;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}

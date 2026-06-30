package org.keycloak.representations.idm.license;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriceRepresentation {
    private String priceId;
    private String consider;
    private String usageType;
    private String billingScheme;
    private List<CurrencyOptionRepresentation> currencyOptions;
//    private MetaDataRepresentation metadata;
    private Map<String,String> metadata;

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

    public List<CurrencyOptionRepresentation> getCurrencyOptions() {
        return currencyOptions;
    }

    public void setCurrencyOptions(List<CurrencyOptionRepresentation> currencyOptions) {
        this.currencyOptions = currencyOptions;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

//    public MetaDataRepresentation getMetadata() {
//        return metadata;
//    }
//
//    public void setMetadata(MetaDataRepresentation metadata) {
//        this.metadata = metadata;
//    }
}

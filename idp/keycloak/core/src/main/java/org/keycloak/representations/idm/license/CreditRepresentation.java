package org.keycloak.representations.idm.license;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditRepresentation {
    private String name;
    private String externalId;
    private String prodId;
    private String image;
    private String consider;
    private String alternative;
    private String description;
    private double chargedAmount;
    private String pricingType;
    private String desc_en;
    private String desc_ar;

    private PriceRepresentation pricing;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProdId() {
        return prodId;
    }

    public void setProdId(String prodId) {
        this.prodId = prodId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public double getChargedAmount() {
        return chargedAmount;
    }

    public void setChargedAmount(double chargedAmount) {
        this.chargedAmount = chargedAmount;
    }

    public String getPricingType() {
        return pricingType;
    }

    public void setPricingType(String pricingType) {
        this.pricingType = pricingType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getConsider() {
        return consider;
    }

    public void setConsider(String consider) {
        this.consider = consider;
    }

    public String getAlternative() {
        return alternative;
    }

    public void setAlternative(String alternative) {
        this.alternative = alternative;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PriceRepresentation getPricing() {
        return pricing;
    }

    public void setPricing(PriceRepresentation pricing) {
        this.pricing = pricing;
    }

    public void setDesc_en(String desc_en) { this.desc_en = desc_en; }

    public String getDesc_en() {
        return desc_en;
    }

    public void setDesc_ar(String desc_ar) { this.desc_ar = desc_ar; }

    public String getDesc_ar() {return desc_ar; }
}

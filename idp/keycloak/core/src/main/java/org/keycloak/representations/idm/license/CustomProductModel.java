package org.keycloak.representations.idm.license;

import java.util.HashMap;
import java.util.List;

public class CustomProductModel {
    private String name;
    private String externalId;
    private String image;
    private String consider;
    private String alternative;
    private String productId;
    private List<CustomPricingModel> pricing;
    private HashMap<String, String> description;
    private String productType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public List<CustomPricingModel> getPricing() {
        return pricing;
    }

    public void setPricing(List<CustomPricingModel> pricing) {
        this.pricing = pricing;
    }

    public HashMap<String, String> getDescription() {
        return description;
    }

    public void setDescription(HashMap<String, String> description) {
        this.description = description;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }
}

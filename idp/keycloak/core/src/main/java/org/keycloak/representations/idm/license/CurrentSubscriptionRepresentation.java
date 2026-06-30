package org.keycloak.representations.idm.license;

public class CurrentSubscriptionRepresentation {
    private String title;
    private String prodId;
    private String logo;
    private String min;
    private String max;
    private String consider;
    private String alternative;
    private String amount;
    private String isSubscribed;
    private String description;
    private String isAllowedToUpdate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProdId() {
        return prodId;
    }

    public void setProdId(String prodId) {
        this.prodId = prodId;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getIsSubscribed() {
        return isSubscribed;
    }

    public void setIsSubscribed(String isSubscribed) {
        this.isSubscribed = isSubscribed;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIsAllowedToUpdate() {
        return isAllowedToUpdate;
    }

    public void setIsAllowedToUpdate(String isAllowedToUpdate) {
        this.isAllowedToUpdate = isAllowedToUpdate;
    }
}

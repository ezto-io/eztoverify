package org.keycloak.representations.idm.license;

public class RecurringRepresentation {
    private String title;
    private String prodId;
    private String logo;
    private double chargedAmount;
    private String currency;
    private Integer consumedCount;
    private Integer freeTierCount;
    private Integer maxProdCount;
    private Integer chargeableCount;


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

    public double getChargedAmount() {
        return chargedAmount;
    }

    public void setChargedAmount(double chargedAmount) {
        this.chargedAmount = chargedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getConsumedCount() {
        return consumedCount;
    }

    public void setConsumedCount(Integer consumedCount) {
        this.consumedCount = consumedCount;
    }

    public Integer getFreeTierCount() {
        return freeTierCount;
    }

    public void setFreeTierCount(Integer freeTierCount) {
        this.freeTierCount = freeTierCount;
    }

    public Integer getMaxProdCount() {
        return maxProdCount;
    }

    public void setMaxProdCount(Integer maxProdCount) {
        this.maxProdCount = maxProdCount;
    }

    public Integer getChargeableCount() {
        return chargeableCount;
    }

    public void setChargeableCount(Integer chargeableCount) {
        this.chargeableCount = chargeableCount;
    }
}

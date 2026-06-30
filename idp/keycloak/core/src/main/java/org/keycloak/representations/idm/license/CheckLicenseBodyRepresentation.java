package org.keycloak.representations.idm.license;

import java.util.List;
import java.util.Map;

public class  CheckLicenseBodyRepresentation {

    private String message;
    private Integer days;
    private Boolean isActivated;
    private List<RecurringRepresentation> recurrings;
    private List<CreditRepresentation> credits;
    private Map<String,String> invoice;
    private String walletId;
    private Double walletBalance;
    private String walletCurrency;
    private Double walletThreshold;
    private String authenticationUsage;
    private String stage;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Boolean getActivated() {
        return isActivated;
    }

    public void setActivated(Boolean activated) {
        isActivated = activated;
    }

    public List<RecurringRepresentation> getRecurrings() {
        return recurrings;
    }

    public void setRecurrings(List<RecurringRepresentation> recurrings) {
        this.recurrings = recurrings;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getStage() {
        return stage;
    }

    public List<CreditRepresentation> getCredits() {
        return credits;
    }

    public void setCredits(List<CreditRepresentation> credits) {
        this.credits = credits;
    }

    public Map<String, String> getInvoice() {
        return invoice;
    }

    public void setInvoice(Map<String, String> invoice) {
        this.invoice = invoice;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public Double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(Double walletBalance) {
        this.walletBalance = walletBalance;
    }

    public String getWalletCurrency() {
        return walletCurrency;
    }

    public void setWalletCurrency(String walletCurrency) {
        this.walletCurrency = walletCurrency;
    }

    public Double getWalletThreshold() {
        return walletThreshold;
    }

    public void setWalletThreshold(Double walletThreshold) {
        this.walletThreshold = walletThreshold;
    }

    public String getAuthenticationUsage() {
        return authenticationUsage;
    }

    public void setAuthenticationUsage(String authenticationUsage) {
        this.authenticationUsage = authenticationUsage;
    }
}

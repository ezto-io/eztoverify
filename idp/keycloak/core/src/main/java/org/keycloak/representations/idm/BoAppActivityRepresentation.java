package org.keycloak.representations.idm;

public class BoAppActivityRepresentation {

    private int uniqueUsers;
    private int transactions;
    private int rejected;
    private int completed;
    private String appName;
    private String appId;
    private boolean isLive;
    private int expired;
    private int pending;
    private boolean isAPIApp;
    private String sandboxAppId;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public int getUniqueUsers() {
        return uniqueUsers;
    }

    public void setUniqueUsers(int uniqueUsers) {
        this.uniqueUsers = uniqueUsers;
    }

    public int getTransactions() {
        return transactions;
    }

    public void setTransactions(int transactions) {
        this.transactions = transactions;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getRejected() {
        return rejected;
    }

    public void setRejected(int rejected) {
        this.rejected = rejected;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public int getExpired() {
        return expired;
    }

    public void setExpired(int expired) {
        this.expired = expired;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }

    public void setIsAPIApp(boolean isAPIApp){ this.isAPIApp = isAPIApp;}

    public boolean getIsAPIApp() { return this.isAPIApp;}

    public String getSandboxAppId() { return  this.sandboxAppId;}

    public void setSandboxAppId(String sandboxAppId) { this.sandboxAppId = sandboxAppId; }
}

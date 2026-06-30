package org.keycloak.representations.idm;

import java.util.HashMap;
import java.util.List;

public class BoActivityRepresentation {

    private int uniqueUsers;
    private int transactions;
    private int rejected;
    private int completed;
    private int pending;
    private long apps;
    private List<BoAppActivityRepresentation> appsStats;

    private List<HashMap<String,String>> history;

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

    public long getApps() {
        return apps;
    }

    public void setApps(long apps) {
        this.apps = apps;
    }

    public List<BoAppActivityRepresentation> getAppsStats() {
        return appsStats;
    }

    public void setAppsStats(List<BoAppActivityRepresentation> appsStats) {
        this.appsStats = appsStats;
    }

    public List<HashMap<String, String>> getHistory() {
        return history;
    }

    public void setHistory(List<HashMap<String, String>> history) {
        this.history = history;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }
}

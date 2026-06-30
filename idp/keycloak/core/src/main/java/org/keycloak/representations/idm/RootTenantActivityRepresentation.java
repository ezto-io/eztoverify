package org.keycloak.representations.idm;

import java.util.HashMap;
import java.util.List;

public class RootTenantActivityRepresentation {

    private int tenantsCount;
    private int usersCount;
    private int appsCount;
    private int loginSuccessCount;
    private int signupsCount;

    private List<String> topPerformingTenants;

    private List<HashMap<String,String>> history;

    public int getTenantsCount() {
        return tenantsCount;
    }

    public void setTenantsCount(int tenantsCount) {
        this.tenantsCount = tenantsCount;
    }

    public int getUsersCount() {
        return usersCount;
    }

    public void setUsersCount(int usersCount) {
        this.usersCount = usersCount;
    }

    public int getAppsCount() {
        return appsCount;
    }

    public void setAppsCount(int appsCount) {
        this.appsCount = appsCount;
    }

    public int getLoginSuccessCount() {
        return loginSuccessCount;
    }

    public void setLoginSuccessCount(int loginSuccessCount) {
        this.loginSuccessCount = loginSuccessCount;
    }

    public int getSignupsCount() {
        return signupsCount;
    }

    public void setSignupsCount(int signupsCount) {
        this.signupsCount = signupsCount;
    }

    public List<String> getTopPerformingTenants() {
        return topPerformingTenants;
    }

    public void setTopPerformingTenants(List<String> topPerformingTenants) {
        this.topPerformingTenants = topPerformingTenants;
    }

    public List<HashMap<String, String>> getHistory() {
        return history;
    }

    public void setHistory(List<HashMap<String, String>> history) {
        this.history = history;
    }
}

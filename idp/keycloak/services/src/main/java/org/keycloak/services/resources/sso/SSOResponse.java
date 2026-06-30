package org.keycloak.services.resources.sso;

import org.keycloak.marketplace.template.SSOBean;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Map;

public class SSOResponse {

    List<SSOBean> ssoApps;
    UserRepresentation user;
    RealmRepresentation realm;
    Map<String, String> branding;
    boolean isDesktopAppInstalled;
    Map<String,String> appDetails;
    public List<SSOBean> getSsoApps() {
        return ssoApps;
    }

    public void setSsoApps(List<SSOBean> ssoApps) {
        this.ssoApps = ssoApps;
    }

    public UserRepresentation getUser() {
        return user;
    }

    public void setUser(UserRepresentation user) {
        this.user = user;
    }

    public RealmRepresentation getRealm() {
        return realm;
    }

    public void setRealm(RealmRepresentation realm) {
        this.realm = realm;
    }

    public Map<String, String> getBranding() {
        return branding;
    }

    public void setBranding(Map<String, String> branding) {
        this.branding = branding;
    }

    public boolean isDesktopAppInstalled() {
        return isDesktopAppInstalled;
    }

    public void setDesktopAppInstalled(boolean desktopAppInstalled) {
        isDesktopAppInstalled = desktopAppInstalled;
    }

    public Map<String, String> getAppDetails() {
        return appDetails;
    }

    public void setAppDetails(Map<String, String> appDetails) {
        this.appDetails = appDetails;
    }
}

package org.keycloak.representations.idm;

import java.util.Map;

public class BrandRepresentation {

    boolean isBrandingEnabled;
    Map<String, String> attributes;
    boolean isModeEnabled;

    public boolean isModeEnabled() {
        return isModeEnabled;
    }

    public void setModeEnabled(boolean modeEnabled) {
        isModeEnabled = modeEnabled;
    }

    public boolean isBrandingEnabled() {
        return isBrandingEnabled;
    }

    public void setBrandingEnabled(boolean brandingEnabled) {
        isBrandingEnabled = brandingEnabled;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}

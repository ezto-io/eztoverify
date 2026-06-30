
package org.keycloak.representations.idm.license;

public class License {
    private String field;

    private LicenseFields arguments;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public LicenseFields getArguments() {
        return arguments;
    }

    public void setArguments(LicenseFields arguments) {
        this.arguments = arguments;
    }
}

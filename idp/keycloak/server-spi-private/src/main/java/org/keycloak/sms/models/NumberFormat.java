package org.keycloak.sms.models;

public class NumberFormat {

    private String type;
    private String nationalFormat;
    private String countryCode;
    private String internationalFormat;
    private String e16Format;
    private String region;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNationalFormat() {
        return nationalFormat;
    }

    public void setNationalFormat(String nationalFormat) {
        this.nationalFormat = nationalFormat;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getInternationalFormat() {
        return internationalFormat;
    }

    public void setInternationalFormat(String internationalFormat) {
        this.internationalFormat = internationalFormat;
    }

    public String getE16Format() {
        return e16Format;
    }

    public void setE16Format(String e16Format) {
        this.e16Format = e16Format;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}

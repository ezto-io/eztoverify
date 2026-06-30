package com.qlikverify.services.responses;

public class PoiDocObject {
    private String type;
    private String expiry;
    private String issuer;
    private String authority;
    private boolean hasAddressSticker;

    public PoiDocObject() {
        // NOP
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public boolean isHasAddressSticker() {
        return hasAddressSticker;
    }

    public void setHasAddressSticker(boolean hasAddressSticker) {
        this.hasAddressSticker = hasAddressSticker;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}

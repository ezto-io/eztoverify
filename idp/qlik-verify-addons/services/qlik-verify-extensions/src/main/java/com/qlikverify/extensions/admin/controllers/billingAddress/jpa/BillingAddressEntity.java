package com.qlikverify.extensions.admin.controllers.billingAddress.jpa;

import javax.persistence.*;

@Entity
@Table(name = "BILLING_ADDRESS")
public class BillingAddressEntity {
    @Id
    @Column(name="REALM_ID")
    @Access(AccessType.PROPERTY)
    protected String realmId;
    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String line1;

    @Column(nullable = true)
    private String line2;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = true)
    private String state;

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}


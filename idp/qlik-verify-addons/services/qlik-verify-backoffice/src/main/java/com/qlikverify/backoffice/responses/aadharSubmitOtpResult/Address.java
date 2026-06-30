package com.qlikverify.backoffice.responses.aadharSubmitOtpResult;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "loc",
        "country",
        "house",
        "subdist",
        "vtc",
        "po",
        "state",
        "street",
        "dist"
})
public class Address {
    @JsonProperty("loc")
    private String loc;
    @JsonProperty("country")
    private String country;
    @JsonProperty("house")
    private String house;
    @JsonProperty("subdist")
    private String subdist;
    @JsonProperty("vtc")
    private String vtc;
    @JsonProperty("po")
    private String po;
    @JsonProperty("state")
    private String state;
    @JsonProperty("street")
    private String street;
    @JsonProperty("dist")
    private String dist;

    @JsonProperty("loc")
    public String getLoc() {
        return loc;
    }

    @JsonProperty("loc")
    public void setLoc(String loc) {
        this.loc = loc;
    }

    @JsonProperty("country")
    public String getCountry() {
        return country;
    }

    @JsonProperty("country")
    public void setCountry(String country) {
        this.country = country;
    }

    @JsonProperty("house")
    public String getHouse() {
        return house;
    }

    @JsonProperty("house")
    public void setHouse(String house) {
        this.house = house;
    }

    @JsonProperty("subdist")
    public String getSubdist() {
        return subdist;
    }

    @JsonProperty("subdist")
    public void setSubdist(String subdist) {
        this.subdist = subdist;
    }

    @JsonProperty("vtc")
    public String getVtc() {
        return vtc;
    }

    @JsonProperty("vtc")
    public void setVtc(String vtc) {
        this.vtc = vtc;
    }

    @JsonProperty("po")
    public String getPo() {
        return po;
    }

    @JsonProperty("po")
    public void setPo(String po) {
        this.po = po;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

     @JsonProperty("street")
    public String getStreet() {
        return street;
    }

     @JsonProperty("street")
    public void setStreet(String street) {
        this.street = street;
    }

    @JsonProperty("dist")
    public String getDist() {
        return dist;
    }

    @JsonProperty("dist")
    public void setDist(String dist) {
        this.dist = dist;
    }
}

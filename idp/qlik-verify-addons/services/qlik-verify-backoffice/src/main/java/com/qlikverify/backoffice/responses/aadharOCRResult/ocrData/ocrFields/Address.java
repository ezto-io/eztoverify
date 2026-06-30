package com.qlikverify.backoffice.responses.aadharOCRResult.ocrData.ocrFields;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "value",
        "confidence",
        "first_line",
        "second_line",
        "locality",
        "landmark",
        "house_number",
        "district",
        "city",
        "state",
        "country",
        "zip"
})
public class Address {
    @JsonCreator
    public Address(@JsonProperty("value") String value,
                   @JsonProperty("confidence") int confidence,
                   @JsonProperty("first_line") String firstLine,
                   @JsonProperty("second_line") String secondLine,
                   @JsonProperty("locality") String locality,
                   @JsonProperty("landmark") String landmark,
                   @JsonProperty("house_number") String houseNumber,
                   @JsonProperty("district") String district,
                   @JsonProperty("city") String city,
                   @JsonProperty("state") String state,
                   @JsonProperty("country") String country,
                   @JsonProperty("zip") String zip) {
        this.value = value;
        this.confidence = confidence;
        this.firstLine = firstLine;
        this.secondLine = secondLine;
        this.locality = locality;
        this.landmark = landmark;
        this.houseNumber = houseNumber;
        this.district = district;
        this.city = city;
        this.state = state;
        this.country = country;
        this.zip = zip;
    }

    @JsonProperty("value")
    private String value;
    @JsonProperty("confidence")
    private int confidence;
    @JsonProperty("first_line")
    private String firstLine;
    @JsonProperty("second_line")
    private String secondLine;
    @JsonProperty("locality")
    private String locality;
    @JsonProperty("landmark")
    private String landmark;
    @JsonProperty("house_number")
    private String houseNumber;
    @JsonProperty("district")
    private String district;
    @JsonProperty("city")
    private String city;
    @JsonProperty("state")
    private String state;
    @JsonProperty("country")
    private String country;
    @JsonProperty("zip")
    private String zip;
    @JsonProperty("value")
    public String getValue() {
        return value;
    }
    @JsonProperty("value")
    public void setValue(String value) {
        this.value = value;
    }
    @JsonProperty("confidence")
    public int getConfidence() {
        return confidence;
    }
    @JsonProperty("confidence")
    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    @JsonProperty("first_line")
    public String getFirstLine() {
        return firstLine;
    }

    @JsonProperty("first_line")
    public void setFirstLine(String firstLine) {
        this.firstLine = firstLine;
    }

    @JsonProperty("second_line")
    public String getSecondLine() {
        return secondLine;
    }

    @JsonProperty("second_line")
    public void setSecondLine(String secondLine) {
        this.secondLine = secondLine;
    }

    @JsonProperty("locality")
    public String getLocality() {
        return locality;
    }

    @JsonProperty("locality")
    public void setLocality(String locality) {
        this.locality = locality;
    }

    @JsonProperty("landmark")
    public String getLandmark() {
        return landmark;
    }

    @JsonProperty("landmark")
    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    @JsonProperty("house_number")
    public String getHouseNumber() {
        return houseNumber;
    }

    @JsonProperty("house_number")
    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    @JsonProperty("district")
    public String getDistrict() {
        return district;
    }

    @JsonProperty("district")

    public void setDistrict(String district) {
        this.district = district;
    }

    @JsonProperty("city")

    public String getCity() {
        return city;
    }

    @JsonProperty("city")
    public void setCity(String city) {
        this.city = city;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }
    @JsonProperty("country")
    public String getCountry() {
        return country;
    }

    @JsonProperty("country")
    public void setCountry(String country) {
        this.country = country;
    }

    @JsonProperty("zip")
    public String getZip() {
        return zip;
    }

    @JsonProperty("zip")
    public void setZip(String zip) {
        this.zip = zip;
    }
}
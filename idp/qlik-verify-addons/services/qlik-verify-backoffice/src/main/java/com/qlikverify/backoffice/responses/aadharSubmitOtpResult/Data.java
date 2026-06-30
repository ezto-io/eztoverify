package com.qlikverify.backoffice.responses.aadharSubmitOtpResult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "gender",
        "address",
        "aadhaar_number",
        "dob",
        "client_id",
        "zip",
        "full_name",
        "zip_data",
        "care_of",
        "profile_image",
        "raw_xml",
        "share_code"
})
public class Data {
    @JsonProperty("gender")
    private String gender;
    @JsonProperty("address")
    private Address address;
    @JsonProperty("aadhaar_number")
    private String aadhaarNumber;
    @JsonProperty("dob")
    private String dob;
    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("zip")
    private String zip;
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("zip_data")
    private String zipData;
    @JsonProperty("care_of")
    private String careOf;
    @JsonProperty("profile_image")
    private String profileImage;
    @JsonProperty("raw_xml")
    private String rawXml;
    @JsonProperty("share_code")
    private String shareCode;
    
    @JsonProperty("gender")
    public String getGender() {
        return gender;
    }

    @JsonProperty("gender")
    public void setGender(String gender) {
        this.gender = gender;
    }

    @JsonProperty("address")
    public Address getAddress() {
        return address;
    }

    @JsonProperty("address")
    public void setAddress(Address address) {
        this.address = address;
    }

    @JsonProperty("aadhaar_number")
    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    @JsonProperty("aadhaar_number")
    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    @JsonProperty("dob")
    public String getDob() {
        return dob;
    }

    @JsonProperty("dob")
    public void setDob(String dob) {
        this.dob = dob;
    }

    @JsonProperty("client_id")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("client_id")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("zip")
    public String getZip() {
        return zip;
    }

    @JsonProperty("zip")
    public void setZip(String zip) {
        this.zip = zip;
    }

    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    @JsonProperty("full_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @JsonProperty("zip_data")
    public String getZipData() {
        return zipData;
    }

    @JsonProperty("zip_data")
    public void setZipData(String zipData) {
        this.zipData = zipData;
    }

    @JsonProperty("care_of")
    public String getCareOf() {
        return careOf;
    }

    @JsonProperty("care_of")
    public void setCareOf(String careOf) {
        this.careOf = careOf;
    }

    @JsonProperty("profile_image")
    public String getProfileImage() {
        return profileImage;
    }

    @JsonProperty("profile_image")
    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    @JsonProperty("raw_xml")
    public String getRawXml() {
        return rawXml;
    }

    @JsonProperty("raw_xml")
    public void setRawXml(String rawXml) {
        this.rawXml = rawXml;
    }

    @JsonProperty("share_code")
    public String getShareCode() {
        return shareCode;
    }

    @JsonProperty("share_code")
    public void setShareCode(String shareCode) {
        this.shareCode = shareCode;
    }
}

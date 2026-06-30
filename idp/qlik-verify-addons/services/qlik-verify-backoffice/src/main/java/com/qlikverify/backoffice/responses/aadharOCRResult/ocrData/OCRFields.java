package com.qlikverify.backoffice.responses.aadharOCRResult.ocrData;

import com.fasterxml.jackson.annotation.*;
import com.qlikverify.backoffice.responses.aadharOCRResult.ocrData.ocrFields.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
       "document_type",
        "full_name",
        "gender",
        "mother_name",
        "father_name",
        "dob",
        "aadhaar_number",
        "image_url",
        "uniqueness_id",
        "address",
        "zip",
        "care_of"
})
public class OCRFields {
    @JsonCreator
    public OCRFields(@JsonProperty("document_type") String documentType,
                     @JsonProperty("full_name") FullName fullName,
                     @JsonProperty("gender") Gender gender,
                     @JsonProperty("mother_name") MotherName motherName,
                     @JsonProperty("father_name") FatherName fatherName,
                     @JsonProperty("dob") DOB dob,
                     @JsonProperty("address") Address address,
                     @JsonProperty("zip") Zip zip,
                     @JsonProperty("care_of") CareOf careOf,
                     @JsonProperty("aadhaar_number") AadharNumber aadharNumber,
                     @JsonProperty("image_url") String imageUrl,
                     @JsonProperty("uniqueness_id") String uniquenessId) {
        this.documentType = documentType;
        this.fullName = fullName;
        this.gender = gender;
        this.motherName = motherName;
        this.dob = dob;
        this.address = address;
        this.zip = zip;
        this.careOf = careOf;
        this.aadharNumber = aadharNumber;
        this.imageUrl = imageUrl;
        this.uniquenessId = uniquenessId;
    }

    @JsonProperty("document_type")
    private String documentType;

    @JsonProperty("full_name")
    private FullName fullName;

    @JsonProperty("gender")
    private Gender gender;

    @JsonProperty("mother_name")
    private MotherName motherName;

    @JsonProperty("mother_name")
    private FatherName fatherName;

    @JsonProperty("dob")
    private DOB dob;

    @JsonProperty("address")
    private Address address;

    @JsonProperty("zip")
    private Zip zip;
    @JsonProperty("care_of")
    private CareOf careOf;

    @JsonProperty("aadhaar_number")
    private AadharNumber aadharNumber;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("uniqueness_id")
    private String uniquenessId;

    @JsonProperty("document_type")
    public String getDocumentType() {
        return documentType;
    }
    @JsonProperty("document_type")
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    @JsonProperty("full_name")

    public FullName getFullName() {
        return fullName;
    }
    @JsonProperty("full_name")

    public void setFullName(FullName fullName) {
        this.fullName = fullName;
    }


    @JsonProperty("gender")
    public Gender getGender() {
        return gender;
    }

    @JsonProperty("gender")

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    @JsonProperty("mother_name")
    public MotherName getMotherName() {
        return motherName;
    }

    @JsonProperty("mother_name")
    public void setMotherName(MotherName motherName) {
        this.motherName = motherName;
    }

    @JsonProperty("father_name")
    public FatherName getFatherName() {
        return fatherName;
    }

    @JsonProperty("father_name")
    public void setFatherName(FatherName fatherName) {
        this.fatherName = fatherName;
    }

    @JsonProperty("dob")
    public DOB getDob() {
        return dob;
    }
    @JsonProperty("dob")

    public void setDob(DOB dob) {
        this.dob = dob;
    }
    @JsonProperty("address")

    public Address getAddress() {
        return address;
    }
    @JsonProperty("address")

    public void setAddress(Address address) {
        this.address = address;
    }
    @JsonProperty("zip")

    public Zip getZip() {
        return zip;
    }

    @JsonProperty("zip")
    public void setZip(Zip zip) {
        this.zip = zip;
    }
    @JsonProperty("care_of")

    public CareOf getCareOf() {
        return careOf;
    }
    @JsonProperty("care_of")

    public void setCareOf(CareOf careOf) {
        this.careOf = careOf;
    }

    @JsonProperty("aadhaar_number")

    public AadharNumber getAadharNumber() {
        return aadharNumber;
    }

    @JsonProperty("aadhaar_number")

    public void setAadharNumber(AadharNumber aadharNumber) {
        this.aadharNumber = aadharNumber;
    }
    @JsonProperty("image_url")

    public String getImageUrl() {
        return imageUrl;
    }
    @JsonProperty("image_url")

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    @JsonProperty("uniqueness_id")

    public String getUniquenessId() {
        return uniquenessId;
    }
    @JsonProperty("uniqueness_id")

    public void setUniquenessId(String uniquenessId) {
        this.uniquenessId = uniquenessId;
    }


}

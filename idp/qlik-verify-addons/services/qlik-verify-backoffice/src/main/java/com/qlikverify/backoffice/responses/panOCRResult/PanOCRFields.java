package com.qlikverify.backoffice.responses.panOCRResult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.qlikverify.backoffice.responses.panOCRResult.ocrData.ocrFields.DOB;
import com.qlikverify.backoffice.responses.panOCRResult.ocrData.ocrFields.FatherName;
import com.qlikverify.backoffice.responses.panOCRResult.ocrData.ocrFields.FullName;
import com.qlikverify.backoffice.responses.panOCRResult.ocrData.ocrFields.PanNumber;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "document_type",
        "pan_number",
        "full_name",
        "father_name",
        "dob"
})
public class PanOCRFields {

    @JsonProperty("document_type")
    private String documentType;

    @JsonProperty("pan_number")
    private PanNumber panNumber;

    @JsonProperty("full_name")
    private FullName fullName;

    @JsonProperty("father_name")
    private FatherName fatherName;

    @JsonProperty("dob")
    private DOB dob;

    @JsonProperty("document_type")
    public String getDocumentType() {
        return documentType;
    }

    @JsonProperty("document_type")
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    @JsonProperty("pan_number")
    public PanNumber getPanNumber() {
        return panNumber;
    }

    @JsonProperty("pan_number")
    public void setPanNumber(PanNumber panNumber) {
        this.panNumber = panNumber;
    }

    @JsonProperty("full_name")
    public FullName getFullName() {
        return fullName;
    }

    @JsonProperty("full_name")
    public void setFullName(FullName fullName) {
        this.fullName = fullName;
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
}

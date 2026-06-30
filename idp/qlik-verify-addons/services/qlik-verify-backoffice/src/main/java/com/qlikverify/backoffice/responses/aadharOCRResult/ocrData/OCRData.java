package com.qlikverify.backoffice.responses.aadharOCRResult.ocrData;

import com.fasterxml.jackson.annotation.*;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "client_id",
        "ocr_fields"
})
public class OCRData {
    @JsonCreator
    public OCRData(@JsonProperty("client_id") String clientId,
                   @JsonProperty("ocr_fields") OCRFields[] ocrFields) {
        this.clientId = clientId;
        this.ocrFields = ocrFields;
    }

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("ocr_fields")
    private OCRFields[] ocrFields;

    @JsonProperty("client_id")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("client_id")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("ocr_fields")
    public OCRFields[] getOcrFields() {
        return ocrFields;
    }
    @JsonProperty("ocr_fields")

    public void setOcrFields(OCRFields[] ocrFields) {
        this.ocrFields = ocrFields;
    }

}

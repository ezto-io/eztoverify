package com.qlikverify.backoffice.responses.panOCRResult;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "client_id",
        "ocr_fields"
})
public class PanOCRData {
    @JsonCreator
    public PanOCRData( @JsonProperty("client_id") String clientId, @JsonProperty("ocr_fields") PanOCRFields[] ocrFields) {
        this.clientId = clientId;
        this.ocrFields = ocrFields;
    }

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("ocr_fields")
    private PanOCRFields[] ocrFields;

    @JsonProperty("client_id")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("client_id")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    @JsonProperty("ocr_fields")
    public PanOCRFields[] getOcrFields() {
        return ocrFields;
    }

    @JsonProperty("ocr_fields")
    public void setOcrFields(PanOCRFields[] ocrFields) {
        this.ocrFields = ocrFields;
    }
}

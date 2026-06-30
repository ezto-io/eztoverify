package com.qlikverify.backoffice.responses.aadharOCRResult.ocrData.ocrFields;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "value",
        "confidence",
        "is_masked"
})
public class AadharNumber {
    @JsonCreator
    public AadharNumber(@JsonProperty("value") String value,
                        @JsonProperty("confidence") int confidence,
                        @JsonProperty("is_masked") boolean isMasked) {
        this.value = value;
        this.confidence = confidence;
        this.isMasked = isMasked;
    }

    @JsonProperty("value")
    private String value;

    @JsonProperty("confidence")
    private int confidence;
    @JsonProperty("is_masked")
    private boolean isMasked;

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

    @JsonProperty("is_masked")
    public boolean isMasked() {
        return isMasked;
    }
    @JsonProperty("is_masked")

    public void setMasked(boolean masked) {
        isMasked = masked;
    }

}

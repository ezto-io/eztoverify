package com.qlikverify.backoffice.responses.aadharOCRResult.ocrData.ocrFields;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "value",
        "confidence"
})
public class FullName {

    @JsonCreator
    public FullName(@JsonProperty("value") String value,
                  @JsonProperty("confidence") int confidence) {
        this.value = value;
        this.confidence = confidence;
    }

    @JsonProperty("value")
    private String value;
    @JsonProperty("confidence")
    private int confidence;

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
}

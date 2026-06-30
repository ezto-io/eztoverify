package com.qlikverify.backoffice.responses.aadharOCRResult.ocrData.ocrFields;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "value",
        "confidence",
        "yob"
})
public class DOB {

    @JsonCreator
    public DOB(@JsonProperty("value") String value,
                        @JsonProperty("confidence") int confidence,
               @JsonProperty("yob") boolean yob) {
        this.value = value;
        this.confidence = confidence;
    }

    @JsonProperty("value")
    private String value;
    @JsonProperty("confidence")
    private int confidence;

    @JsonProperty("yob")
    private boolean yob;

    @JsonProperty("yob")
    public boolean isYob() {
        return yob;
    }

    @JsonProperty("yob")
    public void setYob(boolean yob) {
        this.yob = yob;
    }

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

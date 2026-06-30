package com.qlikverify.backoffice.responses.aadharOCRResult.ocrData.ocrFields;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "value",
        "confidence",
        "relation"
})
public class CareOf {
    @JsonCreator
    public CareOf(@JsonProperty("value") String value,
                  @JsonProperty("confidence") int confidence,
                  @JsonProperty("relation") String relation) {
        this.value = value;
        this.confidence = confidence;
        this.relation = relation;
    }

    @JsonProperty("value")
    private String value;
    @JsonProperty("confidence")
    private int confidence;
    @JsonProperty("relation")
    private String relation;

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

    @JsonProperty("relation")
    public String isRelation() {
        return relation;
    }

    @JsonProperty("relation")
    public void setRelation(String relation) {
        this.relation = relation;
    }
}


package com.qlikverify.backoffice.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;



@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "key",
    "valueText"
})

public class DataSummaryEn {

    @JsonProperty("key")
    private String key;
    @JsonProperty("valueText")
    private String valueText;

    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    @JsonProperty("key")
    public void setKey(String key) {
        this.key = key;
    }

    @JsonProperty("valueText")
    public String getValueText() {
        return valueText;
    }

    @JsonProperty("valueText")
    public void setValueText(String valueText) {
        this.valueText = valueText;
    }

}

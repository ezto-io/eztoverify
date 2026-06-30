package com.qlikverify.models.ivr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwilioCallDetails {
    @JsonProperty("price")
    private String price;

    @JsonProperty("price_unit")
    private String priceUnit;
}

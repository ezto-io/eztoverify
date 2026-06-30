
package com.qlikverify.backoffice.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "introduction1_en",
    "authorizationHeader_en",
    "dataSummary_en"
})

public class KobilTransactionMessage {

    @JsonProperty("introduction1_en")
    private String introduction1En;
    @JsonProperty("authorizationHeader_en")
    private String authorizationHeaderEn;
    @JsonProperty("dataSummary_en")
    private List<DataSummaryEn> dataSummaryEn = null;

    @JsonProperty("introduction1_en")
    public String getIntroduction1En() {
        return introduction1En;
    }

    @JsonProperty("introduction1_en")
    public void setIntroduction1En(String introduction1En) {
        this.introduction1En = introduction1En;
    }

    @JsonProperty("authorizationHeader_en")
    public String getAuthorizationHeaderEn() {
        return authorizationHeaderEn;
    }

    @JsonProperty("authorizationHeader_en")
    public void setAuthorizationHeaderEn(String authorizationHeaderEn) {
        this.authorizationHeaderEn = authorizationHeaderEn;
    }

    @JsonProperty("dataSummary_en")
    public List<DataSummaryEn> getDataSummaryEn() {
        return dataSummaryEn;
    }

    @JsonProperty("dataSummary_en")
    public void setDataSummaryEn(List<DataSummaryEn> dataSummaryEn) {
        this.dataSummaryEn = dataSummaryEn;
    }

}

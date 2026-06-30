
package com.qlikverify.backoffice.responses;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "client_id",
    "otp_sent",
    "if_number",
    "valid_aadhaar",
    "status"
})
@Generated("jsonschema2pojo")
public class Data {

    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("otp_sent")
    private Boolean otpSent;
    @JsonProperty("if_number")
    private Boolean ifNumber;
    @JsonProperty("valid_aadhaar")
    private Boolean validAadhaar;
    @JsonProperty("status")
    private String status;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("client_id")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("client_id")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("otp_sent")
    public Boolean getOtpSent() {
        return otpSent;
    }

    @JsonProperty("otp_sent")
    public void setOtpSent(Boolean otpSent) {
        this.otpSent = otpSent;
    }

    @JsonProperty("if_number")
    public Boolean getIfNumber() {
        return ifNumber;
    }

    @JsonProperty("if_number")
    public void setIfNumber(Boolean ifNumber) {
        this.ifNumber = ifNumber;
    }

    @JsonProperty("valid_aadhaar")
    public Boolean getValidAadhaar() {
        return validAadhaar;
    }

    @JsonProperty("valid_aadhaar")
    public void setValidAadhaar(Boolean validAadhaar) {
        this.validAadhaar = validAadhaar;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}

package com.qlikverify.backoffice.responses.aadharGenerateOtpResult;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "if_number",
        "otp_sent",
        "client_id"
})
public class Data {

    @JsonProperty("if_number")
    private boolean ifNumber;
    @JsonProperty("otp_sent")
    private boolean otpSent;
    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("if_number")
    public boolean isIfNumber() {
        return ifNumber;
    }

    @JsonProperty("if_number")
    public void setIfNumber(boolean ifNumber) {
        this.ifNumber = ifNumber;
    }

    @JsonProperty("otp_sent")
    public boolean isOtpSent() {
        return otpSent;
    }

    @JsonProperty("otp_sent")
    public void setOtpSent(boolean otpSent) {
        this.otpSent = otpSent;
    }

    @JsonProperty("client_id")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("client_id")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}


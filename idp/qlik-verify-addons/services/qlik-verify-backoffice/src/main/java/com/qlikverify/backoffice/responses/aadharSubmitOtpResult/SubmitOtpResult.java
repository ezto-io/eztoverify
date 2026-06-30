package com.qlikverify.backoffice.responses.aadharSubmitOtpResult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "data",
        "status_code",
        "message",
        "success",
        "type"
})
public class SubmitOtpResult {
    @JsonProperty("data")
    private Data data;
    @JsonProperty("status_code")
    private int status_code;
    @JsonProperty("message")
    private String message;
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("data")
    public Data getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(Data data) {
        this.data = data;
    }

    @JsonProperty("status_code")
    public int getStatus_code() {
        return status_code;
    }

    @JsonProperty("status_code")
    public void setStatus_code(int status_code) {
        this.status_code = status_code;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("success")
    public boolean isSuccess() {
        return success;
    }

    @JsonProperty("success")
    public void setSuccess(boolean success) {
        this.success = success;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }
}

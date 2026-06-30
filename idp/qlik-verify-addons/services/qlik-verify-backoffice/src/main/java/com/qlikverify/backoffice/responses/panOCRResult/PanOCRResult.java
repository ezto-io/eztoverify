package com.qlikverify.backoffice.responses.panOCRResult;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "data",
        "status_code",
        "message_code",
        "message",
        "success"
})
public class PanOCRResult {


    @JsonCreator
    public PanOCRResult( @JsonProperty("data") PanOCRData data,
                         @JsonProperty("status_code") int statusCode,
                         @JsonProperty("message_code") String messageCode,
                         @JsonProperty("message") String message,
                         @JsonProperty("success") boolean success) {
        this.data = data;
        this.statusCode = statusCode;
        this.messageCode = messageCode;
        this.message = message;
        this.success = success;
    }

    @JsonProperty("data")
    private PanOCRData data;
    @JsonProperty("status_code")
    private int statusCode;
    @JsonProperty("message_code")
    private String messageCode;
    @JsonProperty("message")
    private String message;
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("data")
    public PanOCRData getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(PanOCRData data) {
        this.data = data;
    }

    @JsonProperty("status_code")
    public int getStatusCode() {
        return statusCode;
    }

    @JsonProperty("status_code")
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @JsonProperty("message_code")
    public String getMessageCode() {
        return messageCode;
    }

    @JsonProperty("message_code")
    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("message")
    public boolean isSuccess() {
        return success;
    }

    @JsonProperty("message")
    public void setSuccess(boolean success) {
        this.success = success;
    }
}

package com.qlikverify.backoffice.responses.aadharOCRResult;

import com.fasterxml.jackson.annotation.*;
import com.qlikverify.backoffice.responses.aadharOCRResult.ocrData.OCRData;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "data",
        "status_code",
        "message_code",
        "message",
        "success"
})
public class AadharOCRResult {

    @JsonCreator
    public AadharOCRResult(@JsonProperty("data") OCRData data,
                           @JsonProperty("status_code") int statusCode,
                           @JsonProperty("success") boolean success,
                           @JsonProperty("message") String message,
                           @JsonProperty("message_code") String messageCode) {
        this.data = data;
        this.statusCode = statusCode;
        this.success = success;
        this.message = message;
        this.messageCode = messageCode;
    }


    @JsonProperty("data")
    private OCRData data;
    @JsonProperty("status_code")
    private Integer statusCode;
    @JsonProperty("message_code")
    private String messageCode;
    @JsonProperty("message")
    private String message;
    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("data")
    public OCRData getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(OCRData data) {
        this.data = data;
    }

    @JsonProperty("status_code")
    public Integer getStatusCode() {
        return statusCode;
    }

    @JsonProperty("status_code")
    public void setStatusCode(Integer statusCode) {
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

    @JsonProperty("success")
    public Boolean getSuccess() {
        return success;
    }

    @JsonProperty("success")
    public void setSuccess(Boolean success) {
        this.success = success;
    }


}

package com.qlikverify.backoffice.responses;

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
public class PanResult {

    @JsonCreator
    public PanResult( @JsonProperty("data") PanData data,
                      @JsonProperty("status_code") Integer statusCode,
                      @JsonProperty("message_code") String messageCode,
                      @JsonProperty("message") String message,
                      @JsonProperty("success") Boolean success) {
        this.data = data;
        this.statusCode = statusCode;
        this.messageCode = messageCode;
        this.message = message;
        this.success = success;
    }

    @JsonProperty("data")
    private PanData data;
    @JsonProperty("status_code")
    private Integer statusCode;
    @JsonProperty("message_code")
    private String messageCode;
    @JsonProperty("message")
    private String message;
    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("data")
    public PanData getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(PanData data) {
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


package com.qlikverify.backoffice.responses;

import com.fasterxml.jackson.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "data",
    "status_code",
    "message_code",
    "message",
    "success"
})
public class AadharResult {

    @JsonCreator
    public AadharResult( @JsonProperty("data") Data data,
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
    private Data data;
    @JsonProperty("status_code")
    private Integer statusCode;
    @JsonProperty("message_code")
    private String messageCode;
    @JsonProperty("message")
    private String message;
    @JsonProperty("success")
    private Boolean success;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("data")
    public Data getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(Data data) {
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

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}

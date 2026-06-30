
package com.qlikverify.services.emails.mandrill.models;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "key",
    "message",
    "async",
    "ip_pool",
    "send_at"
})

public class MailChimp {

    @JsonProperty("key")
    private String key;
    @JsonProperty("message")
    private Message message;
    @JsonProperty("async")
    private Boolean async;
    @JsonProperty("ip_pool")
    private String ipPool;
    @JsonProperty("send_at")
    private String sendAt;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    @JsonProperty("key")
    public void setKey(String key) {
        this.key = key;
    }

    @JsonProperty("message")
    public Message getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(Message message) {
        this.message = message;
    }

    @JsonProperty("async")
    public Boolean getAsync() {
        return async;
    }

    @JsonProperty("async")
    public void setAsync(Boolean async) {
        this.async = async;
    }

    @JsonProperty("ip_pool")
    public String getIpPool() {
        return ipPool;
    }

    @JsonProperty("ip_pool")
    public void setIpPool(String ipPool) {
        this.ipPool = ipPool;
    }

    @JsonProperty("send_at")
    public String getSendAt() {
        return sendAt;
    }

    @JsonProperty("send_at")
    public void setSendAt(String sendAt) {
        this.sendAt = sendAt;
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

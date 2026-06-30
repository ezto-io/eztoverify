
package com.qlikverify.services.events.models.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "time",
    "type",
    "realm",
    "client",
    "user",
    "sessionId",
    "ipAddress",
    "details"
})

public class EventData {

    @JsonProperty("id")
    public String id;
    @JsonProperty("time")
    public Long time;
    @JsonProperty("type")
    public String type;
    @JsonProperty("realm")
    public Realm realm;
    @JsonProperty("client")
    public Client client;
    @JsonProperty("user")
    public User user;
    @JsonProperty("sessionId")
    public String sessionId;
    @JsonProperty("ipAddress")
    public String ipAddress;
    @JsonProperty("details")
    public Details details;

}

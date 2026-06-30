
package com.qlikverify.services.events.models.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "clientId",
    "name",
    "appType"
})

public class Client {

    @JsonProperty("id")
    public String id;
    @JsonProperty("clientId")
    public String clientId;
    @JsonProperty("name")
    public String name;
    @JsonProperty("appType")
    public String appType;

}

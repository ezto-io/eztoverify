
package com.qlikverify.services.events.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.keycloak.events.admin.AdminEvent;



@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "eventData",
    "eventType"
})

public class AdminEventSource {

    @JsonProperty("eventData")
    public AdminEvent eventData;
    @JsonProperty("eventType")
    public String eventType;

}


package com.qlikverify.services.events.models.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.keycloak.events.Event;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "eventData",
    "eventType"
})

public class EventSource {

    @JsonProperty("eventData")
    public Event eventData;
    @JsonProperty("eventType")
    public String eventType;

}

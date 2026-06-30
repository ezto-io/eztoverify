package com.qlikverify.services.events.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectInBucket {
    @JsonProperty("hits")
    public EventHits hits;
}

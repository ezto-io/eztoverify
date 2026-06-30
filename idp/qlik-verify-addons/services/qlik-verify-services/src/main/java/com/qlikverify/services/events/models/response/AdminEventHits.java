
package com.qlikverify.services.events.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "total",
    "max_score",
    "hits"
})

public class AdminEventHits {

    @JsonProperty("total")
    public Total total;
    @JsonProperty("max_score")
    public Object maxScore;
    @JsonProperty("hits")
    public List<AdminEventHit> hits;

}

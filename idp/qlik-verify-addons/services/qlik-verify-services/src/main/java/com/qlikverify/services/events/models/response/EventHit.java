
package com.qlikverify.services.events.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "_index",
    "_id",
    "_score",
    "_source",
    "sort"
})

public class EventHit {

    @JsonProperty("_index")
    public String index;
    @JsonProperty("_id")
    public String id;
    @JsonProperty("_score")
    public Object score;
    @JsonProperty("_ignored")
    public Object ignored;
    @JsonProperty("_source")
    public EventSource source;
    @JsonProperty("sort")
    public List<Long> sort;

}

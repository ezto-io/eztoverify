
package com.qlikverify.services.events.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.keycloak.representations.idm.UserRepresentation;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "took",
    "timed_out",
    "_shards",
    "hits"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class AESModel {

    @JsonProperty("took")
    public Integer took;
    @JsonProperty("timed_out")
    public Boolean timedOut;
    @JsonProperty("_shards")
    public Shards shards;
    @JsonProperty("hits")
    public AdminEventHits hits;
    @JsonProperty("aggregations")
    public Aggregations aggregations;
    @JsonProperty("num_reduce_phases")
    public Integer numReducePhases;
    @JsonProperty("accountCreator")
    public UserRepresentation accountCreator;
}

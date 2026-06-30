
package com.qlikverify.services.events.models.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "total",
    "successful",
    "skipped",
    "failed"
})

public class Shards {

    @JsonProperty("total")
    public Integer total;
    @JsonProperty("successful")
    public Integer successful;
    @JsonProperty("skipped")
    public Integer skipped;
    @JsonProperty("failed")
    public Integer failed;

}


package com.qlikverify.services.events.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "buckets"
})
@JsonIncludeProperties({"not marked as ignorable","sum_other_doc_count"})
public class AggregationField {

    @JsonProperty("buckets")
    public List<Bucket> buckets;

    @JsonProperty("doc_count_error_upper_bound")
    public int docCountError;

    @JsonProperty("sum_other_doc_count")
    public int sumOtherDocCount;
}

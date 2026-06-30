
package com.qlikverify.services.events.models.response;


import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({})
public class Aggregations {

    public Map<String, AggregationField> field = new HashMap<>();
    @JsonAnySetter
    public void setField(String key, AggregationField value) {
        field.put(key, value);
    }

    public AggregationField get(String key) {
        return field.get(key);
    }

}

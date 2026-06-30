
package com.qlikverify.services.events.models.response;


import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "key",
    "doc_count"
})

public class Bucket {

    @JsonProperty("key")
    public Object key;
    @JsonProperty("doc_count")
    public Integer docCount;

    @Nullable
    public Map<String, ObjectInBucket> field = new HashMap<>();
    @JsonAnySetter
    public void setField(String key, ObjectInBucket value) {
        field.put(key, value);
    }

    public ObjectInBucket get(String key) {
        return field.get(key);
    }

}

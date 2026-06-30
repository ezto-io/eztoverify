
package com.qlikverify.services.events.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "realm",
    "displayName",
    "hasCustomDomain",
    "dnsenabled",
    "live"
})
public class Realm {

    @JsonProperty("id")
    public String id;
    @JsonProperty("realm")
    public String realm;
    @JsonProperty("displayName")
    public String displayName;
    @JsonProperty("hasCustomDomain")
    public Boolean hasCustomDomain;
    @JsonProperty("dnsenabled")
    public Boolean dnsenabled;
    @JsonProperty("live")
    public Boolean live;

}

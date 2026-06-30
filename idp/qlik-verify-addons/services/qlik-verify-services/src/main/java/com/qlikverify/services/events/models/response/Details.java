
package com.qlikverify.services.events.models.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "auth_method",
    "auth_type",
    "os",
    "browser",
    "redirect_uri",
    "consent",
    "code_id",
    "username",
    "response_type",
    "response_mode"
})

public class Details {

    @JsonProperty("auth_method")
    public String authMethod;
    @JsonProperty("auth_type")
    public String authType;
    @JsonProperty("os")
    public String os;
    @JsonProperty("browser")
    public String browser;
    @JsonProperty("redirect_uri")
    public String redirectUri;
    @JsonProperty("consent")
    public String consent;
    @JsonProperty("code_id")
    public String codeId;
    @JsonProperty("username")
    public String username;
    @JsonProperty("response_type")
    public String responseType;
    @JsonProperty("response_mode")
    public String responseMode;

}

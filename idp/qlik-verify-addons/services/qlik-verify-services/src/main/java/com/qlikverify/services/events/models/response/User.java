
package com.qlikverify.services.events.models.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "username",
    "email",
    "skipSendingEmail"
})

public class User {

    @JsonProperty("id")
    public String id;
    @JsonProperty("username")
    public String username;
    @JsonProperty("email")
    public String email;
    @JsonProperty("skipSendingEmail")
    public Boolean skipSendingEmail;

}

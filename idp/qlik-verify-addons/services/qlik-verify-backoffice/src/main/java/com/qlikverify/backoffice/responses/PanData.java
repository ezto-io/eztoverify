package com.qlikverify.backoffice.responses;

import com.fasterxml.jackson.annotation.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "client_id",
        "pan_number",
        "full_name",
        "category"
})
public class PanData {

    @JsonCreator
    public PanData(@JsonProperty("client_id") String clientId,
                   @JsonProperty("pan_number") String panNumber,
                   @JsonProperty("full_name") String fullName,
                   @JsonProperty("category") String category) {
        this.clientId = clientId;
        this.panNumber = panNumber;
        this.fullName = fullName;
        this.category = category;
    }

    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("pan_number")
    private String panNumber;
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("category")
    private String category;

    @JsonProperty("client_id")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("client_id")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("pan_number")
    public String getPanNumber() {
        return panNumber;
    }

    @JsonProperty("pan_number")
    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    @JsonProperty("full_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @JsonProperty("category")
    public String getCategory() {
        return category;
    }

    @JsonProperty("category")
    public void setCategory(String category) {
        this.category = category;
    }
}

package com.qlikverify.extensions.admin.misc.responses;

public class GetAuthFlowResponse {
    private String id;
    private String alias;
    private String description;
    private String providerId;
    private boolean topLevel;
    private boolean builtIn;

    public GetAuthFlowResponse() {
    }

    public GetAuthFlowResponse(String id, String alias, String description, String providerId, boolean topLevel, boolean builtIn) {
        this.id = id;
        this.alias = alias;
        this.description = description;
        this.providerId = providerId;
        this.topLevel = topLevel;
        this.builtIn = builtIn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    public void setTopLevel(boolean topLevel) {
        this.topLevel = topLevel;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }
}

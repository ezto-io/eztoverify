package com.qlikverify.extensions.admin.misc.responses;

import org.keycloak.models.AuthenticationExecutionModel;

public class GetAuthFlowExecutionResponse {
    private String id;
    private String authenticatorConfig;
    private String authenticator;
    private String flowId;
    private boolean authenticatorFlow;
    private AuthenticationExecutionModel.Requirement requirement;
    private int priority;
    private String parentFlow;

    public GetAuthFlowExecutionResponse() {
    }

    public GetAuthFlowExecutionResponse(String id, String authenticatorConfig, String authenticator, String flowId, boolean authenticatorFlow, AuthenticationExecutionModel.Requirement requirement, int priority, String parentFlow) {
        this.id = id;
        this.authenticatorConfig = authenticatorConfig;
        this.authenticator = authenticator;
        this.flowId = flowId;
        this.authenticatorFlow = authenticatorFlow;
        this.requirement = requirement;
        this.priority = priority;
        this.parentFlow = parentFlow;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthenticatorConfig() {
        return authenticatorConfig;
    }

    public void setAuthenticatorConfig(String authenticatorConfig) {
        this.authenticatorConfig = authenticatorConfig;
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public boolean isAuthenticatorFlow() {
        return authenticatorFlow;
    }

    public void setAuthenticatorFlow(boolean authenticatorFlow) {
        this.authenticatorFlow = authenticatorFlow;
    }

    public AuthenticationExecutionModel.Requirement getRequirement() {
        return requirement;
    }

    public void setRequirement(AuthenticationExecutionModel.Requirement requirement) {
        this.requirement = requirement;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getParentFlow() {
        return parentFlow;
    }

    public void setParentFlow(String parentFlow) {
        this.parentFlow = parentFlow;
    }
}

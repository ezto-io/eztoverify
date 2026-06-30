package com.qlikverify.extensions.admin.controllers.workflows.misc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.annotation.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowsFlowModel {
    private String realmId;
    private String flowId;
    private String flowName;
    private String connectedApp;
    private String webhookUrl;
    private String eventType;
    @Nullable
    private String createdTime;
    @Nullable
    private String enabled;
    private String iconUrl;
    @Nullable
    private Object metaData;

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public String getConnectedApp() {
        return connectedApp;
    }

    public void setConnectedApp(String connectedApp) {
        this.connectedApp = connectedApp;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    @Nullable
    public Object getMetaData() {
        return metaData;
    }

    public void setMetaData(@Nullable Object metaData) {
        this.metaData = metaData;
    }
}

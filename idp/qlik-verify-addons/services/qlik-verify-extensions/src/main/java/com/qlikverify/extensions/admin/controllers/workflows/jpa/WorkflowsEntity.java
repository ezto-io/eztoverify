package com.qlikverify.extensions.admin.controllers.workflows.jpa;

import org.hibernate.annotations.GenericGenerator;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "WORKFLOWS", indexes = {
        @Index(name = "idw_realm_id_event_type", columnList = "REALM_ID, EVENT_TYPE")
})
public class WorkflowsEntity {
    @Id
    @Column(name = "ID")
    private String id;

    @Column(name="REALM_ID", nullable = false)
    private String realmId;

    @Column(name="FLOW_ID", nullable = false)
    private String flowId;

    @Column(name="FLOW_NAME", nullable = false)
    private String flowName;

    @Column(name="CONNECTED_APP", nullable = false)
    private String connectedApp;

    @Column(name="WEBHOOK_URL", nullable = false)
    private String webhookUrl;

    @Column(name="EVENT_TYPE", nullable = false)
    private String eventType;

    @Column(name="ENABLED", nullable = false)
    private boolean enabled;

    @Column(name="CREATED_TIME", nullable = false)
    private String createdTime;

    @Column(name="ICON_URL", nullable = false)
    private String iconUrl;

    @Column(name="METADATA")
    private String metaData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public boolean isSubscribed() {
        return enabled;
    }

    public void setSubscribed(boolean subscribed) {
        this.enabled = subscribed;
    }

    public String getConnectedApp() {
        return connectedApp;
    }

    public void setConnectedApp(String connectedApp) {
        this.connectedApp = connectedApp;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    @PrePersist
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    public void onCreate() {
        if (Objects.isNull(this.id)) {
            this.id = KeycloakModelUtils.generateId();
        }
    }
}

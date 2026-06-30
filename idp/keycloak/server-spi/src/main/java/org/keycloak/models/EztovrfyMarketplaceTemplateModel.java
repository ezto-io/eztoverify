package org.keycloak.models;

import java.io.Serializable;
import java.util.List;

public class EztovrfyMarketplaceTemplateModel implements Serializable {

    private String id;
    private String realmId;
    private String workflowName;
    private String workflowCategory;
    private String imageURL;
    private String workflowDescription;
    private List<String> templateTag;
    private String country;
    private Long modifiedTimestamp;
    private boolean isPublished;
    private String workflowId;
    private String workflowJson;
    private boolean marketplace;

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

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getWorkflowCategory() {
        return workflowCategory;
    }

    public void setWorkflowCategory(String workflowCategory) {
        this.workflowCategory = workflowCategory;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getWorkflowDescription() {
        return workflowDescription;
    }

    public void setWorkflowDescription(String workflowDescription) {
        this.workflowDescription = workflowDescription;
    }

    public List<String> getTemplateTag() {
        return templateTag;
    }

    public void setTemplateTag(List<String> templateTag) {
        this.templateTag = templateTag;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(Long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean published) {
        isPublished = published;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowJson() {
        return workflowJson;
    }

    public void setWorkflowJson(String workflowJson) {
        this.workflowJson = workflowJson;
    }

    public boolean isMarketplace() {
        return marketplace;
    }

    public void setMarketplace(boolean marketplace) {
        this.marketplace = marketplace;
    }
}

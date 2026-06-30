package org.keycloak.models.jpa.entities;


import javax.persistence.*;

@Table(name="EZTOVRFY_MARKETPLACE_TEMPLATE")
@Entity
@NamedQueries({
        @NamedQuery(name = "searchEztovrfyMarketplaceTemplateByName", query = "select template from EztovrfyMarketplaceTemplateEntity template where lower(template.workflowName) like concat('%', lower(:name), '%') or lower(cast(template.templateTag as text)) like concat('%', lower(:tag), '%')")
})
public class EztovrfyMarketplaceTemplateEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "WORKFLOW_NAME")
    protected String workflowName;

    @Column(name = "WORKFLOW_CATEGORY")
    protected String workflowCategory;

    @Column(name = "IMAGE_URL")
    protected String imageURL;

    @Column(name = "WORKFLOW_DESCRIPTION")
    protected String workflowDescription;

    @Column(name = "TEMPLATE_TAG")
    protected String templateTag;

    @Column(name = "COUNTRY")
    protected String country;

    @Column(name = "MODIFIED_TIMESTAMP")
    protected Long modifiedTimestamp;

    @Column(name = "IS_PUBLISHED")
    protected boolean isPublished;

    @Column(name = "WORKFLOW_ID")
    protected String workflowId;

    @Column(name = "WORKFLOW_JSON")
    protected String workflowJson;

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

    public String getTemplateTag() {
        return templateTag;
    }

    public void setTemplateTag(String templateTag) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof EztovrfyMarketplaceTemplateEntity)) return false;

        EztovrfyMarketplaceTemplateEntity that = (EztovrfyMarketplaceTemplateEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}


package org.keycloak.representations.idm;

public class ReportRepresentation {

    protected Long createdTimestamp;
    protected RealmRepresentation realm;
    protected UserRepresentation requestedBy;
    protected String downloadUrl;
    protected String reportType;
    protected String id;
    protected String status;

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public RealmRepresentation getRealm() {
        return realm;
    }

    public void setRealm(RealmRepresentation realm) {
        this.realm = realm;
    }

    public UserRepresentation getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UserRepresentation requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

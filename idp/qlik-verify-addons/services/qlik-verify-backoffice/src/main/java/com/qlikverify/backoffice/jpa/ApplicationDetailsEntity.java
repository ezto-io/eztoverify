package com.qlikverify.backoffice.jpa;


import javax.persistence.*;


@NamedQueries({
        @NamedQuery(name = "getAppsDetails", query = "select u from ApplicationDetailsEntity u where u.realmId = :realmId and u.appType = :appType ORDER BY u.createdAt ASC"),
        @NamedQuery(name = "updateAppDetails", query = "update ApplicationDetailsEntity u set u.appVersion = :appVersion, u.updateUrl = :updateUrl, u.appType = :appType, u.appPlatform = :appPlatform, u.appName = :appName where u.realmId = :realmId and u.id = :id"),
        @NamedQuery(name = "removeAppsById", query = "delete from ApplicationDetailsEntity u where u.realmId = :realmId and u.id = :id"),
        @NamedQuery(name = "getAppDetailsById", query = "select u from ApplicationDetailsEntity u where u.realmId = :realmId and u.id = :id"),
})

@Table(name="APPLICATION_DETAILS")
@Entity
public class ApplicationDetailsEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "APP_NAME")
    protected String appName;

    @Column(name = "APP_VERSION")
    protected String appVersion;

    @Column(name = "UPDATE_URL")
    protected String updateUrl;

    @Column(name = "REALM_ID")
    private String realmId;

    @Column(name = "TYPE")
    private String appType;

    @Column(name = "PLATFORM")
    private String appPlatform;

    @Column(name = "CREATED_AT")
    private Long createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getAppPlatform() {
        return appPlatform;
    }

    public void setAppPlatform(String appPlatform) {
        this.appPlatform = appPlatform;
    }

    public Long getCreatedTimestamp() {
        return createdAt;
    }
    @PrePersist
    public void prePersist() {
        this.createdAt = System.currentTimeMillis();
    }

}

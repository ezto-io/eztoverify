package org.keycloak.downloads;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DownloadData {

    private String description;
    @JsonIgnore
    private String version;
    private String downloadUrl;
    @JsonIgnore
    private boolean isPreRelease;
    @JsonIgnore
    private String downloadWay;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public boolean isPreRelease() {
        return isPreRelease;
    }

    public void setPreRelease(boolean preRelease) {
        isPreRelease = preRelease;
    }

    public String getDownloadWay() {
        return downloadWay;
    }

    public void setDownloadWay(String downloadWay) {
        this.downloadWay = downloadWay;
    }
}

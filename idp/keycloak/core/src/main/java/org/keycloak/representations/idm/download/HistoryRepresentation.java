package org.keycloak.representations.idm.download;

public class HistoryRepresentation {

    private String title;
    private String version;
    private String description;
    private String downloadUrl;
    private String downloadButtonName;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getDownloadButtonName() {
        return downloadButtonName;
    }

    public void setDownloadButtonName(String downloadButtonName) {
        this.downloadButtonName = downloadButtonName;
    }
}

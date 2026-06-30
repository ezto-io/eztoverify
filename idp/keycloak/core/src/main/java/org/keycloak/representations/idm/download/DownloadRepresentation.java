package org.keycloak.representations.idm.download;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DownloadRepresentation {
    private boolean hasHistory;
    private String title;
    private String description;
    private String version;
    private String downloadUrl;
    private String downloadButtonName;
    private String logoUrl;
    private boolean isPreRelease;

    public void setHasHistory(boolean hasHistory) {
        this.hasHistory = hasHistory;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void setDownloadButtonName(String downloadButtonName) {
        this.downloadButtonName = downloadButtonName;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public boolean isPreRelease() {
        return isPreRelease;
    }

    public void setPreRelease(boolean preRelease) {
        isPreRelease = preRelease;
    }
}

package com.qlikverify.services.theme.downloads;

import okhttp3.Response;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.downloads.DownloadData;
import org.keycloak.downloads.DownloadProvider;
import org.keycloak.downloads.DownloadProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.downloads.Tag;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WinLogonDownloadProvider implements DownloadProvider, DownloadProviderFactory {

    protected static final String DEFAULT_SERVER_URL =
            EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    @Override
    public DownloadData getDownloadDetails() {
        String env = EnvironmentReader.getValue("ENV");
        String repo = getId();
        if (env == null || !env.equalsIgnoreCase("prod")) {
            repo = "dev-" + repo;
        }
        HttpUtility httpUtility = new HttpUtility();
        Response response = httpUtility.callRequest("application/vnd.github+json", "GET",
                String.format("https://api.github.com/repos/%s/%s/releases/latest", EnvironmentReader.getValue(
                        "DOWNLOADS_GITHUB_REPO_NAME", "ezto-io"), repo), null,
                "Bearer " + EnvironmentReader.getValue("DOWNLOAD_TOKEN"));
        if (!response.isSuccessful())
            return null;

        try {
            Map<String, Object> data = JsonSerialization.readValue(response.body().string(), Map.class);
            DownloadData downloadData = JsonSerialization.readValue(String.valueOf(data.get("body")),
                    DownloadData.class);
            downloadData.setVersion(String.valueOf(data.get("name")));
            downloadData.setPreRelease(Boolean.parseBoolean(String.valueOf(data.get("prerelease"))));
            return downloadData;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<DownloadData> getHistory() {
        return new ArrayList<>();
    }

    @Override
    public String getTitle() {
        return "qv-winlogon-credentials-provider";
    }

    @Override
    public String getDownloadButtonName() {
        return "download-latest";
    }

    @Override
    public String getLogoUrl() {
        return DEFAULT_SERVER_URL + "/downloads/windows.svg";
    }

    @Override
    public Tag getTag() {
        Tag tag = new Tag();
        tag.setTag("desktop-auth");
        tag.setDisplayName("desktop-auth-agents");
        return tag;
    }

    @Override
    public boolean hasHistory() {
        return false;
    }

    @Override
    public String[] supportedModes() {
        return new String[] {"wiam"};
    }

    @Override
    public DownloadProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return "winlogon";
    }
}

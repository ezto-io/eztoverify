package org.keycloak.downloads;

import org.keycloak.provider.Provider;

import java.util.List;

public interface DownloadProvider extends Provider {

    DownloadData getDownloadDetails();

    List<DownloadData> getHistory();
}

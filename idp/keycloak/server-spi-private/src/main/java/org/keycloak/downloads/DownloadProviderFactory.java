package org.keycloak.downloads;

import org.keycloak.provider.ProviderFactory;

public interface DownloadProviderFactory extends ProviderFactory<DownloadProvider> {

    String getTitle();

    String getDownloadButtonName();

    String getLogoUrl();

    Tag getTag();

    boolean hasHistory();

    String[] supportedModes();
}

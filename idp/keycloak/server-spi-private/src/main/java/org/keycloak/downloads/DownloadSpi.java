package org.keycloak.downloads;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class DownloadSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "download-spi";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return DownloadProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return DownloadProviderFactory.class;
    }
}

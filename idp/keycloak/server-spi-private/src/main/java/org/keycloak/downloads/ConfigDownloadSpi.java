package org.keycloak.downloads;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ConfigDownloadSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "config-download-spi";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ConfigDownloadProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ConfigDownloadProviderFactory.class;
    }
}

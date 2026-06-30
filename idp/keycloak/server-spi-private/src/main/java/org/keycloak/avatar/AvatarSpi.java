package org.keycloak.avatar;


import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class AvatarSpi implements Spi {
    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "avatarStorage";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AvatarProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return  AvatarProviderFactory.class;
    }
}

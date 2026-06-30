package com.qlikverify.backoffice.extensions;

import com.qlikverify.backoffice.extensions.AppliationDetailsProvider;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProvider;
import org.keycloak.services.resource.ezto.EztoPublicRealmResourceProviderFactory;

public class ApplicationDetailsProviderFactory implements EztoPublicRealmResourceProviderFactory {
    public static final String ID = "app-updates";
    @Override
    public EztoPublicRealmResourceProvider create(KeycloakSession session) {
        return new AppliationDetailsProvider(session);
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
        return ID;
    }
}

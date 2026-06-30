package org.keycloak.ciba;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

import java.util.List;

public interface CibaTutorialProviderFactory extends ProviderFactory<CibaTutorialProvider> {
    List<ProviderFactory> getConfig(KeycloakSession session);
}

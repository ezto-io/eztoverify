package org.keycloak.marketplace;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

import java.util.Map;

public interface MarketplaceProvider extends Provider {
    /**
     * This method will install the authentication flows of the template
     * @param config
     * @return
     */
    boolean install(Map<String,String> config);
}

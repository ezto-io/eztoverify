package org.keycloak.ciba;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
import java.util.List;

public interface CibaNotificationProviderFactory extends ProviderFactory<CibaNotificationProvider> {

    boolean isConfigurable();

    String getDisplayName();

    List<ProviderConfigProperty> getConfigProperties(KeycloakSession session);

    default List<ProviderConfigProperty> getOnDemandConfig(KeycloakSession session, HashMap<String,Object> config)
    {
        return null;
    }

    default void onCleanUp (KeycloakSession session, RealmModel realm, ClientModel client) {}
}

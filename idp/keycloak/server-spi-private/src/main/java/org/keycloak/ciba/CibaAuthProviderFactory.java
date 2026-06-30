package org.keycloak.ciba;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface CibaAuthProviderFactory extends ProviderFactory<CibaAuthProvider> {

    boolean isConfigurable();

    String getDisplayName();

    List<ProviderConfigProperty> getConfigProperties(KeycloakSession session);

    default boolean isTemplateSupported() {
        return true;
    }

    default List<ProviderConfigProperty> getOnDemandConfig(KeycloakSession session, HashMap<String, Object> config) {
        return null;
    }

    default Map<String, String> getHiddenConfig(KeycloakSession session) {
        return null;
    }

    default boolean onSave(KeycloakSession session, HashMap<String, String> config) {
        return true;
    }

    default boolean onUpdate(KeycloakSession session, HashMap<String, String> oldConfig, HashMap<String, String> newConfig) {
        return true;
    }

    default boolean onDelete(KeycloakSession session, HashMap<String, String> config) {
        return true;
    }

    default void onCleanUp(KeycloakSession session, RealmModel realm, ClientModel client) {}
}

package org.keycloak.attributes;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

public interface RealmAttributeProvider extends Provider {

    boolean rotateSecret(KeycloakSession session, RealmModel realmModel);

    default boolean generateSecret(KeycloakSession session, RealmModel realmModel){
        return false;
    }

    boolean updateAttribute(KeycloakSession session, RealmModel model, Object value);
}

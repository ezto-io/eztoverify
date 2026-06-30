package org.keycloak.authentication.requiredactions.util;

import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.List;

public class RequiredActionsValidator {
    /**
     * Validate provided required actions
     *
     * @param session         the {@code KeycloakSession}
     * @param requiredActions IDs of tested required actions
     */
    public static boolean validRequiredActions(KeycloakSession session, List<String> requiredActions) {
        final KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

        for (String action : requiredActions) {
            if (sessionFactory.getProviderFactory(RequiredActionProvider.class, action) == null) {
                return false;
            }
        }
        return true;
    }
}

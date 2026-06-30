package org.keycloak.policy.ciba;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

public interface CibaPolicyProvider extends Provider {

    boolean validate(KeycloakSession session, String bindingMessage, String cibaPolicyName);
}

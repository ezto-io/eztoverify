package org.keycloak.authentication;

import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Marking any required action implementation, that is supposed to work with user credentials
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface CredentialAction {

    /**
     * @return credential type, which this action is able to register. This should refer to the same value as returned by {@link org.keycloak.credential.CredentialProvider#getType} of the
     * corresponding credential provider and {@link AuthenticatorFactory#getReferenceCategory()} of the corresponding authenticator
     */
    String getCredentialType(KeycloakSession session, AuthenticationSessionModel authenticationSession);
}
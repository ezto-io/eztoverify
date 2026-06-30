package com.qlikverify.services.actiontoken;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.crypto.*;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.JsonWebToken;

public class OfflineActionTokenValidation {

    public static String generateToken(KeycloakSession session, String clientId, String audience, long expiresIn) {
        return new JWSBuilder().type(OAuth2Constants.JWT).jsonContent(generateJsonWebToken(session, clientId, audience, expiresIn)).sign(getSignatureContext(session));
    }

    public static JsonWebToken validateToken(KeycloakSession session, String encodedToken) {

        if (encodedToken == null) {
            throw new IdentityBrokerException("No token from server.");
        }
        String uri = session.getContext().getRealm().getId();
        JsonWebToken token;
        try {
            TokenVerifier<OfflineActionToken> verifier = TokenVerifier.create(encodedToken, OfflineActionToken.class).withDefaultChecks()
                    .realmUrl(uri);
            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();
            SignatureVerifierContext signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
            verifier.verifierContext(signatureVerifier);
            token = verifier.verify().getToken();
        } catch (Exception e) {
            throw new IdentityBrokerException("Invalid Token", e);
        }
        return token;
    }


    private static SignatureSignerContext getSignatureContext(KeycloakSession session) {
        return new AsymmetricSignatureProvider(session, Algorithm.RS256).signer();
    }

    private static JsonWebToken generateJsonWebToken(KeycloakSession session, String clientId, String audience, long expiresIn) {
        JsonWebToken jwt = new JsonWebToken();
        jwt.id(KeycloakModelUtils.generateId());
        jwt.type("Bearer");
        jwt.issuer(session.getContext().getRealm().getId());
        jwt.subject(clientId);
        jwt.audience(audience);
        jwt.exp(expiresIn);
        jwt.issuedNow();
        return jwt;
    }
}

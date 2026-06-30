package com.qlikverify.extensions.admin.controllers.actionToken;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.crypto.*;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.JsonWebToken;

public class EztoActionTokenVerifier {

    private static KeycloakSession session;
    private static RealmModel realm;


    public EztoActionTokenVerifier(KeycloakSession session, RealmModel realm){
        this.session = session;
        this.realm = realm;
    }

    public static String generateToken(KeycloakSession session, long expiresIn, String requestId, String audience) {
        String jws = new JWSBuilder().type(OAuth2Constants.JWT).jsonContent(generateJsonWebToken(
                expiresIn,session.getContext().getRealm().getId(), requestId, audience)).sign(getSignatureContext(session));
        return jws;
    }

    private static SignatureSignerContext getSignatureContext(KeycloakSession session) {
        return new AsymmetricSignatureProvider(session, Algorithm.RS256).signer();
    }

    private static Object generateJsonWebToken(long expiresIn, String realmId, String requestId, String audience) {
        JsonWebToken jwt = new JsonWebToken();
        jwt.id(KeycloakModelUtils.generateId());
        jwt.type("Bearer");
        jwt.issuer(realmId);
        jwt.subject(requestId);
        jwt.audience(audience);
        jwt.exp(expiresIn/1000);
        jwt.issuedNow();
        return jwt;
    }

    public static JsonWebToken validateToken(String encodedToken, String realmId, boolean ignoreAudience) {
        final JsonWebToken[] token = {null};
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession session) throws JsonProcessingException {

                if (encodedToken == null) {
                    throw new IdentityBrokerException("No token from server.");
                }
                RealmModel realmById = session.realms().getRealm(realmId);
                session.getContext().setRealm(realmById);

                try{
                    TokenVerifier<EztoActionToken> verifier =
                            TokenVerifier.create(encodedToken, EztoActionToken.class).withChecks(
                                            TokenVerifier.IS_ACTIVE
                                    )
                                    .realmUrl(realmById.getId());
                    String kid = verifier.getHeader().getKeyId();
                    String algorithm = verifier.getHeader().getAlgorithm().name();
                    SignatureVerifierContext signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
                    verifier.verifierContext(signatureVerifier);
                    token[0] = verifier.verify().getToken();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        return token[0];
    }
}

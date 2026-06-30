package org.keycloak.services.actionToken;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.crypto.*;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.JsonWebToken;

public class BackgroundProcessorActionTokenVerifier {
    private static KeycloakSession session;
    private static RealmModel realm;

    public BackgroundProcessorActionTokenVerifier(KeycloakSession session, RealmModel realm){
        this.session = session;
        this.realm = realm;
    }

    public BackgroundProcessorActionTokenVerifier(){}

    public static String generateToken(KeycloakSession session, long expiresIn) {
        String jws = new JWSBuilder().type(OAuth2Constants.JWT).jsonContent(generateJsonWebToken(
                expiresIn,session.getContext().getRealm().getId())).sign(getSignatureContext(session));
        return jws;
    }

    private static SignatureSignerContext getSignatureContext(KeycloakSession session) {
        return new AsymmetricSignatureProvider(session, Algorithm.RS256).signer();
    }

    private static Object generateJsonWebToken(long expiresIn, String realmId) {
        JsonWebToken jwt = new JsonWebToken();
        jwt.id(KeycloakModelUtils.generateId());
        jwt.type("Bearer");
        jwt.issuer(realmId);
        jwt.audience("background-service");
        jwt.exp(expiresIn/1000);
        jwt.issuedNow();
        return jwt;
    }

    public JsonWebToken validateToken(String encodedToken, String realmId, boolean ignoreAudience){

        if (encodedToken == null) {
            throw new IdentityBrokerException("No token from server.");
        }
        session.getContext().setRealm(session.realms().getRealm(realmId));
        JsonWebToken token;
        try{
            TokenVerifier<BackgroundProcessorActionToken> verifier =
                    TokenVerifier.create(encodedToken, BackgroundProcessorActionToken.class).withChecks(
                                    TokenVerifier.IS_ACTIVE
                            )
                            .realmUrl(realmId);
            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();
            SignatureVerifierContext signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
            verifier.verifierContext(signatureVerifier);
            token = verifier.verify().getToken();
        }
        catch (Exception e){
            e.printStackTrace();
            return  null;
        }
        return token;
    }
}

package org.keycloak.services.util;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.RandomString;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class PkceUtils {
    // https://tools.ietf.org/html/rfc7636#section-4.1
    public static final int PKCE_CODE_VERIFIER_MAX_LENGTH = 128;

    private final String codeChallenge;
    private final String codeVerifier;

    public PkceUtils(String codeVerifier, String codeChallenge) {
        this.codeChallenge = codeChallenge;
        this.codeVerifier = codeVerifier;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public static PkceUtils generatePkce() {
        try {
            String codeVerifier = new RandomString(PKCE_CODE_VERIFIER_MAX_LENGTH, new SecureRandom()).nextString();
            String codeChallenge = generateS256CodeChallenge(codeVerifier);
            return new PkceUtils(codeVerifier, codeChallenge);
        } catch (Exception ex){
            throw new RuntimeException("Could not generate PKCE", ex);
        }
    }

    // https://tools.ietf.org/html/rfc7636#section-4.6
    private static String generateS256CodeChallenge(String codeVerifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(codeVerifier.getBytes(StandardCharsets.ISO_8859_1));
        return Base64Url.encode(md.digest());
    }
}

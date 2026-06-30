package org.keycloak.services.util;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.jose.jwe.JWEConstants;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.*;
import java.util.Base64;

public class RSAHelper {
    public static String decrypt(String data, String privateKey, String alg) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey rsaPrivateKey = keyFactory.generatePrivate(keySpec);

        Cipher decryptor = Cipher.getInstance(alg);
        decryptor.init(Cipher.DECRYPT_MODE, rsaPrivateKey);


        byte[] encryptedData = Base64.getDecoder().decode(data);
        byte[] decryptedData = decryptor.doFinal(encryptedData);

        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    public static String encrypt(String data, String publicKey, String alg) throws Exception{
        byte[] decodePubKeyBytes = Base64.getDecoder().decode(publicKey.getBytes(StandardCharsets.UTF_8));

        KeyFactory publicKeyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodePubKeyBytes);
        PublicKey restructed = publicKeyFactory.generatePublic(publicKeySpec);

        Cipher cipher = Cipher.getInstance(alg);
        cipher.init(Cipher.ENCRYPT_MODE, restructed);
        byte[] bytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.getEncoder().encode(bytes));
    }

    private static String getJcaAlgorithmName(String jweAlgorithmName) {
        String jcaAlgorithmName = null;
        if (JWEConstants.RSA1_5.equals(jweAlgorithmName)) {
            jcaAlgorithmName = "RSA/ECB/PKCS1Padding";
        } else if (JWEConstants.RSA_OAEP.equals(jweAlgorithmName)) {
            jcaAlgorithmName = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
        } else if (JWEConstants.RSA_OAEP_256.equals(jweAlgorithmName)) {
            jcaAlgorithmName = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
        }
        return jcaAlgorithmName;
    }
}

package com.qlikverify.backoffice.biometric;

import org.junit.Test;
import org.keycloak.common.util.SecretGenerator;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.junit.Assert.assertTrue;

public class RSATests {

    @Test
    public void encryptionDecryptionTest() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGen.initialize(2048); // initialize with key size 2048
        KeyPair keyPair = keyGen.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        byte[] privateKeyBytes = privateKey.getEncoded();

        // Encode the private key bytes in Base64
        String privateKeyString = Base64.getEncoder().encodeToString(privateKeyBytes);

        byte[] publicKeyBytes = publicKey.getEncoded();

        // Encode the private key bytes in Base64
        String publicKeyString = Base64.getEncoder().encodeToString(publicKeyBytes);

        byte[] decodePubKeyBytes = Base64.getDecoder().decode(publicKeyString.getBytes(StandardCharsets.UTF_8));

        KeyFactory publicKeyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodePubKeyBytes);
        PublicKey restructed = publicKeyFactory.generatePublic(publicKeySpec);

        assertTrue(Base64.getEncoder().encodeToString(restructed.getEncoded()).equals(publicKeyString));

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, restructed);
        String challenge = SecretGenerator.getInstance().randomString(16);
        byte[] bytes = cipher.doFinal(challenge.getBytes(StandardCharsets.UTF_8));

        String encryptedChallenge = new String(Base64.getEncoder().encode(bytes));

        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString.getBytes(StandardCharsets.UTF_8)));
        PrivateKey resustructedPrivKey = publicKeyFactory.generatePrivate(privateKeySpec);

        assertTrue(Base64.getEncoder().encodeToString(resustructedPrivKey.getEncoded()).equals(privateKeyString));
        cipher.init(Cipher.DECRYPT_MODE, resustructedPrivKey);

        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedChallenge));
        assertTrue(new String(decryptedBytes).equals(challenge));
    }
}

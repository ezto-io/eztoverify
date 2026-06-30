package com.qlikverify.backoffice.biometric;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class AShieldTests {

    @Test
    public void signTest() throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String transactionId = UUID.randomUUID().toString();
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec("2ba10-4a95a-5f3d6-8aa8dbc3f3ad111bb31bb8c1".getBytes(), "HmacSHA512"));
        byte[] hexBytes = new Hex().encode(mac.doFinal((transactionId+"038").getBytes()));
        String sign = new String(hexBytes, "UTF-8");
        assertTrue(sign != null);
    }
}

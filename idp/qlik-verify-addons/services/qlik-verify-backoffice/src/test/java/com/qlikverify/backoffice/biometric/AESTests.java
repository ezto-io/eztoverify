package com.qlikverify.backoffice.biometric;

import org.junit.Test;
import org.keycloak.utils.AESHelper;

public class AESTests {

    @Test
    public void testKey(){
        try {
            String key =  AESHelper.encrypt("test","Nf6AYeaoIJHNxYBzbBVnofW2w31zW6aQ7l7U7VoJLD0=");
            System.out.println(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //byte[] result = Base64.getDecoder().decode("XTWDqQAL46eS7VJAWK9r6k7AgL2kOZOtfRlVlQaJto8".getBytes(StandardCharsets.UTF_8));
      // String key = new String(result);

    }
}

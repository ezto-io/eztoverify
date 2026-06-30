package org.keycloak.ciba.models;

import org.keycloak.models.ClientModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.AESHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class UserData {

    private Map<String, String> metadata;
    private String userId;
    private String email;
    private String phone;
    private String phoneRegion;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoneRegion() {
        return phoneRegion;
    }

    public void setPhoneRegion(String phoneRegion) {
        this.phoneRegion = phoneRegion;
    }

    public static UserData decode(String rawData, ClientModel clientModel) throws Exception {
        return JsonSerialization.readValue(AESHelper.decrypt(new String(Base64.getDecoder().decode(rawData.getBytes(StandardCharsets.UTF_8))),clientModel.getSecret().substring(0,32)), UserData.class);
    }

    public static String encode(UserData message, ClientModel clientModel) throws Exception {
        return Base64.getEncoder().encodeToString(AESHelper.encrypt(JsonSerialization.writeValueAsString(message),clientModel.getSecret().substring(0,32)).getBytes(StandardCharsets.UTF_8));
    }
}

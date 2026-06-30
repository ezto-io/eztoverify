package com.qlikverify.backoffice.creds.device;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class TokenSecretData implements Serializable {
    private static final long serialVersionUID = 2405172041950251807L;
    private final String token;
    private final String deviceId;
    private final String deviceModel;
    private final String devicePlatform;
    private final String deviceOSVersion;
    private final String keyHashFirst6;
    private final String encryptedDeviceId;

    private String pushType;

    @JsonCreator
    public TokenSecretData(@JsonProperty("token") String token, @JsonProperty("deviceId") String deviceId, @JsonProperty("deviceModel") String deviceModel, @JsonProperty("deviceOSVersion") String deviceOSVersion,@JsonProperty("devicePlatform") String devicePlatform, @JsonProperty("keyHashFirst6") String keyHashFirst6, @JsonProperty("encyptedDeviceId") String encyptedDeviceId, @JsonProperty("pushType")String pushType) {
        this.token = token;
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.deviceOSVersion = deviceOSVersion;
        this.devicePlatform = devicePlatform;
        this.keyHashFirst6 = keyHashFirst6;
        this.encryptedDeviceId = encyptedDeviceId;
        this.pushType = (pushType != null && !pushType.isEmpty()) ? pushType: "fcm";
    }

    public String getToken() {
        return token;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public String getDeviceOSVersion() {
        return deviceOSVersion;
    }

    public String getDevicePlatform() {
        return devicePlatform;
    }
    public String getKeyHashFirst6() {
        return keyHashFirst6;
    }
    public String getEncryptedDeviceId() {
        return encryptedDeviceId;
    }

    public String getPushType() {
        return pushType;
    }

    public void setPushType(String pushType) {
        this.pushType = pushType;
    }
}

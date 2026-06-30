package com.qlikverify.backoffice.creds.biometric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BiometricSecretData {
    private final String privateKey;
    private final String deviceId;
    private final String deviceModel;
    private final String deviceOSVersion;
    private final String devicePlatform;
    private final String deviceHash;

    @JsonCreator
    public BiometricSecretData(@JsonProperty("privateKey") String privateKey, @JsonProperty("deviceId") String deviceId, @JsonProperty("deviceModel") String deviceModel, @JsonProperty("deviceOSVersion") String deviceOSVersion,@JsonProperty("devicePlatform") String devicePlatform, @JsonProperty("deviceHash") String deviceHash) {
        this.privateKey = privateKey;
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.deviceOSVersion = deviceOSVersion;
        this.devicePlatform = devicePlatform;
        this.deviceHash = deviceHash;
    }

    public String getPrivateKey() {
        return privateKey;
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

    public String getDeviceHash() {
        return deviceHash;
    }
}

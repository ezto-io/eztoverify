package org.keycloak.models.credential;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class WebAuthnMobileSecretData {

    private String osVersion;
    private String model;
    private String id;
    private String isPhysicalDevice;
    private String platform;

    private String pushSupport;

    @JsonCreator
    public WebAuthnMobileSecretData() {
    }

    @JsonCreator
    public WebAuthnMobileSecretData(String secretData) {}

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIsPhysicalDevice() {
        return isPhysicalDevice;
    }

    public void setIsPhysicalDevice(String isPhysicalDevice) {
        this.isPhysicalDevice = isPhysicalDevice;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPushSupport() {
        return pushSupport;
    }

    public void setPushSupport(String pushSupport) {
        this.pushSupport = pushSupport;
    }

    @JsonCreator
    public WebAuthnMobileSecretData(@JsonProperty("osVersion") String osVersion,
                              @JsonProperty("model") String model,
                              @JsonProperty("id") String id,
                              @JsonProperty("isPhysicalDevice") String isPhysicalDevice,
                              @JsonProperty("platform") String platform,
                              @JsonProperty("pushSupport") String pushSupport) {
        this.osVersion = osVersion;
        this.model = model;
        this.id = id;
        this.isPhysicalDevice = isPhysicalDevice;
        this.platform = platform;
        this.pushSupport = pushSupport;
    }

    @Override
    public String toString() {
        return "{ " +
                "\"osVersion\":\"" + osVersion + '\"' +
                ", \"model\":\"" + model + '\"' +
                ", \"id\":\"" + id + '\"' +
                ", \"isPhysicalDevice\":\"" + isPhysicalDevice + '\"' +
                ", \"platform\":\"" + platform + '\"' +
                ", \"pushSupport\":\"" + pushSupport + '\"' +
                " }";
    }
}

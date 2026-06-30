
package com.qlikverify.backoffice.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;



@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "appName",
    "callbackUrl",
    "deviceGoOnlineTimeout",
    "deviceId",
    "displayData",
    "pinEntryRequired",
    "pushNotificationMessage",
    "requiresUserConfirmation",
    "timeoutSeconds"
})

public class KobilTransaction {

    @JsonProperty("appName")
    private String appName;
    @JsonProperty("callbackUrl")
    private String callbackUrl;
    @JsonProperty("deviceGoOnlineTimeout")
    private Integer deviceGoOnlineTimeout;
    @JsonProperty("deviceId")
    private String deviceId;
    @JsonProperty("displayData")
    private String displayData;
    @JsonProperty("pinEntryRequired")
    private Boolean pinEntryRequired;
    @JsonProperty("pushNotificationMessage")
    private String pushNotificationMessage;
    @JsonProperty("requiresUserConfirmation")
    private Boolean requiresUserConfirmation;
    @JsonProperty("timeoutSeconds")
    private Integer timeoutSeconds;

    @JsonProperty("appName")
    public String getAppName() {
        return appName;
    }

    @JsonProperty("appName")
    public void setAppName(String appName) {
        this.appName = appName;
    }

    @JsonProperty("callbackUrl")
    public String getCallbackUrl() {
        return callbackUrl;
    }

    @JsonProperty("callbackUrl")
    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    @JsonProperty("deviceGoOnlineTimeout")
    public Integer getDeviceGoOnlineTimeout() {
        return deviceGoOnlineTimeout;
    }

    @JsonProperty("deviceGoOnlineTimeout")
    public void setDeviceGoOnlineTimeout(Integer deviceGoOnlineTimeout) {
        this.deviceGoOnlineTimeout = deviceGoOnlineTimeout;
    }

    @JsonProperty("deviceId")
    public String getDeviceId() {
        return deviceId;
    }

    @JsonProperty("deviceId")
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @JsonProperty("displayData")
    public String getDisplayData() {
        return displayData;
    }

    @JsonProperty("displayData")
    public void setDisplayData(String displayData) {
        this.displayData = displayData;
    }

    @JsonProperty("pinEntryRequired")
    public Boolean getPinEntryRequired() {
        return pinEntryRequired;
    }

    @JsonProperty("pinEntryRequired")
    public void setPinEntryRequired(Boolean pinEntryRequired) {
        this.pinEntryRequired = pinEntryRequired;
    }

    @JsonProperty("pushNotificationMessage")
    public String getPushNotificationMessage() {
        return pushNotificationMessage;
    }

    @JsonProperty("pushNotificationMessage")
    public void setPushNotificationMessage(String pushNotificationMessage) {
        this.pushNotificationMessage = pushNotificationMessage;
    }

    @JsonProperty("requiresUserConfirmation")
    public Boolean getRequiresUserConfirmation() {
        return requiresUserConfirmation;
    }

    @JsonProperty("requiresUserConfirmation")
    public void setRequiresUserConfirmation(Boolean requiresUserConfirmation) {
        this.requiresUserConfirmation = requiresUserConfirmation;
    }

    @JsonProperty("timeoutSeconds")
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @JsonProperty("timeoutSeconds")
    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

}

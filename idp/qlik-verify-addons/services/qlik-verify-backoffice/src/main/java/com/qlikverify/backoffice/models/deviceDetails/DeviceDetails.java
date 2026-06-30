package com.qlikverify.backoffice.models.deviceDetails;

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.protocol.oidc.grants.ciba.util.CibaHook;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "model",
        "isPhysicalDevice",
        "osVersion",
        "id",
        "platform",
        "pushSupport"
})
public class DeviceDetails implements Cloneable{
    public String model;
    @JsonProperty("isPhysicalDevice")
    public boolean isPhysicalDevice;
    public String osVersion;
    public String id;
    public String platform;
    public String pushSupport;

    public String generateV1DeviceHash(RequiredActionContext context) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        return CibaHook.Util.computeHmacSha256(context.getRealm().getComponentsStream(context.getRealm().getId()).filter(x -> x != null && x.get("kid") != null
                && x.get("kid").equals(context.getConfig().get("secret_key"))).findFirst().get().get("secret"), JsonSerialization.writeValueAsString(this));
    }

    public String generateV2DeviceHash(RequiredActionContext context) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        try {
            DeviceDetails deviceDetails = (DeviceDetails) this.clone();
            deviceDetails.setPushSupport(null);
            deviceDetails.setOsVersion(null);
            return CibaHook.Util.computeHmacSha256(context.getRealm().getComponentsStream(context.getRealm().getId()).filter(x -> x != null && x.get("kid") != null
                    && x.get("kid").equals(context.getConfig().get("secret_key"))).findFirst().get().get("secret"), JsonSerialization.writeValueAsString(deviceDetails));
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, String> toMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put("model", this.model);
        map.put("isPhysicalDevice", Boolean.toString(this.isPhysicalDevice));
        map.put("osVersion", this.osVersion);
        map.put("id", this.id);
        map.put("platform", this.platform);
        map.put("pushSupport", this.pushSupport);
        return map;
    }

}


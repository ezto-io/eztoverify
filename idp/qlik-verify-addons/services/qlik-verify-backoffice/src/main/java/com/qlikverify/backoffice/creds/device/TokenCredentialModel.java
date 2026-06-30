package com.qlikverify.backoffice.creds.device;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

public class TokenCredentialModel extends CredentialModel {
    public static final String TYPE = "DEVICE_TOKEN";
    private transient TokenSecretData secretData;


    private TokenCredentialModel(TokenSecretData secretData, String deviceId, String deviceModel, String deviceOSVersion, String devicePlatform, String keyHashFirst6, String encryptedDeviceId, String pushType) {
        if (deviceId == null)
            this.secretData = secretData;
        else
            this.secretData = new TokenSecretData(secretData.getToken(), deviceId, deviceModel, deviceOSVersion,devicePlatform, keyHashFirst6, encryptedDeviceId, pushType);
    }

    private TokenCredentialModel(String privateKey, String deviceId, String deviceModel,String deviceOSVersion, String devicePlatform, String keyHashFirst6, String encryptedDeviceId, String pushType) {
        secretData = new TokenSecretData(privateKey, deviceId,deviceModel,deviceOSVersion,devicePlatform, keyHashFirst6, encryptedDeviceId, pushType);
    }

    public static TokenCredentialModel create(String privateKey, String deviceId, String deviceModel,String deviceOSVersion, String devicePlatform, String keyHashFirst6, String encryptedDeviceId, String pushType) throws IOException {
        TokenCredentialModel credentialModel = new TokenCredentialModel(privateKey,deviceId,deviceModel,deviceOSVersion,devicePlatform, keyHashFirst6, encryptedDeviceId, pushType);
        credentialModel.fillCredentialModelFields();
        return credentialModel;
    }

    public static TokenCredentialModel createFromCredentialModel(CredentialModel credentialModel,String deviceId, String deviceModel,String deviceOSVersion, String devicePlatform, String keyHashFirst6, String encryptedDeviceId, String pushType) throws IOException {
        TokenSecretData secretData = JsonSerialization.readValue(credentialModel.getSecretData(), TokenSecretData.class);

        TokenCredentialModel secretQuestionCredentialModel = new TokenCredentialModel(secretData,deviceId,deviceModel,deviceOSVersion,devicePlatform, keyHashFirst6, encryptedDeviceId, pushType);
        secretQuestionCredentialModel.setUserLabel(credentialModel.getUserLabel());
        secretQuestionCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
        secretQuestionCredentialModel.setType(TYPE);
        secretQuestionCredentialModel.setId(credentialModel.getId());
        secretQuestionCredentialModel.setTemporary(credentialModel.isTemporary());
        secretQuestionCredentialModel.setSecretData(credentialModel.getSecretData());
        secretQuestionCredentialModel.setCredentialData(credentialModel.getCredentialData());
        secretQuestionCredentialModel.setLastUsed(credentialModel.getLastUsed());
        return secretQuestionCredentialModel;
    }

    private void fillCredentialModelFields() throws IOException {
        setSecretData(JsonSerialization.writeValueAsString(secretData));
        setType(TYPE);
        setCreatedDate(Time.currentTimeMillis());
        setLastUsed(Time.currentTimeMillis());
    }
}

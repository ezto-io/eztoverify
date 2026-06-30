package com.qlikverify.backoffice.creds.biometric;

import com.qlikverify.backoffice.models.deviceDetails.DeviceDetails;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.Serializable;

public class BiometricCredentialModel extends CredentialModel implements Serializable {
    public static final String TYPE = "BIOMETRIC";
    BiometricSecretData secretData;


    private BiometricCredentialModel(BiometricSecretData secretData, String deviceId, String deviceModel,String deviceOSVersion,String devicePlatform, String deviceHash) {
        if(deviceId == null)
            this.secretData = secretData;
        else {
            this.secretData = new BiometricSecretData(secretData.getPrivateKey(), deviceId, deviceModel,deviceOSVersion,devicePlatform, deviceHash);
        }
    }

    private BiometricCredentialModel(String privateKey, DeviceDetails deviceDetails, String deviceHash) {
        secretData = new BiometricSecretData(
                privateKey,
                deviceDetails.getId(),
                deviceDetails.getModel(),
                deviceDetails.getOsVersion(),
                deviceDetails.getPlatform(),
                deviceHash
        );
    }

    public static BiometricCredentialModel create(String privateKey, DeviceDetails deviceDetails, String deviceHash) {
        BiometricCredentialModel credentialModel = new BiometricCredentialModel(privateKey, deviceDetails, deviceHash);
        credentialModel.fillCredentialModelFields();
        return credentialModel;
    }

    public BiometricSecretData getBiometricSecretData() {
        return secretData;
    }

    public static BiometricCredentialModel createFromCredentialModel(CredentialModel credentialModel,DeviceDetails deviceDetails, String deviceHash)  {
        try {
            BiometricSecretData secretData = JsonSerialization.readValue(credentialModel.getSecretData(), BiometricSecretData.class);
            BiometricCredentialModel secretQuestionCredentialModel = new BiometricCredentialModel(
                    secretData,
                    deviceDetails.getId(),
                    deviceDetails.getModel(),
                    deviceDetails.getOsVersion(),
                    deviceDetails.getPlatform(),
                    deviceHash
            );

            secretQuestionCredentialModel.setUserLabel(credentialModel.getUserLabel());
            secretQuestionCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
            secretQuestionCredentialModel.setType(TYPE);
            secretQuestionCredentialModel.setId(credentialModel.getId());
            secretQuestionCredentialModel.setTemporary(credentialModel.isTemporary());
            secretQuestionCredentialModel.setSecretData(JsonSerialization.writeValueAsString(secretQuestionCredentialModel.getBiometricSecretData()));
            secretQuestionCredentialModel.setCredentialData(credentialModel.getCredentialData());
            secretQuestionCredentialModel.setEnabled(true);
            return secretQuestionCredentialModel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillCredentialModelFields() {
        try {
            setSecretData(JsonSerialization.writeValueAsString(secretData));
            setType(TYPE);
            setTemporary(true);
            setCreatedDate(Time.currentTimeMillis());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

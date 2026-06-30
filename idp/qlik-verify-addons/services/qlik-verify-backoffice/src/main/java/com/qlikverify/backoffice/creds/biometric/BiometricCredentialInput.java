package com.qlikverify.backoffice.creds.biometric;

import org.keycloak.credential.CredentialInput;

public class BiometricCredentialInput implements CredentialInput {
    private String challenge;
    private String encryptedChallenge;
    private String credentialId;
    private String deviceId;
    private String deviceHash;

    public BiometricCredentialInput(String challenge, String encryptedChallenge, String credentialId,String deviceId, String deviceHash) {
        this.challenge = challenge;
        this.encryptedChallenge = encryptedChallenge;
        this.credentialId = credentialId;
        this.deviceId = deviceId;
        this.deviceHash = deviceHash;
    }

    @Override
    public String getCredentialId() {
        return credentialId;
    }

    @Override
    public String getType() {
        return BiometricCredentialModel.TYPE;
    }

    @Override
    public String getChallengeResponse() {
        return challenge;
    }

    public String getEncryptedChallenge() {
        return encryptedChallenge;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceHash() {
        return deviceHash;
    }
}

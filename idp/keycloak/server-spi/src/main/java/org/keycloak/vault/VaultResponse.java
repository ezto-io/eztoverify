package org.keycloak.vault;

public class VaultResponse {
    private String vaultSecretId;
    private VaultRawSecret secret;

    public String getVaultSecretId() {
        return vaultSecretId;
    }

    public void setVaultSecretId(String vaultSecretId) {
        this.vaultSecretId = vaultSecretId;
    }

    public VaultRawSecret getSecret() {
        return secret;
    }

    public void setSecret(VaultRawSecret secret) {
        this.secret = secret;
    }
}

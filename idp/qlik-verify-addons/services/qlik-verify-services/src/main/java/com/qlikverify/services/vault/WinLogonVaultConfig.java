package com.qlikverify.services.vault;

public class WinLogonVaultConfig {
    private String vaultUser;
    private String vaultPass;
    private String vaultLoginMountPath;
    private String vaultUrl;
    private String vaultSecretEngineName;
    private String vaultNamespace;

    public String getVaultUser() {
        return vaultUser;
    }

    public void setVaultUser(String vaultUser) {
        this.vaultUser = vaultUser;
    }

    public String getVaultPass() {
        return vaultPass;
    }

    public void setVaultPass(String vaultPass) {
        this.vaultPass = vaultPass;
    }

    public String getVaultUrl() {
        return vaultUrl;
    }

    public void setVaultUrl(String vaultUrl) {
        this.vaultUrl = vaultUrl;
    }

    public String getVaultSecretEngineName() {
        return vaultSecretEngineName;
    }

    public void setVaultSecretEngineName(String vaultSecretEngineName) {
        this.vaultSecretEngineName = vaultSecretEngineName;
    }

    public String getVaultNamespace() {
        return vaultNamespace;
    }

    public void setVaultNamespace(String vaultNamespace) {
        this.vaultNamespace = vaultNamespace;
    }

    public String getVaultLoginMountPath() {
        return vaultLoginMountPath;
    }

    public void setVaultLoginMountPath(String vaultLoginMountPath) {
        this.vaultLoginMountPath = vaultLoginMountPath;
    }
}

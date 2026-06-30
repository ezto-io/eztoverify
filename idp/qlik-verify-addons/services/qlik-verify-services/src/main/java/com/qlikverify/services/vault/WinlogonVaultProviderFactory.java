package com.qlikverify.services.vault;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.vault.VaultProvider;
import org.keycloak.vault.VaultProviderFactory;

public class WinlogonVaultProviderFactory implements VaultProviderFactory {
    private static final Logger logger = Logger.getLogger(WinlogonVaultProviderFactory.class);

    public static final String PROVIDER_ID = "winlogon-vault";

    private String vaultUser;
    private String vaultPass;
    private String vaultLoginMountPath;
    private String vaultUrl;
    private String vaultSecretEngineName;
    private String vaultNamespace;

    public WinlogonVaultProviderFactory() {
          // NOP
    }

    @Override
    public VaultProvider create(KeycloakSession session) {
        WinLogonVaultConfig winLogonVaultConfig = new WinLogonVaultConfig();
        winLogonVaultConfig.setVaultPass(vaultPass);
        winLogonVaultConfig.setVaultUser(vaultUser);
        winLogonVaultConfig.setVaultLoginMountPath(vaultLoginMountPath);
        winLogonVaultConfig.setVaultNamespace(vaultNamespace);
        winLogonVaultConfig.setVaultUrl(vaultUrl);
        winLogonVaultConfig.setVaultSecretEngineName(vaultSecretEngineName);

        return new WinlogonVaultProvider(session.getContext().getRealm(), winLogonVaultConfig);

    }


    @Override
    public void init(Config.Scope config) {
        vaultUser = EnvironmentReader.getValue("VAULT_USER");
        vaultPass = EnvironmentReader.getValue("VAULT_PASS");
        vaultLoginMountPath = EnvironmentReader.getValue("VAULT_LOGIN_MOUNT_PATH");
        vaultUrl = EnvironmentReader.getValue("VAULT_URL");
        vaultSecretEngineName = EnvironmentReader.getValue("VAULT_ENGINE_NAME");
        vaultNamespace = EnvironmentReader.getValue("VAULT_NAMESPACE");
        logger.info("Init Hashicorp: " + vaultUrl);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


}

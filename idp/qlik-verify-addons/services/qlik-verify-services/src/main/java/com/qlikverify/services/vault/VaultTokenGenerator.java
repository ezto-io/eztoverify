package com.qlikverify.services.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import org.jboss.logging.Logger;

public class VaultTokenGenerator {
    private static final Logger logger = Logger.getLogger(VaultTokenGenerator.class);
    private static VaultTokenGenerator generator;
    private VaultConfig vaultConfig;
    private WinLogonVaultConfig winConfig;

    public static VaultTokenGenerator getInstance() {
        if (generator == null)
            generator = new VaultTokenGenerator();

        return generator;
    }

    public VaultConfig getVaultConfig(boolean reAuth, WinLogonVaultConfig winConfig) throws VaultException {
        if (vaultConfig != null && !reAuth)
            return vaultConfig;

        this.winConfig = winConfig;

        vaultConfig = createVaultConfig();
        logger.info("Vault New Token Generated");
        return vaultConfig;

    }

    private VaultConfig createVaultConfig() throws VaultException {
        VaultConfig config = null;
        config = new VaultConfig()
                .address(winConfig.getVaultUrl());
        if (winConfig.getVaultNamespace() != null && !winConfig.getVaultNamespace().isEmpty()) {
            config = config.nameSpace(winConfig.getVaultNamespace()).build();
        } else {
            config = config.build();
        }
        Vault vault = new Vault(config);
        AuthResponse authResponse = winConfig.getVaultLoginMountPath() == null ? vault.auth().loginByUserPass(winConfig.getVaultUser(), winConfig.getVaultPass()) : vault.auth().loginByUserPass(winConfig.getVaultUser(), winConfig.getVaultPass(), winConfig.getVaultLoginMountPath());
        config = config.token(authResponse.getAuthClientToken()).build();
        return config;
    }


}

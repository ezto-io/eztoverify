package com.qlikverify.services.vault;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import org.apache.commons.lang.NotImplementedException;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.vault.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class WinlogonVaultProvider implements VaultProvider {
    private static final Logger logger = Logger.getLogger(WinlogonVaultProviderFactory.class);

    private WinLogonVaultService service;
    private RealmModel realmModel;
    private ClientModel clientModel;
    private UserModel userModel;
    private String[] additionalPaths;
    private WinLogonVaultConfig vaultConfig;
    private int retryCount = 0;

    public WinlogonVaultProvider(RealmModel realmModel, WinLogonVaultConfig vaultConfig) {
        this.realmModel = realmModel;
        this.vaultConfig = vaultConfig;
    }

    private WinLogonVaultService getVaultService(boolean regenerate) {
        try {
            VaultConfig config = VaultTokenGenerator.getInstance().getVaultConfig(regenerate,vaultConfig);
            service = new WinLogonVaultService(config, vaultConfig.getVaultSecretEngineName());
            logger.info("Vault available : " + vaultConfig.getVaultUrl());
            return service;
        } catch (VaultException e) {
            if(e.getHttpStatusCode() == 403 && retryCount < 3) {
                logger.info("Retrying :"+retryCount);
                retryCount++;
                return getVaultService(true);
            }

            logger.error("Vault is not configured properly. Check the settings");
            throw new VaultNotFoundException("Vault unavailable : " + vaultConfig.getVaultUrl());
        }
    }


    @Override
    public VaultRawSecret obtainSecret(String vaultSecretId,boolean force) {
        throw new NotImplementedException();
    }

    @Override
    public VaultResponse obtainSecret(boolean force) {
        Map<String, String> secrets = null;
        try {
            secrets = getVaultService(force).getAllSecretsFromVault(realmModel, clientModel, userModel, additionalPaths);
        } catch (Exception e) {
            e.printStackTrace();
            if(e instanceof VaultException) {
                if(((VaultException)e).getHttpStatusCode() == 403)
                    return obtainSecret(true);
            }
        }
        if (secrets.size() == 0)
            return null;

        Optional<String> key = secrets.keySet().stream().findFirst();
        if (!key.isPresent())
            return null;

        Map<String, String> finalSecrets = secrets;
        VaultResponse vaultResponse = new VaultResponse();
        vaultResponse.setVaultSecretId(key.get());
        vaultResponse.setSecret(DefaultVaultRawSecret.forBuffer(Optional.of(ByteBuffer.wrap(finalSecrets.get(key.get()).getBytes(StandardCharsets.UTF_8)))));
        return vaultResponse;
    }

    @Override
    public VaultRawSecret obtainUserSecret(boolean force) {
        logger.info("Taking secret of user :" + userModel.getId());
        ByteBuffer byteBuffer = null;
        try {
            byteBuffer = getVaultService(force).getSecretFromVault(realmModel, clientModel, userModel, userModel.getId(), additionalPaths);
        } catch (Exception e) {
            e.printStackTrace();
            if(e instanceof VaultException) {
                if(((VaultException)e).getHttpStatusCode() == 403)
                    return obtainUserSecret(true);
            }
        }
        if (byteBuffer == null)
            return null;

        return DefaultVaultRawSecret.forBuffer(
                Optional.of(byteBuffer));
    }

    @Override
    public VaultProvider setAdditionalPaths(String[] paths) {
        this.additionalPaths = paths;
        return this;
    }

    @Override
    public VaultProvider setClientModel(ClientModel clientModel) {
        this.clientModel = clientModel;
        return this;
    }

    @Override
    public VaultProvider setUserModel(UserModel userModel) {
        this.userModel = userModel;
        return this;
    }

    @Override
    public boolean writeUserSecret(VaultRawSecret rawSecret, boolean force) {
        return writeUserSecret(userModel.getId(), rawSecret, force);
    }

    @Override
    public boolean writeUserSecret(String key, VaultRawSecret rawSecret, boolean force) {
        if (getVaultService(force) == null)
            return false;

        String isVaultClient = clientModel.getAttribute("vault");
        if (!Boolean.parseBoolean(isVaultClient)) {
            logger.info("Client is not a vault client");
            return false;
        }

        logger.info("Writing secret for the client : " + clientModel.getName());
        try {
            return getVaultService(force).writeToVault(realmModel, clientModel, userModel, key, rawSecret, additionalPaths);
        } catch (Exception e) {
            if(e instanceof VaultException) {
                if(((VaultException)e).getHttpStatusCode() == 403)
                    return writeUserSecret(key,rawSecret,true);
            }

            return false;
        }
    }

    @Override
    public boolean hasSecret(boolean force) {
        try {
            return getVaultService(force).hasSecret(realmModel, clientModel, userModel, additionalPaths);
        } catch (VaultException e) {
            e.printStackTrace();
            if(e.getHttpStatusCode() == 403)
                return hasSecret(true);

            return false;
        }
    }

    @Override
    public boolean removeSecret(boolean force) {
        String isVaultClient = clientModel.getAttribute("vault");
        if (!Boolean.parseBoolean(isVaultClient)) {
            logger.info("Client is not a vault client");
            return false;
        }
        logger.info("Removing secret for the client : " + clientModel.getName());
        try {
            return getVaultService(force).removeFromVault(realmModel, clientModel, userModel, additionalPaths);
        } catch (Exception e) {
            if(e instanceof VaultException) {
                if(((VaultException)e).getHttpStatusCode() == 403)
                    return removeSecret(true);
            }

            return false;
        }

    }

    @Override
    public void close() {
        // NOP
    }


}
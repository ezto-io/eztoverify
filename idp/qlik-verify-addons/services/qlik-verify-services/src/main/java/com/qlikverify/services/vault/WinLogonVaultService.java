package com.qlikverify.services.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.vault.VaultRawSecret;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WinLogonVaultService {

    private static final Logger logger = Logger.getLogger(WinLogonVaultService.class);
    private Vault vault;
    private String vaultSecretEngineName;

    public WinLogonVaultService(VaultConfig vaultConfig, String vaultSecretEngineName) {
        this.vaultSecretEngineName = vaultSecretEngineName;
        this.vault = new Vault(vaultConfig);
    }

    public boolean hasSecret(RealmModel realmModel, ClientModel clientModel, UserModel userModel, String[] paths) throws VaultException {
        Map<String, String> secrets = getAllSecretsFromVault(realmModel, clientModel, userModel, paths);
        return secrets.size() > 0;
    }

    public ByteBuffer getSecretFromVault(RealmModel realmModel, ClientModel clientModel, UserModel userModel, String key, String[] paths) throws VaultException {
        Map<String, String> secrets = getAllSecretsFromVault(realmModel, clientModel, userModel, paths);
        return ByteBuffer.wrap(secrets.get(key).getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, String> getAllSecretsFromVault(RealmModel realmModel, ClientModel clientModel, UserModel userModel, String[] paths) throws VaultException {

        if (paths == null)
            paths = new String[]{};

        String path = vaultSecretEngineName + "/" + realmModel.getName() + "/" + clientModel.getClientId();
        for (int x = 0; x < paths.length; x++) {
            path += "/" + paths[x];
        }
        if (userModel != null) {
            path += "/" + userModel.getId();
        }

        LogicalResponse logicalResponse = vault.logical().read(path);
        if (logicalResponse.getRestResponse() != null && logicalResponse.getRestResponse().getStatus() == 403)
            throw new VaultException("Unauthorized", logicalResponse.getRestResponse().getStatus());

        return logicalResponse.getData();
    }

    public boolean writeToVault(RealmModel realmModel, ClientModel clientModel, UserModel userModel, String key, VaultRawSecret secret, String[] paths) throws VaultException {
        if (!secret.get().isPresent())
            throw new VaultException("No Secret to save", 400);

        if (paths == null)
            paths = new String[]{};

        String newContent = new String(secret.get().get().array(), StandardCharsets.UTF_8);
        String path = vaultSecretEngineName + "/" + realmModel.getName() + "/" + clientModel.getClientId();
        for (int x = 0; x < paths.length; x++) {
            path += "/" + paths[x];
        }
        if (userModel != null) {
            path += "/" + userModel.getId();
        }
        return writeToVault(path, key, newContent);

    }

    public boolean removeFromVault(RealmModel realmModel, ClientModel clientModel, UserModel userModel, String[] paths) throws VaultException {

        if (paths == null)
            paths = new String[]{};

        String path = vaultSecretEngineName + "/" + realmModel.getName() + "/" + clientModel.getClientId();
        for (int x = 0; x < paths.length; x++) {
            path += "/" + paths[x];
        }
        if (userModel != null) {
            path += "/" + userModel.getId();
        }

        List<String> ids = listFromVault(path);

        String finalPath = path;
        ids.stream().forEach(x -> {
            String userPath = finalPath + "/" + x;
            try {
                removeFromVault(userPath);
            } catch (VaultException e) {
                throw new RuntimeException(e);
            }
        });

        return removeFromVault(path);
    }

    private boolean writeToVault(String path, String key, String secret) throws VaultException {
        LogicalResponse logicalResponse = vault.logical().write(path, new HashMap<String, Object>() {{
            put(key, secret);
        }});
        if (logicalResponse.getRestResponse() != null && logicalResponse.getRestResponse().getStatus() == 403)
            throw new VaultException("Unauthorized", logicalResponse.getRestResponse().getStatus());

        return logicalResponse.getRestResponse() != null && logicalResponse.getRestResponse().getStatus() == 200;
    }

    private boolean removeFromVault(String path) throws VaultException {
        LogicalResponse logicalResponse = vault.logical().delete(path);
        if (logicalResponse.getRestResponse() != null && logicalResponse.getRestResponse().getStatus() == 403)
            throw new VaultException("Unauthorized", logicalResponse.getRestResponse().getStatus());

        return logicalResponse.getRestResponse() != null && logicalResponse.getRestResponse().getStatus() == 200;
    }

    private List listFromVault(String path) throws VaultException {
        LogicalResponse logicalResponse = vault.logical().list(path);
        return logicalResponse.getListData();
    }
}
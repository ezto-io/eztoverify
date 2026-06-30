package org.keycloak.avatar;

import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.avatar.utils.MinioConfig;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class MinioAvatarStorageProviderFactory implements  AvatarProviderFactory{


    //TODO remove default settings
    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_URL);
    private static final String DEFAULT_ACCESS_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_ACCESS_KEY);
    private static final String DEFAULT_SECRET_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_SECRET_KEY);

    private MinioConfig minioConfig;

    @Override
    public AvatarProvider create(KeycloakSession session) {
        return new MinioAvatarStorageProvider(minioConfig);
    }

    @Override
    public void init(Config.Scope config) {
        String serverUrl = config.get("server-url", DEFAULT_SERVER_URL);
        String accessKey = config.get("access-key", DEFAULT_ACCESS_KEY);
        String secretKey = config.get("secret-key", DEFAULT_SECRET_KEY);

        this.minioConfig = new MinioConfig(serverUrl, accessKey, secretKey);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "avatar-storage-minio";
    }
}

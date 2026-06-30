package org.keycloak.avatar.utils;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;

import org.jboss.logging.Logger;


public class MinioTemplate {
    private static final Logger logger = Logger.getLogger(MinioTemplate.class);

    private final MinioConfig minioConfig;

    public MinioTemplate(MinioConfig minioConfig) {
        this.minioConfig = minioConfig;
    }

    public <T> T execute(MinioCallback<T> callback) {

        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioConfig.getServerUrl())
                    .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                    .build();
            minioClient.ignoreCertCheck();
            return callback.doInMinio(minioClient);
        } catch (Exception mex) {
            throw new RuntimeException(mex);
        }
    }

    public void ensureBucketExists(String bucketName) {

        execute(minioClient -> {

            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (exists) {
                logger.debugf("Bucket: %s already exists", bucketName);
            } else {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            return null;
        });
    }

    public boolean checkBucketExists(String bucketName) {
        return execute(minioClient -> {
            boolean isExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            return isExists;
        });
    }


    public String getBucketName(String realmName) {
        return realmName + minioConfig.getDefaultBucketSuffix();
    }
}

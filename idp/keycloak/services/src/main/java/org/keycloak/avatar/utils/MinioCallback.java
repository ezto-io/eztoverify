package org.keycloak.avatar.utils;

import io.minio.MinioClient;

public interface MinioCallback<T> {
    T doInMinio(MinioClient minioClient) throws Exception;
}

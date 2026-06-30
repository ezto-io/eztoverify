package org.keycloak.avatar;

import com.google.common.collect.Iterables;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import org.apache.commons.io.FilenameUtils;
import org.jboss.logging.Logger;
import org.keycloak.AvatarConstants;
import org.keycloak.avatar.utils.MinioConfig;
import org.keycloak.avatar.utils.MinioTemplate;

import java.io.InputStream;

public class MinioAvatarStorageProvider implements AvatarProvider {

    private final MinioTemplate minioTemplate;

    public MinioAvatarStorageProvider(MinioConfig minioConfig) {
        this.minioTemplate = new MinioTemplate(minioConfig);
    }

    protected static final Logger logger = Logger.getLogger(MinioAvatarStorageProvider.class);

    //helps to load the avatar for the user from Minio
    @Override
    public InputStream loadAvatarImage(String realmName, String userId) {

        String bucketName = minioTemplate.getBucketName(realmName);
        boolean hasFile = minioTemplate.execute(minioClient -> {
            try {
                StatObjectResponse statObjectResponse = minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(userId).build());
                return statObjectResponse != null && statObjectResponse.size() > 0;
            } catch (Exception er) {
                return false;
            }
        });

        if (!hasFile)
            return null;

        return minioTemplate.execute(minioClient -> {
                    try {
                        return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(userId).build());
                    } catch (Exception e) {
                        return null;
                    }
                }
        );
    }

    //helps to Save the avatar of the user to Minio
    @Override
    public void saveAvatarImage(String realmName, String userId, InputStream input) {
        String bucketName = minioTemplate.getBucketName(realmName);
        minioTemplate.ensureBucketExists(bucketName);

        minioTemplate.execute(minioClient -> {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(userId).stream(
                                    input, -1, 10485760)
                            .contentType("image/png")
                            .build());
            return null;
        });

    }

    //helps to Remove the avatar of the user from Minio
    @Override
    public boolean removeAvatarImage(String realmId, String userId) {
        String bucketName = minioTemplate.getBucketName(realmId);

        return minioTemplate.execute(minioClient -> {
                    try {
                        minioClient.removeObject(
                                RemoveObjectArgs.builder().bucket(bucketName).object(userId).build());
                    } catch (ErrorResponseException e) {
                        return false;
                    }
                    return true;
                }
        );
    }

    //helps to remove the Realm Data from Minio
    @Override
    public boolean removeRealmData(String realmId) {
        String bucketName = minioTemplate.getBucketName(realmId);

        return minioTemplate.execute(minioClient -> {
                    try {
                        //Removes All Objects from the bucket
                        Iterable<Result<Item>> realmObjects = minioClient.listObjects(
                                ListObjectsArgs.builder().bucket(bucketName).build());
                        for (Result<Item> result : realmObjects) {
                            Item item = result.get();
                            if (item != null && item.objectName() != null) {
                                minioClient.removeObject(
                                        RemoveObjectArgs.builder().bucket(bucketName).object(item.objectName()).build()
                                );
                            }
                        }
                        //After deletion of all objects, we delete the bucket
                        minioClient.removeBucket(
                                RemoveBucketArgs.builder().bucket(bucketName).build());
                    } catch (ErrorResponseException e) {
                        return false;
                    }
                    return true;
                }
        );
    }

    //helps to Save the Email Template to Minio
    @Override
    public void saveEmailTemplate(String realmName, String templateName, InputStream input) {
        //Ensures bucket exists or create new bucket
        String bucketName = minioTemplate.getBucketName(realmName);
        minioTemplate.ensureBucketExists(bucketName);

        //Make Template Name Readable
        String readableTempName = templateName + ".html";

        //upload Email Template
        minioTemplate.execute(minioClient -> {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(readableTempName).stream(
                                    input, -1, 10485760)
                            .contentType("web/html")
                            .build());
            return null;
        });
    }

    //helps to load the Email Template from Minio
    @Override
    public InputStream loadEmailTemplate(String realmName, String templateName) {
        //Ensures bucket exists or create new bucket
        String bucketName = minioTemplate.getBucketName(realmName);

        //Make Template Name Readable
        String readableTempName = templateName + ".html";

        //Retrieve Email Template
        return minioTemplate.execute(minioClient -> {
                    try {
                        return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(readableTempName).build());
                    } catch (ErrorResponseException e) {
                        return null;
                    }
                }
        );
    }

    // Save the Image in Minio and Return URL
    @Override
    public String saveImgAndReturnUrl(String realmName, String objectPath, String fileName, String fileContentType, InputStream input) {

        String bucketName = minioTemplate.getBucketName(realmName);
        String objectName = objectPath + fileName;

        minioTemplate.execute(minioClient -> {
            //check BucketPolicy presents or not
            String bucketPolicy = minioClient.getBucketPolicy(GetBucketPolicyArgs.builder().bucket(bucketName).build());

            //if not set Prefix_Policy for public access
            if (bucketPolicy == null || bucketPolicy.isEmpty())
                setPrefixPolicy(minioClient, bucketName);

            //delete the object presents with same name irrespective of extension
            deleteObjectIfPresents(minioClient, bucketName, objectPath, objectName);

            //and upload the new image here
            return minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(objectName)
                            .stream(input, -1, 10485760)
                            .contentType(fileContentType)
                            .build());

        });
        return bucketName + "/" + objectName;
    }

    @Override
    public boolean removeBrandData(String realmName, String brandName) {
        String bucketName = minioTemplate.getBucketName(realmName);
        String objectPath = "public/" + brandName + "/";

        return minioTemplate.execute(minioClient -> {
            try {
                //getting list of objects in specified objectPath
                Iterable<Result<Item>> brandObjects = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix(objectPath).build());
                //Removes the branding data
                for (Result<Item> result : brandObjects) {
                    Item item = result.get();
                    if (item != null && item.objectName() != null) {
                        minioClient.removeObject(
                                RemoveObjectArgs.builder().bucket(bucketName).object(item.objectName()).build()
                        );
                    }
                }
            } catch (ErrorResponseException e) {
                return false;
            }
            return true;
        });
    }

    @Override
    public GetObjectResponse getObject(String objectPath, String realmName) {
        String bucketName = minioTemplate.getBucketName(realmName);
        String replacedString = objectPath.replace(bucketName + "/", "");
        return minioTemplate.execute(minioClient -> {
            GetObjectResponse output = null;
            try{
                output = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(replacedString).build());
            } catch (Exception e){
                logger.error(e.getMessage());
            }
            return output;
        });
    }

    @Override
    public void saveFile(String realmName, String templateName, InputStream inputStream) {
        String bucketName = minioTemplate.getBucketName(realmName);
        minioTemplate.ensureBucketExists(bucketName);

        minioTemplate.execute(minioClient -> {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(templateName).stream(
                                    inputStream, -1, 10485760)
                            .build());
            return null;
        });
    }

    @Override
    public InputStream loadFile(String realmName, String templateName) {
        //Ensures bucket exists or create new bucket
        String bucketName = minioTemplate.getBucketName(realmName);

        return minioTemplate.execute(minioClient -> {
                    try {
                        return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(templateName).build());
                    } catch (ErrorResponseException e) {
                        return null;
                    }
                }
        );
    }


    //Helper Functions
    //used for setting PrefixPolicy of bucket
    private static void setPrefixPolicy(MinioClient minioClient, String bucketName) {

        String policyJson = (AvatarConstants.PREFIX_POLICY_JSON).replace("BUCKET_NAME", bucketName);

        try {
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder().bucket(bucketName).config(policyJson).build());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    //used to delete the object in given path
    private static void deleteObjectIfPresents(MinioClient minioClient, String bucketName, String objectPath, String objectName) {
        String baseFileName = FilenameUtils.getBaseName(objectName);
        try {
            //List objects in the given objectPath,then remove it if presents
            Iterable<Result<Item>> realmObjects = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).prefix(objectPath).build());

            if (!Iterables.isEmpty(realmObjects)) {
                for (Result<Item> result : realmObjects) {
                    Item item = result.get();
                    if (item != null && item.objectName() != null && FilenameUtils.getBaseName(item.objectName()).startsWith(baseFileName)) {
                        removeObject(minioClient, bucketName, item.objectName());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    //used for checking object present or not
    private static boolean checkIfObjectPresents(MinioClient minioClient, String bucketName, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //used to delete the object from bucket
    private static void removeObject(MinioClient minioClient, String bucketName, String objectName) {

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
        }
    }

    @Override
    public void close() {

    }
}

package org.keycloak;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class AvatarConstants {

    public static final String DEFAULT_USER_AVATAR = "iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAABmJLR0QA/wD/AP+gvaeTAAAFUklEQVRoge1ZTWgUZxh+3tlJosGiqKkUtVZMW4s0aGbGihdJrYX+QPFQAtqWHoRaTDD00h9QwYbS9iDUGkEEDxJbG3sRbKFW7aWEzeabXXMIxaYI2rQ9aFrFJeZnZ54editxfna/2SxCMQ/s5f3en+eZ/Wa+d94B5jCHhxtSiyR9fX2p5ubmjSTbfN+3RGQtgMcALCi55AH8SfKKYRgugEutra0ZEfFnW3tWAjKZzErDMPYAeAPA8oThowB6C4VCz6ZNm0ar5VCVgGw22+R5XreIvA2gvtriJUwBOAFgn23bN5MGJxaglNoB4EsAi5PGVsAYyQ7HcU4nCdIWoJSqA3AUwK6kzJJARI6R7LRte1rLX8dJKdUoIt+SfEmTx7iIXCD5m4gIgDUkXwDQqBn/PYDXbdser+RYUYBSqk5EzmqSJ4BDqVSqe8OGDbdmLuRyuUW+7+8n2aVTF8B3ALZX+idMjURHdcmLyJuWZZ2KWiwJes913SzJk6gs4hUAhwG8W87JKLfouu5O6O/5Q3HkZ8KyrF6SX2jm3O26bns5h1gB2Wy2ieRhzULjqVSqW9MXDQ0NBwHc1fEleWRgYGBJ3HqsAM/zuqH5qCR5Mbjny6GlpeUfAJc03ZemUqmDcYuRAtLp9IrSIaUFEbmi6zsDSWJ2KaUej1qIFGCaZgeSnbA16anKoB4xN3NIQF9fXwrF3kYbJJ+qgtTTCf13kgzxDRmam5s3ImFjJiJbc7ncIl3//v7+xQCeT1IDwEqllB00hgSQbEuYGAAafd/fr+tcX19/AMD8pEVEJCQ6JMD3fStpYgAg2eW6bsWtNzg4+BaAzmpqAAhxC53EIpJ0b94LJXnSdd3Wurq6j0uPynvo7+9fXLrynaj+pg9xCyVSSo1h9q3yXQAXRWQEAEg+CWArqtg2Ady0bbtppiGqF1oQYUuK+QBeJVmDVPfhkaChbC/0f0CUgPwDZ6GPO0FDlIC/HgCRahHiFiXg1wdApFqE+qeom3gQwPaEiSdIZkREAXBJ/uJ53g3f98cAwDCMJalUqklEnhERuzQ7eg5AQ8I6qqIAEflJ8+kxDeA8yT4ROes4zu0yvqOlXw7AVwCglFpI8jURaQewDUBdpYK+74da8NA5QNJwXfcagBVxeUTkOMlPbNu+XqmoDrLZ7CrP8z4SkV2IfzJetyxrdXCaF3IuOfTGJLkFYItlWbtrRR4AWltbrzmO8w6ALaUaUTgVNYqMVFsoFHpQnJgF8Zlt2z9XzbQCSrk/j1iaNE2zJyomUkBpVnkiaCc5OSuGGiA5EbSJyPH169f/EeVf7iTeB2AskGhvOp1eNjuK8Uin08tEZG/AfGNycvJAXEysANu2b5LsCJhXmaZ5fmho6NHZEI1COp1eZprmjwBWBZb2bN68+e+4uLK9kOM4p0XkWMDcMj09nVZKPVsl1xBc120xTTMN4L6cJHts2z5TLrZiM0eyE8VZ5UysBpBRSr0/PDxc9Xh9ZGSkQSn1IckMgCcCy+fy+XxXpRzaw10AZwC8HLF8VUQ+nTdv3tfr1q3TagSHh4cXTExM7CD5AYoXI4hzANprMtz9D6Xx+mEAu2Nc7gD4geQFwzCGTNO82tjYeBsAxsfHF3qet8bzvBbDMLaRfBERvT1Q3Db5fL6rra2toMMr8aud67rtJI8AWJo0tgJuANhTac8HkfiFxrKsbzzPW4vix45anAuTInJkampqbVLywCwnapcvX15eKBQ6AOwEsDJh+O8Aek3T7Ik7pHRQk5EgSUMpZZfmNhaK04PluP8z6yiK7xrK9/1LjuO4tfjMOoc5POz4Fwco8+4EM7MnAAAAAElFTkSuQmCC";

    public static final String MINIO_SERVER_URL = "MINIO_SERVER_URL";
    public static final String MINIO_SERVER_API_URL = "MINIO_SERVER_API_URL";
    public static final String MINIO_ACCESS_KEY = "MINIO_ACCESS_KEY";
    public static final String MINIO_SECRET_KEY = "MINIO_SECRET_KEY";
    public static final String MINIO_PUBLIC_SERVER_URL = "MINIO_PUBLIC_URL";
    public static HashMap<String,String> tids = new HashMap<>();
    public static AtomicReference<Map<String, CompletableFuture<Void>>> adThreadStorage = new AtomicReference<Map<String,CompletableFuture<Void>>>();

    //Bucket for Uploading MarketPlace Related Files on StartUp
    public static final String DEFAULT_MARKETPLACE_FILES = "default-marketplace-files";

    //Bucket Policy JSON
    public static final String policyJson =
            "{\n" +
                    "     \"Statement\": [\n" +
                    "         {\n" +
                    "             \"Action\": [\n" +
                    "                 \"s3:GetBucketLocation\",\n" +
                    "                 \"s3:ListBucket\"\n" +
                    "             ],\n" +
                    "             \"Effect\": \"Allow\",\n" +
                    "             \"Principal\": \"*\",\n" +
                    "             \"Resource\": \"arn:aws:s3:::"+"BUCKET_NAME"+"\"\n" +
                    "         },\n" +
                    "         {\n" +
                    "             \"Action\": [\n" +
                    "                 \"s3:GetObject\",\n" +
                    "                 \"s3:PutObject\"\n" +
                    "             ],\n" +
                    "             \"Effect\": \"Allow\",\n" +
                    "             \"Principal\": \"*\",\n" +
                    "             \"Resource\": \"arn:aws:s3:::"+"BUCKET_NAME"+"/*\"\n" +
                    "         }\n" +
                    "     ],\n" +
                    "     \"Version\": \"2012-10-17\"\n" +
                    " }";
    //Access Rule Policy for public prefix
    public static final String PREFIX_POLICY_JSON =
            "{\n" +
                    "     \"Statement\": [\n" +
                    "         {\n" +
                    "             \"Action\": [\n" +
                    "                 \"s3:GetBucketLocation\",\n" +
                    "                 \"s3:ListBucket\"\n" +
                    "             ],\n" +
                    "             \"Effect\": \"Allow\",\n" +
                    "             \"Principal\": \"*\",\n" +
                    "             \"Resource\": \"arn:aws:s3:::"+"BUCKET_NAME"+"\"\n" +
                    "         },\n" +
                    "         {\n" +
                    "             \"Action\":\"s3:GetObject\",\n"  +
                    "             \"Effect\": \"Allow\",\n" +
                    "             \"Principal\": \"*\",\n" +
                    "             \"Resource\": \"arn:aws:s3:::"+"BUCKET_NAME"+"/public/*\"\n" +
                    "         }\n" +
                    "     ],\n" +
                    "     \"Version\": \"2012-10-17\"\n" +
                    " }";
}

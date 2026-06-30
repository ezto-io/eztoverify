package com.qlikverify.backoffice.liveness;

import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.FormPartValue;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.qlikverify.backoffice.helpers.selfieRegistrationHelper.SelfieRegistrationHelper;
import com.qlikverify.extensions.admin.controllers.actionToken.EztoActionTokenVerifier;
import com.qlikverify.extensions.admin.misc.helpers.JsonUtil;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.TrxModel;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.liveness.LivenessConstants.*;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

public class LivenessHelper {

    private static final Logger logger = Logger.getLogger(LivenessHelper.class);
    private static final Random random = new Random();
    private static final SelfieRegistrationHelper selfieRegistrationHelper = new SelfieRegistrationHelper();

    private static ArrayList<String> randomStrings = new ArrayList<>();

    static {
        randomStrings.add("Dog");
        randomStrings.add("Cat");
        randomStrings.add("House");
        randomStrings.add("Car");
        randomStrings.add("Tree");
        randomStrings.add("Sun");
        randomStrings.add("Moon");
        randomStrings.add("Book");
        randomStrings.add("Rain");
        randomStrings.add("Smile");
        randomStrings.add("Hat");
        randomStrings.add("Ball");
        randomStrings.add("Friend");
        randomStrings.add("Water");
        randomStrings.add("Bird");
    }

    public static List<String> getRandomSentences() {
        return randomStrings;
    }

    private Map<String, Object> expectedValues(RequiredActionContext context, String livenessStep) {
        Map<String, Object> expectedValues = new HashMap<>();
        switch (livenessStep) {
            case LIVENESS_MOVEMENT:
                expectedValues.put("enabled", Boolean.parseBoolean(context.getConfig().get(livenessStep)));
                return expectedValues;

            case LIVENESS_FINGER_COUNT:

            case LIVENESS_BLINK:
                expectedValues.put("enabled", Boolean.parseBoolean(context.getConfig().get(livenessStep)));
                expectedValues.put("expected", getRandomNumber());
                return expectedValues;

            case LIVENESS_SPEECH:
                expectedValues.put("enabled", Boolean.parseBoolean(context.getConfig().get(livenessStep)));
                expectedValues.put("expected", getRandomString());
                return expectedValues;
        }
        return expectedValues;
    }

    private int getRandomNumber() {
        return random.nextInt(3) + 2;
    }

    private String getRandomString() {
        String randomSentence = getRandomSentences().get(random.nextInt(14));
        return randomSentence;
    }

    public Map<String, Object> processConfig(RequiredActionContext context) {
        HashMap<String, Object> configJson = new HashMap<>();
        context.getConfig().keySet().stream().filter(x -> !x.equalsIgnoreCase(LIVENESS_FACE_COMPARE))
                .forEach(x -> configJson.put(x.split("-")[1], expectedValues(context, x)));
        context.getAuthenticationSession().setAuthNote("expected_liveness", JsonUtil.getAsJsonString(configJson));
        return configJson;
    }

    public Map<String, String> sendDataToLiveness(KeycloakSession session, RequiredActionContext context, String filePath, Map<String, Object> livenessMetadata, Map<String, Object> expected) {
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

            Long actionTokenExpiry = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);

            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("file_id", filePath);
            requestBodyMap.put("bucket_name", context.getRealm().getName());
            requestBodyMap.put("metadata", livenessMetadata);
            requestBodyMap.put("expected", expected);
            requestBodyMap.put("action_token", EztoActionTokenVerifier.generateToken(session, actionTokenExpiry, context.getRequestId(), "liveness"));

            String trxId = context.getRequestId();
            TrxModel trx = context.getRealm().getTrx(trxId);
            if (trx != null && trx.getAttributes() != null
                    && trx.getAttributes().containsKey(T1_REQUEST_ID)
                    && trx.getAttributes().containsKey(WEB_TO_DEVICE)
                    && Boolean.parseBoolean(trx.getAttributes().get(WEB_TO_DEVICE))) {
                trxId = trx.getAttributes().get(T1_REQUEST_ID);
            }

            requestBodyMap.put("hook_url", session.getContext().getAuthServerUrl() + "realms/api/liveness/" + context.getRealm().getId() + "/" + trxId + "/verification");
            if (Boolean.parseBoolean(context.getConfig().get(LIVENESS_FACE_COMPARE)) && context.getUser().getFirstAttribute(FACE_TEMPLATE) != null) {
                requestBodyMap.put(FACE_TEMPLATE, context.getUser().getFirstAttribute(FACE_TEMPLATE));
            }

            okhttp3.MediaType mimeType = okhttp3.MediaType.parse("application/json" + "; charset=utf-8");
            RequestBody requestBody = RequestBody.create(mimeType, JsonSerialization.writeValueAsString(requestBodyMap));

//            HashMap<String, Object> traits = new HashMap<>();
//            traits.put("event", TwilioSegmentEvent.LV_BACKEND_REQUEST_START.name());
//            traits.put("request_data", JsonSerialization.writeValueAsString(requestBodyMap));
//            traits.put("metadata", JsonSerialization.writeValueAsString(livenessMetadata));
//            traits.put("message", "Liveness backend request start");
//
//            TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), traits);


            Request request = new Request.Builder()
                    .url(EnvironmentReader.getValue("LIVELINESS_URL") + "/live/all")
                    .method("POST", requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = okHttpClient.newCall(request).execute();

            logger.info("------------External Request-----------------");
            logger.info(requestBodyMap);
            logger.info("------------External Request-----------------");

            if (response.code() == 500) {
//                HashMap<String, Object> error = new HashMap<>();
//                error.put("event", TwilioSegmentEvent.LV_BACKEND_REQUEST_END_ERROR.name());
//                error.put("message", "Liveness backend request error");

                String responseBody;
                if (response.body() != null) {
                    responseBody = response.body().string();
                } else {
                    responseBody = "LIVENESS FAILED DUE TO SOME ERRORS";
                }

//                error.put("response_data", responseBody);
//                TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), error);
                logger.error("LIVENESS FAILED:  " + response.code() + "::" + responseBody);
                return null;
            }
            if (!response.isSuccessful()) {
//                HashMap<String, Object> error = new HashMap<>();
//                error.put("event", TwilioSegmentEvent.LV_BACKEND_REQUEST_END_ERROR.name());
//                error.put("message", "Liveness backend request error");

                String responseBody;
                if (response.body() != null) {
                    responseBody = response.body().string();
                } else {
                    responseBody = "LIVENESS FAILED DUE TO SOME ERRORS";
                }

                logger.error("LIVENESS FAILED:  " + response.code() + "::" + responseBody);
//                error.put("response_data", responseBody);
//                TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), error);
                return null;
            }
            String resBody = response.body().string();
            logger.info("------------External Response-----------------");
            logger.info(response.code());
            logger.info("------------External Response-----------------");
//            HashMap<String, Object> respTraits = new HashMap<>();
//            respTraits.put("event", TwilioSegmentEvent.LV_BACKEND_REQUEST_END.name());
//            respTraits.put("message", "Liveness backend request error");
//            respTraits.put("response_data", resBody);
//            TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), respTraits);
            return JsonSerialization.readValue(resBody, Map.class);

        } catch (IOException e) {
//            HashMap<String, Object> respTraits = new HashMap<>();
//            respTraits.put("event", TwilioSegmentEvent.LV_BACKEND_FAILURE.name());
//            respTraits.put("message", "Liveness backend request error");
//            respTraits.put("error", e.getMessage() );
//            TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), respTraits);
            throw new RuntimeException(e);
        }
    }


    public static Map<String, String> compareTwoBase64(String firstImg, String secondImg) {
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("face_image", firstImg.split(",")[1]);
            requestBodyMap.put("face_image_to_compare", secondImg.split(",")[1]);
            requestBodyMap.put("anti_spoofing", false);

            okhttp3.MediaType mimeType = okhttp3.MediaType.parse("application/json" + "; charset=utf-8");
            RequestBody requestBody = RequestBody.create(mimeType, JsonSerialization.writeValueAsString(requestBodyMap));

            Request request = new Request.Builder()
                    .url(EnvironmentReader.getValue("LIVELINESS_URL") + "/live/face/compare/images")
                    .method("POST", requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = okHttpClient.newCall(request).execute();

            logger.info("------------External Request-----------------");
            logger.info(EnvironmentReader.getValue("LIVELINESS_URL") + "/live/face/compare/images");
            logger.info("------------External Request-----------------");

            if (response.code() == 500) {

                String responseBody;
                if (response.body() != null) {
                    responseBody = response.body().string();
                } else {
                    responseBody = "LIVENESS FAILED DUE TO SOME ERRORS";
                }

                logger.error("LIVENESS FAILED:  " + response.code() + "::" + responseBody);
                return null;
            }
            if (!response.isSuccessful()) {
                String responseBody;
                if (response.body() != null) {
                    responseBody = response.body().string();
                } else {
                    responseBody = "LIVENESS FAILED DUE TO SOME ERRORS";
                }

                logger.error("LIVENESS FAILED:  " + response.code() + "::" + responseBody);
                return null;
            }
            String resBody = response.body().string();
            logger.info("------------External Response-----------------");
            logger.info(response.code());
            logger.info("------------External Response-----------------");
            return JsonSerialization.readValue(resBody, Map.class);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String uploadToMinio(RequiredActionContext context, File mediaTempFile) {
        String bucketName = context.getRealm().getName();
        String objectName = context.getUser().getId() + "/" + context.getRequestId() + "/" + mediaTempFile.getName();

        MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_SERVER_URL)
                .credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();

        try {
            minioClient.ignoreCertCheck();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

//            HashMap<String, Object> minioStart = new HashMap<>();
//            minioStart.put("event", TwilioSegmentEvent.LV_MINIO_UPLOAD_START.name());
//            minioStart.put("message", "Liveness Minio upload start");

//            TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), minioStart);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new FileInputStream(mediaTempFile), mediaTempFile.length(), -1)
                            .contentType("video/x-matroska")
                            .build());

//            HashMap<String, Object> minioEnd = new HashMap<>();
//            minioEnd.put("event", TwilioSegmentEvent.LV_MINIO_UPLOAD_END.name());
//            minioEnd.put("message", "Liveness Minio upload end");
//            TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), minioEnd);

            return objectName;
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException
                 | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException
                 | XmlParserException e) {
//            HashMap<String, Object> minioEnd = new HashMap<>();
//            minioEnd.put("event", TwilioSegmentEvent.LV_MINIO_UPLOAD_ERROR.name());
//            minioEnd.put("message", "Liveness Minio upload error");
//            TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), minioEnd);
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
//            HashMap<String, Object> minioEnd = new HashMap<>();
//            minioEnd.put("event", TwilioSegmentEvent.LV_MINIO_UPLOAD_ERROR.name());
//            minioEnd.put("message", "Liveness Minio key exception");
//            TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), minioEnd);
            throw new IllegalStateException(e.getMessage());
        }
    }

    public MultivaluedMap<String, FormPartValue> getMultiPartFormParameters(RequiredActionContext context, MediaType mediaType, HttpHeaders headers, InputStream inputStream) throws IOException {
        try {

            if (!new MediaType("multipart", "form-data").isCompatible(mediaType) || !mediaType.getParameters().containsKey("boundary")) {
                return new MultivaluedHashMap<>();
            }

            Providers providers = ResteasyContext.getContextData(Providers.class);
            MessageBodyReader<MultipartFormDataInput> multiPartProvider = providers.getMessageBodyReader(
                    MultipartFormDataInput.class, null, null, MULTIPART_FORM_DATA_TYPE);
            MultipartFormDataInput inputs = multiPartProvider
                    .readFrom(null, null, null, mediaType, headers.getRequestHeaders(),
                            inputStream);
            MultivaluedHashMap<String, FormPartValue> parts = new MultivaluedHashMap<>();

            for (Map.Entry<String, List<InputPart>> entry : inputs.getFormDataMap().entrySet()) {
                for (InputPart value : entry.getValue()) {
                    javax.ws.rs.core.MediaType valueMediaType = value.getMediaType();

                    if (TEXT_PLAIN_TYPE.isCompatible(valueMediaType)) {
                        parts.add(entry.getKey(), new FormPartValue(value.getBodyAsString()));
                    } else {
                        parts.add(entry.getKey(), new FormPartValue(value.getBody(InputStream.class, null)));
                    }
                }
            }

            return parts;
        } catch (IOException cause) {
            GeneralHelper.auditEvent(context, EventType.EZ_LIVENESS_VERIFICATION_ERROR, ErrorMappings.CONTACT_ADMIN);
            throw new IOException("Failed to parse multi part request", cause);
        }
    }

    public static void updateLivenessAttributes(RealmModel realm, String trx_id, Map<String, Object> results) {
        results.keySet().stream().forEach(x -> {
            Map<String, Object> livenessSteps = (Map<String, Object>) results.get(x);
            realm.addAttribute(trx_id, "sp@liveness-" + x + "-verified", String.valueOf(livenessSteps.get("success")));
        });
    }

    public boolean uploadSelfieImage(RequiredActionContext context) throws IOException {
        MultivaluedMap<String, String> formData;

        try {
            formData = context.getHttpRequest().getDecodedFormParameters();
            String imageDetails = formData.get(LIVELINESS_IMAGE_DETAILS).get(0);
            if (formData.get(LIVELINESS_IMAGE) != null && Boolean.parseBoolean(formData.getFirst(LIVELINESS_IMAGE))) {
                if (formData.containsKey(LIVELINESS_IMAGE_DETAILS) && formData.get(LIVELINESS_IMAGE_DETAILS) != null) {
                    Map<String, String> imageDetailsMap = JsonSerialization.readValue(imageDetails, Map.class);
                    File file = GeneralHelper.createTempFile(imageDetailsMap.get("front"));
                    Map<String, Object> faceTemplateResponse = selfieRegistrationHelper.uploadImage(file, context, context.getSession());
                    if (faceTemplateResponse != null && faceTemplateResponse.containsKey("success") && Boolean.parseBoolean(faceTemplateResponse.get("success").toString())) {
                        context.getUser().setSingleAttribute(FACE_TEMPLATE, String.valueOf(faceTemplateResponse.get("template")));
                        context.success();
                        return true;
                    } else {
                        GeneralHelper.auditEvent(context, EventType.EZ_LIVENESS_VERIFICATION_ERROR, faceTemplateResponse != null && faceTemplateResponse.containsKey("message") ? String.valueOf(faceTemplateResponse.get("message")) : "Couldn't register the face");
                        context.getRealm().addAttribute(context.getRequestId(), LIVENESS_FAILURE_REASON, faceTemplateResponse != null && faceTemplateResponse.containsKey("message") ? String.valueOf(faceTemplateResponse.get("message")) : "Couldn't register the face");
                        context.failure();
                        return false;
                    }
                } else {
                    LoginFormsProvider loginFormsProvider = context.form();
                    context.challenge(loginFormsProvider.createForm(BACKOFFICE_ERROR_FTL));
                    return false;
                }
            }
        } catch (IOException e) {
            GeneralHelper.auditEvent(context, EventType.EZ_LIVENESS_VERIFICATION_ERROR, ErrorMappings.CONTACT_ADMIN);
            throw new IOException(e);
        }
        return true;
    }

    public boolean processFaceTemplate(RequiredActionContext context) {
        MultivaluedMap<String, String> formData;
        if (context.getHttpRequest().getHttpHeaders().getMediaType().equals(org.keycloak.utils.MediaType.APPLICATION_FORM_URLENCODED_TYPE) && context.getHttpRequest().getDecodedFormParameters().get("liveliness_image") != null) {
            try {
                return uploadSelfieImage(context);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
        return false;
    }

    public static boolean isFormUrlEncoded(MediaType mediaType) {
        return new MediaType("application", "x-www-form-urlencoded").isCompatible(mediaType);
    }

    public static boolean isMultipartFormData(MediaType mediaType) {
        return new MediaType("multipart", "form-data").isCompatible(mediaType);
    }
}

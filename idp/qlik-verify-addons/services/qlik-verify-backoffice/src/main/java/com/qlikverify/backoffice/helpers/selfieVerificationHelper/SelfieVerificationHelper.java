package com.qlikverify.backoffice.helpers.selfieVerificationHelper;

import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.qlikverify.backoffice.helpers.selfieRegistrationHelper.SelfieRegistrationHelper;
import com.qlikverify.backoffice.helpers.templateConfigParser.ConfigTemplateParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SelfieVerificationHelper {

    private static HashMap<String, String> dataToBeSent;
    private static final Logger logger = Logger.getLogger(SelfieVerificationHelper.class);

    public Map<String, Object> uploadImage(File image, RequiredActionContext context, KeycloakSession session) throws IOException {

        String imageData = GeneralHelper.putObjectToMinioForLiveness(image, "photo", context, session);
        dataToBeSent = new HashMap<>();
        dataToBeSent.put("file_id", imageData);
        dataToBeSent.put("bucket_name", context.getRealm().getName());
        dataToBeSent.put("face_template", getFaceTemplate(context));
        return verifyFaceTemplate(context, imageData, dataToBeSent);
    }

    private String getFaceTemplate(RequiredActionContext context) {
        if (Boolean.parseBoolean(context.getConfig().getOrDefault("selfie-verification-compare-face", "false"))) {
            try {
                String parsedImageDetails = new ConfigTemplateParser(null, context.getRealm().getTrx(context.getRequestId()).getAttributes()).parseTemplate(
                        context.getConfig().getOrDefault("face-to-be-compared", "")
                );

                SelfieRegistrationHelper selfieRegistrationHelper = new SelfieRegistrationHelper();
                File file = GeneralHelper.createTempFile(parsedImageDetails);
                Map<String, Object> fileUploadResponse = selfieRegistrationHelper.uploadImage(file, context, context.getSession());
                return String.valueOf(fileUploadResponse.getOrDefault("template", ""));
            } catch (IllegalArgumentException illegalArgumentException) {
                logger.error("Invalid face template provided for comparison" + illegalArgumentException.getMessage());
                GeneralHelper.auditEvent(context, EventType.EZ_SELFIE_VERIFICATION_ERROR, ErrorMappings.INVALID_FACE_COMPARISON_FILE);
                return "";
            }
            catch (IOException e) {
                logger.error("Failed to get face template to compare: " + e.getMessage());
                return "";
            }

        }
        return context.getUser().getFirstAttribute("face_template");
    }

    public Map<String, Object> verifyFaceTemplate(RequiredActionContext context, String imageData, HashMap<String, String> dataToBeSent) throws IOException {
        Map<String, Object> faceTemplateRes = new HashMap<>();
        try {
            faceTemplateRes = compareFaceTemplate(imageData, "/face/compare", JsonSerialization.writeValueAsString(dataToBeSent));
            return faceTemplateRes;
        } catch (IOException e) {
//            HashMap<String, Object> traits = new HashMap<>();
//            traits.put("event", TwilioSegmentEvent.LV_SELFIE_UPLOAD_ERROR.name());
//            traits.put("message", "Selfie image upload error");
//            traits.put("error", e.getMessage());
            GeneralHelper.auditEvent(context, EventType.EZ_LIVENESS_VERIFICATION_ERROR, ErrorMappings.CONTACT_ADMIN);
            throw new IOException(e);
        }
    }

    public Map<String, Object> compareFaceTemplate(String imageData, String urlPath, String dataToBeSent) {

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .build();


            MediaType mimeType = MediaType.parse("application/json" + "; charset=utf-8");
            RequestBody requestBody = RequestBody.create(mimeType, dataToBeSent);

            Request request = new Request.Builder()
                    .url(EnvironmentReader.getValue("LIVELINESS_URL") + "/live" + urlPath)
                    .method("POST", requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            okhttp3.Response response = client.newCall(request).execute();


            logger.info("------------External Request-----------------");
            logger.info(dataToBeSent);
            logger.info("------------External Request-----------------");

            if (response.code() == 500)
                return null;
            if (!response.isSuccessful())
                return null;
            String resBody = response.body().string();
            logger.info("------------External Response-----------------");
            logger.info(response.code());
            logger.info("------------External Response-----------------");
            return JsonSerialization.readValue(resBody, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

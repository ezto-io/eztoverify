package com.qlikverify.backoffice.helpers.selfieRegistrationHelper;

import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.qlikverify.backoffice.liveness.analytics.TwilioSegmentEvent;
import com.qlikverify.backoffice.liveness.analytics.TwilioSegmentHelper;
import com.qlikverify.backoffice.services.livelinessVerificationService.LivelinessVerificationServiceImpl;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.events.EventType;
import org.keycloak.models.CibaAuthConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SelfieRegistrationHelper {

    private static HashMap <String, String> dataToBeSent;
    private static final LivelinessVerificationServiceImpl livelinessVerificationServiceImpl = new LivelinessVerificationServiceImpl();

    public Map<String, Object> uploadImage(File image, RequiredActionContext context, KeycloakSession session) throws IOException {
//        HashMap<String, Object> traits = new HashMap<>();
//        traits.put("event", TwilioSegmentEvent.LV_SELFIE_UPLOAD_START.name());
//        traits.put("message", "Selfie image upload start");
//        TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), traits);

        String imageData = GeneralHelper.putObjectToMinioForLiveness(image, "photo", context, session);
        dataToBeSent = new HashMap<>();
        dataToBeSent.put("file_id", imageData);
        dataToBeSent.put("bucket_name", context.getRealm().getName());
        String presignedObjectUrl = GeneralHelper.getPresignedObjectUrl(context, imageData);
        CibaAuthConfigModel cibaAuthConfigByClientId = session.getContext().getRealm().getCibaAuthConfigByClientId(session.getContext().getClient().getClientId(), session.getContext().getRealm().getId());
        context.getRealm().addAttribute(context.getRequestId(), "sp@photo_url", com.qlikverify.backoffice.GeneralHelper.getSecuredUrl(presignedObjectUrl, cibaAuthConfigByClientId));
        return generateFaceTemplate(context, imageData, dataToBeSent);
    }

    public Map<String, Object> generateFaceTemplate(RequiredActionContext context, String imageData, HashMap<String, String> dataToBeSent) throws IOException {
        Map<String, Object> faceTemplateRes = new HashMap<>();
        try {
            faceTemplateRes = livelinessVerificationServiceImpl.generateFaceTemplate(imageData, "/face/template", JsonSerialization.writeValueAsString(dataToBeSent));
            return faceTemplateRes;
        } catch (IOException e) {
//            HashMap<String, Object> traits = new HashMap<>();
//            traits.put("event", TwilioSegmentEvent.LV_SELFIE_UPLOAD_ERROR.name());
//            traits.put("message", "Selfie image upload error");
//            traits.put("error", e.getMessage());
//            TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), traits);
            GeneralHelper.auditEvent(context, EventType.EZ_LIVENESS_VERIFICATION_ERROR, ErrorMappings.CONTACT_ADMIN);
            throw new IOException(e);
        }
    }

}

package com.qlikverify.backoffice.liveness.controller;

import com.qlikverify.backoffice.Errors;
import com.qlikverify.backoffice.GeneralHelper;
import com.qlikverify.backoffice.helpers.nameSimilarityCheckHelper.NameSimilarityCheckHelper;
import com.qlikverify.backoffice.liveness.LivenessHelper;
import com.qlikverify.extensions.admin.controllers.actionToken.EztoActionTokenVerifier;
import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.extensions.admin.misc.helpers.EnvironmentReader;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ciba.CibaNotificationProvider;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.liveness.LivenessConstants.*;

public class LivenessVerificationResource extends BaseAuthCheckResource {

    KeycloakSession session;
    private Cors cors;
    private static final Logger logger = Logger.getLogger(LivenessVerificationResource.class);

    public LivenessVerificationResource(KeycloakSession session) {
        super(session);
        this.session = session;
    }

    String videoKYCBaseUrl = EnvironmentReader.getInstance().getValue("EZTO_VERIFY_VIDEO_SDK_URL");

    @OPTIONS
    @Path("{realmId}/{trxId}/{any:.*}")
    public Response corsPreflight() {
        return Response.ok()
                .header("Access-Control-Allow-Origin", videoKYCBaseUrl) // Or restrict to specific domains
                .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    @POST
    @Path("/{realmId}/{trxId}/verification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response verifyLiveness(@PathParam("trxId") String trxId, @PathParam("realmId") String realmId, Map<String, Object> reqBody, @Context HttpRequest req) {

//        HashMap<String, Object> hookStart = new HashMap<>();
//        hookStart.put("event", TwilioSegmentEvent.LV_HOOK_START.name());
//        hookStart.put("message", "Liveness received hook response");
//
//        TwilioSegmentHelper.queueEvent(trxId, "null", hookStart);


        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);

        //check action token
        EztoActionTokenVerifier eztoActionTokenVerifier = new EztoActionTokenVerifier(session, realm);
        String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        if (actionToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        JsonWebToken jsonWebToken = eztoActionTokenVerifier.validateToken(actionToken.split(" ")[1],
                realm.getId(),
                false);
        if (jsonWebToken == null || jsonWebToken.isExpired()) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        logger.info("LIVENESS HOOK RESPONSE:: " + reqBody);

        if (reqBody.containsKey("success")) {
//           HashMap<String, Object> successTraits = new HashMap<>();
//           successTraits.put("event", TwilioSegmentEvent.LV_BACKEND_SUCCESS.name());
//           successTraits.put("response_data", reqBody);
//           successTraits.put("message", "Liveness success");
//
//           TwilioSegmentHelper.queueEvent(trxId, "null", successTraits);
            realm.addAttribute(trxId, LIVENESS_COMPLETED, "true");
            realm.addAttribute(trxId, SP_LIVENESS_VERIFIED_AT, String.valueOf(Time.currentTime()));
            // add trx attributes for liveness verification for both success and failure
            LivenessHelper.updateLivenessAttributes(realm, trxId, (Map<String, Object>) reqBody.get("results"));
            if (reqBody.get("success").equals(false)) {
//               HashMap<String, Object> failureTraits = new HashMap<>();
//               failureTraits.put("event", TwilioSegmentEvent.LV_BACKEND_FAILURE.name());
//               failureTraits.put("response_data", reqBody);
//               failureTraits.put("message", "Liveness success");
//
//               TwilioSegmentHelper.queueEvent(trxId, "null", failureTraits);
                realm.addAttribute(trxId, LIVENESS_FAILED_REASON, String.valueOf(reqBody.get("code")));
            }
        }
//        HashMap<String, Object> hookEnds = new HashMap<>();
//        hookEnds.put("event", TwilioSegmentEvent.LV_END.name());
//        hookEnds.put("message", "Liveness hook ends");
//        TwilioSegmentHelper.queueEvent(trxId, "null", hookEnds);

        TrxModel trx = realm.getTrx(trxId);

        if (trx != null
                && trx.getAttributes() != null
                && trx.getAttributes().containsKey("ik@hook_way")
                && ("push".equals(trx.getAttributes().get("ik@hook_way")) || "kafka".equals(trx.getAttributes().get("ik@hook_way")))) {
            String clientId = trx.getAttributes().get("ik@client_id");
            ClientModel client = realm.getClientByClientId(clientId);

            EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection());
            event.client(client.getId());
            CibaConfig cibaConfig = realm.getCibaPolicy();
            String notProviderId = cibaConfig.getBackchannelTokenDeliveryMode(client);
            CibaNotificationProvider cibaNotificationProvider = session.getProvider(CibaNotificationProvider.class, notProviderId);
            CibaNotificationConfigModel configModel = realm.getCibaNotificationConfigByProviderId(notProviderId, client.getClientId(), realm.getId());

            session.getContext().setClient(client);
            OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
            OAuth2DeviceCodeModel deviceModel = store.getByDeviceCode(realm, trx.getAttributes().get("ik@device_code"));
            cibaNotificationProvider.setEventBuilder(event).sendNotification(session, realm, client, cibaConfig, deviceModel, configModel, null, null);
        }

        return Response.ok().build();
    }

    @POST
    @Path("/{realmId}/{trxId}/update-video-recordings")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response videoSDKRecording(@PathParam("trxId") String trxId, @PathParam("realmId") String realmId, Map<String, Object> reqBody, @Context HttpRequest req) {

        cors = Cors.add(req).auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);

        //check action token
        EztoActionTokenVerifier eztoActionTokenVerifier = new EztoActionTokenVerifier(session, realm);
        String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        if (actionToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        JsonWebToken jsonWebToken = eztoActionTokenVerifier.validateToken(actionToken.split(" ")[1],
                realm.getId(),
                false);
        if (jsonWebToken == null || jsonWebToken.isExpired()) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        logger.info("VIDEO KYC HOOK RESPONSE:: " + reqBody);

        realm.addAttribute(trxId, VIDEO_KYC_COMPLETED, "true");
        realm.addAttribute(trxId, VIDEO_KYC_VERIFIED_AT_CLAIM, String.valueOf(Time.currentTime()));
        if (reqBody.containsKey("success") && reqBody.get("success") != null && reqBody.get("success").equals(true)) {
            CibaAuthConfigModel cibaAuthConfigByClientId = realm.getCibaAuthConfigByClientId(reqBody.get("client_id").toString(), realm.getId());
            String shortenVideoURL = GeneralHelper.getSecuredUrl((String) reqBody.get("recording-url"), cibaAuthConfigByClientId);
            realm.addAttribute(trxId, VIDEO_KYC_RECORDED_VIDEO_CLAIM, shortenVideoURL);
            realm.addAttribute(trxId, VIDEO_KYC_RECORDED_VIDEO_STATUS_CLAIM, "success");
            realm.addAttribute(trxId, VIDEO_KYC_VERIFIED_CLAIM, "true");
        } else if (reqBody.containsKey("error-code") && reqBody.get("error-code") != null) {
            com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(realm, trxId, Errors.valueOf(reqBody.get("error-code").toString()), VIDEO_KYC);
            realm.addAttribute(trxId, VIDEO_KYC_RECORDED_VIDEO_STATUS_CLAIM, "failed");
            realm.addAttribute(trxId, VIDEO_KYC_VERIFIED_CLAIM, "false");
        }

        TrxModel trx = realm.getTrx(trxId);

        if (trx != null
                && trx.getAttributes() != null
                && trx.getAttributes().containsKey("ik@hook_way")
                && ("push".equals(trx.getAttributes().get("ik@hook_way")) || "kafka".equals(trx.getAttributes().get("ik@hook_way")))) {
            String clientId = trx.getAttributes().get("ik@client_id");
            ClientModel client = realm.getClientByClientId(clientId);

            EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection());
            event.client(client.getId());
            CibaConfig cibaConfig = realm.getCibaPolicy();
            String notProviderId = cibaConfig.getBackchannelTokenDeliveryMode(client);
            CibaNotificationProvider cibaNotificationProvider = session.getProvider(CibaNotificationProvider.class, notProviderId);
            CibaNotificationConfigModel configModel = realm.getCibaNotificationConfigByProviderId(notProviderId, client.getClientId(), realm.getId());

            session.getContext().setClient(client);
            OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
            OAuth2DeviceCodeModel deviceModel = store.getByDeviceCode(realm, trx.getAttributes().get("ik@device_code"));
            cibaNotificationProvider.setEventBuilder(event).sendNotification(session, realm, client, cibaConfig, deviceModel, configModel, null, null);
        }

        return Response.ok().build();
    }

    @POST
    @Path("/{realmId}/{trxId}/{actionId}/video-kyc-user-verification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response videoKYCUserVerification(@PathParam("trxId") String trxId, @PathParam("realmId") String realmId, @PathParam("actionId") String actionId, Map<String, Object> reqBody, @Context HttpRequest req) {

        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);

        String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        if (actionToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        JsonWebToken jsonWebToken = EztoActionTokenVerifier.validateToken(actionToken.split(" ")[1],
                realm.getId(),
                false);
        if (jsonWebToken == null || jsonWebToken.isExpired()) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        reqBody.forEach((key, value) -> {
            boolean isExist = Arrays.asList(realm.getRequiredActionProviderById(actionId).getConfig().get("vkyc-claims")
                    .split("##")).contains("video_kyc@" + key);
            if (isExist) {
                realm.addAttribute(trxId, "video_kyc@" + key, value.toString());
            }
            if ("Unable To Verify".equals(value.toString())) {
                com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(realm, trxId, Errors.valueOf("EVVKYC0007"), VIDEO_KYC);
            } else if ("Rejected".equals(value.toString()) && VIDEO_KYC_VERIFICATION_STATUS.replace("video_kyc@", "").equals(key)) {
                com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(realm, trxId, Errors.valueOf("EVVKYC0008"), VIDEO_KYC);
            }
        });
        return Response.ok().header("Access-Control-Allow-Origin", videoKYCBaseUrl) // Or restrict to specific domains
                .header("Access-Control-Allow-Methods", "POST")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    @Path("/{realmId}/{trxId}/check-pan")
    @POST
    @NoCache
    public Response checkPan(@PathParam("realmId") String realmId, @PathParam("trxId") String trxId, Map<String, Object> reqBody, @Context HttpRequest req) {
        logger.info("LivenessVerificationResource :: getTrxAttributes :: Entered");
        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);

        //check action token
        EztoActionTokenVerifier eztoActionTokenVerifier = new EztoActionTokenVerifier(session, realm);
        String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        if (actionToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        JsonWebToken jsonWebToken = eztoActionTokenVerifier.validateToken(actionToken.split(" ")[1],
                realm.getId(),
                false);
        if (jsonWebToken == null || jsonWebToken.isExpired()) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        Map<String, String> attributes = realm.getTrx(trxId).getAttributes();
        Map<String, Object> reqAttrs = new HashMap<>();

        String panNumber = attributes.getOrDefault("verify_pan@pan_number",
                attributes.getOrDefault("vkyc_pan_number", ""));
        String panName = attributes.getOrDefault("verify_pan@name", attributes.getOrDefault("vkyc_pan_name", ""));
        String OCRPanNumber = reqBody.get("idNumber").toString();
        String OCRPanName = reqBody.get("name").toString();

        double nameScore = NameSimilarityCheckHelper.calculateNameSimilarity(panName, OCRPanName);
        double noScore = NameSimilarityCheckHelper.calculateNameSimilarity(panNumber, OCRPanNumber);

        reqAttrs.put("panNumber", panNumber);
        reqAttrs.put("panName", panName);
        reqAttrs.put("nameScore", nameScore);
        reqAttrs.put("noScore", noScore);

        logger.info(String.format("LivenessVerificationResource :: getTrxAttributes :: %s :: %s", trxId, reqAttrs));
        return Response.status(Response.Status.OK).entity(reqAttrs).header("Access-Control-Allow-Origin", videoKYCBaseUrl) // Or restrict to specific domains
                .header("Access-Control-Allow-Methods", "POST")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    @Path("/{realmId}/{trxId}/face-match")
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response faceMatch(@PathParam("realmId") String realmId, @PathParam("trxId") String trxId, MultipartFormDataInput input, @Context HttpRequest req) {
        logger.info("LivenessVerificationResource :: faceMatch :: Entered");
        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);

//        check action token
        EztoActionTokenVerifier eztoActionTokenVerifier = new EztoActionTokenVerifier(session, realm);
        String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        if (actionToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        JsonWebToken jsonWebToken = eztoActionTokenVerifier.validateToken(actionToken.split(" ")[1],
                realm.getId(),
                false);
        if (jsonWebToken == null || jsonWebToken.isExpired()) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        Map<String, List<InputPart>> formParts = input.getFormDataMap();
        List<InputPart> firstImageParts = formParts.get("firstImage");
        List<InputPart> secondImageParts = formParts.get("secondImage");
        String firstImage = "";
        String secondImage = "";

        try {

            if (firstImageParts != null && !firstImageParts.isEmpty()) {
                firstImage = firstImageParts.get(0).getBodyAsString();
            }

            if (secondImageParts != null && !secondImageParts.isEmpty()) {
                secondImage = secondImageParts.get(0).getBodyAsString();
            }
        } catch (IOException e) {
            logger.error("IOException during face match", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        Map<String, String> livenessResponse = LivenessHelper.compareTwoBase64(firstImage, secondImage);

        logger.info(String.format("LivenessVerificationResource :: faceMatch :: %s :: %s", trxId, livenessResponse));
        return Response.status(Response.Status.OK).entity(livenessResponse).header("Access-Control-Allow-Origin", videoKYCBaseUrl) // Or restrict to specific domains
                .header("Access-Control-Allow-Methods", "POST")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    @Path("/{realmId}/{actionId}/get-configs")
    @GET
    @NoCache
    public Response getConfigs(@PathParam("realmId") String realmId, @PathParam("actionId") String actionId, @Context HttpRequest req) {
        logger.info("LivenessVerificationResource :: getConfigs:: Entered");
        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);

        //check action token
        EztoActionTokenVerifier eztoActionTokenVerifier = new EztoActionTokenVerifier(session, realm);
        String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        if (actionToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        JsonWebToken jsonWebToken = eztoActionTokenVerifier.validateToken(actionToken.split(" ")[1],
                realm.getId(),
                false);
        if (jsonWebToken == null || jsonWebToken.isExpired()) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        Map<String, String> configs = realm.getRequiredActionProviderById(actionId).getConfig();

        logger.info(String.format("LivenessVerificationResource :: getConfigs :: %s :: %s", actionId, configs));
        return Response.status(Response.Status.OK).entity(configs).header("Access-Control-Allow-Origin", videoKYCBaseUrl) // Or restrict to specific domains
                .header("Access-Control-Allow-Methods", "GET")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

}

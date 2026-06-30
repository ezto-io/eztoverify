package com.qlikverify.backoffice.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.backoffice.Constants;
import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.models.ClaimModel;
import com.qlikverify.extensions.admin.misc.helpers.JsonUtil;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.oidc.grants.ciba.util.UserTransactionStatus;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.JsonSerialization;

import javax.imageio.ImageIO;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.helpers.GeneralHelper.getCibaDeadLetterConfigModel;
import static com.qlikverify.helpers.GeneralHelper.getCibaNotificationConfigModel;
import static org.keycloak.utils.GeneralHelper.addCustomSupportMessageDetails;

public class GeneralHelper {
    private static final Logger logger = Logger.getLogger(GeneralHelper.class);
    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_URL);
    private static final String MINIO_SERVER_API_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_API_URL);
    private static final String DEFAULT_ACCESS_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_ACCESS_KEY);
    private static final String DEFAULT_SECRET_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_SECRET_KEY);
    public static String ROOT_HOST_NAME = EnvironmentReader.getValue("ROOT_DNS_NAME");
    public static final String GENERIC_ERROR = "Our services failed to process this request, if error persists please contact support.";
    public static String DECENTRO = "decentro";
    public static String SIGNZY = "signzy";
    public static String SUREPASS = "surepass";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static File createTempFile(String fileContent) throws IOException {
        String[] split = fileContent.split(",");
        String img;
        if (split.length == 1) {
            img = split[0];
        } else {
            img = split[1];
        }
        byte[] decodedBytes = Base64.getDecoder().decode(img);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(decodedBytes));
        File tempFile = File.createTempFile("temp", ".png");
        ImageIO.write(image, "png", tempFile);
        return tempFile;
    }

    public static Map<String, String> getImageDetails(String imageDetails) {
        try {
            return objectMapper.readValue(imageDetails, Map.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    public static void showView(RequiredActionContext context, String error, String ftl,
                                Map<String, String> attributes) {
        LoginFormsProvider formsProvider = context.form();
        formsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());
        Map<String, Object> params = addCustomSupportMessageDetails(context, null, new HashMap<>());
        params.forEach(formsProvider::setAttribute);

        if (error != null) {
            formsProvider.addError(new FormMessage(error));
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        if (attributes != null) {
            attributes.forEach(formsProvider::setAttribute);
        }
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    public static void showView(RequiredActionContext context, String error, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        Map<String, Object> attributes = addCustomSupportMessageDetails(context, null, new HashMap<>());
        attributes.forEach(formsProvider::setAttribute);
        if (error != null) {
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        formsProvider.setAttribute("defReg", context.getRealm().getDefaultRegion());
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    public static void auditEvent(KeycloakSession session, ClientModel clientModel, UserModel userModel,
                                  EventType eventType, String error, Map<String, String> additionalDetails) {

        EventBuilder eventBuilder = new EventBuilder(session.getContext().getRealm(), session,
                session.getContext().getConnection());
        eventBuilder.client(clientModel.getClientId());
        if (userModel != null) {
            eventBuilder.user(userModel);
        } else {
            eventBuilder.user(UNKNOWN_USER);
        }
        eventBuilder.event(eventType);
        if (additionalDetails != null && additionalDetails.size() > 0) {
            additionalDetails.forEach(eventBuilder::detail);
        }
        if (error == null || error.equals("")) {
            eventBuilder.success();
        } else {
            eventBuilder.error(error);
        }
    }

    public static void auditEvent(RequiredActionContext context, EventType eventType, String error,
                                  Map<String, String> additionalDetails) {

        EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        eventBuilder.detail("requestId", context.getAuthenticationSession().getAuthNote("request"));
        eventBuilder.client(context.getAuthenticationSession().getClient().getClientId());
        if (context.getUser() != null) {
            eventBuilder.user(context.getUser());
        } else {
            eventBuilder.user(UNKNOWN_USER);
        }
        eventBuilder.event(eventType);
        if (additionalDetails != null && additionalDetails.size() > 0) {
            additionalDetails.forEach(eventBuilder::detail);
        }
        if (error == null || error.equals("")) {
            eventBuilder.success();
        } else {
            eventBuilder.error(error);
        }
    }

    public static EventBuilder getEvent(RequiredActionContext context, EventType eventType) {
        EventBuilder eventBuilder = new EventBuilder(context.getRealm(), context.getSession(), context.getConnection());
        eventBuilder.detail("requestId", context.getAuthenticationSession().getAuthNote("request"));
        eventBuilder.client(context.getAuthenticationSession().getClient().getClientId());
        if (context.getUser() != null) {
            eventBuilder.user(context.getUser());
        } else {
            eventBuilder.user(UNKNOWN_USER);
        }
        eventBuilder.event(eventType);
        return eventBuilder;
    }

    public static void auditEvent(RequiredActionContext context, EventType eventType, String error) {
        error = sanitizeErrorMessage(error);
        logger.info(error);
        EventBuilder eventBuilder = getEvent(context, eventType);
        if (error == null || error.equals("")) {
            eventBuilder.success();
        } else {
            eventBuilder.error(error);
        }
    }

    public static String sanitizeErrorMessage(String error){
        if(error == null)
            return null;
        error = (error.contains(DECENTRO) || error.contains(DECENTRO.toUpperCase())) ? GENERIC_ERROR : error;
        error = (error.contains(SUREPASS) || error.contains(SUREPASS.toUpperCase())) ? GENERIC_ERROR : error;
        error = (error.contains(SIGNZY) || error.contains(SIGNZY.toUpperCase())) ? GENERIC_ERROR : error;
        return error;
    }

    public static void putObjectToMinio(RealmModel realm, Map<String, Object> data,
                                        String verificationId, String requesId) {
        String bucketName = realm.getName();
        String objectName = requesId + "/" + verificationId + ".json";

        MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_SERVER_URL)
                .credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();

        try {
            minioClient.ignoreCertCheck();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            String jsonString = objectMapper.writeValueAsString(data);
            InputStream dataJson = new ByteArrayInputStream(jsonString.getBytes());
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(dataJson, jsonString.length(), -1)
                    .build());

            realm.addAttribute(requesId, "spm@" + verificationId.replace("bo-", ""), verificationId.replace("bo-", ""));

        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException
                 | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException
                 | XmlParserException e) {
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
            logger.error("Failed with mino: " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static void handleSettingClaims(RealmModel realm, Map<String, Object> responseDataFromSurePass, List<String> claimsToBeAdded, String requiredActionId, String requestId) {
        Map<String, Object> responseDataFromSurePassLowerCase = new HashMap<>();
        responseDataFromSurePass.keySet().stream().forEach(key -> responseDataFromSurePassLowerCase.put(key.toLowerCase(), responseDataFromSurePass.get(key)));
        claimsToBeAdded = claimsToBeAdded.stream().map(x-> x.replace(requiredActionId + "@","")).collect(Collectors.toList());

        claimsToBeAdded.forEach(claim -> {
            if (responseDataFromSurePass.containsKey(claim)) {
                try {
                    if (objectMapper.readTree(JsonUtil.getAsJsonString(responseDataFromSurePass.get(claim))).isTextual()) {
                        realm.addAttribute(requestId, requiredActionId + "@" + claim, String.valueOf(responseDataFromSurePass.get(claim)));
                    } else
                        realm.addAttribute(requestId,  requiredActionId + "@" + claim, JsonUtil.getAsJsonString(responseDataFromSurePass.get(claim)));
                } catch (JsonProcessingException e) {
                    logger.info("failed to parse json " + e.getMessage());
                }
            } else if (responseDataFromSurePassLowerCase.containsKey(claim.replace("_", "").toLowerCase())) {
                String claimLowerCase = claim.replace("_", "").toLowerCase();
                try {
                    if (objectMapper.readTree(JsonUtil.getAsJsonString(responseDataFromSurePassLowerCase.get(claimLowerCase))).isTextual()) {
                        realm.addAttribute(requestId,  requiredActionId + "@" + claim, String.valueOf(responseDataFromSurePassLowerCase.get(claimLowerCase)));
                    } else
                        realm.addAttribute(requestId,  requiredActionId + "@" + claim, JsonUtil.getAsJsonString(responseDataFromSurePassLowerCase.get(claimLowerCase)));
                } catch (JsonProcessingException e) {
                    logger.info("failed to parse json " + e.getMessage());
                }
            }
        });
    }

    public static Map<String, String> processClaims(Map<String, Object> responseMap, String verifierName) {
        Map<String, String> claims = new HashMap<>();

        responseMap.forEach((key, value) -> {
            String claimKey = verifierName + "@" + key;

            if (value instanceof Map || value instanceof List) {
                try {
                    claims.put(claimKey, JsonSerialization.writeValueAsString(value));
                } catch (IOException e) {
                    logger.error("Error while serializing claim: {} :: {}", key, e);
                }
            } else {
                claims.put(claimKey, String.valueOf(value));
            }
        });
        return claims;
    }



    public static Map<String, Object> getRequiredClaims(RequiredActionContext context, Map<String, Object> userDetails, List<Map<String, String>> defaultClaimsWithType, String verifierName,
                                                        String claims, String additionalDetails) throws IOException{
        List requiredClaimsWithType = new ArrayList<>();

        try{
            if(context.getConfig().isEmpty()) {
                requiredClaimsWithType = defaultClaimsWithType;
            } else {
                if(!context.getConfig().get(claims).isBlank()) {
                    requiredClaimsWithType = JsonSerialization.readValue(context.getConfig().get(claims), List.class);
                }
                List finalRequiredClaimsWithType = requiredClaimsWithType;
                if(!context.getConfig().get(additionalDetails).isBlank()) {
                    JsonSerialization.readValue(context.getConfig().get(additionalDetails), List.class).stream().forEach(x -> {
                        finalRequiredClaimsWithType.add(x);
                    });
                    requiredClaimsWithType = finalRequiredClaimsWithType;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        List<ClaimModel> claimList = new ArrayList<>();
        requiredClaimsWithType.stream()
                .filter(claim -> !((Map<String, String>) claim).get("name").contains("verified"))
                .forEach(x -> {
                    Map<String, String> item = (Map<String, String>) x; // Cast to Map<String, String>
                    String type = item.get("claimType");
                    String name = item.get("name");
                    claimList.add(ClaimModel.builder()
                            .name(name)
                            .claimType(type)
                            .build());
                });
//        String providerName = getProviderName(context, "");

        return new HashMap<>();

//        return Verification.doVerification(context, userDetails, verifierName, "", null, null, stripeMappingId()[0]);
    }

    public static boolean checkForAvailableMetadatas(String requestId, RealmModel realmModel, Map<String, Object> surePassResponse) {

        List<String> keyValues = new ArrayList<>();
        keyValues.addAll(surePassResponse.keySet());

        boolean result = false;
        result = keyValues.stream().map(key -> {
            String surePassValue = "";
            if (surePassResponse.get(key) instanceof String) {
                surePassValue = String.valueOf(surePassResponse.get(key));
            } else if (surePassResponse.get(key) instanceof Map) {
                Map<String, Object> keyMap = (Map<String, Object>) surePassResponse.get(key);
                surePassValue = String.valueOf(keyMap.get("value"));
            }
            TrxModel trxModel = realmModel.getTrx(requestId);
            return !trxModel.getAttributes().containsKey("sp@" + key)
                    || (trxModel.getAttributes().get("sp@" + key).isBlank() || surePassValue.equals(""))
                    || (trxModel.getAttributes().get("sp@" + key).equals(surePassValue));
        }).collect(Collectors.toList()).contains(false);
        return !result;
    }

    public static File createMediaTempFile(InputStream inputStream) throws IOException {
        File tempFile = File.createTempFile("temp", ".mkv");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.write(inputStream.readAllBytes(), out);
        }
        return tempFile;
    }

    public static void triggerHookFailedEmailNotification(KeycloakSession session, RealmModel realm, ClientModel client, String hookKey) {
        String hookUrl;
        hookUrl = hookKey.equals(FAILED_HOOK_ATTEMPTS_FOR_DEAD_LETTER) ? getCibaDeadLetterConfigModel(session, realm, client).getConfig().get("notification-endpoint") : getCibaNotificationConfigModel(session, realm, client).getConfig().get("notification-endpoint");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("applicationName", client.getName());
        attrs.put("numberOfFailures", client.getAttribute(hookKey.equals(FAILED_HOOK_ATTEMPTS_FOR_DEAD_LETTER) ? FAILED_HOOK_ATTEMPTS_FOR_DEAD_LETTER : FAILED_HOOK_ATTEMPTS));
        attrs.put("hookUrl", hookUrl);
        attrs.put("workspaceName", realm.getName());
        if (hookKey.equalsIgnoreCase(FAILED_HOOK_ATTEMPTS_FOR_DEAD_LETTER)) {
            attrs.put("triggeredForDeadLetter", true);
        }
        AdminEventBuilder adEvt = new AdminEventBuilder(realm, new AdminAuth(session.getContext().getRealm(),
                null, null, session.clients().getClientByClientId(realm, "dashboard")), session,
                session.getContext().getConnection());
        adEvt.realm(realm);
        adEvt.operation(OperationType.HOOK_FAILURE_NOTIFICATION)
                .resource(ResourceType.NOTIFICATION)
                .resourcePath(session.getContext().getUri()).representation(attrs).success();
    }

    public static String putObjectToMinioForLiveness(File file, String media, RequiredActionContext context, KeycloakSession session) {
        String bucketName = context.getRealm().getName();
        String objectName = context.getUser().getId() + "/" + context.getRequestId() + "/" + media + "/" + file.getName();

        MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_SERVER_URL)
                .credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();

        try {
            minioClient.ignoreCertCheck();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new FileInputStream(file), file.length(), -1)
                            .contentType("application/octet-stream")
                            .build());

            return objectName;
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException
                 | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException
                 | XmlParserException e) {
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
            logger.error("Failed with mino: " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static String getPresignedObjectUrl(RequiredActionContext context, String objectName) {
        String bucketName = context.getRealm().getName();

        MinioClient minioClient = MinioClient.builder().endpoint(MINIO_SERVER_API_URL)
                .credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();
        try {
            minioClient.ignoreCertCheck();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .build());

        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException
                 | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException
                 | XmlParserException e) {
            throw new IllegalStateException(e);
        } catch (KeyManagementException e) {
            logger.error("Failed with mino: " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Determines whether the current flow is an onboarding flow based on the provided
     * {@link RequiredActionContext}. This method checks if a specific transaction value
     * attribute ("sp@workflow") exists and if its value indicates an onboarding process.
     *
     * @param context The {@link RequiredActionContext} containing information about the
     *                current request and realm. This context is used to access realm
     *                attributes and transaction values.
     * @return {@code true} if the transaction value for "sp@workflow" is set to "onboarding";
     * {@code false} otherwise. This includes cases where the attribute does not exist
     * or its value is blank.
     */
    public static boolean isOnboardingFlow(RequiredActionContext context) {
        boolean hasWorkflowValue = context.getRealm().hasTrxValueAttribute(context.getRequestId(), "sp@workflow");
        if (!hasWorkflowValue) return false;

        String trxValue = context.getRealm().getTrxValue(context.getRequestId(), "sp@workflow");
        if (Validation.isBlank(trxValue)) return false;


        return trxValue.equalsIgnoreCase("onboarding");
    }

    public static boolean checkForClaims(RequiredActionContext context, String numberClaim, String verifiedClaim, String verifiedAtClaim) {

        return (numberClaim.isEmpty() || context.getRealm().hasTrxValueAttribute(context.getRequestId(), "sp@" + numberClaim))
                && (verifiedClaim.isEmpty() || context.getRealm().hasTrxValueAttribute(context.getRequestId(), "sp@" + verifiedClaim))
                && (verifiedAtClaim.isEmpty() || context.getRealm().hasTrxValueAttribute(context.getRequestId(), "sp@" + verifiedAtClaim));

    }

    public static void handleCancel(RequiredActionContext context, EventType eventType) {
        GeneralHelper.auditEvent(context, eventType, ErrorMappings.USER_CANCELLED);
        context.getRealm().updateTransactionStatus(context.getRequestId(), context.getUser().getUsername(), UserTransactionStatus.USER_CANCELLED.getDisplayName());
        context.failure();
    }

    public static List<Map> getCommonClaimsWithType(List<CommonClaimModel> commonClaims, Map<String, String> defaultClaims) {
        List<Map> commonClaimsWithType = new ArrayList<>();
        defaultClaims.keySet().stream().forEach(x -> {
            Map<String, String> claims = new HashMap<>();
            claims.put("name", x);
            claims.put("claimType", defaultClaims.get(x));
            commonClaimsWithType.add(claims);
        });

        if (commonClaims == null) {
            commonClaims = new ArrayList<>();
        }


        commonClaims.stream().filter(Objects::nonNull).forEach(commonClaimModel -> {
            try {
                String json = commonClaimModel.getCommonClaims();
                ClaimModel[] commonClaimModels;

                if (json.startsWith("[")) {
                    commonClaimModels = JsonSerialization.readValue(json, ClaimModel[].class);
                } else {
                    ClaimModel singleClaim = JsonSerialization.readValue(json, ClaimModel.class);
                    commonClaimModels = new ClaimModel[]{singleClaim};
                }

                Arrays.asList(commonClaimModels).stream().forEach(x -> {
                    Map<String, String> claimObject = new HashMap<>();
                    claimObject.put("name", x.getName());
                    claimObject.put("claimType", x.getClaimType());
                    if(!commonClaimsWithType.contains(claimObject)) {
                        commonClaimsWithType.add(claimObject);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return commonClaimsWithType;
    }

    public static List<Map> getClaimsWithType(List<Map<String, String>> claims) {
        List<Map> commonClaimsWithType = new ArrayList<>();

        claims.forEach(claim -> {
            try {
                Map<String, String> claimObject = new HashMap<>();
                claimObject.put("name", claim.get("name"));
                claimObject.put("claimType", claim.get("claimType"));
                if(!commonClaimsWithType.contains(claimObject)) {
                    commonClaimsWithType.add(claimObject);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return commonClaimsWithType;
    }

    public static List<List<Map<String, String>>> getDefaultClaimsWithType(List<OtherClaimModel> otherClaims) {
        List<List<Map<String, String>>> defaultClaims = new ArrayList<>();
        if (otherClaims == null) {
            otherClaims = new ArrayList<>();
        }

        otherClaims.stream().forEach(otherClaimModel -> {
            try{
                String json = otherClaimModel.getOtherClaims();
                ClaimModel[] otherClaimModels;
                List<Map<String, String>> claimsWithType = new ArrayList<>();

                if (json.startsWith("[")) {
                    otherClaimModels = JsonSerialization.readValue(json, ClaimModel[].class);
                } else {
                    ClaimModel singleClaim = JsonSerialization.readValue(json, ClaimModel.class);
                    otherClaimModels = new ClaimModel[] { singleClaim };
                }
                Arrays.asList(otherClaimModels).stream().forEach(x -> {
                    claimsWithType.add(new HashMap<String, String>() {{
                        put("name", x.getName());
                        put("claimType", x.getClaimType());
                    }});
                });
                defaultClaims.add(claimsWithType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return defaultClaims;
    }

    public static List<List<Map<String, String>>> getClaimsListWithType(List<Map<String, String>> claims) {
        List<List<Map<String, String>>> defaultClaims = new ArrayList<>();
        claims.forEach(claim -> {
            try{
                List<Map<String, String>> claimsWithType = new ArrayList<>();
                claimsWithType.add(new HashMap<String, String>() {{
                    put("name", claim.get("name"));
                    put("claimType", claim.get("claimType"));
                }});
                defaultClaims.add(claimsWithType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return defaultClaims;
    }

    public static List<Map<String, String>> getDefaultValueWithType(Map<String, String> defaultClaims) {
        List<Map<String, String>> claimsWithType = new ArrayList<>();
        defaultClaims.keySet().stream().forEach(x -> {
            Map<String, String> claims = new HashMap<>();
            claims.put("name", x);
            claims.put("claimType", defaultClaims.get(x));
            claimsWithType.add(claims);
        });
        return claimsWithType;
    }

    public static Map<String, String> getPotentialClaims(List<OtherClaimModel> otherClaims) {
        Map<String, String> potentialClaims = new HashMap<>();
        otherClaims.stream().forEach(otherClaimModel -> {
            try{
                String json = otherClaimModel.getOtherClaims();
                ClaimModel[] otherClaimModels;

                if (json.startsWith("[")) {
                    otherClaimModels = JsonSerialization.readValue(json, ClaimModel[].class);
                } else {
                    ClaimModel singleClaim = JsonSerialization.readValue(json, ClaimModel.class);
                    otherClaimModels = new ClaimModel[] { singleClaim };
                }
                Arrays.asList(otherClaimModels).stream().forEach(x -> {
                    potentialClaims.put(x.getName(), x.getClaimType());
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return potentialClaims;
    }

    public static String getProviderName(KeycloakSession session, String verifierName) {
        String realmId = session.getContext().getRealm().getId();
        String masterRealmId = session.realms().getRealmByName(Config.getAdminRealm()).getId();
        String providerName = "";
        APIPriorityModel verifierAPIDetails;

        if(!session.getContext().getRealm().getVerifierProviderDetails(realmId, null, null, "",null, "ASC").isEmpty()) {
            verifierAPIDetails = session.getContext().getRealm().getVerifierAPIDetails(verifierName, realmId);
            if(verifierAPIDetails == null){
                verifierAPIDetails = session.getContext().getRealm().getVerifierAPIDetails(verifierName, masterRealmId);
            }
            if (verifierAPIDetails != null && !verifierAPIDetails.getIsEnabled()) {
                verifierAPIDetails = session.getContext().getRealm().getVerifierAPIDetails(verifierName, masterRealmId);
                providerName = verifierAPIDetails.getSelectedProvider();
            }else if (verifierAPIDetails != null) {
                providerName = verifierAPIDetails.getSelectedProvider();
            }
            return providerName;
        }

        verifierAPIDetails = session.getContext().getRealm().getVerifierAPIDetails(verifierName, masterRealmId);
        providerName = verifierAPIDetails.getSelectedProvider();
        return providerName;
    }

    public static Map<String, String> getClaims(RealmModel realm, String providerName, String apiName, String prefix) {
        return realm.getVerifierConfigs(apiName, providerName).getClaims()
                .entrySet()
                .stream()
                .map(x -> Map.entry(prefix + x.getKey(), x.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

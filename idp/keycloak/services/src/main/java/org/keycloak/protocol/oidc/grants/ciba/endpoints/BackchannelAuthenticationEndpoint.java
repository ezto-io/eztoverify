/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.protocol.oidc.grants.ciba.endpoints;

import com.qlikverify.apiConnector.spi.APIConnectorProvider;
import com.qlikverify.constants.APIErrorCodes;
import org.apache.pdfbox.io.IOUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.EnvironmentReader;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.authenticators.client.ClientAuthUtil;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.ciba.CibaAuthProviderFactory;
import org.keycloak.ciba.CibaNotificationProvider;
import org.keycloak.ciba.models.BindingMessage;
import org.keycloak.ciba.models.Consent;
import org.keycloak.ciba.models.UserData;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelProvider;
import org.keycloak.protocol.oidc.grants.ciba.channel.CIBAAuthenticationRequest;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context.BackchannelAuthenticationRequestContext;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.request.BackchannelAuthenticationEndpointRequest;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.request.BackchannelAuthenticationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.grants.ciba.resolvers.CIBALoginUserResolver;
import org.keycloak.protocol.oidc.grants.ciba.util.UserTransactionStatus;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.GeneralHelper;
import org.keycloak.utils.StringUtil;

import javax.persistence.PersistenceException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import java.io.*;
import java.security.AccessControlException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.keycloak.models.AuthenticationFlowBindings.CIBA_AUTHORIZER;
import static org.keycloak.models.CibaConfig.*;

public class BackchannelAuthenticationEndpoint extends AbstractCibaEndpoint {

    private final RealmModel realm;

    private static final Pattern BINDING_MESSAGE_VALIDATION = Pattern.compile("^[a-zA-Z0-9-._+/!?#]{1,50}$");
    private static final String WEB_TO_DEVICE = "sp@web_to_device";
    private static final String REQUESTED_VERIFICATION = "requestedVerification";
    private static final String API_GENERIC_ERROR = "failed to process the request, please try after some time";
    private static APIConnectorProvider apiConnectorProvider;

    private Logger logger = Logger.getLogger(BackchannelAuthenticationEndpoint.class.getName());

    public BackchannelAuthenticationEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
        this.realm = session.getContext().getRealm();
        event.event(EventType.EZ_TRANSACTION_REQUEST);
        apiConnectorProvider = session.getProvider(APIConnectorProvider.class);
    }

    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processGrantRequest(@Context HttpRequest httprequest) throws IOException {
        logger.info("BackChannerlAuthenticationEndpoint::ProcessGrantRequest::Platform transaction");

        MultivaluedMap<String, String> map = httprequest.getDecodedFormParameters();
        return processGrantRequestHelper(map, httprequest.getHttpHeaders(), null);
    }
    @POST
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processGrantRequest(@MultipartForm MultipartFormDataInput req, @Context HttpRequest httprequest) throws IOException {
        logger.info("BackChannerlAuthenticationEndpoint::ProcessGrantRequest::API transaction - OCR");

        Map<String, List<InputPart>> requestMap = req.getFormDataMap();

        Response validationFailedResponse = checkIfVerifierExists(requestMap.get(REQUESTED_VERIFICATION).get(0).getBodyAsString(), realm);
        if(validationFailedResponse != null)
            return validationFailedResponse;

        if(!validateOcrRequestDetails(new HashMap<>(requestMap), realm)) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "The requested verification details are invalid.");
            return Response.status(400).entity(errorMap).build();
        }

        String requestedVerification = requestMap.get(REQUESTED_VERIFICATION).get(0).getBodyAsString();
        File file = null;
        if(requestMap.containsKey("file")) {
            String ext = requestMap.get("file").get(0).getMediaType().getSubtype();
            ext = "file." + ext;
            file = File.createTempFile("temp", ext);
            InputStream inputStream = requestMap.get("file").get(0).getBody(InputStream.class, null);
            try (OutputStream outputStream = new FileOutputStream(file)) {
                IOUtils.copy(inputStream, outputStream);
            } catch (FileNotFoundException e) {
                return Response.status(400).entity("bad request").build();
            } catch (IOException e) {
                return Response.status(400).entity("bad request").build();
            }
        }
        String requestId = UUID.randomUUID().toString();
        String workspaceId = realm.getId();
        HashMap<String, Object> requestData = new HashMap<>();
        HashMap<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("workspaceId", workspaceId);
        HashMap<String, Object> finalData = data;
        requestMap.forEach((key, value) ->{
            if(!key.equals("file") && !key.equals(REQUESTED_VERIFICATION)) {
                try {
                    finalData.put(key, requestMap.get(key).get(0).getBodyAsString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        data.putAll(finalData);
        requestData.put("metadata", data);
        requestData.put("requested_verification", requestedVerification);
        data = new HashMap<>();
        data.put("metadata", new HashMap<>(){{put("workflow", "verification");}});
        requestData.put("user", data);

        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.put("request", Arrays.asList(JsonSerialization.writeValueAsString(requestData)));
        return processGrantRequestHelper(map, httprequest.getHttpHeaders(), file);
    }

    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processGrantRequest(HashMap<String, String> requestMap, @Context HttpRequest httprequest) throws IOException {
        logger.info("BackChannerlAuthenticationEndpoint::ProcessGrantRequest::API transaction - KYC");

        Response validationFailedResponse = checkIfVerifierExists(requestMap.get(REQUESTED_VERIFICATION), realm);
        if(validationFailedResponse != null)
            return validationFailedResponse;

        if(!validateRequestDetails(new HashMap<>(requestMap), realm)) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "The requested verification details are invalid.");
            return Response.status(400).entity(errorMap).build();
        }

        String requestedVerification = requestMap.get(REQUESTED_VERIFICATION);
        String requestId = UUID.randomUUID().toString();
        String workspaceId = realm.getId();
        HashMap<String, Object> requestData = new HashMap<>();
        HashMap<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("workspaceId", workspaceId);
        HashMap<String, Object> finalData = data;
        requestMap.forEach((key, value) ->{
            if(!key.equals("file") && !key.equals(REQUESTED_VERIFICATION)) {
                finalData.put(key, requestMap.get(key));
            }
        });
        data.putAll(finalData);
        requestData.put("metadata", data);
        requestData.put("requested_verification", requestedVerification);
        data = new HashMap<>();
        data.put("metadata", new HashMap<>(){{put("workflow", "verification");}});
        requestData.put("user", data);

        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.put("request", Arrays.asList(JsonSerialization.writeValueAsString(requestData)));
        return processGrantRequestHelper(map, httprequest.getHttpHeaders(), null);
    }

    private boolean validateOcrRequestDetails(HashMap<String, List<InputPart>> requestMap, RealmModel realm){
        realm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
        String requestedVerification = null;
        Set<String> keysFromRequest = requestMap.keySet();
        try {
            requestedVerification = requestMap.get(REQUESTED_VERIFICATION).get(0).getBodyAsString();
            keysFromRequest.remove(REQUESTED_VERIFICATION);
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
        String finalRequestedVerification = requestedVerification;
        Optional<APIPriorityModel> apiDetails =
                apiConnectorProvider.getApiConnectorConfigModel(
                        realm.getId()).stream().filter(x->x.getVerifierName().equals(finalRequestedVerification)).findFirst();
        if(apiDetails.isEmpty())
            return false;
        List<String> list = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\$\\{(.*?)}").matcher(apiConnectorProvider.getVerifierConfigs(requestedVerification,
                apiDetails.get().getSelectedProvider()).getRequestBody());

        while (matcher.find()) {
            list.add(matcher.group(1));
        }
        keysFromRequest.removeAll(list);
        return keysFromRequest.isEmpty();
    }

    private Response checkIfVerifierExists(String requestedVerification, RealmModel realm){
        RealmModel masterRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());

        Optional<APIPriorityModel> apiDetails =
                apiConnectorProvider.getApiConnectorConfigModel(realm.getId())
                        .stream()
                        .filter(x->x.getVerifierName().equals(requestedVerification))
                        .findFirst();
        if(apiDetails.isEmpty()){
            Optional<APIPriorityModel> masterApiDetails =
                    apiConnectorProvider.getApiConnectorConfigModel(masterRealm.getId())
                            .stream()
                            .filter(x->x.getVerifierName().equals(requestedVerification))
                            .findFirst();
            if(masterApiDetails.isEmpty()){
                Map<String, String> errorMap = new HashMap<>();
                logger.error(realm.getName() + " - The requested verification details are invalid - " + requestedVerification);
                errorMap.put("error", "The requested verification details are invalid.");
                return Response.status(404).entity(errorMap).build();
            }
            AdminEventBuilder adEvt = new AdminEventBuilder(realm,
                    new AdminAuth(realm, null, null, realm.getClientByClientId("dashboard")),
                    session, session.getContext().getConnection());
            adEvt.realm(realm);
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("error", "access denied");
            errorDetails.put("api", requestedVerification);
            errorDetails.put("message", "Request access for this API, mail to support@ezto.io");
            adEvt.operation(OperationType.ACTION).resource(ResourceType.API).representation(errorDetails).success();

            Map<String, String> errorMap = new HashMap<>();
            logger.error(realm.getName() + " - Permission denied - " + requestedVerification);
            errorMap.put("error", "Permission denied");
            return Response.status(403).entity(errorMap).build();
        }
        return null;
    }
    private boolean validateRequestDetails(HashMap<String, String> requestMap, RealmModel realm){
        if(realm.getVerifierProviderDetails(realm.getId(), null, null, "",null, "ASC").isEmpty()) {
            realm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
        }
        String requestedVerification = null;
        Set<String> keysFromRequest = requestMap.keySet();
        requestedVerification = requestMap.get(REQUESTED_VERIFICATION);
        if (StringUtil.isBlank(requestedVerification)) {
            logger.error("error in request body, requestedVerification is empty");
            return false;
        }
        keysFromRequest.remove(REQUESTED_VERIFICATION);
        String finalRequestedVerification = requestedVerification;
        Optional<APIPriorityModel> apiDetails =
                apiConnectorProvider.getApiConnectorConfigModel(
                        realm.getId()).stream().filter(x->x.getVerifierName().equals(finalRequestedVerification)).findFirst();
        if(apiDetails.isEmpty()) {
            logger.error("api details is empty");
            return false;
        }
        List<String> list = new ArrayList<>();
        APIVerifierConfigModel apiVerifierConfigModel = apiConnectorProvider.getVerifierConfigs(requestedVerification, apiDetails.get().getSelectedProvider());
        if(apiVerifierConfigModel == null){
            logger.error("api verifier config model is empty");
            return false;
        }
        Matcher matcher = Pattern.compile("\\$\\{(.*?)}").matcher(apiVerifierConfigModel.getRequestBody());

        while (matcher.find()) {
            list.add(matcher.group(1));
        }
        keysFromRequest.removeAll(list);
        return keysFromRequest.isEmpty();
    }

    private Response processGrantRequestHelper(MultivaluedMap<String, String> map, HttpHeaders httpHeaders, File file) throws IOException {
        List<String> appIds = null;
        List<String> appSecrets = null;
        String version = "0";
        List<String> expectedVersions = List.of("0", "1");

        if (httpHeaders != null) {
            version = httpHeaders.getHeaderString("api-version");
            if (version == null || (version != null && !expectedVersions.contains(version)))
                version = "0";
        }

        if (httpHeaders == null)
            throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Invalid app credentials", Response.Status.UNAUTHORIZED);

        String authorizationHeader = httpHeaders.getHeaderString("Authorization");

        switch (version) {
            case "1": {
                if (authorizationHeader == null || authorizationHeader.isEmpty())
                    throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Invalid app credentials", Response.Status.UNAUTHORIZED);

                String[] secrets = BasicAuthHelper.parseHeader(authorizationHeader);
                appIds = new ArrayList<>();
                appIds.add(secrets[0]);
                appSecrets = new ArrayList<>();
                appSecrets.add(secrets[1]);
            }
            break;
            default: {
                if (version.equalsIgnoreCase("0") && !com.qlikverify.helpers.GeneralHelper.isV0Supported(realm)) {
                    event.event(EventType.EZ_TRANSACTION_START_ERROR).error("Authentication method not supported. Please use v1 authentication.");
                    OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(APIErrorCodes.ez_transaction_1000.value, "Authentication method not supported. Please use v1 authentication.");
                    throw new ErrorResponseException(errorRep.getError(), errorRep.getErrorDescription(), Response.Status.UNAUTHORIZED);
                }
                if (!map.containsKey("app_id") || !map.containsKey("app_secret")) {
                    throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Invalid app credentials", Response.Status.UNAUTHORIZED);
                }
                appIds = new ArrayList<>();
                appIds.add(map.getFirst("app_id"));
                appSecrets = new ArrayList<>();
                appSecrets.add(map.getFirst("app_secret"));
            }
            break;
        }

        if (appIds == null || appSecrets == null)
            throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Invalid app credentials", Response.Status.UNAUTHORIZED);


        // Bypassed license and subscription checks

        map.put("client_id", appIds);
        map.put("client_secret", appSecrets);

        String requestId = UUID.randomUUID().toString();
        boolean isApiRequest = appIds.stream().filter(appId -> realm.getClientByClientId(appId) != null).anyMatch(appId -> {
            String cibaAuth = realm.getClientByClientId(appId).getAuthenticationFlowBindingOverrides().get("ciba_auth");
            return cibaAuth.equalsIgnoreCase("api-authorizer");
        });

        if(!isApiRequest)
            event.event(EventType.EZ_TRANSACTION_REQUEST_START).detail("requestId", requestId).success();
        event.detail("requestId", requestId);
        CIBAAuthenticationRequest request = authorizeClient(map, requestId, version);

        try {
            String authReqId = request.serialize(session);
            AuthenticationChannelProvider provider = session.getProvider(AuthenticationChannelProvider.class);

            if (provider == null) {
                event.error("Authentication Channel Provider not found.");
                throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR, "Something went wrong. ErrorCode: 100", Response.Status.INTERNAL_SERVER_ERROR);
            }

            CIBALoginUserResolver resolver = session.getProvider(CIBALoginUserResolver.class);

            if (resolver == null) {
                event.event(EventType.EZ_TRANSACTION_REQUEST).error("Login User Resolver not setup properly.");
                throw new ErrorResponseException(OAuthErrorException.INVALID_USER, "Login User Resolver not setup properly.", Response.Status.BAD_REQUEST);
            }

            CibaConfig cibaPolicy = realm.getCibaPolicy();
            String deliveryMode = cibaPolicy.getBackchannelTokenDeliveryMode(request.getClient());
            CibaNotificationConfigModel cibaNotificationConfigByProviderId = realm.getCibaNotificationConfigByProviderId(deliveryMode, request.getClient().getClientId(),
                    realm.getId());

            if (cibaNotificationConfigByProviderId == null && !request.getClient().getAuthenticationFlowBindingOverride("ciba_auth")
                    .equalsIgnoreCase("api-authorizer")) {
                event.event(EventType.EZ_TRANSACTION_REQUEST).error("Result Notification Way not configured.");
                return ClientAuthUtil.errorResponse(400, "result_notification_way_not_set", "Result Notification Way is not set for the application");
            }

            if(request.getClient().getAuthenticationFlowBindingOverride("ciba_auth")
                    .equalsIgnoreCase("api-authorizer")) {
                return getResponseFromApiConnector(request, file);
            }

            UserModel user = request.getUser();
            event.user(user);
            UserData infoUsedByAuthentication = resolver.getInfoUsedByAuthentication(user);

            if (cibaNotificationConfigByProviderId == null && !request.getClient().getAuthenticationFlowBindingOverride("ciba_auth")
                    .equalsIgnoreCase("api-authorizer")) {
                event.event(EventType.EZ_TRANSACTION_REQUEST).error("Result Notification Way not configured.");
                return ClientAuthUtil.errorResponse(400, "result_notification_way_not_set", "Result Notification Way is not set for the application");
            }

            if (provider.requestAuthentication(request, infoUsedByAuthentication, authReqId, event)) {

                storeAuthenticationRequest(request, cibaPolicy, authReqId, user);

                Map<String, Object> response = new HashMap<>();

                CibaNotificationProvider notificationProvider = session.getProvider(CibaNotificationProvider.class, deliveryMode);
                String finalVersion = version;

                TrxModel trxModel = realm.getTrx(request.getClientNotificationToken());

                Map<String, Object> result = notificationProvider.getResult(request.getClient(), cibaNotificationConfigByProviderId, new HashMap<String, Object>() {{
                    put("auth_req_id", authReqId);
                    put("version", finalVersion);
                }});

                response.put("request_id", request.getClientNotificationToken());
                response.put("success", true);
                response.put(OAuth2Constants.EXPIRES_IN, TimeUnit.MILLISECONDS.toSeconds(GeneralHelper.getEpochDuration(cibaPolicy.getExpiresInConfig(request.getClient(), realm))));
                if (result != null && result.size() > 0) {
                    response.putAll(result);
                }
                response.put("user_id", user.getUsername());
                String transactionLink = trxModel.getAttributes().get("transaction_link");
                String openInNewTab = trxModel.getAttributes().get("open_in_new_tab");
                if (transactionLink != null) {
                    response.put("transaction_link", transactionLink);
                    realm.removeTrxAttribute(request.getClientNotificationToken(), "transaction_link");
                }

                if (openInNewTab != null) {
                    response.put("open_in_new_tab", openInNewTab);
                    realm.removeTrxAttribute(request.getClientNotificationToken(), "open_in_new_tab");
                }
                String deepLink = trxModel.getAttributes().get("deep_link");
                if (deepLink != null) {
                    response.put("deep_link", deepLink);
                    realm.removeTrxAttribute(request.getClientNotificationToken(), "deep_link");
                }
                String qrCode = trxModel.getAttributes().get("qr_code");
                String qrPayload = trxModel.getAttributes().get("qr_payload");
                if (qrCode != null) {
                    response.put("qr", qrCode);
                    realm.removeTrxAttribute(request.getClientNotificationToken(), "qr_code");
                }
                if (qrPayload != null) {
                    response.put("qr_payload", qrPayload);
                    realm.removeTrxAttribute(request.getClientNotificationToken(), "qr_payload");
                }

                event.user(user);
                event.client(request.getClient().getClientId());
                event.detail("requestId", requestId);
                event.event(EventType.EZ_TRANSACTION_REQUEST_COMPLETED).success();
                return Response.ok(JsonSerialization.writeValueAsBytes(response))
                        .build();
            }
        } catch (ErrorResponseException e) {
            logger.info(e.getMessage());
            event.event(EventType.EZ_TRANSACTION_REQUEST).error(e.getErrorDescription());
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, e.getErrorDescription(), Response.Status.BAD_REQUEST);
        } catch (PersistenceException pe) {
            event.event(EventType.EZ_TRANSACTION_REQUEST).error("Discrepancies exist in the user information provided");
            pe.printStackTrace();
            throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR,
                    "Discrepancies exist in the user information provided",
                    Response.Status.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            logger.info(e.getMessage());
            event.event(EventType.EZ_TRANSACTION_REQUEST).error("Failed to send authentication request");
            e.printStackTrace();
            throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR, "Failed to send authentication request", Response.Status.SERVICE_UNAVAILABLE);
        }
        String errorMsg = "Unexpected error occured. Check Audit > logs by selecting the event type EZ_TRANSACTION for more details.";
        if (session.getAttribute("requestId") != null) {
            errorMsg = errorMsg + " RequestId - " + session.getAttribute("requestId");
        }
        throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR, errorMsg, Response.Status.SERVICE_UNAVAILABLE);
    }

    private Response getResponseFromApiConnector(CIBAAuthenticationRequest request, File file) {
        HashMap<String, Object> userDetails;
        try {
            userDetails = (HashMap<String, Object>) request.getApiRequestBody();
        } catch (ClassCastException e) {
            logger.error("failed to cast request body");
            logger.error(e.getMessage());
            return Response.status(400).build();
        }
        if (userDetails == null || userDetails.isEmpty()) {
            logger.error("improper request details");
            return Response.status(400).build();
        }
        HashMap<String, Object> requestDetails = new HashMap<>();
        String apiRequestName = request.getApiRequestName() != null ? request.getApiRequestName() : "";
        String providerName;
        try {
            providerName = GeneralHelper.getProviderName(session, apiRequestName);
        } catch (AccessControlException e){
            return Response.status(403).entity(e).build();
        }

        requestDetails.put("userDetails", userDetails);
        requestDetails.put("requestId", userDetails.get("requestId"));
        requestDetails.put("providerName", providerName);
        requestDetails.put("workspaceId", session.getContext().getRealm().getId());
        requestDetails.put("fromEzto", true);
        Optional<RequiredActionFactory> optionalRequiredActionFactory = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(RequiredActionProvider.class)
                .map(RequiredActionFactory.class::cast)
                .filter(x->x.getApiMappingName() != null && Arrays.asList(x.getApiMappingName()).contains(apiRequestName))
                .findFirst();
        if(optionalRequiredActionFactory.isPresent()) {
            logger.info("required action found for the verification " + apiRequestName);
            RequiredActionFactory requiredActionFactory = optionalRequiredActionFactory.get();
            List<String> stripeIds = Arrays.asList(requiredActionFactory.stripeMappingId());
            String stripeId;
            if(("penny_drop").equals(apiRequestName) || ("penny_less").equals(apiRequestName)){
                stripeId = ("penny_drop").equals(apiRequestName) ? "bank_account_verification_penny_drop" : "bank_account_verification";
                logger.info("stipe id is being added for bank verification - " + stripeId);
            } else {
                logger.info("penny less is being added as stipe id");
                stripeId = stripeIds.isEmpty() ? "" : stripeIds.get(0);
                logger.info("stipe id is being added - " + stripeId);
            }
            if(requiredActionFactory.getApiMappingPrice() != null){
                logger.info("multi api verification triggered");
                if(Arrays.asList(requiredActionFactory.getApiMappingPrice()).contains(apiRequestName)) {
                    logger.info("pricing added for " + apiRequestName);
                    requestDetails.put("pricing", GeneralHelper.getPricingDetails(session, providerName, stripeId));
                }
                else {
                    logger.info("pricing set to zero for " + apiRequestName);
                    Map<String, Object> pricingDetails = new HashMap<>();
                    pricingDetails.put("currency", "INR");
                    pricingDetails.put("amount", "0.00");
                    requestDetails.put("pricing", pricingDetails);
                }
            } else {
                requestDetails.put("pricing", GeneralHelper.getPricingDetails(session, providerName, stripeId));
            }
        } else {
            logger.info("required action not found for the verification " + apiRequestName + ", not adding pricing key");
        }


        HttpUtility httpUtility = new HttpUtility();
        String url = EnvironmentReader.getValue("API_CONNECTOR_HOST_URL", "http://localhost:8081");
        url = url + "/api/" + apiRequestName + "/verify";
        try {
            okhttp3.Response res = null;
            if (file == null) {
                res = httpUtility.callRequest(
                        MediaType.APPLICATION_JSON, "POST", url, JsonSerialization.writeValueAsString(requestDetails), null, null, 1, false);
            } else {
                userDetails.forEach((key,value) -> requestDetails.put(key, value));
                res = httpUtility.callRequest(file, url, requestDetails);
            }
            if (res == null) {
                logger.error("request to api connector failed with status code - " + 400);
                return Response.status(400).entity(API_GENERIC_ERROR).build();
            }
            if (res.isSuccessful()) {
                return Response.status(200).entity(res.body().string()).build();
            } else {
                logger.error("request to api connector failed with status code - " + res.code());
                String errorResponse = res.body() != null ? res.body().string() : "request failed in api-connector";
                logger.error(errorResponse);

                return Response.status(400).entity(errorResponse).build();
            }
        } catch(Exception e){
            logger.error(API_GENERIC_ERROR);
            logger.error(e.getStackTrace());
            return Response.status(400).entity(API_GENERIC_ERROR).build();
        }
    }


    /**
     * TODO: Leverage the device code storage for tracking authentication requests. Not sure if we need a specific storage,
     * but probably make the {@link OAuth2DeviceTokenStoreProvider} more generic for ciba, device, or any other use case
     * that relies on cross-references for unsolicited user authentication requests from devices.
     */
    private void storeAuthenticationRequest(CIBAAuthenticationRequest request, CibaConfig cibaConfig, String authReqId, UserModel user) {
        ClientModel client = request.getClient();
        long expiresIn = cibaConfig.getExpiresIn(request.getClient(), realm);
        int poolingInterval = cibaConfig.getPoolingInterval(request.getClient());
        String cibaMode = cibaConfig.getBackchannelTokenDeliveryMode(client);

        // Set authReqId just for the ping mode as it is relatively big and not necessarily needed in the infinispan cache for the "poll" mode
        if (CIBA_POLL_MODE.equals(cibaMode)) {
            authReqId = null;
        }

        OAuth2DeviceCodeModel deviceCode = OAuth2DeviceCodeModel.create(realm, client,
                request.getId(), request.getScope(), null, expiresIn, poolingInterval, request.getClientNotificationToken(), authReqId,
                Collections.emptyMap(), null, null);
        String authResultId = request.getAuthResultId();
        OAuth2DeviceUserCodeModel userCode = new OAuth2DeviceUserCodeModel(realm, deviceCode.getDeviceCode(),
                authResultId);

        if (!realm.hasTrxValueAttribute(deviceCode.getClientNotificationToken(), WEB_TO_DEVICE) && !Boolean.parseBoolean(realm.getTrx(deviceCode.getClientNotificationToken()).getAttributes().get(WEB_TO_DEVICE))) {
            realm.addTransactionStatus(deviceCode.getClientNotificationToken(), user.getUsername(), UserTransactionStatus.PENDING.getDisplayName(), deviceCode.getExpiration());
            realm.removeTrxAttribute(deviceCode.getClientNotificationToken(), WEB_TO_DEVICE);
        }

        if (client.getAttributes().get(CIBA_BACKCHANNEL_DEAD_LETTER_HOOK_DELIVERY_MODE_PER_CLIENT) != null) {
            event.event(EventType.EZ_DEAD_LETTER_HOOK).detail("requestId", deviceCode.getClientNotificationToken()).success();
        }

        // To inform "expired_token" to the client, the lifespan of the cache provider is longer than device code
        long lifespanSeconds = expiresIn + poolingInterval + 10;

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);

        store.put(deviceCode, userCode, lifespanSeconds);
    }

    private CIBAAuthenticationRequest authorizeClient(MultivaluedMap<String, String> params, String requestId, String version) {
        ClientModel client = null;
        try {
            client = authenticateClient();
        } catch (WebApplicationException wae) {
            OAuth2ErrorRepresentation errorRep = (OAuth2ErrorRepresentation) wae.getResponse().getEntity();
            throw new ErrorResponseException(errorRep.getError(), errorRep.getErrorDescription(), Response.Status.UNAUTHORIZED);
        }
        BackchannelAuthenticationEndpointRequest endpointRequest = null;
        try {
            endpointRequest = BackchannelAuthenticationEndpointRequestParserProcessor.parseRequest(session, client, params, realm.getCibaPolicy(), event);

            if (client.getAttributes().get(CIBA_BACKCHANNEL_DEAD_LETTER_HOOK_DELIVERY_MODE_PER_CLIENT) != null &&
                    endpointRequest.getUser().getMetadata().containsKey("sp@request_id")
            ) {
                event.event(EventType.EZ_DEAD_LETTER_HOOK_TRIGGER).detail("requestId", requestId).success();
            }

        } catch (IllegalArgumentException e) {
            event.event(EventType.EZ_TRANSACTION_REQUEST).error("Encryption is enabled under developers tab, but the request body is not encrypted.");
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, e.getCause().getMessage(), Response.Status.BAD_REQUEST);
        }

        if (Boolean.parseBoolean(client.getAttributes().get("ciba.consent"))) {
            BindingMessage bindingMessage = endpointRequest.getBindingMessage();
            if (bindingMessage == null) {
                throwConsentError();
            }
            Consent consent = bindingMessage.getConsent();
            if (consent == null || consent.getType() == null || consent.getTitle() == null || consent.getMessage() == null) {
                throwConsentError();
            }
        }
        
        UserModel user = null;

        if(!client.getAuthenticationFlowBindingOverrides().get("ciba_auth").equals("api-authorizer")) {
            user = resolveUser(endpointRequest, DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT, requestId);
        }

        CIBAAuthenticationRequest request = new CIBAAuthenticationRequest(session, user, client);

        request.setClient(client);
        request.setApiRequestName(endpointRequest.getApiRequestName());
        request.setApiRequestBody(endpointRequest.getApiRequestBody());
        String cibaAuthorizer = client.getAuthenticationFlowBindingOverride(CIBA_AUTHORIZER);
        CibaAuthProviderFactory cibaAuthProviderFactory = (CibaAuthProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaAuthProvider.class, cibaAuthorizer);
        if (cibaAuthProviderFactory.isTemplateSupported()) {
            if (client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.ACTION_BINDING) == null) {
                event.event(EventType.EZ_TRANSACTION_REQUEST_ERROR).error("Workflow is not set for the application");
                throw new ErrorResponseException("invalid_app_setting", "Workflow is not set for the application", Response.Status.BAD_REQUEST);
            }
        }

        List<String> requiredActionsInFlow = realm.getRequiredActionProvidersStream(client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.ACTION_BINDING))
                .map(RequiredActionProviderModel::getProviderId).collect(Collectors.toList());

        session.getKeycloakSessionFactory().getProviderFactoriesStream(RequiredActionProvider.class)
                .filter(factory -> requiredActionsInFlow.contains(factory.getId()))
                .forEach(factory -> {
                    if (!Arrays.asList(((RequiredActionFactory) factory).supportedRegions()).contains(realm.getDefaultRegion())) {
                        if (!Arrays.asList(((RequiredActionFactory) factory).supportedRegions()).contains("all"))
                            throw new ErrorResponseException("unsupported_template", "Template associated with the application is not supported in current region", Response.Status.BAD_REQUEST);
                    }
                });

        String scope = "openid";
        request.setScope(scope);

        // optional parameters
        if (endpointRequest.getBindingMessage() != null) {
            request.setBindingMessage(endpointRequest.getBindingMessage());
        }

        CibaConfig policy = realm.getCibaPolicy();

        // create JWE encoded auth_req_id from Auth Req ID.
        long expiresIn = policy.getExpiresIn(client, realm);

        request.exp(request.getIat() + expiresIn);
        realm.addAttribute(requestId, "linkExp", String.valueOf(request.getExp()));

        StringBuilder scopes = new StringBuilder(Optional.ofNullable(request.getScope()).orElse(""));
        client.getClientScopes(true)
                .forEach((key, value) -> {
                    if (value.isDisplayOnConsentScreen())
                        scopes.append(" ").append(value.getName());
                });
        request.setScope(scopes.toString());

        request.setClientNotificationToken(requestId);

        if (endpointRequest.getUserCode() != null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "User code not supported",
                    Response.Status.BAD_REQUEST);
        }

        extractAdditionalParams(endpointRequest, request);

        try {
            session.clientPolicy().triggerOnEvent(new BackchannelAuthenticationRequestContext(endpointRequest, request, params));
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        return request;
    }

    private void throwConsentError() {
        event.event(EventType.EZ_TRANSACTION_REQUEST).error("This application has consent functionality enabled, but the request contains invalid consent information." +
                " The consent data must include a valid type, title, and message.");
        throw new ErrorResponseException(
                "invalid consent",
                "This application has consent functionality enabled, but the request contains invalid consent information." +
                        " The consent data must include a valid type, title, and message.",
                Response.Status.BAD_REQUEST
        );
    }

    protected void extractAdditionalParams(BackchannelAuthenticationEndpointRequest endpointRequest, CIBAAuthenticationRequest request) {
        for (String paramName : endpointRequest.getAdditionalReqParams().keySet()) {
            request.setOtherClaims(paramName, endpointRequest.getAdditionalReqParams().get(paramName));
        }
    }

    protected void validateBindingMessage(String bindingMessage) {
        if (!BINDING_MESSAGE_VALIDATION.matcher(bindingMessage).matches()) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_BINDING_MESSAGE, "the binding_message value has to be max 50 characters in length and must contain only basic plain-text characters without spaces",
                    Response.Status.BAD_REQUEST);
        }
    }

    private UserModel resolveUser(BackchannelAuthenticationEndpointRequest endpointRequest, String authRequestedUserHint, String requestId) {
        CIBALoginUserResolver resolver = session.getProvider(CIBALoginUserResolver.class);

        if (resolver == null) {
            throw new RuntimeException("CIBA Login User Resolver not setup properly.");
        }

        UserData userHint = endpointRequest.getUser();
        UserModel user;
        user = resolver.setEvent(event).setRequestId(requestId).getUserFromLoginHint(userHint);

        if (user == null || !user.isEnabled()) {
            event.error("Invalid user");
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "invalid user", Response.Status.BAD_REQUEST);
        }

        CibaAuthConfigModel cibaAuthConfigModel = realm.getCibaAuthConfigByClientId(endpointRequest.getAdditionalReqParams().get("client_id"), realm.getId());
        ClientModel client = realm.getClientByClientId(endpointRequest.getAdditionalReqParams().get("client_id"));
        if (client == null) {
            client = realm.getClientBySandboxClientId(endpointRequest.getAdditionalReqParams().get("client_id"));
            cibaAuthConfigModel = realm.getCibaAuthConfigByClientId(client.getClientId(), realm.getId());
        }

        String cibaAuthorizer = client.getAuthenticationFlowBindingOverride(CIBA_AUTHORIZER);
        CibaAuthProvider cibaAuthProvider = session.getProvider(CibaAuthProvider.class, cibaAuthorizer);
        cibaAuthProvider.setEventBuilder(event);

        if (!cibaAuthProvider.validateUserDetails(user, realm, cibaAuthConfigModel, requestId)) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST,
                    "Invalid details. Check Audit > logs for more details.", Response.Status.BAD_REQUEST);
        }
        return user;
    }

    public static String getBody(HttpRequest request) throws IOException {

        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        body = stringBuilder.toString();
        return body;
    }
}

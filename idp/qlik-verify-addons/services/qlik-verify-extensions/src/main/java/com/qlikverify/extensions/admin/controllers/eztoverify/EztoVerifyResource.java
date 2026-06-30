package com.qlikverify.extensions.admin.controllers.eztoverify;

import com.qlikverify.constants.APIErrorCodes;
import com.qlikverify.exceptions.APIException;
import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.extensions.admin.misc.helpers.GeneralHelper;
import com.qlikverify.models.ManagementKeysPermissions;
import com.qlikverify.responses.APIResponse;
import liquibase.pro.packaged.S;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.authentication.authenticators.client.ClientAuthUtil;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class EztoVerifyResource extends BaseAuthCheckResource {

    private static final Logger logger = Logger.getLogger(EztoVerifyResource.class);
    public static final String ACCESS_KEY_NAME = "accessKeyName";
    public static final String API_VERSION = "apiVersion";
    KeycloakSession session;

    private static final String MESSAGE = "message";

    public EztoVerifyResource(KeycloakSession session) {
        super(session);
        this.session = session;
    }

    @GET
    @Path("{realmid}/users/{userid}/devices")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevices(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmid") String realmId, @PathParam("userid") String userId) throws APIException {

        Map<String, String> attrs = isValid(req, realmId, ManagementKeysPermissions.LIST);

        UserModel userModel = isValidUser(userId, attrs);

        List<CredentialRepresentation> reps = session.userCredentialManager().getStoredCredentialsStream(session.getContext().getRealm(), userModel)
                .filter(x -> !x.isTemporary())
                .map(ModelToRepresentation::toRepresentation).filter(Objects::nonNull).map(x -> {
                    x.setId(null);
                    return x;
                }).collect(Collectors.toList());

        if (reps.isEmpty()) {
            return ClientAuthUtil.errorResponse(404, APIErrorCodes.ez_devices_1005.value, "No device(s) available for this user");
        }

        Map<String, List<CredentialRepresentation>> devices = new HashMap();
        List<CredentialRepresentation> mobileDevices = reps.stream()
                .filter(credentialRepresentation -> credentialRepresentation.getDevicePlatform() != null)
                .filter(credentialRepresentation -> credentialRepresentation.getDevicePlatform().equalsIgnoreCase("android")
                        || credentialRepresentation.getDevicePlatform().equalsIgnoreCase("ios"))
                .collect(Collectors.toList());

        if (!mobileDevices.isEmpty())
            devices.put("mobile_devices", mobileDevices);

        List<CredentialRepresentation> desktopDevices = reps.stream()
                .filter(credentialRepresentation -> credentialRepresentation.getDevicePlatform() != null)
                .filter(credentialRepresentation -> !credentialRepresentation.getDevicePlatform().equalsIgnoreCase("android")
                        && !credentialRepresentation.getDevicePlatform().equalsIgnoreCase("ios"))
                .collect(Collectors.toList());
        if (!desktopDevices.isEmpty())
            devices.put("desktop_devices", desktopDevices);

        if (devices.isEmpty()) {
            return ClientAuthUtil.errorResponse(404, APIErrorCodes.ez_devices_1005.value, "No device(s) available for this user");
        }

        return Response.status(Response.Status.OK).entity(devices).build();
    }

    @DELETE
    @Path("{realmid}/users/{userid}/devices/{deviceid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDevice(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmid") String realmId, @PathParam("userid") String userId, @PathParam("deviceid") String deviceId) throws APIException {
        Map<String, String> attrs = isValid(req, realmId, ManagementKeysPermissions.REMOVE);

        UserModel userModel = isValidUser(userId, attrs);

        CredentialRepresentation credModel = isValidDevice(deviceId, userModel, attrs);

        boolean isRemoved = session.userCredentialManager().removeStoredCredential(session.getContext().getRealm(), userModel, credModel.getId());

        if (!isRemoved) {
            auditAdminEvent(userModel, deviceId, "Invalid device details", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            return ClientAuthUtil.errorResponse(304, APIErrorCodes.ez_devices_1006.value, "Device cannot be removed");
        }

        auditAdminEvent(userModel, deviceId, "Device removed successfully", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
        APIResponse apiResponse = new APIResponse();
        apiResponse.getResponse().put(MESSAGE,"Device removed successfully");
        return Response.status(200).entity(apiResponse.getResponse()).build();
    }

    @PUT
    @Path("{realmid}/users/{userid}/devices/{deviceid}/enable")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enableDevice(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmid") String realmId, @PathParam("userid") String userId, @PathParam("deviceid") String deviceId) throws APIException {
        Map<String, String> attrs = isValid(req, realmId, ManagementKeysPermissions.ENABLE);

        UserModel userModel = isValidUser(userId, attrs);

        CredentialRepresentation credModel = isValidDevice(deviceId, userModel, attrs);

        CredentialModel credentialModel = session.userCredentialManager().getStoredCredentialById(session.getContext().getRealm(), userModel, credModel.getId());

        if (credentialModel == null) {
            auditAdminEvent(null, null, APIErrorCodes.ez_devices_1005.value + ": No devices available for this user", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            throw new APIException(404, APIErrorCodes.ez_devices_1005.value, "No device(s) available for this user", null);
        }

        if (credentialModel.isEnabled()) {
            auditAdminEvent(userModel, deviceId, "Device already enabled", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            return Response.status(Response.Status.NOT_MODIFIED).entity("Device already enabled").build();
        }

        credentialModel.setEnabled(true);
        session.userCredentialManager().updateCredential(session.getContext().getRealm(), userModel, credentialModel);
        auditAdminEvent(userModel, deviceId, "Device enabled successfully", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
        APIResponse apiResponse = new APIResponse();
        apiResponse.getResponse().put(MESSAGE,"Device enabled successfully");
        return Response.status(200).entity(apiResponse.getResponse()).build();
    }

    @PUT
    @Path("{realmid}/users/{userid}/devices/{deviceid}/disable")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response disableDevice(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmid") String realmId, @PathParam("userid") String userId, @PathParam("deviceid") String deviceId) throws APIException {
        Map<String, String> attrs = isValid(req, realmId, ManagementKeysPermissions.DISABLE);

        UserModel userModel = isValidUser(userId, attrs);

        CredentialRepresentation credModel = isValidDevice(deviceId, userModel, attrs);

        CredentialModel credentialModel = session.userCredentialManager().getStoredCredentialById(session.getContext().getRealm(), userModel, credModel.getId());

        if (credentialModel == null) {
            auditAdminEvent(null, null, APIErrorCodes.ez_devices_1005.value + ": No device(s) available for this user", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            throw new APIException(404, APIErrorCodes.ez_devices_1005.value, "No device(s) available for this user", null);
        }
        if (!credentialModel.isEnabled()) {
            auditAdminEvent(userModel, deviceId, "Device already disabled", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            return Response.status(Response.Status.NOT_MODIFIED).entity("Device already disabled").build();
        }

        credentialModel.setEnabled(false);
        session.userCredentialManager().updateCredential(session.getContext().getRealm(), userModel, credentialModel);
        auditAdminEvent(userModel, deviceId, "Device disabled successfully", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
        APIResponse apiResponse = new APIResponse();
        apiResponse.getResponse().put(MESSAGE,"Device disabled successfully");
        return Response.status(200).entity(apiResponse.getResponse()).build();
    }

    @PUT
    @Path("{realmid}/users/{userid}/devices/{deviceid}/mark-as-last-used")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response markAsLastUsed(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmid") String realmId, @PathParam("userid") String userId, @PathParam("deviceid") String deviceId) throws APIException {
        Map<String, String> attrs = isValid(req, realmId, ManagementKeysPermissions.MARK_AS_LAST_USED);

        UserModel userModel = isValidUser(userId, attrs);

        CredentialRepresentation credModel = isValidDevice(deviceId, userModel, attrs);

        Optional<CredentialModel> lastUsedDevice = session.userCredentialManager().getStoredCredentialsStream(session.getContext().getRealm(), userModel)
                .filter(x -> !x.isTemporary() && x.isEnabled() && x.getType().equals("DEVICE_TOKEN")).
                filter(CredentialModel::isEnabled).min((cred1, cred2) -> {
                    Long lastUsed1 = cred1.getLastUsed() != null ? cred1.getLastUsed() : cred1.getCreatedDate();
                    Long lastUsed2 = cred2.getLastUsed() != null ? cred2.getLastUsed() : cred2.getCreatedDate();
                    return Long.compare(lastUsed2, lastUsed1);
                }).stream().findFirst();


        CredentialModel credentialModel = session.userCredentialManager().getStoredCredentialById(session.getContext().getRealm(), userModel, credModel.getId());

        if (credentialModel == null) {
            auditAdminEvent(null, null, APIErrorCodes.ez_devices_1005.value + ": No device(s) available for this user", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            throw new APIException(404, APIErrorCodes.ez_devices_1005.value, "No device(s) available for this user", null);
        }

        if (!credentialModel.isEnabled()) {
            auditAdminEvent(userModel, deviceId, "The device is disabled. Unable to perform requested action.", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            throw  new APIException(400, APIErrorCodes.ez_devices_1008.value, "The device is disabled. Unable to perform requested action", null);
        }

        if (lastUsedDevice.isPresent() && lastUsedDevice.get().getId().equals(credentialModel.getId())) {
            auditAdminEvent(userModel, deviceId, "Device is already marked as last used.", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            return Response.status(Response.Status.NOT_MODIFIED).entity("Device is already marked as last used").build();
        }

        credentialModel.setLastUsed(Time.currentTimeMillis());
        session.userCredentialManager().updateCredential(session.getContext().getRealm(), userModel, credentialModel);
        auditAdminEvent(userModel, deviceId, "Device marked as last used successfully", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
        APIResponse apiResponse = new APIResponse();
        apiResponse.getResponse().put(MESSAGE,"Device marked as last used successfully");
        return Response.status(200).entity(apiResponse.getResponse()).build();
    }


    private CredentialRepresentation isValidDevice(String deviceId, UserModel userModel, Map<String, String> attrs) throws APIException {
        if (deviceId == null || deviceId.equals("")) {
            auditAdminEvent(null, null, APIErrorCodes.ez_devices_1005.value + ": No device(s) available for this user", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            throw new APIException(400, APIErrorCodes.ez_devices_1005.value, "No device(s) available for this user", null);
        }
        Optional<CredentialRepresentation> credModel = session.userCredentialManager().getStoredCredentialsStream(session.getContext().getRealm(), userModel)
                .filter(x -> !x.isTemporary() && !x.getType().equals("password") && !x.getType().equals("MFA_RECOVERY") && !x.getType().equals("otp"))
                .map(ModelToRepresentation::toRepresentation).filter(x -> x != null && x.getDeviceId() != null && x.getDeviceId().equals(deviceId)).findFirst();

        if (!credModel.isPresent()) {
            auditAdminEvent(null, null, APIErrorCodes.ez_devices_1005.value + ": No device(s) available for this user", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            throw new APIException(404, APIErrorCodes.ez_devices_1005.value, "No device(s) available for this user", null);
        }

        return credModel.get();
    }

    private UserModel isValidUser(String userId, Map<String, String> attrs) throws APIException {
        if (userId == null || userId.equals("")) {
            auditAdminEvent(null, null, APIErrorCodes.ez_devices_1004.value + ": Invalid user", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            throw new APIException(400, APIErrorCodes.ez_devices_1004.value, "Invalid user", null);
        }

        UserModel userModel = session.users().getUserByUsername(session.getContext().getRealm(), userId);
        if (userModel == null) {
            auditAdminEvent(null, null, APIErrorCodes.ez_devices_1004.value + ": Invalid user", attrs.get(API_VERSION), attrs.get(ACCESS_KEY_NAME));
            throw new APIException(400, APIErrorCodes.ez_devices_1004.value, "Invalid user", null);
        }

        return userModel;
    }

    private Map<String, String> isValid(HttpRequest httpRequest, String realmId, ManagementKeysPermissions request) throws APIException {

        if (realmId == null) {
            throw new APIException(400, APIErrorCodes.ez_devices_1000.value, "Invalid workspace", null);
        }

        if (httpRequest == null) {
            throw new APIException(400, APIErrorCodes.ez_devices_1001.value, "Request has no Authorization in header", null);
        }

        if (httpRequest.getHttpHeaders() == null || httpRequest.getHttpHeaders().getRequestHeader("Authorization") == null || httpRequest.getHttpHeaders().getRequestHeader("Authorization").isEmpty()) {
            throw new APIException(401, APIErrorCodes.ez_devices_1001.value, "Request has no Authorization in header", null);
        }

        String authorization = httpRequest.getHttpHeaders().getRequestHeader("Authorization").get(0);

        if (authorization == null || authorization.isEmpty()) {
            throw new APIException(401, APIErrorCodes.ez_devices_1001.value, "Request has no Authorization in header", null);
        }

        String version = httpRequest.getHttpHeaders().getHeaderString("api-version");
        if (version == null)
            version = "0";

        RealmModel realmModel = session.realms().getRealm(realmId);

        if (realmModel == null) {
            throw new APIException(400, APIErrorCodes.ez_devices_1000.value, "Invalid workspace", null);
        }

        session.getContext().setRealm(realmModel);
        Map<String, String> attrs;
        if (version.equals("1")) {

            Optional<MgmtAccessModel> isValid = realmModel.getMgmtAccessKeys().filter(x -> {
                String authToken = (authorization.replace("Bearer","").trim());
                return (x.getSecret().equals(authToken) || (x.getSandboxSecret() != null && x.getSandboxSecret().equals(authToken)))
                        && x.getPermissions().containsKey("Devices@" + request.getName())
                        && x.getPermissions().get("Devices@" + request.getName()).booleanValue()
                        && x.isEnabled();
                    }).findFirst();

            if (!isValid.isPresent()) {
                throw new APIException(401, APIErrorCodes.ez_devices_1002.value, "Permission denied", null);
            }

            MgmtAccessModel accessModel = isValid.get();
            accessModel.setLastUsedDate(Time.currentTimeMillis());

            realmModel.updateMgmtAccess(accessModel);
            attrs = new HashMap<>();
            attrs.put(ACCESS_KEY_NAME, accessModel.getName());
            attrs.put(API_VERSION, "1");
        } else {
            String decoded = decodeBasicAuthorization(authorization);
            String[] splited = decoded.split(":");
            if (splited.length != 2) {
                throw new APIException(401, APIErrorCodes.ez_devices_1003.value, "Invalid Authorization header", null);
            }

            ClientModel clientModel = session.getContext().getRealm().getClientByClientId(splited[0]);
            if(clientModel == null) {
                clientModel = session.getContext().getRealm().getClientBySandboxClientId(splited[0]);
            }
            if (clientModel == null ) {
                throw new APIException(401, APIErrorCodes.ez_devices_1003.value, "Invalid Authorization header", null);
            }

            if (!GeneralHelper.isV0Supported(realmModel)) {
                throw new APIException(401, APIErrorCodes.ez_devices_1007.value, "Authentication method not supported. Please use v1 authentication.", null);
            }

            if (!clientModel.getSecret().equals(splited[1]) && !clientModel.getSandboxSecret().equals(splited[1])) {
                throw new APIException(401, APIErrorCodes.ez_devices_1003.value, "Invalid Authorization header", null);
            }
            session.getContext().setClient(clientModel);
            attrs = new HashMap<>();
            attrs.put(API_VERSION, "0");
        }

        return attrs;

    }

    private String decodeBasicAuthorization(String authorizationHeader) {
        // Extract encoded credentials part after "Basic "
        String encodedCredentials = authorizationHeader.split("\\s+")[1];

        // Decode using Base64
        byte[] decodedBytes = Base64.getDecoder().decode(encodedCredentials);

        // Convert to String using UTF-8 encoding
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private void auditAdminEvent(UserModel userModel, String deviceId, String operation, String apiVersion, String accessKeyName) {
        AdminEventBuilder adEvt = new AdminEventBuilder(session.getContext().getRealm(),
                new AdminAuth(session.getContext().getRealm(),
                        null, null, session.getContext().getRealm().getClientByClientId("dashboard")), session,
                session.getContext().getConnection());

        Map<String, Object> attrs = new HashMap<>();
        if (userModel != null) {
            attrs.put("userId", userModel.getUsername());
        }
        attrs.put("deviceId", deviceId);
        attrs.put(MESSAGE, operation);
        if (session.getContext().getClient() != null) {
            attrs.put("clientID", session.getContext().getClient().getClientId());
        }
        if (apiVersion == null)
            attrs.put("api-version", "0");
        else
            attrs.put("api-version", "1");

        if (accessKeyName != null)
            attrs.put("mgmtAccessKeyUsed", accessKeyName);


        adEvt.realm(session.getContext().getRealm());
        adEvt.operation(OperationType.ACTION)
                .resource(ResourceType.USER)
                .resourcePath(session.getContext().getUri(),
                        session.clients().getClientByClientId(session.getContext().getRealm(), "dashboard").getId())
                .representation(attrs).success();
    }
}

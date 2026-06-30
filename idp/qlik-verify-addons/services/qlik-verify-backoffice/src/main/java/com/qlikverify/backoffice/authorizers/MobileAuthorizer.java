package com.qlikverify.backoffice.authorizers;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.qlikverify.backoffice.GeneralHelper;
import com.qlikverify.backoffice.authorizers.helpers.FirebaseHelper;
import com.qlikverify.backoffice.authorizers.helpers.HuaweiHelper;
import com.qlikverify.backoffice.authorizers.utility.TokenCredentialUtility;
import com.qlikverify.backoffice.controllers.DeepLinkingResource;
import com.qlikverify.backoffice.creds.device.TokenCredentialModel;
import com.qlikverify.backoffice.creds.device.TokenSecretData;
import com.qlikverify.extensions.admin.controllers.actionToken.EztoActionTokenVerifier;
import com.qlikverify.services.theme.downloads.EztoVrfyAndroidDownloadProvider;
import com.qlikverify.services.theme.downloads.EztoVrfyIOSDownloadProvider;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.OAuthErrorException;
import org.keycloak.ciba.AuthenticationChannelRequest;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.ciba.CibaAuthProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.grants.ciba.AbstractCibaAuthorizer;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.AESHelper;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;
import static org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper.updateResult;

public class MobileAuthorizer extends AbstractCibaAuthorizer implements CibaAuthProvider, CibaAuthProviderFactory {

    private static final Logger logger = Logger.getLogger(MobileAuthorizer.class);
    public static final String ENCRYPTION_ENABLE = "encryption-enable";
    public static final String PROXIMITY_ENABLE = "proximity-enable";
    public static final String INTEGRATION_TYPE = "integration-type";
    public static final String PUSH_TOKEN = "push_token";
    public static final String PUSH_TYPE = "push_type";
    public static final String TAG = "MobileAuthorizer: ";
    private static final String REQUEST_ID = "requestId";
    EztoVrfyAndroidDownloadProvider eztoVrfyAndroidDownloadProvider = new EztoVrfyAndroidDownloadProvider();
    EztoVrfyIOSDownloadProvider eztoVrfyIOSDownloadProvider = new EztoVrfyIOSDownloadProvider();

    private static final List<String> supportedMobilePushTypes = new ArrayList<>();


    static {
        supportedMobilePushTypes.add("hms");
        supportedMobilePushTypes.add("fcm");
    }

    @Override
    public CibaAuthProvider setEventBuilder(EventBuilder eventBuilder) {
        this.event = eventBuilder;
        return this;
    }

    @Override
    public boolean onAuthorize(ClientModel clientModel, RealmModel realm, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, AuthenticationChannelRequest request, CibaStateModel state) {
        try {
            if (request.getBindingMessage() != null) {
                realm.addAttribute(state.getRequestId(), "hidden_ua_" + BINDING_MESSAGE,
                        JsonSerialization.writeValueAsString(request.getBindingMessage()));
            }
        } catch (IOException e) {
            logger.error("Failed to parse binding message");
            throw new RuntimeException(e);
        }
        String oidcUrl = getSecuredUrl(session, clientModel, cibaAuthConfigModel, state.getCibaStateId(), getId(), request);
        if (oidcUrl == null)
            return false;

        String registerNewDevice = realm.getTrxValue(state.getRequestId(),"sp@"+REGISTER_NEW_DEVICE);
        boolean isNewDeviceRegistration = Boolean.parseBoolean(registerNewDevice);
        String notifyWay = cibaAuthConfigModel.getConfig().get(PUSH_WAY);
        if (!isNewDeviceRegistration && notifyWay.equals(CIBA_PUSH)) {
            try {
                String integrationType = cibaAuthConfigModel.getConfig().get(INTEGRATION_TYPE);
                boolean isFirebasePossible = triggerPush(userModel, realm, cibaAuthConfigModel, oidcUrl, (String) request.getAdditionalParameters().get(REQUEST_ID));

                if (!isFirebasePossible &&
                        (integrationType.equalsIgnoreCase(EZTO_VERIFY_APP) ||
                                Boolean.parseBoolean(cibaAuthConfigModel.getConfig().get(AUTOMATIC_FALLBACK)))) {
                    return triggerQR(realm, cibaAuthConfigModel, request, oidcUrl, state.getRequestId());
                } else if(!isFirebasePossible){
                    throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST,
                            "Failed to send push notification. Please select QR as a fallback", Response.Status.BAD_REQUEST);
                }
                return isFirebasePossible;
            } catch (InterruptedException e) {
                logger.error("Error occurred in Mobile Authorizer: " + e.getMessage());
                return false;
            }
        }

        return triggerQR(realm, cibaAuthConfigModel, request, oidcUrl, state.getRequestId());
    }

    private boolean triggerQR(RealmModel realmModel, CibaAuthConfigModel cibaAuthConfigModel, AuthenticationChannelRequest request, String oidcUrl, String requestId) {
        HashMap<String, Object> result = null;
        try {
            result = getPayload(cibaAuthConfigModel, oidcUrl, (String) request.getAdditionalParameters().get(REQUEST_ID));
            if (result == null)
                return false;
        } catch (Exception e) {
            event.detail(REQUEST_ID, (String) request.getAdditionalParameters().get(REQUEST_ID)).error(TAG + e.getMessage());
            return false;
        }

        String qrCode = null;
        try {
            qrCode = qrCode(JsonSerialization.writeValueAsString(result));
            if (qrCode == null)
                return false;
            if (realmModel.hasTrxValueAttribute(requestId, "include_qr_payload") && Boolean.parseBoolean(realmModel.getTrxValue(requestId, "include_qr_payload"))) {
                realmModel.addAttribute(requestId, "qr_payload", JsonSerialization.writeValueAsString(result));
            }
        } catch (IOException e) {
            event.detail(REQUEST_ID, (String) request.getAdditionalParameters().get(REQUEST_ID)).error(TAG + e.getMessage());
            return false;
        }

        realmModel.addAttribute(requestId, "qr_code", "data:image/png;base64, " + qrCode);

        CibaAuthConfigModel cibaAuthConfigByClientId = realmModel.getCibaAuthConfigByClientId(session.getContext().getClient().getClientId(), realmModel.getId());
        if(cibaAuthConfigByClientId == null){
            return false;
        }

        EztoActionTokenVerifier eztoActionTokenVerifier = new EztoActionTokenVerifier(session, session.getContext().getRealm());
        Long tokenExpiry = cibaAuthConfigByClientId.getConfig().get("verify-expiry-time") != null ?  org.keycloak.utils.GeneralHelper.getEpochDuration(cibaAuthConfigByClientId.getConfig().get("verify-expiry-time")) : System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);
        String actionToken = eztoActionTokenVerifier.generateToken(session, tokenExpiry, requestId, "deep-link");

        if(cibaAuthConfigModel.getConfig().get(INTEGRATION_TYPE).equals(SDK)){
            if(Boolean.parseBoolean(cibaAuthConfigModel.getConfig().get(DEEP_LINKING)) && cibaAuthConfigModel.getConfig().get(PLAY_STORE_LINK) != null && cibaAuthConfigModel.getConfig().get(APP_STORE_LINK) != null){
                String deepLinkUrl = "id=" + requestId + "&base_url=" + session.getContext().getAuthServerUrl().toString() + "realms/api/deeplink/payload" + "&action_token=" + actionToken + "&realm_id=" + realmModel.getId();
                String appUrl = cibaAuthConfigModel.getConfig().get(APP_STORE_LINK);
                String playStoreUrl = cibaAuthConfigModel.getConfig().get(PLAY_STORE_LINK);
                realmModel.addAttribute(requestId, "deep_link", session.getContext().getAuthServerUrl().toString() + "deeplink.html?" + deepLinkUrl + "&app_url=" + appUrl + "&windows_url=" + playStoreUrl);
            }
        }else{
            String env = "";
            String domainName = "";
            if(EnvironmentReader.getValue("ENV") != null){
                env = EnvironmentReader.getValue("ENV");
                env = env.equals("prod") ? "" : env + ".";
            }
            if (realmModel.getCustomDomainMappings().count() > 0 && realmModel.getCustomDomainMappings().filter(x->x.isActive()).findFirst().isPresent()){
                String hostName = session.getContext().getUri().getBaseUri().getAuthority();
                domainName = "https://" + realmModel.getCustomDomainMappingByDomain(hostName).getCustomDomain();
            }
            else{
                domainName = "https://dashboard";
            }
            if (env.equalsIgnoreCase("prod")) {
                env = "";
            }
            String deepLinkUrl = "id=" + requestId + "&base_url=" + session.getContext().getAuthServerUrl().toString() + "realms/api/deeplink/payload" + "&action_token=" + actionToken + "&realm_id=" + realmModel.getId();
            // OSS: app download links come from realm CIBA config (admin-set), falling back to server
            // env (VERIFY_APP_IOS_URL / VERIFY_APP_ANDROID_URL); omitted if neither is set. Previously a
            // blocking GitHub Releases fetch via *DownloadProvider that returned null on-prem -> NPE and
            // aborted the whole backchannel auth.
            String appUrl = cibaAuthConfigModel.getConfig().get(APP_STORE_LINK);
            if (appUrl == null || appUrl.trim().isEmpty()) appUrl = EnvironmentReader.getValue("VERIFY_APP_IOS_URL");
            String androidUrl = cibaAuthConfigModel.getConfig().get(PLAY_STORE_LINK);
            if (androidUrl == null || androidUrl.trim().isEmpty()) androidUrl = EnvironmentReader.getValue("VERIFY_APP_ANDROID_URL");
            String baseUrl = domainName + "." + env  + EnvironmentReader.getValue("ROOT_DNS_NAME")+ "/auth";
            StringBuilder deepLink = new StringBuilder(baseUrl + "/deeplink.html?" + deepLinkUrl);
            if (appUrl != null && !appUrl.trim().isEmpty()) deepLink.append("&app_url=").append(appUrl);
            if (androidUrl != null && !androidUrl.trim().isEmpty()) deepLink.append("&windows_url=").append(androidUrl);
            realmModel.addAttribute(requestId, "deep_link", deepLink.toString());
        }
        try{
            DeepLinkingResource deepLinkingResource = new DeepLinkingResource(session);
            deepLinkingResource.addPayload(JsonSerialization.writeValueAsString(result), requestId);
        } catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
        }



        return true;
    }

    private String qrCode(String text) throws IOException {
        try {
            int width = 512;
            int height = 512;

            QRCodeWriter writer = new QRCodeWriter();
            final BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
            bos.close();

            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    private boolean triggerPush(UserModel userModel, RealmModel realm, CibaAuthConfigModel cibaAuthConfigModel, String oidcUrl, String requestId) throws InterruptedException {
        String pushToken = realm.getTrxValue(requestId, PUSH_TOKEN);
        String pushType = realm.getTrxValue(requestId, PUSH_TYPE);
        if (pushType != null && !pushType.isEmpty()) {
            if (!supportedMobilePushTypes.contains(pushType)) {
                if(!cibaAuthConfigModel.getConfig().get(INTEGRATION_TYPE).equals(EZTO_VERIFY_APP) && cibaAuthConfigModel.getConfig().get(AUTOMATIC_FALLBACK) != null
                        && !Boolean.parseBoolean(cibaAuthConfigModel.getConfig().get(AUTOMATIC_FALLBACK))){
                    event.error("Push is configured, but the push_type is invalid.");
                    throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid details. Check Audit > logs for more details.", Response.Status.BAD_REQUEST);
                } else {
                    return false;
                }
            }
        }

        if (pushToken == null || pushToken.isEmpty()) {
            Optional<CredentialModel> lastUsedDeviceCredentialModel = TokenCredentialUtility.getInstance()
                    .getLastUsedDeviceCredentialModel(session, userModel, TokenCredentialUtility.DeviceType.MOBILE);
            if (lastUsedDeviceCredentialModel.isEmpty()) {
                return false;
            }
            Optional<String> lastUsedDeviceToken = TokenCredentialUtility.getInstance()
                    .getDeviceToken(lastUsedDeviceCredentialModel.get());
            if (lastUsedDeviceToken.isEmpty()) {
                return false;
            }
            pushToken = lastUsedDeviceToken.get();
            pushType = getLastUsedDevicePushType(lastUsedDeviceCredentialModel.get());
        }

        String integrationType = cibaAuthConfigModel.getConfig().get(INTEGRATION_TYPE);
        assert pushType != null;
        if (pushType.equalsIgnoreCase(FCM)) {
            String serviceJson = integrationType.equalsIgnoreCase(SDK) ? cibaAuthConfigModel.getConfig().get(FIREBASE_JSON) :
                    session.realms().getRealmByName(Config.getAdminRealm()).getAttribute(FIREBASE_JSON);
            return triggerFirebasePush(cibaAuthConfigModel, userModel, requestId, serviceJson, oidcUrl, pushToken);
        } else if (pushType.equalsIgnoreCase(HMS)) {
            return triggerHuaweiPush(cibaAuthConfigModel, requestId, oidcUrl, pushToken);
        } else{
            if (!Boolean.parseBoolean(cibaAuthConfigModel.getConfig().get(AUTOMATIC_FALLBACK))) {
                event.error("Push is configured, but the push_type is invalid.");
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid details. Check Audit > logs for more details.", Response.Status.BAD_REQUEST);
            }
        }

        return false;
    }

    private boolean triggerHuaweiPush(CibaAuthConfigModel cibaAuthConfigModel, String requestId, String oidcUrl, String token) {
        try {
            HashMap<String, Object> payload = generateNotificationPayload(cibaAuthConfigModel, oidcUrl, requestId);
            return HuaweiHelper.sendHuaweiPushNotification(session, cibaAuthConfigModel, requestId, token, payload, event);
        } catch (Exception e) {
            event.detail(REQUEST_ID, requestId).error("Invalid Huawei JSON");
            return false;
        }
    }

    private boolean triggerFirebasePush(CibaAuthConfigModel cibaAuthConfigModel, UserModel userModel, String requestId,
                                        String json, String oidcUrl, String token) {
        try {
            new JSONObject(json);
            FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(FirebaseHelper.getCredentials(json)).build();
            boolean firebaseAppNotFound = FirebaseApp.getApps().stream()
                    .filter(x -> x.getName().equalsIgnoreCase(cibaAuthConfigModel.getClientId())).findAny().isEmpty();
            if (firebaseAppNotFound) {
                FirebaseApp.initializeApp(options, cibaAuthConfigModel.getClientId());
            }

            if (!FirebaseApp.getInstance(cibaAuthConfigModel.getClientId()).getOptions().equals(options)) {
                FirebaseApp.getInstance(cibaAuthConfigModel.getClientId()).delete();
                FirebaseApp.initializeApp(options, cibaAuthConfigModel.getClientId());
            }

            HashMap<String, Object> payload = generateNotificationPayload(cibaAuthConfigModel, oidcUrl, requestId);
            Message message = generateFirebaseMessageObject(cibaAuthConfigModel, payload, token);
            String firebaseResponse = FirebaseMessaging
                    .getInstance(FirebaseApp.getInstance(cibaAuthConfigModel.getClientId()))
                    .sendAsync(message).get();
            TokenCredentialUtility.getInstance().updateLastUsedToken(session, userModel, token);
            event.detail(REQUEST_ID, requestId).detail("firebase_response", firebaseResponse).success();
            return true;
        } catch (ExecutionException e) {
            event.detail(REQUEST_ID, requestId).error(e.getCause().getMessage());
            if (e.getCause().getMessage().equalsIgnoreCase("Requested entity was not found.")) {
                removeStoredDeviceAndToken(session.getContext().getRealm(), userModel, List.of(token), requestId);
                event.detail(REQUEST_ID, requestId).error("Device specific to the requested token has been removed");
                return false;
            }
            if (!Boolean.parseBoolean(cibaAuthConfigModel.getConfig().get(AUTOMATIC_FALLBACK))) {
                throw new ErrorResponseException(e.getMessage(), e.getCause().getMessage(), Response.Status.BAD_REQUEST);
            }
        } catch (Exception e) {
            event.detail(REQUEST_ID, requestId).error("Invalid Firebase JSON");
            return false;
        }
        event.detail(REQUEST_ID, requestId).error("Unknown error. Failed to send firebase push notification");
        return false;
    }

    private Message generateFirebaseMessageObject(CibaAuthConfigModel cibaAuthConfigModel,
                                                  HashMap<String, Object> payload, String token) throws IOException {
        return Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(cibaAuthConfigModel.getConfig().get("notification-title"))
                        .setBody(cibaAuthConfigModel.getConfig().get("notification-body"))
                        .build())
                .putData("payload", JsonSerialization.writeValueAsString(payload))
                .setToken(token)
                .build();
    }

    private HashMap<String, Object> generateNotificationPayload(CibaAuthConfigModel cibaAuthConfigModel,
                                                                String oidcUrl, String requestId) throws Exception {
        boolean isEncryptionEnabled = Boolean.parseBoolean(cibaAuthConfigModel.getConfig().get(ENCRYPTION_ENABLE));
        String secretKey = null;
        if (isEncryptionEnabled) {
            secretKey = TokenCredentialUtility.getInstance().getSecret(session, cibaAuthConfigModel, TokenCredentialUtility.DeviceType.MOBILE);
        }
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("version", 1);
        payload.put("id", "qv_auth");
        payload.put("isEncrypted", isEncryptionEnabled);
        HashMap<String, Object> data = new HashMap<>();
        data.put("action", oidcUrl);
        data.put("type", "url");
        HashMap<String, String> socket = new HashMap<>();
        socket.put("url", EnvironmentReader.getValue("SOCKET_URL"));
        socket.put(REQUEST_ID, requestId);
        data.put("socket", socket);
        payload.put("data", isEncryptionEnabled && secretKey != null ? AESHelper
                .encrypt(JsonSerialization.writeValueAsString(data), secretKey) : data);
        return payload;
    }

    private void removeStoredDeviceAndToken(RealmModel realm, UserModel userModel, List<String> tokens, String requestId) {
        session.userCredentialManager().getStoredCredentialsStream(realm, userModel).forEach(x -> {
            try {
                if (x.getSecretData() != null) {
                    JSONObject jsonObject = new JSONObject(x.getSecretData());
                    if (jsonObject.get("token") != null) {
                        String token = String.valueOf(jsonObject.get("token"));
                        if (tokens.contains(token)) {
                            session.userCredentialManager().removeStoredCredential(realm, userModel, x.getId());
                            realm.removeTrxAttribute(requestId, PUSH_TOKEN);
                        }
                    }
                }
            } catch (JSONException e) {
                logger.error(e.getMessage());
            }
        });
    }

    private String getLastUsedDevicePushType(CredentialModel credentialModel) {
        try {
            TokenSecretData tokenSecretData = JsonSerialization.readValue(credentialModel.getSecretData(), TokenSecretData.class);
            if ((tokenSecretData.getDevicePlatform().equalsIgnoreCase("Android")
                    || tokenSecretData.getDevicePlatform().equalsIgnoreCase("iOS"))) {
                return tokenSecretData.getPushType();
            }
        } catch (IOException e) {
            logger.error("Error occurred while retrieving last used push type: " + e.getMessage());
        }
        return "fcm";
    }

    @NotNull
    private HashMap<String, Object> getPayload(CibaAuthConfigModel cibaAuthConfigModel, String oidcUrl, String requestId) throws Exception {
        boolean isEncryptionEnabled = Boolean.parseBoolean(cibaAuthConfigModel.getConfig().get(ENCRYPTION_ENABLE));
        String secretKey = null;
        if (isEncryptionEnabled) {
            secretKey = TokenCredentialUtility.getInstance().getSecret(session, cibaAuthConfigModel, TokenCredentialUtility.DeviceType.MOBILE);
        }
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("version", 1);
        payload.put("id", "qv_auth");
        payload.put("isEncrypted", isEncryptionEnabled);
        HashMap<String, Object> data = new HashMap<>();
        data.put("action", oidcUrl);
        data.put("type", "url");
        HashMap<String, String> socket = new HashMap<>();
        socket.put("url", EnvironmentReader.getValue("SOCKET_URL"));
        socket.put(REQUEST_ID, requestId);
        data.put("socket", socket);
        payload.put("data", isEncryptionEnabled && secretKey != null ? AESHelper.encrypt(JsonSerialization.writeValueAsString(data), secretKey) : data);
        return payload;
    }

    @Override
    public void onAuthCompleted(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, String callbackUrl, CibaStateModel cibaState, EventBuilder event, Map<String, Object> response) {
        logger.debug("Mobile Authorizer:: onAuthCompleted ");

        RealmModel realm = session.getContext().getRealm();
        updateTransaction(cibaState.getRequestId(), clientModel, userModel, realm);
        try {
            String authCode = (String) response.get("authorization_code");
            logger.debug("Mobile Authorizer:: authCode:: " + authCode);
            if (authCode == null || authCode.isEmpty()) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Web Authorizer:: No Authorization code received", Response.Status.BAD_REQUEST);
            }
            // In-process authorization-code validation. Previously a synchronous loopback HTTP POST
            // to this server's own /token endpoint (120s*retries) — under load it let the JTA reaper
            // close the request EntityManager before event.success(), throwing "Session/EntityManager
            // is closed". The minted token was discarded; only validity/consumption mattered.
            OAuth2CodeParser.ParseResult parseResult = OAuth2CodeParser.parseCode(session, authCode, realm, event);
            if (parseResult.isIllegalCode() || parseResult.isExpiredCode()) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Mobile Authorizer:: Invalid or expired authorization code", Response.Status.BAD_REQUEST);
            }
            if (parseResult.getClientSession() == null
                    || !clientModel.getId().equals(parseResult.getClientSession().getClient().getId())) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Mobile Authorizer:: Authorization code does not match client", Response.Status.BAD_REQUEST);
            }

            sendSuccess(callbackUrl, cibaState, event, null);
        }
        /*catch (ErrorResponseException e) {
            logger.error(e.getMessage());
            event.event(EventType.EZ_TRANSACTION_UNAUTHORIZED);
            event.detail("reason", e.getErrorDescription());
            event.success();
            sendUnAuthorized(callbackUrl, cibaState, null);
        }*/ catch (Exception err) {
            logger.error(err.getMessage());
            event.event(EventType.EZ_TRANSACTION_NOT_PERMITTED);
            event.detail("reason", err.getMessage());
            event.error(err.getMessage());
            sendUnAuthorized(callbackUrl, cibaState, event, null);
        }

        realm.removeCibaState(cibaState);
    }

    @Override
    public void onAuthFailed(ClientModel clientModel, UserModel userModel, String callbackUrl, CibaStateModel cibaState, EventBuilder event, Map<String, Object> response) {
        RealmModel realm = session.getContext().getRealm();
        realm.removeCibaState(cibaState);
        updateTransaction(cibaState.getRequestId(), clientModel, userModel, realm);
        if (response == null) {
            updateResult(session, realm, clientModel, userModel, cibaState.getRequestId());
        }
        sendUnAuthorized(callbackUrl, cibaState, event, null);
    }

    @Override
    public boolean validateUserDetails(UserModel user, RealmModel realm, CibaAuthConfigModel cibaAuthConfigModel, String requestId) {
        if(cibaAuthConfigModel != null
                && cibaAuthConfigModel.getConfig().get(PUSH_WAY).equals(CIBA_PUSH)){
            String cibaAuthProviderId = cibaAuthConfigModel.getCibaAuthProviderId();
            if ((cibaAuthProviderId.equals(getId()))
                    && ((!isTokenAlreadyReg(user))
                        && (realm.getTrxValue(requestId, PUSH_TOKEN) == null || realm.getTrxValue(requestId, PUSH_TOKEN).isEmpty()))) {
                if(!cibaAuthConfigModel.getConfig().get(INTEGRATION_TYPE).equals(EZTO_VERIFY_APP) && cibaAuthConfigModel.getConfig().get(AUTOMATIC_FALLBACK) != null
                        && !Boolean.parseBoolean(cibaAuthConfigModel.getConfig().get(AUTOMATIC_FALLBACK))){
                    event.event(EventType.EZ_TRANSACTION_ERROR);
                    event.error(" Push is configured, but the push token is missing to send the push. If automatic fallback to QR is needed, enable it in the configuration");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isTokenAlreadyReg(UserModel user) {
        return session.userCredentialManager().getStoredCredentialsByTypeStream(session.getContext().getRealm(), user, TokenCredentialModel.TYPE).count() != 0; // here we check if the user have already setup the tokens for devices
    }

    @Override
    public String getStep1(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, String actionFlow) {
        String pushWay = cibaAuthConfigModel.getConfig().get(PUSH_WAY);
        if (pushWay.equals(CIBA_PUSH)) {
            String data = "{\n" +
                    "    \"user\": {\n" +
                    "       \"email\":\"" + userModel.getEmail() + "\",\n" +
                    "       \"metadata\": {\n" +
                    "           \"push_token\": \"@@token here@@\",\n" +
                    "           \"push_type\": \"@@token type here@@\"\n" +
                    "        }\n" +
                    "    }\n";
            boolean isShowConsent = Boolean.parseBoolean(clientModel.getAttribute("ciba.consent"));
            if (isShowConsent) {
                data += "  ,  \"message\": {\n" +
                        "        \"consent\":{\n" +
                        "           \"type\":\"string\",\n" +
                        "           \"title\":\"Verification Request\",\n" +
                        "           \"message\": \"Test Verification from " + clientModel.getName() + "\"\n" +
                        "         }\n" +
                        "    }\n" +
                        "}";
            } else {
                data += "}";
            }
            return GeneralHelper.updateUserMetadata(session, clientModel, data, session.getContext().getRealm(), actionFlow);
        }


        if (pushWay.equals(CIBA_QR)) {
            String data = "{\n" +
                    "    \"user\": {\n" +
                    "        \"email\":\"" + userModel.getEmail() + "\"\n" +
                    "    }\n";
            boolean isShowConsent = Boolean.parseBoolean(clientModel.getAttribute("ciba.consent"));
            if (isShowConsent) {
                data += "  ,  \"message\": {\n" +
                        "        \"consent\":{\n" +
                        "           \"type\":\"string\",\n" +
                        "           \"title\":\"Verification Request\",\n" +
                        "           \"message\": \"Test Verification from " + clientModel.getName() + "\"\n" +
                        "         }\n" +
                        "    }\n" +
                        "}";
            } else {
                data += "}";
            }
            return GeneralHelper.updateUserMetadata(session, clientModel, data, session.getContext().getRealm(), actionFlow);
        }
        return "Invalid Settings";
    }

    @Override
    public String getStep2(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel) {
        String pushWay = cibaAuthConfigModel.getConfig().get(PUSH_WAY);
        if (pushWay.equals(CIBA_PUSH)) {
            return "Check your app for notification";
        }

        if (pushWay.equals(CIBA_QR)) {
            return "Scan the QR from ezto verify SDK integrated mobile app or ezto verify app";
        }
        return "Invalid Settings";
    }


    @Override
    public boolean isMobile() {
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "mobile-sdk-title";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Map<String, Object> dependencyMap = new HashMap<>();
    private static final Map<String, Object> deeplinkingDependencyMap = new HashMap<>();
    private static final Map<String, Object> appLinkDependencyMap = new HashMap<>();
    private static final Map<String, Object> encrytionDependencyMap = new HashMap<>();
    private static final Map<String, Object> integrationDependencyMap = new HashMap<>();
    private static final Map<String, Object> keyOptsDependencyMap = new HashMap<>();
    private static final Map<String, Object> workspaceDependencyMap = new HashMap<>();
    private static final Map<String, Object> firebaseDependencyMap = new HashMap<>();
    public static final String SECRET_KEY = "secret-keys";
    public static final String SDK = "SDK";
    public static final String HMS = "hms";
    public static final String FCM = "fcm";
    public static final String EZTO_VERIFY_APP = "ezto-verify-app";

    public static final String FROM_WORKSPACE = "from-workspace";

    public static final String WILL_SET_APPLICATION_LEVEL = "will-set-application-level";

    public static final String KEY_OPTIONS = "key-options";

    static {
        dependencyMap.put(PUSH_WAY, CIBA_PUSH);
        dependencyMap.put(INTEGRATION_TYPE, SDK);
        deeplinkingDependencyMap.put(INTEGRATION_TYPE, SDK);
        appLinkDependencyMap.put(INTEGRATION_TYPE, SDK);
        appLinkDependencyMap.put(DEEP_LINKING, "true");
        encrytionDependencyMap.put(ENCRYPTION_ENABLE, "true");
        integrationDependencyMap.put(INTEGRATION_TYPE, SDK);
        keyOptsDependencyMap.put(KEY_OPTIONS, WILL_SET_APPLICATION_LEVEL);
        keyOptsDependencyMap.put(INTEGRATION_TYPE, SDK);
        workspaceDependencyMap.put(KEY_OPTIONS, FROM_WORKSPACE);
        workspaceDependencyMap.put(INTEGRATION_TYPE, SDK);
        firebaseDependencyMap.put(PUSH_WAY, CIBA_PUSH);
    }


    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(VERIFICATION_EXPIRY_CONFIG);
        property.setLabel("ciba-link-expiry-duration");
        property.setType(ProviderConfigProperty.EXPIRY_TIME_TYPE);
        property.setHelpText("ciba-link-expiry-duration-helptext");
        property.setDefaultValue("120s");
        configProperties.add(property);

        List<String> pushTypes = new ArrayList<>();
        pushTypes.add(CIBA_PUSH);
        pushTypes.add(CIBA_QR);
        property = new ProviderConfigProperty();
        property.setName(PUSH_WAY);
        property.setLabel("push-way-label");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(pushTypes);
        property.setDefaultValue(CIBA_PUSH);
        property.setHelpText("push-way-hlp-txt");
        configProperties.add(property);

        List<String> intTypes = new ArrayList<>();
        intTypes.add(SDK);
        intTypes.add(EZTO_VERIFY_APP);
        property = new ProviderConfigProperty();
        property.setName(INTEGRATION_TYPE);
        property.setLabel("integration-type-label");
        property.setRequired(true);
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(intTypes);
        property.setHelpText("integration-type-hlp-txt");
        configProperties.add(property);


        property = new ProviderConfigProperty();
        property.setName(FIREBASE_JSON);
        property.setLabel("firebase-json-label");
        property.setType(ProviderConfigProperty.TEXT_TYPE);
        property.setHelpText("firebase-json-hlp-txt");
        property.setDependsOn(dependencyMap);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(HUAWEI_CLIENT_ID);
        property.setLabel("huawei-client-id-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("huawei-client-id-hlp-txt");
        property.setDependsOn(dependencyMap);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(HUAWEI_CLIENT_SECRET);
        property.setLabel("huawei-client-secret-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("huawei-client-secret-hlp-txt");
        property.setDependsOn(dependencyMap);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("notification-title");
        property.setLabel("notification-title-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setHelpText("notification-title-hlp-txt");
        property.setDependsOn(firebaseDependencyMap);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("notification-body");
        property.setLabel("notification-body-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setHelpText("notification-body-hlp-txt");
        property.setDependsOn(firebaseDependencyMap);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(AUTOMATIC_FALLBACK);
        property.setLabel("automatic-fallback-label");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        property.setHelpText("automatic-fallback-hlp-txt");
        property.setDependsOn(dependencyMap);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(DEEP_LINKING);
        property.setLabel("deep-linking-label");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        property.setHelpText("deep-linking-hlp-txt");
        property.setDependsOn(deeplinkingDependencyMap);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(PLAY_STORE_LINK);
        property.setLabel("play-store-link-label");
        property.setType(ProviderConfigProperty.URL);
        property.setRequired(true);
        property.setHelpText("play-store-link-hlp-txt");
        property.setDependsOn(appLinkDependencyMap);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(APP_STORE_LINK);
        property.setLabel("app-store-link-label");
        property.setType(ProviderConfigProperty.URL);
        property.setRequired(true);
        property.setHelpText("app-store-link-hlp-txt");
        property.setDependsOn(appLinkDependencyMap);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(PROXIMITY_ENABLE);
        property.setLabel("proximity-enable-label");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        property.setHelpText("proximity-enable-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ENCRYPTION_ENABLE);
        property.setLabel("encryption-enable-label");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        property.setHelpText("encryption-enable-hlp-txt");
        configProperties.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(KEY_OPTIONS)).findFirst();
        ProviderConfigProperty provider;
        provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);
        provider.setName(KEY_OPTIONS);
        provider.setLabel("key-options-label");

        List<Map> keyOpts = new ArrayList<>();
        HashMap<String, String> key = new HashMap<>();
        key.put("name", FROM_WORKSPACE);
        key.put("value", FROM_WORKSPACE);
        keyOpts.add(key);
        key = new HashMap<>();
        key.put("name", WILL_SET_APPLICATION_LEVEL);
        key.put("value", WILL_SET_APPLICATION_LEVEL);
        keyOpts.add(key);
        provider.setType(ProviderConfigProperty.MAP_LIST_TYPE);
        provider.setMapOptions(keyOpts);
        provider.setRequired(true);
        provider.setOnDemand("/admin/realms/" + session.getContext().getRealm().getName() + "/clients/" + session.getContext().getClient().getClientId() + "/trx-auth-config/ondemand/" + getId());
        provider.setHelpText("key-options-hlp-txt");

        final HashMap<String, Object> keysDepMap = new HashMap<>();
        keysDepMap.putAll(integrationDependencyMap);
        keysDepMap.putAll(encrytionDependencyMap);
        provider.setDependsOn(keysDepMap);

        if (providerConfigProperty.isEmpty()) {
            configProperties.add(provider);
        }


        setKeys(session);
        return configProperties;
    }

    private void setKeys(KeycloakSession session) {
        List<Map> hmacKeys = session.keys().getKeysStream(session.getContext().getRealm(), KeyUse.ENC, "AES").filter(x -> x.getStatus().isEnabled() && x.getStatus().isActive())
                .map(x -> {
                    HashMap<String, String> key = new HashMap<>();
                    key.put("name", x.getName());
                    key.put("value", x.getProviderId());
                    return key;
                })
                .collect(Collectors.toList());
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(SECRET_KEY)).findFirst();
        ProviderConfigProperty provider;
        provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);
        provider.setName(SECRET_KEY);
        provider.setLabel("secret-keys-label");

        if (hmacKeys.isEmpty()) {
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("");
            provider.setHelperLinkText("no-aes-keys");
            provider.setHelperLink("/create/keys/" + session.getContext().getRealm().getName() + "/providers/aes-generated");
        } else {
            provider.setType(ProviderConfigProperty.MAP_LIST_TYPE);
            provider.setRequired(true);
            provider.setMapOptions(hmacKeys);
            provider.setOnDemand("/admin/realms/" + session.getContext().getRealm().getName() + "/clients/" + session.getContext().getClient().getClientId() + "/trx-auth-config/ondemand/" + getId());
            provider.setHelpText("");
            provider.setHelperLinkText("select-keys-to-enable-encryption");
            provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/defense/keys/");
        }


        final HashMap<String, Object> dependencyMap = new HashMap<>();
        dependencyMap.putAll(encrytionDependencyMap);
        dependencyMap.putAll(keyOptsDependencyMap);
        provider.setDependsOn(dependencyMap);

        if (providerConfigProperty.isEmpty()) {
            configProperties.add(provider);
        }
    }

    @Override
    public CibaAuthProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        //Do Nothing
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //Do Nothing
    }

    @Override
    public void close() {
        //Do Nothing
    }

    @Override
    public String getId() {
        return "ciba-mobile-sdk";
    }

    @Override
    public List<ProviderConfigProperty> getOnDemandConfig(KeycloakSession session, HashMap<String, Object> config) {

        if (config.containsKey(KEY_OPTIONS) && config.get(KEY_OPTIONS) != null && config.get(KEY_OPTIONS).equals(FROM_WORKSPACE)) {
            String secret = session.getContext().getRealm().getAttribute(MOBILE_ENC_KEY);
            if (secret == null || secret.isEmpty()) {
                ProviderConfigProperty property = new ProviderConfigProperty();
                property.setName("secret-key");
                property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                property.setHelpText("");
                property.setHelperLinkText("no-workspace-keys-reg");
                property.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/realm-attributes/mobile-apps");
                property.setLabel("secret-key-label");
                final HashMap<String, Object> dependencyMap = new HashMap<>();
                dependencyMap.putAll(encrytionDependencyMap);
                dependencyMap.putAll(workspaceDependencyMap);
                property.setDependsOn(dependencyMap);
                List<ProviderConfigProperty> providerConfigProperties = new ArrayList<>();
                providerConfigProperties.add(property);
                return providerConfigProperties;
            }
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName("secret-key");
            property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            property.setHelpText("");
            property.setHelperLink("");
            property.setHelperLinkText("secret-key-to-decrypt-in-mobile-sdk");
            property.setOnDemandComponent(session.getContext().getRealm().getAttribute(MOBILE_ENC_KEY));
            property.setLabel("secret-key-label");
            final HashMap<String, Object> dependencyMap = new HashMap<>();
            dependencyMap.putAll(encrytionDependencyMap);
            dependencyMap.putAll(workspaceDependencyMap);
            property.setDependsOn(dependencyMap);
            List<ProviderConfigProperty> providerConfigProperties = new ArrayList<>();
            providerConfigProperties.add(property);
            return providerConfigProperties;
        }
        if (config.containsKey(KEY_OPTIONS) && config.get(KEY_OPTIONS) != null && config.get(KEY_OPTIONS).equals(WILL_SET_APPLICATION_LEVEL)) {
            ComponentRepresentation componentRepresentation = getEncryptionComponent(session, config);
            if (componentRepresentation == null) return new ArrayList<>();
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName("secret-key");
            property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            property.setHelpText("Use the following secret key to decrypt payload in mobile SDK.<br>KEY: <br><b>" + componentRepresentation.getConfig().get(SECRET).get(0) + "</b>");
            property.setLabel("secret-key-label");

            final HashMap<String, Object> dependencyMap = new HashMap<>();
            dependencyMap.putAll(encrytionDependencyMap);
            dependencyMap.putAll(keyOptsDependencyMap);
            property.setDependsOn(dependencyMap);
            List<ProviderConfigProperty> providerConfigProperties = new ArrayList<>();
            providerConfigProperties.add(property);
            return providerConfigProperties;
        }
        return new ArrayList<>();

    }

    @Nullable
    private ComponentRepresentation getEncryptionComponent(KeycloakSession session, HashMap<String, Object> config) {
        ComponentModel optionalComponentModel = session.getContext().getRealm().getComponent(String.valueOf(config.get(SECRET_KEY)));
        if (optionalComponentModel == null) {
            return null;
        }

        return ModelToRepresentation.toRepresentation(session, optionalComponentModel, true);
    }

    @Override
    public void onCleanUp(KeycloakSession session, RealmModel realm, ClientModel client) {
        CibaAuthConfigModel authenticationConfig = realm.getCibaAuthConfigByClientId(client.getClientId(), realm.getId());
        if (authenticationConfig != null) {
            realm.removeCibaAuthConfig(authenticationConfig);
        }
    }
}

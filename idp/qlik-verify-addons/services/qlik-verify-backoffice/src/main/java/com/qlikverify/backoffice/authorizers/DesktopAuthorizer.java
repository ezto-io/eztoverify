package com.qlikverify.backoffice.authorizers;

import com.qlikverify.backoffice.GeneralHelper;
import com.qlikverify.backoffice.authorizers.utility.TokenCredentialUtility;
import com.qlikverify.backoffice.controllers.DeepLinkingResource;
import com.qlikverify.extensions.admin.controllers.actionToken.EztoActionTokenVerifier;
import com.qlikverify.services.theme.downloads.EztoVrfyMacDownloadProvider;
import com.qlikverify.services.theme.downloads.EztoVrfyWindowsDownloadProvider;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.Nullable;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.authorizers.MobileAuthorizer.*;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;
import static org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper.updateResult;

public class DesktopAuthorizer extends AbstractCibaAuthorizer implements CibaAuthProvider, CibaAuthProviderFactory {

    private static final Logger logger = Logger.getLogger(DesktopAuthorizer.class);
    public static final String ENCRYPTION_ENABLE = "encryption-enable";

    public static final String TAG = "DesktopAuthorizer: ";
    private static final String REQUEST_ID = "requestId";
    private static final Map<String, Object> integrationDependencyMap = new HashMap<>();
    EztoVrfyMacDownloadProvider eztoVrfyMacDownloadProvider = new EztoVrfyMacDownloadProvider();
    EztoVrfyWindowsDownloadProvider eztoVrfyWindowsDownloadProvider = new EztoVrfyWindowsDownloadProvider();

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

        final TrxModel trx = realm.getTrx(state.getRequestId());
        String registerNewDevice = realm.getTrxValue(state.getRequestId(),"sp@"+REGISTER_NEW_DEVICE);
        boolean isNewDeviceRegistration = Boolean.parseBoolean(registerNewDevice);
        if (isNewDeviceRegistration) {
            return generateDeepLinkUrl(realm, cibaAuthConfigModel, request, oidcUrl, state.getRequestId());
        }
        Optional<String> pushToken;
        if (trx != null && trx.getAttributes() != null && trx.getAttributes().get(PUSH_TOKEN) != null && !trx.getAttributes().get(PUSH_TOKEN).isEmpty()) {
            pushToken = Optional.of(trx.getAttributes().get(PUSH_TOKEN));
        } else {
            pushToken = getPushTokenFromCredentialModel(userModel);
        }
        if (pushToken.isPresent()) {
            return sendSocketPush(cibaAuthConfigModel, userModel, request, oidcUrl, pushToken.get());
        } else {
            return generateDeepLinkUrl(realm, cibaAuthConfigModel, request, oidcUrl, state.getRequestId());
        }
    }

    private Optional<String> getPushTokenFromCredentialModel(UserModel user) {
        Optional<CredentialModel> lastUsedDeviceCredentialModel = TokenCredentialUtility.getInstance()
                .getLastUsedDeviceCredentialModel(session, user, TokenCredentialUtility.DeviceType.DESKTOP);
        if (lastUsedDeviceCredentialModel.isPresent()) {
            return TokenCredentialUtility.getInstance().getDeviceToken(lastUsedDeviceCredentialModel.get());
        }
        return Optional.empty();
    }

    private boolean sendSocketPush(CibaAuthConfigModel cibaAuthConfigModel, UserModel userModel, AuthenticationChannelRequest request, String oidcUrl, String deviceToken) {
        final HashMap<String, Object> getPayLoad = getPayload(cibaAuthConfigModel, request, oidcUrl);
        if (getPayLoad == null) return false;
        String socketUrl = EnvironmentReader.getValue("SOCKET_URL");
        HttpUtility httpUtility = new HttpUtility();
        try (okhttp3.Response socketResponse = httpUtility.callRequest(
                "application/json",
                "POST",
                socketUrl + "/send?key=" + deviceToken,
                JsonSerialization.writeValueAsString(getPayLoad),
                null)) {

            if (!socketResponse.isSuccessful()) {
                event.error("Failed to send request id to socket");
                throw new IOException("Failed to send request id to socket");
            }

            TokenCredentialUtility.getInstance().updateLastUsedToken(session, userModel, deviceToken);
            event.detail(REQUEST_ID, (String) request.getAdditionalParameters().get(REQUEST_ID)).detail("desktop_push", String.valueOf(socketResponse.code())).success();
            return true;

        } catch (Exception ioe) {
            event.detail(REQUEST_ID, (String) request.getAdditionalParameters().get(REQUEST_ID)).error(ioe.getMessage());
           logger.error("Failed to send request to socket endpoint");
            return false;
        }
    }

    @Nullable
    private HashMap<String, Object> getPayload(CibaAuthConfigModel cibaAuthConfigModel, AuthenticationChannelRequest request, String oidcUrl) {
        HashMap<String, Object> result;
        try {
            result = getPayload(cibaAuthConfigModel, oidcUrl, (String) request.getAdditionalParameters().get(REQUEST_ID));
        } catch (Exception e) {
            event.detail(REQUEST_ID, (String) request.getAdditionalParameters().get(REQUEST_ID)).error(TAG + e.getMessage());
            return null;
        }

        final HashMap<String, Object> deeplinkPayload = new HashMap<>();
        deeplinkPayload.put("title", cibaAuthConfigModel.getConfig().getOrDefault("notification-title", " "));
        deeplinkPayload.put("body", cibaAuthConfigModel.getConfig().getOrDefault("notification-body", " "));
        try {
            deeplinkPayload.put("payload", JsonSerialization.writeValueAsString(result));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return deeplinkPayload;
    }

    private boolean generateDeepLinkUrl(RealmModel realmModel, CibaAuthConfigModel cibaAuthConfigModel, AuthenticationChannelRequest request, String oidcUrl, String requestId) {
        final HashMap<String, Object> getPayLoad = getPayload(cibaAuthConfigModel, request, oidcUrl);
        if (getPayLoad == null) return false;
        String qrCode;
        try {
            qrCode = Base64.getEncoder().encodeToString(JsonSerialization.writeValueAsString(getPayLoad).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            event.detail(REQUEST_ID, (String) request.getAdditionalParameters().get(REQUEST_ID)).error(TAG + e.getMessage());
            return false;
        }

        if (qrCode == null)
            return false;

        CibaAuthConfigModel cibaAuthConfigByClientId = realmModel.getCibaAuthConfigByClientId(session.getContext().getClient().getClientId(), realmModel.getId());
        if(cibaAuthConfigByClientId == null){
            return false;
        }

        EztoActionTokenVerifier eztoActionTokenVerifier = new EztoActionTokenVerifier(session, session.getContext().getRealm());
        Long tokenExpiry = cibaAuthConfigByClientId.getConfig().get("verify-expiry-time") != null ?  org.keycloak.utils.GeneralHelper.getEpochDuration(cibaAuthConfigByClientId.getConfig().get("verify-expiry-time")) : System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);
        String actionToken = eztoActionTokenVerifier.generateToken(session, tokenExpiry, requestId, "deep-link");

        realmModel.addAttribute(requestId, "transaction_link", "eztovrfy://sdk?data=" + qrCode);
        String deepLinkUrl = "eztovrfy://deeplink?id=" + requestId + "&base_url=" + session.getContext().getAuthServerUrl().toString() + "realms/api/deeplink/payload" + "&action_token=" + actionToken + "&realm_id=" + realmModel.getId();
        String appUrl = eztoVrfyMacDownloadProvider.getDownloadDetails().getDownloadUrl();
        String windowsUrl = eztoVrfyWindowsDownloadProvider.getDownloadDetails().getDownloadUrl();
        realmModel.addAttribute(requestId, "deep_link", session.getContext().getAuthServerUrl().toString() + "deeplink.html?deep_link_url=" + deepLinkUrl + "&app_url=" + appUrl + "&windows_url=" + windowsUrl);
        DeepLinkingResource deepLinkingResource = new DeepLinkingResource(session);
        try{
            deepLinkingResource.addPayload(JsonSerialization.writeValueAsString(getPayLoad), requestId);
        } catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }


    @NotNull
    private HashMap<String, Object> getPayload(CibaAuthConfigModel cibaAuthConfigModel, String oidcUrl, String requestId) throws Exception {
        boolean isEncryptionEnabled = Boolean.parseBoolean(cibaAuthConfigModel.getConfig().get(ENCRYPTION_ENABLE));
        String secretKey = null;
        if (isEncryptionEnabled) {
            secretKey = TokenCredentialUtility.getInstance().getSecret(session, cibaAuthConfigModel, TokenCredentialUtility.DeviceType.DESKTOP);
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
        logger.debug("Desktop Authorizer:: onAuthCompleted ");

        RealmModel realm = session.getContext().getRealm();
        updateTransaction(cibaState.getRequestId(), clientModel, userModel, realm);
        try {
            String authCode = (String) response.get("authorization_code");
            logger.debug("Desktop Authorizer:: authCode:: " + authCode);
            if (authCode == null || authCode.isEmpty()) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Web Authorizer:: No Authorization code received", Response.Status.BAD_REQUEST);
            }
            // In-process authorization-code validation (was a synchronous loopback HTTP POST to this
            // server's own /token endpoint, 120s*retries — under load it let the JTA reaper close
            // the request EntityManager before event.success()). Minted token was discarded.
            OAuth2CodeParser.ParseResult parseResult = OAuth2CodeParser.parseCode(session, authCode, realm, event);
            if (parseResult.isIllegalCode() || parseResult.isExpiredCode()) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Desktop Authorizer:: Invalid or expired authorization code", Response.Status.BAD_REQUEST);
            }
            if (parseResult.getClientSession() == null
                    || !clientModel.getId().equals(parseResult.getClientSession().getClient().getId())) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Desktop Authorizer:: Authorization code does not match client", Response.Status.BAD_REQUEST);
            }

            sendSuccess(callbackUrl, cibaState, event, null);

        } catch (Exception err) {
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
        return true;
    }

    @Override
    public String getStep1(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, String actionFlow) {

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

    @Override
    public String getStep2(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel) {
        return "Open the link in browser to trigger verification in your desktop app";
    }


    @Override
    public boolean isDesktop() {
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "desktop-sdk-title";
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Map<String, Object> dependencyMap = new HashMap<>();
    private static final Map<String, Object> encrytionDependencyMap = new HashMap<>();
    private static final Map<String, Object> workspaceDependencyMap = new HashMap<>();
    private static final Map<String, Object> keyOptsDependencyMap = new HashMap<>();
    public static final String SECRET_KEY = "secret-keys";


    public static final String OWN_DESKTOP_APP = "own-desktop-app";

    static {
        dependencyMap.put(PUSH_WAY, CIBA_PUSH);
        encrytionDependencyMap.put(ENCRYPTION_ENABLE, "true");
        integrationDependencyMap.put(INTEGRATION_TYPE, OWN_DESKTOP_APP);
        keyOptsDependencyMap.put(KEY_OPTIONS, WILL_SET_APPLICATION_LEVEL);
        keyOptsDependencyMap.put(INTEGRATION_TYPE, OWN_DESKTOP_APP);
        workspaceDependencyMap.put(KEY_OPTIONS, FROM_WORKSPACE);
        workspaceDependencyMap.put(INTEGRATION_TYPE, OWN_DESKTOP_APP);
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

        property = new ProviderConfigProperty();
        property.setName("notification-title");
        property.setLabel("notification-title-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setHelpText("notification-title-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("notification-body");
        property.setLabel("notification-body-label");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setHelpText("notification-body-hlp-txt");
        configProperties.add(property);

        List<String> intTypes = new ArrayList<>();
        intTypes.add(OWN_DESKTOP_APP);
        intTypes.add(EZTO_VERIFY_APP);
        property = new ProviderConfigProperty();

        property.setName(INTEGRATION_TYPE);
        property.setLabel("desktop-integration-type-label");
        property.setRequired(true);
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(intTypes);
        property.setHelpText("integration-type-hlp-txt");
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

        final HashMap<String, Object> dependencyMap = new HashMap<>();
        dependencyMap.putAll(encrytionDependencyMap);
        dependencyMap.putAll(integrationDependencyMap);
        provider.setDependsOn(dependencyMap);

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
        return "ciba-desktop-sdk";
    }

    @Override
    public List<ProviderConfigProperty> getOnDemandConfig(KeycloakSession session, HashMap<String, Object> config) {
        if (config.containsKey(KEY_OPTIONS) && config.get(KEY_OPTIONS) != null && config.get(KEY_OPTIONS).equals(FROM_WORKSPACE)) {
            String secret = session.getContext().getRealm().getAttribute("desktop-enc-key");
            if (secret == null || secret.isEmpty()) {
                ProviderConfigProperty property = new ProviderConfigProperty();
                property.setName("secret-key");
                property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
                property.setHelpText("");
                property.setHelperLinkText("no-workspace-keys-reg");
                property.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/realm-attributes/desktop-apps");
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
            property.setHelperLinkText("use-the-secret-key");
            property.setOnDemandComponent(session.getContext().getRealm().getAttribute("desktop-enc-key"));
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
            property.setHelperLinkText("use-the-secret-key");
            property.setOnDemandComponent(componentRepresentation.getConfig().get(SECRET).get(0));
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

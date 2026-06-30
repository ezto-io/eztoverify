package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.backoffice.Constants;
import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.Errors;
import com.qlikverify.backoffice.GeneralHelper;
import com.qlikverify.backoffice.creds.device.TokenCredentialModel;
import com.qlikverify.backoffice.creds.device.TokenSecretData;
import com.qlikverify.services.theme.downloads.EztoVrfyAndroidDownloadProvider;
import com.qlikverify.services.theme.downloads.EztoVrfyIOSDownloadProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.ciba.models.BindingMessage;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.validation.Validation;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.urls.UrlType;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.authorizers.MobileAuthorizer.PROXIMITY_ENABLE;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;
import static org.keycloak.protocol.oidc.grants.ciba.resolvers.DefaultCIBALoginUserResolver.SP_WORKFLOW;

public class Web2Device implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    public static final Logger logger = Logger.getLogger(Web2Device.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty configProperty;
        configProperty = new ProviderConfigProperty();
        configProperty.setName("consent-message");
        configProperty.setLabel("consent-message");
        configProperty.setType(ProviderConfigProperty.TEXT_TYPE);
        configProperty.setHelpText("consent-message-helptext");
        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setLabel("sample-consent-message");
        configProperty.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        configProperty.setHelpText(
                "\n" +
                        "{\n" +
                        "    \"consent\": {\n" +
                        "      \"type\": \"json\",\n" +
                        "      \"title\": \"Onboarding Request\",\n" +
                        "      \"message\": {\n" +
                        "        \"RequestFrom\": \"EventBeep\",\n" +
                        "        \"Purpose\": \"Onboarding\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "}");

        configProperties.add(configProperty);

        configProperty = new ProviderConfigProperty();
        configProperty.setName("is-prompt");
        configProperty.setLabel("is-prompt");
        configProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperty.setHelpText("is-prompt-helptext");
        configProperties.add(configProperty);
    }

    EztoVrfyIOSDownloadProvider eztoVrfyIOSDownloadProvider = new EztoVrfyIOSDownloadProvider();
    EztoVrfyAndroidDownloadProvider eztoVrfyAndroidDownloadProvider = new EztoVrfyAndroidDownloadProvider();

    private static void updateResult(RequiredActionContext context, Map<String, String> resObj) {
        resObj.entrySet().stream()
                .filter(x -> !x.getKey().equalsIgnoreCase("hidden_ua_message"))
                .filter(x -> !x.getKey().equalsIgnoreCase("ik@t1_request_id"))
                .filter(x -> !x.getKey().equalsIgnoreCase("ik@linkExp"))
                .filter(x -> !x.getKey().equalsIgnoreCase("ik@skip_user_attrs_delete"))
                .forEach(attr -> {
                    if (attr.getKey().equals("liveness_completed")) {
                        context.getRealm().addAttribute(context.getRequestId(),
                                attr.getKey(),
                                attr.getValue());
                    } else if (attr.getKey().equals("error_code")) {
                        try {
                            List<Map<String, Object>> errors = JsonSerialization.readValue(
                                    JsonSerialization.writeValueAsString(attr.getValue()), List.class);
                            List<Errors> e = errors.stream().map(x -> Errors.valueOf(x.get("error_code").toString())).collect(Collectors.toList());
                            List<String> formattedErrors = e.stream().map(x ->
                                    x.toString()
                            ).collect(Collectors.toList());
                            context.getRealm().addAttribute(context.getRequestId(),
                                    attr.getKey(), String.valueOf(formattedErrors));
                        } catch (IOException e) {
                            logger.error("Web2Device :: updateResult :: " + e.getMessage(), e);
                        }
                    } else {
                        context.getRealm().addAttribute(context.getRequestId(),
                                attr.getKey(),
                                attr.getValue());
                    }
                });
        context.getRealm().addAttribute(context.getRequestId(), WEB_TO_DEVICE, "true");
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "bo-web-to-device";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals("app-id")).findFirst();
        ProviderConfigProperty provider;
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        provider.setName("app-id");
        provider.setLabel("mobile-apps-supported-client");
        provider.setHelperLinkText("to-view-selected-apps");
        provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/clients/{{id}}");
        provider.setRequired(true);

        List<ClientModel> clientModels = session.clients()
                .getClientsStream(session.getContext().getRealm()).filter(x -> x.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER) != null && (x.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER).equals("ciba-mobile-sdk"))).filter(x -> {
                    String deliveryMode = session.getContext().getRealm().getCibaPolicy().getBackchannelTokenDeliveryMode(x);
                    CibaNotificationConfigModel cibaNotificationConfigByProviderId = session.getContext().getRealm().getCibaNotificationConfigByProviderId(deliveryMode, x.getClientId(),
                            session.getContext().getRealm().getId());
                    if (cibaNotificationConfigByProviderId == null || cibaNotificationConfigByProviderId.getConfig().get(HOOK_WAY) == null) {
                        return false;
                    }
                    return cibaNotificationConfigByProviderId.getConfig().get(HOOK_WAY).equals(MOBILE_SDK);
                }).collect(Collectors.toList());

        if (clientModels.isEmpty()) {
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelperLinkText("web2device-no-mobile-app");
            provider.setHelperLink("/create/ciba-client/type/ciba/" + session.getContext().getRealm().getName());
        } else {
            List<Map> clients = clientModels.stream().map(x -> {
                HashMap<String, String> newMap = new HashMap<>();
                newMap.put("name", x.getName());
                newMap.put("value", x.getClientId());
                return newMap;
            }).collect(Collectors.toList());
            provider.setType(ProviderConfigProperty.MAP_LIST_TYPE);
            provider.setMapOptions(clients);
            provider.setHelpText("");
        }
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
        return configProperties;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"bo"};
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
        }

        boolean isWebToDevice = context.getRealm().hasTrxValueAttribute(context.getRequestId(), WEB_TO_DEVICE);
        if (result && (!isWebToDevice)) {
            addAction(context);
        }
    }

    private void addAction(RequiredActionContext context) {
        RequiredActionProviderModel model = context.getModel();
        if (context.getUser().getRequiredActionsStream().noneMatch(action -> action.equals(model.getId()))) {
            context.getUser().addRequiredAction(model.getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        String requestId = context.getAuthenticationSession().getAuthNote("requestId");
        if (requestId != null && !requestId.isEmpty()) {
            GeneralHelper.sendCancelStatusToSocket(requestId, "205");
            context.getAuthenticationSession().removeAuthNote("requestId");
        }

        boolean allowDeviceRegister = Boolean.parseBoolean(context.getRealm().getTrxValue(context.getRequestId(), "allow_new_device"));

        List<CredentialModel> registeredDevices = context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), TokenCredentialModel.TYPE).filter(x -> x.isEnabled()).filter(x -> {
            try {
                Map<String, String> secretData = JsonSerialization.readValue(x.getSecretData(), Map.class);
                return !secretData.get("pushType").equals("desktop");
            } catch (Exception e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
            return false;
        }).collect(Collectors.toList());

        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        if (registeredDevices.isEmpty() && !allowDeviceRegister) {
            Map<String, String> additionalDetails = new HashMap<>();
            additionalDetails.put("reason", "User doesn't have any devices and request doesn't allow to register new device");
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR, ErrorMappings.WEB_2_DEVICE_NO_DEVICE_ERROR, additionalDetails);
            context.failure();
            return;
        }

        String appId = context.getConfig().get("app-id");
        ClientModel client = context.getSession().clients().getClientByClientId(context.getRealm(), appId);
        if (client == null) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR,
                    MessageFormat.format("Application not found for web to device", appId));
            context.failure();
            return;
        }

        if (!client.isEnabled()) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR,
                    MessageFormat.format("Application with id {0} is disabled", appId));
            context.failure();
            return;
        }

        if (client != null) {
            String deliveryMode = context.getRealm().getCibaPolicy().getBackchannelTokenDeliveryMode(client);
            CibaNotificationConfigModel cibaNotificationConfigByProviderId = context.getRealm().getCibaNotificationConfigByProviderId(deliveryMode, client.getClientId(),
                    context.getRealm().getId());
            if (!(client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER).equals("ciba-mobile-sdk") && cibaNotificationConfigByProviderId.getConfig().get(HOOK_WAY).equals(MOBILE_SDK))) {
                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR,
                        MessageFormat.format("Application not found for web to device", appId));
                context.failure();
                return;
            }
        }

        CibaAuthConfigModel cibaAuthConfigByProviderId = context.getRealm().getCibaAuthConfigByProviderId(client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER), client.getClientId(), context.getRealm().getId());


        boolean isQR = cibaAuthConfigByProviderId.getConfig().get("push-way") != null && cibaAuthConfigByProviderId.getConfig().get(
                "push-way").equalsIgnoreCase("qr");

        if (context.getConfig().containsKey("is-prompt") && "true".equalsIgnoreCase(context.getConfig().get("is-prompt"))) {
            int promptNumber = GeneralHelper.getRandomTwoDigitNumber();
            loginFormsProvider.setAttribute("isPrompt", "true");
            loginFormsProvider.setAttribute("promptNumber", String.valueOf(promptNumber));

            context.getAuthenticationSession().setAuthNote("promptNumber", String.valueOf(promptNumber));
        }

        if (registeredDevices.size() > 1 && !isQR && (!Boolean.parseBoolean(cibaAuthConfigByProviderId.getConfig().get(PROXIMITY_ENABLE)) || (Boolean.parseBoolean(cibaAuthConfigByProviderId.getConfig().get(PROXIMITY_ENABLE)) && !context.getRealm().getTrxValue(context.getRequestId(), SP_WORKFLOW).equalsIgnoreCase("verification")))) {

            List<TokenSecretData> deviceDetails = registeredDevices.stream().filter(x -> x.isEnabled()).map(x -> {
                try {
                    return JsonSerialization.readValue(x.getSecretData(), TokenSecretData.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).map(x ->
                    new TokenSecretData(null, x.getDeviceId(), x.getDeviceModel(), x.getDeviceOSVersion(), x.getDevicePlatform(), null, null, null)
            ).collect(Collectors.toList());
            loginFormsProvider
                    .setAttribute("devices", deviceDetails)
                    .setAttribute("trxId", "null")
                    .setAttribute("pollUrl", EnvironmentReader.getValue("SOCKET_URL"))
                    .setAttribute("applicationName", "ezto verify mobile app")
                    .setAttribute("appStore", eztoVrfyIOSDownloadProvider.getDownloadDetails().getDownloadUrl())
                    .setAttribute("playStore", eztoVrfyAndroidDownloadProvider.getDownloadDetails().getDownloadUrl())
                    .setAttribute("showDevices", "true");


            context.challenge(loginFormsProvider.createForm("web-2-device.ftl"));
            return;
        }

        if (Boolean.parseBoolean(cibaAuthConfigByProviderId.getConfig().get(PROXIMITY_ENABLE)) && context.getRealm().getTrxValue(context.getRequestId(), SP_WORKFLOW).equalsIgnoreCase("verification")) {
            //TODO: Here we have to check the transaction is a onboarding flow or verification flow
            // if its onboarding flow we skip this
            // if its only verification flow we enable the proximity concept
            if (registeredDevices.size() >= 1) {
                context.getRealm().addAttribute(context.getRequestId(), "web_2_device_cred_id", registeredDevices.get(0).getId());
                context.challenge(loginFormsProvider.createForm("web-2-device-geo.ftl"));
                return;
            }
        }

        triggerMobile(context, registeredDevices, loginFormsProvider);
    }

    private void triggerMobile(RequiredActionContext context, List<CredentialModel> registeredCount, LoginFormsProvider loginFormsProvider) {
        if (context.getConfig().containsKey("is-prompt") && "true".equalsIgnoreCase(context.getConfig().get("is-prompt"))) {
            int promptNumber = GeneralHelper.getRandomTwoDigitNumber();
            loginFormsProvider.setAttribute("isPrompt", "true");
            loginFormsProvider.setAttribute("promptNumber", String.valueOf(promptNumber));

            context.getAuthenticationSession().setAuthNote("promptNumber", String.valueOf(promptNumber));
        }

        String appId = context.getConfig().get("app-id");

        ClientModel clientModel = context.getSession().clients().getClientByClientId(context.getRealm(), appId);
        if (clientModel == null) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR, "Application not found for web to device.");
            context.failure();
            return;
        }


        triggerMobileVerification(context, registeredCount, loginFormsProvider, clientModel);
    }

    private void triggerMobileVerification(RequiredActionContext context, List<CredentialModel> registeredCount, LoginFormsProvider loginFormsProvider, ClientModel clientModel) {
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", context.getUser().getUsername());

        Map<String, Object> userMetadata = new HashMap<>();
        userMetadata.put("ik@skip_user_attrs_delete", true);
        userMetadata.put(T1_REQUEST_ID, context.getRequestId());
        userMetadata.put(WEB_TO_DEVICE, true);
        context.getRealm().getTrx(context.getRequestId()).getAttributes().keySet().forEach(x -> {
            if (context.getRealm().getRequiredActionProviderById(x) == null && !x.equalsIgnoreCase("hidden_ua_" + BINDING_MESSAGE) && !x.contains("_counter")) {
                userMetadata.put(x.replace("sp@", ""), context.getRealm().getTrxValue(context.getRequestId(), x));
            }
        });

        boolean hasMessageMetadata = context.getRealm().hasTrxValueAttribute(context.getRequestId(), "hidden_ua_" + BINDING_MESSAGE);
        if (hasMessageMetadata) {
            try {
                String bindingMessageString = context.getRealm().getTrxValue(context.getRequestId(), "hidden_ua_" + BINDING_MESSAGE);
                BindingMessage bindingMessage = JsonSerialization.readValue(bindingMessageString, BindingMessage.class);
                String numberPrompt = context.getConfig().getOrDefault("is-prompt", "false");
                if ("true".equalsIgnoreCase(numberPrompt)) {
                    bindingMessage.getMetadata().put("numberPrompt", String.valueOf(context.getAuthenticationSession().getAuthNote("promptNumber")));
                }
                HashMap<String, Object> metadata = new HashMap<>(JsonSerialization.readValue(JsonSerialization.writeValueAsString(bindingMessage), new TypeReference<Map<String, Object>>() {
                }));

                request.put("message", metadata);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            Map<String, Object> message = new HashMap<>();
            Map<String, Object> metadata = new HashMap<>();
            String numberPrompt = context.getConfig().getOrDefault("is-prompt", "false");
            if ("true".equalsIgnoreCase(numberPrompt)) {
                metadata.put("numberPrompt", String.valueOf(context.getAuthenticationSession().getAuthNote("promptNumber")));
            }
            message.put("metadata", metadata);
            request.put("message", message);
        }

        if (registeredCount.size() == 1) {
            TokenSecretData secretData = null;
            try {
                secretData = JsonSerialization.readValue(registeredCount.get(0).getSecretData(), TokenSecretData.class);
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
            if (secretData != null) {
                userMetadata.put("push_token", secretData.getToken());
                userMetadata.put("push_type", secretData.getPushType());
                loginFormsProvider.setAttribute("device", new TokenSecretData(null, null, secretData.getDeviceModel(), secretData.getDeviceOSVersion(), secretData.getDevicePlatform(), null, null, secretData.getPushType()));
            }
        }
        userData.put("metadata", userMetadata);
        request.put("user", userData);

        boolean isShowConsent = Boolean.parseBoolean(clientModel.getAttribute("ciba.consent"));
        if (isShowConsent) {
            Map<String, Object> messageData = new HashMap<>();
            BindingMessage requestBindingData = new BindingMessage();
            final boolean requestBindingMessage = context.getRealm().hasTrxValueAttribute(context.getRequestId(), "hidden_ua_" + BINDING_MESSAGE);
            if (requestBindingMessage) {
                try {
                    requestBindingData = JsonSerialization.readValue(context.getRealm().getTrxValue(context.getRequestId(), "hidden_ua_" + BINDING_MESSAGE), BindingMessage.class);
                } catch (IOException e) {
                    logger.error("failed to parse request binding message");
                }
            }

            if ("true".equalsIgnoreCase(context.getConfig().getOrDefault("is-prompt", "false"))) {
                requestBindingData.getMetadata().put("numberPrompt", String.valueOf(context.getAuthenticationSession().getAuthNote("promptNumber")));
            }

            String consentMessage = context.getConfig().get("consent-message");

            if ((consentMessage == null || consentMessage.isEmpty()) && requestBindingData.getConsent() == null) {
                GeneralHelper.auditEvent(context, EventType.EZ_TRANSACTION_REQUEST, "This application has consent " +
                        "functionality " +
                        "enabled, but " +
                        "the request contains invalid consent information." +
                        " The consent data must include a valid type, title, and message.");
                context.failure();
                return;
            }

            Map consentMessageObject = null;
            try {
                if (consentMessage != null && !consentMessage.isEmpty())
                    consentMessageObject = JsonSerialization.readValue(context.getConfig().get("consent-message"),
                            Map.class);
            } catch (IOException e) {
                GeneralHelper.auditEvent(context, EventType.EZ_TRANSACTION_REQUEST, "Invalid consent");
                context.failure();
                return;
            }

            messageData.put("consent", consentMessageObject != null ? consentMessageObject.get("consent") :
                    requestBindingData.getConsent());
            messageData.put("metadata", requestBindingData.getMetadata());

            request.put("message", messageData);
        }

        CibaAuthConfigModel config = context.getRealm().getCibaAuthConfigByProviderId(clientModel.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER), clientModel.getClientId(), context.getRealm().getId());
        String timeout = config.getConfig().get(VERIFICATION_EXPIRY_CONFIG);
        if (timeout == null) {
            timeout = "120s";
        }

        OkHttpClient client = HttpUtility.getUnsafeOkHttpClient();
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
        String content = null;
        if (!request.isEmpty()) {
            try {
                content = "&request=" + (Boolean.parseBoolean(clientModel.getAttribute(ENABLE_CIBA_REQUEST_ENCRYPTION)) ?
                        URLEncoder.encode(GeneralHelper.encryptUserReqBody(context.getRealm(), clientModel, JsonSerialization.writeValueAsString(request)), StandardCharsets.UTF_8) :
                        JsonSerialization.writeValueAsString(request));
            } catch (IOException e) {
                logger.error(String.format("Failed to generate transaction request URI: %s", e.getMessage()));
            }
        }
        UriInfo backendUriInfo = context.getSession().getContext().getUri(UrlType.BACKEND);
        String authUrl = CibaGrantType.authorizationUrl(backendUriInfo.getBaseUriBuilder()).build(context.getRealm().getName()).toString();
        RequestBody body = RequestBody.create(content == null ? new byte[]{} : content.getBytes(), mediaType);
        Request requestBuilder = new Request.Builder()
                .url(authUrl)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Authorization", BasicAuthHelper.createHeader(clientModel.getClientId(), clientModel.getSecret()))
                .addHeader("API-version", "1")
                .method("POST", body)
                .build();
        try (okhttp3.Response response = client.newCall(requestBuilder).execute()) {
            if (!response.isSuccessful()) {
                showView(context, "contact-admin", BACKOFFICE_ERROR_FTL);
                return;
            }

            Map<String, String> resObj = JsonSerialization.readValue(response.body().string(), Map.class);
            context.getAuthenticationSession().setAuthNote("requestId", resObj.get("request_id"));
            String qr = resObj.get("qr");
            String deepLink = resObj.get("deep_link");
            Map<String, String> callBackResponse = new HashMap<>();
            callBackResponse.put("trxId", resObj.get("request_id"));
            callBackResponse.put("isToken", Boolean.toString(qr != null));
            addWebDetailsToTrx(context, resObj.get("request_id"));
            if (qr != null) {
                callBackResponse.put("qr", qr);
            }
            callBackResponse.put("pollUrl", EnvironmentReader.getValue("SOCKET_URL"));
            loginFormsProvider
                    .setAttribute("trxId", resObj.get("request_id"))
                    .setAttribute("isPush", String.valueOf(qr == null))
                    .setAttribute("showDevices", "false")
                    .setAttribute("timeout", timeout)
                    .setAttribute("applicationName", "ezto verify mobile app")
                    .setAttribute("pollUrl", EnvironmentReader.getValue("SOCKET_URL"));
            if (qr != null) {
                loginFormsProvider.
                        setAttribute("deep_link", deepLink)
                        .setAttribute("qr", qr)
                        .setAttribute("appStore", eztoVrfyIOSDownloadProvider.getDownloadDetails().getDownloadUrl())
                        .setAttribute("playStore",
                                eztoVrfyAndroidDownloadProvider.getDownloadDetails().getDownloadUrl());
            }

            context.challenge(loginFormsProvider.createForm("web-2-device.ftl"));
        } catch (IOException e) {
        }
    }

    private void addWebDetailsToTrx(RequiredActionContext context, String requestId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        context.getRealm().addAttribute(requestId, "ik@" + IP, context.getConnection().getRemoteAddr());
        context.getRealm().addAttribute(requestId, "ik@" + DEVICE, context.getConnection().getClientOs());
        context.getRealm().addAttribute(requestId, "ik@" + LOCATION, context.getConnection().getClientLocation().getLocation());
        context.getRealm().addAttribute(requestId, "ik@" + DATE_AND_TIME,
                ZonedDateTime.now(ZoneId.of(context.getConnection().getClientLocation().getBody().getZoneId()))
                        .format(formatter));
    }

    private void showView(RequiredActionContext context, String error, String ftl) {
        LoginFormsProvider formsProvider = context.form();
        if (error != null) {
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        formsProvider.setAttribute(DEFREG, context.getRealm().getDefaultRegion());
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String retry = formData.getFirst("retry");
        String trxId = formData.getFirst("trxId");
        String cancel = formData.getFirst("cancel");
        String from = formData.getFirst("from");


        if (Boolean.parseBoolean(cancel)) {
            com.qlikverify.backoffice.helpers.GeneralHelper.handleCancel(context, EventType.EZ_WHATSAPP_VERIFICATION_ERROR);
            return;
        }

        if (Boolean.parseBoolean(retry)) {
            requiredActionChallenge(context);
            return;
        }

        if (from != null && from.equals("web_2_device_geo")) {

            String error = formData.getFirst("error");
            if (error != null && !error.isEmpty()) {
                LoginFormsProvider loginFormsProvider = context.form();
                loginFormsProvider.setAttribute("error", error);
                context.challenge(loginFormsProvider.createForm("web-2-device-geo.ftl"));
                return;
            }

            String latitude = formData.getFirst("latitude");
            String longitude = formData.getFirst("longitude");
            String accuracy = formData.getFirst("accuracy");
            if (latitude != null && !latitude.isEmpty() && longitude != null && !longitude.isEmpty()) {
                double lat = (new BigDecimal(Double.valueOf(latitude)).setScale(5, RoundingMode.HALF_UP)).doubleValue();
                double lng = (new BigDecimal(Double.valueOf(longitude)).setScale(5, RoundingMode.HALF_UP)).doubleValue();
                context.getRealm().addAttribute(context.getRequestId(), "web_lat", String.valueOf(lat));
                context.getRealm().addAttribute(context.getRequestId(), "web_long", String.valueOf(lng));
                context.getRealm().addAttribute(context.getRequestId(), "web_geo_acc", accuracy);
                context.getRealm().addAttribute(context.getRequestId(), "web_ip", context.getConnection().getRemoteAddr());
                String credId = context.getRealm().getTrxValue(context.getRequestId(), "web_2_device_cred_id");
                CredentialModel credModel = context.getSession().userCredentialManager().getStoredCredentialById(context.getRealm(), context.getUser(), credId);
                String appId = context.getConfig().get("app-id");

                ClientModel clientModel = context.getSession().clients().getClientByClientId(context.getRealm(), appId);
                if (clientModel == null) {
                    com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR, "Application not found for web to device.");
                    context.failure();
                    return;
                }
                if (formData.get("promptNumber") != null) {
                    context.form().setAttribute("promptNumber", String.valueOf(formData.get("promptNumber").get(0)));
                    context.form().setAttribute("isPrompt", "true");
                }

                CibaAuthConfigModel cibaAuthConfigByProviderId = context.getRealm().getCibaAuthConfigByProviderId(clientModel.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER), clientModel.getClientId(), context.getRealm().getId());


                boolean isQR = cibaAuthConfigByProviderId.getConfig().get("push-way") != null && cibaAuthConfigByProviderId.getConfig().get(
                        "push-way").equalsIgnoreCase("qr");

                List<CredentialModel> registeredDevices = context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), TokenCredentialModel.TYPE).filter(x -> x.isEnabled()).filter(x -> {
                    try {
                        Map<String, String> secretData = JsonSerialization.readValue(x.getSecretData(), Map.class);
                        return !secretData.get("pushType").equals("desktop");
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        e.printStackTrace();
                    }
                    return false;
                }).collect(Collectors.toList());

                if (registeredDevices.size() > 1 && !isQR) {

                    List<TokenSecretData> deviceDetails = registeredDevices.stream().filter(x -> x.isEnabled()).map(x -> {
                        try {
                            return JsonSerialization.readValue(x.getSecretData(), TokenSecretData.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }).map(x ->
                            new TokenSecretData(null, x.getDeviceId(), x.getDeviceModel(), x.getDeviceOSVersion(), x.getDevicePlatform(), null, null, null)
                    ).collect(Collectors.toList());

                    LoginFormsProvider loginFormsProvider = context.form();
                    loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());

                    loginFormsProvider
                            .setAttribute("devices", deviceDetails)
                            .setAttribute("trxId", "null")
                            .setAttribute("pollUrl", EnvironmentReader.getValue("SOCKET_URL"))
                            .setAttribute("applicationName", "ezto verify mobile app")
                            .setAttribute("appStore", eztoVrfyIOSDownloadProvider.getDownloadDetails().getDownloadUrl())
                            .setAttribute("playStore", eztoVrfyAndroidDownloadProvider.getDownloadDetails().getDownloadUrl())
                            .setAttribute("showDevices", "true");


                    context.challenge(loginFormsProvider.createForm("web-2-device.ftl"));
                    return;
                }

                triggerMobileVerification(context, List.of(credModel), context.form(), clientModel);
                return;
            } else {
                requiredActionChallenge(context);
                return;
            }
        }

        String showDevices = formData.getFirst("showDevices");
        if (Boolean.parseBoolean(showDevices)) {
            String selectedDevice = formData.getFirst("selectedDevice");
            if (selectedDevice == null) {
                requiredActionChallenge(context);
                return;
            }
            List<CredentialModel> registeredCount = context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), TokenCredentialModel.TYPE).filter(x -> x.isEnabled()).filter(x -> x.getSecretData().contains(selectedDevice)).collect(Collectors.toList());
            triggerMobile(context, registeredCount, context.form());
            return;
        }

        String authReqId = formData.getFirst("authReqId");

        if (authReqId == null) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR, "no_auth_req_id_received");
            showView(context, "invalid_request", BACKOFFICE_ERROR_FTL);
            return;
        }

        String appId = context.getConfig().get("app-id");

        ClientModel clientModel = context.getSession().clients().getClientByClientId(context.getRealm(), appId);
        if (clientModel == null) {
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR,
                    "Application not found for web to device.");
            context.failure();
            return;
        }

        OkHttpClient client = HttpUtility.getUnsafeOkHttpClient();
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
        String content = "app_id=" + clientModel.getClientId() + "&app_secret=" + clientModel.getSecret() +
                "&auth_req_id=" + authReqId;
        UriInfo backendUriInfo = context.getSession().getContext().getUri(UrlType.BACKEND);
        String authUrl = OIDCLoginProtocolService.backofficetokenUrl(backendUriInfo.getBaseUriBuilder()).build(context.getRealm().getName()).toString();
        RequestBody body = RequestBody.create(mediaType, content.getBytes());
        Request requestBuilder = new Request.Builder()
                .url(authUrl)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Authorization", BasicAuthHelper.createHeader(clientModel.getClientId(), clientModel.getSecret()))
                .addHeader("API-version", "1")
                .method("POST", body)
                .build();
        try (okhttp3.Response response = client.newCall(requestBuilder).execute()) {
            if (!response.isSuccessful()) {
                Map<String, String> additionalDetails = new HashMap<>();
                if (response.body() != null) {
                    String resBody = response.body().string();
                    additionalDetails.put("reason", resBody != null ? resBody : "Failed to fetch the result");
                    context.failure();
                    return;
                }
//                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR, String.valueOf(response.code()));
                showView(context, "invalid_request", BACKOFFICE_ERROR_FTL);
                return;
            }

            Map<String, String> resObj = JsonSerialization.readValue(response.body().string(), Map.class);

            String resTrxId = resObj.get("request_id");
            if (!resTrxId.equals(trxId)) {
                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR, "invalid_transaction");
                context.failure();
                return;
            }

            String userId = resObj.get("user_id");
            if (!context.getUser().getUsername().equals(userId)) {
                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR, "invalid_transaction_user");
                context.failure();
                return;
            }

            if (resObj.containsKey("sp@error")) {
                updateResult(context, resObj);
                context.failure();
                return;
            } else if (context.getConfig().containsKey("is-prompt") && "true".equalsIgnoreCase(context.getConfig().get("is-prompt"))
                    && resObj.containsKey("prompt_success") && "false".equals(resObj.get("prompt_success"))) {
                updateResult(context, resObj);
                context.failure();
                return;
            } else {
                updateResult(context, resObj);
            }

            context.success();
        } catch (Exception err) {
            Map<String, String> additionalDetails = new HashMap<>();
            additionalDetails.put("reason", err.getMessage());
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_WEB_2_DEVICE_ERROR, "Application not found for web to device.", additionalDetails);
            showView(context, "contact-admin", BACKOFFICE_ERROR_FTL);
        }

    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOP
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return "bo-web-to-device";
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("allow_new_device", true);
        return metadata;
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> claims = new LinkedHashMap<>();
        String clientId = model.getConfig().get("app-id");
        if (Validation.isBlank(clientId)) {
            return claims;
        }
        RealmModel realmModel = session.getContext().getRealm();
        ClientModel clientModel = session.clients().getClientByClientId(realmModel, clientId);
        if (clientModel == null) {
            logger.error("getActionClaims() :: Web2Device App ID Not Found");
            return claims;
        }
        Map<String, String> flowBindingOverrides = clientModel.getAuthenticationFlowBindingOverrides();

        for (String key : new String[]{"onboarding-action", "verification-action"}) {
            String actionId = flowBindingOverrides.get(key);
            if (!Validation.isBlank(actionId)) {
                getAllActionClaimsFromMappedWorkflow(session, realmModel, actionId, claims);
            } else
                logger.errorv("getActionClaims() :: {0} is not present for {1} client", key, clientModel.getName());
        }
        return claims;
    }

    private void getAllActionClaimsFromMappedWorkflow(KeycloakSession session, RealmModel realmModel, String actionId, Map<String, String> claims) {
        realmModel.getRequiredActionProvidersStream(actionId)
                .forEach(actionModel -> {
                    RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory()
                            .getProviderFactory(RequiredActionProvider.class, actionModel.getProviderId());
                    if (factory != null) {
                        Map<String, String> actionClaims = factory.getActionClaims(session, actionModel);
                        if (actionClaims != null) {
                            claims.putAll(actionClaims);
                        }
                    }
                });
    }

    @Override
    public boolean skippable() {
        return false;
    }

    @Override
    public boolean isClaimsCustomizable() {
        return false;
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{"web_to_device"};
    }
}


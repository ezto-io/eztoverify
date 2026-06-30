package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.backoffice.creds.device.TokenCredentialModel;
import com.qlikverify.backoffice.creds.device.TokenCredentialProvider;
import com.qlikverify.backoffice.creds.device.TokenCredentialProviderFactory;
import com.qlikverify.backoffice.creds.device.TokenSecretData;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.util.RSAHelper;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.authorizers.MobileAuthorizer.PUSH_TOKEN;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;

public class DeviceRegister implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(DeviceRegister.class);

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "bo-device-register";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String SECRET_KEY = "secret_key";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ALLOW_MULTI_DEVICE);
        property.setLabel("allow-multi-device-reg");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("allow-multi-device-reg-hlptxt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("max_device_count");
        property.setLabel("max-device-count");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("max-device-count-hlptxt");
        Map<String, Object> maxDeviceCountDependsOn = new HashMap<>();
        maxDeviceCountDependsOn.put(ALLOW_MULTI_DEVICE, true);
        property.setDependsOn(maxDeviceCountDependsOn);
        property.setDefaultValue(1);
        property.setMinVal(1);
        property.setMaxVal(10);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("clear-data");
        property.setLabel("clear-data");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("clear-data-hlptext");
        configProperties.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {

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
    public String[] stripeMappingId() {
        return new String[]{"device_register"};
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context,
                    context.getPolicyId(), context.getUser());
        }
        String requestId = context.getRequestId();

        boolean isOnboardingFlow = GeneralHelper.isOnboardingFlow(context);
        if (isOnboardingFlow && context.getAuthenticationSession().getAuthNote(context.getModel().getId()) == null) {
            cleanUpForResume(context);
        }

        boolean deviceRegistered = context.getRealm().hasTrxValueAttribute(requestId, DEVICE_REGISTERED);
        boolean deviceRegisteredAt = context.getRealm().hasTrxValueAttribute(requestId, DEVICE_REGISTERED_AT);
        boolean deviceRegisterFailureReason = context.getRealm().hasTrxValueAttribute(requestId,
                DEVICE_REGISTER_FAILED_REASON);

        if (result && (!deviceRegistered && !deviceRegisteredAt &&
                !deviceRegisterFailureReason)) {
            addAction(context);
        } else {
            context.getUser().removeRequiredAction(context.getModel().getId());
        }
    }

    private void cleanUpForResume(RequiredActionContext context) {
        String shouldCleanUp = context.getConfig().getOrDefault("clear-data", "false");
        if (Boolean.parseBoolean(shouldCleanUp) && Validation.isBlank(context.getAuthenticationSession().getAuthNote(context.getModel().getId()))) {
            boolean hasDeviceDetails = context.getRealm().hasTrxValueAttribute(context.getRequestId(), "push_token");
            if (hasDeviceDetails) {
                String pushToken = context.getRealm().getTrxValue(context.getRequestId(), "push_token");
                Optional<CredentialModel> deviceCredentials = context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), TokenCredentialModel.TYPE).filter(x -> {
                    String credentialData = x.getSecretData();
                    try {
                        TokenSecretData tokenSecretData = JsonSerialization.readValue(credentialData, TokenSecretData.class);
                        String token = tokenSecretData.getToken();
                        return token.equalsIgnoreCase(pushToken);
                    } catch (IOException e) {
                        return false;
                    }
                }).findAny();

                if (deviceCredentials.isEmpty()) {
                    return;
                }

                CredentialModel credentialModel = deviceCredentials.get();
                boolean success = getCredentialProvider(context).deleteCredential(context.getRealm(), context.getUser(), credentialModel.getId());

                context.getRealm().removeTrxAttribute(context.getRequestId(), DEVICE_REGISTER_FAILED_REASON);
                context.getRealm().removeTrxAttribute(context.getRequestId(), DEVICE_REGISTERED);
                context.getRealm().removeTrxAttribute(context.getRequestId(), DEVICE_REGISTERED_AT);
                context.getRealm().removeTrxAttribute(context.getRequestId(), PUSH_TOKEN);

                context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
            }
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
        showVerificationView(context);
    }

    private void showVerificationView(RequiredActionContext context) {
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);

        HashMap<String, String> publicKeyString = generateKeyPair(context);

        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute(PUBLIC_KEY, publicKeyString.get(PUBLIC_KEY));
        context.getAuthenticationSession().setAuthNote(PRIVATE_KEY, publicKeyString.get(PRIVATE_KEY));
        loginFormsProvider.setAttribute("ismobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop());
        context.challenge(loginFormsProvider.createForm("device-register.ftl"));
    }

    private HashMap<String, String> generateKeyPair(RequiredActionContext context) {
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        byte[] privateKeyBytes = privateKey.getEncoded();

        byte[] publicKeyBytes = publicKey.getEncoded();

        // Encode the private key bytes in Base64
        String privateKeyString = Base64.getEncoder().encodeToString(privateKeyBytes);

        // Encode the private key bytes in Base64
        String publicKeyString = Base64.getEncoder().encodeToString(publicKeyBytes);

        HashMap<String, String> generatedKeyPair = new HashMap<>();
        generatedKeyPair.put(PUBLIC_KEY, publicKeyString);
        generatedKeyPair.put(PRIVATE_KEY, privateKeyString);
        return generatedKeyPair;
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId =
                context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String requestId = context.getRequestId();
        incrementModuleCounter(requestId, context.getRealm(), context.getUser(), "device_register");
        if (formData.getFirst("isretry") != null && Boolean.parseBoolean(formData.getFirst("isretry"))) {
            showVerificationView(context);
            return;
        }
        long registeredCount =
                context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(),
                        context.getUser(), TokenCredentialModel.TYPE).filter(x -> x.isEnabled()).count();
        boolean allowMultiDevice = Boolean.parseBoolean(context.getConfig().get(ALLOW_MULTI_DEVICE));
        if (allowMultiDevice) {
            String count = com.qlikverify.helpers.GeneralHelper.getConfigValue(context.getConfig(), "max_device_count"
                    , "1");
            if (registeredCount >= Integer.parseInt(count)) {
                GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_REGISTER, "Reached Max Device Registered Count");
                context.getRealm().addAttribute(requestId, DEVICE_REGISTERED, "false");
                context.getRealm().addAttribute(requestId, DEVICE_REGISTERED_AT, String.valueOf(Time.currentTime()));
                context.getRealm().addAttribute(requestId, DEVICE_REGISTER_FAILED_REASON, "Reached Max Device Registered Count");
                context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
                context.success();
                return;
            }
        } else if (registeredCount == 1) {
            GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_REGISTER, "Already user have a device registered");

            context.getRealm().addAttribute(requestId, DEVICE_REGISTERED, "false");
            context.getRealm().addAttribute(requestId, DEVICE_REGISTER_FAILED_REASON, "Exceed the maximum device " +
                    "count");
            context.getRealm().addAttribute(requestId, DEVICE_REGISTERED_AT, String.valueOf(Time.currentTime()));
            context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
            context.success();
            return;
        }

        String device = formData.getFirst("device");
        HashMap<String, String> deviceDetails = null;
        try {
            deviceDetails = JsonSerialization.readValue(device, HashMap.class);
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
        String deviceId = deviceDetails.get("id");
        String token = formData.getFirst("token");
        String pushType = formData.getFirst("pushType");
        if (pushType != null && !pushType.isEmpty()) {
            switch (pushType.toLowerCase()) {
                case "hms":
                    pushType = "hms";
                    break;
                case "socket":
                    pushType = "desktop";
                    break;
                default:
                    pushType = "fcm";
            }
        }
        List<HashMap<String, String>> enabledCredentials = new ArrayList<>();
        List<String> credIdWithSameToken = new ArrayList<>();


        final Optional<CredentialModel> optionalCredentialModel =
                context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(),
                        context.getUser(), TokenCredentialModel.TYPE).filter(credentialModel -> {
                    try {
                        return !credentialModel.isEnabled() && JsonSerialization.readValue(credentialModel.getSecretData(),
                                new TypeReference<HashMap<String, String>>() {
                                }).get("deviceId").equals(deviceId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }).findAny();

        if (optionalCredentialModel.isPresent()) {
            GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_REGISTER_ERROR,
                    "The requested device is disabled.");
            context.failure();
            return;
        }

        Map<String, String> deviceRegisterFailed = new HashMap<>();

        if (!Boolean.parseBoolean(formData.getFirst("success"))) {
            GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_REGISTER_ERROR, "Failed to register device");
            context.failure();
            return;
        }

        String encryptedDeviceId = formData.getFirst("encryptedDeviceId");

        if (encryptedDeviceId == null || encryptedDeviceId.isEmpty()) {
            GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_REGISTER_ERROR, "No encrypted device Id received");
            deviceRegisterFailed.put(ERROR_MESSAGE, "No encrypted device Id received");
            GeneralHelper.showView(context, null, BACKOFFICE_ERROR_FTL, deviceRegisterFailed);
            return;
        }

        String decryptedDeviceId = "";
        try {
            decryptedDeviceId = RSAHelper.decrypt(encryptedDeviceId,
                    context.getAuthenticationSession().getAuthNote(PRIVATE_KEY), "RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        } catch (Exception e) {
            GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_REGISTER_ERROR, "Error while getting device id");
            deviceRegisterFailed.put(ERROR_MESSAGE, "Error while getting device id");
            GeneralHelper.showView(context, null, BACKOFFICE_ERROR_FTL, deviceRegisterFailed);
            return;
        }


        String finalDecryptedDeviceId = decryptedDeviceId;
        context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(),
                context.getUser(), TokenCredentialModel.TYPE).forEach(credentialModel -> {
            try {
                enabledCredentials.add(JsonSerialization.readValue(credentialModel.getSecretData(),
                        new TypeReference<HashMap<String, String>>() {
                        }));
                if (enabledCredentials.get(enabledCredentials.size() - 1).get("encryptedDeviceId").equals(finalDecryptedDeviceId)) {
                    credIdWithSameToken.add(credentialModel.getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

        });

        if (token != null && deviceDetails != null) {
            TokenCredentialProvider sqcp = getCredentialProvider(context);
            try {
                long userWithSameToken = 0;
                if (!enabledCredentials.isEmpty()) {
                    if (enabledCredentials.stream().filter(x -> x.get("encryptedDeviceId").equals(finalDecryptedDeviceId)).count() == 0) {
                        sqcp.createCredential(context.getRealm(), context.getUser(),
                                TokenCredentialModel.create(token, deviceDetails.get("id"),
                                        deviceDetails.get("model"), deviceDetails.get("osVersion"),
                                        deviceDetails.get("platform"), decryptedDeviceId.substring(0, 5),
                                        decryptedDeviceId, pushType));
                    } else {
                        userWithSameToken =
                                enabledCredentials.stream().filter(x -> x.get("encryptedDeviceId")
                                        .equals(finalDecryptedDeviceId) && !x.get("deviceId").equals(deviceId)).count();

                        if (userWithSameToken != 0 && !credIdWithSameToken.isEmpty()) {
                            context.getSession().userCredentialManager().removeStoredCredential(context.getRealm(),
                                    context.getUser(), credIdWithSameToken.get(0));
                            sqcp.createCredential(context.getRealm(), context.getUser(),
                                    TokenCredentialModel.create(token, deviceDetails.get("id"),
                                            deviceDetails.get("model"), deviceDetails.get("osVersion"),
                                            deviceDetails.get("platform"), decryptedDeviceId.substring(0, 5),
                                            decryptedDeviceId, pushType));
                        } else {
                            GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_REGISTER,
                                    String.format("Device (%s) is already registered", deviceDetails.get("model")));
                            context.getRealm().addAttribute(requestId, DEVICE_REGISTERED, "false");
                            context.getRealm().addAttribute(requestId, DEVICE_REGISTER_FAILED_REASON,
                                    String.format("Device (%s) is already registered", deviceDetails.get("model")));
                            context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
                            context.success();
                            return;
                        }
                    }
                } else {
                    sqcp.createCredential(context.getRealm(), context.getUser(), TokenCredentialModel.create(token,
                            deviceDetails.get("id"), deviceDetails.get("model"), deviceDetails.get("osVersion"),
                            deviceDetails.get("platform"), decryptedDeviceId.substring(0, 5), decryptedDeviceId, pushType));
                }


                CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
                if (cibaAuthProvider.isMobile()) {
                    context.getRealm().addAttribute(requestId, PUSH_TOKEN, token);
                }
                context.getRealm().addAttribute(requestId, DEVICE_REGISTERED, "true");
                context.getRealm().addAttribute(requestId, DEVICE_REGISTERED_AT, String.valueOf(Time.currentTime()));
                GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_REGISTER, null);
            } catch (IOException e) {
                GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_REGISTER, e.getMessage());
                context.getRealm().addAttribute(requestId, DEVICE_REGISTERED, "false");
                context.getRealm().addAttribute(requestId, DEVICE_REGISTER_FAILED_REASON, "Something went wrong");
                context.getRealm().addAttribute(requestId, DEVICE_REGISTERED_AT, String.valueOf(Time.currentTime()));
            }
        } else {

            GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_REGISTER, "No " + deviceDetails == null ? "device " +
                    "details" : "token " + "Received");
            context.getRealm().addAttribute(requestId, DEVICE_REGISTERED, "false");
            context.getRealm().addAttribute(requestId, DEVICE_REGISTER_FAILED_REASON, "No token or device details " +
                    "Received");
            context.getRealm().addAttribute(requestId, DEVICE_REGISTERED_AT, String.valueOf(Time.currentTime()));
        }

        context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
        context.success();
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return "bo-device-register";
    }

    public TokenCredentialProvider getCredentialProvider(RequiredActionContext context) {
        EventBuilder eventBuilder = GeneralHelper.getEvent(context, EventType.EZ_DEVICE_REGISTER);
        return ((TokenCredentialProvider) context.getSession().getProvider(CredentialProvider.class,
                TokenCredentialProviderFactory.PROVIDER_ID)).setEvent(eventBuilder);
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put(DEVICE_REGISTERED, ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put(DEVICE_REGISTERED_AT, ValidationTypes.DATE_AND_TIME.getValue());
        defaultClaims.put(PUSH_TOKEN, ValidationTypes.STRING.getValue());

        return defaultClaims;
    }

    @Override
    public boolean skippable() {
        return false;
    }

    @Override
    public boolean isClaimsCustomizable() {
        return false;
    }
}

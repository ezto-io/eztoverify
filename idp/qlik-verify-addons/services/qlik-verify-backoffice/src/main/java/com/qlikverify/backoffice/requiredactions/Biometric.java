package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.backoffice.creds.biometric.BiometricCredentialInput;
import com.qlikverify.backoffice.creds.biometric.BiometricCredentialModel;
import com.qlikverify.backoffice.creds.biometric.BiometricCredentialProvider;
import com.qlikverify.backoffice.creds.biometric.BiometricCredentialProviderFactory;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.qlikverify.backoffice.models.deviceDetails.DeviceDetails;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;

public class Biometric implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final String BIOMETRIC_REGISTER = "biometric_register";
    private static final String BIOMETRIC_VERIFY = "biometric_verify";

    private static final Logger logger = Logger.getLogger(Biometric.class);

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "bo-biometric";
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
        property.setHelpText("allow-multiple-devices-for-same-user");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(MAX_DEVICE_COUNT);
        property.setLabel("max-device-count");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("max-device-count-hlptxt");
        Map<String, Object> maxDeviceCount = new HashMap<>();
        maxDeviceCount.put(ALLOW_MULTI_DEVICE, true);
        property.setDependsOn(maxDeviceCount);
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
        List<Map> hmacKeys = session.getContext().getRealm().getComponentsStream(session.getContext().getRealm().getId()).filter(x -> x.getProviderId().equals("hmac-generated")).filter(x -> x.get("enabled", false) && x.get("active", false) && x.get("secretSize") != null && x.get("secretSize").equals("256") && x.get("algorithm") != null && x.get("algorithm").equals("HS256"))
                .map(x -> {
                    HashMap<String, String> key = new HashMap<>();
                    key.put("name", x.getName());
                    key.put("value", x.get("kid"));
                    return key;
                })
                .collect(Collectors.toList());
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName() != null && x.getName().equals(SECRET_KEY)).findFirst();
        ProviderConfigProperty provider;
        provider = providerConfigProperty.orElseGet(ProviderConfigProperty::new);
        provider.setName(SECRET_KEY);
        provider.setLabel("secret-keys-label");
        provider.setHelperLink("");
        provider.setHelperLinkText("");
        provider.setRequired(true);

        if (hmacKeys.isEmpty()) {
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("");
            provider.setHelperLinkText("no-hmac-keys");
            provider.setHelperLink("/create/keys/" + session.getContext().getRealm().getName() + "/providers/hmac-generated");
        } else {
            provider.setType(ProviderConfigProperty.MAP_LIST_TYPE);
            provider.setMapOptions(hmacKeys);
            provider.setHelperLinkText("");
            provider.setHelperLink("");
            provider.setHelpText("select-key-enable-hashing-biometric-data");
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
    public String[] stripeMappingId() {
        return new String[]{
                BIOMETRIC_REGISTER,
                BIOMETRIC_VERIFY
        };
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
        }
        String requestId = context.getRequestId();
        boolean isOnboardingFlow = GeneralHelper.isOnboardingFlow(context);

        if (isOnboardingFlow && context.getAuthenticationSession().getAuthNote(context.getModel().getId()) == null) {
            cleanUpForResume(context);
        }


        boolean biometricVerified = context.getRealm().hasTrxValueAttribute(requestId, BIOMETRIC_VERIFIED);
        boolean biometricVerifiedAt = context.getRealm().hasTrxValueAttribute(requestId, BIOMETRIC_VERIFIED_AT);
        if (result && (!biometricVerified || !biometricVerifiedAt)) {
            addAction(context);
        } else {
            context.getUser().removeRequiredAction(context.getModel().getId());
        }
    }

    private void cleanUpForResume(RequiredActionContext context) {
        String shouldCleanUp = context.getConfig().getOrDefault("clear-data", "false");
        if (Boolean.parseBoolean(shouldCleanUp) && Validation.isBlank(context.getAuthenticationSession().getAuthNote(context.getModel().getId()))) {
            boolean hasDeviceDetails = context.getRealm().hasTrxValueAttribute(context.getRequestId(), BIOMETRIC_DEVICE);
            if (hasDeviceDetails) {
                String deviceDetails = context.getRealm().getTrxValue(context.getRequestId(), BIOMETRIC_DEVICE);
                try {
                    HashMap<String, Object> details = JsonSerialization.readValue(deviceDetails, new TypeReference<HashMap<String, Object>>() {
                    });
                    boolean success = getCredentialProvider(context).deleteCredential(context.getRealm(), context.getUser(), (String) details.get("id"));

                    context.getRealm().removeTrxAttribute(context.getRequestId(), BIOMETRIC_VERIFIED);
                    context.getRealm().removeTrxAttribute(context.getRequestId(), BIOMETRIC_VERIFIED_AT);
                    context.getRealm().removeTrxAttribute(context.getRequestId(), BIOMETRIC_DEVICE);

                    context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
                } catch (IOException e) {
                    // NOP
                }
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
        showCheckView(context);
    }

    private void showCheckView(RequiredActionContext context) {
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute(IS_MOBILE, cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop());
        context.challenge(loginFormsProvider.createForm("biometric-check.ftl"));
    }

    private void showVerificationView(RequiredActionContext context) {
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        String challenge = SecretGenerator.getInstance().randomString(16);
        context.getAuthenticationSession().setAuthNote(CHALLENGE, challenge);
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute(CHALLENGE, challenge);
        loginFormsProvider.setAttribute(IS_MOBILE, cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop());
        context.challenge(loginFormsProvider.createForm("biometric-verify.ftl"));
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId = context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        Map<String, String> biometricFailed = new HashMap<>();
        biometricFailed.put(ERROR_MESSAGE, "Biometric Failed");

        if (formData.getFirst("isretry") != null && Boolean.parseBoolean(formData.getFirst("isretry"))) {
            showCheckView(context);
            return;
        }

        String from = formData.getFirst("from");
        if (from == null) {
            GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "Invalid from");
            context.failure();
            return;
        }

        if (from.equalsIgnoreCase("biometric-check")) {
            biometricCheck(context, formData, biometricFailed);
            return;
        }

        if (from.equalsIgnoreCase("biometric-register")) {
                String count = com.qlikverify.helpers.GeneralHelper.getConfigValue(context.getConfig(), MAX_DEVICE_COUNT, "1");
                long registeredCount = context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), BiometricCredentialModel.TYPE).filter(x -> x.isEnabled()).filter(x -> !x.isTemporary()).count();
                if (registeredCount >= Integer.parseInt(count)) {
                    String requestId = context.getRequestId();
                    context.getRealm().addAttribute(requestId, BIOMETRIC_VERIFIED, "false");
                    context.getRealm().addAttribute(requestId, BIOMETRIC_FAILED_REASON, BIOMETRIC_MAX_DEVICE_REACHED);
                    GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, BIOMETRIC_MAX_DEVICE_REACHED);
                    context.failure();
                    return;
                }
            biometricRegister(context, formData);
            return;
        }

        if (from.equalsIgnoreCase("biometric-verify")) {
            biometricVerify(context, formData);
            return;
        }

        context.failure();
    }

    private void biometricCheck(RequiredActionContext context, MultivaluedMap<String, String> formData, Map<String, String> biometricFailed) {
        String registered = formData.getFirst("isRegistered");
        String device = formData.getFirst(DEVICE);
        if (device == null) {
            GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, NO_DEVICE_RECEIVED);
            context.failure();
            return;
        }
        if (Boolean.parseBoolean(registered)) {
            String credId = formData.getFirst("credids");
            if (credId != null) {
                String[] credids = new String[0];
                try {
                    credids = JsonSerialization.readValue(credId, String[].class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (credids == null || credids.length == 0) {
                    GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "No Credential Id received on Biometric check");
                    context.failure();
                    return;
                }
                String[] finalCredids = credids;
                long registeredCount = context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), BiometricCredentialModel.TYPE).filter(x -> !x.isTemporary() && x.isEnabled()).filter(x -> Arrays.asList(finalCredids).contains(x.getId())).count();
                if (registeredCount == 0) {
                    boolean allowMultiDevice;
                    if (context.getConfig().get(ALLOW_MULTI_DEVICE) == null || context.getConfig().get(ALLOW_MULTI_DEVICE).isBlank() || context.getConfig().get(ALLOW_MULTI_DEVICE).isEmpty()) {
                        allowMultiDevice = false;
                    } else {
                        allowMultiDevice = Boolean.parseBoolean(context.getConfig().get(ALLOW_MULTI_DEVICE));
                    }

                    long registeredDeviceCount = context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), BiometricCredentialModel.TYPE).filter(x -> !x.isTemporary() && x.isEnabled()).count();
                    if (allowMultiDevice) {
                        String count = com.qlikverify.helpers.GeneralHelper.getConfigValue(context.getConfig(), MAX_DEVICE_COUNT, "1");

                        if (registeredDeviceCount >= Integer.parseInt(count)) {
                            GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "Reached Max Device Registration Count");
                            context.getRealm().addAttribute(context.getRequestId(), BIOMETRIC_FAILED_REASON, "Reached Max Device Registration Count");
                            context.failure();
                            return;
                        }

                        showRegistrationView(context);
                        return;
                    } else {
                        if (registeredDeviceCount == 0) {
                            showRegistrationView(context);
                            return;
                        } else {
                            GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "Reached Max Device Registration Count");
                            context.getRealm().addAttribute(context.getRequestId(), BIOMETRIC_FAILED_REASON, "Reached Max Device Registration Count");
                            context.failure();
                            return;
                        }
                    }
                }
            }
        }
        boolean isRegistered = getCredentialProvider(context).isConfiguredFor(context.getRealm(), context.getUser(), BiometricCredentialModel.TYPE);
        if (!isRegistered) {
            showRegistrationView(context);
            return;
        }
        if (registered == null || !Boolean.parseBoolean(registered)) {
            boolean allowMultiDevice;
            if (context.getConfig().get(ALLOW_MULTI_DEVICE) == null || context.getConfig().get(ALLOW_MULTI_DEVICE).isBlank() || context.getConfig().get(ALLOW_MULTI_DEVICE).isEmpty()) {
                allowMultiDevice = false;
            } else {
                allowMultiDevice = Boolean.parseBoolean(context.getConfig().get(ALLOW_MULTI_DEVICE));
            }
            if (allowMultiDevice) {
                String count = com.qlikverify.helpers.GeneralHelper.getConfigValue(context.getConfig(), MAX_DEVICE_COUNT, "1");
                long registeredCount = context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), BiometricCredentialModel.TYPE).filter(x -> !x.isTemporary() && x.isEnabled()).count();
                if (registeredCount >= Integer.parseInt(count)) {
                    GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "Reached Max Device Registration Count");
                    context.getRealm().addAttribute(context.getRequestId(), BIOMETRIC_FAILED_REASON, "Reached Max Device Registration Count");
                    context.failure();
                    return;
                }

                showRegistrationView(context);
                return;
            }
        } else {
            showVerificationView(context);
            return;
        }

        String requestId = context.getRequestId();
        context.getRealm().addAttribute(requestId, BIOMETRIC_VERIFIED, "false");
        context.getRealm().addAttribute(requestId, BIOMETRIC_FAILED_REASON, BIOMETRIC_MAX_DEVICE_REACHED);
        GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, BIOMETRIC_MAX_DEVICE_REACHED);
        context.failure();
    }

    private void showRegistrationView(RequiredActionContext context) {
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        HashMap<String, String> publicKeyString = generateKeyPair(context);

        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute(PUBLIC_KEY, publicKeyString.get(PUBLIC_KEY));
        loginFormsProvider.setAttribute(CRED_ID, publicKeyString.get(CRED_ID));
        loginFormsProvider.setAttribute(IS_MOBILE, cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop());
        context.challenge(loginFormsProvider.createForm("biometric-register.ftl"));
    }

    private HashMap<String, String> generateKeyPair(RequiredActionContext context) {

        BiometricCredentialProvider sqcp = getCredentialProvider(context);
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        byte[] privateKeyBytes = privateKey.getEncoded();

        // Encode the private key bytes in Base64
        String privateKeyString = Base64.getEncoder().encodeToString(privateKeyBytes);

        byte[] publicKeyBytes = publicKey.getEncoded();

        // Encode the private key bytes in Base64
        String publicKeyString = Base64.getEncoder().encodeToString(publicKeyBytes);

        CredentialModel credentialModel = sqcp.createCredential(context.getRealm(), context.getUser(), BiometricCredentialModel.create(privateKeyString, new DeviceDetails(), ""));
        HashMap<String, String> generatedKeyPair = new HashMap<>();
        generatedKeyPair.put(PUBLIC_KEY, publicKeyString);
        generatedKeyPair.put(CRED_ID, credentialModel.getId());
        return generatedKeyPair;
    }

    private void biometricRegister(RequiredActionContext context, MultivaluedMap<String, String> formData) {
        if (formData.getFirst("success") != null && Boolean.parseBoolean(formData.getFirst("success"))) {
            String credId = formData.getFirst(CRED_ID);
            Map<String, String> biometricFailed = new HashMap<>();
            biometricFailed.put(ERROR_MESSAGE, "Biometric Failed");

            if (credId == null || credId.isEmpty()) {
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "No Credential Id received");
                context.failure();
                return;
            }
            String device = formData.getFirst(DEVICE);
            if (device == null) {
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, NO_DEVICE_RECEIVED);
                context.failure();
                return;
            }
            HashMap<String, String> deviceDetails = null;
            try {
                deviceDetails = JsonSerialization.readValue(device, new TypeReference<HashMap<String, String>>() {
                });
            } catch (IOException e) {
                e.printStackTrace();
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "No device details received : " + e.getMessage());
                context.failure();
                return;
            }

            String deviceId = deviceDetails.get("id");

            List<HashMap<String, String>> credentials = new ArrayList<>();
            context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), BiometricCredentialModel.TYPE).forEach(x -> {
                try {
                    if (!x.isEnabled()) {
                        credentials.add(JsonSerialization.readValue(x.getSecretData(), new TypeReference<HashMap<String, String>>() {
                        }));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

            });
            Map<String, String> deviceRegisterFailed = new HashMap<>();

            if (!credentials.isEmpty()) {
                if (credentials.stream().filter(x -> x.get("deviceId").equals(deviceId)).count() > 0) {
                    GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "The requested device is disabled.");
                    context.getRealm().addAttribute(context.getRequestId(), BIOMETRIC_FAILED_REASON, "The requested device is disabled.");
                    context.failure();
                    return;
                }
            }


            CredentialModel credentialModel = context.getSession().userCredentialManager().getStoredCredentialById(context.getRealm(), context.getUser(), credId);
            BiometricCredentialModel biometricCredentialModel = null;
            try {
                DeviceDetails deviceDetailsModel = new DeviceDetails();
                deviceDetailsModel.setId(deviceDetails.get("id"));
                deviceDetailsModel.setModel(deviceDetails.get("model"));
                deviceDetailsModel.setPlatform(deviceDetails.get("platform"));
                deviceDetailsModel.setPhysicalDevice(Boolean.parseBoolean(deviceDetails.get("isPhysicalDevice")));
                deviceDetailsModel.setOsVersion(deviceDetails.get("osVersion"));
                deviceDetailsModel.setPushSupport(deviceDetails.get("pushSupport"));


                biometricCredentialModel = BiometricCredentialModel.createFromCredentialModel(credentialModel,
                        deviceDetailsModel,
                        deviceDetailsModel.generateV2DeviceHash(context)
                );
            } catch (NoSuchAlgorithmException e) {
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "Invalid algorithm : " + e.getMessage(), deviceDetails);
                context.failure();
                return;
            } catch (InvalidKeyException e) {
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "Invalid secret key : " + e.getMessage(), deviceDetails);
                context.failure();
                return;
            } catch (NoSuchElementException e) {
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "No Secret Key found ", deviceDetails);
                context.failure();
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            biometricCredentialModel.setTemporary(false);
            context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), biometricCredentialModel);
            GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC, "", deviceDetails);
            String requestId = context.getRequestId();
            context.getRealm().addAttribute(requestId, BIOMETRIC_VERIFIED, "true");
            context.getRealm().addAttribute(requestId, BIOMETRIC_VERIFIED_AT, String.valueOf(Time.currentTime()));
            context.getRealm().addAttribute(requestId, BIOMETRIC_DEVICE, device);
            incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), BIOMETRIC_REGISTER);
            context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
            context.success();
        } else {
            auditDeviceDetails(formData, context);
        }
    }

    private void auditDeviceDetails(MultivaluedMap<String, String> formData, RequiredActionContext context) {
        String device = formData.getFirst(DEVICE);
        String message = formData.getFirst("message");

        HashMap<String, String> deviceDetails = null;
        try {
            deviceDetails = JsonSerialization.readValue(device, new TypeReference<HashMap<String, String>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
            GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "No device details received : " + e.getMessage());
            context.failure();
            return;
        }
        GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, message != null ? message : "Biometric Failed.", deviceDetails);
        context.failure();
    }

    private void biometricVerify(RequiredActionContext context, MultivaluedMap<String, String> formData) {

        if (formData.getFirst("success") != null && Boolean.parseBoolean(formData.getFirst("success"))) {

            String encryptedChallenge = formData.getFirst("encrypted_challenge");
            if (encryptedChallenge == null || encryptedChallenge.isEmpty()) {
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "No Encrypted challenge received");
                context.failure();
                return;
            }

            String credentialId = formData.getFirst(CRED_ID);

            if (credentialId == null || credentialId.isEmpty()) {
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "No Credential Id received");
                context.failure();
                return;
            }

            String device = formData.getFirst(DEVICE);
            if (device == null) {
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, NO_DEVICE_RECEIVED);
                context.failure();
                return;
            }


            DeviceDetails deviceDetails = null;
            String deviceId = "";
            List<HashMap<String, String>> credentials = new ArrayList<>();
            try {
                deviceDetails = JsonSerialization.readValue(device, DeviceDetails.class);
                // For forward compatibility
                deviceDetails.setOsVersion(null);
                deviceDetails.setPushSupport(null);

                deviceId = deviceDetails.getId();

                context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), BiometricCredentialModel.TYPE).forEach(x -> {
                    try {
                        if (!x.isEnabled()) {
                            credentials.add(JsonSerialization.readValue(x.getSecretData(), new TypeReference<HashMap<String, String>>() {
                            }));
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        e.printStackTrace();
                        return;
                    }

                });
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }

            String finalDeviceId = deviceId;
            if (!credentials.isEmpty() && (credentials.stream().anyMatch(x -> x.get("deviceId").equals(finalDeviceId)))) {
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "The requested device is disabled.");
                context.getRealm().addAttribute(context.getRequestId(), BIOMETRIC_FAILED_REASON, "The requested device is disabled.");
                context.failure();
                return;
            }


            BiometricCredentialInput biometricCredentialInput = null;
            biometricCredentialInput = getBiometricCredentialInput(context, encryptedChallenge, credentialId, deviceDetails, "v2");
            if (biometricCredentialInput == null) return;

            boolean isValid = getCredentialProvider(context).isValid(context.getRealm(), context.getUser(), biometricCredentialInput);
            if (!isValid) {
                if (context.getAuthenticationSession() != null) {
                    ClientModel client = context.getAuthenticationSession().getClient();
                    String cibaAuthorizerId = client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
                    deviceDetails.setOsVersion(getCredentailOsValue(context, credentialId, deviceDetails));

                    if (cibaAuthorizerId.equals("ciba-mobile-sdk")) {
                        deviceDetails.setPushSupport("firebase");
                    }
                    if (cibaAuthorizerId.equals("ciba-desktop-sdk")) {
                        deviceDetails.setPushSupport("socket");
                    }


                    biometricCredentialInput = getBiometricCredentialInput(context, encryptedChallenge, credentialId, deviceDetails, "v1");
                    if (biometricCredentialInput != null) {
                        isValid = getCredentialProvider(context).isValid(context.getRealm(), context.getUser(), biometricCredentialInput);
                        if (isValid) {
                            context.getSession().userCredentialManager().updateCredential(context.getRealm(), context.getUser(), biometricCredentialInput);
                            biometricSuccess(context, deviceDetails, device);
                            return;
                        } else {
                            // Remove invalid biometric credential
                            getCredentialProvider(context).deleteCredential(context.getRealm(), context.getUser(), biometricCredentialInput.getCredentialId());
                        }
                    }

                }
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "Challenge validation failed", deviceDetails.toMap());
                context.failure();
                return;
            }

            biometricSuccess(context, deviceDetails, device);
        } else {
            auditDeviceDetails(formData, context);
        }
    }

    private static String getCredentailOsValue(RequiredActionContext context, String credentialId, DeviceDetails deviceDetails) {
        CredentialModel storedCredentialById = context
                .getSession()
                .userCredentialManager()
                .getStoredCredentialById(context.getRealm(), context.getUser(), credentialId);
        if (storedCredentialById != null) {
            try {
                Map<String, String> credentialSecretData = JsonSerialization.readValue(storedCredentialById.getSecretData(), Map.class);
                return credentialSecretData.getOrDefault("deviceOSVersion", null);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    private void biometricSuccess(RequiredActionContext context, DeviceDetails deviceDetails, String device) {
        GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC, "", deviceDetails.toMap());
        String requestId = context.getRequestId();
        context.getRealm().addAttribute(requestId, BIOMETRIC_VERIFIED, "true");
        context.getRealm().addAttribute(requestId, BIOMETRIC_VERIFIED_AT, String.valueOf(Time.currentTime()));
        context.getRealm().addAttribute(requestId, BIOMETRIC_DEVICE, device);
        incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), BIOMETRIC_VERIFY);
        context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
        context.success();
    }

    private static BiometricCredentialInput getBiometricCredentialInput(RequiredActionContext context, String encryptedChallenge, String credentialId, DeviceDetails deviceDetails, String hashVersion) {
        BiometricCredentialInput biometricCredentialInput;
        try {
            try {
                biometricCredentialInput = new BiometricCredentialInput(context.getAuthenticationSession().getAuthNote(CHALLENGE),
                        encryptedChallenge,
                        credentialId,
                        deviceDetails.getId(),
                        hashVersion.equalsIgnoreCase("v1") ?
                                deviceDetails.generateV1DeviceHash(context) :
                                deviceDetails.generateV2DeviceHash(context)
                );
            } catch (IOException e) {
                GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "Failed to create biometric credential : " + e.getMessage(), deviceDetails.toMap());
                context.failure();
                return null;
            }
        } catch (NoSuchAlgorithmException e) {
            GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "Invalid algorithm : " + e.getMessage(), deviceDetails.toMap());
            context.failure();
            return null;
        } catch (InvalidKeyException e) {
            GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "Invalid secret key : " + e.getMessage(), deviceDetails.toMap());
            context.failure();
            return null;
        }
        return biometricCredentialInput;
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
        return "bo-biometric";
    }

    public BiometricCredentialProvider getCredentialProvider(RequiredActionContext context) {
        EventBuilder eventBuilder = GeneralHelper.getEvent(context, EventType.EZ_BIOMETRIC);
        return ((BiometricCredentialProvider) context.getSession().getProvider(CredentialProvider.class, BiometricCredentialProviderFactory.PROVIDER_ID)).setEvent(eventBuilder);
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> claims = new LinkedHashMap<>();

        claims.put(BIOMETRIC_VERIFIED, ValidationTypes.BOOLEAN.getValue());
        claims.put(BIOMETRIC_VERIFIED_AT, ValidationTypes.DATE_AND_TIME.getValue());
        claims.put(BIOMETRIC_DEVICE, ValidationTypes.OBJECT.getValue());

        return claims;
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

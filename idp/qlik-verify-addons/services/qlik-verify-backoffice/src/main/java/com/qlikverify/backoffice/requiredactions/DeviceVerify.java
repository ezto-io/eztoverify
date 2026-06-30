package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.backoffice.creds.device.TokenCredentialModel;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;

public class DeviceVerify implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final Logger logger = Logger.getLogger(DeviceVerify.class);

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "bo-device-verify";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
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
        String requestId = context.getRequestId();
        boolean silentDeviceVerified = context.getRealm().hasTrxValueAttribute(requestId, DEVICE_VERIFIED);
        boolean silentDeviceVerifiedAt = context.getRealm().hasTrxValueAttribute(requestId, DEVICE_VERIFIED_AT);
        if (result && (!silentDeviceVerified || !silentDeviceVerifiedAt)) {
            addAction(context);
        } else {
            context.getUser().removeRequiredAction(context.getModel().getId());
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

    public void showVerificationView(RequiredActionContext context) {
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);

        byte[] randomBytes = generateRandomBytes(32);
        String challenge = encodeToBase64(randomBytes);

        context.getAuthenticationSession().setAuthNote(CHALLENGE, challenge);
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute(CHALLENGE, challenge);
        loginFormsProvider.setAttribute(IS_MOBILE, cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop());
        context.challenge(loginFormsProvider.createForm("device-verify.ftl"));
    }

    private static byte[] generateRandomBytes(int length) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }

    private static String encodeToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId = context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        Map<String, String> deviceVerificationFailed = new HashMap<>();
        deviceVerificationFailed.put(ERROR_MESSAGE, "Device Verify Failed");

        incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), DEVICE_VERIFICATION);

        String from = formData.getFirst("from");

        if (from.equalsIgnoreCase("device-verify")) {

            String device = formData.getFirst("device");
            HashMap<String, String> deviceDetails = null;
            try {
                deviceDetails = JsonSerialization.readValue(device, HashMap.class);
            } catch (IOException e) {
                logger.warn(e.getMessage());
            }

            String deviceId = deviceDetails.get("id");

            List<HashMap<String, String>> credentials = new ArrayList<>();
            context.getSession().userCredentialManager().getStoredCredentialsByTypeStream(context.getRealm(), context.getUser(), TokenCredentialModel.TYPE).forEach(x -> {
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

            if (!credentials.isEmpty()) {
                if (credentials.stream().filter(x -> x.get("deviceId").equals(deviceId)).count() > 0) {
                    GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_VERIFY_ERROR, "The requested device is disabled.");
                    context.getRealm().addAttribute(context.getRequestId(), DEVICE_VERIFIED, "false");
                    context.getRealm().addAttribute(context.getRequestId(), DEVICE_VERIFIED_AT, String.valueOf(Time.currentTime()));
                    context.failure();
                    return;
                }
            }

            silentDeviceVerify(context, formData);
        }
    }

    public void silentDeviceVerify(RequiredActionContext context, MultivaluedMap<String, String> formData) {
        if (formData.getFirst("success") != null && Boolean.parseBoolean(formData.getFirst("success"))) {
            String challengeHash = formData.getFirst("challengeHash");
            String keyHashFirst6 = formData.getFirst("keyHashFirst6");
            Map<String, String> deviceVerificationFailed = new HashMap<>();
            deviceVerificationFailed.put(ERROR_MESSAGE, "Device Verify Failed");
            if (challengeHash == null || challengeHash.isEmpty()) {
                GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_VERIFY_ERROR, "No encrypted device Id received");
                GeneralHelper.showView(context, null, BACKOFFICE_ERROR_FTL, deviceVerificationFailed);
                return;
            }
            if (keyHashFirst6 == null || keyHashFirst6.isEmpty()) {
                GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_VERIFY_ERROR, "No sufficient datas received.");
                GeneralHelper.showView(context, null, BACKOFFICE_ERROR_FTL, deviceVerificationFailed);
                return;
            }

            List<HashMap<String, String>> credentials = new ArrayList<>();
            context.getSession().userCredentialManager().getStoredCredentialsStream(context.getRealm(), context.getUser()).filter(cred -> cred.getType().equals("DEVICE_TOKEN")).forEach(x -> {
                try {
                    credentials.add(JsonSerialization.readValue(x.getSecretData(), new TypeReference<HashMap<String, String>>() {
                    }));
                } catch (Exception e) {
                    GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_VERIFY_ERROR, "Error while adding device credentials");
                    GeneralHelper.showView(context, null, BACKOFFICE_ERROR_FTL, deviceVerificationFailed);
                    return;
                }

            });
            String deviceId = "";
            List<HashMap<String, String>> credentialWithKeyHash = credentials.stream().filter(x ->
                    keyHashFirst6.contains(x.get("keyHashFirst6"))
            ).collect(Collectors.toList());

            if (!credentialWithKeyHash.isEmpty()) {
                deviceId = credentialWithKeyHash.get(0).get("encryptedDeviceId");
            } else {
                GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_VERIFY_ERROR, "Device does not registered");
                context.getRealm().addAttribute(context.getRequestId(), DEVICE_VERIFIED, "false");
                context.getRealm().addAttribute(context.getRequestId(), DEVICE_VERIFIED_AT, String.valueOf(Time.currentTime()));
                context.failure();
                return;
            }
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                logger.error("Error while encypting device id");
                throw new RuntimeException(e);
            }
            byte[] decodedChallenge = Base64.getDecoder().decode(context.getAuthenticationSession().getAuthNote(CHALLENGE));
            byte[] decodedHashedDeviceId = Base64.getDecoder().decode(deviceId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                outputStream.write(decodedChallenge);
                outputStream.write(decodedHashedDeviceId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[] combinedArray = outputStream.toByteArray();

            byte[] check = md.digest(combinedArray);

            String hash = Base64.getEncoder().encodeToString(check);
            if (challengeHash.startsWith(hash)) {
                context.getRealm().addAttribute(context.getRequestId(), DEVICE_VERIFIED, "true");
                context.getRealm().addAttribute(context.getRequestId(), DEVICE_VERIFIED_AT, String.valueOf(Time.currentTime()));
                GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_VERIFY, null);
                context.success();
            } else {
                GeneralHelper.auditEvent(context, EventType.EZ_DEVICE_VERIFY_ERROR, "Device does not match");
                context.getRealm().addAttribute(context.getRequestId(), DEVICE_VERIFIED, "false");
                context.getRealm().addAttribute(context.getRequestId(), DEVICE_VERIFIED_AT, String.valueOf(Time.currentTime()));
                context.failure();
            }
        }
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
        return "bo-device-verify";
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{DEVICE_VERIFICATION};
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put(DEVICE_VERIFIED, ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put(DEVICE_VERIFIED_AT, ValidationTypes.DATE_AND_TIME.getValue());

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

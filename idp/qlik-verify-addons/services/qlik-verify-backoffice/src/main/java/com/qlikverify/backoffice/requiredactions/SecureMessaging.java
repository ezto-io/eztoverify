package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import org.jetbrains.annotations.NotNull;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.ciba.models.BindingMessage;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.*;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;

public class SecureMessaging implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        return this;
    }

    @Override
    public String getDisplayText() {
        return "bo-secure-messaging";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName("require_device");
        property.setLabel("sm-device-details");
        property.setHelpText("sm-device-details-hlptxt");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("require_location");
        property.setLabel("sm-location");
        property.setHelpText("sm-location-hlptxt");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("require_ip");
        property.setLabel("sm-ip-address");
        property.setHelpText("sm-ip-address-hlptxt");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
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
    public void evaluateTriggers(RequiredActionContext context) {
        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
        }
        String requestId = context.getRequestId();
        /*sm_read_time : //time with timezone
        sm_read_location: //loc (lat/long)
        sm_read_ip: //ipaddress
        sm_read_device: //device details
        sm_read_confirmation: yes/no*/
//        boolean smReadAt = context.getRealm().hasTrxValueAttribute(requestId, "sp@sm_read_at");
//        boolean smReadLoc = context.getRealm().hasTrxValueAttribute(requestId, "sp@sm_read_location");
//        boolean smReadIP = context.getRealm().hasTrxValueAttribute(requestId, "sp@sm_read_ip");
//        boolean smReadDevice = context.getRealm().hasTrxValueAttribute(requestId, "sp@sm_read_device");
        boolean smReadConf = context.getRealm().hasTrxValueAttribute(requestId, "sm_read_confirmation");
        if (result && !smReadConf) {
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
        Map<String, String> config = context.getConfig();
        //Read config
        //Check the config values set to trigger secure messaging check
        boolean isDeviceRequired = Boolean.parseBoolean(config.get("require_device"));
        boolean isLocationRequired = Boolean.parseBoolean(config.get("require_location"));
        boolean isIPRequired = Boolean.parseBoolean(config.get("require_ip"));

        if (isDeviceRequired || isLocationRequired || isIPRequired) {
            //Show secure-messaging check view
            showSMCheckView(context);
        } else {
            //Show direct message view
            showSMView(context);
        }

    }

    private void showSMCheckView(RequiredActionContext context) {
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute(IS_MOBILE, cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop());
        context.challenge(loginFormsProvider.createForm("secure-messaging-check.ftl"));
    }

    private void showSMView(RequiredActionContext context) {
        String value = context.getRealm().getTrxValue(context.getRequestId(), "hidden_ua_" + BINDING_MESSAGE);
        BindingMessage bindingMessage = null;
        if (value != null) {
            try {
                bindingMessage = JsonSerialization.readValue(value, BindingMessage.class);
            } catch (IOException e) {
                e.printStackTrace();
                GeneralHelper.auditEvent(context, EventType.EZ_SECURE_MESSAGING_ERROR, "Cannot read the binding message");
                context.failure();
                return;
            }
        }

        if (bindingMessage == null) {
            GeneralHelper.auditEvent(context, EventType.EZ_SECURE_MESSAGING_ERROR, "Cannot read the binding message");
            context.failure();
            return;
        }

        final String smMessage = bindingMessage.getMetadata().get("sm_message");
        final String smType = bindingMessage.getMetadata().get("sm_type");
        final String smOtp = bindingMessage.getMetadata().get("sm_otp");
        if (smMessage == null || smMessage.isEmpty()) {
            GeneralHelper.auditEvent(context, EventType.EZ_SECURE_MESSAGING_ERROR, "Param sm_message is required to " +
                    "send a secure message");
            context.failure();
            return;
        }
        if (smType != null && smType.equalsIgnoreCase("otp") && (smOtp == null || smOtp.isEmpty())) {
            GeneralHelper.auditEvent(context, EventType.EZ_SECURE_MESSAGING_ERROR, "Param sm_otp is required to " +
                    "send a secure otp message for message type 'otp'");
            context.failure();
            return;
        }

        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute(IS_MOBILE, cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop());
        loginFormsProvider.setAttribute("sm", smMessage);
        loginFormsProvider.setAttribute("sm_type", smType);
        loginFormsProvider.setAttribute("sm_otp", smOtp);
        final TrxModel trx = context.getRealm().getTrx(context.getRequestId());
        loginFormsProvider.setAttribute("time",
                String.valueOf(trx.getCreatedAt()));
        context.challenge(loginFormsProvider.createForm("secure-messaging.ftl"));
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId = context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        Map<String, String> secureMessagingFailed = getErrorMessage();

        String from = formData.getFirst("from");
        if (from == null) {
            GeneralHelper.auditEvent(context, EventType.EZ_SECURE_MESSAGING_ERROR, "Invalid form");
            context.failure();
            return;
        }

        if (from.equalsIgnoreCase("secure-messaging-check")) {
            boolean isSuccess = Boolean.parseBoolean(formData.getFirst("success"));
            if (!isSuccess) {
                String error = formData.getFirst("error");
                if (error == null || error.isEmpty()) {
                    error = "Something went wrong";
                }
                GeneralHelper.auditEvent(context, EventType.EZ_SECURE_MESSAGING_ERROR, error);
                context.failure();
                return;
            }
            incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), SECURE_MESSAGING);
            Map<String, String> config = context.getConfig();
            //Read config
            //Check the config values set to trigger secure messaging check
            boolean isDeviceRequired = Boolean.parseBoolean(config.get("require_device"));
            boolean isLocationRequired = Boolean.parseBoolean(config.get("require_location"));
            boolean isIPRequired = Boolean.parseBoolean(config.get("require_ip"));

            String requestId = context.getRequestId();
            if (isDeviceRequired) {
                String deviceDetails = formData.getFirst("device");
                if (deviceDetails == null || deviceDetails.isEmpty()) {
                    GeneralHelper.auditEvent(context, EventType.EZ_SECURE_MESSAGING_ERROR, "Since its a mandatory. Device details are missing");
                    context.failure();
                    return;
                }
                context.getRealm().addAttribute(requestId, "sm_read_device", deviceDetails);
            }

            if (isLocationRequired) {
                String locationDetails = formData.getFirst("location");
                if (locationDetails == null || locationDetails.isEmpty()) {
                    GeneralHelper.auditEvent(context, EventType.EZ_SECURE_MESSAGING_ERROR, "Since its a mandatory. Device Location details are missing");
                    context.failure();
                    return;
                }
                context.getRealm().addAttribute(requestId, "sm_read_location", locationDetails);
            }

            if (isIPRequired) {
                String locationDetails = context.getConnection().getRemoteAddr();
                if (locationDetails == null || locationDetails.isEmpty()) {
                    GeneralHelper.auditEvent(context, EventType.EZ_SECURE_MESSAGING_ERROR, "Since its a mandatory. Device Location details are missing");
                    context.failure();
                    return;
                }
                context.getRealm().addAttribute(requestId, "sm_read_ip", locationDetails);
            }

            String readTimeDetails = formData.getFirst("time");
            if (readTimeDetails != null) {
                HashMap<String, String> timeDetails = null;
                try {
                    timeDetails = JsonSerialization.readValue(readTimeDetails, new TypeReference<HashMap<String, String>>() {
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    GeneralHelper.auditEvent(context, EventType.EZ_BIOMETRIC_ERROR, "No time details received : " + e.getMessage());
                    context.failure();
                    return;
                }
                context.getRealm().addAttribute(requestId, "sm_read_at", timeDetails.get("epoch"));
            }
            showSMView(context);
            return;
        }

        if (from.equalsIgnoreCase("secure-messaging")) {

            context.getRealm().addAttribute(context.getRequestId(), "sm_read_confirmation", "yes");
            context.success();
            return;
        }

        context.failure();
    }

    @NotNull
    private Map<String, String> getErrorMessage() {
        Map<String, String> secureMessagingFailed = new HashMap<>();
        secureMessagingFailed.put(ERROR_MESSAGE, "Secure Messaging Failed");
        return secureMessagingFailed;
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
        return "bo-secure-messaging";
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("update_in_user_metadata", false);
        metadata.put("sm_message", "Test message");
        metadata.put("sm_type", "otp");
        metadata.put("sm_otp", "123456");
        return metadata;
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{SECURE_MESSAGING};
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put("sm_read_confirmation", ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put("sm_read_at", ValidationTypes.DATE_AND_TIME.getValue());
        defaultClaims.put("sm_read_ip", ValidationTypes.STRING.getValue());
        defaultClaims.put("sm_read_location", ValidationTypes.OBJECT.getValue());
        defaultClaims.put("sm_read_device", ValidationTypes.OBJECT.getValue());

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

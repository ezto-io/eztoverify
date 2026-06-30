package com.qlikverify.backoffice.requiredactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import org.keycloak.Config;
import org.keycloak.authentication.*;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.qlikverify.backoffice.Constants.IS_MOBILE;
import static com.qlikverify.backoffice.Constants.PROXIMITY_VERIFICATION;
import static com.qlikverify.backoffice.helpers.GeneralHelper.handleCancel;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;

public class ProximityVerify implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        return this;
    }

    @Override
    public String getDisplayText() {
        return "bo-proximity-verify";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    private static List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String PROXIMITY_VERIFY = "proximity-verify";
    public static final String PROXIMITY_CONFIG_WARN = "proximity-config-warn";

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(PROXIMITY_VERIFY);
        property.setLabel(PROXIMITY_VERIFY);
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("proximity-verify-helptext");
        property.setMinVal(1);
        property.setMaxVal(500);
        property.setRequired(true);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(PROXIMITY_CONFIG_WARN);
        property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        property.setHelpText("proximity-config-warn-helptext");
        property.setDefaultValue(30);
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

        boolean webLat = context.getRealm().hasTrxValueAttribute(requestId, "web_lat");
        boolean webLng = context.getRealm().hasTrxValueAttribute(requestId, "web_long");

        boolean mobLat = context.getRealm().hasTrxValueAttribute(requestId, "mob_lat");
        boolean mobLng = context.getRealm().hasTrxValueAttribute(requestId, "mob_long");
        if (result && (!mobLat || !mobLng) && (webLat && webLng)) {
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
        showView(context, null);
    }

    private void showView(RequiredActionContext context, String error) {
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute(IS_MOBILE, cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop());
        if (error != null) {
            loginFormsProvider.setAttribute("isRetry", true);
            loginFormsProvider.setAttribute("error", error);
        }
        context.challenge(loginFormsProvider.createForm("proximity.ftl"));
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId = context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        String from = formData.getFirst("from");
        String retry = formData.getFirst("retry");
        String cancel = formData.getFirst("cancel");
        if (from == null) {
            GeneralHelper.auditEvent(context, EventType.EZ_PROXIMITY_ERROR, "Invalid form");
            context.failure();
            return;
        }

        if (Boolean.parseBoolean(cancel)) {
            handleCancel(context, EventType.EZ_PROXIMITY_ERROR);
            return;
        }

        if (Boolean.parseBoolean(retry)) {
            requiredActionChallenge(context);
            return;
        }

        if (from.equalsIgnoreCase("proximity-check")) {
            incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), PROXIMITY_VERIFICATION);
            boolean isSuccess = Boolean.parseBoolean(formData.getFirst("success"));
            if (!isSuccess) {
                String error = formData.getFirst("error");
                if (error == null || error.isEmpty()) {
                    error = "Something went wrong";
                }
                GeneralHelper.auditEvent(context, EventType.EZ_PROXIMITY_ERROR, error);
                showView(context, error);
                return;
            }

            String requestId = context.getRequestId();

            String readTimeDetails = formData.getFirst("time");
            if (readTimeDetails != null) {
                HashMap<String, String> timeDetails = null;
                try {
                    timeDetails = JsonSerialization.readValue(readTimeDetails, new TypeReference<HashMap<String, String>>() {
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    GeneralHelper.auditEvent(context, EventType.EZ_PROXIMITY_ERROR, "No time details received : " + e.getMessage());
                    context.failure();
                    return;
                }
                context.getRealm().addAttribute(requestId, "prx_read_at", timeDetails.get("epoch"));
            }

            String locationDetails = formData.getFirst("location");
            if (locationDetails == null || locationDetails.isEmpty()) {
                GeneralHelper.auditEvent(context, EventType.EZ_PROXIMITY_ERROR, "Since its a mandatory. Device Location details are missing");
                showView(context, "Location is mandatory to proceed further. Please enable to share location and click retry");
                return;
            }

            try {
                Map<String, String> locs = JsonSerialization.readValue(locationDetails, Map.class);
                String latString = String.valueOf(locs.get("lat"));
                BigDecimal latBigDecimal = new BigDecimal(Double.valueOf(latString));
                double lat = latBigDecimal.setScale(5, RoundingMode.HALF_UP).doubleValue();

                String longString = String.valueOf(locs.get("long"));
                BigDecimal longBigDecimal = new BigDecimal(Double.valueOf(longString));
                double lng = longBigDecimal.setScale(5, RoundingMode.HALF_UP).doubleValue();

                double webLat = Double.valueOf(context.getRealm().getTrxValue(requestId, "web_lat"));
                double webLng = Double.valueOf(context.getRealm().getTrxValue(requestId, "web_long"));
                double accuracy = Double.valueOf(context.getRealm().getTrxValue(requestId, "web_geo_acc"));
                int distance = calculateDistance(webLat, webLng, lat, lng, accuracy);

                int expectedDistance = Integer.parseInt(context.getConfig().get(PROXIMITY_VERIFY));

                if (distance > expectedDistance && distance <= 500) {
                    GeneralHelper.auditEvent(context, EventType.EZ_PROXIMITY_ERROR, "Web and Mobile Device are neither far away or near : " + String.valueOf(distance) + " in meteres");
                    showView(context, "Device is not detectable. Try to bring the device near by to web portal");
                    return;
                }

                if (distance > 500) {
                    GeneralHelper.auditEvent(context, EventType.EZ_PROXIMITY_ERROR, "Web and Mobile Device are far away : " + String.valueOf(distance) + " in meteres");
                    context.getRealm().addAttribute(context.getRequestId(), "proximity_failed_reason", "Web and Mobile Device are far away : " + String.valueOf(distance) + " in meteres");
                    context.failure();
                    return;
                }

                if (distance <= expectedDistance) {
                    context.getRealm().addAttribute(context.getRequestId(), "mob_lat", String.valueOf(lat));
                    context.getRealm().addAttribute(context.getRequestId(), "mob_long", String.valueOf(lng));
                    context.getRealm().addAttribute(context.getRequestId(), "web_mob_dist", String.valueOf(distance));
                    context.getRealm().addAttribute(context.getRequestId(), "proximity_verified", "true");
                    context.getRealm().addAttribute(context.getRequestId(), "proximity_verified_at", String.valueOf(Time.currentTime()));
                    GeneralHelper.auditEvent(context, EventType.EZ_PROXIMITY, "");
                    context.success();
                    return;
                }

            } catch (IOException e) {
                e.printStackTrace();
                GeneralHelper.auditEvent(context, EventType.EZ_PROXIMITY_ERROR, "Since its a mandatory. Device Location details are missing");
                showView(context, "Location is mandatory to proceed further. Please enable to share location and click retry");
                return;
            }
        }

        context.failure();
    }

    private static final double R = 6371e3; // Radius of the Earth in meters

    public static int calculateDistance(double lat1, double lon1, double lat2, double lon2, double accuracy) {

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // Convert to meters
        double adjustedDistance = distance * (accuracy / 100.0);
        return (int) adjustedDistance;
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "bo-proximity-verify";
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{PROXIMITY_VERIFICATION};
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put("mob_lat", ValidationTypes.NUMBER.getValue());
        defaultClaims.put("mob_long", ValidationTypes.NUMBER.getValue());
        defaultClaims.put("web_mob_dist", ValidationTypes.NUMBER.getValue());
        defaultClaims.put("proximity_verified", ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put("proximity_verified_at", ValidationTypes.DATE_AND_TIME.getValue());

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

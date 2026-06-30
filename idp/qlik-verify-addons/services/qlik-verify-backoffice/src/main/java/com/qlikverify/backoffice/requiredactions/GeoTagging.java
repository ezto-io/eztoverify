package com.qlikverify.backoffice.requiredactions;

import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.Errors;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.qlikverify.backoffice.validators.ValidatorPolicy;
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
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.qlikverify.backoffice.Constants.*;

public class GeoTagging implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final Logger logger = Logger.getLogger(GeoTagging.class);
    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "bo-geo-tagging";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    //
    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(GEO_TAGGING_RETRY);
        property.setLabel(GEO_TAGGING_RETRY);
        property.setHelpText("geo-tagging-retry-hlptxt");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(false);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(GEO_TAGGING_LATITUDE);
        property.setLabel(GEO_TAGGING_LATITUDE);
        property.setHelpText(GEO_TAGGING_LATITUDE_HLP_TXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(GEO_TAGGING_LONGITUDE);
        property.setLabel(GEO_TAGGING_LONGITUDE);
        property.setHelpText(GEO_TAGGING_LONGITUDE_HLP_TXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(GEO_TAGGING_ACCURACY);
        property.setLabel(GEO_TAGGING_ACCURACY);
        property.setHelpText(GEO_TAGGING_ACCURACY_HLP_TXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(GEO_TAGGING_DEVICE_TYPE);
        property.setLabel(GEO_TAGGING_DEVICE_TYPE);
        property.setHelpText(GEO_TAGGING_DEVICE_TYPE_HLP_TXT);
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
//        boolean geoLat = context.getRealm().hasTrxValueAttribute(requestId, SP_GEO_TAGGING_LATITUDE);
//        boolean geoLong = context.getRealm().hasTrxValueAttribute(requestId, SP_GEO_TAGGING_LONGITUDE);
        boolean geoVerified = context.getRealm().hasTrxValueAttribute(requestId, GEO_TAGGING_VERIFIED);

        boolean geoVerifiedAt = context.getRealm().hasTrxValueAttribute(requestId, GEO_TAGGING_VERIFIED_AT);
//        boolean geoAccuracy = context.getRealm().hasTrxValueAttribute(requestId, SP_GEO_TAGGING_ACCURACY);



        if (result && (!geoVerified || !geoVerifiedAt)) {
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
        showView(context, "", "false", "0");
    }

    @Override
    public void processAction(RequiredActionContext context) {
        if(context.getUser() == null){
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_GEO_TAGGING_ERROR, ErrorMappings.USER_NOT_ONBOARDED);
            com.qlikverify.backoffice.helpers.GeneralHelper.showView(context, "user_not_onboarded", BACKOFFICE_ERROR_FTL, null);
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String isRetry = formData.getFirst("isRetry");
        String retriedCount = formData.getFirst("retriedCount");
        String retryConfig = context.getConfig().getOrDefault(GEO_TAGGING_RETRY, "1");
        String geoTaggingVerified = formData.getFirst("geoTaggingVerified");

        if(formData.containsKey("cancel") && Boolean.parseBoolean(formData.getFirst("cancel"))) {
            com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EV0008, getId());
            com.qlikverify.backoffice.helpers.GeneralHelper.handleCancel(context, EventType.EZ_GEO_TAGGING_ERROR);
            return;
        }

        if (Boolean.parseBoolean(isRetry) && !Boolean.parseBoolean (geoTaggingVerified) && (Integer.valueOf(retryConfig.isEmpty() ? "1" : retryConfig) != Integer.valueOf(retriedCount))) {
            showView(context, "", isRetry, retriedCount);
            return;
        }

        if (formData.getFirst("from")!= null && formData.getFirst("from").equals("geo_tagging") && formData.getFirst("ismobile") != null && !Boolean.parseBoolean(formData.getFirst("ismobile"))) {
            String errorMsg = formData.getFirst("errorMsg");

            if(Boolean.parseBoolean(isRetry) && !Boolean.parseBoolean(geoTaggingVerified)) {
                context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_VERIFIED, "false");
                context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_VERIFIED_AT, String.valueOf(Time.currentTime()));
                com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EVGEO0001, getId());
                context.success();
                return;
            }

            if ((isRetry != null && Boolean.parseBoolean(isRetry)) && (errorMsg != null && !errorMsg.isEmpty())) {
                LoginFormsProvider loginFormsProvider = context.form();
                loginFormsProvider.setAttribute("errorMsg", errorMsg);
                loginFormsProvider.setAttribute("isRetry", isRetry);
                loginFormsProvider.setAttribute("retryCount", !context.getConfig().getOrDefault(GEO_TAGGING_RETRY, "1").isEmpty() ? context.getConfig().getOrDefault(GEO_TAGGING_RETRY, "1") : "1");
                loginFormsProvider.setAttribute("retriedCount", retriedCount);
                context.challenge(loginFormsProvider.createForm(GEO_TAGGING_FTL));
                return;
            }



            String latitude = formData.getFirst("latitude");
            String longitude = formData.getFirst("longitude");
            String accuracy = formData.getFirst("accuracy");

            if (latitude != null && !latitude.isEmpty() && longitude != null && !longitude.isEmpty() && accuracy != null && !accuracy.isEmpty()) {
                double lat = (new BigDecimal(Double.valueOf(latitude)).setScale(5, RoundingMode.HALF_UP)).doubleValue();
                double lng = (new BigDecimal(Double.valueOf(longitude)).setScale(5, RoundingMode.HALF_UP)).doubleValue();
                double acc = (new BigDecimal(Double.valueOf(accuracy)).setScale(5, RoundingMode.HALF_UP)).doubleValue();

                if (context.getConfig().get(GEO_TAGGING_LATITUDE) != null && Boolean.parseBoolean(context.getConfig().get(GEO_TAGGING_LATITUDE))) {
                    context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_LATITUDE, String.valueOf(lat));
                }
                if (context.getConfig().get(GEO_TAGGING_LONGITUDE) != null && Boolean.parseBoolean(context.getConfig().get(GEO_TAGGING_LONGITUDE))) {
                    context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_LONGITUDE, String.valueOf(lng));
                }
                if (context.getConfig().get(GEO_TAGGING_ACCURACY) != null && Boolean.parseBoolean(context.getConfig().get(GEO_TAGGING_ACCURACY))) {
                    context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_ACCURACY, String.valueOf(acc));
                }
                if (context.getConfig().get(GEO_TAGGING_DEVICE_TYPE) != null && Boolean.parseBoolean(context.getConfig().get(GEO_TAGGING_DEVICE_TYPE))) {
                    context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_DEVICE_TYPE, Boolean.parseBoolean(formData.getFirst("ismobile")) ? "mobile" : "web");
                }

                context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_VERIFIED, "true");
                context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_VERIFIED_AT, String.valueOf(Time.currentTime()));
                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_GEO_TAGGING, "");
                incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), stripeMappingId()[0]);
                context.success();
                return;
            } else {
                LoginFormsProvider loginFormsProvider = context.form();
                loginFormsProvider.setAttribute("errorMsg", errorMsg);
                loginFormsProvider.setAttribute("isRetry", isRetry);
                loginFormsProvider.setAttribute("retryCount", !context.getConfig().getOrDefault(GEO_TAGGING_RETRY, "1").isEmpty() ? context.getConfig().getOrDefault(GEO_TAGGING_RETRY, "1") : "1");
                loginFormsProvider.setAttribute("retriedCount", retriedCount);
                context.challenge(loginFormsProvider.createForm(GEO_TAGGING_FTL));
                return;
            }

        }

        if (formData.getFirst("from")!= null && formData.getFirst("from").equals("geo_tagging") && formData.getFirst("ismobile") != null && Boolean.parseBoolean(formData.getFirst("ismobile"))) {
            boolean isSuccess = Boolean.parseBoolean(formData.getFirst("success"));

            if (!isSuccess) {
                String error = formData.getFirst("errorMsg");
                if (error == null || error.isEmpty()) {
                    error = "Something went wrong";
                }
                GeneralHelper.auditEvent(context, EventType.EZ_GEO_TAGGING_ERROR, error);
                context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_VERIFIED, "false");
                context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_VERIFIED_AT, String.valueOf(Time.currentTime()));
                com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EVGEO0001, getId());
                context.success();
//                showView(context, error, "false");
                return;
            }

            String locationDetails = formData.getFirst("location");
            if (locationDetails == null || locationDetails.isEmpty()) {
                GeneralHelper.auditEvent(context, EventType.EZ_GEO_TAGGING_ERROR, "Since its a mandatory. Device Location details are missing");
                showView(context, "Location is mandatory to proceed further. Please enable to share location and click retry", "true", retriedCount);
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

                String accuracy = String.valueOf(locs.get("accuracy"));
                BigDecimal AccBigDecimal = new BigDecimal(Double.valueOf(accuracy));
                double acc = AccBigDecimal.setScale(5, RoundingMode.HALF_UP).doubleValue();

                if (context.getConfig().get(GEO_TAGGING_LATITUDE) != null && Boolean.parseBoolean(context.getConfig().get(GEO_TAGGING_LATITUDE))) {
                    context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_LATITUDE, String.valueOf(lat));
                }
                if (context.getConfig().get(GEO_TAGGING_LONGITUDE) != null && Boolean.parseBoolean(context.getConfig().get(GEO_TAGGING_LONGITUDE))) {
                    context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_LONGITUDE, String.valueOf(lng));
                }
                if (context.getConfig().get(GEO_TAGGING_ACCURACY) != null && Boolean.parseBoolean(context.getConfig().get(GEO_TAGGING_ACCURACY))) {
                    context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_ACCURACY, String.valueOf(acc));
                }
                if (context.getConfig().get(GEO_TAGGING_DEVICE_TYPE) != null && Boolean.parseBoolean(context.getConfig().get(GEO_TAGGING_DEVICE_TYPE))) {
                    context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_DEVICE_TYPE, Boolean.parseBoolean(formData.getFirst("ismobile")) ? "mobile" : "web");
                }

                context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_VERIFIED, "true");
                context.getRealm().addAttribute(context.getRequestId(), GEO_TAGGING_VERIFIED_AT, String.valueOf(Time.currentTime()));
                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_GEO_TAGGING, "");
                incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), stripeMappingId()[0]);
                context.success();
            } catch (IOException e) {
                e.printStackTrace();
                GeneralHelper.auditEvent(context, EventType.EZ_GEO_TAGGING_ERROR, "Since its a mandatory. Device Location details are missing");
                showView(context, "Location is mandatory to proceed further. Please enable to share location and click retry", "true", retriedCount);
                return;
            }
        }
    }

    private void showView(RequiredActionContext context, String error, String retry, String retriedCount) {
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute(IS_MOBILE, cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop());
        loginFormsProvider.setAttribute("isRetry", retry);
        loginFormsProvider.setAttribute("retryCount", !context.getConfig().getOrDefault(GEO_TAGGING_RETRY, "1").isEmpty() ? context.getConfig().getOrDefault(GEO_TAGGING_RETRY, "1") : "1");
        loginFormsProvider.setAttribute("retriedCount", retriedCount);
        if (error != null) {
            loginFormsProvider.setAttribute("errorMsg", error);
        }
        context.challenge(loginFormsProvider.createForm(GEO_TAGGING_FTL));
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
        return "bo-geo-tagging";
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }

    @Override
    public String[] supportedRegions() {
        return new String[]{"in"};
    }

    @Override
    public String[] stripeMappingId() {
        return new String[] {GEO_TAGGING};
    }

    @Override
    public boolean isBeta() {
        return true;
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> claims = getDefaultClaims();

        Map<String, String> potentialClaims = getGeoTaggingPotentialClaims();

        if(model.getConfig().containsKey(GEO_TAGGING_LATITUDE) && Boolean.parseBoolean(model.getConfig().get(GEO_TAGGING_LATITUDE))) {
            claims.put(GEO_TAGGING_LATITUDE, potentialClaims.get(GEO_TAGGING_LATITUDE));
        }
        if(model.getConfig().containsKey(GEO_TAGGING_LONGITUDE) && Boolean.parseBoolean(model.getConfig().get(GEO_TAGGING_LONGITUDE))) {
            claims.put(GEO_TAGGING_LONGITUDE, potentialClaims.get(GEO_TAGGING_LONGITUDE));
        }
        if(model.getConfig().containsKey(GEO_TAGGING_ACCURACY) && Boolean.parseBoolean(model.getConfig().get(GEO_TAGGING_ACCURACY))) {
            claims.put(GEO_TAGGING_ACCURACY, potentialClaims.get(GEO_TAGGING_ACCURACY));
        }
        if(model.getConfig().containsKey(GEO_TAGGING_DEVICE_TYPE) && Boolean.parseBoolean(model.getConfig().get(GEO_TAGGING_DEVICE_TYPE))) {
            claims.put(GEO_TAGGING_DEVICE_TYPE, potentialClaims.get(GEO_TAGGING_DEVICE_TYPE));
        }

        return claims;
    }

    private static Map<String, String> getDefaultClaims() {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put(GEO_TAGGING_VERIFIED, ValidatorPolicy.ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put(GEO_TAGGING_VERIFIED_AT, ValidatorPolicy.ValidationTypes.DATE_AND_TIME.getValue());

        return defaultClaims;
    }

    private static Map<String, String> getGeoTaggingPotentialClaims() {
        Map<String, String> potentialClaims = new LinkedHashMap<>();

        potentialClaims.put(GEO_TAGGING_LATITUDE, ValidatorPolicy.ValidationTypes.NUMBER.getValue());
        potentialClaims.put(GEO_TAGGING_LONGITUDE, ValidatorPolicy.ValidationTypes.NUMBER.getValue());
        potentialClaims.put(GEO_TAGGING_ACCURACY, ValidatorPolicy.ValidationTypes.NUMBER.getValue());
        potentialClaims.put(GEO_TAGGING_DEVICE_TYPE, ValidatorPolicy.ValidationTypes.STRING.getValue());

        return potentialClaims;
    }

    @Override
    public boolean skippable() {
        return false;
    }

    @Override
    public void onUpdate(RequiredActionProviderRepresentation rep) {
        validateConfigForGeoTagging(rep);
    }

    private void validateConfigForGeoTagging(RequiredActionProviderRepresentation rep) {
        String retryCount = rep.getConfig().get(GEO_TAGGING_RETRY);

        if(!retryCount.matches("^[1-3]$") && !retryCount.isEmpty()) {
            throw new IllegalArgumentException("Enter a valid retry count between 1-3");
        }
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId = context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }
}

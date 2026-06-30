package com.qlikverify.backoffice.requiredactions;

import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.Errors;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.qlikverify.backoffice.helpers.selfieRegistrationHelper.SelfieRegistrationHelper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
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
import org.keycloak.utils.MediaType;

import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.ValidationTypes;

public class SelfieRegistration implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final Logger logger = Logger.getLogger(SelfieRegistration.class);
    public static final String VERIFY_LIVELINESS_IMAGE_FTL = "liveliness_image_verification.ftl";
    private static final SelfieRegistrationHelper selfieRegistrationHelper = new SelfieRegistrationHelper();
    private static final String SELFIE_REGISTERED = "selfie_registered";
    private static final String SELFIE_REGISTERED_AT = "selfie_registered_at";
    private static final String SELFIE_REGISTRATION_FAILURE_REASON = "selfie_registration_failure_reason";

    KeycloakSession session;

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "bo-selfie-registration";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    //
    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(ENABLE_SELFIE_REG_GEO_TAGGING);
        property.setLabel(ENABLE_SELFIE_REG_GEO_TAGGING);
        property.setHelpText(GEO_TAGGING_LATITUDE_HLP_TXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        configProperties.add(property);

        Map<String, Object> dependsOn = new HashMap<>();
        dependsOn.put(ENABLE_SELFIE_REG_GEO_TAGGING, true);

        property = new ProviderConfigProperty();
        property.setName(SELFIE_LATITUDE);
        property.setLabel(SELFIE_LATITUDE);
        property.setHelpText(GEO_TAGGING_LATITUDE_HLP_TXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDependsOn(dependsOn);
        property.setDefaultValue(false);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(SELFIE_LONGITUDE);
        property.setLabel(SELFIE_LONGITUDE);
        property.setHelpText(GEO_TAGGING_LONGITUDE_HLP_TXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDependsOn(dependsOn);
        property.setDefaultValue(false);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(SELFIE_ACCURACY);
        property.setLabel(SELFIE_ACCURACY);
        property.setHelpText(GEO_TAGGING_ACCURACY_HLP_TXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDependsOn(dependsOn);
        property.setDefaultValue(false);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(SELFIE_DEVICE_TYPE);
        property.setLabel(SELFIE_DEVICE_TYPE);
        property.setHelpText(SELFIE_DEVICE_TYPE_HLP_TXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDependsOn(dependsOn);
        property.setDefaultValue(false);
        configProperties.add(property);
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String[] supportedModes() {
        return new String[]{MODE};
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        logger.info("SelfieRegistration :: evaluateTriggers()");
        boolean result = true;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
        }
        String faceTemplate = context.getUser().getFirstAttribute("face_template");
        boolean hasSkipped = Boolean.parseBoolean(context.getRealm().getTrxValue(context.getRequestId(), SELFIE_REGISTRATION_SKIPPED));
        if (result && !hasSkipped && faceTemplate == null) {
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
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider
                .setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute("geo_mobile", cibaAuthProvider.isMobile() || cibaAuthProvider.isDesktop());
        loginFormsProvider.setAttribute("geo_tagging_enabled", context.getConfig().containsKey(ENABLE_SELFIE_REG_GEO_TAGGING) && Boolean.parseBoolean(context.getConfig().get(ENABLE_SELFIE_REG_GEO_TAGGING)));
        loginFormsProvider.setAttribute("totalSteps", Long.valueOf(1));
        loginFormsProvider.setAttribute("title", "selfie-register");
        loginFormsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());
        context.getAuthenticationSession().setAuthNote("stepCount", String.valueOf(1));
        loginFormsProvider.setAttribute("ismobile", cibaAuthProvider.isMobile());
        loginFormsProvider.setAttribute("isdesktop", cibaAuthProvider.isDesktop());
        loginFormsProvider.setAttribute(IS_LIVENESS, true);
        loginFormsProvider.setAttribute(REQUEST_ID, context.getRequestId());
        loginFormsProvider.setAttribute("userId", context.getUser().getUsername());
        loginFormsProvider.setAttribute("jsWriterKey", EnvironmentReader.getValue("EZTO_JS_SEGMENT_WRITE_KEY", ""));
        context.challenge(loginFormsProvider.createForm(SELFIE_FTL));
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId = context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public void processAction(RequiredActionContext context) {

        if(context.getHttpRequest().getHttpHeaders().getMediaType().equals(MediaType.APPLICATION_FORM_URLENCODED_TYPE) && context.getHttpRequest().getDecodedFormParameters().get("liveliness_image") != null){
            if(context.getHttpRequest().getFormParameters().containsKey("cancel") && context.getAuthenticationSession().getAuthNote("liveliness_config") == null) {
                com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EV0008,getId());
                GeneralHelper.handleCancel(context, EventType.EZ_SELFIE_REGISTRATION_ERROR);
                return;
            }

            if(context.getHttpRequest().getFormParameters().containsKey("skipped") && Boolean.parseBoolean(context.getHttpRequest().getFormParameters().getFirst("skipped"))) {
                context.getRealm().addAttribute(context.getRequestId(), SELFIE_REGISTRATION_SKIPPED, "true");
                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_SELFIE_REGISTRATION, "", skipDetails);
                context.success();
                return;
            }
            MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
            if ((context.getConfig().containsKey(ENABLE_SELFIE_REG_GEO_TAGGING) && Boolean.parseBoolean(context.getConfig().get(ENABLE_SELFIE_REG_GEO_TAGGING)))) {
                if (!Boolean.parseBoolean(formData.getFirst("geo_mobile"))) {
                    String errorMsg = formData.getFirst("errorMsg");

                    String latitude = formData.getFirst("latitude");
                    String longitude = formData.getFirst("longitude");
                    String accuracy = formData.getFirst("accuracy");

                    if(!errorMsg.isEmpty()) {
                        com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EVSR0003, getId());
                        GeneralHelper.auditEvent(context, EventType.EZ_SELFIE_REGISTRATION_ERROR, errorMsg);
                    }

                    if (latitude != null && !latitude.isEmpty() && longitude != null && !longitude.isEmpty() && accuracy != null && !accuracy.isEmpty()) {
                        double lat = (new BigDecimal(Double.valueOf(latitude)).setScale(5, RoundingMode.HALF_UP)).doubleValue();
                        double lng = (new BigDecimal(Double.valueOf(longitude)).setScale(5, RoundingMode.HALF_UP)).doubleValue();
                        double acc = (new BigDecimal(Double.valueOf(accuracy)).setScale(5, RoundingMode.HALF_UP)).doubleValue();

                        if (context.getConfig().get(SELFIE_LATITUDE) != null && Boolean.parseBoolean(context.getConfig().get(SELFIE_LATITUDE))) {
                            context.getRealm().addAttribute(context.getRequestId(), SELFIE_REG_LATITUDE, String.valueOf(lat));
                        }
                        if (context.getConfig().get(SELFIE_LONGITUDE) != null && Boolean.parseBoolean(context.getConfig().get(SELFIE_LONGITUDE))) {
                            context.getRealm().addAttribute(context.getRequestId(), SELFIE_REG_LONGITUDE, String.valueOf(lng));
                        }
                        if (context.getConfig().get(SELFIE_ACCURACY) != null && Boolean.parseBoolean(context.getConfig().get(SELFIE_ACCURACY))) {
                            context.getRealm().addAttribute(context.getRequestId(), SELFIE_REG_ACCURACY, String.valueOf(acc));
                        }
                        if (context.getConfig().get(SELFIE_DEVICE_TYPE) != null && Boolean.parseBoolean(context.getConfig().get(SELFIE_DEVICE_TYPE))) {
                            context.getRealm().addAttribute(context.getRequestId(), SELFIE_REG_DEVICE_TYPE, Boolean.parseBoolean(formData.getFirst("geo_mobile")) ? "mobile" : "web");
                        }
                    }
                }
                if (Boolean.parseBoolean(formData.getFirst("geo_mobile"))) {
                    boolean isSuccess = Boolean.parseBoolean(formData.getFirst("success"));

                    if (!isSuccess) {
                        String error = formData.getFirst("errorMsg");
                        if (error == null || error.isEmpty()) {
                            error = "Something went wrong";
                        }
                        GeneralHelper.auditEvent(context, EventType.EZ_SELFIE_REGISTRATION_ERROR, error);
                        com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EVSR0003, getId());
                    } else {
                        String locationDetails = formData.getFirst("location");

                        if(!locationDetails.isEmpty()) {
                            try {
                                Map<String, String> locs = JsonSerialization.readValue(locationDetails, Map.class);
                                String latString = String.valueOf(locs.get("lat"));
                                String longString = String.valueOf(locs.get("long"));
                                String accuracy = String.valueOf(locs.get("accuracy"));

                                if(latString != null && !latString.isEmpty() && longString != null && !longString.isEmpty() && accuracy != null && !accuracy.isEmpty()) {
                                    BigDecimal latBigDecimal = new BigDecimal(Double.valueOf(latString));
                                    double lat = latBigDecimal.setScale(5, RoundingMode.HALF_UP).doubleValue();

                                    BigDecimal longBigDecimal = new BigDecimal(Double.valueOf(longString));
                                    double lng = longBigDecimal.setScale(5, RoundingMode.HALF_UP).doubleValue();

                                    BigDecimal AccBigDecimal = new BigDecimal(Double.valueOf(accuracy));
                                    double acc = AccBigDecimal.setScale(5, RoundingMode.HALF_UP).doubleValue();

                                    if (context.getConfig().get(SELFIE_LATITUDE) != null && Boolean.parseBoolean(context.getConfig().get(SELFIE_LATITUDE))) {
                                        context.getRealm().addAttribute(context.getRequestId(), SELFIE_REG_LATITUDE, String.valueOf(lat));
                                    }
                                    if (context.getConfig().get(SELFIE_LONGITUDE) != null && Boolean.parseBoolean(context.getConfig().get(SELFIE_LONGITUDE))) {
                                        context.getRealm().addAttribute(context.getRequestId(), SELFIE_REG_LONGITUDE, String.valueOf(lng));
                                    }
                                    if (context.getConfig().get(SELFIE_ACCURACY) != null && Boolean.parseBoolean(context.getConfig().get(SELFIE_ACCURACY))) {
                                        context.getRealm().addAttribute(context.getRequestId(), SELFIE_REG_ACCURACY, String.valueOf(acc));
                                    }
                                    if (context.getConfig().get(SELFIE_DEVICE_TYPE) != null && Boolean.parseBoolean(context.getConfig().get(SELFIE_DEVICE_TYPE))) {
                                        context.getRealm().addAttribute(context.getRequestId(), SELFIE_REG_DEVICE_TYPE, Boolean.parseBoolean(formData.getFirst("geo_mobile")) ? "mobile" : "web");
                                    }
                                } else {
                                    com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EVSR0003, getId());
                                    GeneralHelper.auditEvent(context, EventType.EZ_SELFIE_REGISTRATION_ERROR, Errors.EVSR0003.getErrorReason());
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                GeneralHelper.auditEvent(context, EventType.EZ_SELFIE_REGISTRATION_ERROR, "Unable to get location");
                            }
                        } else {
                            GeneralHelper.auditEvent(context, EventType.EZ_SELFIE_REGISTRATION_ERROR, "Unable to get location");
                            com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EVSR0003, getId());
                        }
                    }
                }
            }
            try {
                incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), SELFIE_REGISTRATION);
                uploadSelfieImage(context);
            } catch (IOException e) {
                LoginFormsProvider loginFormsProvider = context.form();
                context.challenge(loginFormsProvider.createForm(BACKOFFICE_ERROR_FTL));
            }
        }
    }

    private void uploadSelfieImage(RequiredActionContext context) throws IOException {
        MultivaluedMap<String, String> formData;

        try {
            CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
            formData = context.getHttpRequest().getDecodedFormParameters();
            String imageDetails = formData.get(LIVELINESS_IMAGE_DETAILS).get(0);
            if (formData.get(LIVELINESS_IMAGE) != null && Boolean.parseBoolean(formData.getFirst(LIVELINESS_IMAGE))) {
                if (formData.containsKey(LIVELINESS_IMAGE_DETAILS) && formData.get(LIVELINESS_IMAGE_DETAILS) != null) {
                    Map<String, String> imageDetailsMap = JsonSerialization.readValue(imageDetails, Map.class);
                    File file = GeneralHelper.createTempFile(imageDetailsMap.get("front"));
                    Map<String, Object> faceTemplateResponse = selfieRegistrationHelper.uploadImage(file, context, session);
                    if(faceTemplateResponse != null && faceTemplateResponse.containsKey("success") && Boolean.parseBoolean(faceTemplateResponse.get("success").toString())) {
                        context.getUser().setSingleAttribute("face_template", String.valueOf(faceTemplateResponse.get("template")));
                        GeneralHelper.auditEvent(context, EventType.EZ_SELFIE_REGISTRATION, "");
                        context.getRealm().addAttribute(context.getRequestId(), SELFIE_REGISTERED, "true");
                        context.getRealm().addAttribute(context.getRequestId(), SELFIE_REGISTERED_AT, String.valueOf(Time.currentTime()));
                        context.success();
                    } else {
                        String errorMessage = faceTemplateResponse != null && faceTemplateResponse.containsKey("message") ? String.valueOf(faceTemplateResponse.get("message")) : "Couldn't register the face";
                        GeneralHelper.auditEvent(context, EventType.EZ_SELFIE_REGISTRATION_ERROR, faceTemplateResponse != null && faceTemplateResponse.containsKey("message") ? String.valueOf(faceTemplateResponse.get("message")) : "Couldn't register the face");
                        context.getRealm().addAttribute(context.getRequestId(), SELFIE_REGISTERED, "false");
                        context.getRealm().addAttribute(context.getRequestId(), SELFIE_REGISTERED_AT, String.valueOf(Time.currentTime()));
                        handleError(context, errorMessage);
                        context.getRealm().addAttribute(context.getRequestId(), SELFIE_REGISTRATION_FAILURE_REASON, faceTemplateResponse != null && faceTemplateResponse.containsKey("message") ? String.valueOf(faceTemplateResponse.get("message")) : "Couldn't register the face");
                        context.failure();
                    }
                } else {
                    LoginFormsProvider loginFormsProvider = context.form();
                    context.challenge(loginFormsProvider.createForm(BACKOFFICE_ERROR_FTL));
                }
            }
        } catch (IOException e) {
            GeneralHelper.auditEvent(context, EventType.EZ_SELFIE_REGISTRATION_ERROR, ErrorMappings.CONTACT_ADMIN);
            throw new IOException(e);
        }
    }

    private void handleError(RequiredActionContext context, String errorMessage) {
        if(errorMessage.contains("Spoof")) {
            com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EVSR0001,getId());
        } else if (errorMessage.equalsIgnoreCase("file_download_failed")) {
            com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EVSR0002,getId());
        }
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        this.session = session;
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
        return "bo-selfie-registration";
    }

    @Override
    public String[] stripeMappingId() {
        return new String[] {SELFIE_REGISTRATION};
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> claims = getDefaultClaims();

        Map<String, String> potentialClaims = getSelfieRegistrationPotentialClaims();

        if(model.getConfig().containsKey(SELFIE_LATITUDE) && Boolean.parseBoolean(model.getConfig().get(SELFIE_LATITUDE))) {
            claims.put(SELFIE_REG_LATITUDE, potentialClaims.get(SELFIE_REG_LATITUDE));
        }
        if(model.getConfig().containsKey(SELFIE_LONGITUDE) && Boolean.parseBoolean(model.getConfig().get(SELFIE_LONGITUDE))) {
            claims.put(SELFIE_REG_LONGITUDE, potentialClaims.get(SELFIE_REG_LONGITUDE));
        }
        if(model.getConfig().containsKey(SELFIE_ACCURACY) && Boolean.parseBoolean(model.getConfig().get(SELFIE_ACCURACY))) {
            claims.put(SELFIE_REG_ACCURACY, potentialClaims.get(SELFIE_REG_ACCURACY));
        }
        if(model.getConfig().containsKey(SELFIE_DEVICE_TYPE) && Boolean.parseBoolean(model.getConfig().get(SELFIE_DEVICE_TYPE))) {
            claims.put(SELFIE_REG_DEVICE_TYPE, potentialClaims.get(SELFIE_REG_DEVICE_TYPE));
        }

        return claims;
    }

    private static Map<String, String> getDefaultClaims() {
        Map<String, String> defaultClaims = new LinkedHashMap<>();

        defaultClaims.put(SELFIE_REGISTERED, ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put(SELFIE_REGISTERED_AT, ValidationTypes.DATE_AND_TIME.getValue());
        defaultClaims.put(SELFIE_REGISTRATION_SKIPPED, ValidationTypes.BOOLEAN.getValue());

        return defaultClaims;
    }

    private static Map<String, String> getSelfieRegistrationPotentialClaims() {
        Map<String, String> potentialClaims = new LinkedHashMap<>();

        potentialClaims.put(SELFIE_REG_LATITUDE, ValidationTypes.NUMBER.getValue());
        potentialClaims.put(SELFIE_REG_LONGITUDE, ValidationTypes.NUMBER.getValue());
        potentialClaims.put(SELFIE_REG_ACCURACY, ValidationTypes.NUMBER.getValue());
        potentialClaims.put(SELFIE_REG_DEVICE_TYPE, ValidationTypes.STRING.getValue());

        return potentialClaims;
    }

    @Override
    public boolean isClaimsCustomizable() {
        return false;
    }
}

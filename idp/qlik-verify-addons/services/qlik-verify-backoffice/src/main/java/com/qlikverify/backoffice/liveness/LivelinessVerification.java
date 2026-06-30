package com.qlikverify.backoffice.liveness;

import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.FormPartValue;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.qlikverify.backoffice.liveness.analytics.TwilioSegmentEvent;
import com.qlikverify.backoffice.liveness.analytics.TwilioSegmentHelper;
import com.qlikverify.backoffice.validators.ValidatorPolicy;
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
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.liveness.LivenessConstants.*;

public class LivelinessVerification implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    public static final String PROVIDER_ID = "bo-liveliness-verification";
    public static final String VERIFY_LIVELINESS_IMAGE_FTL = "liveliness_image_verification.ftl";
    private static final Logger logger = Logger.getLogger(LivelinessVerification.class);
    private static final String MODE = "bo";
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(LIVENESS_FACE_COMPARE);
        property.setLabel(LIVENESS_FACE_COMPARE);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText(LIVENESS_FACE_COMPARE_HELPTEXT);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(LIVENESS_BLINK);
        property.setLabel(LIVENESS_BLINK);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText(LIVENESS_BLINK_HELPTEXT);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(LIVENESS_MOVEMENT);
        property.setLabel(LIVENESS_MOVEMENT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText(LIVENESS_MOVEMENT_HELPTEXT);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(LIVENESS_FINGER_COUNT);
        property.setLabel(LIVENESS_FINGER_COUNT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText(LIVENESS_FINGER_COUNT_HELPTEXT);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(LIVENESS_SPEECH);
        property.setLabel(LIVENESS_SPEECH);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText(LIVENESS_SPEECH_HELPTEXT);
        configProperties.add(property);

    }

    LivenessHelper livenessHelper = new LivenessHelper();
    KeycloakSession session;

    public LivelinessVerification() {
        // NOP
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return PROVIDER_ID;
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        boolean result = true;
        String requestId = context.getRequestId();
        boolean hasAvailableVerification = context.getConfig().keySet().stream().filter(x -> !x.equalsIgnoreCase(LIVENESS_FACE_COMPARE))
                .filter(key -> context.getConfig().get(key).equals("true")).count() > 0;
        if (context.getPolicyId() != null) {
            result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
        }
        boolean livenessVerified = context.getRealm().hasTrxValueAttribute(requestId, LIVENESS_COMPLETED);
        boolean livenessVerifiedAt = context.getRealm().hasTrxValueAttribute(requestId, SP_LIVENESS_VERIFIED_AT);
        boolean hasSkipped = Boolean.parseBoolean(context.getRealm().getTrxValue(context.getRequestId(), "sp@" + LIVENESS_VERIFICATION_SKIPPED));
        if (result && !hasSkipped && hasAvailableVerification && !livenessVerified && !livenessVerifiedAt) {
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
        showView(context);
    }

    private void showView(RequiredActionContext context) {
        CibaAuthProvider cibaAuthProvider = getCibaAuthProvider(context);
        LoginFormsProvider loginFormsProvider = context.form();
        loginFormsProvider.setAuthenticationSession(context.getAuthenticationSession());
        loginFormsProvider.setAttribute("skippable", context.getModel().isSkippable());
        loginFormsProvider.setAttribute(IS_MOBILE, cibaAuthProvider.isMobile());
        loginFormsProvider.setAttribute(IS_DESKTOP, cibaAuthProvider.isDesktop());
        loginFormsProvider.setAttribute(IS_LIVENESS, true);
        loginFormsProvider.setAttribute(REQUEST_ID, context.getRequestId());
        loginFormsProvider.setAttribute("userId", context.getUser().getUsername());

        String authorizerId = context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        CibaAuthProvider provider = context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
        loginFormsProvider.setAttribute("isMobile", provider.isMobile());
        loginFormsProvider.setAttribute("jsWriterKey", EnvironmentReader.getValue("EZTO_JS_SEGMENT_WRITE_KEY", ""));

        boolean isRetry = context.getAuthenticationSession().getAuthNote("liveness_retry") != null && context.getAuthenticationSession().getAuthNote("liveness_retry").equals("true");

        if (context.getConfig() != null && context.getConfig().containsKey(LIVENESS_FACE_COMPARE) && Boolean.parseBoolean(context.getConfig().get(LIVENESS_FACE_COMPARE)) && context.getUser().getFirstAttribute(FACE_TEMPLATE) == null && !isRetry) {
            context.challenge(loginFormsProvider.createForm(SELFIE_FTL));
            return;
        }

        loginFormsProvider.setAttribute(TITLE, LIVENESS_VERIFICATION);
        loginFormsProvider.setAttribute(CONFIG, livenessHelper.processConfig(context));

//        HashMap<String, Object> traits = new HashMap<>();
//        traits.put("event", TwilioSegmentEvent.LV_START.name());
//        traits.put("message", "Liveness challenge start");
//
//        TwilioSegmentHelper.queueEvent(context.getRequestId(), context.getUser().getUsername(), traits);
        context.challenge(loginFormsProvider.createForm(LIVENESS_FTL));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        try {

            MultivaluedMap<String, FormPartValue> multiPartFormParameters = livenessHelper.getMultiPartFormParameters(context, context.getHttpRequest().getHttpHeaders().getMediaType(), context.getHttpRequest().getHttpHeaders(), context.getHttpRequest().getInputStream());

            MediaType mediaType = context.getHttpRequest().getHttpHeaders().getMediaType();

            if (LivenessHelper.isFormUrlEncoded(mediaType)) {
                MultivaluedMap<String, String> formParameters = context.getHttpRequest().getFormParameters();
                if (handleCancelAndRetry(context, formParameters, null)) {
                    return;
                }
            } else if (LivenessHelper.isMultipartFormData(mediaType)) {
                if (handleCancelAndRetry(context, null, multiPartFormParameters)) {
                    return;
                }
            }
            if (context.getHttpRequest().getHttpHeaders().getMediaType().equals(org.keycloak.utils.MediaType.APPLICATION_FORM_URLENCODED_TYPE) && context.getHttpRequest().getDecodedFormParameters().get("liveliness_image") != null) {
                if (livenessHelper.processFaceTemplate(context)) {
                    incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), stripeMappingId()[0]);
                    context.success();
                    return;
                } else {
                    context.getRealm().addAttribute(context.getRequestId(), SP_LIVENESS_VERIFIED, "false");
                    context.getRealm().addAttribute(context.getRequestId(), SP_LIVENESS_VERIFIED_AT, String.valueOf(Time.currentTime()));
                    context.failure();
                    return;
                }
            }

            incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), USER_LIVELINESS);
            context.getRealm().addAttribute(context.getRequestId(), LIVENESS_COMPLETED, "false");
            FormPartValue livenessVideo = multiPartFormParameters.getFirst("livenessVideo");
            CibaAuthConfigModel cibaAuthConfigByClientId = session.getContext().getRealm().getCibaAuthConfigByClientId(session.getContext().getClient().getClientId(), context.getRealm().getId());

            String filePath = livenessHelper.uploadToMinio(context, GeneralHelper.createMediaTempFile(livenessVideo.asInputStream()));

            //make request to liveness
            Map<String, String> sendDataToLivenessResponse = livenessHelper.sendDataToLiveness(context.getSession(), context, filePath, JsonSerialization.readValue(multiPartFormParameters.getFirst("livenessMeta").asString(), Map.class), JsonSerialization.readValue(context.getAuthenticationSession().getAuthNote("expected_liveness"), Map.class));

            if (sendDataToLivenessResponse != null && sendDataToLivenessResponse.containsKey("success") && String.valueOf(sendDataToLivenessResponse.get("success")).equals("true")) {
                logger.info("LIVENESS RESPONSE ::" + sendDataToLivenessResponse);
                GeneralHelper.auditEvent(context, EventType.EZ_LIVENESS_VERIFICATION, "");
            } else {
                logger.error("LIVENESS RESPONSE:: " + sendDataToLivenessResponse != null ? sendDataToLivenessResponse : "null");
                GeneralHelper.auditEvent(context, EventType.EZ_LIVENESS_VERIFICATION_ERROR, sendDataToLivenessResponse.get("code"));
            }
            String presignedObjectUrl = GeneralHelper.getPresignedObjectUrl(context, filePath);
            context.getRealm().addAttribute(context.getRequestId(), "sp@liveness_video", com.qlikverify.backoffice.GeneralHelper.getSecuredUrl(presignedObjectUrl, cibaAuthConfigByClientId));
            incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), stripeMappingId()[0]);
            context.success();
            return;
        } catch (IOException e) {
            logger.error("LIVENESS FAILED:: " + e.getMessage());
        }

        context.failure();
    }

    private boolean handleCancelAndRetry(RequiredActionContext context, MultivaluedMap<String, String> formParameters, MultivaluedMap<String, FormPartValue> multiPartFormParameters) {
        if (formParameters != null) {
            if (formParameters.containsKey(CANCEL)) {
                com.qlikverify.backoffice.helpers.GeneralHelper.handleCancel(context, EventType.EZ_LIVENESS_VERIFICATION_ERROR);
                return true;
            }
            if(context.getHttpRequest().getFormParameters().containsKey("skipped") && Boolean.parseBoolean(context.getHttpRequest().getFormParameters().getFirst("skipped"))) {
                context.getRealm().addAttribute(context.getRequestId(), "sp@" + LIVENESS_VERIFICATION_SKIPPED, "true");
                Map<String, String> skipDetails = new HashMap<>();
                skipDetails.put("User Action", "user_skipped");
                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_LIVENESS_VERIFICATION, "", skipDetails);
                context.success();
                return true;
            }
            if (formParameters.get(RETRY) != null && Boolean.parseBoolean(formParameters.get(RETRY).get(0))) {
                context.getAuthenticationSession().setAuthNote("liveness_retry", "true");
                showView(context);
                return true;
            }
        } else if (multiPartFormParameters != null) {
            if (multiPartFormParameters.containsKey(CANCEL)) {
                context.failure();
                return true;
            }
            if (multiPartFormParameters.get(RETRY) != null && Boolean.parseBoolean(multiPartFormParameters.get(RETRY).get(0).asString())) {
                context.getAuthenticationSession().setAuthNote("liveness_retry", "true");
                showView(context);
                return true;
            }
        }
        return false;
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
        return new String[]{MODE};
    }


    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId = context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        this.session = session;
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
    public String[] supportedRegions() {
        return new String[]{"in"};
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{USER_LIVELINESS};
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map <String, String> skipClaim = new HashMap<>();
        skipClaim.put(LIVENESS_VERIFICATION_SKIPPED, ValidatorPolicy.ValidationTypes.BOOLEAN.getValue());

        return skipClaim;
    }

    @Override
    public boolean isClaimsCustomizable() {
        return false;
    }
}

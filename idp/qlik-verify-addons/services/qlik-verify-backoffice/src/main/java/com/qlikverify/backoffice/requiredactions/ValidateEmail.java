package com.qlikverify.backoffice.requiredactions;

import com.qlikverify.backoffice.Constants;
import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import com.qlikverify.backoffice.helpers.emailValidationHelper.EmailValidationHelper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.validators.ValidatorPolicy.*;

public class ValidateEmail implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(ValidateEmail.class);

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "bo-validate-email";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {

        String prefix = getId().replace("bo-", "").replace("-", "_") + "@";

        configProperties.stream().filter(x -> x.getName().contains("claims"))
                .forEach(providerConfigProperty -> {
                            providerConfigProperty.setDefaultValue(getDefaultClaims().keySet().stream().map(y->prefix + y).collect(Collectors.joining("##")));
                            providerConfigProperty.setOptions(getValidateEmailPotentialClaims().keySet().stream().map(y->prefix + y).collect(Collectors.toList()));
                        }
                        );

        return configProperties;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    static {

        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName("email-valid-claims");
        property.setLabel("email-valid-claims");
        property.setType(ProviderConfigProperty.MULTIVALUED_LIST_TYPE);
        property.setHelpText("claims-hlp-txt");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName("email-valid-quality-score");
        property.setLabel("threshold-email-validation-quality-score");
        property.setType(ProviderConfigProperty.NUMBER_TYPE);
        property.setHelpText("email-validation-quality-score-hlp-txt");
        property.setRequired(true);
        property.setMaxVal(100);
        configProperties.add(property);
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
        boolean email = context.getRealm().hasTrxValueAttribute(requestId, VALIDATE_EMAIL_EMAIL_CLAIM);
        boolean emailVerified = context.getRealm().hasTrxValueAttribute(requestId, VALIDATE_EMAIL_EMAIL_VERIFIED_CLAIM);
        boolean emailVerifiedAt = context.getRealm().hasTrxValueAttribute(requestId, VALIDATE_EMAIL_EMAIL_VERIFIED_AT_CLAIM);
        boolean hasSkipped = Boolean.parseBoolean(context.getRealm().getTrxValue(requestId,  VALIDATE_EMAIL_SKIPPED));
        if(result && !hasSkipped && (!emailVerifiedAt || !email || !emailVerified)) {
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

        if(context.getConfig().get("email-valid-quality-score") == null || context.getConfig().get("email-valid-quality-score").isEmpty()){
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VALIDATION_ERROR, "Validate Email Threshold score is missing in config.");
            showView(context, "An error occurred. Please contact admin", BACKOFFICE_ERROR_FTL, null);
            return;
        }

        if(context.getUser().getEmail() != null) {
            validateEmail(context, context.getUser().getEmail());
        } else {
            LoginFormsProvider loginFormsProvider = context.form();
            loginFormsProvider
                    .setAuthenticationSession(context.getAuthenticationSession());
            loginFormsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());
            loginFormsProvider.setAttribute("title", "bo_email_val_title");
            context.challenge(loginFormsProvider.createForm(BO_VERIFY_EMAIL_FTL));
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        if(context.getUser() == null){
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VALIDATION_ERROR, ErrorMappings.USER_NOT_ONBOARDED);
            showView(context, "user_not_onboarded", BACKOFFICE_ERROR_FTL, null);
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if(formData.containsKey("cancel") && Boolean.parseBoolean(formData.getFirst("cancel"))) {
            GeneralHelper.handleCancel(context, EventType.EZ_EMAIL_VALIDATION_ERROR);
            return;
        }

        if(formData.containsKey("skipped") && Boolean.parseBoolean(formData.getFirst("skipped"))) {
            context.getRealm().addAttribute(context.getRequestId(),  VALIDATE_EMAIL_SKIPPED, "true");
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VALIDATION, "", skipDetails);
            context.success();
            return;
        }

        String email = formData.getFirst(EMAIL);

        UserModel user = context.getSession().users().getUserByEmail(context.getRealm(),email);
        if(user != null && context.getUser() != null && !context.getUser().getId().equals(user.getId())){
            com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VALIDATION_ERROR, ErrorMappings.USER_EXISTS_SAME_EMAIL);
            Map<String, String> validateEmailAttributes = new HashMap<>();
            validateEmailAttributes.put("title", "bo_email_val_title");
            showView(context, ErrorMappings.USER_EXISTS_SAME_EMAIL, BO_VERIFY_EMAIL_FTL, validateEmailAttributes);
            return;
        }

        validateEmail(context, email);

    }

    public void validateEmail(RequiredActionContext context, String email){
        try {
            Map<String, Object> response = EmailValidationHelper.getEmailDetails(context, email);
            logger.info(response);

            if (response == null) {
                GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VALIDATION_ERROR, ErrorMappings.EMAIL_VALIDATION_REACH_ERROR);
                Map<String, String> validateEmailAttributes = new HashMap<>();
                validateEmailAttributes.put("title", "bo_email_val_title");
                showView(context,ErrorMappings.EMAIL_VALIDATION_REACH_ERROR, BO_VERIFY_EMAIL_FTL,validateEmailAttributes);
                return;
            }

            if(response.containsKey("error")){
                com.qlikverify.backoffice.helpers.GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VALIDATION_ERROR, ((HashMap)response.get("error")).get("message").toString());
                showView(context, ErrorMappings.EMAIL_VALIDATION_CONFIG_MISSING, BACKOFFICE_ERROR_FTL, null);
                return;
            }

            if(context.getConfig().get("email-valid-quality-score") != null && response.get("quality_score") != null && (float) Integer.parseInt(context.getConfig().get("email-valid-quality-score"))/100 > Float.parseFloat(response.get("quality_score").toString())){
                GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VALIDATION_ERROR, ErrorMappings.EMAIL_VALIDATION_QUALITY_SCORE_ERROR);
                incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(),VALIDATE_EMAIL);
                context.failure();
                return;
            }

            GeneralHelper.auditEvent(context, EventType.EZ_EMAIL_VALIDATION, "");
            setClaims(context,response, email);
            incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(),VALIDATE_EMAIL);
            context.getUser().setEmail(email);
            context.success();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void setClaims(RequiredActionContext context, Map<String, Object> emailData, String email){
        RealmModel realmModel = context.getRealm();
        //default-claims
        realmModel.addAttribute(context.getRequestId(), VALIDATE_EMAIL_EMAIL_CLAIM, email);
        realmModel.addAttribute(context.getRequestId(), VALIDATE_EMAIL_EMAIL_VERIFIED_CLAIM, "true");
        realmModel.addAttribute(context.getRequestId(), VALIDATE_EMAIL_EMAIL_VERIFIED_AT_CLAIM, String.valueOf(Time.currentTime()));

        if(context.getConfig().size() == 0) {
            ArrayList<String> claims = new ArrayList<>();
            claims.add(EMAIL_VALIDATION_CLAIM);
            claims.add(EMAIL_VALIDATION_VERIFIED_CLAIM);
            claims.add(EMAIL_VALIDATION_VERIFIED_AT_CLAIM);

            GeneralHelper.handleSettingClaims(context.getRealm(), emailData, claims, "validate_email",context.getRequestId());
        }
        else {
            Arrays.asList(context.getConfig().get("email-valid-claims").split("##")).stream().forEach(x -> {
                if(emailData.containsKey(x) && emailData.get(x) instanceof HashMap){
                    emailData.put(x,  ((HashMap<?, ?>) emailData.get(x)).get("value"));
                }
            });
            GeneralHelper.handleSettingClaims(context.getRealm(), emailData, Arrays.asList(context.getConfig().get("email-valid-claims").split("##")), "validate_email",context.getRequestId());
        }
    }

    public static void showView(RequiredActionContext context, String error, String ftl,
                                Map<String, String> attributes) {
        LoginFormsProvider formsProvider = context.form();
        formsProvider.setAttribute(SKIPPABLE, context.getModel().isSkippable());
        if (error != null) {
            formsProvider.addError(new FormMessage(error));
            formsProvider.setAttribute(Constants.ERROR, error);
        }
        if (attributes != null) {
            attributes.forEach(formsProvider::setAttribute);
        }
        Response challenge = formsProvider.createForm(ftl);
        context.challenge(challenge);
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
        return "bo-validate-email";
    }

    @Override
    public String[] stripeMappingId() {
        return new String[] {"validate_email"};
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> claims = getDefaultClaims();
        // Define additional claims that may be added in the runtime to the model's config's
        Map<String, String> potentialClaims = getValidateEmailPotentialClaims();

        // Process claims from model config
        Set<String> configClaimKeys = model.getConfig().entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().contains("claims"))
                .flatMap(entry -> Arrays.stream(entry.getValue().split("##")))
                .collect(Collectors.toSet());

        // Add matching claims from potentialClaims
        configClaimKeys.forEach(key -> {
            if (potentialClaims.containsKey(key)) {
                claims.put(key, potentialClaims.get(key));
            }
        });

        //Modifying claims as per template specific
        Map<String, String> finalClaims = claims.entrySet().stream()
                .map(x-> Map.entry(getId().replace("bo-","").replace("-","_") + "@" +  x.getKey(), x.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        finalClaims.put(VALIDATE_EMAIL_SKIPPED, ValidationTypes.BOOLEAN.getValue());

        return finalClaims;
    }

    private static Map<String, String> getDefaultClaims() {
        Map<String, String> defaultClaims = new LinkedHashMap<>();
        defaultClaims.put("valid_email", ValidationTypes.STRING.getValue());
        defaultClaims.put("email_valid_verified", ValidationTypes.BOOLEAN.getValue());
        defaultClaims.put("email_valid_verified_at", ValidationTypes.DATE_AND_TIME.getValue());

        return defaultClaims;
    }

    private static Map<String, String> getValidateEmailPotentialClaims() {
        Map<String, String> potentialClaims = new LinkedHashMap<>();
        potentialClaims.put("valid_email", ValidationTypes.STRING.getValue());
        potentialClaims.put("email_valid_verified", ValidationTypes.BOOLEAN.getValue());
        potentialClaims.put("email_valid_verified_at", ValidationTypes.DATE_AND_TIME.getValue());
        potentialClaims.put("autocorrect", ValidationTypes.STRING.getValue());
        potentialClaims.put("deliverability", ValidationTypes.STRING.getValue());
        potentialClaims.put("quality_score", ValidationTypes.STRING.getValue());
        potentialClaims.put("is_valid_format", ValidationTypes.STRING.getValue());
        potentialClaims.put("is_free_email", ValidationTypes.STRING.getValue());
        potentialClaims.put("is_disposable_email", ValidationTypes.STRING.getValue());
        potentialClaims.put("is_role_email", ValidationTypes.STRING.getValue());
        potentialClaims.put("is_catchall_email", ValidationTypes.STRING.getValue());
        potentialClaims.put("is_mx_found", ValidationTypes.STRING.getValue());
        potentialClaims.put("is_smtp_valid", ValidationTypes.STRING.getValue());

        return potentialClaims;

    }

}

package com.qlikverify.backoffice.requiredactions;

import com.qlikverify.backoffice.Errors;
import com.qlikverify.backoffice.GeneralHelper;
import com.qlikverify.backoffice.helpers.nameSimilarityCheckHelper.NameSimilarityCheckHelper;
import com.qlikverify.backoffice.helpers.templateConfigParser.ConfigTemplateParser;
import com.qlikverify.backoffice.validators.ValidatorPolicy;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;

import java.util.*;

import static com.qlikverify.backoffice.Constants.*;
import static org.keycloak.models.Constants.CURRENT_ACTION_ID;

public class NameSimilarityCheck implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(NameSimilarityCheck.class);
    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "bo-name-similarity-check";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(NAME_SIMILARITY_CHECK_TITLE);
        property.setLabel(NAME_SIMILARITY_CHECK_TITLE);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText(NAME_SIMILARITY_CHECK_TITLE_HLP_TXT);
        property.setRequired(true);

        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(NAME_SIMILARITY_CHECK_DESC);
        property.setLabel(NAME_SIMILARITY_CHECK_DESC);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText(NAME_SIMILARITY_CHECK_DESC_HLP_TXT);

        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(FIRST_NAME_TO_BE_COMPARED);
        property.setLabel(FIRST_NAME_TO_BE_COMPARED);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setHelpText(FIRST_NAME_TO_BE_COMPARED_HLP_TXT);

        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(SECOND_NAME_TO_BE_COMPARED);
        property.setLabel(SECOND_NAME_TO_BE_COMPARED);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setRequired(true);
        property.setHelpText(SECOND_NAME_TO_BE_COMPARED_HLP_TXT);

        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
        property.setHelpText(NAME_SIMILARITY_CHECK_WARN_HLP_TXT);
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
        String title = context.getConfig().get(NAME_SIMILARITY_CHECK_TITLE);
        boolean similarityCheckDone = context.getRealm().hasTrxValueAttribute(requestId, title.toLowerCase().replaceAll("\\s+", "_").toLowerCase() + "_score");

        if (result && !similarityCheckDone) {
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
        LoginFormsProvider loginFormsProvider = context.form();
        if (!context.getConfig().isEmpty()) {
            loginFormsProvider.setAttribute("nameSimilarityCheckTitle", context.getConfig().get(NAME_SIMILARITY_CHECK_TITLE));
            if(context.getConfig().get(NAME_SIMILARITY_CHECK_DESC) != null && !context.getConfig().get(NAME_SIMILARITY_CHECK_DESC).isEmpty()) {
                loginFormsProvider.setAttribute("nameSimilarityCheckDesc", context.getConfig().get(NAME_SIMILARITY_CHECK_DESC));
            }
            loginFormsProvider.setAttribute("isMobile", getCibaAuthProvider(context).isMobile());
            context.challenge(loginFormsProvider.createForm("name_similarity_check.ftl"));
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        String title = context.getConfig().get(NAME_SIMILARITY_CHECK_TITLE);
        String firstNameToBeCompared = context.getConfig().get(FIRST_NAME_TO_BE_COMPARED);
        String secondNameToBeCompared = context.getConfig().get(SECOND_NAME_TO_BE_COMPARED);

        ActionModel actionById = context.getRealm().getActionById(context.getAuthenticationSession().getAuthNote(CURRENT_ACTION_ID));
        ConfigTemplateParser configTemplateParser = new ConfigTemplateParser(actionById.getActionVariables(), context.getRealm().getTrx(context.getRequestId()).getAttributes());
        firstNameToBeCompared = configTemplateParser.parseTemplate(firstNameToBeCompared);
        secondNameToBeCompared = configTemplateParser.parseTemplate(secondNameToBeCompared);

        if(firstNameToBeCompared.isEmpty() || secondNameToBeCompared.isEmpty()) {
            GeneralHelper.auditEvent(context, EventType.EZ_NAME_SIMILARITY_CHECK_ERROR, String.format("First Name - [%s]  Second Name - [%s]  Comparison failed because one of the values is empty.", firstNameToBeCompared, secondNameToBeCompared));
            context.getRealm().addAttribute(context.getRequestId(), title.toLowerCase().replaceAll("\\s+", "_").toLowerCase() + "_failure_reason", "Comparison value is empty.");
            setNameSimilarityClaims(context, title, "0.0", firstNameToBeCompared, secondNameToBeCompared);
            com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EVNS0001, getId());
            context.success();
            return;
        }

        if(!firstNameToBeCompared.matches("[a-zA-Z .]+") || !secondNameToBeCompared.matches("[a-zA-Z .]+")) {
            GeneralHelper.auditEvent(context, EventType.EZ_NAME_SIMILARITY_CHECK_ERROR, String.format("First Name - [%s]  Second Name - [%s]  Comparison failed because the value contains characters other than alphabets.", firstNameToBeCompared, secondNameToBeCompared));
            setNameSimilarityClaims(context, title, "0.0", firstNameToBeCompared, secondNameToBeCompared);
            context.getRealm().addAttribute(context.getRequestId(), title.toLowerCase().replaceAll("\\s+", "_").toLowerCase() + "_failure_reason", "Comparison value contains characters other than alphabets.");
            com.qlikverify.backoffice.GeneralHelper.setErrorAttribute(context, Errors.EVNS0002, getId());
            context.success();
            return;
        }


        double score = NameSimilarityCheckHelper.calculateNameSimilarity(firstNameToBeCompared, secondNameToBeCompared);
        incrementModuleCounter(context.getRequestId(), context.getRealm(), context.getUser(), NAME_SIMILARITY_CHECK);
        setNameSimilarityClaims(context, title, String.valueOf(score), firstNameToBeCompared, secondNameToBeCompared);
        GeneralHelper.auditEvent(context, EventType.EZ_NAME_SIMILARITY_CHECK, "");
        context.success();
    }

    private void setNameSimilarityClaims(RequiredActionContext context, String title, String score, String firstNameToBeCompared, String secondNameToBeCompared) {
        String lowerCase = title.toLowerCase().replaceAll("\\s+", "_").toLowerCase();
        Map<String, String> claims = new HashMap<>();
        claims.put(lowerCase + "_first_name_to_be_compared", firstNameToBeCompared);
        claims.put(lowerCase + "_second_name_to_be_compared", secondNameToBeCompared);
        claims.put(lowerCase + "_score", score);
        context.getRealm().addTrxAttributes(context.getRequestId(), claims);
        context.getAuthenticationSession().setAuthNote(context.getModel().getId(), "completed");
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
        return "bo-name-similarity-check";
    }

    @Override
    public String[] stripeMappingId() {
        return new String[] {NAME_SIMILARITY_CHECK};
    }

    private CibaAuthProvider getCibaAuthProvider(RequiredActionContext context) {
        String authorizerId =
                context.getAuthenticationSession().getClient().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        return context.getSession().getProvider(CibaAuthProvider.class, authorizerId);
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> claims = getDefaultClaims(model);
        return claims;
    }

    @Override
    public boolean skippable() {
        return false;
    }

    @Override
    public void onUpdate(RequiredActionProviderRepresentation rep) {
        validateConfigs(rep);
    }

    private static Map<String, String> getDefaultClaims(RequiredActionProviderModel model) {
        Map<String, String> defaultClaims = new LinkedHashMap<>();
        String title = model.getConfig().get(NAME_SIMILARITY_CHECK_TITLE);

        defaultClaims.put(title.toLowerCase().replaceAll("\\s+", "_").toLowerCase() + "_score", ValidatorPolicy.ValidationTypes.NUMBER.getValue());
        defaultClaims.put(title.toLowerCase().replaceAll("\\s+", "_").toLowerCase() + "_first_name_to_be_compared", ValidatorPolicy.ValidationTypes.STRING.getValue());
        defaultClaims.put(title.toLowerCase().replaceAll("\\s+", "_").toLowerCase() + "_second_name_to_be_compared", ValidatorPolicy.ValidationTypes.STRING.getValue());

        return defaultClaims;
    }

    private void validateConfigs(RequiredActionProviderRepresentation rep) {
        String title = rep.getConfig().get(NAME_SIMILARITY_CHECK_TITLE);

        if (!title.matches("[a-zA-Z0-9 ]+")) {
            throw new IllegalArgumentException("Enter valid title without special characters.");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("Invalid configs. Title should not exceed 200 characters.");
        }
    }

}

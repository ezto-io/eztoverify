package com.qlikverify.backoffice.requiredactions;

import com.qlikverify.backoffice.helpers.templateConfigParser.ConfigTemplateParser;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keycloak.models.Constants.CURRENT_ACTION_ID;

public class CustomResponse implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    public static final String ID = "custom-response";
    public static final String CUSTOM_RESPONSE_KEY = "custom-response-key";
    public static final String CUSTOM_RESPONSE_VALUE = "custom-response-value";

    private static final Logger logger = Logger.getLogger(CustomResponse.class);
    private static final String LOG_TAG = "CUSTOM_RESPONSE:: ";

    static List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(CUSTOM_RESPONSE_KEY);
        property.setLabel(CUSTOM_RESPONSE_KEY);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setMaxVal(100);
        property.setRequired(true);
        property.setHelpText("custom-response-key-helptext");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(CUSTOM_RESPONSE_VALUE);
        property.setLabel(CUSTOM_RESPONSE_VALUE);
        property.setType(ProviderConfigProperty.TEXT_TYPE);
        property.setRequired(true);
        property.setHelpText("custom-response-value-helptext");
        configProperties.add(property);
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        return this;
    }

    @Override
    public String getDisplayText() {
        return ID;
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
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
        ActionModel actionById = context.getRealm().getActionById(context.getAuthenticationSession().getAuthNote(CURRENT_ACTION_ID));
        String responseValue = new ConfigTemplateParser(actionById.getActionVariables(), context.getRealm().getTrx(requestId).getAttributes()).parseTemplate(context.getConfig().get(CUSTOM_RESPONSE_VALUE));
        String trxValue = context.getRealm().getTrxValue(context.getRequestId(), context.getConfig().get(CUSTOM_RESPONSE_KEY));

        if (result && !(trxValue != null && trxValue.equals(responseValue))) {
            addAction(context);
        } else {
            context.getUser().removeRequiredAction(context.getModel().getId());
        }

    }

    private void addAction(RequiredActionContext context) {
        RequiredActionProviderModel model = context.getModel();
        if (context.getUser().getRequiredActionsStream().noneMatch(action -> action.equals(model.getId()))) {
            logger.info(LOG_TAG + "required action added");
            context.getUser().addRequiredAction(model.getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Map<String, String> config = context.getConfig();
        if (config.containsKey(CUSTOM_RESPONSE_KEY) && config.containsKey(CUSTOM_RESPONSE_VALUE)) {
            ActionModel actionById = context.getRealm().getActionById(context.getAuthenticationSession().getAuthNote(CURRENT_ACTION_ID));
            TrxModel trx = context.getRealm().getTrx(context.getRequestId());
            String key = config.get(CUSTOM_RESPONSE_KEY);
            String value = config.get(CUSTOM_RESPONSE_VALUE);
            context.getRealm().addAttribute(
                    context.getRequestId(),
                    config.get(CUSTOM_RESPONSE_KEY),
                    new ConfigTemplateParser(actionById.getActionVariables(), trx.getAttributes()).parseTemplate(config.get(CUSTOM_RESPONSE_VALUE))
            );
            logger.info(LOG_TAG + "value has been assigned to the custom key - " + key);
        }

        context.success();
    }

    @Override
    public void processAction(RequiredActionContext context) {
        //NOP
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        //NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOP
    }

    @Override
    public void close() {
        //NOP
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{ID.replace("-", "_")};
    }

    @Override
    public boolean skippable() {
        return false;
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public void onUpdate(RequiredActionProviderRepresentation rep) {
        validateConfigForCustomResponse(rep);
    }

    private static void validateConfigForCustomResponse(RequiredActionProviderRepresentation rep) {
        try {
            String customResponseKey = rep.getConfig().get(CUSTOM_RESPONSE_KEY);
            String regex = "[^a-zA-Z0-9_]";

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(customResponseKey);

            if(matcher.find()) {
                throw new IllegalArgumentException("Enter valid key without special characters");
            }
        }  catch (Exception e) {
            logger.error("CustomResponse :: validateConfigForCustomResponse  :: " + e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        }
    }

}
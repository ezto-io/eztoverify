package com.qlikverify.backoffice.requiredactions;

import com.qlikverify.backoffice.ErrorMappings;
import com.qlikverify.backoffice.helpers.GeneralHelper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.*;
import org.keycloak.authentication.requiredactions.ConsoleTermsAndConditions;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.util.JsonSerialization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.helpers.GeneralHelper.showView;

public class CustomMessage implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(CustomMessage.class);
    private static final String LOG_TAG = "CUSTOM_MESSAGE:: ";

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return this;
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        return ConsoleTermsAndConditions.SINGLETON;
    }

    @Override
    public String getDisplayText() {
        return "bo-custom-message";
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
        property.setName(CUSTOM_MESSAGE_LOGO);
        property.setLabel(CUSTOM_MESSAGE_LOGO);
        property.setType(ProviderConfigProperty.IMAGE_FILE);
        property.setHelpText("custom-message-logo-hlp-txt");
        property.setRequired(true);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(CUSTOM_MESSAGE_TITLE);
        property.setLabel(CUSTOM_MESSAGE_TITLE);
        property.setMaxVal(60);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("custom-message-title-hlp-txt");
        property.setRequired(true);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(CUSTOM_MESSAGE_DESC);
        property.setLabel(CUSTOM_MESSAGE_DESC);
        property.setMaxVal(200);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("custom-message-desc-hlp-txt");
        property.setRequired(true);
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

        if (result && !context.getRealm().hasTrxValueAttribute(requestId, "ik@custom_message")) {
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
        if (!context.getRealm().hasTrxValueAttribute(context.getRequestId(), "custom_message")) {
            Map<String, String> config = context.getConfig();
            if (config.isEmpty() ||
                    Stream.of("custom-message-logo", "custom-message-title", "custom-message-desc")
                            .anyMatch(key -> config.get(key) == null || config.get(key).isEmpty())) {
                GeneralHelper.auditEvent(context, EventType.EZ_CUSTOM_MESSAGE_ERROR, "Config(s) missing");
                Map<String, String> contactAdminErrMsg = new HashMap<>();
                contactAdminErrMsg.put(ERROR_MESSAGE, ErrorMappings.CONTACT_ADMIN);
                showView(context, null, BACKOFFICE_ERROR_FTL, contactAdminErrMsg);
                logger.error(LOG_TAG + "requiredActionChallenge :: Config(s) missing");
                return;
            }
            try {
                GeneralHelper.auditEvent(context, EventType.EZ_CUSTOM_MESSAGE, "");
                String customMessage = JsonSerialization.writeValueAsString(context.getConfig());
                context.getRealm().addAttribute(context.getRequestId(), "ik@custom_message", customMessage);
                logger.info(LOG_TAG + "custom message - " + customMessage);
                context.success();
            } catch (Exception e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // NOP
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
        return "bo-custom-message";
    }

    @Override
    public void onUpdate(RequiredActionProviderRepresentation rep) {

    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{CUSTOM_MESSAGE};
    }

    @Override
    public boolean skippable() {
        return false;
    }
}

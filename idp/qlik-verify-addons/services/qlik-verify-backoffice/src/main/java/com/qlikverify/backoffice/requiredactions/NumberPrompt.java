package com.qlikverify.backoffice.requiredactions;

import com.qlikverify.backoffice.GeneralHelper;
import com.qlikverify.backoffice.validators.ValidatorPolicy;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.ciba.models.BindingMessage;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.qlikverify.backoffice.Constants.*;
import static com.qlikverify.backoffice.helpers.GeneralHelper.handleCancel;

public class NumberPrompt implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(NumberPrompt.class);

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        return this;
    }

    @Override
    public String getDisplayText() {
        return "number-prompt";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return List.of();
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
        boolean promptSuccess = context.getRealm().hasTrxValueAttribute(requestId, PROMPT_SUCCESS);
        boolean promptCompleted = context.getRealm().hasTrxValueAttribute(requestId, PROMPT_COMPLETED_AT);
        if (result && (!promptSuccess || !promptCompleted) && Integer.parseInt(getNumberPrompt(context)) != 0) {
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        Map<String, Object> connectionDetails = new HashMap<>();
        connectionDetails.put(IP, context.getConnection().getRemoteAddr());
        connectionDetails.put(DEVICE, context.getConnection().getClientOs());
        connectionDetails.put(LOCATION, context.getConnection().getClientLocation().getLocation());
        connectionDetails.put(DATE_AND_TIME, ZonedDateTime.now(
                        ZoneId.of(context.getConnection().getClientLocation().getBody().getZoneId()))
                .format(formatter));

        BindingMessage bindingMessage;
        try {
            bindingMessage = getBindingMessage(context);
        } catch (Exception e) {
            GeneralHelper.auditEvent(context, EventType.EZ_NUMBER_PROMPT_ERROR, "Error while deserializing binding message: " + e.getMessage());
            logger.error("Error while deserializing " + e.getMessage());
            context.failure();
            return;
        }

        String promptTitle = "prompt-title";
        String promptDescription = "prompt-description";

        if (bindingMessage != null) {
            if (!bindingMessage.getMetadata().isEmpty()) {
                promptTitle = bindingMessage.getMetadata().getOrDefault(PROMPT_TITLE, promptTitle);
                promptDescription = bindingMessage.getMetadata().getOrDefault(PROMPT_DESCRIPTION, promptDescription);
            }
        }


        Response challenge = context.form()
                .setAttribute(PROMPT_TITLE, promptTitle)
                .setAttribute(PROMPT_DESCRIPTION, promptDescription)
                .setAttribute(CONNECTION_DETAILS, connectionDetails)
                .setAttribute(PROMPT_OPTIONS, GeneralHelper.generateUniqueRandomNumbers(
                        Integer.parseInt(getNumberPrompt(context))
                ))
                .createForm("number-prompt.ftl");
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("cancel") && Boolean.parseBoolean(formData.getFirst("cancel"))) {
            handleCancel(context, EventType.EZ_NUMBER_PROMPT_ERROR);
            return;
        }

        if (formData.containsKey("selectedNumber")) {
            String selectedNumber = formData.getFirst("selectedNumber");
            if (Validation.isBlank(selectedNumber)) {
                GeneralHelper.auditEvent(context, EventType.EZ_NUMBER_PROMPT_ERROR, "Invalid prompt selection");
                context.failure();
                return;
            }

            String numberPrompt = getNumberPrompt(context);

            if (!Objects.equals(numberPrompt, selectedNumber)) {
                GeneralHelper.auditEvent(context, EventType.EZ_NUMBER_PROMPT_ERROR, "Wrong prompt selected");
                context.getRealm().addAttribute(context.getRequestId(), PROMPT_SUCCESS, "false");
                context.getRealm().addAttribute(context.getRequestId(), PROMPT_COMPLETED_AT, String.valueOf(Time.currentTime()));
                context.failure();
                return;
            }
        }

        GeneralHelper.auditEvent(context, EventType.EZ_NUMBER_PROMPT, "");
        context.getRealm().addAttribute(context.getRequestId(), PROMPT_SUCCESS, "true");
        context.getRealm().addAttribute(context.getRequestId(), PROMPT_COMPLETED_AT, String.valueOf(Time.currentTime()));
        context.success();
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
        return "number-prompt";
    }

    @Override
    public Map<String, String> getActionClaims(KeycloakSession session, RequiredActionProviderModel model) {
        Map<String, String> claims = new LinkedHashMap<>();
        claims.put(PROMPT_SUCCESS, ValidatorPolicy.ValidationTypes.BOOLEAN.getValue());
        claims.put(PROMPT_COMPLETED_AT, ValidatorPolicy.ValidationTypes.DATE_AND_TIME.getValue());

        return claims;
    }

    private BindingMessage getBindingMessage(RequiredActionContext context) throws IOException {
        return JsonSerialization.readValue(context.getAuthenticationSession().getAuthNote("message"), BindingMessage.class);
    }

    private String getNumberPrompt(RequiredActionContext context) {
        try {
            BindingMessage bindingMessage = getBindingMessage(context);

            if (bindingMessage == null || bindingMessage.getMetadata() == null) return "0";
            return bindingMessage.getMetadata().getOrDefault("numberPrompt", "0");
        } catch (IOException e) {
            return "0";
        }
    }

    @Override
    public String[] stripeMappingId() {
        return new String[]{"number_prompt"};
    }

    @Override
    public boolean skippable() {
        return false;
    }

    @Override
    public boolean isNew() { return true; }

}
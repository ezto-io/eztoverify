package com.qlikverify.services.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.policy.ciba.CibaPolicyProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;

public class AllowAccessRequiredAction implements RequiredActionProvider, RequiredActionFactory {

    public static final String ERROR_MESSAGE = "denyErrorMessage";
    public static final String EVALUATE_POLICY = "evaluatePolicy";
    public static final String DEFAULT_ERROR_MESSAGE = "deny-error-message";



    private static final Logger logger = Logger.getLogger(BlockUserRequiredAction.class);

    @Override
    public String getDisplayText() {
        return "allow-access-ra";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();


    static {

        ProviderConfigProperty errorMessage = new ProviderConfigProperty();
        errorMessage.setType(ProviderConfigProperty.STRING_TYPE);
        errorMessage.setName(ERROR_MESSAGE);
        errorMessage.setLabel("deny-access-authenticator-err-msg");
        errorMessage.setHelpText("deny-access-authenticator-error-msg-helptext");
        configProperties.add(errorMessage);
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
        return new String[] {"ciam","wiam"};
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getPolicyId() != null) {
            boolean result;
            String policyType = context.getPolicyType();
            if (policyType.equals("action")) {
                result = context.getSession().getProvider(ActionPolicyManagerProvider.class).validate(context, context.getPolicyId(), context.getUser());
            } else {
                result = context.getSession().getProvider(CibaPolicyProvider.class).validate(context.getSession(), context.getAuthenticationSession().getAuthNote(BINDING_MESSAGE), context.getPolicyId());
            }
            if (result) {
                addAction(context);
            } else {
                context.getUser().removeRequiredAction(getId());
            }
        }
    }

    private void addAction(RequiredActionContext context) {
        if (!context.getUser().getRequiredActionsStream().filter(x -> x.equalsIgnoreCase(getId())).findFirst().isPresent()) {
            context.getUser().addRequiredAction(getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        context.getEvent().user(context.getUser()).event(EventType.LOGIN_ERROR).error("User Denied Access");

        showview(context);
    }

    private void showview(RequiredActionContext context) {
        String errorMessage = Optional.ofNullable(context.getConfig())
                .map(f -> f.get(ERROR_MESSAGE))
                .filter(x-> x !=null && !x.isEmpty())
                .orElse(DEFAULT_ERROR_MESSAGE);

        Response challenge = context.form()
                .setError(errorMessage)
                .createErrorPage(Response.Status.UNAUTHORIZED);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        showview(context);
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
        return "allow-access-ra";
    }
}

package com.qlikverify.services.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.policy.ciba.CibaPolicyProvider;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;

public class BlockUserRequiredAction implements RequiredActionProvider, RequiredActionFactory {

    private static final Logger logger = Logger.getLogger(BlockUserRequiredAction.class);

    @Override
    public String getDisplayText() {
        return "block-user-account-ra";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
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
        context.getEvent().user(context.getUser()).event(EventType.LOGIN_ERROR).error("User Blocked");
        //This should be added to block the account from accessing again
        context.getUser().setEnabled(false);
        LoginFormsProvider loginFormsProvider = context.form();
        context.challenge(loginFormsProvider.createForm("block-user.ftl"));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        LoginFormsProvider loginFormsProvider = context.form();
        context.challenge(loginFormsProvider.createForm("block-user.ftl"));
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
        return "block-user-account";
    }
}

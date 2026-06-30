package com.qlikverify.backoffice.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.qlikverify.backoffice.Constants.*;

public class SubWorkflow implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(SubWorkflow.class);

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        return this;
    }

    @Override
    public String getDisplayText() {
        return "bo-sub-workflow";
    }

    @Override
    public RequiredActionEnum requiredActionType() {
        return RequiredActionEnum.ACTION;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();
        ProviderConfigProperty provider = new ProviderConfigProperty();

        provider.setName("sub-workflow");
        provider.setLabel("sub-workflow");
        provider.setHelperLinkText("to-view-selected-sub-workflow");
        provider.setHelperLink("/realms/" + session.getContext().getRealm().getName() + "/authentication/sub-workflows/{{id}}");
        provider.setRequired(true);
        provider.setType(ProviderConfigProperty.MAP_LIST_EXCEPT_CURRENT_CONFIG_TYPE);
        List<ActionModel> availableSubWorkflows = session.getContext().getRealm().getActionStream()
                .filter(actionModel -> !actionModel.isInBuilt())
                .filter(actionModel -> actionModel.getIsSubWorkflow())
                .filter(actionModel -> {
                    if (session.getContext().getRealm().getMode().equals("bo")) {
                        return actionModel.getActionType() != null && !actionModel.getActionType().equals("neutral");
                    }
                    return true;
                })
                .collect(Collectors.toList());

        List<Map> actionMap = availableSubWorkflows.stream().map(action -> {
            HashMap<String, String> newMap = new HashMap<>();
            newMap.put("name", action.getAlias());
            newMap.put("value", action.getId());
            return newMap;
        }).collect(Collectors.toList());
        provider.setMapOptions(actionMap);
        provider.setHelpText("");

        configProperties.add(provider);


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

        if (result && !context.getRealm().hasTrxValueAttribute(context.getRequestId(), "ik@" + context.getModel().getId())) {
            addAction(context);
        }
        if(context.getRealm().hasTrxValueAttribute(context.getRequestId(), context.getModel().getId()) && canRemoveWorkflow(context)) {
            context.getUser().removeRequiredAction(context.getModel().getId());
        }
    }

    public boolean canRemoveWorkflow(RequiredActionContext context) {
        String subWorkflowId = context.getConfig().get("sub-workflow");

        // If there is no sub-workflow, allow removal
        if (subWorkflowId == null) {
            return true;
        }

        // Get the required actions in the sub-workflow
        List<String> subWorkflowActions = context.getRealm()
                .getRequiredActionProvidersStream(subWorkflowId)
                .map(RequiredActionProviderModel::getId)
                .collect(Collectors.toList());

        // Get the user's currently required actions
        List<String> userRequiredActions = context.getUser()
                .getRequiredActionsStream()
                .collect(Collectors.toList());

        // If any sub-workflow action is still pending (present in userRequiredActions), prevent removal
        boolean canRemove = subWorkflowActions.stream()
                .noneMatch(userRequiredActions::contains);

        return canRemove;
    }

    private void addAction(RequiredActionContext context) {
        RequiredActionProviderModel model = context.getModel();
        if (context.getUser().getRequiredActionsStream().noneMatch(action -> action.equals(model.getId()))) {
            context.getUser().addRequiredAction(model.getId());
        }
    }


    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        context.getRealm().addAttribute(context.getRequestId(), "ik@" + context.getModel().getId(), "true");
        context.challenge(null);
        context.success();
    }

    @Override
    public void processAction(RequiredActionContext context) {

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
        return "bo-sub-workflow";
    }

    @Override
    public String[] stripeMappingId() {
        return new String[] {"sub_workflow"};
    }

    @Override
    public boolean skippable() {
        return false;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}

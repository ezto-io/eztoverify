package com.qlikverify.services.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionEnum;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.policy.action.ActionPolicyManagerProvider;
import org.keycloak.policy.ciba.CibaPolicyProvider;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;
import java.util.stream.Stream;

import static com.qlikverify.services.constants.Constants.EDIT_SUPPORTED_APPS;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;
public class SSOMandateUserRoleRequiredAction implements RequiredActionProvider, RequiredActionFactory {
    private static final Logger logger = Logger.getLogger(SSOMandateUserRoleRequiredAction.class);
    @Override
    public String getDisplayText() {
        return "sso-user-role-check-ra";
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
        return new String[] {"wiam"};
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
        ClientModel clientModel = context.getSession().getContext().getClient();

        UserModel userModel = context.getUser();
        boolean isSuccess = true;
        if (clientModel.getAttribute("isSSO") != null) {
            if (clientModel.getAttribute(EDIT_SUPPORTED_APPS) != null && !clientModel.getAttribute(EDIT_SUPPORTED_APPS).isEmpty()){
                List<String> subAppRoles = new ArrayList<>();
                Arrays.stream(clientModel.getAttribute(EDIT_SUPPORTED_APPS).split(",")).forEach(x ->
                        subAppRoles.add("sso-"+clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_")+"-"+x.replace(" ", "_")+"-role")
                );

                isSuccess = hasUserRole(subAppRoles, context.getRealm(), userModel);
            } else {
                String roleName = "sso-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-" + clientModel.getAttribute("sso_type") + "-role";
                isSuccess = hasUserRole(List.of(roleName),context.getRealm(),userModel);
            }
        }

        // Here role for iws or other sso will be created
        if(clientModel.getAttribute("sso.show") != null && clientModel.getAttribute("sso.show").equals("true"))
        {
            String roleName = "";
            if (clientModel.getAttribute("sso_type")!=null && !clientModel.getAttribute("sso_type").isEmpty()){
                roleName= "sso-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-"+ clientModel.getAttribute("sso_type") +
            "-role";
            } else {
                if(clientModel.getAttribute("iws.enable") != null && clientModel.getAttribute("iws.enable").equalsIgnoreCase("true")){
                        roleName = "iws-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-role";
                }
                else{
                    roleName= "sso-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-role";
                }
            }
            isSuccess = hasUserRole(List.of(roleName),context.getRealm(),userModel);
        }
        if (!isSuccess) {
            context.getEvent().user(context.getUser()).event(EventType.SSO).error("User Blocked to access SSO due to invalid role");
            LoginFormsProvider loginFormsProvider = context.form();
            context.challenge(loginFormsProvider.createForm("sso-block-user.ftl"));
            return;
        }

        context.success();
    }
    private boolean hasUserRole(List<String> roles, RealmModel realmModel, UserModel userModel) {
        return roles.stream()
                .map(x -> Optional.ofNullable(x)
                        .flatMap(roleName -> realmModel.getRolesStream()
                                .filter(role -> role != null && role.getName().equalsIgnoreCase(roleName))
                                .findFirst())
                        .orElse(null))
                .filter(Objects::nonNull)
                .anyMatch(role -> {
                    Stream<RoleModel> userRolesStream = userModel.getRoleMappingsStream()
                            .filter(userRole -> userRole != null && userRole.getName().equalsIgnoreCase(role.getName()));
                    Stream<RoleModel> groupRolesStream = userModel.getGroupsStream()
                            .flatMap(groupModel -> groupModel.getRoleMappingsStream()
                                    .filter(userRole -> userRole != null && userRole.getName().equalsIgnoreCase(role.getName())));
                    return Stream.concat(userRolesStream, groupRolesStream).count() > 0;
                });
    }
    @Override
    public void processAction(RequiredActionContext context) {
        ClientModel clientModel = context.getSession().getContext().getClient();

        UserModel userModel = context.getUser();
        boolean isSuccess = true;
        if (clientModel.getAttribute("isSSO") != null) {
            String roleName = "sso-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-" + clientModel.getAttribute("sso_type") + "-role";
            isSuccess = hasUserRole(List.of(roleName),context.getRealm(),userModel);
        }

        if(clientModel.getAttribute("sso.show") != null && clientModel.getAttribute("sso.show").equals("true"))
        {
            String roleName = "sso-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_")  + "-role";
            isSuccess = hasUserRole(List.of(roleName),context.getRealm(),userModel);
        }
        if (!isSuccess) {
            LoginFormsProvider loginFormsProvider = context.form();
            context.challenge(loginFormsProvider.createForm("sso-block-user.ftl"));
            return;
        }

        context.success();
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
        return "sso-user-role-check";
    }
}
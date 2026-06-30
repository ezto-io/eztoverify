package com.qlikverify.extensions.admin.controllers.workflows.eventlistener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.extensions.admin.controllers.workflows.handlers.YoutrackHandler;
import com.qlikverify.extensions.admin.controllers.workflows.jpa.WorkflowsEntity;
import com.qlikverify.extensions.admin.controllers.workflows.misc.WorkflowBaseUserRequestModel;
import com.qlikverify.extensions.admin.controllers.workflows.misc.helper.WorkflowHelper;
import com.qlikverify.extensions.admin.controllers.workflows.misc.models.EventMessageData;
import com.qlikverify.extensions.admin.controllers.workflows.rest.WorkflowsResourceFunction;
import com.qlikverify.extensions.admin.misc.helpers.JsonUtil;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.validation.Validation;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.qlikverify.extensions.admin.controllers.workflows.misc.Constants.*;

public class WorkflowsEventListenerResource {

    //~ Static fields/initializers =========================================================================================================

    private final KeycloakSession session;

    private final RealmModel realmModel;

    private final AdminEventBuilder adminEventBuilder;

    private final WorkflowHelper workflowHelper =  WorkflowHelper.getInstance();

    private static final Logger logger = Logger.getLogger(WorkflowsEventListenerResource.class);

    private static final String LOG_TAG = "WORKFLOWS::EVENT_LISTENER_RESOURCE: ";

    private static final String EVENT_REP_EMPTY = "Event representation is null or empty";

    //~ Constructor ========================================================================================================================

    public WorkflowsEventListenerResource(KeycloakSession session) {
        this.session = session;
        this.realmModel = session.getContext().getRealm();
        this.adminEventBuilder = new AdminEventBuilder(realmModel, new AuthDetails(), session, session.getContext().getConnection());
    }

    //~ Methods ============================================================================================================================

    public void triggerUserRoleMappingWorkflows(AdminEvent adminEvent) {
        try {
            String errorMessage = null;
            List<String> connectedAppsInWorkspace = JsonSerialization.readValue(realmModel.getAttribute(WORKFLOWS_CONNECTED_APPS), new TypeReference<List<String>>(){});
            List<RoleRepresentation> roles = JsonSerialization.readValue(adminEvent.getRepresentation(), new TypeReference<List<RoleRepresentation>>() {});
            String userId = adminEvent.getDetails().get(ASSIGNED_USER_KEY) != null ? adminEvent.getDetails().get(ASSIGNED_USER_KEY) : "";
            String groupId = adminEvent.getDetails().get(ASSIGNED_GROUP_KEY) != null ? adminEvent.getDetails().get(ASSIGNED_GROUP_KEY) : "";
            UserModel userModel = session.users().getUserById(realmModel, userId);
            if (userModel != null && roles != null && !roles.isEmpty()) {
                // This will happen when a user is assigned with a role (add role in users section or add user in roles section)
                roles.forEach(roleRepresentation -> processUserRoleMappingExecution(adminEvent, userModel, roleRepresentation, connectedAppsInWorkspace, false));
            } else if (!Validation.isBlank(groupId) && roles != null) {
                // The below logic is for adding multiple roles in groups against user
                GroupModel groupModel = realmModel.getGroupById(groupId);
                Set<UserModel> groupUserModelSet = session.users().getGroupMembersStream(realmModel, groupModel).collect(Collectors.toSet());
                if (groupUserModelSet.isEmpty()) return;
                // Prepare and collect role names for prefix matching to set into the userAttribute
                Map<String, List<String>> filteredAttributes = groupModel.getAttributes().entrySet().stream()
                        .filter(entry -> roles.stream().anyMatch(role -> entry.getKey().startsWith(role.getName())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                groupUserModelSet.forEach(user -> filteredAttributes.forEach(user::setAttribute));
                groupUserModelSet.forEach(user -> roles.forEach(roleRepresentation ->
                                processUserRoleMappingExecution(adminEvent, user, roleRepresentation, connectedAppsInWorkspace, true))
                );
            } else {
                errorMessage = EVENT_REP_EMPTY;
                logger.error(LOG_TAG + EVENT_REP_EMPTY);
            }
            if(groupId == null && userModel != null && errorMessage != null) {
                adminEventBuilder.operation(OperationType.USER).resource(ResourceType.WORKFLOW_ERROR)
                        .realm(realmModel)
                        .authUser(userModel)
                        .error(errorMessage)
                        .success();
            }
        } catch (IOException e) {
            logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
        }
    }

    //~ ====================================================================================================================================

    public void triggerUserDeletionToConnectedApps(AdminEvent adminEvent) {
        try {
            String errorMessage = null;
            List<String> connectedAppsInWorkspace = JsonSerialization.readValue(realmModel.getAttribute(WORKFLOWS_CONNECTED_APPS), new TypeReference<List<String>>(){});
            UserModel sessionAttributeUserModel = (UserModel) session.getAttribute("user");
            Stream<RoleModel> userAvailableRoleModels = sessionAttributeUserModel.getRealmRoleMappingsStream();
            Stream<RoleRepresentation> roleRepresentationStream = userAvailableRoleModels.map(ModelToRepresentation::toRepresentation);
            List<RoleRepresentation> roles = roleRepresentationStream.filter(role -> !role.isInBuilt()).collect(Collectors.toList());
            if (!roles.isEmpty()) {
                roles.forEach(roleRepresentation -> processUserRoleMappingExecution(adminEvent, sessionAttributeUserModel, roleRepresentation, connectedAppsInWorkspace, false));
            } else {
                errorMessage = "Deleted user role mappings are empty";
                logger.error(LOG_TAG + "Deleted user role mappings are empty");
            }
            if(errorMessage != null) {
                adminEventBuilder.operation(OperationType.USER).resource(ResourceType.WORKFLOW_TRIGGERED)
                        .realm(realmModel)
                        .authUser(sessionAttributeUserModel)
                        .error(errorMessage)
                        .success();
            }
        } catch (IOException e) {
            logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
        }
    }

    //~ ====================================================================================================================================

    public void triggerRoleActivityWorkflows(AdminEvent adminEvent) {
        try {
            String errorMessage = null;
            UserModel userModel = workflowHelper.getAccountCreatorByRealm(session, realmModel).orElse(null);
            RoleRepresentation role = JsonSerialization.readValue(adminEvent.getRepresentation(), RoleRepresentation.class);
            if (role != null) {
                String roleName = role.getName();
                String connectedApp = role.getAttributes().get(CONNECTED_APP) != null ? role.getAttributes().get(CONNECTED_APP).get(0) : null;
                if (roleName != null && connectedApp != null) {
                    switch (connectedApp.toLowerCase(Locale.ROOT)) {
                        case GITLAB: // Roles activity not supported for gitlab
                            break;
                        case YOUTRACK:
                            new YoutrackHandler(session, realmModel, adminEvent, adminEventBuilder).invokeRoleWorkflowsApis(userModel, role, connectedApp);
                            break;
                        default: logger.warnv(LOG_TAG + "Unsupported connected app : ", connectedApp);
                    }
                } else {
                    errorMessage = "Role name or Connected App is null";
                    logger.info(LOG_TAG + "Role name or Connected App is null");
                }
            } else {
                errorMessage = EVENT_REP_EMPTY;
                logger.error(LOG_TAG + EVENT_REP_EMPTY);
            }
            if(errorMessage != null) {
                adminEventBuilder.operation(OperationType.WORKFLOW_ROLE).resource(ResourceType.WORKFLOW_ERROR)
                        .realm(realmModel)
                        .authUser(userModel)
                        .error(errorMessage)
                        .success();
            }
        } catch (IOException e) {
            logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
        }
    }

    //~ ====================================================================================================================================

    private void processUserRoleMappingExecution(AdminEvent adminEvent, UserModel userModel, RoleRepresentation role, List<String> connectedAppsInWorkspace, boolean fromGroup) {
        if (!role.isWorkflowRole()) {
            String roleName = role.getName();
            connectedAppsInWorkspace.stream()
                    .filter(connectedApp -> connectedApp != null && roleName.toLowerCase().contains(connectedApp.toLowerCase()))
                    .forEach(connectedApp -> invokeGenericUserWorkFlowsApis(adminEvent, userModel, connectedApp));
        } else {
            String connectedApp = role.getAttributes().get(CONNECTED_APP) != null ? role.getAttributes().get(CONNECTED_APP).get(0) : null;
            if(connectedApp != null)
                processCustomConnectedAppRoleExecution(adminEvent, userModel, role, connectedApp);
        }
    }

    //~ ====================================================================================================================================

    private void invokeGenericUserWorkFlowsApis(AdminEvent adminEvent, UserModel userModel, String connectedApp) {
        String eventType;
        switch (adminEvent.getOperationType()) {
            case CREATE:
                eventType = CREATE_USER;
                break;
            case DELETE:
                eventType = DELETE_USER;
                break;
            default:
                logger.warnv(LOG_TAG + "Unsupported adminEvent type received {0}", adminEvent.getOperationType());
                return;
        }
        List<WorkflowsEntity> workflowEntries = new WorkflowsResourceFunction(session).getAllWorkflowEntries(realmModel, connectedApp, eventType);
        if (workflowEntries == null || workflowEntries.isEmpty()) {
            logger.errorv(LOG_TAG + "Workflows entries are empty for {0}", eventType);
            return;
        }
        workflowEntries.stream().filter(WorkflowsEntity::isSubscribed)
            .forEach(workflowsEntity -> {
                String webhookUrl = workflowsEntity.getWebhookUrl();
                if (webhookUrl == null) {
                    logger.error(LOG_TAG + "Webhook URL is missing for : " + workflowsEntity.getConnectedApp());
                    return;
                }
                WorkflowBaseUserRequestModel userRequestModel = new WorkflowBaseUserRequestModel(userModel);
                logger.infov(LOG_TAG + "Triggered {0} \n {1}", eventType , JsonUtil.getAsJsonPrettyString(userRequestModel));
                HttpUtility httpUtility = new HttpUtility();
                try(Response response = httpUtility.callRequest(APPLICATION_JSON, POST, webhookUrl, JsonSerialization.writeValueAsString(userRequestModel), null))
                {
                    if (response.isSuccessful()) {
                        adminEventBuilder.operation(eventType.equals(CREATE_USER) ? OperationType.CREATE : OperationType.DELETE)
                                .resource(ResourceType.WORKFLOW_TRIGGERED)
                                .realm(realmModel)
                                .authUser(userModel)
                                .representation(new EventMessageData("Workflow for " + eventType + " is successfully triggered to " + connectedApp))
                                .success();
                    }
                } catch (IOException e) {
                    logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
                }
        });
    }

    //~ ====================================================================================================================================

    private void processCustomConnectedAppRoleExecution(AdminEvent adminEvent, UserModel userModel, RoleRepresentation role, String connectedApp) {
        switch (connectedApp.toLowerCase(Locale.ROOT)) {
            case GITLAB: // custom user Workflow role activity is currently supported for gitlab
                break;
            case YOUTRACK:
                new YoutrackHandler(session, realmModel, adminEvent, adminEventBuilder).invokeUserRoleMappingWorkflowsApis(userModel, role, connectedApp);
                break;
            default: break;
        }
    }
}

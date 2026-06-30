package com.qlikverify.extensions.admin.controllers.workflows.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.extensions.admin.controllers.workflows.jpa.WorkflowsEntity;
import com.qlikverify.extensions.admin.controllers.workflows.misc.WorkflowBaseUserRequestModel;
import com.qlikverify.extensions.admin.controllers.workflows.misc.models.EventMessageData;
import com.qlikverify.extensions.admin.controllers.workflows.misc.models.youtrack.YoutrackRoleRequestModel;
import com.qlikverify.extensions.admin.controllers.workflows.rest.WorkflowsResourceFunction;
import com.qlikverify.extensions.admin.misc.helpers.JsonUtil;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.validation.Validation;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.extensions.admin.controllers.workflows.misc.Constants.*;

public class YoutrackHandler {

    //~ Static fields/initializers =========================================================================================================

    private static final Logger logger = Logger.getLogger(YoutrackHandler.class);

    private static final String LOG_TAG = "WORKFLOWS::YOUTRACK:HANDLER: ";

    private static final String LOG_MESSAGE = "Triggered {0} for {1} with :: \n {2} ";

    private final KeycloakSession session;

    private final RealmModel realmModel;

    private final AdminEvent adminEvent;

    private final AdminEventBuilder adminEventBuilder;

    public static final String PROJECT_SUFFIX = "_projects";

    public static final String ORGANIZATION_SUFFIX = "_organizations";

    private static final String PERMISSIONS = "permissions";

    //~ Constructor ========================================================================================================================

    public YoutrackHandler(KeycloakSession session, RealmModel realmModel, AdminEvent adminEvent, AdminEventBuilder adminEventBuilder) {
        this.session = session;
        this.realmModel = realmModel;
        this.adminEvent = adminEvent;
        this.adminEventBuilder = adminEventBuilder;
    }

    //~ Methods ============================================================================================================================

    public void invokeUserRoleMappingWorkflowsApis(UserModel userModel, RoleRepresentation role, String connectedApp) {
        logger.info(LOG_TAG + "Entering invokeUserRoleWorkflowsApi()");
        String eventType;
        switch (adminEvent.getOperationType()) {
            case CREATE:
                eventType = CREATE_USER;
                break;
            case DELETE:
                if (adminEvent.getResourceType().equals(ResourceType.USER))
                    eventType = DELETE_USER;
                else
                    eventType = UNASSIGN_ROLE;
                break;
            default:
                logger.warnv(LOG_TAG + "Unsupported event type {0}", adminEvent.getOperationType());
                return;
        }
        List<WorkflowsEntity> workflowEntries = new WorkflowsResourceFunction(session).getAllWorkflowEntries(realmModel, connectedApp, eventType);
        if (workflowEntries == null) {
            logger.errorv(LOG_TAG + "Workflows entries are empty for eventType :: {0}", eventType);
            return;
        }
        workflowEntries.stream().filter(WorkflowsEntity::isSubscribed)
            .forEach(workflowsEntity -> {
                String webhookUrl = workflowsEntity.getWebhookUrl();
                if (Validation.isBlank(webhookUrl)) {
                    logger.error(LOG_TAG + "Webhook URL is missing for : " + workflowsEntity.getConnectedApp());
                    return;
                }
                HttpUtility httpUtility = new HttpUtility();
                switch (workflowsEntity.getEventType()) {
                    case CREATE_USER:
                        WorkflowBaseUserRequestModel assignRolesMetadata = new WorkflowBaseUserRequestModel(userModel);
                        assignRolesMetadata.setMetaData(YoutrackHandler.RequestBodyHelper.getRoleAccessFromMetadata(userModel, role.getName()));
                        invokeUserCreationWithAssignRoles(httpUtility, userModel, webhookUrl, connectedApp, assignRolesMetadata);
                        break;
                    case UNASSIGN_ROLE:
                        WorkflowBaseUserRequestModel unAssignRolesMetadata = new WorkflowBaseUserRequestModel(userModel);
                        unAssignRolesMetadata.setMetaData(YoutrackHandler.RequestBodyHelper.unAssignRoleFromUser(userModel, role.getName()));
                        invokeUnAssignRoleByUser(httpUtility, userModel, webhookUrl, connectedApp, role.getName(), unAssignRolesMetadata);
                        break;
                    case DELETE_USER:
                        WorkflowBaseUserRequestModel deleteUserRequestBody = new WorkflowBaseUserRequestModel(userModel);
                        invokeUserDeletion(httpUtility, userModel, webhookUrl, connectedApp, deleteUserRequestBody);
                        break;
                    default: logger.warnv(LOG_TAG + "Unsupported event type :: {0}", workflowsEntity.getEventType());
                }
            });
    }

    //~ ====================================================================================================================================

    public void invokeRoleWorkflowsApis(UserModel userModel, RoleRepresentation role, String connectedApp) {
        logger.info(LOG_TAG + "Entering invokeYoutrackRoleWorkflowsApis()");
        String eventType = null;
        switch (adminEvent.getOperationType()) {
            case CREATE:
                eventType = CREATE_ROLE;
                break;
            case UPDATE:
                eventType = UPDATE_ROLE;
                break;
            case DELETE:
                eventType = DELETE_ROLE;
                break;
            default:
                logger.warnv(LOG_TAG + "Unsupported event type received {0}", adminEvent.getOperationType());
        }
        List<WorkflowsEntity> workflowEntries = new WorkflowsResourceFunction(session).getAllWorkflowEntries(realmModel, connectedApp, eventType);
        if (workflowEntries == null) {
            logger.errorv(LOG_TAG + "Workflows entries are empty for {0}", eventType);
            return;
        }
        workflowEntries.stream().filter(WorkflowsEntity::isSubscribed)
            .forEach(workflowsEntity -> {
                String webhookUrl = workflowsEntity.getWebhookUrl();
                if (Validation.isBlank(webhookUrl)) {
                    logger.error(LOG_TAG + "Webhook URL is missing for : " + workflowsEntity.getConnectedApp());
                    return;
                }
                YoutrackRoleRequestModel roleRequestModel = getRoleRequestBody(role, connectedApp);
                HttpUtility httpUtility = new HttpUtility();
                switch (workflowsEntity.getEventType()) {
                    case CREATE_ROLE: invokeRoleCreation(httpUtility, userModel, webhookUrl, connectedApp, roleRequestModel); break;
                    case UPDATE_ROLE: invokeRoleUpdate(httpUtility, userModel, webhookUrl, connectedApp, roleRequestModel); break;
                    case DELETE_ROLE: invokeRoleDeletion(httpUtility, userModel, webhookUrl, connectedApp, role.getName()); break;
                    default: logger.warnv(LOG_TAG + "Unsupported event type received {0}", workflowsEntity.getEventType());
                }
            });
    }

    //~ ====================================================================================================================================

    private YoutrackRoleRequestModel getRoleRequestBody(RoleRepresentation roleRepresentation, String connectedApp) {
        YoutrackRoleRequestModel requestBody = null;
        List<String> connectedAppPermissions;
        String roleName = roleRepresentation.getName();
        if (connectedApp.toLowerCase(Locale.ROOT).equals(YOUTRACK)) {
            if(roleRepresentation.getAttributes() != null && roleRepresentation.getAttributes().containsKey(PERMISSIONS)) {
                connectedAppPermissions = Collections.singletonList(roleRepresentation.getAttributes().get(PERMISSIONS)).get(0);
                try {
                    requestBody = YoutrackHandler.RequestBodyHelper.getYoutrackRoleRequestBody(roleName, JsonSerialization.readValue(connectedAppPermissions.get(0), List.class));
                } catch (IOException e) {
                    logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
                }
            }
        } else {
            logger.errorv(LOG_TAG + "Unsupported connected app for role creation {0}", connectedApp);
        }
        return requestBody;
    }

    //~ ====================================================================================================================================
    /* Against Workspace Role action */

    private void invokeRoleCreation(HttpUtility httpUtility, UserModel userModel, String webhookUrl, String connectedApp, YoutrackRoleRequestModel roleRequestModel) {
        logger.infov(LOG_TAG + LOG_MESSAGE, CREATE_ROLE, connectedApp, JsonUtil.getAsJsonPrettyString(roleRequestModel));
        try(Response response = httpUtility.callRequest(APPLICATION_JSON, POST, webhookUrl, JsonSerialization.writeValueAsString(roleRequestModel), null)) {
            if (response.isSuccessful()) {
                adminEventBuilder.operation(OperationType.CREATE).resource(ResourceType.WORKFLOW_TRIGGERED)
                        .realm(realmModel)
                        .authUser(userModel)
                        .representation(new EventMessageData("Workflow for " + CREATE_ROLE + " is successfully triggered to " + connectedApp))
                        .success();
            }
        } catch (IOException e) {
            logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
        }
    }

    //~ ====================================================================================================================================

    private void invokeRoleUpdate(HttpUtility httpUtility, UserModel userModel, String webhookUrl, String connectedApp, YoutrackRoleRequestModel roleRequestModel) {
        List<Map<String, String>> permissionsList;
        try {
            permissionsList = JsonSerialization.readValue(JsonUtil.getAsJsonString(roleRequestModel.getPermissions()), new TypeReference<List<Map<String, String>>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Object> permissions = permissionsList.stream().map(x -> x.get("id")).collect(Collectors.toList());
        HashMap<String, Object> requestBody = new HashMap<>();
        requestBody.put("role", roleRequestModel.getName());
        requestBody.put(PERMISSIONS, permissions);

        logger.infov(LOG_TAG + LOG_MESSAGE, UPDATE_ROLE, connectedApp, JsonUtil.getAsJsonPrettyString(requestBody));
        try(Response response = httpUtility.callRequest(APPLICATION_JSON, POST, webhookUrl, JsonSerialization.writeValueAsString(requestBody), null)) {
            if (response.isSuccessful()) {
                adminEventBuilder.operation(OperationType.UPDATE).resource(ResourceType.WORKFLOW_TRIGGERED)
                        .realm(realmModel)
                        .authUser(userModel)
                        .representation(new EventMessageData("Workflow for " + UPDATE_ROLE + " is successfully triggered to " + connectedApp))
                        .success();
            }
        } catch (IOException e) {
            logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
        }
    }

    //~ ====================================================================================================================================

    private void invokeRoleDeletion(HttpUtility httpUtility, UserModel userModel, String webhookUrl, String connectedApp, String roleName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", roleName);

        logger.infov(LOG_TAG + LOG_MESSAGE, DELETE_ROLE, connectedApp, JsonUtil.getAsJsonPrettyString(requestBody));
        try(Response response = httpUtility.callRequest(APPLICATION_JSON, POST, webhookUrl, JsonSerialization.writeValueAsString(requestBody), null)) {
            if (response.isSuccessful()) {
                adminEventBuilder.operation(OperationType.DELETE).resource(ResourceType.WORKFLOW_TRIGGERED)
                        .realm(realmModel)
                        .authUser(userModel)
                        .representation(new EventMessageData("Workflow for " + DELETE_ROLE + " is successfully triggered to " + connectedApp))
                        .success();
            }
        } catch (IOException e) {
            logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
        }
    }

    //~ ====================================================================================================================================
    /* Against User Role action */

    private void invokeUserCreationWithAssignRoles(HttpUtility httpUtility, UserModel userModel, String webhookUrl, String connectedApp, WorkflowBaseUserRequestModel roleRequestModel) {
        logger.infov(LOG_TAG + LOG_MESSAGE, CREATE_USER, connectedApp, JsonUtil.getAsJsonPrettyString(roleRequestModel));
        try(Response response = httpUtility.callRequest(APPLICATION_JSON, POST, webhookUrl, JsonSerialization.writeValueAsString(roleRequestModel), null)) {
            if (response.isSuccessful()) {
                adminEventBuilder.operation(OperationType.CREATE).resource(ResourceType.WORKFLOW_TRIGGERED)
                        .realm(realmModel)
                        .authUser(userModel)
                        .representation(new EventMessageData("Workflow for " + CREATE_USER + " With assign roles is successfully triggered to " + connectedApp))
                        .success();
            }
        } catch (IOException e) {
            logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
        }
    }

    //~ ====================================================================================================================================

    private void invokeUnAssignRoleByUser(HttpUtility httpUtility, UserModel userModel, String webhookUrl, String connectedApp, String roleName, WorkflowBaseUserRequestModel unAssignRequestModel) {
        logger.infov(LOG_TAG + LOG_MESSAGE, UNASSIGN_ROLE, connectedApp, JsonUtil.getAsJsonPrettyString(unAssignRequestModel));
        try(Response response = httpUtility.callRequest(APPLICATION_JSON, POST, webhookUrl, JsonSerialization.writeValueAsString(unAssignRequestModel), null)) {
            if(response.isSuccessful()) {
                userModel.getAttributes().forEach((key, values) -> {
                    if (key.startsWith(roleName)) userModel.removeAttribute(key);
                });
                adminEventBuilder.operation(OperationType.UPDATE).resource(ResourceType.WORKFLOW_TRIGGERED)
                        .realm(realmModel)
                        .authUser(userModel)
                        .representation(new EventMessageData("Workflow for " + UNASSIGN_ROLE + " is successfully triggered to " + connectedApp))
                        .success();
            }
        } catch (IOException e) {
            logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
        }
    }

    //~ ====================================================================================================================================

    private void invokeUserDeletion(HttpUtility httpUtility, UserModel userModel, String webhookUrl, String connectedApp, WorkflowBaseUserRequestModel unAssignRequestModel) {
        logger.infov(LOG_TAG + LOG_MESSAGE, DELETE_USER, connectedApp, JsonUtil.getAsJsonPrettyString(unAssignRequestModel));
        try(Response response = httpUtility.callRequest(APPLICATION_JSON, POST, webhookUrl, JsonSerialization.writeValueAsString(unAssignRequestModel), null)) {
            if (response.isSuccessful()) {
                adminEventBuilder.operation(OperationType.DELETE).resource(ResourceType.WORKFLOW_TRIGGERED)
                        .realm(realmModel)
                        .authUser(userModel)
                        .representation(new EventMessageData("Workflow for " + DELETE_USER + " is successfully triggered to " + connectedApp))
                        .success();
            }
        } catch (IOException e) {
            logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
        }
    }

    //~ Inner Classes ======================================================================================================================

    public static class RequestBodyHelper {

        private RequestBodyHelper() {}

        public static YoutrackRoleRequestModel getYoutrackRoleRequestBody(String roleName, List<Object> permissions) {
            YoutrackRoleRequestModel roleRequestModel = new YoutrackRoleRequestModel();
            roleRequestModel.setName(roleName);
            roleRequestModel.setDescription("Workflow-role created from ezto auth");
            roleRequestModel.setPermissions(permissions);
            return roleRequestModel;
        }

        //~ ====================================================================================================================================

        public static Map<String, Object> getRoleAccessFromMetadata(UserModel userModel, String roleName) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("role", roleName);
            try {
                List<String> projects = getAttributeFromUserModel(userModel, roleName + PROJECT_SUFFIX);
                List<String> organizations = getAttributeFromUserModel(userModel, roleName + ORGANIZATION_SUFFIX);

                metadata.put("projects", projects);
                metadata.put("organizations", organizations);

                if (projects.isEmpty() && organizations.isEmpty()) {
                    return Collections.emptyMap();
                }
            } catch (IOException e) {
                logger.error(LOG_TAG + FAILED_TO_PARSE_JSON);
            }
            return metadata;
        }

        //~ ====================================================================================================================================

        private static List<String> getAttributeFromUserModel(UserModel userModel, String attributeName) throws IOException {
            List<String> attributeIds = new ArrayList<>();
            String attributeValue = userModel.getFirstAttribute(attributeName);

            if (attributeValue != null) {
                List<Map<String, String>> attributeValues = JsonSerialization.readValue(attributeValue, new TypeReference<List<Map<String, String>>>() {});
                for (Map<String, String> value : attributeValues) {
                    if (value.containsKey("id")) {
                        attributeIds.add(value.get("id"));
                    }
                }
            }
            return attributeIds;
        }

        //~ ====================================================================================================================================

        public static Map<String, Object> unAssignRoleFromUser(UserModel userModel, String roleName) {
            Map<String, Object> unAssignRoleMetaData = new HashMap<>();
            List<String> projectRoles = new ArrayList<>();
            List<String> organizationRoles = new ArrayList<>();

            userModel.getAttributes().forEach((key, value) -> {
                if (key.equalsIgnoreCase(roleName + PROJECT_SUFFIX)) {
                    projectRoles.add(roleName);
                } else if (key.equalsIgnoreCase(roleName + ORGANIZATION_SUFFIX)) {
                    organizationRoles.add(roleName);
                }
            });

            unAssignRoleMetaData.put("projectRoles", projectRoles);
            unAssignRoleMetaData.put("organizationRoles", organizationRoles);

            return unAssignRoleMetaData;
        }
    }
}

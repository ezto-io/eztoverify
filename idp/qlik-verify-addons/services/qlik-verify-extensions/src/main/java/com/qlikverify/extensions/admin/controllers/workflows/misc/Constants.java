package com.qlikverify.extensions.admin.controllers.workflows.misc;

public class Constants {

    private Constants(){}

    // General
    public static final String WORKFLOWS = "WORKFLOWS";
    public static final String WORKFLOWS_REQUEST = "WORKFLOWS_REQUEST";
    public static final String WORKFLOWS_URL = "WORKFLOWS_URL";
    public static final String ASSIGNED_USER_KEY = "assigned_user";
    public static final String ASSIGNED_GROUP_KEY = "assigned_group";
    public static final String WORKFLOWS_SUBSCRIBED = "enabled";
    public static final String CONFIGURED_WORKFLOWS_PROVIDERS = "configuredWorkflowsProviders";
    public static final String WORKFLOWS_CONFIGURED = "workflowsConfigured";

    public static final String APPLICATION_JSON = "application/json";

    public static final String POST = "POST";

    public static final String REALM_ID = "realmId";
    public static final String FLOW_ID = "flowId";
    public static final String FLOW_NAME = "flowName";
    public static final String ICON_URL = "iconUrl";
    public static final String WEBHOOK_URL = "webhookUrl";
    public static final String ATTRIBUTES = "attributes";
    public static final String WORKFLOWS_CONNECTED_APPS = "workflowsConnectedApps";
    public static final String CONNECTED_APP = "connectedApp";
    public static final String METADATA = "metaData";
    public static final String WORK_FLOWS_ENTITY_NOT_FOUND = "WorkFlowsEntity not found";
    public static final String EVENT_TYPE = "eventType";
    public static final String ACCOUNT_CREATOR = "ACCOUNT_CREATOR";

    // Error Messages
    public static final String FAILED_TO_PARSE_JSON = "Failed to parse JSON";

    // Event Types
    public static final String CREATE_USER = "CREATE_USER";
    public static final String DELETE_USER = "DELETE_USER";
    public static final String CREATE_ROLE = "CREATE_ROLE";
    public static final String UPDATE_ROLE = "UPDATE_ROLE";
    public static final String DELETE_ROLE = "DELETE_ROLE";
    public static final String UNASSIGN_ROLE = "UNASSIGN_ROLE";

    // Connected App
    public static final String YOUTRACK = "youtrack";
    public static final String GITLAB = "gitlab";
}

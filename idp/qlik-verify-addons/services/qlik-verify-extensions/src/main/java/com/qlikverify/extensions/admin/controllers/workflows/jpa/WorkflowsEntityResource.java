package com.qlikverify.extensions.admin.controllers.workflows.jpa;

import com.qlikverify.extensions.admin.controllers.workflows.misc.WorkflowsFlowModel;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public interface WorkflowsEntityResource {

    String getConnectedApplicationsMetadata(String type);
    Response addNewWorkflowEntry(WorkflowsFlowModel workflowsFlowModel);
    List<WorkflowsEntity> getAllWorkflowEntries(RealmModel realm);
    List<WorkflowsEntity> getAllWorkflowEntries(RealmModel realm, String connectedApp, String eventType);
    Response updateWorkflowEntry(RealmModel realm, Map<String, Object> data);
    Response deleteWorkflowEntry(WorkflowsFlowModel workflowsFlowModel);
    String getWorkflowRoleDetails(String flowName);
    Response postWorkflowRoleDetails(Map<String, Object> roleDetails, String userId);
}

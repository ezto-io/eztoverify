package com.qlikverify.extensions.admin.controllers.workflows.rest;

import com.qlikverify.extensions.admin.controllers.workflows.jpa.WorkflowsEntityResource;
import com.qlikverify.extensions.admin.controllers.workflows.misc.WorkflowsFlowModel;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.validation.Validation;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowsResource implements RealmResourceProvider {

    //~ Static fields/initializers =========================================================================================================

    private final KeycloakSession session;
    private final WorkflowsEntityResource workflowsEntityResource;

    //~ Constructor ========================================================================================================================

    public WorkflowsResource(KeycloakSession session) {
        this.session = session;
        this.session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();
        this.workflowsEntityResource = new WorkflowsResourceFunction(session);
    }

    //~ Methods ============================================================================================================================

    @Override
    public Object getResource() {
        return this;
    }

    //~ ====================================================================================================================================

    @Override
    public void close() {
        // No implementation required
    }

    //~ API ================================================================================================================================

    @GET
    @Path("/connectedAppMetadata")
    @NoCache
    public Response getConnectedApp(@QueryParam("type") String type) {
        if (Validation.isBlank(type))
            Response.status(Response.Status.PRECONDITION_FAILED).entity("Query Param cannot be null").build();
        return Response.ok().entity(workflowsEntityResource.getConnectedApplicationsMetadata(type)).build();
    }

    //~ ====================================================================================================================================

    @GET
    @Path("/workflowRoles/{flowName}")
    @NoCache
    public String getWorkflowRoles(@PathParam("flowName") String flowName) {
        return workflowsEntityResource.getWorkflowRoleDetails(flowName);
    }

    //~ ====================================================================================================================================

    @POST
    @Path("/workflowRoles/{userId}")
    @NoCache
    public Response postWorkflowRoles(@PathParam("userId") String userId, Map<String, Object> roleDetails) {
        return workflowsEntityResource.postWorkflowRoleDetails(roleDetails, userId);
    }

    //~ ====================================================================================================================================

    @POST
    @NoCache
    public Response postWorkflowData(@Valid final WorkflowsFlowModel workflowsFlowModel) {
        if (workflowsFlowModel == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Request body should not be null").build();
        }
        return workflowsEntityResource.addNewWorkflowEntry(workflowsFlowModel);
    }

    //~ ====================================================================================================================================

    @DELETE
    @NoCache
    public Response deleteWorkflowData(final WorkflowsFlowModel workflowsFlowModel) {
        if (workflowsFlowModel == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Request body should not be null").build();
        }
        return workflowsEntityResource.deleteWorkflowEntry(workflowsFlowModel);
    }

    //~ ====================================================================================================================================

    @DELETE
    @Path("/remove")
    @NoCache
    public Response deleteStreamsClientFromAllWorkspace() {
            session.realms().getRealmsStream()
                    .filter(realm -> !realm.getMode().equals("bo"))
                    .forEach(realm -> {
                        List<String> clientIdsToRemove = realm.getClientsStream()
                                .filter(client -> client.getName().equals(realm.getName()))
                                .map(ClientModel::getId)
                                .collect(Collectors.toList());
                        clientIdsToRemove.forEach(realm::removeClient);
                    });
        return Response.ok().entity("Remove Streams Client From All workspace").build();
    }
}

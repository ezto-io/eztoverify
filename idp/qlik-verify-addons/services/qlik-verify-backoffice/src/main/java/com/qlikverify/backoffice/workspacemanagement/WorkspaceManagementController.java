package com.qlikverify.backoffice.workspacemanagement;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public class WorkspaceManagementController {
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private static final Logger logger = Logger.getLogger(WorkspaceManagementController.class);
    private final WorkspaceManagementResource workspaceManagementResource;

    public WorkspaceManagementController(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        workspaceManagementResource = new WorkspaceManagementResource();
    }

    @Path("/{realmName}/details")
    @GET
    public Response getWorkspaceDetails(@PathParam("realmName") String realmName){
        logger.info("WorkspaceManagementController:: /{realmName}/details endpoint");
        return workspaceManagementResource.getWorkspaceDetails(session, realmName);
    }

    @Path("/{realmName}/{tenantTypeToUpdate}")
    @POST
    public Response updateTenantType(
            @PathParam("realmName") String realmName,
            @PathParam("tenantTypeToUpdate") String tenantTypeToUpdate,
            @QueryParam("value") boolean value
    ){
        logger.info("WorkspaceManagementController:: /{realmName}/{tenantTypeToUpdate} endpoint");
        return workspaceManagementResource.updateTenantType(session, realm, realmName, tenantTypeToUpdate, value, adminEvent, auth.adminAuth().getUser().getEmail());
    }
}
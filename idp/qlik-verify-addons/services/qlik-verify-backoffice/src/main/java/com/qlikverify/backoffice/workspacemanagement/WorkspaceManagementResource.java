package com.qlikverify.backoffice.workspacemanagement;

import org.jboss.logging.Logger;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.qlikverify.backoffice.Constants.*;

public class WorkspaceManagementResource {
    private static final Logger logger = Logger.getLogger(WorkspaceManagementResource.class);

    public Response getWorkspaceDetails(KeycloakSession session, String realmName){
        logger.info("WorkspaceManagementResource:: getWorkspaceDetails");
        RealmModel realm = session.realms().getRealmByName(realmName);
        if(realm == null) {
            logger.error(WORKSPACE_NOT_FOUND);
            return Response.status(404).entity(new HashMap<>() {{put("error", "workspace not found");}}).build();
        }

        String logoUrl = realm.getRealmLogo() == null ?
                EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_API_URL) + "/public/logo/eztovrfy-logo.svg" :
                realm.getRealmLogo();

        String balance = "99999.0";

        Map<String, Object> workspaceDetails = new LinkedHashMap<>();
        workspaceDetails.put("workspaceName", realm.getName());
        workspaceDetails.put("workspaceId", realm.getId());
        workspaceDetails.put("balance", balance);
        workspaceDetails.put("logo", logoUrl);
        workspaceDetails.put("displayName", realm.getDisplayName());
        workspaceDetails.put("attributes", realm.getAttributes());
        workspaceDetails.put(IS_API_WORKSPACE, Boolean.parseBoolean(realm.getAttribute(IS_API_WORKSPACE)));
        workspaceDetails.put(IS_PLATFORM_WORKSPACE, Boolean.parseBoolean(realm.getAttribute(IS_PLATFORM_WORKSPACE)));
        if(!Config.getAdminRealm().equalsIgnoreCase(realmName)) {
            workspaceDetails.put("isLive", true);
        } else {
            workspaceDetails.put("isLive", false);
        }
        return Response.ok().entity(workspaceDetails).build();
    }

    public Response updateTenantType(KeycloakSession session, RealmModel adminRealm, String realmName, String tenantTypeToUpdate, boolean value,
                                     AdminEventBuilder adminEvent, String adminEmail){
        logger.info("WorkspaceManagementResource:: updateTenantType");
        RealmModel realm = session.realms().getRealmByName(realmName);
        if(realm == null) {
            logger.error(WORKSPACE_NOT_FOUND);
            return Response.status(404).entity(new HashMap<>() {{put("error", "workspace not found");}}).build();
        }
        if(org.keycloak.Config.getAdminRealm().equals(realmName)){
            realm.setAttribute(IS_PLATFORM_WORKSPACE, true);
            realm.setAttribute(IS_API_WORKSPACE, true);
            return Response.ok().entity(new HashMap<>(){{put("message", "Cant update master workspace details");}}).build();
        }
        String apiWorkspace = realm.getAttribute(IS_API_WORKSPACE);
        String platformWorkspace = realm.getAttribute(IS_PLATFORM_WORKSPACE);
        if((IS_API_WORKSPACE.equals(tenantTypeToUpdate) && !value && !Boolean.parseBoolean(platformWorkspace)) ||
                (IS_PLATFORM_WORKSPACE.equals(tenantTypeToUpdate) && !value && !Boolean.parseBoolean(apiWorkspace))){
            return Response.status(409).entity(new HashMap<>(){{put("message", "Both options cannot be disabled, please ensure either one is enabled");}}).build();
        }
        if(IS_API_WORKSPACE.equals(tenantTypeToUpdate) || IS_PLATFORM_WORKSPACE.equals(tenantTypeToUpdate)) {
            adminEvent
                    .operation(OperationType.UPDATE)
                    .resource(ResourceType.WORKSPACE)
                    .realm(adminRealm)
                    .detail("tenantTypeToUpdate", String.valueOf(value))
                    .detail("user", adminEmail)
                    .detail("Sub workspace", realmName)
                    .success();
            realm.setAttribute(tenantTypeToUpdate, String.valueOf(value));
            return Response.ok().entity(new HashMap<>(){{put("message", "updated successfully");}}).build();
        }
        return Response.status(400).entity(new HashMap<>(){{put("error", "invalid details");}}).build();
    }
}

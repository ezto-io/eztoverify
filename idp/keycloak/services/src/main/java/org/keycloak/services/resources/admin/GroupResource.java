/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources.admin;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import javax.ws.rs.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authentication.authenticators.utils.GeneralHelper;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.AssignUsersRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @resource Groups
 * @author Bill Burke
 */
public class GroupResource {
    private static final Logger logger = Logger.getLogger(GroupResource.class);

    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final GroupModel group;

    public GroupResource(RealmModel realm, GroupModel group, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.GROUP);
        this.group = group;
    }

     /**
     *
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GroupRepresentation getGroup() {
        this.auth.groups().requireView(group);

        GroupRepresentation rep = ModelToRepresentation.toGroupHierarchy(group, true);
        List<String> defGrpIds = realm.getDefaultGroupsStream()
                                    .map( g -> g.getId())
                                    .collect(Collectors.toList());
        rep.setDefaultGroup(false);
        if(defGrpIds!= null && !defGrpIds.isEmpty() && defGrpIds.contains(group.getId())){
            rep.setDefaultGroup(true);
        }
        rep.setAccess(auth.groups().getAccess(group));

        return rep;
    }

    /**
     * Update group, ignores subgroups.
     *
     * @param rep
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateGroup(GroupRepresentation rep) {
        this.auth.groups().requireManage(group);

        String groupName = rep.getName();
        if (ObjectUtil.isBlank(groupName)) {
            return ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
        }

        if (!Objects.equals(groupName, group.getName())) {
            boolean exists = siblings().filter(s -> !Objects.equals(s.getId(), group.getId()))
                    .anyMatch(s -> Objects.equals(s.getName(), groupName));
            if (exists) {
                return ErrorResponse.exists("Sibling group named '" + groupName + "' already exists.");
            }
        }
        updateGroup(rep, group);

        if(rep.isDefaultGroup())
            realm.addDefaultGroup(group);

        if(!rep.isDefaultGroup())
            realm.removeDefaultGroup(group);

        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
        return Response.noContent().build();
    }
    
    private Stream<GroupModel> siblings() {
        if (group.getParentId() == null) {
            return realm.getTopLevelGroupsStream();
        } else {
            return group.getParent().getSubGroupsStream();
        }
    }

    @DELETE
    public void deleteGroup() {
        this.auth.groups().requireManage(group);

        List<UserModel> users = session.users().getGroupMembersStream(realm, group, -1, -1).collect(Collectors.toList());
        List<String> email = new ArrayList<>();
        users.forEach(user -> {
            email.add(user.getEmail());
            if(user.getAttributes().containsKey("roleViaGroup")){
                user.removeAttribute("roleViaGroup");
            }
        });

        session.setAttribute("groupUsers", String.join(",", email));
        realm.removeGroup(group);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.GROUP_MEMBERSHIP).
                representation(ModelToRepresentation.toRepresentation(group, true)).
                resourcePath(session.getContext().getUri()).success();
    }


    @POST
    @Path("assign-users")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUsersToGroup(AssignUsersRepresentation rep){
        this.auth.realm().canManageRealm();

        List<String> userIds = rep.getUserIds();
        if(userIds.isEmpty()) {
            return ErrorResponse.error("Users List is Empty", Response.Status.BAD_REQUEST);
        }

        if(userIds.size() > 10) {
            return ErrorResponse.error("Only 10 members added at one time", Response.Status.BAD_REQUEST);
        }

        userIds.forEach(uId -> {
            UserModel user = session.users().getUserById(realm,uId);
            if(user == null) {
                logger.error("User not found with this id " + uId + " in the realm " + realm.getName());
            } else {
                if (!user.isMemberOf(group)) {
                    user.joinGroup(group);
                    List<RoleModel> roleModelList = group.getRealmRoleMappingsStream().collect(Collectors.toList());
                    roleModelList.forEach(role -> {
                        user.setSingleAttribute(role.getName() + "_organizations", group.getFirstAttribute(role.getName() + "_organizations"));
                        user.setSingleAttribute(role.getName() + "_projects", group.getFirstAttribute(role.getName() + "_projects"));
                    });
                    triggerNotification(OperationType.CREATE, ResourceType.WORKSPACE_ROLE_MAPPING, null, user, roleModelList);
                    triggerNotification(OperationType.CREATE, ResourceType.GROUP_MEMBERSHIP, group, user, null);
                }
            }
        });
        return Response.noContent().build();
    }

    private void triggerNotification(OperationType opType, ResourceType rType,GroupModel group , UserModel user, List<RoleModel> roleModelList){
        AdminEventBuilder adminEventBuilder = new AdminEventBuilder(realm, new AuthDetails(), session, session.getContext().getConnection());
        adminEventBuilder.operation(opType)
                .resource(rType)
                .resourcePath(session.getContext().getUri())
                .representation(group != null ? ModelToRepresentation.toRepresentation(group, true) : roleModelList.stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList()))
                .detail("assigned_user", user.getId())
                .authUser(user)
                .success();
    }


    /**
     * Set or create child.  This will just set the parent if it exists.  Create it and set the parent
     * if the group doesn't exist.
     *
     * @param rep
     */
    @POST
    @Path("children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addChild(GroupRepresentation rep) {
        this.auth.groups().requireManage(group);

        String groupName = rep.getName();
        if (ObjectUtil.isBlank(groupName)) {
            return ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
        }

        Response.ResponseBuilder builder = Response.status(204);
        GroupModel child = null;
        if (rep.getId() != null) {
            child = realm.getGroupById(rep.getId());
            if (child == null) {
                throw new NotFoundException("Could not find child by id");
            }
            realm.moveGroup(child, group);
            adminEvent.operation(OperationType.UPDATE);
        } else {
            try{
                child = realm.createGroup(groupName, group);
                updateGroup(rep, child);
                URI uri = session.getContext().getUri().getBaseUriBuilder()
                                            .path(session.getContext().getUri().getMatchedURIs().get(2))
                                            .path(child.getId()).build();
                builder.status(201).location(uri);
                rep.setId(child.getId());
                adminEvent.operation(OperationType.CREATE);
            } catch (ModelDuplicateException mde) {
                return ErrorResponse.exists("Group with name '" + rep.getName() + "' already exists in same level.");
            }
        }
        adminEvent.resourcePath(session.getContext().getUri()).representation(rep).success();

        GroupRepresentation childRep = ModelToRepresentation.toGroupHierarchy(child, true);
        return builder.type(MediaType.APPLICATION_JSON_TYPE).entity(childRep).build();
    }

    public static void updateGroup(GroupRepresentation rep, GroupModel model) {
        if (rep.getName() != null) model.setName(rep.getName());

        if (rep.getAttributes() != null) {
            Set<String> attrsToRemove = new HashSet<>(model.getAttributes().keySet());
            attrsToRemove.removeAll(rep.getAttributes().keySet());
            for (Map.Entry<String, List<String>> attr : rep.getAttributes().entrySet()) {
                model.setAttribute(attr.getKey(), attr.getValue());
            }

            for (String attr : attrsToRemove) {
                model.removeAttribute(attr);
            }
        }
    }

    @Path("role-mappings")
    public RoleMapperResource getRoleMappings() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.groups().requireManage(group);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.groups().requireView(group);
        RoleMapperResource resource =  new RoleMapperResource(realm, auth, group, adminEvent, manageCheck, viewCheck);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;

    }

    /**
     * Get users
     *
     * Returns a stream of users, filtered according to query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @param briefRepresentation Only return basic information (only guaranteed to return id, username, created, first and last name,
     *  email, enabled state, email verification state, federation link, and access.
     *  Note that it means that namely user attributes, required actions, and not before are not returned.)
     * @return a non-null {@code Stream} of users
     */
    @GET
    @NoCache
    @Path("members")
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserRepresentation> getMembers(@QueryParam("first") Integer firstResult,
                                               @QueryParam("max") Integer maxResults,
                                               @QueryParam("briefRepresentation") Boolean briefRepresentation) {
        this.auth.groups().requireViewMembers(group);
        
        firstResult = firstResult != null ? firstResult : 0;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;
        boolean briefRepresentationB = briefRepresentation != null && briefRepresentation;
        return session.users().getGroupMembersStream(realm, group, firstResult, maxResults)
                .map(user -> briefRepresentationB
                        ? ModelToRepresentation.toBriefRepresentation(user)
                        : ModelToRepresentation.toRepresentation(session, realm, user));
    }

    @GET
    @NoCache
    @Path("members/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> getMembersCount() {
        this.auth.groups().requireViewMembers(group);

        Set<String> grpSet = new HashSet<>();
        grpSet.add(group.getId());

        Map<String, Integer> map = new HashMap<>();
        map.put("count", session.users().getUsersCount(realm,grpSet));
        return map;
    }

    @POST
    @NoCache
    @Path("send-group-invite")
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse sendGroupInvites() {
        this.auth.groups().requireViewMembers(group);

        if((session.users().getGroupMembersStream(realm, group, -1, -1).count() == 0)){
            return new GeneralResponse() {{
                setStatus(400);
                setBody(new HashMap<String,String>() {{
                    put("message", "no-memebers-available");
                }});
            }};
        }

        if (GeneralHelper.isEmailConfigured(session,realm)) {
            return new GeneralResponse() {{
                setStatus(400);
                setBody(new HashMap<String,String>() {{
                    put("message", "Email Provider is not Configured. Please check your plan");
                }});
            }};
        }


        group.setSingleAttribute("hidden_ga_grp_invite_started", "true");
        HashMap<String,String> dataToBeSent = new HashMap<>();
        dataToBeSent.put("grpId",   group.getId());
        adminEvent.operation(OperationType.TRIGGER).resource(ResourceType.SEND_GROUP_INVITE)
                .realm(realm).authUser(auth.adminAuth().getUser()).representation(dataToBeSent).success();

        return new GeneralResponse() {{
            setStatus(200);
            setBody(new HashMap<String,String>() {{
                put("message", "invite-sent-to-all-users");
            }});
        }};

    }

    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     * @return
     */
    @Path("management/permissions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ManagementPermissionReference getManagementPermissions() {
        auth.groups().requireView(group);

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (!permissions.groups().isPermissionsEnabled(group)) {
            return new ManagementPermissionReference();
        }
        return toMgmtRef(group, permissions);
    }

    public static ManagementPermissionReference toMgmtRef(GroupModel group, AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.groups().resource(group).getId());
        ref.setScopePermissions(permissions.groups().getPermissions(group));
        return ref;
    }


    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     *
     * @return initialized manage permissions reference
     */
    @Path("management/permissions")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public ManagementPermissionReference setManagementPermissionsEnabled(ManagementPermissionReference ref) {
        auth.groups().requireManage(group);
        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.groups().setPermissionsEnabled(group, ref.isEnabled());
        if (ref.isEnabled()) {
            return toMgmtRef(group, permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }

}


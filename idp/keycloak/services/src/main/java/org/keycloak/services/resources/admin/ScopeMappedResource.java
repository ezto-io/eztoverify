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

import org.apache.http.util.TextUtils;
import org.jboss.resteasy.annotations.cache.NoCache;
import javax.ws.rs.NotFoundException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.util.ScopeMappedUtil;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for managing the scope mappings of a specific client.
 *
 * @resource Scope Mappings
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeMappedResource {
    protected RealmModel realm;
    protected AdminPermissionEvaluator auth;
    protected AdminPermissionEvaluator.RequirePermissionCheck managePermission;
    protected AdminPermissionEvaluator.RequirePermissionCheck viewPermission;

    protected ScopeContainerModel scopeContainer;
    protected KeycloakSession session;
    protected AdminEventBuilder adminEvent;

    public ScopeMappedResource(RealmModel realm, AdminPermissionEvaluator auth, ScopeContainerModel scopeContainer,
                               KeycloakSession session, AdminEventBuilder adminEvent,
                               AdminPermissionEvaluator.RequirePermissionCheck managePermission,
                               AdminPermissionEvaluator.RequirePermissionCheck viewPermission) {
        this.realm = realm;
        this.auth = auth;
        this.scopeContainer = scopeContainer;
        this.session = session;
        this.adminEvent = adminEvent.resource(ResourceType.WORKSPACE_SCOPE_MAPPING);
        this.managePermission = managePermission;
        this.viewPermission = viewPermission;
    }

    /**
     * Get all scope mappings for the client
     *
     * @return
     * @deprecated the method is not used neither from admin console or from admin client. It may be removed in future releases.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Deprecated
    public MappingsRepresentation getScopeMappings() {
        viewPermission.require();

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        MappingsRepresentation all = new MappingsRepresentation();
        List<RoleRepresentation> realmRep = scopeContainer.getRealmScopeMappingsStream()
                .map(ModelToRepresentation::toBriefRepresentation)
                .collect(Collectors.toList());
        if (!realmRep.isEmpty()) {
            all.setRealmMappings(realmRep);
        }

        Stream<ClientModel> clients = realm.getClientsStream();
        Map<String, ClientMappingsRepresentation> clientMappings = clients
                .map(c -> ScopeMappedUtil.toClientMappingsRepresentation(c, scopeContainer))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ClientMappingsRepresentation::getClient, Function.identity()));

        if (!clientMappings.isEmpty()) {
            all.setClientMappings(clientMappings);
        }
        return all;
    }

    /**
     * Get realm-level roles associated with the client's scope
     *
     * @return
     */
    @Path("realm")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<RoleRepresentation> getRealmScopeMappings(
        @QueryParam("search") String search,
        @QueryParam("first") Integer firstResult,
        @QueryParam("max") Integer maxResults) {
        viewPermission.require();
        firstResult = firstResult == null || firstResult < 0 ? 0 : firstResult;
        maxResults = maxResults == null || maxResults < 0 ? Constants.DEFAULT_MAX_RESULTS : maxResults;

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        Stream<RoleModel> availableRoleModels = getAvailableScopes(search, firstResult, maxResults, scopeContainer.getRealmScopeMappingsStream());

        return availableRoleModels != null ? availableRoleModels.map(ModelToRepresentation::toBriefRepresentation) : Stream.empty();
    }

    private Stream<RoleModel> getAvailableScopes(String search, Integer firstResult, Integer maxResults, Stream<RoleModel> roleModelStream) {
        Stream<RoleModel> filteredRoleModels = StringUtil.isNotBlank(search) ?
                roleModelStream.filter(x -> x.getName().toLowerCase().contains(search.toLowerCase())) :
                roleModelStream;

        return filteredRoleModels.skip(firstResult).limit(maxResults);
    }

    @Path("realm/count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public HashMap<String, Long> getRealmScopeMappingsCount(
            @QueryParam("search") String search){
        viewPermission.require();
        Long searchResults = search == null ?
                scopeContainer.getRealmScopeMappingsStream().count() :
                scopeContainer.getRealmScopeMappingsStream().filter(x -> x.getName().toLowerCase().contains(search.toLowerCase())).count();

        HashMap<String, Long> scopeMappingsCount = new HashMap<>();
        scopeMappingsCount.put("count",searchResults);

        return scopeMappingsCount;
    }


    /**
     * Get realm-level roles that are available to attach to this client's scope
     *
     * @return
     */
    @Path("realm/available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<RoleRepresentation> getAvailableRealmScopeMappings(
        @QueryParam("search") String search,
        @QueryParam("first") Integer firstResult,
        @QueryParam("max") Integer maxResults) {
            viewPermission.require();
            firstResult = firstResult == null || firstResult < 0 ? 0 : firstResult;
            maxResults = maxResults == null || maxResults < 0 ? Constants.DEFAULT_MAX_RESULTS : maxResults;
            Stream<RoleModel> roleModelStream = realm.getRolesStream()
                .filter(((Predicate<RoleModel>) scopeContainer::hasDirectScope).negate())
                    .filter(auth.roles()::canMapClientScope);

            Stream<RoleModel> availableRoleModels = getAvailableRealmScopes(search, firstResult, maxResults, roleModelStream);

            return availableRoleModels != null ? availableRoleModels.map(ModelToRepresentation::toBriefRepresentation) : Stream.empty();
        }

        private Stream<RoleModel> getAvailableRealmScopes(String search, Integer firstResult, Integer maxResults, Stream<RoleModel> roleModelStream) {
            Stream<RoleModel> filteredRoleModels = StringUtil.isNotBlank(search) ?
                    roleModelStream.filter(x -> x.getName().toLowerCase().contains(search.toLowerCase())) :
                    roleModelStream;

            return filteredRoleModels.skip(firstResult).limit(maxResults);
        }

    @Path("realm/available/count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public HashMap<String, Long> getAvailableRealmScopeMappingsCount(
            @QueryParam("search") String search){
        viewPermission.require();
        Stream<RoleModel> roleModelStream = realm.getRolesStream()
                .filter(((Predicate<RoleModel>) scopeContainer::hasDirectScope).negate());
        Long searchResults = search != null && !TextUtils.isEmpty(search) ?
                roleModelStream.filter(x -> x.getName().toLowerCase().contains(search.toLowerCase())).map(ModelToRepresentation::toBriefRepresentation).count() :
                roleModelStream.map(ModelToRepresentation::toBriefRepresentation).count();

        HashMap<String, Long> scopeMappingsCount = new HashMap<>();
        scopeMappingsCount.put("count",searchResults);

        return scopeMappingsCount;
    }

    /**
     * Get effective realm-level roles associated with the client's scope
     *
     * What this does is recurse
     * any composite roles associated with the client's scope and adds the roles to this lists.  The method is really
     * to show a comprehensive total view of realm-level roles associated with the client.
     *
     * @param briefRepresentation if false, return roles with their attributes
     * 
     * @return
     */
    @Path("realm/composite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<RoleRepresentation> getCompositeRealmScopeMappings(@QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        viewPermission.require();

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        Function<RoleModel, RoleRepresentation> toBriefRepresentation = briefRepresentation ?
                ModelToRepresentation::toBriefRepresentation : ModelToRepresentation::toRepresentation;
        return realm.getRolesStream()
                .filter(scopeContainer::hasScope)
                .map(toBriefRepresentation);
    }

    /**
     * Add a set of realm-level roles to the client's scope
     *
     * @param roles
     */
    @Path("realm")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addRealmScopeMappings(List<RoleRepresentation> roles) {
        managePermission.require();

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = realm.getRoleById(role.getId());
            if (roleModel == null) {
                throw new NotFoundException("Role not found");
            }
            scopeContainer.addScopeMapping(roleModel);
        }

        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(roles).success();
    }

    /**
     * Remove a set of realm-level roles from the client's scope
     *
     * @param roles
     */
    @Path("realm")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteRealmScopeMappings(List<RoleRepresentation> roles) {
        managePermission.require();

        if (scopeContainer == null) {
            throw new NotFoundException("Could not find client");
        }

        if (roles == null) {
            roles = scopeContainer.getRealmScopeMappingsStream()
                    .peek(scopeContainer::deleteScopeMapping)
                    .map(ModelToRepresentation::toBriefRepresentation)
                    .collect(Collectors.toList());
       } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRoleById(role.getId());
                if (roleModel == null) {
                    throw new NotFoundException("Role not found");
                }
                scopeContainer.deleteScopeMapping(roleModel);
            }
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(roles).success();

    }

    @Path("clients/{client}")
    public ScopeMappedClientResource getClientByIdScopeMappings(@PathParam("client") String client) {
        ClientModel clientModel = realm.getClientById(client);
        if (clientModel == null) {
            throw new NotFoundException("Could not find client");
        }
        return new ScopeMappedClientResource(realm, auth, this.scopeContainer, session, clientModel, adminEvent, managePermission, viewPermission);
    }
}

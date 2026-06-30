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

package org.keycloak.models.jpa;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.branding.LoginBrandingProvider;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.*;
import org.keycloak.models.RoleContainerModel.RoleRemovedEvent;
import org.keycloak.models.delegate.ClientModelLazyDelegate;
import org.keycloak.models.jpa.entities.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.utils.GeneralHelper;

import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaRealmProvider implements RealmProvider, ClientProvider, ClientScopeProvider, GroupProvider, RoleProvider, DeploymentStateProvider, LoginBrandingProvider,RealmPolicyProvider {
    protected static final Logger logger = Logger.getLogger(JpaRealmProvider.class);
    private final KeycloakSession session;
    protected EntityManager em;
    private Set<String> clientSearchableAttributes;

    public JpaRealmProvider(KeycloakSession session, EntityManager em, Set<String> clientSearchableAttributes) {
        this.session = session;
        this.em = em;
        this.clientSearchableAttributes = clientSearchableAttributes;
    }

    @Override
    public MigrationModel getMigrationModel() {
        return new MigrationModelAdapter(em);
    }

    @Override
    public MigrationModel getQVMigrationModel() {
        return new QVMigrationModelAdapter(em);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmEntity realm = new RealmEntity();
        realm.setName(name);
        realm.setId(id);
        realm.setUserInviteWay("otp");
        em.persist(realm);
        em.flush();
        final RealmModel adapter = new RealmAdapter(session, em, realm);
        session.getKeycloakSessionFactory().publish(new RealmModel.RealmCreationEvent() {
            @Override
            public RealmModel getCreatedRealm() {
                return adapter;
            }
            @Override
            public KeycloakSession getKeycloakSession() {
            	return session;
            }
        });
        return adapter;
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) return null;
        RealmAdapter adapter = new RealmAdapter(session, em, realm);
        return adapter;
    }

    @Override
    public Stream<RealmModel> getRealmsWithProviderTypeStream(Class<?> providerType) {
        TypedQuery<String> query = em.createNamedQuery("getRealmIdsWithProviderType", String.class);
        query.setParameter("providerType", providerType.getName());
        return getRealms(query);
    }

    @Override
    public Stream<RealmModel> getRealmsStream() {
        return getRealmsStream(-1,-1);
    }

    @Override
    public int getRealmsCount() {
        Query query = em.createNamedQuery("getAllRealmCount");
        Number n = (Number) query.getSingleResult();
        return n.intValue();
    }

    @Override
    public Stream<RealmModel> searchRealmsByRealmName(String realmName, Integer firstResult, Integer maxResults) {
        TypedQuery<RealmEntity> query = em.createNamedQuery("searchRealmsByRealmName", RealmEntity.class);
        if (firstResult != null && firstResult > 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null && maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        query.setParameter("realmName", realmName);

        Stream<RealmModel> realms = closing(query.getResultStream().map(entity -> new RealmAdapter( session,em, entity)));
        return realms
                .filter(Objects::nonNull);
    }

    @Override
    public int searchRealmsByRealmNameCount(String realmName) {
        Query query = em.createNamedQuery("searchRealmsByRealmNameCount");
        query.setParameter("realmName", realmName);

        Number n = (Number) query.getSingleResult();
        return n.intValue();
    }

    private Stream<RealmModel> getRealms(TypedQuery<String> query) {
        return closing(query.getResultStream().map(session.realms()::getRealm).filter(Objects::nonNull));
    }

    @Override
    public RealmModel getRealmByName(String name) {
        TypedQuery<String> query = em.createNamedQuery("getRealmIdByName", String.class);
        query.setParameter("name", name);
        List<String> entities = query.getResultList();
        if (entities.isEmpty()) return null;
        if (entities.size() > 1) throw new IllegalStateException("Should not be more than one realm with same name");
        String id = query.getResultList().get(0);

        return session.realms().getRealm(id);
    }

    @Override
    public Stream<RealmModel> getRealmsStream(Integer firstResult, Integer maxResults) {
        TypedQuery<RealmEntity> query = em.createNamedQuery("getAllRealmIds", RealmEntity.class);
        query.setParameter("rootAccountName", Config.getAdminRealm());
        if (firstResult != null && firstResult > 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null && maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        Stream<RealmModel> realms = closing(query.getResultStream().map(entity -> new RealmAdapter( session,em, entity)));
        return realms
                .filter(Objects::nonNull);
    }

    @Override
    public boolean removeRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (realm == null) {
            return false;
        }
        em.refresh(realm);
        final RealmAdapter adapter = new RealmAdapter(session, em, realm);
        session.users().preRemove(adapter);

        realm.getDefaultGroupIds().clear();
        em.flush();

        int num = em.createNamedQuery("deleteGroupRoleMappingsByRealm")
                .setParameter("realm", realm.getId()).executeUpdate();

        session.clients().removeClients(adapter);

        num = em.createNamedQuery("deleteDefaultClientScopeRealmMappingByRealm")
                .setParameter("realm", realm).executeUpdate();

        session.clientScopes().removeClientScopes(adapter);
        for(ClientScopeEntity a: new LinkedList<>(realm.getClientScopes())) {
            adapter.removeClientScope(a.getId());
        }
        session.roles().removeRoles(adapter);

        removeAllBrandings(adapter);
        removePolicies(adapter);

        adapter.getTopLevelGroupsStream().forEach(adapter::removeGroup);

        num = em.createNamedQuery("removeClientInitialAccessByRealm")
                .setParameter("realm", realm).executeUpdate();

        em.remove(realm);

        em.flush();
        em.clear();

        session.getKeycloakSessionFactory().publish(new RealmModel.RealmRemovedEvent() {
            @Override
            public RealmModel getRealm() {
                return adapter;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        return true;
    }

    @Override
    public void close() {
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String name) {
       return addRealmRole(realm, KeycloakModelUtils.generateId(), name);

    }
    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        if (getRealmRole(realm, name) != null) {
            throw new ModelDuplicateException();
        }
        RoleEntity entity = new RoleEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setRealmId(realm.getId());
        em.persist(entity);
        em.flush();
        RoleAdapter adapter = new RoleAdapter(session, realm, em, entity);
        return adapter;

    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        TypedQuery<String> query = em.createNamedQuery("getRealmRoleIdByName", String.class);
        query.setParameter("name", name);
        query.setParameter("realm", realm.getId());
        List<String> roles = query.getResultList();
        if (roles.isEmpty()) return null;
        return session.roles().getRoleById(realm, roles.get(0));
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String name) {
        return addClientRole(client, KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String id, String name) {
        if (getClientRole(client, name) != null) {
            throw new ModelDuplicateException();
        }
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setRealmId(client.getRealm().getId());
        roleEntity.setClientId(client.getId());
        roleEntity.setClientRole(true);
        em.persist(roleEntity);
        RoleAdapter adapter = new RoleAdapter(session, client.getRealm(), em, roleEntity);
        return adapter;
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getRealmRoleIds", String.class);
        query.setParameter("realm", realm.getId());
        Stream<String> roles = query.getResultStream();

        return closing(roles.map(realm::getRoleById));
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        TypedQuery<String> query = em.createNamedQuery("getClientRoleIdByName", String.class);
        query.setParameter("name", name);
        query.setParameter("client", client.getId());
        List<String> roles = query.getResultList();
        if (roles.isEmpty()) return null;
        return session.roles().getRoleById(client.getRealm(), roles.get(0));
    }

    @Override
    public Map<ClientModel, Set<String>> getAllRedirectUrisOfEnabledClients(RealmModel realm) {
        TypedQuery<Map> query = em.createNamedQuery("getAllRedirectUrisOfEnabledClients", Map.class);
        query.setParameter("realm", realm.getId());
        return closing(query.getResultStream()
          .filter(s -> s.get("client") != null))
          .collect(
            Collectors.groupingBy(
              s -> new ClientAdapter(realm, em, session, (ClientEntity) s.get("client")),
              Collectors.mapping(s -> (String) s.get("redirectUri"), Collectors.toSet())
            )
          );
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("getRealmRoles", RoleEntity.class);
        query.setParameter("realm", realm.getId());

        return getRolesStream(query, realm, first, max);
    }

    @Override
    public Stream<RoleModel> getRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        if (ids == null) return Stream.empty();

        TypedQuery<String> query;

        if (search == null) {
            query = em.createNamedQuery("getRoleIdsFromIdList", String.class);
        } else {
            query = em.createNamedQuery("getRoleIdsByNameContainingFromIdList", String.class)
                    .setParameter("search", search);
        }

        query.setParameter("realm", realm.getId())
                .setParameter("ids", ids.collect(Collectors.toList()));

        return closing(paginateQuery(query, first, max).getResultStream())
                .map(g -> session.roles().getRoleById(realm, g));
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("getClientRoles", RoleEntity.class);
        query.setParameter("client", client.getId());

        return getRolesStream(query, client.getRealm(), first, max);
    }

    protected Stream<RoleModel> getRolesStream(TypedQuery<RoleEntity> query, RealmModel realm, Integer first, Integer max) {
        Stream<RoleEntity> results = paginateQuery(query, first, max).getResultStream();

        return closing(results.map(role -> new RoleAdapter(session, realm, em, role)));
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("searchForClientRoles", RoleEntity.class);
        query.setParameter("client", client.getId());
        return searchForRoles(query, client.getRealm(), search, first, max);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("searchForRealmRoles", RoleEntity.class);
        query.setParameter("realm", realm.getId());

        return searchForRoles(query, realm, search, first, max);
    }

    protected Stream<RoleModel> searchForRoles(TypedQuery<RoleEntity> query, RealmModel realm, String search, Integer first, Integer max) {
        query.setParameter("search", "%" + search.trim().toLowerCase() + "%");
        Stream<RoleEntity> results = paginateQuery(query, first, max).getResultStream();

        return closing(results.map(role -> new RoleAdapter(session, realm, em, role)));
    }

    @Override
    public boolean removeRole(RoleModel role) {
        RealmModel realm;
        if (role.getContainer() instanceof RealmModel) {
            realm = (RealmModel) role.getContainer();
        } else if (role.getContainer() instanceof ClientModel) {
            realm = ((ClientModel)role.getContainer()).getRealm();
        } else {
            throw new IllegalStateException("RoleModel's container isn not instance of either RealmModel or ClientModel");
        }
        session.users().preRemove(realm, role);
        RoleEntity roleEntity = em.getReference(RoleEntity.class, role.getId());
        if (roleEntity == null || !roleEntity.getRealmId().equals(realm.getId())) {
            // Throw model exception to ensure transaction rollback and revert previous operations (removing default roles) as well
            throw new ModelException("Role not found or trying to remove role from incorrect realm");
        }
        String compositeRoleTable = JpaUtils.getTableNameForNativeQuery("COMPOSITE_ROLE", em);
        em.createNativeQuery("delete from " + compositeRoleTable + " where CHILD_ROLE = :role").setParameter("role", roleEntity).executeUpdate();
        em.createNamedQuery("deleteClientScopeRoleMappingByRole").setParameter("role", roleEntity).executeUpdate();

        em.flush();
        em.remove(roleEntity);

        session.getKeycloakSessionFactory().publish(roleRemovedEvent(role));

        em.flush();
        return true;

    }

    public RoleRemovedEvent roleRemovedEvent(RoleModel role) {
        return new RoleContainerModel.RoleRemovedEvent() {
            @Override
            public RoleModel getRole() {
                return role;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
    }

    @Override
    public void removeRoles(RealmModel realm) {
        // No need to go through cache. Roles were already invalidated
        realm.getRolesStream().forEach(this::removeRole);
    }

    @Override
    public void removeRoles(ClientModel client) {
        // No need to go through cache. Roles were already invalidated
        client.getRolesStream().forEach(this::removeRole);
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        if (!realm.getId().equals(entity.getRealmId())) return null;
        RoleAdapter adapter = new RoleAdapter(session, realm, em, entity);
        return adapter;
    }

    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        GroupEntity groupEntity = em.find(GroupEntity.class, id);
        if (groupEntity == null) return null;
        if (!groupEntity.getRealm().equals(realm.getId())) return null;
        GroupAdapter adapter =  new GroupAdapter(realm, em, groupEntity);
        return adapter;
    }

    @Override
    public void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent) {
        if (toParent != null && group.getId().equals(toParent.getId())) {
            return;
        }
        if (group.getParentId() != null) {
            group.getParent().removeChild(group);
        }
        group.setParent(toParent);
        if (toParent != null) toParent.addChild(group);
        else session.groups().addTopLevelGroup(realm, group);

        // TODO: Remove em.flush(), currently this needs to be there to translate ConstraintViolationException to
        //  DuplicateModelException {@link PersistenceExceptionConverter} is not called if the
        //  ConstraintViolationException is not thrown in method called directly from EntityManager
        em.flush();
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) {
        return closing(em.createNamedQuery("getGroupIdsByRealm", String.class)
                .setParameter("realm", realm.getId())
                .getResultStream())
                .map(g -> session.groups().getGroupById(realm, g));
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        if (search == null || search.isEmpty()) return getGroupsStream(realm, ids, first, max);

        TypedQuery<String> query = em.createNamedQuery("getGroupIdsByNameContainingFromIdList", String.class)
                .setParameter("realm", realm.getId())
                .setParameter("search", search)
                .setParameter("ids", ids.collect(Collectors.toList()));

        return closing(paginateQuery(query, first, max).getResultStream())
                .map(g -> session.groups().getGroupById(realm, g));
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, Integer first, Integer max) {
        if (first == null && max == null) {
            return getGroupsStream(realm, ids);
        }

        TypedQuery<String> query = em.createNamedQuery("getGroupIdsFromIdList", String.class)
                .setParameter("realm", realm.getId())
                .setParameter("ids", ids.collect(Collectors.toList()));


        return closing(paginateQuery(query, first, max).getResultStream())
                .map(g -> session.groups().getGroupById(realm, g));
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids) {
        return ids.map(id -> session.groups().getGroupById(realm, id)).sorted(GroupModel.COMPARE_BY_NAME);
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Stream<String> ids, String search) {
        TypedQuery<Long> query;
        if (search != null && !search.isEmpty()) {
            query = em.createNamedQuery("getGroupCountByNameContainingFromIdList", Long.class)
                        .setParameter("search", search);
        } else {
            query = em.createNamedQuery("getGroupIdsFromIdList", Long.class);
        }

        return query.setParameter("realm", realm.getId())
                .setParameter("ids", ids.collect(Collectors.toList()))
                .getSingleResult();
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        if(Objects.equals(onlyTopGroups, Boolean.TRUE)) {
            return em.createNamedQuery("getTopLevelGroupCount", Long.class)
                    .setParameter("realm", realm.getId())
                    .setParameter("parent", GroupEntity.TOP_PARENT_ID)
                    .getSingleResult();
        } else {
            return em.createNamedQuery("getGroupCount", Long.class)
                    .setParameter("realm", realm.getId())
                    .getSingleResult();
        }
    }

    @Override
    public long getClientsCount(RealmModel realm) {
        final Long res = em.createNamedQuery("getRealmClientsCount", Long.class)
          .setParameter("realm", realm.getId())
          .getSingleResult();
        return res == null ? 0l : res;
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        return searchForGroupByNameStream(realm, search, null, null).count();
    }

    @Override
    public Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        TypedQuery<GroupEntity> query = em.createNamedQuery("groupsInRole", GroupEntity.class);
        query.setParameter("roleId", role.getId());

        Stream<GroupEntity> results = paginateQuery(query, firstResult, maxResults).getResultStream();

        return closing(results
        		.map(g -> (GroupModel) new GroupAdapter(realm, em, g))
                .sorted(GroupModel.COMPARE_BY_NAME));
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm) {
        return getTopLevelGroupsStream(realm, null, null);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, Integer first, Integer max) {
        TypedQuery<String> groupsQuery =  em.createNamedQuery("getTopLevelGroupIds", String.class)
                .setParameter("realm", realm.getId())
                .setParameter("parent", GroupEntity.TOP_PARENT_ID);

        return closing(paginateQuery(groupsQuery, first, max).getResultStream()
                .map(realm::getGroupById)
                .sorted(GroupModel.COMPARE_BY_NAME)
        );
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel group) {
        if (group == null) {
            return false;
        }

        GroupModel.GroupRemovedEvent event = new GroupModel.GroupRemovedEvent() {
            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public GroupModel getGroup() {
                return group;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
        session.getKeycloakSessionFactory().publish(event);

        session.users().preRemove(realm, group);

        realm.removeDefaultGroup(group);
        group.getSubGroupsStream().forEach(realm::removeGroup);

        GroupEntity groupEntity = em.find(GroupEntity.class, group.getId(), LockModeType.PESSIMISTIC_WRITE);
        if ((groupEntity == null) || (!groupEntity.getRealm().equals(realm.getId()))) {
            return false;
        }
        em.createNamedQuery("deleteGroupRoleMappingsByGroup").setParameter("group", groupEntity).executeUpdate();

        em.remove(groupEntity);
        return true;


    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent) {
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        } else if (GroupEntity.TOP_PARENT_ID.equalsIgnoreCase(id)) {
            // maybe it's impossible but better ensure this doesn't happen
            throw new ModelException("The ID of the new group is equals to the tag used for top level groups");
        }
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(id);
        groupEntity.setName(name);
        groupEntity.setRealm(realm.getId());
        groupEntity.setParentId(toParent == null? GroupEntity.TOP_PARENT_ID : toParent.getId());
        em.persist(groupEntity);
        em.flush();

        return new GroupAdapter(realm, em, groupEntity);
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroup) {
        subGroup.setParent(null);
    }

    public void preRemove(RealmModel realm, RoleModel role) {
        // GroupProvider method implementation starts here
        em.createNamedQuery("deleteGroupRoleMappingsByRole").setParameter("roleId", role.getId()).executeUpdate();
        // GroupProvider method implementation ends here

        // ClientProvider implementation
        String clientScopeMapping = JpaUtils.getTableNameForNativeQuery("SCOPE_MAPPING", em);
        em.createNativeQuery("delete from " + clientScopeMapping + " where ROLE_ID = :role").setParameter("role", role.getId()).executeUpdate();
    }

    @Override
    public ClientModel addClient(RealmModel realm, String clientId) {
        return addClient(realm, KeycloakModelUtils.generateId(), clientId);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }

        if (clientId == null) {
            clientId = id;
        }

        logger.tracef("addClient(%s, %s, %s)%s", realm, id, clientId, getShortStackTrace());

        ClientEntity entity = new ClientEntity();
        entity.setId(id);
        entity.setClientId(clientId);
        entity.setEnabled(true);
        entity.setStandardFlowEnabled(true);
        entity.setRealmId(realm.getId());
        entity.setLastUpdatedTimestamp(System.currentTimeMillis());
        em.persist(entity);

        final ClientModel resource = new ClientAdapter(realm, em, session, entity);

        session.getKeycloakSessionFactory().publish((ClientModel.ClientCreationEvent) () -> resource);
        return resource;
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm) {
        return getClientsStream(realm, null, null);
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        TypedQuery<String> query = em.createNamedQuery("getClientIdsByRealm", String.class);

        if (firstResult != null && firstResult > 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null && maxResults > 0) {
            query.setMaxResults(maxResults);
        }

        query.setParameter("realm", realm.getId());
        Stream<String> clients = paginateQuery(query, firstResult, maxResults).getResultStream();

        return closing(clients.map(id -> (ClientModel) new ClientModelLazyDelegate.WithId(session, realm, id)));
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm, String orderBy) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<ClientEntity> queryBuilder = builder.createQuery(ClientEntity.class);
        Root<ClientEntity> root = queryBuilder.from(ClientEntity.class);

        Predicate realmPredicate = builder.equal(root.get("realmId"), realm.getId());

        switch ((orderBy == null) ? "default" : orderBy) {
            case "asc": queryBuilder.orderBy(builder.asc(root.get("name"))); break;
            case "desc": queryBuilder.orderBy(builder.desc(root.get("name"))); break;
            default: {
                Expression<Long> clientLastUpdatedTimestamp = builder.max(root.get(ClientModel.LAST_UPDATED_TIMESTAMP));
                queryBuilder.groupBy(root.get("id"));

                queryBuilder.orderBy(
                        builder.asc(
                                builder.selectCase()
                                        .when(builder.equal(root.get(ClientModel.LAST_UPDATED_TIMESTAMP), clientLastUpdatedTimestamp), 0)
                                        .otherwise(builder.diff(clientLastUpdatedTimestamp, root.get(ClientModel.LAST_UPDATED_TIMESTAMP)))
                        ),
                        builder.desc(root.get(ClientModel.LAST_UPDATED_TIMESTAMP))
                );
            } break;
        }
        queryBuilder.where(realmPredicate);
        TypedQuery<ClientEntity> query = em.createQuery(queryBuilder);

        return closing(paginateQuery(query, null, null).getResultStream())
                .map(client -> new ClientModelLazyDelegate.WithId(session, realm, client.getId()));
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getAlwaysDisplayInConsoleClients", String.class);
        query.setParameter("realm", realm.getId());
        Stream<String> clientStream = query.getResultStream();

        return closing(clientStream.map(c -> session.clients().getClientById(realm, c)).filter(Objects::nonNull));
    }

    @Override
    public ClientModel getClientById(RealmModel realm, String id) {
        logger.tracef("getClientById(%s, %s)%s", realm, id, getShortStackTrace());

        ClientEntity client = em.find(ClientEntity.class, id);
        // Check if client belongs to this realm
        if (client == null || !realm.getId().equals(client.getRealmId())) return null;
        ClientAdapter adapter = new ClientAdapter(realm, em, session, client);
        return adapter;

    }

    @Override
    public ClientModel getClientByClientId(RealmModel realm, String clientId) {
        logger.tracef("getClientByClientId(%s, %s)%s", realm, clientId, getShortStackTrace());

        TypedQuery<String> query = em.createNamedQuery("findClientIdByClientId", String.class);
        query.setParameter("clientId", clientId);
        query.setParameter("realm", realm.getId());
        List<String> results = query.getResultList();
        if (results.isEmpty()) return null;
        String id = results.get(0);
        return session.clients().getClientById(realm, id);
    }

    @Override
    public ClientModel getClientBySandboxClientId(RealmModel realm, String sandboxClientId) {
        logger.tracef("sandboxClientId(%s, %s)%s", realm, sandboxClientId, getShortStackTrace());

        TypedQuery<String> query = em.createNamedQuery("findClientIdBySandboxClientId", String.class);
        query.setParameter("sandboxClientId", sandboxClientId);
        query.setParameter("realm", realm.getId());
        List<String> results = query.getResultList();
        if (results.isEmpty()) return null;
        String id = results.get(0);
        return session.clients().getClientByClientId(realm, id);
    }

    @Override
    public Stream<ClientModel> searchClientsByClientIdStream(RealmModel realm, String clientId, Integer firstResult, Integer maxResults) {
        TypedQuery<String> query = em.createNamedQuery("searchClientsByClientId", String.class);
        query.setParameter("clientId", clientId);
        query.setParameter("realm", realm.getId());

        Stream<String> results = paginateQuery(query, firstResult, maxResults).getResultStream();
        return closing(results.map(id -> (ClientModel) new ClientModelLazyDelegate.WithId(session, realm, id)));
    }

    @Override
    public Stream<ClientModel> searchClientsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        Map<String, String> filteredAttributes = clientSearchableAttributes == null ? attributes :
                attributes.entrySet().stream().filter(m -> clientSearchableAttributes.contains(m.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<ClientEntity> root = queryBuilder.from(ClientEntity.class);
        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));

        for (Map.Entry<String, String> entry : filteredAttributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            Join<ClientEntity, ClientAttributeEntity> attributeJoin = root.join("attributes");

            Predicate attrNamePredicate = builder.equal(attributeJoin.get("name"), key);
            Predicate attrValuePredicate = builder.equal(attributeJoin.get("value"), value);
            predicates.add(builder.and(attrNamePredicate, attrValuePredicate));
        }

        Predicate finalPredicate = builder.and(predicates.toArray(new Predicate[0]));
        queryBuilder.where(finalPredicate).orderBy(builder.asc(root.get("clientId")));

        TypedQuery<String> query = em.createQuery(queryBuilder);
        return closing(paginateQuery(query, firstResult, maxResults).getResultStream())
                .map(id -> session.clients().getClientById(realm, id));
    }

    @Override
    public Stream<ClientModel> searchClientsByClientNameStream(RealmModel realm, String clientName, Integer firstResult, Integer maxResults) {
        TypedQuery<ClientEntity> query = em.createNamedQuery("searchClientsByClientName", ClientEntity.class);
        if (firstResult != null && firstResult > 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null && maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        query.setParameter("clientName", clientName);
        query.setParameter("realm", realm.getId());
        Stream<ClientModel> clients = closing(query.getResultStream().map(entity -> new ClientAdapter(realm, em, session, entity)));
        return clients
                .filter(Objects::nonNull);
    }

    @Override
    public void removeClients(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getClientIdsByRealm", String.class);
        query.setParameter("realm", realm.getId());
        List<String> clients = query.getResultList();
        for (String client : clients) {
            // No need to go through cache. Clients were already invalidated
            removeClient(realm, client);
        }
    }

    @Override
    public boolean removeClient(RealmModel realm, String id) {

        logger.tracef("removeClient(%s, %s)%s", realm, id, getShortStackTrace());

        final ClientModel client = getClientById(realm, id);
        if (client == null) return false;

        session.users().preRemove(realm, client);
        session.roles().removeRoles(client);

        ClientEntity clientEntity = em.find(ClientEntity.class, id, LockModeType.PESSIMISTIC_WRITE);

        session.getKeycloakSessionFactory().publish(new ClientModel.ClientRemovedEvent() {
            @Override
            public ClientModel getClient() {
                return client;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        int countRemoved = em.createNamedQuery("deleteClientScopeClientMappingByClient")
                .setParameter("clientId", clientEntity.getId())
                .executeUpdate();
        em.remove(clientEntity);  // i have no idea why, but this needs to come before deleteScopeMapping

        try {
            em.flush();
        } catch (RuntimeException e) {
            logger.errorv("Unable to delete client entity: {0} from realm {1}", client.getClientId(), realm.getName());
            throw e;
        }

        return true;
    }

    @Override
    public ClientScopeModel getClientScopeById(RealmModel realm, String id) {
        ClientScopeEntity clientScope = em.find(ClientScopeEntity.class, id);

        // Check if client scope belongs to this realm
        if (clientScope == null || !realm.getId().equals(clientScope.getRealmId())) return null;
        ClientScopeAdapter adapter = new ClientScopeAdapter(realm, em, session, clientScope);
        return adapter;
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getClientScopeIds", String.class);
        query.setParameter("realm", realm.getId());
        Stream<String> scopes = query.getResultStream();

        return closing(scopes.map(realm::getClientScopeById));
    }

    @Override
    public ClientScopeModel addClientScope(RealmModel realm, String id, String name) {
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }
        ClientScopeEntity entity = new ClientScopeEntity();
        entity.setId(id);
        name = KeycloakModelUtils.convertClientScopeName(name);
        entity.setName(name);
        entity.setRealmId(realm.getId());
        em.persist(entity);
        em.flush();
        return new ClientScopeAdapter(realm, em, session, entity);
    }

    @Override
    public boolean removeClientScope(RealmModel realm, String id) {
        if (id == null) return false;
        ClientScopeModel clientScope = getClientScopeById(realm, id);
        if (clientScope == null) return false;

        session.users().preRemove(clientScope);
        realm.removeDefaultClientScope(clientScope);
        ClientScopeEntity clientScopeEntity = em.find(ClientScopeEntity.class, id, LockModeType.PESSIMISTIC_WRITE);

        em.createNamedQuery("deleteClientScopeClientMappingByClientScope").setParameter("clientScopeId", clientScope.getId()).executeUpdate();
        em.createNamedQuery("deleteClientScopeRoleMappingByClientScope").setParameter("clientScope", clientScopeEntity).executeUpdate();
        em.remove(clientScopeEntity);

        session.getKeycloakSessionFactory().publish(new ClientScopeModel.ClientScopeRemovedEvent() {

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public ClientScopeModel getClientScope() {
                return clientScope;
            }
        });

        em.flush();
        return true;
    }

    @Override
    public void removeClientScopes(RealmModel realm) {
        // No need to go through cache. Client scopes were already invalidated
        realm.getClientScopesStream().map(ClientScopeModel::getId).forEach(id -> this.removeClientScope(realm, id));
    }

    @Override
    public void addClientScopes(RealmModel realm, ClientModel client, Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        // Defaults to openid-connect
        String clientProtocol = client.getProtocol() == null ? OIDCLoginProtocol.LOGIN_PROTOCOL : client.getProtocol();

        Map<String, ClientScopeModel> existingClientScopes = getClientScopes(realm, client, true);
        existingClientScopes.putAll(getClientScopes(realm, client, false));

        clientScopes.stream()
            .filter(clientScope -> ! existingClientScopes.containsKey(clientScope.getName()))
            .filter(clientScope -> Objects.equals(clientScope.getProtocol(), clientProtocol))
            .forEach(clientScope -> {
                ClientScopeClientMappingEntity entity = new ClientScopeClientMappingEntity();
                entity.setClientScopeId(clientScope.getId());
                entity.setClientId(client.getId());
                entity.setDefaultScope(defaultScope);
                em.persist(entity);
                em.flush();
                em.detach(entity);
            });
    }

    @Override
    public void removeClientScope(RealmModel realm, ClientModel client, ClientScopeModel clientScope) {
        em.createNamedQuery("deleteClientScopeClientMapping")
                .setParameter("clientScopeId", clientScope.getId())
                .setParameter("clientId", client.getId())
                .executeUpdate();
        em.flush();
    }


    public Map<String, ClientScopeModel> getClientScopes(RealmModel realm, ClientModel client, boolean defaultScope) {
        // Defaults to openid-connect
        String clientProtocol = client.getProtocol() == null ? OIDCLoginProtocol.LOGIN_PROTOCOL : client.getProtocol();

        TypedQuery<String> query = em.createNamedQuery("clientScopeClientMappingIdsByClient", String.class);
        query.setParameter("clientId", client.getId());
        query.setParameter("defaultScope", defaultScope);

        return closing(query.getResultStream())
                .map(clientScopeId -> session.clientScopes().getClientScopeById(realm, clientScopeId))
                .filter(Objects::nonNull)
                .filter(clientScope -> Objects.equals(clientScope.getProtocol(), clientProtocol))
                .collect(Collectors.toMap(ClientScopeModel::getName, Function.identity()));
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Integer first, Integer max) {
        TypedQuery<String> query = em.createNamedQuery("getGroupIdsByNameContaining", String.class)
                .setParameter("realm", realm.getId())
                .setParameter("search", search);

        Stream<String> groups =  paginateQuery(query, first, max).getResultStream();

        return closing(groups.map(id -> {
            GroupModel groupById = session.groups().getGroupById(realm, id);
            while (Objects.nonNull(groupById.getParentId())) {
                groupById = session.groups().getGroupById(realm, groupById.getParentId());
            }
            return groupById;
        }).sorted(GroupModel.COMPARE_BY_NAME).distinct());
    }

    @Override
    public void removeExpiredClientInitialAccess() {
        int currentTime = Time.currentTime();

        em.createNamedQuery("removeExpiredClientInitialAccess")
                .setParameter("currentTime", currentTime)
                .executeUpdate();
    }

    private RealmLocalizationTextsEntity getRealmLocalizationTextsEntity(String locale, String realmId) {
        RealmLocalizationTextsEntity.RealmLocalizationTextEntityKey key = new RealmLocalizationTextsEntity.RealmLocalizationTextEntityKey();
        key.setRealmId(realmId);
        key.setLocale(locale);
        return em.find(RealmLocalizationTextsEntity.class, key);
    }

    @Override
    public boolean updateLocalizationText(RealmModel realm, String locale, String key, String text) {
        RealmLocalizationTextsEntity entity = getRealmLocalizationTextsEntity(locale, realm.getId());
        if (entity != null && entity.getTexts() != null && entity.getTexts().containsKey(key)) {
            entity.getTexts().put(key, text);

            em.persist(entity);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void saveLocalizationText(RealmModel realm, String locale, String key, String text) {
        RealmLocalizationTextsEntity entity = getRealmLocalizationTextsEntity(locale, realm.getId());
        if(entity == null) {
            entity = new RealmLocalizationTextsEntity();
            entity.setRealmId(realm.getId());
            entity.setLocale(locale);
            entity.setTexts(new HashMap<>());
        }
        entity.getTexts().put(key, text);
        em.persist(entity);
    }

    @Override
    public void saveLocalizationTexts(RealmModel realm, String locale, Map<String, String> localizationTexts) {
        RealmLocalizationTextsEntity entity = new RealmLocalizationTextsEntity();
        entity.setTexts(localizationTexts);
        entity.setLocale(locale);
        entity.setRealmId(realm.getId());
        em.merge(entity);
    }

    @Override
    public boolean deleteLocalizationTextsByLocale(RealmModel realm, String locale) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaDelete<RealmLocalizationTextsEntity> criteriaDelete =
                builder.createCriteriaDelete(RealmLocalizationTextsEntity.class);
        Root<RealmLocalizationTextsEntity> root = criteriaDelete.from(RealmLocalizationTextsEntity.class);

        criteriaDelete.where(builder.and(
                builder.equal(root.get("realmId"), realm.getId()),
                builder.equal(root.get("locale"), locale)));
        int linesUpdated = em.createQuery(criteriaDelete).executeUpdate();
        return linesUpdated == 1?true:false;
    }

    @Override
    public String getLocalizationTextsById(RealmModel realm, String locale, String key) {
        RealmLocalizationTextsEntity entity = getRealmLocalizationTextsEntity(locale, realm.getId());
        if (entity != null && entity.getTexts() != null && entity.getTexts().containsKey(key)) {
            return entity.getTexts().get(key);
        }
        return null;
    }

    @Override
    public boolean deleteLocalizationText(RealmModel realm, String locale, String key) {
        RealmLocalizationTextsEntity entity = getRealmLocalizationTextsEntity(locale, realm.getId());
        if (entity != null && entity.getTexts() != null && entity.getTexts().containsKey(key)) {
            entity.getTexts().remove(key);

            em.persist(entity);
            return true;
        } else {
            return false;
        }
    }

    public Set<String> getClientSearchableAttributes() {
        return clientSearchableAttributes;
    }
    @Override
    public BrandingModel getAppBranding(RealmModel realmModel, String clientId) {
        ClientModel clientModel = getClientByClientId(realmModel, clientId);
        if(clientModel != null){
            String brandName = clientModel.getAttributes().get("login_theme");
            if (brandName == null){
                clientModel.setAttribute("login_theme","default");
                return getDefaultBrand(realmModel);
            }
            TypedQuery<BrandingEntity> query = em.createNamedQuery("getBrandByBrandName", BrandingEntity.class);
            query.setParameter("realm", realmModel.getId());
            query.setParameter("brandName", brandName);
            try {
                BrandingEntity brandingEntity = query.getSingleResult();
                if (brandingEntity != null)
                    return new BrandingAdapter(realmModel, em, session, brandingEntity);
            } catch (Exception err) {
                clientModel.setAttribute("login_theme","default");
                return getDefaultBrand(realmModel);
            }
        }
        return getDefaultBrand(realmModel);
    }

    @Override
    public BrandingModel getDefaultBrand(RealmModel realmModel) {
        TypedQuery<BrandingEntity> query = em.createNamedQuery("getBrandsByRealm", BrandingEntity.class);

        query.setParameter("realm", realmModel.getId());

        try {
            Stream<BrandingEntity> brandingEntityStream = query.getResultStream();
            Optional<BrandingEntity> optBrandingEntity = brandingEntityStream.filter(x -> x.isRealmBrand()).findFirst();
            if (!optBrandingEntity.isPresent()) {
                logger.error("No Branding available for the realm");
                //TODO: here we have to see and create new brand for the realm
                return createBranding(realmModel, "default", true, RealmManager.getDefaultTheme(session,realmModel));
            }
            return new BrandingAdapter(realmModel, em, session, optBrandingEntity.get());
        }
        catch (Exception err) {

        }
        return createBranding(realmModel, "default", true, RealmManager.getDefaultTheme(session,realmModel));
    }

    @Override
    public boolean updateAppBranding(RealmModel realmModel, String clientId, String brandId) {

        ClientModel clientModel = getClientByClientId(realmModel, clientId);
        clientModel.setAttribute("login_theme", brandId);
        return true;
    }

    @Override
    public BrandingModel createBranding(RealmModel realmModel, String brandName, boolean isRealmBrand, Map<String, String> config) {
        String id = KeycloakModelUtils.generateId();

        BrandingEntity brandingEntity = new BrandingEntity();
        brandingEntity.setName(brandName);
        brandingEntity.setRealmBrand(isRealmBrand);
        brandingEntity.setId(id);
        brandingEntity.setCreatedTimestamp(System.currentTimeMillis());
        brandingEntity.setRealm(realmModel.getId());

        em.persist(brandingEntity);
        em.flush();

        BrandingModel brandingModel = new BrandingAdapter(realmModel, em, session, brandingEntity);
        if(brandingEntity.getAttributes() == null)
        {
            brandingEntity.setAttributes(new ArrayList<>());
        }

        for (Map.Entry<String, String> attr : config.entrySet()
        ) {
            brandingModel.setAttribute(attr.getKey(), attr.getValue());
        }

        return brandingModel;
    }

    public static boolean containsAnyExtension(String str, String[] extensions) {
        for (String ext : extensions) {
            if (str.contains(ext)) {
                return true; // Found a matching extension
            }
        }
        return false; // No matching extensions found
    }

    @Override
    public BrandingModel updateBranding(RealmModel realmModel, String id, Map<String, String> config) {

        BrandingModel brandingModel = getBrandById(realmModel,id);
        if (("default").equals(config.get("fontPreference"))) {
            config.put("customFontLink", "");
            config.put("customFontFile", "");
        } else if (("custom").equals(config.get("fontPreference")) && ("link").equals(config.get("customFontPreference"))) {
            config.put("customFontFile", "");
        } else if (("custom").equals(config.get("fontPreference")) && ("file").equals(config.get("customFontPreference"))) {
            config.put("customFontLink", "");
        }
        for (Map.Entry<String, String> attr : config.entrySet()) {
            if(("customFontFile").equals(attr.getKey()) && !attr.getValue().isEmpty() && !attr.getValue().startsWith("http")){
                byte[] decodedBytes = Base64.getDecoder().decode(GeneralHelper.extractBase64String(attr.getValue()));
                String bucketName = session.getContext().getRealm().getName() + "-avatars";
                String fileName = GeneralHelper.extractFileNameFromBase64String(attr.getValue());
                String[] fileFormat = {".ttf", ".otf", ".woff", ".woff2"};
                boolean containsExtension = containsAnyExtension(fileName, fileFormat);
                if(!containsExtension){
                    throw new ErrorResponseException("invalid-font-file-format", "invalid-font-file-format", Response.Status.BAD_REQUEST);
                }
                String objectName = "public/fonts/branding/" + id + "/" + fileName;
                String minioFileUrl = GeneralHelper.uploadToMinIO(bucketName, objectName, decodedBytes);
                brandingModel.setAttribute(attr.getKey(), minioFileUrl);
            }else{
                brandingModel.setAttribute(attr.getKey(), attr.getValue());
            }
        }

        return brandingModel;
    }

    @Override
    public boolean removeBranding(RealmModel realmModel, String id) {
        BrandingEntity brandingEntity = em.find(BrandingEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (brandingEntity == null) return false;
        em.remove(brandingEntity);
        em.flush();
        return true;
    }

    public void removeAllBrandings(RealmModel realm){
        session.loginBranding().getAllBrands(realm,-1,-1)
                .forEach( brand -> removeBranding(realm, brand.getId()));
    }

    @Override
    public Stream<BrandingModel> getAllBrands(RealmModel realmModel, Integer first, Integer max) {
        TypedQuery<BrandingEntity> query = em.createNamedQuery("getBrandsByRealm", BrandingEntity.class);

        query.setParameter("realm", realmModel.getId());

        return closing(paginateQuery(query, first, max).getResultStream()).map(x -> new BrandingAdapter(realmModel, em, session, x));
    }

    @Override
    public BrandingModel getBrandById(RealmModel realmModel, String id) {
        BrandingEntity brandingEntity = em.find(BrandingEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        return new BrandingAdapter(realmModel, em, session, brandingEntity);
    }

    @Override
    public BrandingModel getBrandByName(RealmModel realmModel, String name) {
        TypedQuery<BrandingEntity> query = em.createNamedQuery("getBrandByBrandName", BrandingEntity.class);

        query.setParameter("realm", realmModel.getId());
        query.setParameter("brandName", name);

        try {
            BrandingEntity brandingEntity = query.getSingleResult();

            if (brandingEntity == null)
                return null;

            return new BrandingAdapter(realmModel, em, session, brandingEntity);
        } catch (Exception err){
            logger.error("Error Occurred on Fetching BrandByName " + err.getMessage());
        }
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getBrandingConfig(RealmModel realmModel) {

        BrandingAdapter brandingAdapter = new BrandingAdapter(realmModel, em, session, null);
        return brandingAdapter.getConfig();
    }


    @Override
    public PolicyModel addPolicy(RealmModel realm, PolicyModel model) {
        String id = KeycloakModelUtils.generateId();

        RealmPolicyEntity entity = new RealmPolicyEntity();
        entity.setId(id);
        entity.setCreatedTimestamp(System.currentTimeMillis());
        entity.setPolicyName(model.getPolicyName() == null ? id : model.getPolicyName());
        entity.setRealmId(realm.getId());
        entity.setPolicyValue(model.getPolicyValue());
        entity.setRealmPolicy(model.isRealmPolicy());
        entity.setPolicyType(model.getPolicyType());
        entity.setInBuilt(model.isInBuilt());
        em.persist(entity);
        model.setId(entity.getId());
        em.flush();
        if (model.isRealmPolicy()) {
            realm.setPasswordPolicy(PasswordPolicy.parse(session, model.getPolicyValue()));
        }

        return model;
    }

    @Override
    public boolean updatePolicy(RealmModel realm, String id, String policyName, String policyValue, boolean isRealmPolicy, String policyType, boolean inBuilt) {

        RealmPolicyEntity entity = new RealmPolicyEntity();
        entity.setId(id);
        entity.setCreatedTimestamp(System.currentTimeMillis());
        entity.setPolicyName(policyName);
        entity.setRealmId(realm.getId());
        entity.setPolicyValue(policyValue);
        entity.setRealmPolicy(isRealmPolicy);
        entity.setPolicyType(policyType);
        entity.setInBuilt(inBuilt);
        em.merge(entity);
        em.flush();
        return true;
    }

    @Override
    public boolean removePolicy(RealmModel realm, String id) {
        RealmPolicyEntity realmPolicyEntity = em.find(RealmPolicyEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (realmPolicyEntity == null) return false;
        em.remove(realmPolicyEntity);
        em.flush();
        return true;
    }

    @Override
    public void removePolicies(RealmModel realm) {
        Stream<RealmPolicyModel> policies = getAllPoliciesStream(realm, -1, -1);
        policies.forEach(policy -> {
            removePolicy(realm, policy.getId());
        });
    }

    @Override
    public Stream<RealmPolicyModel> getPoliciesStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        TypedQuery<String> query;
        if(firstResult == null && firstResult == null) {
            query = em.createNamedQuery("getPolicyIdsByRealm", String.class);
        } else {
            query = realm.getMode().equalsIgnoreCase("bo") ? em.createNamedQuery(
                    "getNonRealmPolicyIdsByRealm", String.class)
                    : em.createNamedQuery("getPolicyIdsByRealm", String.class);
        }
        if (firstResult != null && firstResult > 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null && maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        query.setParameter("realm", realm.getId());
        Stream<String> policies = query.getResultStream();

        return closing(policies.map(c -> getPolicyById(realm, c)).filter(Objects::nonNull));
    }

    @Override
    public Stream<RealmPolicyModel> getCibaPoliciesStream(RealmModel realm, Integer firstResult, Integer maxResults, String policyName) {
        TypedQuery<String> query = em.createNamedQuery(
                    "getCibaPolicyIdsByRealm", String.class);
        if (firstResult != null && firstResult > 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null && maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        query.setParameter("realm", realm.getId());
        query.setParameter("policyType", "ciba");
        query.setParameter("policyName", policyName);
        Stream<String> policies = query.getResultStream();

        return closing(policies.map(c -> getPolicyById(realm, c)).filter(Objects::nonNull));
    }


    //used to get all policies including inBuilt policies
    @Override
    public Stream<RealmPolicyModel> getAllPoliciesStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        TypedQuery<String> query = em.createNamedQuery("getAllPolicyIdsOfRealm", String.class);
        if (firstResult != null && firstResult > 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null && maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        query.setParameter("realm", realm.getId());
        Stream<String> policies = query.getResultStream();

        return closing(policies.map(c -> getPolicyById(realm, c)).filter(Objects::nonNull));
    }


    @Override
    public Stream<RealmPolicyModel> searchPoliciesByPolicyNameStream(String policyName, RealmModel realm, Integer firstResult, Integer maxResults) {
        TypedQuery<RealmPolicyEntity> query = em.createNamedQuery("searchPoliciesByPolicyName", RealmPolicyEntity.class);
        if (firstResult != null && firstResult > 0) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null && maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        query.setParameter("policyName", policyName);
        query.setParameter("realm", realm.getId());
        Stream<RealmPolicyModel> policies = closing(query.getResultStream().map(entity -> new RealmPolicyAdapter(realm, em, session, entity)));
        return policies
                .filter(Objects::nonNull);
    }


    @Override
    public RealmPolicyModel getPolicyById(RealmModel realm, String id) {
        RealmPolicyEntity realmPolicyEntity = em.find(RealmPolicyEntity.class, id);
        if (realmPolicyEntity == null || !realm.getId().equals(realmPolicyEntity.getRealmId())) return null;
        return new RealmPolicyAdapter(realm, em, session, realmPolicyEntity);
    }

    @Override
    public RealmPolicyModel getPolicyByName(RealmModel realm, String policyName) {
        return getPoliciesStream(realm, null, null)
                .filter(policy -> policy.getPolicyName().equalsIgnoreCase(policyName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public RealmPolicyModel getRealmPolicy(RealmModel realm, String policyType) {
        try {
            TypedQuery<RealmPolicyEntity> query = em.createNamedQuery("getRealmPolicy", RealmPolicyEntity.class);
            query.setParameter("realm", realm.getId());
            query.setParameter("policyType", policyType);
            RealmPolicyEntity realmPolicyEntity = query.getSingleResult();
            return new RealmPolicyAdapter(realm, em, session, realmPolicyEntity);
        } catch (NoResultException e) {
            logger.errorv("No Results found");
        }
        return null;
    }

    @Override
    public Stream<RealmPolicyModel> getPoliciesByType(RealmModel realm, String policyType) {
        TypedQuery<RealmPolicyEntity> query = em.createNamedQuery("getPolicyByType", RealmPolicyEntity.class);

        query.setParameter("realm", realm.getId());
        query.setParameter("policyType", policyType);
        Stream<RealmPolicyEntity> policies = query.getResultStream();

        return closing(policies.map(c -> ((RealmPolicyModel) new RealmPolicyAdapter(realm, em, session, c))).filter(Objects::nonNull));
    }
}

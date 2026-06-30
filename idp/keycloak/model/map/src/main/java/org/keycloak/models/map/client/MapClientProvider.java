/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.client;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.models.ClientModel.ClientUpdatedEvent;
import org.keycloak.models.ClientModel.SearchableFields;
import org.keycloak.models.map.common.TimeAdapter;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.Order.LAST_UPDATED_TIMESTAMP;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapClientProvider implements ClientProvider {

    private static final Logger LOG = Logger.getLogger(MapClientProvider.class);
    private final KeycloakSession session;
    final MapKeycloakTransaction<MapClientEntity, ClientModel> tx;
    private final ConcurrentMap<String, ConcurrentMap<String, Long>> clientRegisteredNodesStore;

    public MapClientProvider(KeycloakSession session, MapStorage<MapClientEntity, ClientModel> clientStore, ConcurrentMap<String, ConcurrentMap<String, Long>> clientRegisteredNodesStore) {
        this.session = session;
        this.clientRegisteredNodesStore = clientRegisteredNodesStore;
        this.tx = clientStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
    }

    private ClientUpdatedEvent clientUpdatedEvent(ClientModel c) {
        return new ClientModel.ClientUpdatedEvent() {
            @Override
            public ClientModel getUpdatedClient() {
                return c;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
    }

    private <T extends MapClientEntity> Function<T, ClientModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to the caller

        return origEntity -> new MapClientAdapter(session, realm, origEntity) {
            @Override
            public void updateClient() {
                LOG.tracef("updateClient(%s)%s", realm, origEntity.getId(), getShortStackTrace());
                session.getKeycloakSessionFactory().publish(clientUpdatedEvent(this));
            }

            @Override
            public boolean validateSandboxSecret(String sandboxSecret) {
                return false;
            }
            @Override
            public String getMfaPolicyId() {
                return null;
            }

            @Override
            public void setMfaPolicyId(String mfaPolicyId) {

            }

            @Override
            public String getMfaPolicyType() {
                return null;
            }

            @Override
            public void setMfaPolicyType(String mfaPolicyType) {

            }

            @Override
            public boolean isRegistrationAllowed() {
                return false;
            }

            @Override
            public void setRegistrationAllowed(boolean registrationAllowed) {

            }

            @Override
            public boolean isResetPasswordAllowed() {
                return false;
            }

            @Override
            public void setResetPasswordAllowed(boolean resetPasswordAllowed) {

            }

            @Override
            public boolean isRememberMe() {
                return false;
            }

            @Override
            public void setRememberMe(boolean rememberMe) {

            }

            @Override
            public Boolean getApplicationConnections(String connId) {
                return null;
            }

            @Override
            public Map<String, Boolean> getApplicationsConnections() {
                return null;
            }

            @Override
            public void removeApplicationConnections(String connId) {

            }

            @Override
            public void setApplicationConnections(String connId, Boolean connEnabled) {

            }

            @Override
            public Map<String, Boolean> getMFAConnections() {
                return null;
            }

            @Override
            public void removeMFAConnections(String connId) {

            }

            @Override
            public void setMFAConnections(String connId, Boolean connEnabled) {

            }

            @Override
            public Map<String, ClientScopeModel> getClientScopes(boolean defaultScope, boolean filterByProtocol) {
                return null;
            }

            /** This is runtime information and should have never been part of the adapter */
            @Override
            public Map<String, Integer> getRegisteredNodes() {
                return Collections.unmodifiableMap(getMapForEntity()
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(e.getValue())))
                );
            }

            @Override
            public void registerNode(String nodeHost, int registrationTime) {
                getMapForEntity().put(nodeHost, TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(registrationTime));
            }

            @Override
            public void unregisterNode(String nodeHost) {
                getMapForEntity().remove(nodeHost);
            }

            @Override
            public boolean isOverrideAuthentication() {
                return false;
            }

            @Override
            public void setOverrideAuthentication(boolean overrideAuthentication) {

            }

            @Override
            public Long getLastUpdatedTimestamp() {
                return null;
            }

            @Override
            public void setLastUpdatedTimestamp(Long lastUpdatedTimestamp) {
                // NO OP
            }

            @Override
            public Boolean isSandboxMode() {
                return false;
            }

            @Override
            public void setSandboxMode(Boolean isSandboxMode) {
                // NOP
            }

            @Override
            public String getSandboxClientId() {
                return null;
            }

            @Override
            public void setSandboxClientId(String sandboxClientId) {
                // NOP
            }

            @Override
            public String getSandboxSecret() {
                return null;
            }

            @Override
            public void setSandboxSecret(String sandboxSecret) {
                // NOP
            }


            private ConcurrentMap<String, Long> getMapForEntity() {
                return clientRegisteredNodesStore.computeIfAbsent(entity.getId(), k -> new ConcurrentHashMap<>());
            }

        };
    }

    private Predicate<MapClientEntity> entityRealmFilter(RealmModel realm) {
        if (realm == null || realm.getId() == null) {
            return c -> false;
        }
        String realmId = realm.getId();
        return entity -> Objects.equals(realmId, entity.getRealmId());
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.CLIENT_ID))
            .map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm, String orderBy) {
        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return tx.read(withCriteria(mcb).orderBy(SearchableFields.LAST_UPDATED_TIMESTAMP, LAST_UPDATED_TIMESTAMP))
                .map(entityToAdapterFunc(realm));
//        return tx.read(withCriteria(mcb)).map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm) {
        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return tx.read(withCriteria(mcb).orderBy(SearchableFields.CLIENT_ID, ASCENDING))
          .map(entityToAdapterFunc(realm));
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        LOG.tracef("addClient(%s, %s, %s)%s", realm, id, clientId, getShortStackTrace());

        if (id != null && tx.read(id) != null) {
            throw new ModelDuplicateException("Client with same id exists: " + id);
        }
        if (clientId != null && getClientByClientId(realm, clientId) != null) {
            throw new ModelDuplicateException("Client with same clientId in realm " + realm.getName() + " exists: " + clientId);
        }

        MapClientEntity entity = new MapClientEntityImpl();
        entity.setId(id);
        entity.setRealmId(realm.getId());
        entity.setClientId(clientId);
        entity.setEnabled(true);
        entity.setStandardFlowEnabled(true);
        entity = tx.create(entity);
        if (clientId == null) {
            clientId = entity.getId();
            entity.setClientId(clientId);
        }
        final ClientModel resource = entityToAdapterFunc(realm).apply(entity);

        // TODO: Sending an event should be extracted to store layer
        session.getKeycloakSessionFactory().publish((ClientModel.ClientCreationEvent) () -> resource);
        resource.updateClient();        // This is actualy strange contract - it should be the store code to call updateClient

        return resource;
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream(RealmModel realm) {
        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                 .compare(SearchableFields.ALWAYS_DISPLAY_IN_CONSOLE, Operator.EQ, Boolean.TRUE);
        return tx.read(withCriteria(mcb).orderBy(SearchableFields.CLIENT_ID, ASCENDING))
                  .map(entityToAdapterFunc(realm));
    }

    @Override
    public void removeClients(RealmModel realm) {
        LOG.tracef("removeClients(%s)%s", realm, getShortStackTrace());

        getClientsStream(realm)
          .map(ClientModel::getId)
          .collect(Collectors.toSet())  // This is necessary to read out all the client IDs before removing the clients
          .forEach(cid -> removeClient(realm, cid));
    }

    @Override
    public boolean removeClient(RealmModel realm, String id) {
        if (id == null) {
            return false;
        }

        LOG.tracef("removeClient(%s, %s)%s", realm, id, getShortStackTrace());

        // TODO: Sending an event (and client role removal) should be extracted to store layer
        final ClientModel client = getClientById(realm, id);
        if (client == null) return false;
        session.users().preRemove(realm, client);
        session.roles().removeRoles(client);

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
        // TODO: ^^^^^^^ Up to here

        tx.delete(id);

        return true;
    }

    @Override
    public long getClientsCount(RealmModel realm) {
        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return tx.getCount(withCriteria(mcb));
    }

    @Override
    public ClientModel getClientById(RealmModel realm, String id) {
        if (id == null) {
            return null;
        }

        LOG.tracef("getClientById(%s, %s)%s", realm, id, getShortStackTrace());

        MapClientEntity entity = tx.read(id);
        return (entity == null || ! entityRealmFilter(realm).test(entity))
          ? null
          : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public ClientModel getClientByClientId(RealmModel realm, String clientId) {
        if (clientId == null) {
            return null;
        }
        LOG.tracef("getClientByClientId(%s, %s)%s", realm, clientId, getShortStackTrace());

        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(SearchableFields.CLIENT_ID, Operator.EQ, clientId);

        return tx.read(withCriteria(mcb))
          .map(entityToAdapterFunc(realm))
          .findFirst()
          .orElse(null)
        ;
    }

    @Override
    public ClientModel getClientBySandboxClientId(RealmModel realm, String sandboxClientId) {
        if (sandboxClientId == null) {
            return null;
        }
        LOG.tracef("getClientBySandboxClientId(%s, %s)%s", realm, sandboxClientId, getShortStackTrace());

        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(SearchableFields.SANDBOX_CLIENT_ID, Operator.EQ, sandboxClientId);

        return tx.read(withCriteria(mcb))
                .map(entityToAdapterFunc(realm))
                .findFirst()
                .orElse(null)
                ;
    }

    @Override
    public Stream<ClientModel> searchClientsByClientIdStream(RealmModel realm, String clientId, Integer firstResult, Integer maxResults) {
        if (clientId == null) {
            return Stream.empty();
        }

        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(SearchableFields.CLIENT_ID, Operator.ILIKE, "%" + clientId + "%");

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.CLIENT_ID))
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<ClientModel> searchClientsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            mcb = mcb.compare(SearchableFields.ATTRIBUTE, Operator.EQ, entry.getKey(), entry.getValue());
        }

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.CLIENT_ID))
                .map(entityToAdapterFunc(realm));
    }

    @Override
    public Stream<ClientModel> searchClientsByClientNameStream(RealmModel realm, String clientName, Integer firstResult, Integer maxResults) {
        return null;
    }

    @Override
    public void addClientScopes(RealmModel realm, ClientModel client, Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        final String id = client.getId();
        MapClientEntity entity = tx.read(id);

        if (entity == null) return;

        // Defaults to openid-connect
        String clientProtocol = client.getProtocol() == null ? OIDCLoginProtocol.LOGIN_PROTOCOL : client.getProtocol();

        LOG.tracef("addClientScopes(%s, %s, %s, %b)%s", realm, client, clientScopes, defaultScope, getShortStackTrace());

        Map<String, ClientScopeModel> existingClientScopes = getClientScopes(realm, client, true);
        existingClientScopes.putAll(getClientScopes(realm, client, false));

        clientScopes.stream()
                .filter(clientScope -> ! existingClientScopes.containsKey(clientScope.getName()))
                .filter(clientScope -> Objects.equals(clientScope.getProtocol(), clientProtocol))
                .forEach(clientScope -> entity.setClientScope(clientScope.getId(), defaultScope));
    }

    @Override
    public void removeClientScope(RealmModel realm, ClientModel client, ClientScopeModel clientScope) {
        final String id = client.getId();
        MapClientEntity entity = tx.read(id);

        if (entity == null) return;

        LOG.tracef("removeClientScope(%s, %s, %s)%s", realm, client, clientScope, getShortStackTrace());

        entity.removeClientScope(clientScope.getId());
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(RealmModel realm, ClientModel client, boolean defaultScopes) {
        final String id = client.getId();
        MapClientEntity entity = tx.read(id);

        if (entity == null) return null;

        // Defaults to openid-connect
        String clientProtocol = client.getProtocol() == null ? OIDCLoginProtocol.LOGIN_PROTOCOL : client.getProtocol();

        LOG.tracef("getClientScopes(%s, %s, %b)%s", realm, client, defaultScopes, getShortStackTrace());

        return entity.getClientScopes(defaultScopes)
                .map(clientScopeId -> session.clientScopes().getClientScopeById(realm, clientScopeId))
                .filter(Objects::nonNull)
                .filter(clientScope -> Objects.equals(clientScope.getProtocol(), clientProtocol))
                .collect(Collectors.toMap(ClientScopeModel::getName, Function.identity()));
    }

    @Override
    public Map<ClientModel, Set<String>> getAllRedirectUrisOfEnabledClients(RealmModel realm) {
        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(SearchableFields.ENABLED, Operator.EQ, Boolean.TRUE);

        try (Stream<MapClientEntity> st = tx.read(withCriteria(mcb))) {
            return st
              .filter(mce -> mce.getRedirectUris() != null && ! mce.getRedirectUris().isEmpty())
              .collect(Collectors.toMap(
                mce -> entityToAdapterFunc(realm).apply(mce),
                mce -> new HashSet<>(mce.getRedirectUris()))
              );
        }
    }

    public void preRemove(RealmModel realm, RoleModel role) {
        DefaultModelCriteria<ClientModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId())
                .compare(SearchableFields.SCOPE_MAPPING_ROLE, Operator.EQ, role.getId());

        try (Stream<MapClientEntity> toRemove = tx.read(withCriteria(mcb))) {
            toRemove
                .map(clientEntity -> session.clients().getClientById(realm, clientEntity.getId()))
                .filter(Objects::nonNull)
                .forEach(clientModel -> clientModel.deleteScopeMapping(role));
        }
    }

    @Override
    public void close() {

    }

}

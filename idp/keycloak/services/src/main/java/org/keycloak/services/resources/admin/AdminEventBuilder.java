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

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdminEventBuilder {

    protected static final Logger logger = Logger.getLogger(AdminEventBuilder.class);

    private EventStoreProvider store;
    private KeycloakSession session;
    private Map<String, EventListenerProvider> listeners;
    private RealmModel realm;
    private AdminEvent adminEvent;

    public AdminEventBuilder(RealmModel realm, AdminAuth auth, KeycloakSession session, ClientConnection clientConnection) {
        this.realm = realm;
        this.session = session;
        adminEvent = new AdminEvent();

        this.listeners = new HashMap<>();
        updateStore(session);
        addListeners(session);
        if(auth != null) {
            authRealm(auth.getRealm());
            authClient(auth.getClient());
            authUser(auth.getUser());
        }
        if (clientConnection != null) {
            authIpAddress(clientConnection.getRemoteAddr());
            detail("browser", clientConnection.getClientBrowser());
            detail("os", clientConnection.getClientOs());
            /*IPResponse ipResponse = clientConnection.getClientLocation();
            if(ipResponse.getStatus() == 200)
            {
                detail("lat",String.valueOf(ipResponse.getBody().getLatitude()));
                detail("long",String.valueOf(ipResponse.getBody().getLongitude()));
            }
            detail("location",ipResponse.getLocation());*/
        }
    }

    public AdminEventBuilder(RealmModel realm, AuthDetails auth, KeycloakSession session, ClientConnection clientConnection) {
        this.realm = realm;
        this.session = session;
        adminEvent = new AdminEvent();

        this.listeners = new HashMap<>();
        updateStore(session);
        addListeners(session);
        if(auth != null) {
            adminEvent.setAuthDetails(auth);
        }
        if (clientConnection != null) {
            authIpAddress(clientConnection.getRemoteAddr());
            detail("browser", clientConnection.getClientBrowser());
            detail("os", clientConnection.getClientOs());
            /*IPResponse ipResponse = clientConnection.getClientLocation();
            if(ipResponse.getStatus() == 200)
            {
                detail("lat",String.valueOf(ipResponse.getBody().getLatitude()));
                detail("long",String.valueOf(ipResponse.getBody().getLongitude()));
            }
            detail("location",ipResponse.getLocation());*/
        }
    }

    public AdminEventBuilder realm(RealmModel realm) {
        adminEvent.setRealmId(realm.getId());
        return this;
    }

    public AdminEventBuilder realm(String realmId) {
        adminEvent.setRealmId(realmId);
        return this;
    }

    /**
     * Refreshes the builder assuming that the realm event information has
     * changed. Thought to be used when the updateRealmEventsConfig has
     * modified the events configuration. Now the store and the listeners are
     * updated to have previous and new setup.
     *
     * @param session The session
     * @return The same builder
     */
    public AdminEventBuilder refreshRealmEventsConfig(KeycloakSession session) {
        return this.updateStore(session).addListeners(session);
    }

    private AdminEventBuilder updateStore(KeycloakSession session) {
        if (realm.isAdminEventsEnabled() && store == null) {
            this.store = session.getProvider(EventStoreProvider.class);
            if (store == null) {
                ServicesLogger.LOGGER.noEventStoreProvider();
            }
        }
        return this;
    }

    private AdminEventBuilder addListeners(KeycloakSession session) {
        realm.getEventsListenersStream()
                .filter(((Predicate<String>) listeners::containsKey).negate())
                .forEach(id -> {
                    EventListenerProvider listener = session.getProvider(EventListenerProvider.class, id);
                    if (listener != null) {
                        listeners.put(id, listener);
                    } else {
                        ServicesLogger.LOGGER.providerNotFound(id);
                    }
                });
        return this;
    }

    public AdminEventBuilder operation(OperationType operationType) {
        adminEvent.setOperationType(operationType);
        return this;
    }

    public AdminEventBuilder resource(ResourceType resourceType) {
        adminEvent.setResourceType(resourceType);
        return this;
    }

    /**
     * Setter for custom resource types with values different from {@link ResourceType}.
     */
    public AdminEventBuilder resource(String resourceType) {
        adminEvent.setResourceTypeAsString(resourceType);
        return this;
    }

    public AdminEventBuilder authRealm(RealmModel realm) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if (authDetails == null) {
            authDetails = new AuthDetails();
        }
        authDetails.setRealm(ModelToRepresentation.toLogRepresentation(realm));
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder authClient(ClientModel client) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if (authDetails == null) {
            authDetails = new AuthDetails();
        }
        authDetails.setClient(ModelToRepresentation.toLogRepresentation(client));
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder authUser(UserModel user) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if (authDetails == null) {
            authDetails = new AuthDetails();
        }
        if (user != null) {
            authDetails.setUser(ModelToRepresentation.toLogRepresentation(user));
        }
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder authIpAddress(String ipAddress) {
        AuthDetails authDetails = adminEvent.getAuthDetails();
        if (authDetails == null) {
            authDetails = new AuthDetails();
        }
        authDetails.setIpAddress(ipAddress);
        adminEvent.setAuthDetails(authDetails);
        return this;
    }

    public AdminEventBuilder resourcePath(String... pathElements) {
        StringBuilder sb = new StringBuilder();
        for (String element : pathElements) {
            sb.append("/");
            sb.append(element);
        }
        if (pathElements.length > 0) sb.deleteCharAt(0); // remove leading '/'

        adminEvent.setResourcePath(sb.toString());
        return this;
    }

    public AdminEventBuilder resourcePath(UriInfo uriInfo) {
        String path = getResourcePath(uriInfo);
        adminEvent.setResourcePath(path);
        return this;
    }

    public AdminEventBuilder resourcePath(UriInfo uriInfo, String id) {
        StringBuilder sb = new StringBuilder();
        sb.append(getResourcePath(uriInfo));
        sb.append("/");
        sb.append(id);
        adminEvent.setResourcePath(sb.toString());
        return this;
    }

    public AdminEventBuilder error(String error)
    {
        adminEvent.setError(error);
        return this;
    }

    private String getResourcePath(UriInfo uriInfo) {
        String path = uriInfo.getPath();

        StringBuilder sb = new StringBuilder();
        sb.append("/realms/");
        sb.append(realm.getName());
        sb.append("/");
        String realmRelative = sb.toString();

        return path.substring(path.indexOf(realmRelative) + realmRelative.length());
    }

    public AdminEventBuilder representation(Object value) {
        if (value == null || value.equals("")) {
            return this;
        }
        try {
            String json = JsonSerialization.writeValueAsString(value);
            adminEvent.setRepresentation(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    public AdminEventBuilder detail(String key, String value) {
        if (value == null || value.equals("")) {
            return this;
        }

        if (adminEvent.getDetails() == null) {
            adminEvent.setDetails(new HashMap<>());
        }
        adminEvent.getDetails().put(key, value);
        return this;
    }

    /**
     * Add event detail where strings from the input Collection are filtered not to contain <code>null</code> and then joined using <code>::</code> character.
     *
     * @param key of the detail
     * @return builder for chaining
     */
    public AdminEventBuilder detail(String key, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }
        return detail(key, values.stream().filter(Objects::nonNull).collect(Collectors.joining("::")));
    }

    /**
     * Add event detail where strings from the input Stream are filtered not to contain <code>null</code> and then joined using <code>::</code> character.
     *
     * @param key of the detail
     * @return builder for chaining
     */
    public AdminEventBuilder detail(String key, Stream<String> values) {
        if (values == null) {
            return this;
        }
        return detail(key, values.filter(Objects::nonNull).collect(Collectors.joining("::")));
    }


    public AdminEventBuilder removeDetail(String key) {
        if (adminEvent.getDetails() != null) {
            adminEvent.getDetails().remove(key);
        }
        return this;
    }

    public AdminEvent getEvent() {
        return adminEvent;
    }

    public void success() {
        send();
    }

    private void send() {
        boolean includeRepresentation = realm.isAdminEventsDetailsEnabled();
        boolean isEventsEnabled = realm.isAdminEventsEnabled();

        if(!isEventsEnabled)
            return;

        // Event needs to be copied because the same builder can be used with another event
        AdminEvent eventCopy = new AdminEvent(adminEvent);
        if (adminEvent.getRealmId() == null) eventCopy.setRealmId(realm.getId());
        eventCopy.setTime(Time.currentTimeMillis());
        eventCopy.setId(UUID.randomUUID().toString());

        if (store == null) {
            this.store = session.getProvider(EventStoreProvider.class);
            if (store == null) {
                ServicesLogger.LOGGER.noEventStoreProvider();
            }
        }
        if (listeners != null) {
            for (EventListenerProvider l : listeners.values()) {
                try {
                    Map<String, String> config = getConfig(l);
                    l.onEvent(eventCopy, config, includeRepresentation);
                } catch (Throwable t) {
                    ServicesLogger.LOGGER.failedToSendType(t, l);
                }
            }
        }

        if (store != null) {
            store.onEvent(eventCopy, null, includeRepresentation);
        }


    }

    private Map<String, String> getConfig(EventListenerProvider l) {
        Map<String, String> config = null;
        if (l.getFactory().isConfigurable()) {
            EventConfigModel eventConfigModel = realm.getEventConfigByProviderId(l.getFactory().getId());
            if (eventConfigModel != null) {
                config = eventConfigModel.getConfig();
            }
        }
        return config;
    }

}

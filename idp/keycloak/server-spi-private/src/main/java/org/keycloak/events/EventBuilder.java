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

package org.keycloak.events;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.models.IPResponse;
import org.keycloak.common.util.Time;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EventBuilder {

    private static final Logger log = Logger.getLogger(EventBuilder.class);

    private EventStoreProvider store;
    private KeycloakSession session;
    private List<EventListenerProvider> listeners;
    private RealmModel realm;
    private Event event;

    public EventBuilder(RealmModel realm, KeycloakSession session, ClientConnection clientConnection) {
        this.realm = realm;
        this.session = session;

        event = new Event();

        if (realm.isEventsEnabled()) {
            EventStoreProvider store = session.getProvider(EventStoreProvider.class);
            if (store != null) {
                this.store = store;
            } else {
                log.error("Events enabled, but no event store provider configured");
            }
        }


        this.listeners = realm.getEventsListenersStream()
                .map(id -> {
                    EventListenerProvider listener = session.getProvider(EventListenerProvider.class, id);
                    if (listener != null) {
                        return listener;
                    } else {
                        log.error("Event listener '" + id + "' registered, but provider not found");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        realm(realm);
        if(clientConnection != null) {
            ipAddress(clientConnection.getRemoteAddr());
            if(clientConnection.getClientBrowser().equalsIgnoreCase("twilio")){
                detail("provider",clientConnection.getClientBrowser());
            }
            else {
                detail("browser", clientConnection.getClientBrowser());
                detail("os", clientConnection.getClientOs());
            }
            /*IPResponse ipResponse = clientConnection.getClientLocation();
            if(ipResponse.getStatus() == 200)
            {
                detail("lat",String.valueOf(ipResponse.getBody().getLatitude()));
                detail("long",String.valueOf(ipResponse.getBody().getLongitude()));
            }
            detail("location",ipResponse.getLocation());*/
        }
    }

    private EventBuilder(EventStoreProvider store, List<EventListenerProvider> listeners, RealmModel realm, Event event) {
        this.store = store;
        this.listeners = listeners;
        this.realm = realm;
        this.event = event;
    }

    public EventBuilder realm(RealmModel realm) {
        event.setRealm(realm == null ? null : ModelToRepresentation.toLogRepresentation(realm));
        return this;
    }

    public EventBuilder client(ClientModel client) {
        event.setClient(client == null ? null : ModelToRepresentation.toLogRepresentation(client));
        return this;
    }

    public EventBuilder client(String clId) {
        event.setClient(new ClientRepresentation(){{
            ClientModel client = session != null ? session.clients().getClientByClientId(realm, clId) : null;
            setClientId((client != null && client.isSandboxMode() != null && client.isSandboxMode()) ?
                    client.getSandboxClientId() : clId);
        }});
        return this;
    }

    public EventBuilder user(UserModel user) {
        event.setUser(user == null ? null : ModelToRepresentation.toLogRepresentation(user));
        return this;
    }

    public EventBuilder user(String userId){
        event.setUser(new UserRepresentation(){{
            setId(userId);
        }});
        return this;
    }

    public EventBuilder session(UserSessionModel session) {
        event.setSessionId(session == null ? null : session.getId());
        return this;
    }

    public EventBuilder session(String sessionId) {
        event.setSessionId(sessionId);
        return this;
    }

    public EventBuilder ipAddress(String ipAddress) {
        event.setIpAddress(ipAddress);
        return this;
    }

    public EventBuilder event(EventType e) {
        event.setType(e);
        return this;
    }

    public EventBuilder detail(String key, String value) {
        if (value == null || value.equals("")) {
            return this;
        }

        if (event.getDetails() == null) {
            event.setDetails(new HashMap<>());
        }
        event.getDetails().put(key, value);
        return this;
    }

    /**
     * Add event detail where strings from the input Collection are filtered not to contain <code>null</code> and then joined using <code>::</code> character.
     *
     * @param key of the detail
     * @param value, can be null
     * @return builder for chaining
     */
    public EventBuilder detail(String key, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }
        return detail(key, values.stream().filter(Objects::nonNull).collect(Collectors.joining("::")));
    }

    /**
     * Add event detail where strings from the input Stream are filtered not to contain <code>null</code> and then joined using <code>::</code> character.
     *
     * @param key of the detail
     * @param value, can be null
     * @return builder for chaining
     */
    public EventBuilder detail(String key, Stream<String> values) {
        if (values == null) {
            return this;
        }
        return detail(key, values.filter(Objects::nonNull).collect(Collectors.joining("::")));
    }


    public EventBuilder removeDetail(String key) {
        if (event.getDetails() != null) {
            event.getDetails().remove(key);
        }
        return this;
    }

    public Event getEvent() {
        return event;
    }

    public void success() {
        System.out.println("------------INCOMING EVENT SUCCESS--------------");
        System.out.println(this.event.getType().name());
        System.out.println("------------INCOMING EVENT SUCCESS--------------");
        send();
    }

    public void error(String error) {
        if (Objects.isNull(event.getType())) {
            throw new IllegalStateException("Attempted to define event error without first setting the event type");
        }

        if (!event.getType().name().endsWith("_ERROR")) {
            event.setType(EventType.valueOf(event.getType().name() + "_ERROR"));
        }
        event.setError(error);
        System.out.println("------------INCOMING EVENT SUCCESS--------------");
        System.out.println(this.event.getType().name());
        System.out.println("------------INCOMING EVENT SUCCESS--------------");
        send();
    }

    public EventBuilder clone() {
        return new EventBuilder(store, listeners, realm, event.clone());
    }

    private void send() {
        boolean isEventsEnabled = realm.isEventsEnabled();

        if(!isEventsEnabled)
            return;

        event.setTime(Time.currentTimeMillis());

        if(store == null){
            EventStoreProvider store = session.getProvider(EventStoreProvider.class);
            if (store != null) {
                this.store = store;
            } else {
                log.error("Events enabled, but no event store provider configured");
            }
        }

        if (listeners != null) {
            for (EventListenerProvider l : listeners) {
                try {
                    Map<String, String> config = getConfig(l);
                    l.onEvent(event,config);
                } catch (Throwable t) {
                    log.error("Failed to send type to " + l, t);
                }
            }
        }

        if (store != null) {
            if(store.enableEventFilter()) {
                Set<String> eventTypes = realm.getEnabledEventTypesStream().collect(Collectors.toSet());
                if (!eventTypes.isEmpty() ? eventTypes.contains(event.getType().name()) : event.getType().isSaveByDefault()) {
                    try {
                        store.onEvent(event, null);
                    } catch (Throwable t) {
                        log.error("Failed to save event", t);
                    }
                }
            }
            else
            {
                try {
                    store.onEvent(event, null);
                } catch (Throwable t) {
                    log.error("Failed to save event", t);
                }
            }
        }


    }

    private Map<String, String> getConfig(EventListenerProvider l) {
        Map<String,String> config = null;
        if(l.getFactory().isConfigurable())
        {
            EventConfigModel eventConfigModel;
            if(session != null)
                eventConfigModel = session.getContext().getRealm().getEventConfigByProviderId(l.getFactory().getId());
            else
                eventConfigModel = realm.getEventConfigByProviderId(l.getFactory().getId());
            if(eventConfigModel != null)
            {
                config = eventConfigModel.getConfig();
            }
        }
        return config;
    }

}

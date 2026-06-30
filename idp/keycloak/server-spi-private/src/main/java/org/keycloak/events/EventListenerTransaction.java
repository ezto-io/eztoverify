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

package org.keycloak.events;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.utils.TriConsumer;
import org.keycloak.models.AbstractKeycloakTransaction;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EventListenerTransaction extends AbstractKeycloakTransaction {

    private static class AdminEventEntry {
        private final AdminEvent event;
        private Map<String,String> config;
        private final boolean includeRepresentation;

        public AdminEventEntry(AdminEvent event, Map<String,String> config, boolean includeRepresentation) {
            this.event = event;
            this.config = config;
            this.includeRepresentation = includeRepresentation;
        }
    }

    private static class EventEntry {
        private final Event event;
        private Map<String,String> config;

        public EventEntry(Event event, Map<String,String> config) {
            this.event = event;
            this.config = config;
        }
    }

    private final List<AdminEventEntry> adminEventsToSend = new LinkedList<>();
    private final List<EventEntry> eventsToSend = new LinkedList<>();
    private final TriConsumer<AdminEvent,Map, Boolean> adminEventConsumer;
    private final BiConsumer<Event,Map> eventConsumer;

    public EventListenerTransaction(TriConsumer<AdminEvent, Map, Boolean> adminEventConsumer, BiConsumer<Event,Map> eventConsumer) {
        this.adminEventConsumer = adminEventConsumer;
        this.eventConsumer = eventConsumer;
    }

    public void addAdminEvent(AdminEvent adminEvent,Map<String,String> config, boolean includeRepresentation) {
        adminEventsToSend.add(new AdminEventEntry(adminEvent,config, includeRepresentation));
    }

    public void addEvent(Event event,Map<String,String> config) {
        eventsToSend.add(new EventEntry(event,config));
    }

    @Override
    protected void commitImpl() {
        adminEventsToSend.forEach(this::consumeAdminEventEntry);
        eventsToSend.forEach(this::consumeEventEntry);
    }

    private void consumeAdminEventEntry(AdminEventEntry entry) {
        if (adminEventConsumer != null) {
            adminEventConsumer.accept(entry.event,entry.config, entry.includeRepresentation);
        }
    }

    private void consumeEventEntry(EventEntry entry) {
        if (eventConsumer != null) {
            eventConsumer.accept(entry.event,entry.config);
        }
    }

    @Override
    protected void rollbackImpl() {
        adminEventsToSend.clear();
        eventsToSend.clear();
    }


}

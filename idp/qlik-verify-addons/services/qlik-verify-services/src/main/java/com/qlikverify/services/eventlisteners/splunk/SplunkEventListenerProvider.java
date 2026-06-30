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

package com.qlikverify.services.eventlisteners.splunk;

import com.qlikverify.services.eventlisteners.splunk.models.SplunkAdminEvent;
import com.qlikverify.services.eventlisteners.splunk.models.SplunkEvent;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SplunkEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;
    private SplunkEventListenerProviderFactory factory;
    private static final Logger logger = Logger.getLogger(SplunkEventListenerProvider.class);
    HttpUtility httpUtility;

    public SplunkEventListenerProvider(KeycloakSession session, SplunkEventListenerProviderFactory factory) {
        this.session = session;
        this.factory = factory;
        this.httpUtility = new HttpUtility();
    }

    @Override
    public void onEvent(Event event, Map<String, String> config) {
        if (config != null) {
            String events = config.get("SET_EVENTS");
            if (events != null) {
                List<String> listEvents = Arrays.asList(events.split("##"));
                if (listEvents.contains(event.getType().name())) {
                    logEvent(event, config);
                }
            }
        }
    }

    @Override
    public void onEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        if (config != null) {
            logAdminEvent(event, config, includeRepresentation);
        }
    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return factory;
    }

    private void logEvent(Event event, Map<String, String> config) {
        try {
            SplunkEvent splunkEvent = new SplunkEvent(event);
            SplunkProducer.publishEvent(session, httpUtility, config, JsonSerialization.writeValueAsString(splunkEvent));
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void logAdminEvent(AdminEvent adminEvent, Map<String, String> config, boolean includeRepresentation) {
        String isAdminEventEnabled = config.get("SEND_ADMIN_EVENT");
        if (!Boolean.parseBoolean(isAdminEventEnabled)) {
            return;
        }

        try {
            SplunkAdminEvent splunkAdminEvent = new SplunkAdminEvent(adminEvent);
            SplunkProducer.publishEvent(session, httpUtility, config, JsonSerialization.writeValueAsString(splunkAdminEvent));
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void close() {
        // NOP
    }

}

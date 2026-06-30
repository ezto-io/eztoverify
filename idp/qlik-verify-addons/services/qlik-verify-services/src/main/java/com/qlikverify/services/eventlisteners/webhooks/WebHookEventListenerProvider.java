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

package com.qlikverify.services.eventlisteners.webhooks;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventListenerTransaction;
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
public class WebHookEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;
    private WebHookEventListenerProviderFactory factory;
    HttpUtility httpUtility;
    private final EventListenerTransaction tx = new EventListenerTransaction(this::logAdminEvent, this::logEvent);
    private static final Logger logger = Logger.getLogger(WebHookEventListenerProvider.class);

    public WebHookEventListenerProvider(KeycloakSession session, WebHookEventListenerProviderFactory factory) {
        this.session = session;
        this.factory = factory;
        this.httpUtility = new HttpUtility();
        this.session.getTransactionManager().enlistAfterCompletion(tx);
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
            WebHookProducer.publishEvent(session, httpUtility, config.get("WEBHOOK_URL"), config.get("AUTHORIZATION"), JsonSerialization.writeValueAsString(event));
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
            WebHookProducer.publishEvent(session, httpUtility, config.get("WEBHOOK_URL"), config.get("AUTHORIZATION"), JsonSerialization.writeValueAsString(adminEvent));
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void close() {
        // NOP
    }

}

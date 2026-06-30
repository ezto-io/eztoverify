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

package org.keycloak.events.email;

import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EmailEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Set<EventType> SUPPORTED_EVENTS = new HashSet<>();
    static {
        Collections.addAll(SUPPORTED_EVENTS, EventType.LOGIN_ERROR, EventType.UPDATE_PASSWORD, EventType.REMOVE_TOTP, EventType.UPDATE_TOTP);
    }

    private Set<EventType> includedEvents = new HashSet<>();

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        EmailTemplateProvider emailTemplateProvider = session.getProvider(EmailTemplateProvider.class);
        return new EmailEventListenerProvider(session,emailTemplateProvider, this);
    }

    @Override
    public void init(Config.Scope config) {
        String[] include = config.getArray("include-events");
        if (include != null) {
            for (String i : include) {
                includedEvents.add(EventType.valueOf(i.toUpperCase()));
            }
        } else {
            includedEvents.addAll(SUPPORTED_EVENTS);
        }

        String[] exclude = config.getArray("exclude-events");
        if (exclude != null) {
            for (String e : exclude) {
                includedEvents.remove(EventType.valueOf(e.toUpperCase()));
            }
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "email";
    }

    @Override
    public String getDisplayName() {
        return "Email Listener";
    }

    @Override
    public String getLogo() {
        return null;
    }

    @Override
    public String getHowTodo() {
        return null;
    }
    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public boolean isInBuilt() {
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean isRequiredTestConfigDetails() {
        return false;
    }

    @Override
    public boolean testConfig(KeycloakSession session, Map<String, String> config) {
        return false;
    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        String[] supportedEvents = Arrays.stream(EventType.values())
                .map(EventType::name)
                .map(String::toLowerCase)
                .sorted(Comparator.naturalOrder())
                .toArray(String[]::new);
        return ProviderConfigurationBuilder.create()
                .property()
                .name("include-events")
                .type("string")
                .helpText("A comma-separated list of events that should be sent via email to the user's account.")
                .options(supportedEvents)
                .defaultValue("All events")
                .add()
                .property()
                .name("exclude-events")
                .type("string")
                .helpText("A comma-separated list of events that should not be sent via email to the user's account.")
                .options(supportedEvents)
                .add()
                .build();
    }

    @Override
    public String[] supportedModes() {
        return new String[]{"bo", "ciam", "wiam"};
    }
}

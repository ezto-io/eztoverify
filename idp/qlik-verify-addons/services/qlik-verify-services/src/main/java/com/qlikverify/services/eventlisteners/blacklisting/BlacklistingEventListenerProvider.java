package com.qlikverify.services.eventlisteners.blacklisting;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

import java.util.Arrays;
import java.util.Map;

public class BlacklistingEventListenerProvider implements EventListenerProvider {
    private final KeycloakSession session;
    private final BlacklistingEventListenerProviderFactory factory;

    public BlacklistingEventListenerProvider(KeycloakSession session, BlacklistingEventListenerProviderFactory factory) {
        this.session = session;
        this.factory = factory;
    }

    @Override
    public void onEvent(Event event, Map<String, String> config) {
        if(Arrays.asList(factory.supportedModes()).contains(session.getContext().getRealm().getMode())) {
            new BlacklistingEventListenerResource(session, event).checkBlacklistConfig();
        }
    }

    @Override
    public void onEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        // NOP
    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return factory;
    }

    @Override
    public void close() {
        // NOP
    }
}

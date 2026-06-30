package com.qlikverify.services.events;

import org.keycloak.Config;
import org.keycloak.events.EventStoreProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class ElasticEventStoreProviderFactory implements EventStoreProviderFactory {

    public static final String ELASTIC_EVENT = "elastic-event";

    @Override
    public ElasticEventStoreProvider create(KeycloakSession session) {
        return new ElasticEventStoreProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return ELASTIC_EVENT;
    }

    @Override
    public int order(){
        return 1;
    }
}

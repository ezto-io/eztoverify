package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class MigrateTo4_6_0 implements QVMigration{

    public static final ModelVersion VERSION = new ModelVersion("4.6.0");

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream()
                .filter(realm -> !realm.getMode().equals("bo"))
                .forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+x.getName());
            migrateEventListener(x);
            System.out.println("MIGRATION COMPLETED FOR :"+x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }

    private void migrateEventListener(RealmModel x) {
        List<String> eventListeners = x.getEventsListenersStream().collect(Collectors.toList());
        if (!eventListeners.contains("black-listing-listener")) {
            eventListeners.add("black-listing-listener");
            x.setEventsListeners(new HashSet<>(eventListeners));
        }
        if (!eventListeners.contains("workflows-event-listener")) {
            eventListeners.add("workflows-event-listener");
            x.setEventsListeners(new HashSet<>(eventListeners));
        }
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {
        // NOP
    }
}


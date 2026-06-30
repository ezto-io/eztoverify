package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.events.EventCollection;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEventCollection;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class MigrateTo3_8_0 implements QVMigration{

    public static final ModelVersion VERSION = new ModelVersion("3.8.0");
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+x.getName());
            migrateLogs(session,x);
            System.out.println("MIGRATION COMPLETED FOR :"+x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }
    private void migrateLogs(KeycloakSession session, RealmModel x) {

        List<String> eventListeners = x.getEventsListenersStream().collect(Collectors.toList());
        if (!eventListeners.contains("report-listener")) {
            eventListeners.add("report-listener");
            x.setEventsListeners(new HashSet<String>(eventListeners));
        }
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {

    }
}

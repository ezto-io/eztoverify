package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class MigrateTo8_5_0 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("8.5.0");
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream()
                .filter(realm -> realm.getMode().equals("bo"))
                .forEach(x -> {
                    System.out.println("-------------------------------");
                    System.out.println("MIGRATION STARTED FOR :"+x.getName());
                    addUserLastLoginEventListener(x);
                    System.out.println("MIGRATION COMPLETED FOR :"+x.getName());
                    System.out.println("-------------------------------");
                });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }

    private void addUserLastLoginEventListener(RealmModel x) {
        List<String> eventListeners = x.getEventsListenersStream().collect(Collectors.toList());
        if (!eventListeners.contains("user-last-login-listener")) {
            eventListeners.add("user-last-login-listener");
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

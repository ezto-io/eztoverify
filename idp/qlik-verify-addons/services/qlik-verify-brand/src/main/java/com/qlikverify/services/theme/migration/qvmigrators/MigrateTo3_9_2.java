package com.qlikverify.services.theme.migration.qvmigrators;

import org.jboss.logging.Logger;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.keycloak.models.Constants.DEFAULT_PASSWORD_POLICY;
import static org.keycloak.models.utils.DefaultAuthenticationFlows.REGISTRATION_FLOW;

public class MigrateTo3_9_2 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("3.9.2");
    private static Logger logger = Logger.getLogger(MigrateTo3_9_2.class);
    private static Executor executor = Executors.newCachedThreadPool();

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :" + x.getName());
            addNewDefaultEventListeners(session,x);
            System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
    }


    private void addNewDefaultEventListeners(KeycloakSession session, RealmModel realm){
        Set<String> listeners = realm.getEventsListenersStream().collect(Collectors.toSet());
        if(listeners != null){
            listeners.add("google-ws-sync");
            listeners.add("duo-ws-sync");
            listeners.add("okta-ws-sync");
        }
        realm.setEventsListeners(listeners);
    }
    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {

    }
}

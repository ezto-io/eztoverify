package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashSet;
import java.util.Set;

public class MigrateTo7_0_1 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("7.0.1");

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().filter(realm -> realm.getMode().equalsIgnoreCase("bo")).forEach(realm -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+realm.getName());
            addDefaultWorkflowLocale(realm);
            System.out.println("MIGRATION COMPLETED FOR :"+realm.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }

    private void addDefaultWorkflowLocale(RealmModel realm){
        if(realm.getPreferredLoginLocalesStream().count() == 0){
            Set<String> defaultLocale = new HashSet<>();
            defaultLocale.add("en");
            realm.setPreferredLoginLocales(defaultLocale);
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

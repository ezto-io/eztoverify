package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.CibaNotificationConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Map;

public class MigrateTo8_14_0 implements QVMigration {
    public static final ModelVersion VERSION = new ModelVersion("8.14.0");

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream()
                .filter(realm -> realm.getMode().equals("bo"))
                .forEach(x -> {
                    System.out.println("-------------------------------");
                    System.out.println("MIGRATION STARTED FOR :" + x.getName());
                    setCibaNotificationAttribute(session, x);
                    System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
                    System.out.println("-------------------------------");
                });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {
        // NOP
    }

    private void setCibaNotificationAttribute(KeycloakSession session, RealmModel realm) {
        realm.getCibaNotificationConfigsStream().forEach(x-> {
            CibaNotificationConfigModel cibaNotificationConfigModel = x;
            Map<String, String> config = cibaNotificationConfigModel.getConfig();
            config.put("result-version", "v0");
            cibaNotificationConfigModel.setConfig(config);
            realm.updateCibaNotificationConfig(cibaNotificationConfigModel);
        });

    }

}
package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class MigrateTo8_13_0 implements QVMigration {
    public static final ModelVersion VERSION = new ModelVersion("8.13.0");

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream()
                .filter(realm -> realm.getMode().equals("bo"))
                .forEach(x -> {
                    System.out.println("-------------------------------");
                    System.out.println("MIGRATION STARTED FOR :" + x.getName());
                    updateBranding(session, x);
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

    private void updateBranding(KeycloakSession session, RealmModel realm) {
        session.loginBranding().getAllBrands(realm, -1, -1).forEach(x -> {
            x.setAttribute("textBoxTextColor", "#0b2238");
            x.setAttribute("formTextColor", "#0b2238");
            x.setAttribute("textBoxBgColor", "#ffffff");
            session.loginBranding().updateBranding(realm, x.getId(), x.getAttributes());
        });

    }

}
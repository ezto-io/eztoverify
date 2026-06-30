package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;


public class MigrateTo8_7_0 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("8.7.0");
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream()
                .filter(realm -> realm.getMode().equals("bo"))
                .forEach(x -> {
                    System.out.println("-------------------------------");
                    System.out.println("MIGRATION STARTED FOR :"+x.getName());
                    changeAadhaarProiderId(x);
                    System.out.println("MIGRATION COMPLETED FOR :"+x.getName());
                    System.out.println("-------------------------------");
                });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }

    private void changeAadhaarProiderId(RealmModel realm) {
        realm.getRequiredActionProvidersStream()
                .filter(x -> (x.getProviderId() != null) && (x.getProviderId().contains("aadhar") || x.getProviderId().contains("adhaar")))
                .forEach(x -> {
                    x.setProviderId(x.getProviderId().replaceAll("aadhar|adhaar", "aadhaar"));
                    x.setAlias(x.getAlias().replaceAll("aadhar|adhaar", "aadhaar"));
                    realm.updateRequiredActionProvider(x);
                });
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

package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;

public class MigrateTo8_1_1 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("8.1.1");

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().forEach(realm -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :" + realm.getName());
            addCustomFontBrandConfig(session, realm);
            System.out.println("MIGRATION COMPLETED FOR :" + realm.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }

    private void addCustomFontBrandConfig(KeycloakSession session, RealmModel realmModel){
        session.loginBranding().getAllBrands(realmModel,-1,-1).forEach(x -> addConfigsForBranding(session, realmModel, x));
    }

    private void addConfigsForBranding(KeycloakSession session, RealmModel realm, BrandingModel brandingModel){

        brandingModel.setAttribute("fontPreference", "default");
        brandingModel.setAttribute("customFontPreference", "link");

        session.loginBranding().updateBranding(realm,brandingModel.getId(),brandingModel.getAttributes());

    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {

    }
}

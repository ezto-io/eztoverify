package com.qlikverify.services.theme.migration.qvmigrators;

import org.jboss.logging.Logger;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;

public class MigrateTo3_9_4 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("3.9.4");
    private static Logger logger = Logger.getLogger(MigrateTo3_9_4.class);

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
                System.out.println("-------------------------------");
                System.out.println("MIGRATION STARTED FOR :" + x.getName());
                addNewBrandConfig(session, x);
                System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
                System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
    }

    private void addNewBrandConfig(KeycloakSession session, RealmModel realmModel){
        session.loginBranding().getAllBrands(realmModel,-1,-1).forEach(x -> addConfigsForBranding(session, realmModel, x));
    }

    private void addConfigsForBranding(KeycloakSession session, RealmModel realm, BrandingModel brandingModel){

            brandingModel.setAttribute("formBorderColor", "#FFFFFF");
            brandingModel.setAttribute("formBorderWidth", "1");
            brandingModel.setAttribute("formBorderRadius", "10");
            brandingModel.setAttribute("showLogoInForm", "true");

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

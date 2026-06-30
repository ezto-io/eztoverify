package com.qlikverify.services.theme.migration.qvmigrators;

import org.jboss.logging.Logger;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSyncConfigModel;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MigrateTo3_9_3 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("3.9.3");
    private static Logger logger = Logger.getLogger(MigrateTo3_9_3.class);
    private static Executor executor = Executors.newCachedThreadPool();


    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :" + x.getName());
            changeUserSyncProviderID(session,x);
            System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
    }


    private void changeUserSyncProviderID(KeycloakSession session, RealmModel realm){
        List<UserSyncConfigModel> userSyncConfigModelList = realm.getUserSyncConfigsStream().filter(x -> x.getProviderId().equals("success")).collect(Collectors.toList());
        for (UserSyncConfigModel userSyncConfigModel : userSyncConfigModelList) {
            userSyncConfigModel.setProviderId("azure_ad");
            realm.updateUserSyncConfigModel(userSyncConfigModel);
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

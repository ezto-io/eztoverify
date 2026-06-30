package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.MgmtAccessModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MigrateTo7_0_0 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("7.0.0");
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().filter(realm -> realm.getMode().equalsIgnoreCase("bo")).forEach(realm -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+realm.getName());
            addVersionCompatibleForApps(session, realm);
            System.out.println("MIGRATION COMPLETED FOR :"+realm.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }

    private void addVersionCompatibleForApps(KeycloakSession session, RealmModel realm) {
        realm.getClientsStream().forEach(client -> {
            client.setAttribute("hidden_ua_multi_version","true",true);
        });

        MgmtAccessModel accessModel = new MgmtAccessModel();
        accessModel.setName("default");
        Map<String,Boolean> permissions = new HashMap<>();
        permissions.put("Devices@list", true);
        permissions.put("Devices@remove", true);
        permissions.put("Devices@enable",true);
        permissions.put("Devices@disable",true);
        permissions.put("Devices@mark-as-last-used",true);
        accessModel.setPermissions(permissions);
        accessModel.setRealmId(realm.getId());
        accessModel.setEnabled(true);
        Optional<UserModel> accountCreator = session.users().getUsersStream(realm,false).filter(UserModel::isAccountCreator).findFirst();
        accountCreator.ifPresent(userModel -> accessModel.setRequestedBy(userModel.getId()));
        realm.addMgmtAccess(accessModel);
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {

    }
}

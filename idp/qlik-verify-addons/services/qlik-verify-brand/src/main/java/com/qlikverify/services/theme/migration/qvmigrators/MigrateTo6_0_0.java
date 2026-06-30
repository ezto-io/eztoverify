package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.CibaAuthConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class MigrateTo6_0_0 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("6.0.0");
    String CIBA_AUTHORIZER ="ciba_auth";
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().forEach(realm -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+realm.getName());
            changeNotificationWayAsPush(session, realm);
            System.out.println("MIGRATION COMPLETED FOR :"+realm.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }

    private void changeNotificationWayAsPush(KeycloakSession session, RealmModel realm) {
        realm.getClientsStream().forEach(client -> {
            String providerId = client.getAuthenticationFlowBindingOverride(CIBA_AUTHORIZER);
            if(providerId != null && providerId.equals("ciba-mobile-sdk")){
                CibaAuthConfigModel config = realm.getCibaAuthConfigByProviderId(providerId, client.getClientId(), realm.getId());
                if(!config.getConfig().containsKey("push-way")){
                    config.getConfig().put("push-way", "Push");
                    realm.updateCibaAuthConfig(config);
                }
            }
        });
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {

    }
}

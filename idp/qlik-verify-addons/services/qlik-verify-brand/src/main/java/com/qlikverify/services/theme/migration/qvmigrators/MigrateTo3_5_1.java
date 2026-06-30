package com.qlikverify.services.theme.migration.qvmigrators;

import org.checkerframework.checker.units.qual.C;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;

import java.util.stream.Collectors;

public class MigrateTo3_5_1 implements QVMigration{

    public static final ModelVersion VERSION = new ModelVersion("3.5.1");
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+x.getName());
            updateSSOManagement(session,x);
            System.out.println("MIGRATION COMPLETED FOR :"+x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }
    private void updateSSOManagement(KeycloakSession session, RealmModel realm) {
        realm.getClientsStream().collect(Collectors.toList()).forEach(x -> {
            String clientName = x.getName();
            if (realm.getUserInviteClientId().equalsIgnoreCase(x.getClientId())) {
                x.setAttribute("inviteOnly", "true");
            } else {
                x.setAttribute("inviteOnly", "false");
            }
            if (clientName.equalsIgnoreCase(Constants.SSO_MANAGEMENT_CLIENT_ID)){
                x.setRegistrationAllowed(false);
                x.setAttribute("inviteOnly", "true");
                if (realm.getUserInviteClientId() == null) realm.setUserInviteClientId(x.getClientId());
                System.out.println(clientName +": UPDATED INVITE ONLY ATTRIBUTE");
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

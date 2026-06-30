package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class MigrateTo8_1_0 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("8.1.0");
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().filter(realm -> realm.getMode().equalsIgnoreCase("bo")).forEach(realm -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+realm.getName());
            addClientEndMessageEnabledAttribute(session, realm);
            System.out.println("MIGRATION COMPLETED FOR :"+realm.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");

    }

    private void addClientEndMessageEnabledAttribute(KeycloakSession session, RealmModel realm){
        realm.getClientsStream()
                .filter(client -> !client.getAttributes().containsKey("enableCustomOnboardingMsg") || !client.getAttributes().containsKey("enableCustomVerificationMsg"))
                .forEach(client -> {
                    client.setAttribute("enableCustomOnboardingMsg", "true");
                    client.setAttribute("enableCustomVerificationMsg", "true");
                    client.setAttribute("customSuccessOnboardingMsgTitle", "Onboarding Completed");
                    client.setAttribute("customSuccessOnboardingMsgDesc", "You can now close the window safely.");
                    client.setAttribute("customFailureOnboardingMsgTitle", "Onboarding Failed");
                    client.setAttribute("customFailureOnboardingMsgDesc", "No action required from your side. You will be redirected to app in 5 secs.");
                    client.setAttribute("customSuccessVerificationMsgTitle", "Verification Completed");
                    client.setAttribute("customSuccessVerificationMsgDesc", "You can now close the window safely.");
                    client.setAttribute("customFailureVerificationMsgTitle", "Verification Failed");
                    client.setAttribute("customFailureVerificationMsgDesc", "No action required from your side. You can now close the window safely.");
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

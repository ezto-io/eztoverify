package com.qlikverify.services.userSync.helpers;

import com.qlikverify.services.eventlisteners.azureadsync.AzureSyncRunner;
import com.qlikverify.services.eventlisteners.duowssync.DuoWSSyncRunner;
import com.qlikverify.services.eventlisteners.googlewssync.GoogleWSSyncRunner;
import com.qlikverify.services.eventlisteners.oktawssync.OktaWSSyncRunner;
import org.keycloak.models.KeycloakSession;

public class SyncRunner {
    public static void syncRunner(KeycloakSession session, String realmId, String representation, Object result, String providerId, String userId){
        switch (providerId){
            case "azure-sync":
                AzureSyncRunner.syncFromAAD(session, realmId, representation, result, userId);
                break;
            case "duo-ws-sync":
                DuoWSSyncRunner.syncFromDWS(session, realmId, representation, result, userId);
                break;
            case "okta-ws-sync":
                OktaWSSyncRunner.syncFromOWS(session, realmId, representation, result, userId);
                break;
            case "google-ws-sync":
                GoogleWSSyncRunner.syncFromGWS(session, realmId, result, representation, userId);
        }

    }
}

package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.events.EventCollection;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserTransactionModel;
import org.keycloak.models.utils.ModelToRepresentation;

public class MigrateTo7_0_3 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("7.0.3");

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().filter(realm -> realm.getMode().equalsIgnoreCase("bo")).forEach(realm -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+realm.getName());
            updateUserTransactionStatusEntity(session, realm);
            System.out.println("MIGRATION COMPLETED FOR :"+realm.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }

    private void updateUserTransactionStatusEntity(KeycloakSession session, RealmModel realm){
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);
            EventQuery eventQuery = provider.createQuery().realm(realm.getId());
            eventQuery.type(EventType.EZ_TRANSACTION_USER_IDENTIFIED);
            EventCollection result = eventQuery.getResult();

            result.getEvents().stream().map(ModelToRepresentation::toRepresentation).forEach(eventRepresentation -> {
                if (eventRepresentation.getDetails().get("requestId") != null) {
                    UserTransactionModel userTransactionDetails = realm.getUserTransactionDetails(eventRepresentation.getDetails().get("requestId"), eventRepresentation.getUser().getUsername());
                    if (userTransactionDetails != null) {
                        session.getContext().setRealm(realm);
                        realm.updateTransactionStatus(userTransactionDetails.getTrxId(), userTransactionDetails.getUserId(), userTransactionDetails.getStatus());
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

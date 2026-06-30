package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.events.EventCollection;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.AdminEventCollection;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.stream.Collectors;

public class MigrateTo3_7_0 implements QVMigration{

    public static final ModelVersion VERSION = new ModelVersion("3.7.0");
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+x.getName());
            migrateLogs(session,x);
            System.out.println("MIGRATION COMPLETED FOR :"+x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }
    private void migrateLogs(KeycloakSession session, RealmModel x) {
        session.getContext().setRealm(x);
        EventStoreProvider esStore = session.getProvider(EventStoreProvider.class,"elastic-event");
        EventStoreProvider storeProvider = session.getProvider(EventStoreProvider.class,"jpa");
        EventCollection eventCollection = storeProvider.createQuery().realm(x.getId()).getResult();
        if(eventCollection.getTotal() > 0)
        {
            eventCollection.getEvents().stream().forEach(e-> esStore.onEvent(e,null));
        }

        AdminEventCollection adEventCollection = storeProvider.createAdminQuery().realm(x.getId()).getResult();
        if(adEventCollection.getTotal() > 0)
        {
            adEventCollection.getEvents().stream().forEach(e-> esStore.onEvent(e,null, true));
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

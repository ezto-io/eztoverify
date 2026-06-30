package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.migration.migrators.Migration;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public interface QVMigration extends Migration {
    void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel);
}

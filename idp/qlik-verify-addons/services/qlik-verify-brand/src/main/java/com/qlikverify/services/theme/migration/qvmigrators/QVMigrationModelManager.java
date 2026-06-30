/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlikverify.services.theme.migration.qvmigrators;

import org.jboss.logging.Logger;
import org.keycloak.common.Version;
import org.keycloak.migration.MigrationModel;
import org.keycloak.migration.ModelVersion;
import org.keycloak.migration.migrators.Migration;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class QVMigrationModelManager {
    private static Logger logger = Logger.getLogger(QVMigrationModelManager.class);

    private static final QVMigration[] migrations = {
//            new MigrateTo2_3_0_beta(),
//            new MigrateTo2_5_0(),
//            new MigrateTo2_6_0(),
//            new MigrateTo2_6_1(),
//            new MigrateTo2_6_2(),
            new MigrateTo3_5_0(),
            new MigrateTo3_5_1(),
            new MigrateTo3_7_0(),
            new MigrateTo3_8_0(),
            new MigrateTo3_8_1(),
            new MigrateTo3_9_0(),
            new MigrateTo3_9_1(),
            new MigrateTo3_9_2(),
            new MigrateTo3_9_3(),
            new MigrateTo3_9_4(),
            new MigrateTo3_9_9(),
            new MigrateTo4_1_0(),
            new MigrateTo4_6_0(),
            new MigrateTo6_0_0(),
            new MigrateTo6_0_1(),
            new MigrateTo7_0_0(),
            new MigrateTo7_0_1(),
            new MigrateTo7_0_2(),
            new MigrateTo7_0_3(),
            new MigrateTo8_1_0(),
            new MigrateTo8_1_1(),
            new MigrateTo8_3_1(),
            new MigrateTo8_5_0(),
            new MigrateTo8_7_0(),
            new MigrateTo8_13_0(),
            new MigrateTo8_14_0(),
            new MigrateTo8_17_0(),
            new MigrateTo9_0_5()
    };

    public static void migrate(KeycloakSession session) {
        MigrationModel model = session.realms().getQVMigrationModel();

        ModelVersion currentVersion = new ModelVersion(Version.QV_VERSION);
        ModelVersion latestUpdate = migrations[migrations.length - 1].getVersion();
        ModelVersion databaseVersion = model.getStoredVersion() != null ? new ModelVersion(model.getStoredVersion()) : null;
        if (databaseVersion == null || databaseVersion.lessThan(latestUpdate)) {
            for (Migration m : migrations) {
                if (databaseVersion == null || databaseVersion.lessThan(m.getVersion())) {
                    if (databaseVersion != null) {
                        logger.debugf("Migrating older model to %s", m.getVersion());
                    }
                    m.migrate(session);
                }
            }
        }

        if (databaseVersion == null || databaseVersion.lessThan(latestUpdate)) {
            model.setStoredVersion(latestUpdate.toString());
        }

        Version.QV_RESOURCES_VERSION = model.getResourcesTag();
    }

    public static void lazyUserMigrate(KeycloakSession session, RealmModel realmModel, UserModel userModel) {
        ModelVersion currentVersion = new ModelVersion(Version.QV_VERSION);
        String migrateVersion = userModel.getFirstAttribute("migrate_version");
        ModelVersion databaseVersion = migrateVersion == null ? null : new ModelVersion(migrateVersion);
        for (QVMigration m : migrations) {
            if (databaseVersion == null || databaseVersion.lessThan(m.getVersion())) {
                m.lazyUserMigration(session, realmModel, userModel);
            }
        }

        if (databaseVersion == null || databaseVersion.lessThan(currentVersion)) {
            userModel.setSingleAttribute("hidden_ua_migrate_version", currentVersion.toString());
        }
    }
}

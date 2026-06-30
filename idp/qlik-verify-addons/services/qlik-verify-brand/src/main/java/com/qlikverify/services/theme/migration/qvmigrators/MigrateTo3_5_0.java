/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.stream.Collectors;

public class MigrateTo3_5_0 implements QVMigration {

    public static final ModelVersion VERSION = new ModelVersion("3.5.0");

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :" + x.getName());
            updateOfflineAuthenticator(session, x);
            System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
    }


    private void updateOfflineAuthenticator(KeycloakSession session, RealmModel realm) {

        realm.getAuthenticationFlowsStream().forEach(x -> {
                    long count = realm.getAuthenticationExecutionsStream(x.getId())
                            .filter(f -> f.getAuthenticator()!=null &&
                                    !f.getAuthenticator().equals("offline-authenticator") &&
                                    f.getAuthenticator().equals("auth-cookie"))
                            .count();
                    if(count > 0)
                    {
                        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
                        execution.setParentFlow(x.getId());
                        execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
                        execution.setAuthenticator("offline-authenticator");
                        execution.setPriority(8);
                        execution.setHidden(true);
                        execution.setAuthenticatorFlow(false);
                        realm.addAuthenticatorExecution(execution);
                    }
                }
        );
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {

    }
}

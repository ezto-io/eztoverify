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
import org.keycloak.models.*;

import java.util.Set;

public class MigrateTo2_6_0 implements QVMigration {

    public static final ModelVersion VERSION = new ModelVersion("2.6.0");

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :"+x.getName());
            setupSSOManagement(session,x);
            System.out.println("MIGRATION COMPLETED FOR :"+x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:"+VERSION.toString()+" COMPLETED-------------");
    }

    private void setupSSOManagement(KeycloakSession session, RealmModel realm) {
        ClientModel accountClient = realm.getClientByClientId(Constants.SSO_DESKTOP_CLIENT_ID);
        if (accountClient != null) {
            Set<String> redirectUris = accountClient.getRedirectUris();
            if(!redirectUris.contains("qlikverify://sso/"+realm.getName().toLowerCase()))
            {
                accountClient.addRedirectUri("qlikverify://sso/"+realm.getName().toLowerCase());
            }
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

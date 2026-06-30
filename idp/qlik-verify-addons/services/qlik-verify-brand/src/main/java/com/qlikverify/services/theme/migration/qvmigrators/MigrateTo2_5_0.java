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
import org.keycloak.models.utils.KeycloakModelUtils;

public class MigrateTo2_5_0 implements QVMigration {

    public static final ModelVersion VERSION = new ModelVersion("2.5.0");

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
        if (accountClient == null) {
            System.out.println(Constants.SSO_DESKTOP_CLIENT_ID + " NOT AVAILABLE");
            if (realm.getRole("user") == null) {
                realm.addDefaultRole("user");
            }
            accountClient = KeycloakModelUtils.createClient(realm, Constants.SSO_DESKTOP_CLIENT_ID);
            accountClient.setName("SSO Desktop");
            accountClient.setEnabled(true);
            accountClient.setAlwaysDisplayInConsole(false);
            accountClient.setFullScopeAllowed(false);

            accountClient.setImplicitFlowEnabled(true);
            accountClient.setStandardFlowEnabled(false);
            accountClient.setDirectAccessGrantsEnabled(false);
            accountClient.setServiceAccountsEnabled(false);
            accountClient.setBearerOnly(false);

            accountClient.setRootUrl(Constants.AUTH_BASE_URL_PROP);
            String baseUrl = "/realms/" + realm.getName() + "/sso/";
            accountClient.setBaseUrl(baseUrl);
            accountClient.addRedirectUri(baseUrl + "*");

            accountClient.setProtocol("openid-connect");
            accountClient.setAttribute("login_theme", "default");
            accountClient.setAttribute("isInBuilt", "true");
            setupDefaultActionAndPoliciesSSO(session,realm,accountClient);

            System.out.println(Constants.SSO_DESKTOP_CLIENT_ID + " CREATED");
        }
        else{
            System.out.println(Constants.SSO_DESKTOP_CLIENT_ID + " AVAILABLE");
            String binding = accountClient.getAuthenticationFlowBindingOverride("action");
            if(binding == null)
            {
                setupDefaultActionAndPoliciesSSO(session,realm,accountClient);
                System.out.println(Constants.SSO_DESKTOP_CLIENT_ID + " UPDATED POLICIES");
            }
            System.out.println(Constants.SSO_DESKTOP_CLIENT_ID + " POLICY AVAILABLE");
        }
    }

    private void setupDefaultActionAndPoliciesSSO(KeycloakSession session, RealmModel realm, ClientModel adminConsole) {
        PolicyModel model = new PolicyModel();

        model.setPolicyName(Constants.SSO_DESKTOP_CLIENT_ID+"_temppwd_policy");
        model.setPolicyType("action");
        model.setPolicyValue("temppassword() and decision()");
        model.setRealmPolicy(false);
        model.setInBuilt(true);
        model = session.policies().addPolicy(realm,model);


        PolicyModel emailPolicyModel = new PolicyModel();

        emailPolicyModel.setPolicyName(Constants.SSO_DESKTOP_CLIENT_ID+"_email_policy");
        emailPolicyModel.setPolicyType("action");
        emailPolicyModel.setPolicyValue("isemailverified(false) and decision()");
        emailPolicyModel.setRealmPolicy(false);
        emailPolicyModel.setInBuilt(true);

        emailPolicyModel = session.policies().addPolicy(realm,emailPolicyModel);

        ActionModel actionModel = new ActionModel();
        actionModel.setAlias(Constants.SSO_DESKTOP_CLIENT_ID+"_action");
        actionModel.setInBuilt(true);
        actionModel = realm.addAction(actionModel);

        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setAlias(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setName(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setProviderId(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setDefaultAction(false);
        requiredAction.setPolicyId(model.getId());
        requiredAction.setPriority(0);
        requiredAction.setEnabled(true);
        requiredAction.setActionId(actionModel.getId());
        realm.addRequiredActionProvider(requiredAction);

        RequiredActionProviderModel emailRA = new RequiredActionProviderModel();
        emailRA.setAlias(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setName(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setProviderId(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setDefaultAction(false);
        emailRA.setPolicyId(emailPolicyModel.getId());
        emailRA.setPriority(1);
        emailRA.setEnabled(true);
        emailRA.setActionId(actionModel.getId());
        realm.addRequiredActionProvider(emailRA);

        adminConsole.setAuthenticationFlowBindingOverride("action", actionModel.getId());
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {

    }
}

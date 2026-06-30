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

import org.keycloak.EnvironmentReader;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.*;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.sessions.AuthenticationSessionProvider;

import java.util.*;
import java.util.stream.Collectors;

public class MigrateTo2_6_1 implements QVMigration {

    public static final ModelVersion VERSION = new ModelVersion("2.6.1");

    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream().forEach(x -> {
            System.out.println("-------------------------------");
            System.out.println("MIGRATION STARTED FOR :" + x.getName());
            setupSSOManagement(session, x);
            System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
            System.out.println("-------------------------------");
        });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
    }

    private void setupSSOManagement(KeycloakSession session, RealmModel realm) {
        ClientModel accountClient = realm.getClientByClientId(Constants.SSO_DESKTOP_CLIENT_ID);
        if (accountClient != null) {
            realm.removeClient(accountClient.getId());
        }

        ClientRepresentation rep = new ClientRepresentation();
        rep.setName("SSO Desktop");
        rep.setAppType("browser_based");
        List<String> redirectUris = new ArrayList<>();
        redirectUris.add("https://oauth.pstmn.io/v1/callback");
        String env = EnvironmentReader.getValue("ENV");
        if ( env != null && env.equalsIgnoreCase("prod")) {
            redirectUris.add("https://ssodesktop.qlikverify.com/login/success/" + realm.getName());
            redirectUris.add("https://ssodesktop.qlikverify.com/logout/success/" + realm.getName());
        } else {
            redirectUris.add("https://ssodesktop-dev.qlikverify.com/login/success/" + realm.getName());
            redirectUris.add("https://ssodesktop-dev.qlikverify.com/logout/success/" + realm.getName());
        }
        rep.setRedirectUris(redirectUris);
        Map<String, String> attributes = new HashMap<>();
        attributes.put("login_theme", "default");
        attributes.put("isInBuilt", "true");
        rep.setAttributes(attributes);
        ClientModel client = RepresentationToModel.createClient(session, realm, rep, true);

        if (rep.getProtocol() != null) {
            LoginProtocolFactory providerFactory = (LoginProtocolFactory) session.getKeycloakSessionFactory().getProviderFactory(LoginProtocol.class, rep.getProtocol());
            providerFactory.setupClientDefaults(rep, client);
        }


        // remove default mappers if there is a template
        if (rep.getProtocolMappers() == null && rep.getClientTemplate() != null) {
            client.getProtocolMappersStream().collect(Collectors.toList()).forEach(client::removeProtocolMapper);
        }
        client.setClientId(Constants.SSO_DESKTOP_CLIENT_ID);

        client.setImplicitFlowEnabled(true);
        client.setStandardFlowEnabled(false);
        client.setDirectAccessGrantsEnabled(false);
        client.setServiceAccountsEnabled(false);
        client.setBearerOnly(false);

        setupDefaultActionAndPoliciesSSO(realm,client,Constants.SSO_DESKTOP_CLIENT_ID,session);
    }

    private void setupDefaultActionAndPoliciesSSO(RealmModel realm, ClientModel adminConsole, String clientId,KeycloakSession session) {

        PolicyModel model = (PolicyModel) session.policies().getPolicyByName(realm,clientId + "_temppwd_policy");
        if(model == null) {
            model = new PolicyModel();

            model.setPolicyName(clientId + "_temppwd_policy");
            model.setPolicyType("action");
            model.setPolicyValue("temppassword() and decision()");
            model.setRealmPolicy(false);
            model.setInBuilt(true);
            model = session.policies().addPolicy(realm, model);
        }


        PolicyModel emailPolicyModel = (PolicyModel) session.policies().getPolicyByName(realm,clientId + "_email_policy");
        if(emailPolicyModel == null) {
            emailPolicyModel = new PolicyModel();
            emailPolicyModel.setPolicyName(clientId + "_email_policy");
            emailPolicyModel.setPolicyType("action");
            emailPolicyModel.setPolicyValue("isemailverified(false) and decision()");
            emailPolicyModel.setRealmPolicy(false);
            emailPolicyModel.setInBuilt(true);

            emailPolicyModel = session.policies().addPolicy(realm, emailPolicyModel);
        }

       Optional<ActionModel> optActionModel =  realm.getActionStream().filter(x->x.getAlias().equalsIgnoreCase(clientId + "_action")).findFirst();

        ActionModel actionModel = null;
        if(optActionModel.isPresent()) {
            realm.removeAction(optActionModel.get());
        }

        actionModel = new ActionModel();
        actionModel.setAlias(clientId + "_action");
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

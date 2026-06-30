/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.authentication.authenticators.browser;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationType;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.*;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnRegisterPasswordlessAuthenticatorFactory implements AuthenticatorFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "webauthn-reg-auth-passwordless";

    @Override
    public String getReferenceCategory() {
        return WebAuthnCredentialModel.TYPE_PASSWORDLESS;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getDisplayType() {
        return "webauthn-passwordless-onboarding-provider";
    }

    @Override
    public String getHelpText() {
        return "Register Authenticator for Passwordless WebAuthn";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new WebAuthnRegisterPasswordlessAuthenticator(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        RealmPolicyProvider realmPolicyProvider = session.policies();

        List<String> providerList = new ArrayList<>();
        if (realmPolicyProvider != null) {
            List<RealmPolicyModel> policyModels = realmPolicyProvider.getPoliciesByType(session.getContext().getRealm(), "webauthn").collect(Collectors.toList());
            if (!policyModels.isEmpty()) {
                providerList.addAll(policyModels.stream().map(x -> x.getPolicyName()).collect(Collectors.toList()));
            }
        }

        ProviderConfigProperty provider;
        Optional<ProviderConfigProperty> providerConfigProperty = configProperties.stream().filter(x -> x.getName().equals("WEBAUTHN_POLICY")).findFirst();
        if (!providerConfigProperty.isPresent()) {
            provider = new ProviderConfigProperty();
        } else {
            provider = providerConfigProperty.get();
        }
        if(providerList.isEmpty()){
            provider.setName("WEBAUTHN_POLICY");
            provider.setLabel("webauthn-policy");
            provider.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            provider.setHelpText("No WebAuthn Policies are registered. Click <a href=' #/realms/"+session.getContext().getRealm().getName()+"/policies'> here </a> to create one");
            provider.setRequired(true);
        }
        else{
            provider.setName("WEBAUTHN_POLICY");
            provider.setLabel("webauthn-policy");
            provider.setType(ProviderConfigProperty.LIST_TYPE);
            provider.setOptions(providerList);
            provider.setRequired(true);
            provider.setHelpText("webauthn-policy-hlptxt");
        }
        if (!providerConfigProperty.isPresent()) {
            configProperties.add(provider);
        }
        return configProperties;
    }

    @Override
    public boolean isInBuilt() {
        return false;
    }

    @Override
    public AuthenticationType authenticatorType() {
        return AuthenticationType.ONBOARDING;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.WEB_AUTHN);
    }

    @Override
    public String getArticleId() {
        return "84000147274";
    }
    
}

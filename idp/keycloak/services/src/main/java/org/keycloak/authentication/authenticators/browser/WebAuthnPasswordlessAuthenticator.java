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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.WebAuthnPasswordlessCredentialProvider;
import org.keycloak.credential.WebAuthnPasswordlessCredentialProviderFactory;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmPolicyModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.keycloak.utils.ServicesUtils.toWebAuthPolicy;

/**
 * Authenticator for WebAuthn authentication with passwordless credential. This class is temporary and will be likely
 * removed in the future during future improvements in authentication SPI
 */
public class WebAuthnPasswordlessAuthenticator extends WebAuthnAuthenticator {

    public WebAuthnPasswordlessAuthenticator(KeycloakSession session) {
        super(session);
    }

    @Override
    protected WebAuthnPolicy getWebAuthnPolicy(AuthenticationFlowContext context) {
        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null)
            return null;

        String policyName = configModel.getConfig().get("WEBAUTHN_POLICY");
        RealmPolicyModel realmPolicyModel = context.getSession().policies().getPolicyByName(context.getRealm(), policyName);

        if (realmPolicyModel == null)
            return null;

        String policyValue = realmPolicyModel.getPolicyValue();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String,Object> objectMap = objectMapper.readValue(policyValue,Map.class);
            WebAuthnPolicy webAuthnPolicy = toWebAuthPolicy(objectMap);
            return webAuthnPolicy;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected String getCredentialType() {
        return WebAuthnCredentialModel.TYPE_PASSWORDLESS;
    }

    @Override
    protected boolean shouldDisplayAuthenticators(AuthenticationFlowContext context){
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // ask the user to do required action to register webauthn authenticator
        AuthenticationSessionModel authenticationSession = session.getContext().getAuthenticationSession();
        if (!authenticationSession.getRequiredActions().contains(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID)) {
            authenticationSession.addRequiredAction(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
        }
    }

    @Override
    public List<RequiredActionFactory> getRequiredActions(KeycloakSession session) {
        return Collections.singletonList((WebAuthnPasswordlessRegisterFactory)session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID));
    }


    @Override
    public WebAuthnPasswordlessCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (WebAuthnPasswordlessCredentialProvider)session.getProvider(CredentialProvider.class, WebAuthnPasswordlessCredentialProviderFactory.PROVIDER_ID);
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

}

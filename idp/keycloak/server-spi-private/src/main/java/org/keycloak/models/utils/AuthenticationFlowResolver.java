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
package org.keycloak.models.utils;

import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationFlowResolver {

    public static AuthenticationFlowModel resolveBrowserFlow(AuthenticationSessionModel authSession) {
        AuthenticationFlowModel flow = null;
        ClientModel client = authSession.getClient();
        String clientFlow = null;
        if (!client.isOverrideAuthentication() ){
            clientFlow =
                    authSession.getRealm().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.LOGIN_BINDING);
        } else {
            clientFlow = client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.LOGIN_BINDING);
        }
        if (clientFlow != null) {
            flow = authSession.getRealm().getAuthenticationFlowById(clientFlow);
            if (flow == null) {
                throw new ModelException("Client " + client.getClientId() + " has browser flow override, but this flow does not exist");
            }
            return flow;
        }
        return authSession.getRealm().getBrowserFlow();
    }
    public static AuthenticationFlowModel resolveOnboardingFlow(AuthenticationSessionModel authSession) {
        AuthenticationFlowModel flow = null;
        ClientModel client = authSession.getClient();
        String clientFlow = null;
        if (!client.isOverrideAuthentication()){
            clientFlow =
                    authSession.getRealm().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.ONBOARDING_BINDING);
        } else {
            clientFlow = client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.ONBOARDING_BINDING);
        }
        if (clientFlow != null) {
            flow = authSession.getRealm().getAuthenticationFlowById(clientFlow);
            if (flow == null) {
                throw new ModelException("Client " + client.getClientId() + " has browser flow override, but this flow does not exist");
            }
            return flow;
        }
        return authSession.getRealm().getRegistrationFlow();
    }
    public static AuthenticationFlowModel resolveRecoveryFlow(AuthenticationSessionModel authSession) {
        AuthenticationFlowModel flow = null;
        ClientModel client = authSession.getClient();

        String clientFlow = null;
        if (!client.isOverrideAuthentication()){
            clientFlow =
                    authSession.getRealm().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.RECOVERY_BINDING);
        } else {
            clientFlow = client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.RECOVERY_BINDING);
        }
        if (clientFlow != null) {
            flow = authSession.getRealm().getAuthenticationFlowById(clientFlow);
            if (flow == null) {
                throw new ModelException("Client " + client.getClientId() + " has browser flow override, but this flow does not exist");
            }
            return flow;
        }
        return authSession.getRealm().getResetCredentialsFlow();
    }
    public static AuthenticationFlowModel resolveDirectGrantFlow(AuthenticationSessionModel authSession) {
        AuthenticationFlowModel flow = null;
        ClientModel client = authSession.getClient();
        String clientFlow = null;
        if (!client.isOverrideAuthentication()){
            clientFlow =
                    authSession.getRealm().getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.DIRECT_GRANT_BINDING);
        } else {
            clientFlow = client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.DIRECT_GRANT_BINDING);
        }
        if (clientFlow != null) {
            flow = authSession.getRealm().getAuthenticationFlowById(clientFlow);
            if (flow == null) {
                throw new ModelException("Client " + client.getClientId() + " has direct grant flow override, but this flow does not exist");
            }
            return flow;
        }
        return authSession.getRealm().getDirectGrantFlow();
    }
}

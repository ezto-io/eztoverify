/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication;

import com.google.common.collect.Sets;
import org.jboss.logging.Logger;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.*;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.keycloak.services.managers.AuthenticationManager.FORCED_REAUTHENTICATION;
import static org.keycloak.services.managers.AuthenticationManager.SSO_AUTH;

public class AuthenticatorUtil {

    private static final Logger logger = Logger.getLogger(AuthenticatorUtil.class);

    // It is used for identification of note included in authentication session for storing callback provider factories
    public static String CALLBACKS_FACTORY_IDS_NOTE = "callbacksFactoryProviderIds";


    public static boolean isSSOAuthentication(AuthenticationSessionModel authSession) {
        return "true".equals(authSession.getAuthNote(SSO_AUTH));
    }

    public static boolean isForcedReauthentication(AuthenticationSessionModel authSession) {
        return "true".equals(authSession.getAuthNote(FORCED_REAUTHENTICATION));
    }

    /**
     * Set authentication session note for callbacks defined for {@link AuthenticationFlowCallbackFactory) factories
     *
     * @param authSession   authentication session
     * @param authFactoryId authentication factory ID which should be added to the authentication session note
     */
    public static void setAuthCallbacksFactoryIds(AuthenticationSessionModel authSession, String authFactoryId) {
        if (authSession == null || StringUtil.isBlank(authFactoryId)) return;

        final String callbacksFactories = authSession.getAuthNote(CALLBACKS_FACTORY_IDS_NOTE);

        if (StringUtil.isNotBlank(callbacksFactories)) {
            boolean containsProviderId = callbacksFactories.equals(authFactoryId) ||
                    callbacksFactories.contains(Constants.CFG_DELIMITER + authFactoryId) ||
                    callbacksFactories.contains(authFactoryId + Constants.CFG_DELIMITER);

            if (!containsProviderId) {
                authSession.setAuthNote(CALLBACKS_FACTORY_IDS_NOTE, callbacksFactories + Constants.CFG_DELIMITER + authFactoryId);
            }
        } else {
            authSession.setAuthNote(CALLBACKS_FACTORY_IDS_NOTE, authFactoryId);
        }
    }

    /**
     * Get set of Authentication factories IDs defined in authentication session as CALLBACKS_FACTORY_IDS_NOTE
     *
     * @param authSession authentication session
     * @return set of factories IDs
     */
    public static Set<String> getAuthCallbacksFactoryIds(AuthenticationSessionModel authSession) {
        if (authSession == null) return Collections.emptySet();

        final String callbacksFactories = authSession.getAuthNote(CALLBACKS_FACTORY_IDS_NOTE);

        if (StringUtil.isNotBlank(callbacksFactories)) {
            return Sets.newHashSet(callbacksFactories.split(Constants.CFG_DELIMITER));
        } else {
            return Collections.emptySet();
        }
    }


    /**
     * @param realm
     * @param flowId
     * @param providerId
     * @return all executions of given "provider_id" type. This is deep (recursive) obtain of executions of the particular flow
     */
    public static List<AuthenticationExecutionModel> getExecutionsByType(RealmModel realm, String flowId, String providerId) {
        List<AuthenticationExecutionModel> executions = new LinkedList<>();
        realm.getAuthenticationExecutionsStream(flowId).forEach(authExecution -> {
            if (providerId.equals(authExecution.getAuthenticator())) {
                executions.add(authExecution);
            } else if (authExecution.isAuthenticatorFlow() && authExecution.getFlowId() != null) {
                executions.addAll(getExecutionsByType(realm, authExecution.getFlowId(), providerId));
            }
        });
        return executions;
    }

    /**
     * Useful if we need to find top-level flow from executionModel
     *
     * @param realm
     * @param executionModel
     * @return Top parent flow corresponding to given executionModel.
     */
    public static AuthenticationFlowModel getTopParentFlow(RealmModel realm, AuthenticationExecutionModel executionModel) {
        if (executionModel.getParentFlow() != null) {
            AuthenticationFlowModel flow = realm.getAuthenticationFlowById(executionModel.getParentFlow());
            if (flow == null) throw new IllegalStateException("Flow '" + executionModel.getParentFlow() + "' referenced from execution '" + executionModel.getId() + "' not found in realm " + realm.getName());
            if (flow.isTopLevel()) return flow;

            AuthenticationExecutionModel execution = realm.getAuthenticationExecutionByFlowId(flow.getId());
            if (execution == null) throw new IllegalStateException("Not found execution referenced by flow '" + flow.getId() + "' in realm " + realm.getName());
            return getTopParentFlow(realm, execution);
        } else {
            throw new IllegalStateException("Execution '" + executionModel.getId() + "' does not have parent flow in realm " + realm.getName());
        }
    }

    /**
     * @param session
     * @return all credential providers available
     */
    public static Stream<CredentialProvider> getCredentialProviders(KeycloakSession session) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(CredentialProvider.class)
                .filter(f -> Types.supports(CredentialProvider.class, f, CredentialProviderFactory.class))
                .map(f -> session.getProvider(CredentialProvider.class, f.getId()));
    }

}

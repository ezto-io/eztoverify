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

package org.keycloak.services.clientregistration.policy;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jboss.logging.Logger;
import org.keycloak.common.Version;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.Details;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationPolicyManager {

    private static final Logger logger = Logger.getLogger(ClientRegistrationPolicyManager.class);

    public static void triggerBeforeRegister(KeycloakSession session, RegistrationAuth authType, String providerId) throws ClientRegistrationPolicyException {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
            @Override
            public void run(KeycloakSession session) throws JsonProcessingException {
                triggerPolicies(session, null, authType, "before register client", (ClientRegistrationPolicy policy) -> {

                    policy.beforeRegister(null);

                }, providerId);
            }
        });
    }

    public static void triggerBeforeRegister(ClientRegistrationContext context, RegistrationAuth authType) throws ClientRegistrationPolicyException {
        triggerPolicies(context.getSession(), context.getProvider(), authType, "before register client", (ClientRegistrationPolicy policy) -> {

            policy.beforeRegister(context);

        });
    }

    public static void triggerAfterRegister(KeycloakSession session, RegistrationAuth authType, String providerId, Map<String, Object> attributes) {
        try {
            triggerPolicies(session, null, authType, "after register client ", (ClientRegistrationPolicy policy) -> {

                policy.afterRegister(null, null, attributes);

            }, providerId);
        } catch (ClientRegistrationPolicyException crpe) {
            throw new IllegalStateException(crpe);
        }
    }

    public static void triggerAfterRegister(KeycloakSession session, RegistrationAuth authType, String providerId) {
        triggerAfterRegister(session, authType, providerId, null);
    }

    public static void triggerAfterRegister(ClientRegistrationContext context, RegistrationAuth authType, ClientModel client, Map<String, Object> attributes) {
        try {
            triggerPolicies(context.getSession(), context.getProvider(), authType, "after register client " + client.getClientId(), (ClientRegistrationPolicy policy) -> {

                policy.afterRegister(context, client, attributes);

            });
        } catch (ClientRegistrationPolicyException crpe) {
            throw new IllegalStateException(crpe);
        }
    }

    public static void triggerAfterRegister(ClientRegistrationContext context, RegistrationAuth authType, ClientModel client) {
        triggerAfterRegister(context, authType, client, null);
    }


    public static void triggerBeforeUpdate(ClientRegistrationContext context, RegistrationAuth authType, ClientModel client) throws ClientRegistrationPolicyException {
        triggerPolicies(context.getSession(), context.getProvider(), authType, "before update client " + client.getClientId(), (ClientRegistrationPolicy policy) -> {

            policy.beforeUpdate(context, client);

        });
    }

    public static void triggerAfterUpdate(ClientRegistrationContext context, RegistrationAuth authType, ClientModel client) {
        try {
            triggerPolicies(context.getSession(), context.getProvider(), authType, "after update client " + client.getClientId(), (ClientRegistrationPolicy policy) -> {

                policy.afterUpdate(context, client);

            });
        } catch (ClientRegistrationPolicyException crpe) {
            throw new IllegalStateException(crpe);
        }
    }

    public static void triggerBeforeView(KeycloakSession session, ClientRegistrationProvider provider, RegistrationAuth authType, ClientModel client) throws ClientRegistrationPolicyException {
        triggerPolicies(session, provider, authType, "before view client " + client.getClientId(), (ClientRegistrationPolicy policy) -> {

            policy.beforeView(provider, client);

        });
    }

    public static void triggerBeforeRemove(KeycloakSession session, ClientRegistrationProvider provider, RegistrationAuth authType, ClientModel client) throws ClientRegistrationPolicyException {
        triggerPolicies(session, provider, authType, "before delete client " + client.getClientId(), (ClientRegistrationPolicy policy) -> {

            policy.beforeDelete(provider, client);

        });
    }


    private static void triggerPolicies(KeycloakSession session, ClientRegistrationProvider provider, RegistrationAuth authType,
                                        String opDescription, ClientRegOperation op, String providerId) throws ClientRegistrationPolicyException {

        if (session.getContext() == null)
            return;

        RealmModel realm = session.getContext().getRealm();

        // No realm in context (e.g. master-realm admin-user bootstrap during
        // onboarding) -> nothing to trigger. Must bail before realm.getId().
        // Previously this null-check was gated on OFFERING==cloud, which NPE'd
        // on on-prem.
        if (realm == null)
            return;

        if (realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm()) && Version.OFFERING.equalsIgnoreCase("cloud"))
            return;

        String policyTypeKey = getComponentTypeKey(authType);
        realm.getComponentsStream(realm.getId(), ClientRegistrationPolicy.class.getName())
                .filter(componentModel -> Objects.equals(componentModel.getSubType(), policyTypeKey))
                .filter(componentModel -> providerId != null && componentModel.getProviderId().equalsIgnoreCase(providerId))
                .forEach(policyModel -> runPolicy(policyModel, session, provider, opDescription, op));
    }

    private static void triggerPolicies(KeycloakSession session, ClientRegistrationProvider provider, RegistrationAuth authType,
                                        String opDescription, ClientRegOperation op) throws ClientRegistrationPolicyException {
        triggerPolicies(session, provider, authType, opDescription, op, null);
    }

    private static void runPolicy(ComponentModel policyModel, KeycloakSession session, ClientRegistrationProvider provider,
                                  String opDescription, ClientRegOperation op) throws ClientRegistrationPolicyException {
        ClientRegistrationPolicy policy = session.getProvider(ClientRegistrationPolicy.class, policyModel);
        if (policy == null) {
            throw new ClientRegistrationPolicyException("Policy of type '" + policyModel.getProviderId() + "' not found");
        }

        if (logger.isTraceEnabled()) {
            logger.tracef("Running policy '%s' %s", policyModel.getName(), opDescription);
        }

        try {
            op.run(policy);
        } catch (ClientRegistrationPolicyException crpe) {
            if (provider != null) {
                provider.getEvent().detail(Details.CLIENT_REGISTRATION_POLICY, policyModel.getName());
            }
            crpe.setPolicyModel(policyModel);
            ServicesLogger.LOGGER.clientRegistrationRequestRejected(opDescription, crpe.getMessage());
            throw crpe;
        }
    }

    private interface ClientRegOperation {

        void run(ClientRegistrationPolicy policy) throws ClientRegistrationPolicyException;

    }

    public static String getComponentTypeKey(RegistrationAuth authType) {
        return authType.toString().toLowerCase();
    }
}

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

package com.qlikverify.services.theme.subscriptionpolicies;

import org.jboss.logging.Logger;
import org.keycloak.common.Version;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static com.qlikverify.services.theme.onboarding.Constants.*;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MaxClientsClientRegistrationPolicy implements ClientRegistrationPolicy {

    private final KeycloakSession session;
    private final ComponentModel componentModel;
    protected static final Logger logger = Logger.getLogger(MaxClientsClientRegistrationPolicy.class);

    public MaxClientsClientRegistrationPolicy(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {

        RealmModel realm = session.getContext().getRealm();
        if (componentModel.getSubType().equalsIgnoreCase(RegistrationAuth.AUTHENTICATED.toString())) {
            if (Version.OFFERING.equalsIgnoreCase("cloud")) {
                cloudOffering(realm);
            }
            if (Version.OFFERING.equalsIgnoreCase("onpremise")) {
                Long clientCount = realm.getClientsStream()
                        .filter(c -> !Constants.defaultClients.contains(c.getClientId()))
                        .filter(c -> !c.getClientId().contains(Constants.REALM_AS_CLIENT)).count();
                if ((clientCount > 0 && Version.APP_COUNT > 0) && (clientCount >= Version.APP_COUNT)) {
                    throw new ClientRegistrationPolicyException(MAX_APP_COUNT_REACHED);
                }
            }
        } else {
            long currentCount = realm.getClientsCount();
            int maxCount = componentModel.get(MaxClientsClientRegistrationPolicyFactory.MAX_CLIENTS, MaxClientsClientRegistrationPolicyFactory.DEFAULT_MAX_CLIENTS);

            if (currentCount >= maxCount && maxCount > 0) {
                throw new ClientRegistrationPolicyException("It's allowed to have max " + maxCount + " clients per realm");
            }
        }
    }

    public void cloudOffering(RealmModel realm) {
        // Bypassed
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel, Map<String, Object> attributes) {
        // Bypassed
    }

    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
        // NOP
    }

    @Override
    public void afterUpdate(ClientRegistrationContext context, ClientModel clientModel) {
        // NOP
    }

    @Override
    public void beforeView(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
        // NOP
    }

    @Override
    public void beforeDelete(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
        // NOP
    }

}

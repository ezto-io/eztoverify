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

import org.keycloak.common.Version;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CanMultiTenantRegistrationPolicy implements ClientRegistrationPolicy {

    private final KeycloakSession session;

    public CanMultiTenantRegistrationPolicy(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {

        if (Version.OFFERING.equalsIgnoreCase("onpremise")) {

            if(!Version.IS_MULTITENANT)
                throw new ClientRegistrationPolicyException("single-tenant-license-check-sales");

            int tenantCount = session.realms().getRealmsCount();
            if ( (tenantCount > 0 && Version.TENANT_COUNT > 0) && (tenantCount >= Version.TENANT_COUNT) ) {
                    throw new ClientRegistrationPolicyException("max-tenant-reached-contact-sales");
            }
        }
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel, Map<String, Object> attributes) {
        // NOP
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

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

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientregistration.policy.AbstractClientRegistrationPolicyFactory;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.impl.CanMultiTenantRegistrationPolicy;
import static com.qlikverify.services.theme.onboarding.Constants.*;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CanMultiTenantRegistrationPolicyFactory extends AbstractClientRegistrationPolicyFactory {

    private static List<ProviderConfigProperty> configProperties = new LinkedList<>();

    static {
        MAX_REALMS_PROPERTY.setName(MAX_REALMS);
        MAX_REALMS_PROPERTY.setLabel("max-realms.label");
        MAX_REALMS_PROPERTY.setHelpText("max-realms.tooltip");
        MAX_REALMS_PROPERTY.setType(ProviderConfigProperty.STRING_TYPE);
        MAX_REALMS_PROPERTY.setDefaultValue(String.valueOf(DEFAULT_MAX_REALMS));
        configProperties.add(MAX_REALMS_PROPERTY);
    }

    public static final String PROVIDER_ID = "max-realms";

    @Override
    public ClientRegistrationPolicy create(KeycloakSession session, ComponentModel model) {
        return new CanMultiTenantRegistrationPolicy(session);
    }

    @Override
    public String getHelpText() {
        return "can-multi-tenant-reg-policy-helptext";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        ConfigurationValidationHelper.check(config)
                .checkInt(MAX_REALMS_PROPERTY, true);
    }
}

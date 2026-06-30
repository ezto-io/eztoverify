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

package org.keycloak.policy.password;

import org.keycloak.models.*;
import org.keycloak.policy.PolicyError;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultPasswordPolicyManagerProvider implements PasswordPolicyManagerProvider {

    private KeycloakSession session;

    public DefaultPasswordPolicyManagerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public PolicyError validate(String policyId, UserModel user, String password) {
        PasswordPolicy policy = getUsernamePolicy(policyId);
        if(policy == null)
            return null;

        for (PasswordPolicyProvider p : getProviders(policy, session)) {
            PolicyError policyError = p.validate(policy, user, password);
            if (policyError != null) {
                return policyError;
            }
        }
        return null;
    }

    @Override
    public PolicyError validate(String policyId,String user, String password) {
        PasswordPolicy policy = getUsernamePolicy(policyId);
        if(policy == null)
            return null;

        for (PasswordPolicyProvider p : getProviders(policy,session)) {
            PolicyError policyError = p.validate(policy,user, password);
            if (policyError != null) {
                return policyError;
            }
        }
        return null;
    }

    @Override
    public void close() {
    }

    private PasswordPolicy getUsernamePolicy(String policyId) {
        RealmPolicyModel policyModel = null;
        if(policyId == null) {
            policyModel =session.policies().getRealmPolicy(session.getContext().getRealm(), RealmPolicyType.PASSWORD);
        }
        else
        {
            policyModel = session.policies().getPolicyById(session.getContext().getRealm(), policyId);
        }

        if(policyModel == null)
            return null;

        PasswordPolicy policy = PasswordPolicy.parse(session, policyModel.getPolicyValue());
        return policy;
    }

    private List<PasswordPolicyProvider> getProviders(PasswordPolicy policyModel, KeycloakSession session) {
        LinkedList<PasswordPolicyProvider> list = new LinkedList<>();
        for (String id : policyModel.getPolicies()) {
            PasswordPolicyProvider provider = session.getProvider(PasswordPolicyProvider.class, id);
            list.add(provider);
        }
        return list;
    }

}

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

package org.keycloak.policy.username;

import org.keycloak.models.*;
import org.keycloak.policy.PolicyError;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultUsernamePolicyManagerProvider implements UsernamePolicyManagerProvider {

    private KeycloakSession session;

    public DefaultUsernamePolicyManagerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public PolicyError validate(String policyId, UserModel user) {
        UsernamePolicy policy = getUsernamePolicy(policyId);
        if(policy == null)
            return null;

        for (UsernamePolicyProvider p : getProviders(policy, session)) {
            PolicyError policyError = p.validate(policy,user);
            if (policyError != null) {
                return policyError;
            }
        }
        return null;
    }

    @Override
    public PolicyError validate(String policyId,String user) {
        UsernamePolicy policy = getUsernamePolicy(policyId);
        if(policy == null)
            return null;

        for (UsernamePolicyProvider p : getProviders(policy, session)) {
            PolicyError policyError = p.validate(policy,user);
            if (policyError != null) {
                return policyError;
            }
        }
        return null;
    }

    private UsernamePolicy getUsernamePolicy(String policyId) {
        RealmPolicyModel policyModel = null;
        if(policyId == null) {
            policyModel =session.policies().getRealmPolicy(session.getContext().getRealm(), RealmPolicyType.USERNAME);
        }
        else
        {
            policyModel = session.policies().getPolicyById(session.getContext().getRealm(), policyId);
        }
        if(policyModel == null)
            return null;

        UsernamePolicy policy = UsernamePolicy.parse(session, policyModel.getPolicyValue());
        return policy;
    }

    @Override
    public void close() {
    }


    private List<UsernamePolicyProvider> getProviders(UsernamePolicy policyModel, KeycloakSession session) {
        LinkedList<UsernamePolicyProvider> list = new LinkedList<>();
        for (String id : policyModel.getPolicies()) {
            UsernamePolicyProvider provider = session.getProvider(UsernamePolicyProvider.class, id);
            list.add(provider);
        }
        return list;
    }

}

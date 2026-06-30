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

package org.keycloak.policy.action;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.models.*;
import org.keycloak.policy.PolicyError;
import org.keycloak.policy.username.UsernamePolicyProvider;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultActionPolicyManagerProvider implements ActionPolicyManagerProvider {

    private KeycloakSession session;

    public DefaultActionPolicyManagerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean validate(RequiredActionContext context, String policyId, UserModel user) {
        //TODO: Check to remove this method in next quarter part 2
        ActionPolicy policy = getActionPolicy(policyId);
        if(policy == null)
            return false;

        List<Boolean> results = new ArrayList<>();

        for (ActionPolicyProvider p : getProviders(policy, session)) {
            results.add(p.validate(context.getSession(),policy,user));
        }

        if(policy.getDecisionStrategy().equals(ActionPolicy.UNANIMOUS_STRATEGY))
        {
            return results.stream().allMatch(x-> x);
        }

        if(policy.getDecisionStrategy().equals(ActionPolicy.AFFIRMATIVE_STRATEGY))
        {
            return results.stream().filter(x -> x == true).findAny().isPresent();
        }

        return false;
    }

    @Override
    public boolean validate(String policyId, UserModel user) {
        ActionPolicy policy = getActionPolicy(policyId);
        if(policy == null)
            return false;

        List<Boolean> results = new ArrayList<>();

        for (ActionPolicyProvider p : getProviders(policy, session)) {
            results.add(p.validate(session,policy,user));
        }

        if(policy.getDecisionStrategy().equals(ActionPolicy.UNANIMOUS_STRATEGY))
        {
            return results.stream().allMatch(x-> x == true);
        }

        if(policy.getDecisionStrategy().equals(ActionPolicy.AFFIRMATIVE_STRATEGY))
        {
            return results.stream().filter(x -> x == true).findAny().isPresent();
        }

        return false;
    }

    private ActionPolicy getActionPolicy(String policyId) {
        RealmPolicyModel policyModel = null;
        policyModel = session.policies().getPolicyById(session.getContext().getRealm(), policyId);
        if(policyModel == null)
            return null;

        ActionPolicy policy = ActionPolicy.parse(session, policyModel.getPolicyValue());
        return policy;
    }

    @Override
    public void close() {
    }


    private List<ActionPolicyProvider> getProviders(ActionPolicy policyModel, KeycloakSession session) {
        LinkedList<ActionPolicyProvider> list = new LinkedList<>();
        for (String id : policyModel.getPolicies()) {
            if (id != null && id.contains("@")){
                id = id.split("@")[0];
            }
            ActionPolicyProvider provider = session.getProvider(ActionPolicyProvider.class, id);
            if(provider != null && !list.contains(provider)) {
                list.add(provider);
            }
        }
        return list;
    }

}

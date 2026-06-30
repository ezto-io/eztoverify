package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.List;
import java.util.stream.Stream;

public interface RealmPolicyProvider extends Provider {

    PolicyModel addPolicy(RealmModel realm, PolicyModel model);
    boolean updatePolicy(RealmModel realm, String id, String policyName, String policyValue, boolean isRealmPolicy, String policyType, boolean inBuilt);
    boolean removePolicy(RealmModel realm, String id);
    void removePolicies(RealmModel realm);
    Stream<RealmPolicyModel> getPoliciesStream(RealmModel realm, Integer firstResult, Integer maxResults);
    Stream<RealmPolicyModel> getAllPoliciesStream(RealmModel realm, Integer firstResult, Integer maxResults);
    Stream<RealmPolicyModel> searchPoliciesByPolicyNameStream(String policyName,RealmModel realm,Integer firstResult, Integer maxResults);
    RealmPolicyModel getPolicyById(RealmModel model, String id);
    RealmPolicyModel getPolicyByName(RealmModel model, String name);
    RealmPolicyModel getRealmPolicy(RealmModel model, String policyType);
    Stream<RealmPolicyModel> getPoliciesByType(RealmModel realm, String policyType);
    Stream<RealmPolicyModel> getCibaPoliciesStream(RealmModel realm, Integer firstResult, Integer maxResults, String policyName);
}

package org.keycloak.policy.ciba;

import java.util.List;

public class CibaPolicyCollection {
    private String decision;
    private List<CibaPolicy> policyList;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public List<CibaPolicy> getPolicyList() {
        return policyList;
    }

    public void setPolicyList(List<CibaPolicy> policyList) {
        this.policyList = policyList;
    }
}

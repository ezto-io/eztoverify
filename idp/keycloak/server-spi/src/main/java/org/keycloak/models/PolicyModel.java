package org.keycloak.models;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PolicyModel implements Serializable {
    private String id;
    private String policyName;
    private String policyValue;
    private boolean isRealmPolicy;
    private String policyType;
    private boolean inBuilt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getPolicyValue() {
        return policyValue;
    }

    public void setPolicyValue(String policyValue) {
        this.policyValue = policyValue;
    }

    public boolean isRealmPolicy() {
        return isRealmPolicy;
    }

    public void setRealmPolicy(boolean realmPolicy) {
        isRealmPolicy = realmPolicy;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public boolean isInBuilt() {
        return inBuilt;
    }

    public void setInBuilt(boolean inBuilt) {
        this.inBuilt = inBuilt;
    }
}
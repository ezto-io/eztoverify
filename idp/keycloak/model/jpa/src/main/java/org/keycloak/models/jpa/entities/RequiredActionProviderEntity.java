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

package org.keycloak.models.jpa.entities;

import javax.persistence.*;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Table(name="REQUIRED_ACTION_PROVIDER")
@Entity
@NamedQueries({
        @NamedQuery(name="deleteRequiredActionProviderByRealm", query="delete from RequiredActionProviderEntity action where action.realm = :realm and action.actionId = :actionId"),})
public class RequiredActionProviderEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name="ALIAS")
    protected String alias;

    @Column(name="NAME")
    protected String name;

    @Column(name="DESCRIPTION")
    protected String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Column(name="PROVIDER_ID")
    protected String providerId;

    @Column(name="ENABLED")
    protected boolean enabled;
    @Column(name="SKIPPABLE")
    protected boolean skippable;

    @Column(name="IS_VALIDATOR")
    protected boolean isValidator;

    @Column(name="PARENT_ID")
    protected String parentId;

    @Column(name="DIRECTION")
    protected String direction;

    @Column(name="VALIDATOR_STEP_ID")
    protected String validatorStepId;

    @Column(name="DEFAULT_ACTION")
    protected boolean defaultAction;

    @Column(name="PRIORITY")
    protected int priority;

    @Column(name = "POLICY_ID")
    protected String policyId;

    @Column(name = "POLICY_TYPE")
    protected String policyType;

    @Column(name = "ACTION_ID")
    protected String actionId;

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE")
    @CollectionTable(name="REQUIRED_ACTION_CONFIG", joinColumns={ @JoinColumn(name="REQUIRED_ACTION_ID") })
    private Map<String, String> config;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSkippable() {
        return skippable;
    }

    public void setSkippable(boolean skippable) {
        this.skippable = skippable;
    }

    public boolean isValidator() {
        return isValidator;
    }

    public void setValidator(boolean validator) {
        isValidator = validator;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setValidatorStepId(String validatorStepId) {
        this.validatorStepId = validatorStepId;
    }

    public String getValidatorStepId() {
        return validatorStepId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDirection() {
        return direction;
    }

    public boolean isDefaultAction() {
        return defaultAction;
    }

    public void setDefaultAction(boolean defaultAction) {
        this.defaultAction = defaultAction;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof RequiredActionProviderEntity)) return false;

        RequiredActionProviderEntity that = (RequiredActionProviderEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

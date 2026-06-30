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

package org.keycloak.models;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RequiredActionProviderModel implements Serializable {

    public static class RequiredActionComparator implements Comparator<RequiredActionProviderModel> {
        public static final RequiredActionComparator SINGLETON = new RequiredActionComparator();

        @Override
        public int compare(RequiredActionProviderModel o1, RequiredActionProviderModel o2) {
            //.thenComparing(RequiredActionProviderModel::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
            // Remove case sensitive comparison
            return Comparator
                    .comparingInt(RequiredActionProviderModel::getPriority)
                    .thenComparing(RequiredActionProviderModel::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                    .compare(o1, o2);
        }
    }

    private String id;
    private String alias;
    private String name;
    private String description;
    private String providerId;
    private boolean enabled;
    private boolean skippable;
    private boolean validator;
    private boolean defaultAction;
    private int priority;
    private String policyId;
    private String policyType;
    private String actionId;
    private String direction;
    private String parentId;
    private String validatorStepId;
    private Map<String, String> config = new HashMap<>();


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

    /**
     * Used for display purposes.  Probably should clean this code up and make alias and name the same, but
     * the old code references an Enum and the admin console creates a "friendly" name for each enum.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return validator;
    }

    public void setValidator(boolean validator) {
        this.validator = validator;
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
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

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDirection() {
        return direction;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setValidatorStepId(String validatorStepId) {
        this.validatorStepId = validatorStepId;
    }

    public String getValidatorStepId() {
        return validatorStepId;
    }

    public String getPasswordPolicy() {
        return config.containsKey("passwordPolicy") ? config.get("passwordPolicy") : null;
    }
    public void setPasswordPolicy(String passwordPolicy) {
        config.put("passwordPolicy",passwordPolicy);
    }
    public String getDescription() {return description;}

    public void setDescription(String description) {this.description = description;}
}

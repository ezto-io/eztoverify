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
public class ActionModel implements Serializable {

    private String id;
    private String alias;
    private String realmId;
    private Long modifiedTimestamp;
    private boolean inBuilt;
    private String actionType;
    private String actionVariables;
    private boolean isSubWorkflow;
    private boolean marketPlace;

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

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public Long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(Long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    public boolean isInBuilt() {
        return inBuilt;
    }

    public void setInBuilt(boolean inBuilt) {
        this.inBuilt = inBuilt;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionVariables() {
        return actionVariables;
    }

    public void setActionVariables(String actionVariables) {
        this.actionVariables = actionVariables;
    }

    public boolean getIsSubWorkflow() {
        return isSubWorkflow;
    }

    public void setIsSubWorkflow(boolean isSubWorkflow) {
        this.isSubWorkflow = isSubWorkflow;
    }

    public boolean getMarketPlace() {
        return marketPlace;
    }

    public void setMarketPlace(boolean marketPlace) {
        this.marketPlace = marketPlace;
    }
}

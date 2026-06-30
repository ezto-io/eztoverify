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

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Table(name="ACTION")
@Entity
public class ActionEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name="ALIAS")
    protected String alias;

    @Column(name = "REALM_ID")
    protected String realm;

    @Column(name = "MODIFIED_TIMESTAMP")
    protected Long modifiedTimestamp;

    @Column(name = "IN_BUILT")
    protected boolean inBuilt;

    @Column(name = "ACTION_TYPE")
    protected String actionType;

    @Column(name = "ACTION_VARIABLES")
    protected String actionVariables;

    @Column(name = "SUB_WORKFLOW")
    protected boolean subWorkflow;

    @Column(name = "IS_MARKETPLACE")
    protected boolean marketplace;

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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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

    public boolean isSubWorkflow() {
        return subWorkflow;
    }

    public void setSubWorkflow(boolean subWorkflow) {
        this.subWorkflow = subWorkflow;
    }

    public boolean isMarketplace() {
        return marketplace;
    }

    public void setMarketplace(boolean marketplace) {
        this.marketplace = marketplace;
    }
}

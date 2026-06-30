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
@Entity
@Table(name="MGMT_ACCESS", uniqueConstraints = {@UniqueConstraint(columnNames = {"REALM_ID"})})
@NamedQueries({
        @NamedQuery(name="getMgmtAccess", query="select mgmtAccess from MgmtAccessEntity mgmtAccess where mgmtAccess.realmId = :realmId by mgmtAccess.lastUsedDate"),
        @NamedQuery(name="getMgmtAccessById", query="select mgmtAccess from MgmtAccessEntity mgmtAccess where mgmtAccess.id = :id and mgmtAccess.realmId = :realmId")
})
public class MgmtAccessEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name="NAME")
    private String name;

    @Column(name = "EXPIRY_DATE")
    private Long expiryDate;

    @Column(name="REQUESTED_BY")
    private String requestedBy;

    @Column(name = "CREATED_DATE")
    private Long createdDate;

    @Column(name = "LAST_UPDATED_DATE")
    private Long lastUpdatedDate;

    @Column(name = "LAST_USED_DATE")
    private Long lastUsedDate;

    @Column(name="SECRET")
    private String secret;

    @Column(name = "ENABLED")
    private boolean enabled;

    @Column(name = "SANDBOX_MODE")
    private boolean sandboxMode;

    @Column(name = "SANDBOX_SECRET")
    private String sandboxSecret;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="PERMISSION")
    @Column(name="ENABLED")
    @CollectionTable(name="MGMT_ACCESS_PERMS", joinColumns={ @JoinColumn(name="MGMT_ACCESS_ID") })
    private Map<String, Boolean> permissions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public Long getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(Long lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof MgmtAccessEntity)) return false;

        MgmtAccessEntity that = (MgmtAccessEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


    public boolean isSandboxMode() {
        return sandboxMode;
    }

    public void setSandboxMode(boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    public String getSandboxSecret() {
        return sandboxSecret;
    }

    public void setSandboxSecret(String sandboxSecret) {
        this.sandboxSecret = sandboxSecret;
    }
}

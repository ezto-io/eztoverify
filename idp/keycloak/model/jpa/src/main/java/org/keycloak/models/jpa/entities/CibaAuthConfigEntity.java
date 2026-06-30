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
@Table(name="CIBA_AUTH_CONFIG")
@Entity
@NamedQueries({ @NamedQuery(name = "findByCibaProviderAndRealm", query = "select u from CibaAuthConfigEntity u where u.providerId = :providerId and u.clientId = :clientId and u.realm = :realmId"),
        @NamedQuery(name = "findByCibaProviderByClient", query = "select u from CibaAuthConfigEntity u where u.clientId = :clientId and u.realm = :realmId")})
public class CibaAuthConfigEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name="PROVIDER_ID")
    protected String providerId;

    @Column(name="CLIENT_ID")
    protected String clientId;

    @Column(name = "REALM_ID")
    protected String realm;

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE")
    @CollectionTable(
      name="CIBA_AUTH_CONFIG_ENTRY",
      joinColumns={ @JoinColumn(name="CONFIG_ID") },
      uniqueConstraints = { @UniqueConstraint(columnNames = {"CONFIG_ID", "NAME"})}
    )
    private Map<String, String> config;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CibaAuthConfigEntity)) return false;

        CibaAuthConfigEntity that = (CibaAuthConfigEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}

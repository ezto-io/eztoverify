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

import org.hibernate.annotations.Nationalized;

import javax.persistence.*;
import java.time.Instant;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@NamedQueries({
        @NamedQuery(name="deleteAttributesByTrxId", query="delete from  TrxAttributeEntity attr where attr.trxId = :trxId"),
        @NamedQuery(name="deleteAttributesByTrxIdAndName", query="delete from  TrxAttributeEntity attr where attr.trxId = :trxId and attr.name = :name"),
        @NamedQuery(name = "findByTrxId", query = "select u from TrxAttributeEntity u where u.trxId = :trxId"),
        @NamedQuery(name = "findByTrxIdAndName" , query = "SELECT u FROM TrxAttributeEntity u WHERE u.trxId = :trxId and u.name = :name")
})
@Table(name="TRX_ATTRIBUTE")
public class TrxAttributeEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "TRX_ID")
    protected String trxId;

    @Column(name = "NAME")
    protected String name;
    @Nationalized
    @Column(name = "VALUE")
    protected String value;


    @Column(name = "CREATED_AT")
    private Long createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTrxId() {
        return trxId;
    }

    public void setTrxId(String trxId) {
        this.trxId = trxId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof TrxAttributeEntity)) return false;

        TrxAttributeEntity that = (TrxAttributeEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


    public Long getCreatedTimestamp() {
        return createdAt;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = System.currentTimeMillis();
    }
}

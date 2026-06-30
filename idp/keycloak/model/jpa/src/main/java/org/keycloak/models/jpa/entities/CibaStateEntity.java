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
@Table(name="CIBA_STATE")
@Entity
@NamedQueries({
        @NamedQuery(name = "findByCibaState", query = "select u from CibaStateEntity u where u.cibaStateId = :cibaStateId"),
        @NamedQuery(name = "removeByCibaState", query = "delete from CibaStateEntity u where u.cibaStateId = :cibaStateId")
})

public class CibaStateEntity {
    @Id
    @Column(name="CIBA_STATE_ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String cibaStateId;

    @Column(name="CIBA_TOKEN")
    protected String cibaToken;

    @Column(name="CIBA_USER_ID")
    protected String userId;

    @Column(name="CIBA_REQUEST_ID")
    protected String requestId;

    public String getCibaStateId() {
        return cibaStateId;
    }

    public void setCibaStateId(String cibaStateId) {
        this.cibaStateId = cibaStateId;
    }

    public String getCibaToken() {
        return cibaToken;
    }

    public void setCibaToken(String cibaToken) {
        this.cibaToken = cibaToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}

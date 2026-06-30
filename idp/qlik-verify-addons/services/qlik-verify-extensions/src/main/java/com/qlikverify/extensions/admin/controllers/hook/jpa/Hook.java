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

package com.qlikverify.extensions.admin.controllers.hook.jpa;


import javax.persistence.*;

@Entity
@Table(name = "HOOK")
@NamedQueries({ @NamedQuery(name = "findByRealm", query = "SELECT h FROM Hook h WHERE h.key = :key ORDER BY h" +
        ".createdTime DESC"),
        @NamedQuery(name = "findByChatCode", query = "SELECT h FROM Hook h WHERE h.chatCode = :chatCode ORDER BY h.createdTime DESC"),
        @NamedQuery(name = "deleteByKey", query =  "DELETE FROM Hook h WHERE h.key = :key")
})
public class Hook {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "KEY", nullable = false)
    private String key;

    @Column(name = "CHAT_CODE", nullable = false)
    private String chatCode;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @Column(name = "HOOK_RESULT", nullable = true)
    private String hookResult;

    @Column(name="CREATED_TIME")
    protected long createdTime;

    public String getId() {
		return id;
	}
    
    public String getRealmId() {
        return realmId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getChatCode() {
        return chatCode;
    }

    public void setChatCode(String chatCode) {
        this.chatCode = chatCode;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public String getHookResult() {
        return hookResult;
    }

    public void setHookResult(String hookResult) {
        this.hookResult = hookResult;
    }
}

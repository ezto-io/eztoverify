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

package com.qlikverify.extensions.admin.controllers.hook.spi;

import com.qlikverify.extensions.admin.controllers.hook.jpa.Hook;
import com.qlikverify.hook.spi.HookRepresentation;
import com.qlikverify.hook.spi.HookService;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.List;

public class HookServiceImpl implements HookService {

    private final KeycloakSession session;

    public HookServiceImpl(KeycloakSession session) {
        this.session = session;
        if (getRealm() == null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in its context.");
        }
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    protected RealmModel getRealm() {
        return session.getContext().getRealm();
    }

    public void close() {
        // Nothing to do.
    }

    @Override
    public HookRepresentation findHook(String key) {
        List<Hook> companyEntities = getEntityManager().createNamedQuery("findByRealm", Hook.class)
//                .setParameter("realmId", getRealm().getId())
                .setParameter("key", key)
                .getResultList();

        if (companyEntities.isEmpty())
            return null;

        Hook hook = companyEntities.get(0);
        HookRepresentation hookRepresentation = new HookRepresentation();
        hookRepresentation.setId(hook.getId());
        hookRepresentation.setChatCode(hook.getChatCode());
        hookRepresentation.setKey(hook.getKey());
        hookRepresentation.setRealmId(hook.getRealmId());
        hookRepresentation.setHookResult(hook.getHookResult());
        return hookRepresentation;
    }

    @Override
    public HookRepresentation addHook(HookRepresentation company) {
        getEntityManager().getTransaction().begin();
        getEntityManager().createQuery("DELETE FROM Hook h WHERE h.realmId = :realmId AND h.key = :key")
                        .setParameter("key", company.getKey())
                    .setParameter("realmId", getRealm().getId())
                            .executeUpdate();

        getEntityManager().getTransaction().commit();

        Hook entity = new Hook();
        String id = company.getId() == null ? KeycloakModelUtils.generateId() : company.getId();
        entity.setId(id);
        entity.setKey(company.getKey());
        entity.setChatCode(company.getChatCode());
        entity.setRealmId(getRealm().getId());
        entity.setCreatedTime(Time.currentTime());
        getEntityManager().persist(entity);

        company.setId(id);
        return company;
    }

    @Override
    public void updateHook(String chatCode, String hookResult) {
        Hook foundHook = findHook(chatCode, getRealm().getId(), LockModeType.PESSIMISTIC_WRITE);
        if (foundHook == null)
            return;

        foundHook.setHookResult(hookResult);
        getEntityManager().persist(foundHook);
        getEntityManager().flush();
    }

    @Override
    public void deleteHook(String chatCode, String realmId) {
        Hook foundHook = findHook(chatCode, getRealm().getId(), LockModeType.PESSIMISTIC_WRITE);
        if (foundHook == null)
            return;

        getEntityManager().remove(foundHook);
        getEntityManager().flush();
    }

    private Hook findHook(String chatCode, String realmId, LockModeType lockMode) {
        TypedQuery<Hook> query = getEntityManager().createNamedQuery("findByChatCode", Hook.class);
        query.setParameter("chatCode", chatCode);
        query.setLockMode(lockMode);
        List<Hook> results = query.getResultList();
        return !results.isEmpty() ? results.get(0) : null;
    }
}

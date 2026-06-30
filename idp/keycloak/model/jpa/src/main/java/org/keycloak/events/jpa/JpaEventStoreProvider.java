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

package org.keycloak.events.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.events.*;
import org.keycloak.events.admin.*;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.io.IOException;
import java.util.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JpaEventStoreProvider implements EventStoreProvider {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<Map<String, String>> mapType = new TypeReference<Map<String, String>>() {
    };
    private static final Logger logger = Logger.getLogger(JpaEventStoreProvider.class);

    private final KeycloakSession session;
    private final EntityManager em;
    private final int maxDetailLength;

    public JpaEventStoreProvider(KeycloakSession session, EntityManager em, int maxDetailLength) {
        this.session = session;
        this.em = em;
        this.maxDetailLength = maxDetailLength;
    }

    @Override
    public EventQuery createQuery() {
        return new JpaEventQuery(em);
    }

    @Override
    public void clear() {
        em.createQuery("delete from EventEntity").executeUpdate();
    }

    @Override
    public void clear(RealmModel realm) {
        em.createQuery("delete from EventEntity e where realmId = :realmId and e.type not in (:excludedTypes)").setParameter("realmId", realm.getId()).setParameter("excludedTypes", Constants.defaultEvts).executeUpdate();
    }

    @Override
    public void clear(RealmModel realm, long olderThan) {
        em.createQuery("delete from EventEntity where realmId = :realmId and time < :time").setParameter("realmId", realm.getId()).setParameter("time", olderThan).executeUpdate();
    }

    @Override
    public void clearExpiredEvents() {
        // By default, realm provider is always "jpa", so we can optimize and delete all events in single SQL, assuming that realms are saved in the DB as well.
        // Fallback to model API just with different realm provider than "jpa" (This is never the case in standard Keycloak installations)
        int numDeleted = 0;
        long currentTimeMillis = Time.currentTimeMillis();
        if (KeycloakModelUtils.isRealmProviderJpa(session)) {

            // Group realms by expiration times. This will be effective if different realms have same/similar event expiration times, which will probably be the case in most environments
            List<Long> eventExpirations = em.createQuery("select distinct realm.eventsExpiration from RealmEntity realm where realm.eventsExpiration > 0").getResultList();
            for (Long expiration : eventExpirations) {
                List<String> realmIds = em.createQuery("select realm.id from RealmEntity realm where realm.eventsExpiration = :expiration")
                        .setParameter("expiration", expiration)
                        .getResultList();
                int currentNumDeleted = em.createQuery("delete from EventEntity where realmId in :realmIds and time < :eventTime")
                        .setParameter("realmIds", realmIds)
                        .setParameter("eventTime", currentTimeMillis - (expiration * 1000))
                        .executeUpdate();
                logger.tracef("Deleted %d events for the expiration %d", currentNumDeleted, expiration);
                numDeleted += currentNumDeleted;
            }
            logger.debugf("Cleared %d expired events in all realms", numDeleted);
        } else {
            session.realms().getRealmsStream().forEach(realm -> {
                if (realm.isEventsEnabled() && realm.getEventsExpiration() > 0) {
                    long olderThan = Time.currentTimeMillis() - realm.getEventsExpiration() * 1000;
                    clear(realm, olderThan);
                }
            });
        }
    }

    @Override
    public void onEvent(Event event,Map<String,String> config) {
        em.persist(convertEvent(event));
    }

    @Override
    public AdminEventQuery createAdminQuery() {
        return new JpaAdminEventQuery(em);
    }

    @Override
    public void clearAdmin() {
        em.createQuery("delete from AdminEventEntity").executeUpdate();
    }

    @Override
    public void clearAdmin(RealmModel realm) {
        Set<String> opTyes = new HashSet<>();
        Set<String> rsTypes = new HashSet<>();
        Constants.defaultAdminEvts.forEach(evt -> {
            if(evt != null){
                String temp[] = evt.split("_",2);
                opTyes.add(temp[0]);
                rsTypes.add(temp[1]);
            }
        });
        em.createQuery("delete from AdminEventEntity e where realmId = :realmId and not (e.operationType in (:excludedOpTypes) and e.resourceType in (:excludedRsTypes))")
                .setParameter("realmId", realm.getId())
                .setParameter("excludedOpTypes",opTyes)
                .setParameter("excludedRsTypes",rsTypes)
                .executeUpdate();
    }

    @Override
    public void clearAdmin(RealmModel realm, long olderThan) {
        em.createQuery("delete from AdminEventEntity where realmId = :realmId and time < :time").setParameter("realmId", realm.getId()).setParameter("time", olderThan).executeUpdate();
    }

    @Override
    public void onEvent(AdminEvent event,Map<String,String> config, boolean includeRepresentation) {
        em.persist(convertAdminEvent(event, includeRepresentation));
    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public List<Object[]> getDatewiseRecords(String realmId, boolean isAdminEvt, EventType evtType, ResourceType rsType, OperationType opType, Date fromDate, Date toDate){
        CriteriaBuilder builder = em.getCriteriaBuilder();

        CriteriaQuery<Object[]> criteriaQuery = builder.createQuery(Object[].class);
        Root<?> root = null;
        ArrayList<Predicate> prs = new ArrayList<Predicate>(4);

        if(isAdminEvt){
            root = criteriaQuery.from(AdminEventEntity.class);
            prs.add(builder.equal(root.get("operationType"),(opType.toString())));
            prs.add(builder.equal(root.get("resourceType"),(rsType.toString())));
        } else {
            root = criteriaQuery.from(EventEntity.class);
            prs.add(builder.equal(root.get("type"),(evtType.toString())));
        }

        prs.add(builder.equal(root.get("realmId"), realmId));

        if(fromDate != null){
            prs.add(builder.greaterThanOrEqualTo(root.<Long>get("time"), fromDate.getTime()));
        }
        if(toDate != null){
            prs.add(builder.lessThanOrEqualTo(root.<Long>get("time"), toDate.getTime()));
        }
        if (!prs.isEmpty()) {
            criteriaQuery.where(builder.and(prs.toArray(new Predicate[prs.size()])));
        }

        //Here we follow the formula for converting timestamp into date
        //Formula ==> timeStampInSecs - (timeStampInSecs % (24 * 60 * 60))
        Expression<Number> timeStampInSecs = builder.quot(root.get("time"), 1000);
        criteriaQuery.groupBy(
                builder.diff(timeStampInSecs,
                        builder.mod(timeStampInSecs.as(Integer.class),86400)
                )
        );
        criteriaQuery.multiselect(
                builder.diff(timeStampInSecs, builder.mod(timeStampInSecs.as(Integer.class),86400)),
                builder.count(root)
        );

        List<Object[]> query = em.createQuery(criteriaQuery).getResultList();

        return query;
    }
    @Override
    public List<String> getTopPerformingTenants(EventType eventType,int maxResults) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> criteriaQuery = builder.createQuery(String.class);
        Root<EventEntity> root = criteriaQuery.from(EventEntity.class);

        ArrayList<Predicate> prs = new ArrayList<Predicate>(4);
        prs.add(builder.equal(root.get("type"), eventType.toString()));
        if (!prs.isEmpty()) {
            criteriaQuery.where(builder.and(prs.toArray(new Predicate[prs.size()])));
        }

        criteriaQuery.groupBy(root.get("realmId"));
        criteriaQuery.orderBy(builder.desc(builder.count(root)));

        criteriaQuery.select(root.get("realmId"));
        List<String> query = em.createQuery(criteriaQuery).setMaxResults(maxResults).getResultList();

        List<String> topPerformingTenants = new ArrayList<>();

        query.forEach(r -> {
            String realmName = getRealmNameById(r);
            if(realmName != null){
                topPerformingTenants.add(realmName);
            }
        });
        return topPerformingTenants;
    }

    @Override
    public boolean enableEventFilter() {
        return true;
    }

    private String getRealmNameById(String realmId){
        List<String> realmName = em.createQuery("select realm.name from RealmEntity realm where realm.id = :id").setParameter("id", realmId).getResultList();
        if(!realmName.isEmpty() && realmName.size() == 1)
            return realmName.get(0);
        return null;
    }


    private EventEntity convertEvent(Event event) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setId(event.getId() == null ? UUID.randomUUID().toString() : event.getId());
        eventEntity.setTime(event.getTime());
        eventEntity.setType(event.getType().toString());
        eventEntity.setRealmId(event.getRealm().getId());
        eventEntity.setClientId(event.getClient().getClientId());
        eventEntity.setUserId(event.getUser().getId());
        eventEntity.setSessionId(event.getSessionId());
        eventEntity.setIpAddress(event.getIpAddress());
        eventEntity.setError(event.getError());
        try {
            if (maxDetailLength > 0 && event.getDetails() != null) {
                Map<String, String> result = new HashMap<>(event.getDetails());
                result.entrySet().forEach(t -> t.setValue(trimToMaxLength(t.getValue())));

                eventEntity.setDetailsJson(mapper.writeValueAsString(result));
            } else {
                eventEntity.setDetailsJson(mapper.writeValueAsString(event.getDetails()));
            }
        } catch (IOException ex) {
            logger.error("Failed to write log details", ex);
        }
        return eventEntity;
    }

    private String trimToMaxLength(String detail) {
        if (detail != null && detail.length() > maxDetailLength) {
            // (maxDetailLength - 3) takes "..." into account
            String result = detail.substring(0, maxDetailLength - 3).concat("...");
            logger.warn("Detail was truncated to " + result);
            return result;
        } else {
            return detail;
        }
    }

    static Event convertEvent(EventEntity eventEntity) {
        Event event = new Event();
        event.setId(eventEntity.getId() == null ? UUID.randomUUID().toString() : eventEntity.getId());
        event.setTime(eventEntity.getTime());
        event.setType(EventType.valueOf(eventEntity.getType()));
        event.setRealm(new RealmRepresentation() {{
            setId(eventEntity.getRealmId());
        }});
        event.setClient(new ClientRepresentation() {{
            setClientId(eventEntity.getClientId());
        }});
        event.setUser(new UserRepresentation(){{
            setId(eventEntity.getUserId());
        }});
        event.setSessionId(eventEntity.getSessionId());
        event.setIpAddress(eventEntity.getIpAddress());
        event.setError(eventEntity.getError());
        try {
            Map<String, String> details = mapper.readValue(eventEntity.getDetailsJson(), mapType);
            event.setDetails(details);
        } catch (IOException ex) {
            logger.error("Failed to read log details", ex);
        }
        return event;
    }

    private AdminEventEntity convertAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        AdminEventEntity adminEventEntity = new AdminEventEntity();
        adminEventEntity.setId(adminEvent.getId() == null ? UUID.randomUUID().toString() : adminEvent.getId());
        adminEventEntity.setTime(adminEvent.getTime());
        adminEventEntity.setRealmId(adminEvent.getRealmId());
        setAuthDetails(adminEventEntity, adminEvent.getAuthDetails());
        adminEventEntity.setOperationType(adminEvent.getOperationType().toString());

        if (adminEvent.getResourceTypeAsString() != null) {
            adminEventEntity.setResourceType(adminEvent.getResourceTypeAsString());
        }

        adminEventEntity.setResourcePath(adminEvent.getResourcePath());
        adminEventEntity.setError(adminEvent.getError());

        if(includeRepresentation) {
            adminEventEntity.setRepresentation(adminEvent.getRepresentation());
        }
        try {
            if (maxDetailLength > 0 && adminEvent.getDetails() != null) {
                Map<String, String> result = new HashMap<>(adminEvent.getDetails());
                result.entrySet().forEach(t -> t.setValue(trimToMaxLength(t.getValue())));

                adminEventEntity.setDetailsJson(mapper.writeValueAsString(result));
            } else {
                adminEventEntity.setDetailsJson(mapper.writeValueAsString(adminEvent.getDetails()));
            }
        } catch (IOException ex) {
            logger.error("Failed to write log details", ex);
        }
        return adminEventEntity;
    }

    static AdminEvent convertAdminEvent(AdminEventEntity adminEventEntity) {
        AdminEvent adminEvent = new AdminEvent();
        adminEvent.setId(adminEventEntity.getId() == null ? UUID.randomUUID().toString() : adminEventEntity.getId());
        adminEvent.setTime(adminEventEntity.getTime());
        adminEvent.setRealmId(adminEventEntity.getRealmId());
        setAuthDetails(adminEvent, adminEventEntity);
        adminEvent.setOperationType(OperationType.valueOf(adminEventEntity.getOperationType()));

        if (adminEventEntity.getResourceType() != null) {
            adminEvent.setResourceTypeAsString(adminEventEntity.getResourceType());
        }

        adminEvent.setResourcePath(adminEventEntity.getResourcePath());
        adminEvent.setError(adminEventEntity.getError());

        if(adminEventEntity.getRepresentation() != null) {
            adminEvent.setRepresentation(adminEventEntity.getRepresentation());
        }
        if(adminEventEntity.getDetailsJson() != null) {
            try {
                Map<String, String> details = mapper.readValue(adminEventEntity.getDetailsJson(), mapType);
                adminEvent.setDetails(details);
            } catch (IOException ex) {
                logger.error("Failed to read log details", ex);
            }
        }
        return adminEvent;
    }

    private static void setAuthDetails(AdminEventEntity adminEventEntity, AuthDetails authDetails) {
        adminEventEntity.setAuthRealmId(authDetails.getRealm().getId());
        adminEventEntity.setAuthClientId(authDetails.getClient().getClientId());
        adminEventEntity.setAuthUserId(authDetails.getUser().getId());
        adminEventEntity.setAuthIpAddress(authDetails.getIpAddress());
    }

    private static void setAuthDetails(AdminEvent adminEvent, AdminEventEntity adminEventEntity) {
        AuthDetails authDetails = new AuthDetails();
        authDetails.setRealm(new RealmRepresentation() {{
            setId(adminEventEntity.getAuthRealmId());
        }});
        authDetails.setClient(new ClientRepresentation() {{
            setClientId(adminEventEntity.getAuthClientId());
        }});
        authDetails.setUser(new UserRepresentation(){{
            setId(adminEventEntity.getAuthUserId());
        }});
        authDetails.setIpAddress(adminEventEntity.getAuthIpAddress());
        adminEvent.setAuthDetails(authDetails);
    }

}

package com.qlikverify.backoffice.services;

import com.qlikverify.backoffice.models.audit.AuditTransactionDetails;
import com.qlikverify.backoffice.models.audit.TransactionsHistoryModel;
import org.keycloak.events.Event;
import org.keycloak.events.EventCollection;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EVAPIResourceService {
    private static final List<String> ignoredEvents = Arrays.asList(
            "EZ_TRANSACTION_START",
            "EZ_TRANSACTION_USER_ADD",
            "EZ_TRANSACTION_REQUEST_COMPLETED",
            "EZ_TRANSACTION_USER_IDENTIFIED",
            "EZ_TRANSACTION_NOT_PERMITTED",
            "EZ_TRANSACTION_NOT_PERMITTED_ERROR",
            "EZ_DEAD_LETTER_HOOK",
            "EZ_TRANSACTION_NOTIFICATION_TRIGGER",
            "EZ_TRANSACTION_NOTIFICATION_TRIGGER_ERROR"
    );

    public Response getAllAuditFlows(KeycloakSession session, RealmModel realm, Integer firstResult, Integer maxResults, String search){
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(realm.getId());

        query.firstResult(0);
        query.maxResults(10000);
        if(StringUtil.isNotBlank(search)){
            query.request(search);
        }

        EventCollection eventCollection = query.getDistinctResult();
        if(eventCollection == null){
            return Response.ok(Collections.emptyList()).build();
        }

        // check if the audit event has request id in its config details and process the events
        // filtering for unique request ids - the function passed (t1, t2) -> t1 always returns the first element and ignores the other elements.
        // so only the first audit event based on the request would be available
        List<TransactionsHistoryModel> uniqueTransactions = new ArrayList<>(eventCollection.getEvents().stream()
                .filter(x -> x.getDetails().containsKey(Constants.REQUEST_ID))
                .map(this::toTransactionHistoryModel)
                .collect(Collectors.toMap(
                        TransactionsHistoryModel::getRequestId,
                        Function.identity(),
                        (t1, t2) -> t1,
                        LinkedHashMap::new
                )).values());

        int totalCount = uniqueTransactions.size();
        if(firstResult > totalCount)
            firstResult = totalCount;

        uniqueTransactions = uniqueTransactions.stream().sorted(Comparator.comparingLong(TransactionsHistoryModel::getTime).reversed()).collect(Collectors.toList());
        List<TransactionsHistoryModel> resultList = uniqueTransactions.subList(firstResult, totalCount);

        if (maxResults > resultList.size())
            maxResults = resultList.size();

        Map<String, Object> result = new HashMap<>();
        result.put("transactions", resultList.subList(0, maxResults));
        result.put("totalCount", totalCount);

        return Response.ok().entity(result).build();
    }

    public TransactionsHistoryModel toTransactionHistoryModel(Event event){
        return TransactionsHistoryModel.builder()
                .requestId(event.getDetails().get(Constants.REQUEST_ID))
                .clientId(event.getClient() != null ? event.getClient().getClientId() : null)
                .time(event.getTime())
                .build();
    }

    public Response getTransactionDetails(KeycloakSession session, RealmModel realm, String transactionId) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        List<AuditTransactionDetails> auditEvents = new ArrayList<>();
        EventQuery query = eventStore.createQuery().realm(realm.getId());

        EventCollection eventCollection = query.request(transactionId).getResult();
        if(eventCollection == null){
            return Response.ok(Collections.emptyList()).build();
        }

        eventCollection.getEvents().forEach(x -> {
            if (ignoredEvents.contains(x.getType().name()))
                return;
            // handle skiped
            auditEvents.add(toAuditTransactionDetails(x));
        });
        auditEvents.sort(Comparator.comparing(AuditTransactionDetails::getTime));
        return Response.ok().entity(auditEvents).build();
    }

    public AuditTransactionDetails toAuditTransactionDetails(Event event){
        AuditTransactionDetails auditTransactionDetails = new AuditTransactionDetails();
        auditTransactionDetails.setEventName(event.getType().name());
        auditTransactionDetails.setError(event.getError());
        auditTransactionDetails.setTime(event.getTime());
        if ("EZ_VALIDATOR".equals(event.getType().name())) {
            auditTransactionDetails.setDirection(event.getDetails().get("Validator Condition: ").endsWith(Constants.VALIDATOR_PASSED));
        }
        if (event.getDetails().containsKey("User Action")) {
            auditTransactionDetails.setSkipped(event.getDetails().get("User Action").equals("user_skipped"));
        }
        return auditTransactionDetails;
    }
}

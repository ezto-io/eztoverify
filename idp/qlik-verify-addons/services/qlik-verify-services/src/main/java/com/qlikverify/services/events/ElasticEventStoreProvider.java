package com.qlikverify.services.events;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.services.events.models.ElasticAdminEventModel;
import com.qlikverify.services.events.models.ElasticEventModel;
import com.qlikverify.services.events.models.response.ESModel;
import com.qlikverify.services.events.query.ElasticAdminEventQuery;
import com.qlikverify.services.events.query.ElasticEventQuery;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import de.danielbechler.diff.node.Visit;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.keycloak.events.*;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorPageException;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.qlikverify.services.constants.Constants.CONTACT_ADMIN;

public class ElasticEventStoreProvider implements EventStoreProvider {

    KeycloakSession session;

    private static final Logger logger = Logger.getLogger(ElasticEventStoreProvider.class);
    // Lenient mapper: tolerate ES-version field drift (e.g. "_type" in ES 7.x hits) when parsing responses.
    private static final ObjectMapper ES_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String ES_DOWN_ERROR = "Elastic search is down, failed to get es client";

    public ElasticEventStoreProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public EventQuery createQuery() {
        try{
            ElasticSearch.getESClient().info();
        } catch(Exception e){
            logger.error(ES_DOWN_ERROR);
            throw new ErrorPageException(session, javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE, CONTACT_ADMIN);
        }
        return new ElasticEventQuery();
    }

    @Override
    public AdminEventQuery createAdminQuery() {
        try{
            ElasticSearch.getESClient().info();
        } catch(Exception e){
            logger.error(ES_DOWN_ERROR);
            throw new ErrorPageException(session, javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE, CONTACT_ADMIN);
        }
        return new ElasticAdminEventQuery();
    }

    @Override
    public void clear() {
        ElasticSearch.remove("log");
    }

    @Override
    public void clear(RealmModel realm) {
        ElasticSearch.remove(realm, "log");
    }

    @Override
    public void clear(RealmModel realm, long olderThan) {
        // NOP
    }

    @Override
    public void clearExpiredEvents() {
        // NOP
    }

    @Override
    public void clearAdmin() {
        ElasticSearch.remove("admin_log");
    }

    @Override
    public void clearAdmin(RealmModel realm) {
        ElasticSearch.remove(realm, "admin_log");
    }

    @Override
    public void clearAdmin(RealmModel realm, long olderThan) {
        // NOP
    }

    @Override
    public List<Object[]> getDatewiseRecords(String realmId, boolean isAdminEvt, EventType evtType, ResourceType rsType,
            OperationType opType, Date fromDate, Date toDate) {

        List<Query> queries = new ArrayList<>();

        if (isAdminEvt) {
            queries.add(Query.of(x -> x.match(e -> e.query("admin_log").field("eventType"))));
            List<FieldValue> opValues = new ArrayList<>();
            opValues.add(new FieldValue.Builder().stringValue(opType.name().toLowerCase(Locale.ROOT)).build());

            if (!opValues.isEmpty()) {
                queries.add(
                        Query.of(x -> x.terms(tr -> tr
                                .field("eventData.operationType")
                                .terms(tf -> tf
                                        .value(opValues)))));
            }

            List<FieldValue> reValues = new ArrayList<>();
            reValues.add(new FieldValue.Builder().stringValue(rsType.name().toLowerCase(Locale.ROOT)).build());

            if (!reValues.isEmpty()) {
                queries.add(
                        Query.of(x -> x.terms(tr -> tr
                                .field("eventData.resourceType")
                                .terms(tf -> tf
                                        .value(reValues)))));
            }
            if (realmId != null) {
                queries.add(Query.of(x -> x.match(e -> e.query(realmId).field("eventData.realmId"))));
            }
        } else {
            queries.add(Query.of(x -> x.match(e -> e.query("log").field("eventType"))));
            List<FieldValue> fieldValues = new ArrayList<>();
            fieldValues.add(new FieldValue.Builder().stringValue(evtType.name().toLowerCase(Locale.ROOT)).build());
            if (!fieldValues.isEmpty()) {
                queries.add(
                        Query.of(x -> x.terms(tr -> tr
                                .field("eventData.type")
                                .terms(tf -> tf
                                        .value(fieldValues)))));
            }
            if (realmId != null) {
                queries.add(Query.of(x -> x.match(e -> e.query(realmId).field("eventData.realm.id"))));
            }
        }

        if (fromDate != null) {
            queries.add(Query.of(x -> x.range(e -> e.gte(JsonData.of(fromDate.getTime())).field("eventData.time"))));
        }
        if (toDate != null) {
            queries.add(Query.of(x -> x.range(e -> e.lte(JsonData.of(toDate.getTime())).field("eventData.time"))));
        }

        SearchRequest searchRequest = SearchRequest.of(x -> x
                .query(QueryBuilders
                        .bool(b -> b
                                .must(queries)))
                .aggregations("range",
                        Aggregation.of(a -> a.dateHistogram(DateHistogramAggregation
                                .of(d -> d.field("eventData.time").fixedInterval(i -> i.time("1d"))))))
                .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("eventData.time").order(SortOrder.Asc)))))
                .size(0));

        List<String> splitted = Arrays.stream(searchRequest.toString().split(" ")).skip(1).collect(Collectors.toList());
        logger.info("--------ES URL--------");
        logger.info(splitted.get(1));
        logger.info("--------ES Query--------");
        logger.info(splitted.get(2));
        logger.info("------------------------");
        HttpUtility httpUtility = new HttpUtility();
        Response response = null;
        try {
            response = httpUtility.callRequest("application/json", splitted.get(0), ElasticSearch.getUrl(realmId),
                    splitted.get(2), null);
            if (response != null && response.isSuccessful()) {
                ESModel result = ES_MAPPER.readValue(response.body().string(), ESModel.class);

                return result.aggregations.field.get("by_requestId").buckets.stream()
                        .map(b -> new Object[] { ((Long) b.key).longValue() / 1000, b.docCount })
                        .collect(Collectors.toList());
            }
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return new ArrayList<>();
    }

    @Override
    public List<String> getTopPerformingTenants(EventType eventType, int maxResults) {
        List<Query> queries = new ArrayList<>();
        queries.add(Query.of(x -> x.match(e -> e.query("log").field("eventType"))));
        List<FieldValue> fieldValues = new ArrayList<>();
        fieldValues.add(new FieldValue.Builder().stringValue(eventType.name().toLowerCase(Locale.ROOT)).build());
        if (!fieldValues.isEmpty()) {
            queries.add(
                    Query.of(x -> x.terms(tr -> tr
                            .field("eventData.type")
                            .terms(tf -> tf
                                    .value(fieldValues)))));
        }

        SearchRequest searchRequest = SearchRequest.of(x -> x
                .query(QueryBuilders
                        .bool(b -> b
                                .must(queries)))
                .aggregations("range", Aggregation.of(a -> a.terms(TermsAggregation.of(d -> d.field("_index")))))
                .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("eventData.time").order(SortOrder.Asc)))))
                .size(0));

        List<String> splitted = Arrays.stream(searchRequest.toString().split(" ")).skip(1).collect(Collectors.toList());
        logger.info("--------ES URL--------");
        logger.info(splitted.get(1));
        logger.info("--------ES Query--------");
        logger.info(splitted.get(2));
        logger.info("------------------------");
        HttpUtility httpUtility = new HttpUtility();
        Response response = null;
        try {
            response = httpUtility.callRequest("application/json", splitted.get(0), ElasticSearch.getUrl() + "/_search",
                    splitted.get(2), null);
            if (response != null && response.isSuccessful()) {
                ESModel result = ES_MAPPER.readValue(response.body().string(), ESModel.class);

                return result.aggregations.field.get("range").buckets.stream().map(b -> b.key.toString()).limit(5)
                        .map(x -> session.realms().getRealm(x)).map(x -> x.getName()).collect(Collectors.toList());
            }
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return new ArrayList<>();
    }

    @Override
    public void onEvent(Event event, Map<String, String> config) {
        try{
            ElasticSearch.getESClient().info();
        } catch(Exception e){
            logger.error(ES_DOWN_ERROR);
            return;
        }
        event.setId(UUID.randomUUID().toString());
        RealmModel realmModel = session.getContext().getRealm() == null
                ? session.realms().getRealm(event.getRealm().getId())
                : session.getContext().getRealm();
        ElasticSearch.insert(realmModel, event.getId(), new ElasticEventModel(event));
    }

    @Override
    public void onEvent(AdminEvent eve, Map<String, String> config, boolean includeRepresentation) {
        try{
            ElasticSearch.getESClient().info();
        } catch(Exception e){
            logger.error(ES_DOWN_ERROR);
            return;
        }
        AdminEvent event1 = new AdminEvent(eve);

        if (event1.getId() == null) {
            event1.setId(UUID.randomUUID().toString());
        }

        List<String> changes = new ArrayList<>();
        if (event1.getOldRepresentation() != null && event1.getRepresentation() != null) {
            Object obj1 = JsonSerialization.convertValue(event1.getRepresentation(), Object.class);
            Object obj2 = JsonSerialization.convertValue(event1.getOldRepresentation(), Object.class);
            DiffNode diff = ObjectDifferBuilder.buildDefault().compare(obj1, obj2);
            diff.visit(new DiffNode.Visitor() {
                @Override
                public void node(DiffNode node, Visit visit) {
                    final Object baseValue = node.canonicalGet(obj2);
                    final Object workingValue = node.canonicalGet(obj1);
                    final String message = node.getPath() + " changed from " +
                            baseValue + " to " + workingValue;
                    changes.add(message);
                }
            });
        }
        if (!changes.isEmpty()) {
            try {
                event1.setRepresentation(JsonSerialization.writeValueAsString(changes));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        RealmModel realmModel = session.getContext().getRealm() != null ?
                session.getContext().getRealm():
                session.realms().getRealm(event1.getRealmId());
        ElasticSearch.insert(realmModel, event1.getId(), new ElasticAdminEventModel(event1));
    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return null;
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public boolean enableEventFilter() {
        return false;
    }

}

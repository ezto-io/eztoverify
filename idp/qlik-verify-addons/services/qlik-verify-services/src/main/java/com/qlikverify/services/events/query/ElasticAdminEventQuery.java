package com.qlikverify.services.events.query;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.services.events.ElasticSearch;
import com.qlikverify.services.events.models.response.AESModel;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.keycloak.events.admin.*;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ElasticAdminEventQuery implements AdminEventQuery {
    private final static Logger logger = Logger.getLogger(ElasticEventQuery.class);
    // Lenient mapper: tolerate ES-version field drift (e.g. "_type" in ES 7.x hits) when parsing responses.
    private static final ObjectMapper ES_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    List<Query> queries;
    private Integer firstResult;
    private Integer maxResults;
    private String realmId;

    public ElasticAdminEventQuery() {
        if (queries == null) {
            queries = new ArrayList<>();
            queries.add(Query.of(x -> x.match(e -> e.query("admin_log").field("eventType"))));
        }
    }

    @Override
    public AdminEventQuery realm(String realmId) {
        if (realmId != null) {
            this.realmId = realmId;
            queries.add(Query.of(x -> x.match(e -> e.query(realmId).field("eventData.realmId"))));
        }
        return this;
    }

    @Override
    public AdminEventQuery authRealm(String realmId) {
        if (realmId != null) {
            queries.add(Query.of(x -> x.match(e -> e.query(realmId).field("eventData.authDetails.realmId"))));
        }
        return this;
    }

    @Override
    public AdminEventQuery authClient(String clientId) {
        if (clientId != null) {
            queries.add(Query.of(x -> x.match(e -> e.query(clientId).field("eventData.authDetails.clientId"))));
        }
        return this;
    }

    @Override
    public AdminEventQuery authUser(String userId) {
        if (userId != null) {
            queries.add(Query.of(x -> x.match(e -> e.query(userId).field("eventData.authDetails.userId"))));
        }
        return this;
    }

    @Override
    public AdminEventQuery authIpAddress(String ipAddress) {
        if (ipAddress != null) {
            queries.add(Query.of(x -> x.match(e -> e.query(ipAddress).field("eventData.authDetails.ipAddress"))));
        }
        return this;
    }

    @Override
    public AdminEventQuery operation(OperationType... operations) {
        List<FieldValue> fieldValues = new ArrayList<>();
        for (OperationType e : operations) {
            fieldValues.add(new FieldValue.Builder().stringValue(e.name().toLowerCase(Locale.ROOT)).build());
        }

        if (!fieldValues.isEmpty()) {
            queries.add(
                    Query.of(x -> x.terms(tr -> tr
                            .field("eventData.operationType")
                            .terms(tf -> tf
                                    .value(fieldValues))))
            );
        }
        return this;
    }

    @Override
    public AdminEventQuery resourceType(ResourceType... resourceTypes) {
        List<FieldValue> fieldValues = new ArrayList<>();
        for (ResourceType e : resourceTypes) {
            fieldValues.add(new FieldValue.Builder().stringValue(e.name().toLowerCase(Locale.ROOT)).build());
        }

        if (!fieldValues.isEmpty()) {
            queries.add(
                    Query.of(x -> x.terms(tr -> tr
                            .field("eventData.resourceType")
                            .terms(tf -> tf
                                    .value(fieldValues))))
            );
        }
        return this;
    }

    @Override
    public AdminEventQuery resourcePath(String resourcePath) {
        return this;
    }

    @Override
    public AdminEventQuery fromTime(Date fromTime) {
        if (fromTime != null) {
            queries.add(Query.of(x -> x.range(e -> e.gte(JsonData.of(fromTime.getTime())).field("eventData.time"))));
        }
        return this;
    }

    @Override
    public AdminEventQuery toTime(Date toTime) {
        if (toTime != null) {
            queries.add(Query.of(x -> x.range(e -> e.lte(JsonData.of(toTime.getTime())).field("eventData.time"))));
        }
        return this;
    }

    @Override
    public AdminEventQuery firstResult(int first) {
        this.firstResult = first;
        return this;
    }

    @Override
    public AdminEventQuery maxResults(int max) {
        this.maxResults = max;
        return this;
    }

    @Override
    public Stream<AdminEvent> getResultStream() {

        SearchRequest searchRequest = SearchRequest.of(x ->
                x
                        .query(QueryBuilders
                                .bool(b -> b
                                        .must(queries))).sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("eventData.time").order(SortOrder.Desc))))).from(firstResult).size(maxResults));

        List<String> splitted = Arrays.stream(searchRequest.toString().split(" ")).skip(1).collect(Collectors.toList());
        logger.info("--------ES URL--------");
        logger.info(splitted.get(1));
        logger.info("--------ES Query--------");
        logger.info(splitted.get(2));
        logger.info("------------------------");
        HttpUtility httpUtility = new HttpUtility();
        Response response = null;
        try {
            response = httpUtility.callRequest("application/json", splitted.get(0), ElasticSearch.getUrl() + "/" + realmId + "/_search", splitted.get(2), null);
            if (response != null && response.isSuccessful()) {
                AESModel result = ES_MAPPER.readValue(response.body().string(), AESModel.class);
                if (result.hits.total.value > 0) {
                    return result.hits.hits.stream().map(x -> x.source.eventData);
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return Stream.empty();
    }

    @Override
    public AdminEventCollection getResult() {

        SearchRequest searchRequest = SearchRequest.of(x ->
                x
                        .query(QueryBuilders
                                .bool(b -> b
                                        .must(queries))).sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("eventData.time").order(SortOrder.Desc))))).from(firstResult).size(maxResults));

        List<String> splitted = Arrays.stream(searchRequest.toString().split(" ")).skip(1).collect(Collectors.toList());
        logger.info("--------ES URL--------");
        logger.info(splitted.get(1));
        logger.info("--------ES Query--------");
        logger.info(splitted.get(2));
        logger.info("------------------------");
        HttpUtility httpUtility = new HttpUtility();
        Response response = null;
        try {
            response = httpUtility.callRequest("application/json", splitted.get(0), ElasticSearch.getUrl(realmId), splitted.get(2), null);
            if (response != null && response.isSuccessful()) {
                AESModel result = ES_MAPPER.readValue(response.body().string(), AESModel.class);
                if (result.hits.total.value > 0) {
                    AdminEventCollection adminEventCollection = new AdminEventCollection();
                    adminEventCollection.setTotal(result.hits.total.value);
                    adminEventCollection.setEvents(result.hits.hits.stream().map(x -> x.source.eventData).collect(Collectors.toList()));
                    return adminEventCollection;
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return AdminEventCollection.empty();
    }
}

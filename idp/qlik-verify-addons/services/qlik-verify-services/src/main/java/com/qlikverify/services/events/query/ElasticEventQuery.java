package com.qlikverify.services.events.query;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.services.events.ElasticSearch;
import com.qlikverify.services.events.models.response.ESModel;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.keycloak.events.*;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ElasticEventQuery implements EventQuery {

    private final static Logger logger = Logger.getLogger(ElasticEventQuery.class);
    // Lenient mapper: Elasticsearch 7.x still emits "_type" in hits (removed in ES 8) and may add
    // fields across versions. Tolerate unknown props instead of breaking the event-listener path.
    private static final ObjectMapper ES_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    List<Query> queries;
    private Integer firstResult;
    private Integer maxResults;
    private String realmId;

    public ElasticEventQuery() {
        if (queries == null) {
            queries = new ArrayList<>();
            queries.add(Query.of(x -> x.match(e -> e.query("log").field("eventType"))));
        }
    }

    private List<FieldValue> lastSorted = null;

    @Override
    public EventQuery type(EventType... types) {
        List<FieldValue> fieldValues = new ArrayList<>();
        for (EventType e : types) {
            fieldValues.add(new FieldValue.Builder().stringValue(e.name().toLowerCase(Locale.ROOT)).build());
        }

        if (!fieldValues.isEmpty()) {
            queries.add(
                    Query.of(x -> x.terms(tr -> tr
                            .field("eventData.type")
                            .terms(tf -> tf
                                    .value(fieldValues))))
            );
        }
        return this;
    }

    @Override
    public EventQuery realm(String realmId) {
        if (realmId != null) {
            this.realmId = realmId;
            queries.add(Query.of(x -> x.match(e -> e.query(realmId).field("eventData.realm.id"))));
        }
        return this;
    }

    @Override
    public EventQuery client(String clientId) {
        if (clientId != null) {
            queries.add(Query.of(x -> x.match(e -> e.query(clientId).field("eventData.client.clientId"))));
        }
        return this;
    }

    @Override
    public EventQuery user(String userId) {
        if (userId != null) {
            queries.add(Query.of(x -> x.match(e -> e.query(userId).field("eventData.user.id"))));
        }
        return this;
    }

    @Override
    public EventQuery fromDate(Date fromDate) {
        if (fromDate != null) {
            queries.add(Query.of(x -> x.range(e -> e.gte(JsonData.of(fromDate.getTime())).field("eventData.time"))));
        }
        return this;
    }

    @Override
    public EventQuery toDate(Date toDate) {
        if (toDate != null) {
            queries.add(Query.of(x -> x.range(e -> e.lte(JsonData.of(toDate.getTime())).field("eventData.time"))));
        }
        return this;
    }

    @Override
    public EventQuery ipAddress(String ipAddress) {
        if (ipAddress != null) {
            queries.add(Query.of(x -> x.match(e -> e.query(ipAddress).field("eventData.ipAddress"))));
        }
        return this;
    }

    @Override
    public EventQuery firstResult(int result) {
        this.firstResult = result;
        return this;
    }

    @Override
    public EventQuery maxResults(int results) {
        this.maxResults = results;
        return this;
    }

    @Override
    public EventQuery request(String requestId) {
        if (requestId != null) {
            queries.add(Query.of(x -> x.term(e -> e.field("eventData.details.requestId.keyword").value(requestId))));
        }
        return this;
    }

    @Override
    public Stream<Event> getResultStream() {

        SearchRequest searchRequest = SearchRequest.of(x -> x
                .query(QueryBuilders.bool(b -> b.must(queries)))
                .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("eventData.time").order(SortOrder.Desc)))))
                .from(firstResult).size(maxResults)
        );

        List<String> splitted = Arrays.stream(searchRequest.toString().split(" ")).skip(1).collect(Collectors.toList());
        logger.info("--------ES URL--------");
        logger.info(splitted.get(1));
        logger.info("--------ES Query--------");
        logger.info(splitted.get(2));
        logger.info("------------------------");
        HttpUtility httpUtility = new HttpUtility();
        Response response = null;
        try {
            response = httpUtility.callRequest("application/json", splitted.get(0), ElasticSearch.getUrl() + splitted.get(1), splitted.get(2), null);
            if (response != null && response.isSuccessful()) {
                ESModel result = ES_MAPPER.readValue(response.body().string(), ESModel.class);
                if (result.hits.total.value == 0)
                    return Stream.empty();

                return result.hits.hits.stream().map(x -> x.source.eventData);
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
    public EventCollection getResult() {
        int from = firstResult != null ? firstResult : 0;
        int pageSize = maxResults != null ? maxResults : 10000;
        Integer totalHitCounts = getTotalHitCounts();
        List<Event> events;
        EventCollection eventCollection = new EventCollection();
        eventCollection.setTotal(totalHitCounts);

        if (totalHitCounts > 0) {
            try {
                events = retrieveEvents(from, pageSize, totalHitCounts, queries);
            } catch (IOException e) {
                logger.error("Failed to get events from Elasticsearch", e);
                return EventCollection.empty();
            }

            eventCollection.setEvents(events);
        }

        logger.info("Total events: " + eventCollection.getTotal());
        return eventCollection.getTotal() > 0 ? eventCollection : EventCollection.empty();
    }

    private List<Event> retrieveEvents(int from, int pageSize, int totalHitCounts, List<Query> queries) throws IOException {
        int retrieved = 0;

        // Retrieve the first batch of results without using search_after
        List<Event> events = new ArrayList<>(retrieveEventBatch(from, pageSize, queries, null));
        if (events.size() == 10000 && totalHitCounts > 10000) {
            retrieved += events.size();
            while (retrieved < totalHitCounts) {
                List<Event> batch = retrieveEventBatch(from, pageSize, queries, lastSorted);
                events.addAll(batch);
                retrieved += batch.size();
                lastSorted = batch.isEmpty() ? null : lastSorted;
            }
        }

        return events;
    }

    private List<Event> retrieveEventBatch(int from, int pageSize, List<Query> queries, List<FieldValue> searchAfter) throws IOException {
        SearchRequest searchRequest = createSearchRequest(from, pageSize, queries, searchAfter);
        Response response = sendSearchRequest(searchRequest);
        return processSearchResponse(response);
    }

    private Response sendSearchRequest(SearchRequest searchRequest) throws IOException {
        List<String> splitted = Arrays.stream(searchRequest.toString().split(" ")).skip(1).collect(Collectors.toList());
        logger.info("--------ES URL--------");
        logger.info(splitted.get(1));
        logger.info("--------ES Query--------");
        logger.info(splitted.get(2));
        logger.info("------------------------");

        HttpUtility httpUtility = new HttpUtility();
        return httpUtility.callRequest("application/json", splitted.get(0), ElasticSearch.getUrl(realmId), splitted.get(2), null);
    }

    private List<Event> processSearchResponse(Response response) throws IOException {
        if (response != null && response.isSuccessful()) {
            ESModel result = ES_MAPPER.readValue(response.body().string(), ESModel.class);
            if (result.hits.total.value > 0) {
                List<Long> sort = result.hits.hits.get(result.hits.hits.size() - 1).sort;
                if (sort.isEmpty()){
                    lastSorted = null;
                } else {
                    FieldValue filedValue = new FieldValue.Builder().longValue(sort.get(0)).build();
                    lastSorted = List.of(filedValue);
                }
                return result.hits.hits.stream()
                        .map(hit -> hit.source.eventData)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private SearchRequest createSearchRequest(int from, int pageSize, List<Query> queries, List<FieldValue> searchAfter) {
        SearchRequest.Builder builder = new SearchRequest.Builder();

        builder.query(QueryBuilders.bool(b -> b.must(queries)));
        builder.sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("eventData.time").order(SortOrder.Desc)))));
        builder.from(from);
        builder.size(pageSize);

        SearchRequest.of(x ->
                x.query(QueryBuilders.bool(b -> b.must(queries)))
                        .sort(SortOptions.of(s -> s.field(FieldSort.of(f -> f.field("eventData.time").order(SortOrder.Desc)))))
                        .size(pageSize));

        if (searchAfter != null && !searchAfter.isEmpty()) {
            builder.searchAfter(searchAfter);
        }

        return builder.build();
    }

    @Override
    public Integer getEventsCount() {
        return getTotalHitCounts();
    }

    private Integer getTotalHitCounts() {
        Query query = QueryBuilders.bool(b -> b.must(queries));
        SearchRequest searchRequest = SearchRequest.of(x -> x
                .query(query)
                .size(0)
                .trackTotalHits(new TrackHits.Builder().enabled(true).build())
        );
        List<String> split = Arrays.stream(searchRequest.toString().split(" ")).skip(1).collect(Collectors.toList());
        HttpUtility httpUtility = new HttpUtility();
        Response response = null;
        try {
            response = httpUtility.callRequest("application/json", split.get(0), ElasticSearch.getUrl(realmId), split.get(2), null);
            ESModel result = ES_MAPPER.readValue(response.body().string(), ESModel.class);
            return result.hits.total.value;
        } catch (Exception e) {
            logger.error("Failed to get total hits from elastic search for the given query" + e.getMessage());
        } finally {
            if (!Objects.isNull(response)) {
                response.close();
            }
        }
        return 0;
    }

    @Override
    public EventCollection getDistinctResult() {
        int pageSize = maxResults != null ? maxResults : 10000;
        List<Event> events;
        EventCollection eventCollection = new EventCollection();

        int totalHitCounts = getDistinctTotalHitCounts();
        eventCollection.setTotal(totalHitCounts);

        if (totalHitCounts > 0) {
            try {
                events = retrieveDistinctEvents(pageSize, totalHitCounts, queries);
            } catch (IOException e) {
                logger.error("Failed to get events from Elasticsearch", e);
                return EventCollection.empty();
            }

            eventCollection.setEvents(events);
        }

        logger.info("Total events: " + eventCollection.getTotal());
        return eventCollection.getTotal() > 0 ? eventCollection : EventCollection.empty();
    }

    @Override
    public Integer getDistinctTotalHitCounts() {
        Query query = QueryBuilders.bool(b -> b.must(queries));
        SearchRequest searchRequest = SearchRequest.of(x -> x
                .query(query)
                .size(0)
                .aggregations("by_requestId", a -> a
                        .terms(t -> t
                                .field("eventData.details.requestId.keyword") // must be keyword
                                .size(10000) // number of unique requestIds to fetch
                        )
                        .aggregations("latest_doc", sub -> sub
                                .topHits(th -> th
                                        .size(1) // keep only 1 doc per requestId
                                        .sort(s -> s
                                                .field(f -> f
                                                        .field("eventData.time")
                                                        .order(SortOrder.Desc)
                                                )
                                        )
                                )
                        )
                )
                .trackTotalHits(new TrackHits.Builder().enabled(true).build())
        );

        List<String> split = Collections.emptyList();
        if(searchRequest == null) {
            return 0;
        } else {
            try {
                split = Arrays.stream(searchRequest.toString().split(" ")).skip(1).collect(Collectors.toList());
            } catch (Exception e){
                logger.error("failed to process search request object");
                return 0;
            }
        }

        HttpUtility httpUtility = new HttpUtility();
        try (Response response = httpUtility.callRequest(
                "application/json",
                split.get(0),
                ElasticSearch.getUrl(realmId),
                split.get(2),
                null)
        ) {
            ESModel result = ES_MAPPER.readValue(response.body().string(), ESModel.class);
            return result.aggregations.field.get("by_requestId").buckets.size();
        } catch (Exception e) {
            logger.error("Failed to get total hits from elastic search for the given query" + e.getMessage());
        }
        return 0;
    }

    private List<Event> retrieveDistinctEvents(int pageSize, int totalHitCounts, List<Query> queries) throws IOException {
        int retrieved = 0;

        // Retrieve the first batch of results without using search_after
        List<Event> events = new ArrayList<>(getDetailsFromES(pageSize, queries, null));
        if (events.size() == 10000 && totalHitCounts > 10000) {
            retrieved += events.size();
            while (retrieved < totalHitCounts) {
                List<Event> batch = getDetailsFromES(pageSize, queries, lastSorted);
                events.addAll(batch);
                retrieved += batch.size();
                lastSorted = batch.isEmpty() ? null : lastSorted;
            }
        }

        return events;
    }

    public List<Event> getDetailsFromES(int pageSize, List<Query> queries, List<FieldValue> searchAfter) throws IOException{
        SearchRequest.Builder builder = new SearchRequest.Builder();

        builder.query(QueryBuilders.bool(b -> b.must(queries)));
        builder.from(0);
        builder.size(0);

        builder.aggregations("by_requestId", a -> a
                .terms(t -> t
                        .field("eventData.details.requestId.keyword") // must be keyword
                        .size(pageSize) // number of unique requestIds to fetch
                )
                .aggregations("latest_doc", sub -> sub
                        .topHits(th -> th
                                .size(1) // keep only 1 doc per requestId
                                .sort(s -> s
                                        .field(f -> f
                                                .field("eventData.time")
                                                .order(SortOrder.Desc)
                                        )
                                )
                        )
                )
        );

        if (searchAfter != null && !searchAfter.isEmpty()) {
            builder.searchAfter(searchAfter);
        }

        List<String> splitted = Arrays.stream(builder.build().toString().split(" ")).skip(1).collect(Collectors.toList());
        HttpUtility httpUtility = new HttpUtility();

        try (Response response = httpUtility.callRequest("application/json", splitted.get(0), ElasticSearch.getUrl(realmId), splitted.get(2), null)){
            return processDistinctSearchResponse(response);
        } catch (Exception e){
            logger.error(e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    private List<Event> processDistinctSearchResponse(Response response) throws IOException {
        if (response != null && response.isSuccessful()) {
            ESModel result = ES_MAPPER.readValue(response.body().string(), ESModel.class);
            List<Event> eventList = new ArrayList<>();
            result.aggregations.field.get("by_requestId").buckets.forEach(x -> {
                eventList.addAll(x.field.get("latest_doc").hits.hits.stream()
                        .map(eventHit -> eventHit.source.eventData).collect(Collectors.toList()));
            });
            return eventList;
        }
        return new ArrayList<>();
    }
}

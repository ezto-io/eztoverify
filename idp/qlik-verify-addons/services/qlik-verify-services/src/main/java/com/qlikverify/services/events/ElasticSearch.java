package com.qlikverify.services.events;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.qlikverify.services.events.models.ElasticModel;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.models.RealmModel;

import java.io.IOException;

public class ElasticSearch {

    private final static Logger logger = Logger.getLogger(ElasticSearch.class);
    private static ElasticsearchClient esClientLocal;

    public static ElasticsearchClient getESClient() {
        if(esClientLocal != null)
            return esClientLocal;

        RestClient client = RestClient.builder(getUri()).build();
        ElasticsearchTransport transport = new RestClientTransport(
                client, new JacksonJsonpMapper());
        esClientLocal = new ElasticsearchClient(transport);
        return esClientLocal;
    }

    public static String getUrl() {
        return getUri().toURI();
    }

    public static String getUrl(String realmId){
        return getUrl() + (realmId != null ? "/" + realmId + "/_search" : "/_search");
    }

    private static HttpHost getUri() {
        String host = EnvironmentReader.getValue("ES_HOST") != null ? EnvironmentReader.getValue("ES_HOST") : "localhost";
        int port = EnvironmentReader.getValue("ES_PORT") != null ? Integer.parseInt(EnvironmentReader.getValue("ES_PORT")) : 9200;
        String scheme = EnvironmentReader.getValue("ES_SCHEME") != null ? EnvironmentReader.getValue("ES_SCHEME") : "http";

        return new HttpHost(host, port, scheme);
    }

    private static boolean createRealm(RealmModel realmModel) {
        CreateIndexRequest createIndexRequest = CreateIndexRequest.of(i -> i.index(realmModel.getId())
                .aliases(realmModel.getName(), x -> x.indexRouting(realmModel.getId())));
        CreateIndexResponse createIndexResponse = null;
        try {
            createIndexResponse = getESClient().indices().create(createIndexRequest);
            logger.info("Create Realm Index :" + createIndexResponse.index());
            return true;
        } catch (IOException e) {
            logger.error("Failed to create Realm Index :" + e.getMessage());
        }
        return false;
    }

    public static void insert(RealmModel realmModel, String id, ElasticModel data) {
        if (isRealmExists(realmModel)) {
            IndexRequest<ElasticModel> indexRequest = IndexRequest.of(i -> i.index(realmModel.getId())
                    .id(id)
                    .document(data));
            try {
                IndexResponse indexResponse = getESClient().index(indexRequest);
                logger.info("Added Event : " + indexResponse.index());
            } catch (IOException e) {
                logger.error("Failed to add Event");
                logger.error(e);
            }
        }
    }

    public static void removeAll(RealmModel realmModel) {
        if (isRealmExists(realmModel)) {
            DeleteIndexRequest deleteIndexRequest = DeleteIndexRequest.of(x -> x.index(realmModel.getId()));
            try {
                DeleteIndexResponse deleteIndexResponse = getESClient().indices().delete(deleteIndexRequest);
                if (deleteIndexResponse.acknowledged()) {
                    logger.info("Remove Realm :" + realmModel.getId());
                }
            } catch (IOException e) {
                logger.error("Failed to remove realm " + realmModel.getId() + " events");
                logger.error(e);
            }
        }
    }

    public static void remove(RealmModel realmModel, String eventType) {
        if (isRealmExists(realmModel)) {
            DeleteByQueryRequest queryRequest = DeleteByQueryRequest.of(x -> x.index(realmModel.getId())
                    .query(QueryBuilders.match(q -> q.query(eventType).field("eventType"))));
            try {
                DeleteByQueryResponse response = getESClient().deleteByQuery(queryRequest);
                logger.info(response.deleted() + " events are deleted of type " + eventType + " for realm " + realmModel.getId());
            } catch (IOException e) {
                logger.error(e);
                logger.error("Failed to delete event of type " + eventType + " for realm " + realmModel.getId());
            }
        }
    }

    public static void remove(String eventType) {
        DeleteByQueryRequest queryRequest = DeleteByQueryRequest.of(x -> x
                .query(QueryBuilders.match(q -> q.query(eventType).field("eventType"))));
        try {
            DeleteByQueryResponse response = getESClient().deleteByQuery(queryRequest);
            logger.info(response.deleted() + " events are deleted of type " + eventType);
        } catch (IOException e) {
            logger.error(e);
            logger.error("Failed to delete event of type " + eventType);
        }
    }

    private static boolean isRealmExists(RealmModel realmModel) {
        boolean exists = false;
        try {
            exists = getESClient().indices().exists(ExistsRequest.of(x -> x.index(realmModel.getId()))).value();
        } catch (IOException e) {
        }
        if (!exists) {
            exists = createRealm(realmModel);
        }

        return exists;
    }
}

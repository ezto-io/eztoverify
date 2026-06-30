package com.qlikverify.backoffice.apiconnector.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.backoffice.helpers.apiConnectorHelper.ApiConnectorHelper;
import com.qlikverify.extensions.admin.controllers.apiconnector.APIConnectorProviderImpl;
import com.qlikverify.extensions.admin.controllers.apiconnector.entity.ApiConnectorConfigEntity;
import com.qlikverify.extensions.admin.controllers.apiconnector.entity.ApiPriorityEntity;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.ProviderEntity;
import org.keycloak.models.jpa.entities.VerifierEntity;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class APIConnectorServiceImpl implements APIConnectorService {

    private static final Logger logger = Logger.getLogger(APIConnectorServiceImpl.class);
    private static final String PROVIDERS = "providers";
    private static final String VERIFIERS = "verifiers";
    private static final String COUNT = "count";
    private static final ApiConnectorHelper apiConnectorHelper = new ApiConnectorHelper();

    protected EntityManager em;
    KeycloakSession session;
    APIConnectorProviderImpl apiConnectorProvider;

    public APIConnectorServiceImpl(KeycloakSession session) {
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.session = session;
        this.apiConnectorProvider = new APIConnectorProviderImpl(session);
    }

    @Override
    public Response syncProvidersAndVerifiers(KeycloakSession session, String realmId) {
        logger.info("APIConnectorServiceImpl::syncProvidersAndVerifiers");
        String token = apiConnectorHelper.getToken();

        if (token == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        boolean syncWithApiConnector = false;
        if(session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getId().equals(realmId)){
            syncWithApiConnector = true;
        }

        try {
            if(syncWithApiConnector){
                Request.Builder requestBuilder = new Request.Builder()
                        .url(EnvironmentReader.getValue("API_CONNECTOR_HOST_URL", "http://localhost:8081") + "/api/sync")
                        .get().header("Authorization", "Bearer " + token);
                Request request = requestBuilder.build();
                OkHttpClient okHttpClient = new OkHttpClient();
                okhttp3.Response response = okHttpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    List<APIPriorityModel> result = setSyncDetails(responseBody, realmId);
                    return Response.ok(result).type(MediaType.APPLICATION_JSON).build();
                }
            } else {
                List<APIPriorityModel> result = setSyncDetails(null, realmId);
                return Response.ok(result).type(MediaType.APPLICATION_JSON).build();
            }
        } catch (IOException e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Failed to connect to the API Connector.").build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response getProviders(String realmId, Integer firstResult, Integer max, String search) {
        RealmModel realm = session.realms().getRealm(realmId);
        List<APIVerifierConfigModel> apiVerifierConfigModels = realm.getAPIVerifierConfigModels();
        if (apiVerifierConfigModels.isEmpty()) {
            return syncProvidersAndVerifiers(session, realmId);
        }

        List<String> providers = apiVerifierConfigModels.stream().map(x -> x.getProviderName()).distinct().collect(Collectors.toList());

        return Response.ok(providers).build();
    }

    @Override
    public Response getApiConfig(String realmId) {
        RealmModel realm = session.realms().getRealm(realmId);
        List<APIPriorityModel> verifierProviderDetails = realm.getVerifierProviderDetails(realm.getId(), null, null, "",null, "ASC");
        if(verifierProviderDetails.isEmpty()) {
            realm = session.realms().getRealmByName(Config.getAdminRealm());
        }
        Set<APIPriorityModel> enabledVerifiers = getEnabledVerifiers(realm);
        RealmModel finalRealm = realm;
        List<APIVerifierConfigModel> apiVerifierConfigModels = enabledVerifiers.stream()
                .map(verifier -> finalRealm.getVerifierConfigs(verifier.getVerifierName(), verifier.getSelectedProvider()))
                .collect(Collectors.toList());

        return Response.ok(apiVerifierConfigModels).build();
    }

    private Set<APIPriorityModel> getEnabledVerifiers(RealmModel realm) {
        return realm.getVerifierProviderDetails(realm.getId(), null, null, "", null,"ASC").stream()
                .filter(APIPriorityModel::getIsEnabled)
                .collect(Collectors.toSet());
    }


    @Override
    public Response getVerifiers(String realmId, Integer firstResult, Integer lastResult, Integer max, String search, String sortBy, String category,
                                 String enabled) {
        RealmModel realm = session.realms().getRealm(realmId);
        Map<String, Object> response = new HashMap<>();
        List<APIPriorityModel> verifierProviderDetails = new LinkedList<>();
        List<APIPriorityModel> verifierDetailsFromMaster = new LinkedList<>();

        RealmModel masterRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());

        verifierProviderDetails = realm.getVerifierProviderDetails(realmId, firstResult, max, search, category, sortBy);

        if (Boolean.parseBoolean(enabled)) {
            verifierProviderDetails = verifierProviderDetails.stream().filter(verifiers -> verifiers.getIsEnabled()).collect(Collectors.toList());
        }

        if (verifierProviderDetails.size() < max) {

            verifierDetailsFromMaster = masterRealm.getVerifierProviderDetails(masterRealm.getId(), firstResult, max, search, category, sortBy);
            verifierDetailsFromMaster.forEach(x -> x.setIsEnabled(false));
            if (Boolean.parseBoolean(enabled)) {
                verifierDetailsFromMaster = verifierDetailsFromMaster.stream().filter(APIPriorityModel::getIsEnabled).collect(Collectors.toList());
            }

            Set<String> existingNames = verifierProviderDetails.stream()
                    .map(APIPriorityModel::getVerifierName)
                    .collect(Collectors.toSet());

            List<APIPriorityModel> disabledVerifiersList = verifierDetailsFromMaster.stream()
                    .filter(x-> !existingNames.contains(x.getVerifierName()))
                    .collect(Collectors.toList());
            List<APIPriorityModel> finalVerifierProviderDetails = verifierProviderDetails;
            disabledVerifiersList.forEach(x-> {
                if(finalVerifierProviderDetails.size() < max){
                    finalVerifierProviderDetails.add(x);
                }
            });

        }

        if (verifierProviderDetails.isEmpty()) {
            return Response.ok(response).build();
        }

        int totalVerifiersCount = realm.getVerifierProviderDetails(masterRealm.getId(), null, null, search, category, sortBy).size();
        Integer maxResultSize = totalVerifiersCount > lastResult ? lastResult : totalVerifiersCount;

        response.put(VERIFIERS, verifierProviderDetails);
        response.put("maxResultSize", maxResultSize);
        response.put(COUNT, totalVerifiersCount);

        return Response.ok(response).build();
    }

    @Override
    public List<String> getVerifierCategories() {
        RealmModel realm = session.realms().getRealmByName(Config.getAdminRealm());
        return realm.getVerifierCategories(realm.getId());
    }

    @Override
    public List<String> getAllVerifiersName(String verifierName) {
        RealmModel realm = session.realms().getRealmByName(Config.getAdminRealm());
        return realm.getAllVerifiersName(verifierName);
    }

    @Override
    public Response updateVerifier(Map<String, String> data, String realmId) {
        RealmModel realm = session.realms().getRealm(realmId);
        RealmModel masterRealm = session.realms().getRealmByName(Config.getAdminRealm());

        try {
            Optional<ApiPriorityEntity> verifier = getApiPriorityEntities(data.get("verifierName"), realmId).stream().findFirst();
            if (verifier.isPresent() && "false".equals(data.get("isEnabled")) && !realm.getId().equalsIgnoreCase(masterRealm.getId())) {
                em.remove(verifier.get());
                em.flush();
                return Response.ok(Map.of("message", "Updated Successfully")).build();
            }else if (verifier.isPresent()) {
                ApiPriorityEntity currentApiPriorityEntity = verifier.get();
                List<String> availableProviders = JsonSerialization.readValue(currentApiPriorityEntity.getAvailableProviders(), List.class);
                if (data.containsKey("isEnabled")) {
                    currentApiPriorityEntity.setIsEnabled(Boolean.parseBoolean(data.get("isEnabled")));
                    em.merge(currentApiPriorityEntity);
                    em.flush();
                }
                if (data.containsKey("providerName") && availableProviders.contains(data.get("providerName"))) {
                    currentApiPriorityEntity.setSelectedProvider(data.get("providerName"));
                    em.merge(currentApiPriorityEntity);
                    em.flush();
                }
                List<APIPriorityModel> verifiers = realm.getVerifierProviderDetails(realmId, null, null, "",null, "");
                Map<String, Object> response = new HashMap<>();
                response.put("verifiers", verifiers);
                response.put(COUNT, verifiers.size());
                return Response.ok(Map.of("message", "Updated Successfully")).build();
            } else if (Boolean.parseBoolean(data.get("isEnabled"))) {
                APIPriorityModel masterCopy = masterRealm.getVerifierAPIDetails(data.get("verifierName"), masterRealm.getId());
                ApiPriorityEntity entity = new ApiPriorityEntity();
                entity.setRealmId(realmId);
                entity.setVerifierName(masterCopy.getVerifierName());
                entity.setId(UUID.randomUUID().toString());
                entity.setAvailableProviders(JsonSerialization.writeValueAsString(masterCopy.getAvailableProviders()));
                entity.setIsEnabled(true);
                entity.setCategory(masterCopy.getCategory());
                entity.setSelectedProvider(masterCopy.getSelectedProvider());
                em.persist(entity);
                em.flush();
                return Response.ok(Map.of("message", "Updated Successfully")).build();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }


    @Override
    public Response updateProviderPriority(String realmId, String providerId, Map<String, Integer> priority) {
        RealmModel realm = session.realms().getRealm(realmId);
        ProviderEntity selected = apiConnectorProvider.getAPIProviderByIdAndRealmId(providerId, realmId);
        int overPriority = priority.get("priority");
        int selectedPriority = selected.getProviderPriority();
        realm.getProviders(realmId, null, null, "").forEach(p -> {
            ProviderEntity current = apiConnectorProvider.getAPIProviderByIdAndRealmId(p.getId(), realmId);
            int currentPriority = current.getProviderPriority();
            if (selectedPriority < currentPriority && overPriority >= currentPriority) {
                current.setProviderPriority(currentPriority - 1);
                current.setCustomPriority(true);
                em.merge(current);
                em.flush();
            } else if (selectedPriority > currentPriority && overPriority <= currentPriority) {
                current.setProviderPriority(currentPriority + 1);
                current.setCustomPriority(true);
                em.merge(current);
                em.flush();
            }
        });
        selected.setProviderPriority(overPriority);
        selected.setCustomPriority(true);
        em.merge(selected);
        em.flush();

        List<ProviderModel> providers = realm.getProviders(realmId, null, null, "");
        Map<String, Object> response = new HashMap<>();
        response.put(PROVIDERS, providers);
        response.put(COUNT, providers.size());

        return Response.ok(response).build();
    }

    @Override
    public Response editProvider(String realmId, List<String> providerIds) {
        providerIds.forEach(x -> {
            ProviderEntity apiProviderByIdAndRealmId = apiConnectorProvider.getAPIProviderByIdAndRealmId(x, realmId);
            apiProviderByIdAndRealmId.setLive(false);
            em.merge(apiProviderByIdAndRealmId);
            em.flush();
        });

        return Response.ok(apiConnectorProvider.getProviderModelsByRealmId(realmId, null, null, "")).build();
    }

    @Override
    public Response updateVerifierProviderPriority(String realmId, String verifierId, Map<String, Integer> priority) {
        RealmModel realm = session.realms().getRealm(realmId);
        VerifierEntity selected = apiConnectorProvider.getVerifierByIdAndRealmId(realmId, verifierId);
        int overPriority = priority.get("priority");
        int selectedPriority = selected.getPriority();
        realm.getVerifiers(realmId, null, null, "", "ASC").forEach(v -> {
            VerifierEntity current = apiConnectorProvider.getVerifierByIdAndRealmId(realmId, v.getId());
            int currentPriority = current.getPriority();
            if (selectedPriority < currentPriority && overPriority >= currentPriority) {
                current.setPriority(currentPriority - 1);
                current.setCustomPriority(true);
                em.merge(current);
                em.flush();
            } else if (selectedPriority > currentPriority && overPriority <= currentPriority) {
                current.setPriority(currentPriority + 1);
                current.setCustomPriority(true);
                em.merge(current);
                em.flush();
            }
        });
        selected.setPriority(overPriority);
        selected.setCustomPriority(true);
        em.merge(selected);
        em.flush();

        List<VerifierModel> verifiers = realm.getVerifiers(realmId, null, null, "", "ASC");
        Map<String, Object> response = new HashMap<>();
        response.put(VERIFIERS, verifiers);

        return Response.ok(response).build();
    }

    @Override
    public Response editVerifier(String realmId, List<String> verifierIds) {
        verifierIds.forEach(x -> {
            VerifierEntity verifierByIdAndRealmId = apiConnectorProvider.getVerifierByIdAndRealmId(realmId, x);
            verifierByIdAndRealmId.setLive(false);
            em.merge(verifierByIdAndRealmId);
        });
        em.flush();
        return Response.ok(apiConnectorProvider.getVerifierModelsByRealmId(realmId, null, null, "", "ASC")).build();
    }


    private List<APIPriorityModel> setSyncDetails(String response, String realmId) {
        Map<String, Object> apiConnectorSyncModels = null;
        try {
            if(response == null) {
                apiConnectorSyncModels = new HashMap<>();
            } else {
                apiConnectorSyncModels = JsonSerialization.readValue(response, new TypeReference<Map>() {});
            }
        } catch (IOException e) {
            logger.error("Failed to parse the response from the API Connector.");
        }
        return syncChangedDetailsToDb(apiConnectorSyncModels, realmId, em);
    }

    private List<APIPriorityModel> syncChangedDetailsToDb(Map<String, Object> apiConnectorSyncModel, String realmId, EntityManager em) {
        logger.info("Sync started");
        if (apiConnectorSyncModel == null) {
            logger.info("no data available in api connector");
            return new ArrayList<>();
        }
        try {
            return syncDetails(apiConnectorSyncModel, realmId, em);
        } catch (Exception e) {
            logger.info(e.getMessage());
            logger.info(e.getStackTrace());
            throw new RuntimeException();
        }
    }

    private List<APIPriorityModel> syncDetails(Map<String, Object> apiConnectorConfigModel, String realmId, EntityManager em) {
        logger.info("Syncing details from api connector");
        String masterRealmId = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm()).getId();
        RealmModel realm = session.realms().getRealm(realmId);
        boolean syncWithApiConnector = false;
        if(realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())){
            syncWithApiConnector = true;
        }
        if(syncWithApiConnector) {
            apiConnectorProvider.deleteApiConnectorConfigEntity();
            apiConnectorConfigModel.keySet().forEach(key -> {
                List<Map<String, Object>> providerMapping = JsonSerialization.convertValue(apiConnectorConfigModel.get(key), new TypeReference<>() {
                });
                providerMapping.forEach(verifier -> {
                    String verifierName = verifier.get("verifierName").toString();
                    ApiConnectorConfigEntity entity = new ApiConnectorConfigEntity();
                    entity.setId(UUID.randomUUID().toString());
                    entity.setVerifierName(verifierName);
                    entity.setProviderName(key);
                    entity.setRequestBody(verifier.get("requestBody").toString());
                    entity.setResponseBody(verifier.get("responseBody").toString());
                    entity.setCategory(verifier.get("category") != null ? verifier.get("category").toString() : "default");
                    try {
                        entity.setClaims(JsonSerialization.writeValueAsString(verifier.get("claims")));
                    } catch (IOException e) {
                        logger.error(String.format("APIConnectorServiceImpl :: syncDetails :: Error while parsing the claims :: %s ", verifier.get("claims")));
                    }
                    em.persist(entity);
                    updateValuesToApiPriorityTable(key, verifierName, realmId, verifier.get("category") != null ? verifier.get("category").toString() : "default");
                });
            });
        } else {
            List<ApiPriorityEntity> apis = apiConnectorProvider.getAPIPriorityByRealmId(realmId, 0, Integer.MAX_VALUE, null, null, "ASC");
            // copy values from master to subtenant
            List<ApiPriorityEntity> masterApiDetails = apiConnectorProvider.getAPIPriorityByRealmId(masterRealmId, 0, Integer.MAX_VALUE, null, null, "ASC");
            masterApiDetails.forEach(x->{
                if(!updateEntityIfExists(apis, x)) {
                    ApiPriorityEntity entity = new ApiPriorityEntity();
                    entity.setAvailableProviders(x.getAvailableProviders());
                    entity.setIsEnabled(x.getIsEnabled());
                    entity.setRealmId(realmId);
                    entity.setId(UUID.randomUUID().toString());
                    entity.setSelectedProvider(x.getSelectedProvider());
                    entity.setVerifierName(x.getVerifierName());
                    entity.setCategory(x.getCategory());
                    entity.setCategory(x.getCategory() == null ? "default" : x.getCategory());
                    em.persist(entity);
                }
            });
        }
        em.flush();
        return apiConnectorProvider.getApiConnectorConfigModel(realmId);
    }

    private boolean updateEntityIfExists(List<ApiPriorityEntity> apis, ApiPriorityEntity masterEntity){
        if(apis.stream().anyMatch(x->x.getVerifierName().equalsIgnoreCase(masterEntity.getVerifierName()))){
            Optional<ApiPriorityEntity> optionalEntity =
                    apis.stream().filter(x->x.getVerifierName().equalsIgnoreCase(masterEntity.getVerifierName())).findFirst();
            if(optionalEntity.isEmpty()){
                throw new RuntimeException();
            }
            ApiPriorityEntity entity = optionalEntity.get();
            entity.setAvailableProviders(masterEntity.getAvailableProviders());
            em.persist(entity);
            return true;
        }
        return false;
    }

    private void updateValuesToApiPriorityTable(String key, String verifierName, String realmId, String category) {
        List<ApiPriorityEntity> apiPriorityList = getApiPriorityEntities(verifierName, realmId);
        ApiPriorityEntity apiPriorityEntity;
        if (apiPriorityList == null || apiPriorityList.isEmpty()) {
            apiPriorityEntity = new ApiPriorityEntity();
            apiPriorityEntity.setId(UUID.randomUUID().toString());
            apiPriorityEntity.setRealmId(realmId);
            apiPriorityEntity.setVerifierName(verifierName);
            apiPriorityEntity.setSelectedProvider(key);
            apiPriorityEntity.setIsEnabled(false);
            apiPriorityEntity.setCategory(category);
            try {
                apiPriorityEntity.setAvailableProviders(JsonSerialization.writeValueAsString(Arrays.asList(key)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            em.persist(apiPriorityEntity);
        } else {
            apiPriorityEntity = apiPriorityList.get(0);
            apiPriorityEntity.setId(apiPriorityEntity.getId());
            apiPriorityEntity.setRealmId(apiPriorityEntity.getRealmId());
            apiPriorityEntity.setVerifierName(apiPriorityEntity.getVerifierName());
            apiPriorityEntity.setSelectedProvider(apiPriorityEntity.getSelectedProvider());
            apiPriorityEntity.setIsEnabled(apiPriorityEntity.getIsEnabled());
            apiPriorityEntity.setCategory(category);
            try {
                Set<String> availableProviders = JsonSerialization.readValue(apiPriorityEntity.getAvailableProviders(), Set.class);
                availableProviders.add(key);
                apiPriorityEntity.setAvailableProviders(JsonSerialization.writeValueAsString(availableProviders));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            em.persist(apiPriorityEntity);
        }
    }

    public List<ApiPriorityEntity> getApiPriorityEntities(String verifierName, String realmId) {
        TypedQuery<ApiPriorityEntity> query = em.createNamedQuery("getByVerifierNameRealmId", ApiPriorityEntity.class);
        query.setParameter("verifierName", verifierName);
        query.setParameter("realmId", realmId);
        return query.getResultList();
    }

    public Response getAPILogs(String realmId, String filterByTimestamp,String filterByVerifier, String requestId, boolean isPricingLogs, List<String> eventTypes, int page, int size) {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(EnvironmentReader.getValue("API_CONNECTOR_HOST_URL", "http://localhost:8081") + "/api/audit-logs/get-verification-logs").newBuilder();

        urlBuilder.addQueryParameter("workspaceId", realmId)
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("size", String.valueOf(size))
                .addQueryParameter("isPricingLogs", String.valueOf(isPricingLogs))
                .addQueryParameter("filterByTimestamp", filterByTimestamp);
        if (eventTypes != null && !eventTypes.isEmpty()) {
            eventTypes.forEach(eventType -> urlBuilder.addQueryParameter("eventTypes", eventType));
        }
        if (requestId != null) {
            urlBuilder.addQueryParameter("requestId", requestId);
        }
        if(filterByVerifier != null) {
            urlBuilder.addQueryParameter("filterByVerifierName", filterByVerifier);
        }

        try {
            Request request = new Request.Builder().url(urlBuilder.build())
                    .get()
                    .build();

            OkHttpClient okHttpClient = new OkHttpClient();
            okhttp3.Response response = okHttpClient.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                logger.info(responseBody);
                return Response.ok(responseBody).type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            logger.error("Error while fetching the logs :: " + e.getMessage());
        }
        return Response.status(400).build();

    }
}

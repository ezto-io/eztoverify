package com.qlikverify.extensions.admin.controllers.apiconnector;

import com.qlikverify.apiConnector.spi.APIConnectorProvider;
import com.qlikverify.extensions.admin.controllers.apiconnector.entity.ApiConnectorConfigEntity;
import com.qlikverify.extensions.admin.controllers.apiconnector.entity.ApiPriorityEntity;
import com.qlikverify.extensions.admin.controllers.apiconnector.model.ApiPriorityModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.ProviderEntity;
import org.keycloak.models.jpa.entities.VerifierEntity;
import org.keycloak.util.JsonSerialization;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class APIConnectorProviderImpl implements APIConnectorProvider {

    KeycloakSession session;

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    public APIConnectorProviderImpl(KeycloakSession session) {
        this.session = session;
    }
    public static final String REALM_ID = "realmId";

    @Override
    public List<ProviderModel> getProviderDetails(String realmId, Integer firstResult, Integer max, String search) {
        return getProviderModelsByRealmId(realmId, firstResult, max, search);
    }

    @Override
    public List<VerifierModel> getVerifierDetails(String realmId, Integer firstResult, Integer max, String search, String sortBy) {
        return getVerifierModelsByRealmId(realmId, firstResult, max, search, sortBy);
    }

    @Override
    public List<APIPriorityModel> getVerifierProviderDetails(String realmId, Integer firstResult, Integer max, String search, String category, String sortBy) {
        return getAPIPriorityModelsByRealmId(realmId, firstResult, max, search, category, sortBy);
    }

    @Override
    public List<String> getVerifierCategories(String realmId) {
        TypedQuery<String> query = getEntityManager().createQuery("SELECT DISTINCT u.category FROM ApiPriorityEntity u WHERE u.realmId = :realmId ", String.class);
        query.setParameter("realmId", realmId);
        return query.getResultList();
    }

    @Override
    public List<String> getAllVerifiersName(String verifierName) {
        TypedQuery<String> query  = getEntityManager().createQuery("SELECT DISTINCT u.verifierName FROM ApiPriorityEntity u WHERE (:verifierName IS NULL OR LOWER(u.verifierName) LIKE LOWER (CONCAT('%', :verifierName, '%'))) order by u.verifierName ASC", String.class);
        query.setParameter("verifierName", verifierName == null ? "" : verifierName);
        return query.getResultList();
    }

    @Override
    public APIPriorityModel getVerifierAPIDetails(String verifierName, String realmId) {
        return getAPIPriorityModelByVerifierAndRealmId(verifierName, realmId);
    }

    @Override
    public APIVerifierConfigModel getVerifierConfigs(String verifierName, String providerName) {
        return getVerifierConfigByVerifierNameAndProvider(verifierName, providerName);
    }

    @Override
    public List<APIVerifierConfigModel> getAPIVerifierConfigsModels() {
        return getAPIVerifierConfigs();
    }

    @Override
    public List<String> getClaims(String verification, String provider) {
        return getClaimByVerification(verification, provider);
    }

    @Override
    public void close() {
        //
    }

    public List<ProviderModel> getProviderModelsByRealmId(String realmId, Integer firstResult, Integer max, String search) {
        return getAPIProvidersByRealmId(realmId, firstResult, max, search).stream().map(this::providerEntityToModel).collect(Collectors.toList());
    }

    @Override
    public List<APIPriorityModel> getApiConnectorConfigModel(String realmId){
        TypedQuery<ApiPriorityEntity> query = getEntityManager().createNamedQuery("getConfigByRealmIdAsc", ApiPriorityEntity.class);
        query.setParameter("realmId",realmId);
        query.setParameter("search","");
        query.setParameter("category","");
        List<ApiPriorityEntity> apiConfigList = query.getResultList();
        return apiConnectorConfigEntityToModel(apiConfigList);
    }


    public void deleteApiConnectorConfigEntity(){

        Query query = getEntityManager().createNamedQuery("deleteAllConfig");
        query.executeUpdate();
    }

    public void removeProviderModelByRealmIdProviderId(String realmId, String providerId){
        removeProviderByRealmIdProviderId(realmId, providerId);
    }

    public List<VerifierModel> getVerifierModelsByRealmId(String realmId, Integer firstResult, Integer max, String search, String sortBy) {
        return getVerifiersByRealmId(realmId, firstResult, max, search, sortBy).stream().map(this::verifierEntityToModel).collect(Collectors.toList());
    }

    public List<APIPriorityModel> getAPIPriorityModelsByRealmId(String realmId, Integer firstResult, Integer max, String search, String category, String sortBy) {
        return getAPIPriorityByRealmId(realmId, firstResult, max, search, category, sortBy).stream().map(this::verifierEntityToModel).collect(Collectors.toList());
    }

    public APIPriorityModel getAPIPriorityModelByVerifierAndRealmId(String verifierName, String realmId) {
        return verifierEntityToModel(getAPIPriorityByVerifierAndRealmId(verifierName, realmId));
    }

    public APIVerifierConfigModel getVerifierConfigByVerifierNameAndProvider(String verifierName, String realmId) {
        return apiConnectorConfigEntityToModel(getAPIVerifierConfigByVerifierAndProvider(verifierName, realmId));
    }

    public List<APIVerifierConfigModel> getAPIVerifierConfigs() {
        return verifierConfigEntityToModel(getAPIVerifierConfigEntities());
    }

    public List<String> getClaimByVerification(String verification, String provider) {
        Query query = getEntityManager().createNamedQuery("getConfigByProviderNameVerifierName");
        query.setParameter("verifierName", verification);
        query.setParameter("providerName", provider);
        List<ApiConnectorConfigEntity> apiConfig = query.getResultList();
        List<String> claims = new ArrayList<>();
        if(apiConfig.isEmpty())
            return new ArrayList<>();
        parseTemplateToMap(apiConfig.get(0).getResponseBody()).values().forEach(x->{
            if(x.toString().startsWith("${")){
                claims.add(x.toString().substring(2,x.toString().length()-1));
            }
        });
//        return apiProvider.stream().map(x->x.getRequestBody());
        return claims;
    }

    public ProviderEntity getAPIProviderByIdAndRealmId(String providerId, String realmId) {
        TypedQuery<ProviderEntity> query = getEntityManager().createNamedQuery("getProviderByIdAndRealmId", ProviderEntity.class);
        query.setParameter("id", providerId);
        query.setParameter(REALM_ID, realmId);
        Optional<ProviderEntity> apiProvider = query.getResultList().stream().findFirst();
        return apiProvider.orElse(null);
    }

    public ProviderEntity getAPIProviderByNameAndRealmId(String providerName, String realmId) {
        TypedQuery<ProviderEntity> query = getEntityManager().createNamedQuery("getProviderByNameAndRealmId", ProviderEntity.class);
        query.setParameter("providerName", providerName);
        query.setParameter(REALM_ID, realmId);
        Optional<ProviderEntity> apiProvider = query.getResultList().stream().findFirst();
        return apiProvider.orElse(null);
    }

    public void removeProviderByRealmIdProviderId(String realmId, String providerId){
        TypedQuery<ProviderEntity> query = getEntityManager().createNamedQuery("getProviderByProviderIdAndRealmId", ProviderEntity.class);
        query.setParameter("providerId", providerId);
        query.setParameter(REALM_ID, realmId);
        Optional<ProviderEntity> apiProvider = query.getResultList().stream().findFirst();
        apiProvider.ifPresent(provider -> getEntityManager().remove(provider));
    }

    public List<ProviderEntity> getAPIProvidersByRealmId(String realmId, Integer firstResult, Integer max, String search) {
        TypedQuery<ProviderEntity> query = getEntityManager().createNamedQuery("getAllProvidersByRealmId", ProviderEntity.class);
        query.setParameter(REALM_ID, realmId);
        query.setParameter("search", "%" + search + "%");

        if(firstResult != null && max != null) {
            query.setFirstResult(firstResult);
            query.setMaxResults(max);
        }

        List<ProviderEntity> apiProviders = query.getResultList();

        if (apiProviders.isEmpty()) {
            return Collections.emptyList();
        }
        return apiProviders;
    }

    public List<VerifierEntity> getVerifiersByRealmId(String realmId, Integer firstResult, Integer max, String search, String sortBy) {
        TypedQuery<VerifierEntity> query = getEntityManager().createNamedQuery("getVerifiersByRealmId", VerifierEntity.class);
        query.setParameter(REALM_ID, realmId);
        query.setParameter("search", "%" + search + "%");
        query.setParameter("sortBy", sortBy);

        if(firstResult != null && max != null) {
            query.setFirstResult(firstResult);
            query.setMaxResults(max);
        }

        List<VerifierEntity> verifiers = new ArrayList<>(query.getResultList());
        if (verifiers.isEmpty()) {
            return Collections.emptyList();
        }
        return verifiers;
    }

    public List<ApiPriorityEntity> getAPIPriorityByRealmId(String realmId, Integer firstResult, Integer max, String search, String category, String sortBy) {
        TypedQuery<ApiPriorityEntity> query = getEntityManager().createNamedQuery(sortBy.equals("ASC") ? "getConfigByRealmIdAsc" : "getConfigByRealmIdDesc", ApiPriorityEntity.class);
        query.setParameter(REALM_ID, realmId);

        query.setParameter("search", search == null ? "" : search);
        query.setParameter("category", category == null ? "" : category);

        if(firstResult != null && max != null) {
            query.setFirstResult(firstResult);
            query.setMaxResults(max);
        }

        List<ApiPriorityEntity> verifiers = new ArrayList<>(query.getResultList());
        if (verifiers.isEmpty()) {
            return Collections.emptyList();
        }
        return verifiers;
    }

    public ApiPriorityEntity getAPIPriorityByVerifierAndRealmId(String verifierName, String realmId) {
        TypedQuery<ApiPriorityEntity> query = getEntityManager().createNamedQuery("getByVerifierNameRealmId", ApiPriorityEntity.class);
        query.setParameter(REALM_ID, realmId);
        query.setParameter("verifierName", verifierName);
        Optional<ApiPriorityEntity> verifiers = query.getResultList().stream().findFirst();
        return verifiers.orElse(null);
    }

    public ApiConnectorConfigEntity getAPIVerifierConfigByVerifierAndProvider(String verifierName, String providerName) {
        TypedQuery<ApiConnectorConfigEntity> query = getEntityManager().createNamedQuery("getConfigByProviderNameVerifierName", ApiConnectorConfigEntity.class);
        query.setParameter("providerName", providerName);
        query.setParameter("verifierName", verifierName);
        Optional<ApiConnectorConfigEntity> verifiers = query.getResultList().stream().findFirst();
        return verifiers.orElse(null);
    }

    public List<ApiConnectorConfigEntity> getAPIVerifierConfigEntities() {
        TypedQuery<ApiConnectorConfigEntity> query = getEntityManager().createNamedQuery("getAllConfig", ApiConnectorConfigEntity.class);
        List<ApiConnectorConfigEntity> verifiers = new ArrayList<>(query.getResultList());
        if (verifiers.isEmpty()) {
            return Collections.emptyList();
        }

        return verifiers;
    }

    public VerifierEntity getVerifierByIdAndRealmId(String realmId, String verifierId) {
        TypedQuery<VerifierEntity> query = getEntityManager().createNamedQuery("getVerifierByIdAndRealmId", VerifierEntity.class);
        query.setParameter(REALM_ID, realmId);
        query.setParameter("id", verifierId);
        Optional<VerifierEntity> verifiers = query.getResultList().stream().findFirst();
        return verifiers.orElse(null);
    }

    public ProviderModel providerEntityToModel(ProviderEntity providerEntity) {

        return ProviderModel.builder()
                    .id(providerEntity.getId())
                    .providerId(providerEntity.getProviderId())
                    .providerName(providerEntity.getProviderName())
                    .providerPriority(providerEntity.getProviderPriority())
                    .live(providerEntity.isLive())
                    .customPriority(providerEntity.isCustomPriority())
                    .realmId(providerEntity.getRealmId())
                    .build();
    }

    public VerifierModel verifierEntityToModel(VerifierEntity verifierEntity) {
        return VerifierModel.builder()
                    .id(verifierEntity.getId())
                    .verifierName(verifierEntity.getVerifierName())
                    .priority(verifierEntity.getPriority())
                    .live(verifierEntity.isLive())
                    .customPriority(verifierEntity.isCustomPriority())
                    .realmId(verifierEntity.getRealmId())
                    .apiProvider(providerEntityToModel(verifierEntity.getProvider()))
                    .build();
    }

    public APIPriorityModel verifierEntityToModel(ApiPriorityEntity apiPriorityEntity) {
        if (apiPriorityEntity == null)
            return null;
        try {
            return APIPriorityModel.builder()
                    .id(apiPriorityEntity.getId())
                    .verifierName(apiPriorityEntity.getVerifierName())
                    .availableProviders(JsonSerialization.readValue(apiPriorityEntity.getAvailableProviders(), List.class))
                    .selectedProvider(apiPriorityEntity.getSelectedProvider())
                    .realmId(apiPriorityEntity.getRealmId())
                    .isEnabled(apiPriorityEntity.getIsEnabled())
                    .category(apiPriorityEntity.getCategory())
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public APIVerifierConfigModel apiConnectorConfigEntityToModel(ApiConnectorConfigEntity apiConnectorConfigEntity) {
        if (apiConnectorConfigEntity == null)
            return null;
        try {
            return APIVerifierConfigModel.builder()
                    .id(apiConnectorConfigEntity.getId())
                    .providerName(apiConnectorConfigEntity.getProviderName())
                    .verifierName(apiConnectorConfigEntity.getVerifierName())
                    .responseBody(apiConnectorConfigEntity.getResponseBody())
                    .requestBody(apiConnectorConfigEntity.getRequestBody())
                    .category(apiConnectorConfigEntity.getCategory())
                    .claims(JsonSerialization.readValue(apiConnectorConfigEntity.getClaims(), Map.class))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<APIVerifierConfigModel> verifierConfigEntityToModel(List<ApiConnectorConfigEntity> entities){
        List<APIVerifierConfigModel> models = new ArrayList<>();
        entities.forEach(entity -> {
            APIVerifierConfigModel model = new APIVerifierConfigModel();
            model.setId(entity.getId());
            model.setProviderName(entity.getProviderName());
            model.setVerifierName(entity.getVerifierName());
            model.setResponseBody(entity.getResponseBody());
            model.setRequestBody(entity.getRequestBody());
            model.setCategory(entity.getCategory());
            try {
                model.setClaims(JsonSerialization.readValue(entity.getClaims(), Map.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            models.add(model);
        });
        return models;
    }

    public List<APIPriorityModel> apiConnectorConfigEntityToModel(List<ApiPriorityEntity> entities){
        List<APIPriorityModel> models = new ArrayList<>();
        entities.forEach(entity -> {
            APIPriorityModel model = new APIPriorityModel();
            model.setId(entity.getId());
            model.setVerifierName(entity.getVerifierName());
            model.setCategory(entity.getCategory());
            try {
                model.setAvailableProviders(JsonSerialization.readValue(entity.getAvailableProviders(), List.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            model.setSelectedProvider(entity.getSelectedProvider());
            model.setRealmId(entity.getRealmId());
            model.setIsEnabled(entity.getIsEnabled());
            models.add(model);
        });
        return models;
    }

    public Map<String, Object> parseTemplateToMap(String input) {
        Map<String, Object> map = new HashMap<>();

        // Remove outer braces
        input = input.substring(1, input.length() - 1);

        // Split by commas that separate key-value pairs
        String[] pairs = input.split(",\\s*");

        for (String pair : pairs) {
            if(StringUtils.hasText(pair)) {
                String[] keyValue = pair.split("=");
                String key = keyValue[0];
                String value = keyValue.length < 2 ? "" : keyValue[1];
                if (value.contains("[]")) {
                    map.put(key, new ArrayList<>());
                } else {
                    map.put(key, value);
                }
            }
        }

        return map;
    }
}

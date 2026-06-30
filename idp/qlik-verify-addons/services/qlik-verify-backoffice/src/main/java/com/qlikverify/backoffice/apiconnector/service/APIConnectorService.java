package com.qlikverify.backoffice.apiconnector.service;

import org.keycloak.models.KeycloakSession;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public interface APIConnectorService {

    Response syncProvidersAndVerifiers(KeycloakSession session, String realmId);
    Response getVerifiers(String realmId, Integer firstResult, Integer lastResult, Integer max, String search, String sortBy, String category, String enabled);
    List<String> getVerifierCategories();
    List<String> getAllVerifiersName(String verifierName);
    Response updateVerifier(Map<String, String> data, String realmId);

    Response getProviders(String realmId, Integer firstResult, Integer max, String search);
    Response getApiConfig(String realmId);


    Response updateProviderPriority(String realmId, String providerId, Map<String, Integer> priority);

    Response editProvider(String realmId, List<String> providerIds);

    Response updateVerifierProviderPriority(String realmId, String verifierId, Map<String, Integer> priority);

    Response editVerifier(String realmId, List<String> verifierIds);
}

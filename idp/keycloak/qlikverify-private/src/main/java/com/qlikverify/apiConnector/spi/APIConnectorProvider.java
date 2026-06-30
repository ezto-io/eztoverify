package com.qlikverify.apiConnector.spi;

import org.keycloak.models.*;
import org.keycloak.provider.Provider;

import java.util.List;

public interface APIConnectorProvider extends Provider {

    List<ProviderModel> getProviderDetails(String realmId, Integer firstResult, Integer max, String search);

    List<VerifierModel> getVerifierDetails(String realmId, Integer firstResult, Integer max, String search, String sortBy);

    List<APIPriorityModel> getVerifierProviderDetails(String realmId, Integer firstResult, Integer max, String search, String category, String sortBy);
    List<String> getVerifierCategories(String realmId);
    List<String> getAllVerifiersName(String verifierName);
    APIPriorityModel getVerifierAPIDetails(String verifierName, String realmId);
    APIVerifierConfigModel getVerifierConfigs(String verifierName, String providerName);
    List<APIVerifierConfigModel> getAPIVerifierConfigsModels();

    List<String> getClaims(String verification, String provider);
    List<APIPriorityModel> getApiConnectorConfigModel(String realmId);

}

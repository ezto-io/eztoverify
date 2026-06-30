package org.keycloak.marketplace.template;

import org.keycloak.marketplace.InstallResponse;
import org.keycloak.marketplace.MarketplaceException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.List;
import java.util.Map;

public interface MarketplaceTemplateProvider extends Provider {

    /**
     * This method will install the authentication flows of the template
     * @param config
     * @return
     */
    InstallResponse install(KeycloakSession session, Map<String,String> config) throws MarketplaceException;

    InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String,String> config) throws MarketplaceException;

    default InstallResponse onCleanUp(RealmModel realmModel, ClientModel clientModel) {
        return null;
    }

    default InstallResponse onTrialSetup(KeycloakSession session, RealmModel realmModel){
        return null;
    }
}

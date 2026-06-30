package org.keycloak.marketplace.template;

import org.keycloak.marketplace.InstallResponse;
import org.keycloak.marketplace.MarketplaceException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;

public interface MarketplaceSSOTemplateProvider extends MarketplaceTemplateProvider {

    List<SSOBean> getSSOSubApps(ClientModel clientModel, String baseRedirectUrl);
}

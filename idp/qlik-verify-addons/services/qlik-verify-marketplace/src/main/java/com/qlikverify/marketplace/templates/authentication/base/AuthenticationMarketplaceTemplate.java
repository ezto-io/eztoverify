package com.qlikverify.marketplace.templates.authentication.base;

import com.qlikverify.marketplace.GeneralHelper;
import org.keycloak.marketplace.MarketplaceProvider;
import org.keycloak.marketplace.MarketplaceProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AuthenticationMarketplaceTemplate {

    public abstract String dependentLoginTemplateId();

    public abstract String dependentRecoveryTemplateId();

    public abstract String dependentOnboardingTemplateId();

    public Map<String, Object> getMarketplace(KeycloakSession session) {
        Map<String, Object> data = new HashMap<>();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactoriesStream(MarketplaceProvider.class).collect(Collectors.toList());
        if (dependentOnboardingTemplateId() != null) {
            Optional<ProviderFactory> onboarding = factories.stream().filter(x -> x.getId().equals(dependentOnboardingTemplateId())).findFirst();
            if (onboarding.isPresent()) {
                data.put("onboarding", buildMarketPlaceProviderMetadata(onboarding.get()));
            }

        }

        //adding Login Flows if present
        if (dependentLoginTemplateId() != null) {
            Optional<ProviderFactory> login = factories.stream().filter(x -> x.getId().equals(dependentLoginTemplateId())).findFirst();
            if (login.isPresent()) {
                data.put("login", buildMarketPlaceProviderMetadata(login.get()));
            }
        }

        //adding Recovery Flows if present
        if (dependentRecoveryTemplateId() != null) {
            Optional<ProviderFactory> recovery = factories.stream().filter(x -> x.getId().equals(dependentRecoveryTemplateId())).findFirst();
            if (recovery.isPresent()) {
                data.put("recovery", buildMarketPlaceProviderMetadata(recovery.get()));
            }
        }

        return data;
    }

    private Map<String, Object> buildMarketPlaceProviderMetadata(ProviderFactory factory) {
        MarketplaceProviderFactory mpFactory = (MarketplaceProviderFactory) factory;
        Map<String, Object> data = new HashMap<>();
        data.put("id", factory.getId());
        data.put("templateName", mpFactory.getTemplateName());
        data.put("templateDescription", mpFactory.getTemplateDescription());
        data.put("templateType", mpFactory.getTemplateType());
        data.put("howItWorks", mpFactory.getHowItWorks());
        data.put("templateFlow", mpFactory.getFlow());
        data.put("isNew", mpFactory.isNew());
        return data;
    }

    protected List<String> validateAlias(RealmModel realmModel, Map<String, String> rep) {
        List<String> configTestResponses = new ArrayList<>();
        String alias = GeneralHelper.getConfigValue(rep, "alias", "");
        if (alias == null || alias.isEmpty()) {
            configTestResponses.add("Alias is a required field");
            return configTestResponses;
        }
        // Existing Alias  Check
        if (realmModel.getFlowByAlias(String.valueOf(alias) + "_onboarding") != null ||
                realmModel.getFlowByAlias(String.valueOf(alias) + "_login") != null ||
                realmModel.getFlowByAlias(String.valueOf(alias) + "_recovery") != null) {
            configTestResponses.add("Template with Alias " + alias + " is already exists. Try with different alias");
        }

        return configTestResponses;
    }
}

package com.qlikverify.marketplace;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.marketplace.template.MarketplaceTemplateProviderFactory;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;

import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BasePartnerTemplate {
    protected void removePartner(KeycloakSession session,MarketplaceTemplateProviderFactory factory, RealmModel realm) {
        Map<String, Object> marketProviders = factory.getMarketProviders(session);
        ObjectMapper mapper = new ObjectMapper();

        // To check whether any onboarding and login flows are in use
        List<AuthenticationFlowModel> authenticationFlows = session.getContext().getRealm().getAuthenticationFlowsStream().collect(Collectors.toList());
        for (AuthenticationFlowModel authenticationFlow : authenticationFlows) {
            if (authenticationFlow.getTemplateAlias() != null && authenticationFlow.getTemplateAlias().toLowerCase().contains(factory.getTitle(session).toLowerCase()) && authenticationFlow.getId() != null) {
                if (ModelToRepresentation.checkIfFlowIsLive(session.getContext().getRealm(), authenticationFlow.getId())) {
                    throw new BadRequestException("cant-delete-live-flow");
                }
            }
        }

        if(marketProviders != null || !marketProviders.isEmpty())
        {
            if(marketProviders.containsKey("onboarding")){
                String onboardingTemplateId = (String) mapper.convertValue(marketProviders.get("onboarding"), Map.class).get(
                        "id").toString();
                if( !onboardingTemplateId.isEmpty()){
                    List<AuthenticationFlowModel> onboardingExecutionModel =
                            realm.getAuthenticationFlowsStream().filter(x -> x.getTemplateId() != null &&
                                    x.getTemplateId().equals(onboardingTemplateId)).collect(Collectors.toList());
                    for (AuthenticationFlowModel executionModel : onboardingExecutionModel) {
                        realm.removeAuthenticationFlow(executionModel, false);
                    }
                }
            }

            if(mapper.convertValue(marketProviders.get("login"), Map.class).get("id") != null){
                String loginTemplateId = (String) mapper.convertValue(marketProviders.get("login"), Map.class).get("id").toString();
                if( !loginTemplateId.isEmpty()) {
                    List<AuthenticationFlowModel> loginExecutionModel =
                            realm.getAuthenticationFlowsStream().filter(x -> x.getTemplateId() != null &&
                                    x.getTemplateId().equals(loginTemplateId)).collect(Collectors.toList());
                    for (AuthenticationFlowModel executionModel : loginExecutionModel) {
                        realm.removeAuthenticationFlow(executionModel, false);
                    }
                }
            }

            // It will remove redirect flow that was installed for the partner
            if(mapper.convertValue(marketProviders.get("redirect"),Map.class) != null) {
                String redirectTemplateId = (String) mapper.convertValue(marketProviders.get("redirect"), Map.class).get("id");
                List<AuthenticationFlowModel> redirectExecutionModel =
                        realm.getAuthenticationFlowsStream().filter(x -> x.getTemplateId() != null &&
                                x.getTemplateId().equals(redirectTemplateId)).collect(Collectors.toList());
                for (AuthenticationFlowModel executionModel : redirectExecutionModel) {
                    realm.removeAuthenticationFlow(executionModel, false);
                }
            }
        }


        // Remove MFA
        String partner = factory.getTitle(session).toLowerCase();
        Set<String> realmAttributes = realm.getAttributes().keySet();

        //to check whether MFA is used in any client
        List<ClientModel> configuredMfas = session.getContext().getRealm().getClientsStream().collect(Collectors.toList());
        for (ClientModel configuredMfa : configuredMfas) {
            if (configuredMfa.getMFAConnections().get(partner + "-mfa") != null && configuredMfa.getMFAConnections().get(partner + "-mfa") == true) {
                throw new BadRequestException("We can't upgrade or subscribe the flow since it's used in our applications. Please remove and try again");
            }
        }

        for (String attribute : realmAttributes) {
            // As attribute for expiry time in every providers are different added for a validation for expiry
            if (attribute.toLowerCase().contains(partner)) {
                realm.removeAttribute(attribute);
            }
        }
        Optional<MFAConfigModel> partnerMfa =
                realm.getMFAConfigsStream().filter(x -> ((MFAConfigModel) x).getMfaProviderId().contains(partner)).findFirst();
        if (partnerMfa.isPresent()) {
            realm.removeMFAConfig(partnerMfa.get());
        }

        realm.setMFAProviders(realm.getMFAProvidersStream().filter(x -> !x.contains(partner)).collect(Collectors.toSet()));
        if (partnerMfa.isPresent()) {
            String mfaConnId = partnerMfa.get().getMfaProviderId();
            List<ClientModel> clients = realm.getClientsStream().collect(Collectors.toList());
            for (ClientModel cli : clients) {
                cli.removeMFAConnections(mfaConnId);
            }
        }

        // Remove event listener
        List<String> eventListeners = realm.getEventsListenersStream().collect(Collectors.toList());
        realm.setEventsListeners(eventListeners.stream().filter(x -> !x.contains(partner)).collect(Collectors.toSet()));

        // Remove marketplace config
        if(marketProviders != null){
            List<MarketplaceConfigModel> marketConfigs =
                    realm.getMarketplaceConfigsStream().filter(x -> x.getProviderId().contains(partner)).collect(Collectors.toList());
            for (MarketplaceConfigModel marketConfig : marketConfigs) {
                realm.removeMarketplaceConfig(marketConfig);
            }
        }

    }

}

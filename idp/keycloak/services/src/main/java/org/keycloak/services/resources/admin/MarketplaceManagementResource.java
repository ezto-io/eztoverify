
package org.keycloak.services.resources.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.marketplace.InstallResponse;
import org.keycloak.marketplace.MarketplaceException;
import org.keycloak.marketplace.ThirdPartyProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProviderFactory;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.marketplace.MarketplaceTemplateRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.theme.Theme;
import org.keycloak.utils.GeneralHelper;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.Constants.IWS_MARKETPLACE;

public class MarketplaceManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(MarketplaceManagementResource.class);

    public MarketplaceManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTH_FLOW);
    }

    /**
     * Get Categories of Marketplace Templates
     * <p>
     * Returns a list of Categories presented in marketplace templates.
     */
    @Path("/categories")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getCategories() {
        auth.realm().requireViewRealm();
        List<String> marketplaceCategories = new ArrayList<>(Constants.marketplaceCategories);
        marketplaceCategories.removeAll(session.rootBrand().whitelistedMarketplaceCategories(realm));
        //Traversing each category with getTemplates() and if any category doesn't have any values then remove it.
        marketplaceCategories.removeIf(category -> getTemplates(category).size() == 0);
        return marketplaceCategories;
    }

    /**
     * Get marketplace templates of specific category
     * <p>
     * Returns a list of marketplace templates of given category.
     */
    @Path("/templates/{category}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getTemplates(@PathParam("category") String category) {
        auth.realm().requireViewRealm();

        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(MarketplaceTemplateProvider.class);

        List<ProviderFactory> providerFactory;

        if (category.equalsIgnoreCase("recommended")) {
            providerFactory = factories.stream().filter(x-> !session.rootBrand().whitelistedMarketplaceCategories(realm).contains(((MarketplaceTemplateProviderFactory) x).getCategory())).filter(x -> ((MarketplaceTemplateProviderFactory) x).isRecommended() && !session.rootBrand().whitelistedMarketplace(realm).contains(((MarketplaceTemplateProviderFactory) x).getId())).collect(Collectors.toList());
        } else {
            providerFactory = factories.stream().filter(x-> !session.rootBrand().whitelistedMarketplaceCategories(realm).contains(((MarketplaceTemplateProviderFactory) x).getCategory())).filter(x -> ((MarketplaceTemplateProviderFactory) x).getCategory().equalsIgnoreCase(category) && !session.rootBrand().whitelistedMarketplace(realm).contains(((MarketplaceTemplateProviderFactory) x).getId())).collect(Collectors.toList());
        }
        //Sorting the values before creating the meta-data.
        providerFactory.sort(Comparator.comparing(x -> ((MarketplaceTemplateProviderFactory) x)
                .getTitle(session).trim().toLowerCase()));
        return buildMarketPlaceTemplateMetadata(providerFactory, false);
    }

    /**
     * Get particular template by id
     *
     * @return Returns a template details
     */
    @Path("/templates/{category}/{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getTemplate(@PathParam("category") String category, @PathParam("id") String id) {
        auth.realm().requireViewRealm();

        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(MarketplaceTemplateProvider.class);
        Optional<ProviderFactory> providerFactory = factories.stream().filter(x-> !session.rootBrand().whitelistedMarketplaceCategories(realm).contains(((MarketplaceTemplateProviderFactory) x).getCategory())).filter(x -> ((MarketplaceTemplateProviderFactory) x).getId().equals(id) && !session.rootBrand().whitelistedMarketplace(realm).contains(((MarketplaceTemplateProviderFactory) x).getId())).findFirst();
        if (!providerFactory.isPresent()) {
            throw new NotFoundException("Could not find template with id");
        }
        return buildMarketPlaceTemplateMetadata(providerFactory.get(), true);
    }

    @Path("/templates/{category}/{id}")
    @DELETE
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public void removePartner(@PathParam("category") String category, @PathParam("id") String id) {
        auth.realm().requireViewRealm();
        Optional<ProviderFactory> providerFactory =
                session.getKeycloakSessionFactory().getProviderFactoriesStream(MarketplaceTemplateProvider.class).filter(x -> ((MarketplaceTemplateProviderFactory) x).getId().equalsIgnoreCase(id)).findFirst();
        if (!providerFactory.isPresent()) {
            throw new NotFoundException("Could not find config with id");
        }

        removePartner((MarketplaceTemplateProviderFactory) providerFactory.get());
        if(category.contains("-")){
            adminEvent.resource(category.split("-")[0].toUpperCase());
        }
        else{
            adminEvent.resource(category.toUpperCase());
        }
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(providerFactory.get()).success();
        return;
    }

    private void removePartner(MarketplaceTemplateProviderFactory factory) {
        Map<String, Object> marketProviders = factory.getMarketProviders(session);
        ObjectMapper mapper = new ObjectMapper();
        String onboardingTemplateId = (String) mapper.convertValue(marketProviders.get("onboarding"), Map.class).get(
                "id");
        String loginTemplateId = (String) mapper.convertValue(marketProviders.get("login"), Map.class).get("id");

        // To check whether any onboarding and login flows are in use
        List<AuthenticationFlowModel> authenticationFlows = session.getContext().getRealm().getAuthenticationFlowsStream().collect(Collectors.toList());
        for (AuthenticationFlowModel authenticationFlow : authenticationFlows) {
            if (authenticationFlow.getTemplateAlias() != null && authenticationFlow.getTemplateAlias().toLowerCase().contains(factory.getTitle(session).toLowerCase()) && authenticationFlow.getId() != null) {
                if (ModelToRepresentation.checkIfFlowIsLive(session.getContext().getRealm(), authenticationFlow.getId())) {
                    throw new BadRequestException("cant-delete-live-flow");
                }
            }
        }

        List<AuthenticationFlowModel> onboardingExecutionModel =
                realm.getAuthenticationFlowsStream().filter(x -> x.getTemplateId() != null &&
                        x.getTemplateId().equals(onboardingTemplateId)).collect(Collectors.toList());
        for (AuthenticationFlowModel executionModel : onboardingExecutionModel) {
            realm.removeAuthenticationFlow(executionModel, false);
        }

        List<AuthenticationFlowModel> loginExecutionModel =
                realm.getAuthenticationFlowsStream().filter(x -> x.getTemplateId() != null &&
                        x.getTemplateId().equals(loginTemplateId)).collect(Collectors.toList());
        for (AuthenticationFlowModel executionModel : loginExecutionModel) {
            realm.removeAuthenticationFlow(executionModel, false);
        }

        // Remove MFA
        String partner = factory.getTitle(session).toLowerCase();
        Set<String> realmAttributes = realm.getAttributes().keySet();

        //to check whether MFA is used in any client
        List<ClientModel> configuredMfas = session.getContext().getRealm().getClientsStream().collect(Collectors.toList());
        for (ClientModel configuredMfa : configuredMfas) {
            if (configuredMfa.getMFAConnections().get(partner + "-mfa") != null && configuredMfa.getMFAConnections().get(partner + "-mfa") == true) {
                throw new BadRequestException("We can't delete the MFA since it's used in our applications");
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

        if(partnerMfa.isPresent()){
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
        List<MarketplaceConfigModel> marketConfigs =
                realm.getMarketplaceConfigsStream().filter(x -> x.getProviderId().contains(partner)).collect(Collectors.toList());
        for (MarketplaceConfigModel marketConfig : marketConfigs) {
            realm.removeMarketplaceConfig(marketConfig);
        }

    }


    /**
     * Get Recommended marketplace templates
     * <p>
     * Returns a list of onboarding marketplace templates.
     */
    @Path("/recommended")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getRecommended() {
        auth.realm().requireViewRealm();

        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(MarketplaceTemplateProvider.class);

        List<ProviderFactory> providerFactory = factories.stream().filter(x-> !session.rootBrand().whitelistedMarketplaceCategories(realm).contains(((MarketplaceTemplateProviderFactory) x).getCategory())).filter(x -> ((MarketplaceTemplateProviderFactory) x).isRecommended() && !session.rootBrand().whitelistedMarketplace(realm).contains(((MarketplaceTemplateProviderFactory) x).getId())).collect(Collectors.toList());

        return buildMarketPlaceTemplateMetadata(providerFactory, false);
    }

    /**
     * Install a template
     *
     * @return Returns a response
     */
    @Path("/templates/{category}/{id}/trial")
    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response takeTrial(@PathParam("category") String category, @PathParam("id") String id) {
        auth.realm().requireViewRealm();

        //Template Existence Check
        MarketplaceTemplateProviderFactory mpFactory = (MarketplaceTemplateProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(MarketplaceTemplateProvider.class, id);


        if (mpFactory == null) {
            throw new NotFoundException("Could not find template with id");
        }

        if (session.rootBrand().whitelistedMarketplace(realm).contains(mpFactory.getCategory()) || session.rootBrand().whitelistedMarketplace(realm).contains(mpFactory.getId())) {
            throw new NotFoundException("Could not find template with id");
        }

        InstallResponse installResponse = null;

        MarketplaceTemplateProvider mp = mpFactory.create(session);
        installResponse = mp.onTrialSetup(session,realm);
        if(!installResponse.isSuccess())
            return Response.status(Response.Status.BAD_REQUEST).entity(installResponse).build();

        return Response.ok(installResponse).build();
    }



    /**
     * Install a template
     *
     * @return Returns a response
     */
    @Path("/templates/{category}/{id}")
    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response installTemplate(@PathParam("category") String category, @PathParam("id") String id, MarketplaceTemplateRepresentation representation) {
        auth.realm().requireViewRealm();

        if(representation.getConfig().get("name") != null) {
            String templateName = representation.getConfig().get("name");
            String finalTemplateName = GeneralHelper.checkAppName(templateName);
            representation.getConfig().remove("name");
            representation.getConfig().put("name",finalTemplateName);

            // Check if name have special characters
            String specialCharacters ="[!@#$%&*()^+-_:;'\",.=|<>?{}`\\[\\]~]";
            String pattern = ".*[" + specialCharacters + "&&[^A-Z]&&[^0-9]].*";
            if (templateName.matches(pattern)) {
                return ErrorResponse.error("enter-client-name-without-special-characters", Response.Status.BAD_REQUEST);
            }
        }


        //for adding resourceType for admin logs
        if(category.contains("-")){
            adminEvent.resource(category.split("-")[0].toUpperCase());
        }
        else{
            adminEvent.resource(category.toUpperCase());
        }

        //Template Existence Check
        MarketplaceTemplateProviderFactory mpFactory = (MarketplaceTemplateProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(MarketplaceTemplateProvider.class, id);


        if (mpFactory == null) {
            throw new NotFoundException("Could not find template with id");
        }

        if (session.rootBrand().whitelistedMarketplace(realm).contains(mpFactory.getCategory()) || session.rootBrand().whitelistedMarketplace(realm).contains(mpFactory.getId())) {
            throw new NotFoundException("Could not find template with id");
        }



        InstallResponse installResponse = null;
        List<String> validations = mpFactory.validateConfig(session, representation.getConfig(), false);
        if (validations != null && !validations.isEmpty()) {
            installResponse = new InstallResponse(false, String.join("\n", validations), null);
            return ErrorResponse.error(validations.get(0), Response.Status.BAD_REQUEST);
        }

        MarketplaceTemplateProvider mp = mpFactory.create(session);
        try {
            installResponse = mp.install(session, representation.getConfig());
            representation.setId(id);
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(representation).success();
        } catch (Exception e) {
            e.printStackTrace();
            return ErrorResponse.exists(e.getMessage());
        }
        return Response.ok(installResponse).build();
    }

    /***
     * Search for a template based on its name and keywords
     *
     * @param pattern
     * @return
     */
    @Path("/templates/search")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> searchTemplates(@QueryParam("pattern") String pattern) {
        auth.realm().requireViewRealm();
        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactories(MarketplaceTemplateProvider.class);

        List<ProviderFactory> providerFactory = factories.stream().filter(x -> ((MarketplaceTemplateProviderFactory) x).getTitle(session).toUpperCase().contains(pattern.toUpperCase()) || Arrays.stream(((MarketplaceTemplateProviderFactory) x).getKeywords()).anyMatch(s -> s.toUpperCase().contains(pattern.toUpperCase()))).filter(x-> !session.rootBrand().whitelistedMarketplaceCategories(realm).contains(((MarketplaceTemplateProviderFactory) x).getCategory())).filter(x -> !session.rootBrand().whitelistedMarketplace(realm).contains(((MarketplaceTemplateProviderFactory) x).getId())).collect(Collectors.toList());

        return buildMarketPlaceTemplateMetadata(providerFactory, false);
    }

    public List<Map<String, Object>> buildMarketPlaceTemplateMetadata(List<ProviderFactory> factories, boolean putAll) {
        List<Map<String, Object>> providers = new LinkedList<>();
        for (ProviderFactory factory : factories) {
            providers.add(buildMarketPlaceTemplateMetadata(factory, putAll));
        }
        return providers;
    }

    public Map<String, Object> buildMarketPlaceTemplateMetadata(ProviderFactory factory, boolean pullAll) {
        MarketplaceTemplateProviderFactory mpFactory = (MarketplaceTemplateProviderFactory) factory;
        Map<String, Object> data = new HashMap<>();
        data.put("id", factory.getId());
        data.put("logo", mpFactory.getLogo());
        data.put("title", mpFactory.getTitle(session));
        data.put("category", mpFactory.getCategory());
        data.put("author", mpFactory.getAuthor(session));
        data.put("description", mpFactory.getDescription(session));
        data.put("keywords", mpFactory.getKeywords());
        data.put("isNew", mpFactory.isNew());
        data.put("installType", mpFactory.installType());
        data.put("isInstalled", mpFactory.isInstalled(session));
        data.put("isImplemented", mpFactory.isImplemented());
        data.put("buttonNameBeforeInstall", mpFactory.buttonNameBeforeInstall());
        data.put("buttonNameAfterInstall", mpFactory.buttonNameAfterInstall());
        if(mpFactory.serviceLocations() != null) {
            data.put("serviceLocations", mpFactory.serviceLocations());
        }
        data.put("arrange1to1Meetings", mpFactory.arrange1to1Meetings());
        data.put("meetingLink",mpFactory.meetingLink());
        boolean isUnderTrial = mpFactory.isUnderTrial(realm);

        if(!isUnderTrial)
        {
            String trialDate = realm.getAttribute(mpFactory.getId()+"-trial-date");
            if(trialDate != null)
            {
                realm.removeAttribute(mpFactory.getId()+"-trial-date");
                removePartner(mpFactory);
                isUnderTrial = mpFactory.isUnderTrial(realm);
            }
        }
        data.put("isUnderTrial",isUnderTrial);
        data.put("showTrialOption", mpFactory.showTrialOption(realm));
        data.put("showTrialDays", mpFactory.showTrialDays(realm));

        //if pullAll=true will be  used for response of specific template with ID
        if (pullAll) {
            List<ConfigPropertyRepresentation> rep = new ArrayList<>();
            List<ProviderConfigProperty> configProperties = mpFactory.getConfigProperties(session);
            if (configProperties != null || !configProperties.isEmpty()) {
                for (ProviderConfigProperty prop : configProperties) {
                    ConfigPropertyRepresentation propRep = ModelToRepresentation.toRepresentation(prop);
                    rep.add(propRep);
                }
                data.put("config", rep);
            }
            data.put("overviewMdUrl", mpFactory.getOverviewMdUrl());
            data.put("installationMdUrl", mpFactory.getInstallationMdUrl());

            //getting constraints for the template
            ConstraintResponse constraintResponse = mpFactory.resolveConstraints(session);
            ObjectMapper objectMapper = new ObjectMapper();
            if (constraintResponse == null) {
                constraintResponse = new ConstraintResponse();
            }
            data.put("constraints", objectMapper.convertValue(constraintResponse, Map.class));
            Map<String, Object> marketProviders = mpFactory.getMarketProviders(session);
            if (marketProviders != null && marketProviders.size() > 0) {
                data.putAll(mpFactory.getMarketProviders(session));
            }
            ThirdPartyProvider thirdPartyProvider = mpFactory.get3rdPartyProvider();
            if (thirdPartyProvider != null) {
                data.put("thirdParty", thirdPartyProvider);
            }

        }
        return data;
    }

    public static void checkPolicyExists(KeycloakSession session, RealmModel realm, String policyCheckName){
        session.policies().getAllPoliciesStream(realm,null,null).forEach(policy->{
            if(policy.getPolicyName().equals(policyCheckName)){
               throw new RuntimeException("policy-existing-txt");
            }
        });
    }

    public static String checkTemplateName(MarketplaceTemplateRepresentation representation){
        String templateName = "";
        if(representation.getConfig().get("name") != null){
            templateName = representation.getConfig().get("name");
        }
        if(templateName.contains(" ")){
            templateName = templateName.replaceAll("\\s+", " ");
        }
        return templateName.trim();
    }
}

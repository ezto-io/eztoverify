/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.GeneralException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.email.ConfigTestResponse;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.locale.LocaleType;
import org.keycloak.marketplace.TemplateType;
import org.keycloak.mfa.MFAProvider;
import org.keycloak.mfa.MFAProviderFactory;
import org.keycloak.models.*;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.*;
import org.keycloak.services.AuditTestResponse;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.sms.SmsProvider;
import org.keycloak.sms.SmsProviderFactory;
import org.keycloak.sms.SmsSenderProvider;
import org.keycloak.sms.SmsSenderProviderFactory;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.CredentialHelper;
import org.keycloak.utils.GeneralHelper;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.ws.rs.core.Response.Status.*;
import static org.keycloak.authentication.authenticators.utils.Constants.*;
import static org.keycloak.utils.GeneralHelper.formatCostDetailsForTemplate;
import static org.keycloak.utils.GeneralHelper.getDefaultLogoUrl;

/**
 * @author Bill Burke
 * @resource Authentication Management
 */
public class AuthenticationManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    private Properties properties;

    protected static final Logger logger = Logger.getLogger(AuthenticationManagementResource.class);
    private static final String ERROR_REQ_BODY_NOT_FOUND = "req-body-not-found";

    public AuthenticationManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTH_FLOW);
        this.properties = GeneralHelper.getProperties(session, realm, auth.adminAuth().getUser());
    }

    /**
     * Get form providers
     * <p>
     * Returns a stream of form providers.
     */
    @Path("/form-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<Map<String, Object>> getFormProviders() {
        auth.realm().requireViewRealm();

        return buildProviderMetadata(session.getKeycloakSessionFactory().getProviderFactoriesStream(FormAuthenticator.class));
    }

    /**
     * Get authenticator providers
     * <p>
     * Returns a stream of authenticator providers.
     */
    @Path("/authenticator-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<Map<String, Object>> getAuthenticatorProviders(@QueryParam("type") String type) {
        auth.realm().requireViewRealm();

        List<ProviderFactory> factories = session.getKeycloakSessionFactory().getProviderFactoriesStream(Authenticator.class).collect(Collectors.toList());

        List<ProviderFactory> finalFactories = new ArrayList<>();

        if (type != null) {
            finalFactories.addAll(factories.stream().map(x -> (AuthenticatorFactory) x)
                    .filter(x -> x.authenticatorType() != null
                            && (x.authenticatorType().toString().equals(type.toUpperCase(Locale.ROOT))
                            || x.authenticatorType().toString().equals(AuthenticationType.ALL.toString().toUpperCase(Locale.ROOT))))
                    .filter(x -> !(session.rootBrand().whitelistedAuthenticators(realm).contains(x.getId())))
                    .collect(Collectors.toList()));
            try {
                if (AuthenticationType.valueOf(type.toUpperCase()) == AuthenticationType.LOGIN || AuthenticationType.valueOf(type.toUpperCase(Locale.ROOT)) == AuthenticationType.ONBOARDING) {
                    finalFactories.addAll(factories.stream().map(x -> (AuthenticatorFactory) x).filter(x -> x.authenticatorType() != null && x.authenticatorType().toString().equals(AuthenticationType.ONBOARDING_LOGIN.name().toUpperCase())).collect(Collectors.toList()));
                }
            } catch (Exception err) {
                logger.error(err);
            }
        }

        return buildProviderMetadata(finalFactories.stream().filter(x -> !((AuthenticatorFactory) x).isInBuilt()));
    }

    /**
     * Get client authenticator providers
     * <p>
     * Returns a stream of client authenticator providers.
     */
    @Path("/client-authenticator-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<Map<String, Object>> getClientAuthenticatorProviders() {
        auth.realm().requireViewClientAuthenticatorProviders();

        return buildProviderMetadata(session.getKeycloakSessionFactory().getProviderFactoriesStream(ClientAuthenticator.class));
    }

    public Stream<Map<String, Object>> buildProviderMetadata(Stream<ProviderFactory> factories) {
        return factories.map(factory -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", factory.getId());

            AuthenticatorFactory af = (AuthenticatorFactory) factory;
            data.put("articleId", af.getArticleId());

            ConfigurableAuthenticatorFactory configured = (ConfigurableAuthenticatorFactory) factory;
            data.put("description", configured.getHelpText());
            data.put("displayName", configured.getDisplayType());
            return data;
        });
    }

    /**
     * Get form action providers
     * <p>
     * Returns a stream of form action providers.
     */
    @Path("/form-action-providers")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<Map<String, Object>> getFormActionProviders() {
        auth.realm().requireViewRealm();

        return buildProviderMetadata(session.getKeycloakSessionFactory().getProviderFactoriesStream(FormAction.class));
    }


    /**
     * Get authentication flows
     * <p>
     * Returns a stream of authentication flows.
     */
    @Path("/flows")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<AuthenticationFlowRepresentation> getFlows() {
        auth.realm().requireViewAuthenticationFlows();

        return realm.getAuthenticationFlowsStream()
                .filter(flow -> flow.isTopLevel() && !Objects.equals(flow.getAlias(), DefaultAuthenticationFlows.SAML_ECP_FLOW))
                .filter(flow -> !flow.isBuiltIn() && !realm.getName().equals(org.keycloak.Config.getAdminRealm()))
                .map(flow -> ModelToRepresentation.toRepresentation(realm, flow));
    }

    /**
     * Get authentication flows
     * <p>
     * Returns a list of authentication flows.
     */
    @Path("/installed/flows")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<AuthenticationFlowRepresentation> getInstalledFlows() {
        auth.realm().requireViewAuthenticationFlows();

        return realm.getAuthenticationFlowsStream()
                .filter(flow -> flow.isTopLevel() && !Objects.equals(flow.getAlias(), DefaultAuthenticationFlows.SAML_ECP_FLOW))
                .filter(flow -> !flow.isBuiltIn() && flow.isInstalled() && flow.isTopLevel())
                .map(flow -> ModelToRepresentation.toRepresentation(realm, flow));
    }

    @Path("/installed/onboarding/flows")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<AuthenticationFlowRepresentation> getOnboardingFlows() {
        auth.realm().requireViewAuthenticationFlows();

        return realm.getAuthenticationFlowsStream()
                .filter(flow -> flow.isTopLevel() && !Objects.equals(flow.getAlias(), DefaultAuthenticationFlows.SAML_ECP_FLOW))
                .filter(flow -> !flow.isBuiltIn() && flow.isInstalled() && flow.isTopLevel())
                .filter(flow -> flow.getTemplateType().equalsIgnoreCase(TemplateType.ONBOARDING.name()))
                .map(flow -> ModelToRepresentation.toRepresentation(realm, flow));
    }

    @Path("/installed/recovery/flows")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<AuthenticationFlowRepresentation> getRecoveryFlows() {
        auth.realm().requireViewAuthenticationFlows();

        return realm.getAuthenticationFlowsStream()
                .filter(flow -> flow.isTopLevel() && !Objects.equals(flow.getAlias(), DefaultAuthenticationFlows.SAML_ECP_FLOW))
                .filter(flow -> !flow.isBuiltIn() && flow.isInstalled() && flow.isTopLevel())
                .filter(flow -> flow.getTemplateType().equalsIgnoreCase(TemplateType.RECOVERY.name()))
                .map(flow -> ModelToRepresentation.toRepresentation(realm, flow));
    }

    @Path("/installed/login/flows")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<AuthenticationFlowRepresentation> getLoginFlows() {
        auth.realm().requireViewAuthenticationFlows();

        return realm.getAuthenticationFlowsStream()
                .filter(flow -> flow.isTopLevel() && !Objects.equals(flow.getAlias(), DefaultAuthenticationFlows.SAML_ECP_FLOW))
                .filter(flow -> !flow.isBuiltIn() && flow.isInstalled() && flow.isTopLevel())
                .filter(flow -> flow.getTemplateType().equalsIgnoreCase(TemplateType.LOGIN.name()))
                .map(flow -> ModelToRepresentation.toRepresentation(realm, flow));
    }

    @Path("/installed/direct_grant/flows")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<AuthenticationFlowRepresentation> getDirectGrantFlows() {
        auth.realm().requireViewAuthenticationFlows();

        return realm.getAuthenticationFlowsStream()
                .filter(flow -> flow.isTopLevel() && !Objects.equals(flow.getAlias(), DefaultAuthenticationFlows.SAML_ECP_FLOW))
                .filter(flow -> !flow.isBuiltIn() && flow.isInstalled() && flow.isTopLevel())
                .filter(flow -> flow.getTemplateType().equalsIgnoreCase("direct_grant"))
                .map(flow -> ModelToRepresentation.toRepresentation(realm, flow));
    }

    @Path("actions/gif/{clientId}/{actionId}/{action}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<Map<String, Object>> getGif(@PathParam("actionId") String actionId, @PathParam("action") String action, @PathParam("clientId") String clientId) {
        auth.realm().requireViewRealm();

        List<Map<String, Object>> webToDeviceGifList = new ArrayList<>();

        List<String> requiredActionsInFlow = GeneralHelper.getRequiredActionsInFlow(realm, actionId);

        if (requiredActionsInFlow.contains("validator")) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "preview-not-available");
            List<Map<String, Object>> result = new ArrayList<>();
            result.add(error);
            return result;
        }

        List<Map<String, Object>> gifList = GeneralHelper.getGifUrlMap(session, realm, actionId, session.getContext().resolveLocale(auth.adminAuth().getUser(), LocaleType.ADMIN).getLanguage());

        if (requiredActionsInFlow.contains(WEB_TO_DEVICE)) {
            webToDeviceGifList.add(new HashMap<String, Object>() {{
                put(GIF_ID, realm.getRequiredActionProviderByAlias(WEB_TO_DEVICE).getProviderId());
                put(GIF_URL, DEFAULT_SERVER_URL + TEMPLATE_PATH + session.getContext().resolveLocale(auth.adminAuth().getUser(), LocaleType.ADMIN).getLanguage() + "/" + WEB_TO_DEVICE + "/" + WEB_TO_DEVICE + ".gif");
            }});
            List<String> webToDeviceConfig = realm.getRequiredActionProvidersStream(actionId)
                    .filter(x -> x.getName().equals(WEB_TO_DEVICE) && !x.getConfig().isEmpty())
                    .map(x -> x.getConfig().get("app-id"))
                    .collect(Collectors.toList());

            if (!webToDeviceConfig.isEmpty()) {
                GeneralHelper.getGifUrlMap(session, realm, realm.getClientByClientId(webToDeviceConfig.get(0)).getAuthenticationFlowBindingOverride(action + "-action"), session.getContext().resolveLocale(auth.adminAuth().getUser(), LocaleType.ADMIN).getLanguage())
                        .stream()
                        .forEach(x -> webToDeviceGifList.add(x));
            }
        }

        List<Map<String, Object>> finalGifList = new ArrayList<>();
        if (webToDeviceGifList.isEmpty()) {
            finalGifList.addAll(gifList);
        } else {
            gifList.forEach(x -> {
                if (x.get(GIF_ID).equals(WEB_TO_DEVICE)) {
                    x.put("subFlow", new ArrayList<>(webToDeviceGifList));
                }
                finalGifList.add(x);
            });
        }

        if (webToDeviceGifList.stream().filter(x -> x.get("id").equals("Validator")).count() != 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "preview-not-available");
            List<Map<String, Object>> result = new ArrayList<>();
            result.add(error);
            return result;
        }

        if ((long) finalGifList.size() > 0) {
            finalGifList.add(new HashMap<String, Object>() {{
                put(GIF_ID, action + "-completed");
                put(GIF_URL, DEFAULT_SERVER_URL + TEMPLATE_PATH + session.getContext().resolveLocale(auth.adminAuth().getUser(), LocaleType.ADMIN).getLanguage() + "/" + action + "-completed" + "/" + action + "-completed" + ".gif");
            }});
        }

        return finalGifList;
    }

    @Path("actions/{actionId}/gif")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<Map<String, Object>> getGif(@PathParam("actionId") String actionId) {
        auth.realm().requireViewRealm();

        List<String> requiredActionsInFlow = GeneralHelper.getRequiredActionsInFlow(realm, actionId);

        if (requiredActionsInFlow.contains("validator")) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "preview-not-available");
            List<Map<String, Object>> result = new ArrayList<>();
            result.add(error);
            return result;
        }

        List<Map<String, Object>> gifList = new ArrayList<>();

        requiredActionsInFlow.forEach(templateId -> {
            session.getKeycloakSessionFactory().getProviderFactoriesStream(RequiredActionProvider.class)
                    .filter(factory -> factory.getId().equals(templateId))
                    .map(factory -> (RequiredActionFactory) factory)
                    .forEach(factory -> {
                        if (factory.gifUrl(session.getContext().resolveLocale(auth.adminAuth().getUser(), LocaleType.ADMIN).getLanguage()) != null) {
                            gifList.add(new HashMap<String, Object>() {{
                                put(GIF_ID, factory.getDisplayText());
                                put(GIF_URL, factory.gifUrl(session.getContext().resolveLocale(auth.adminAuth().getUser(), LocaleType.ADMIN).getLanguage()));
                            }});
                        }
                    });
        });

        return gifList;
    }

    /**
     * Install a new template of given template type
     *
     * @return Returns a response
     */
    @Path("/templates/{templatetype}/save")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveCustomTemplate(@PathParam("templatetype") String templateType, AuthenticationFlowRepresentation flow) {

        // Checking permissions
        auth.realm().requireManageRealm();

        // Template type [ onboarding,login,recovery ]  exists check
        if (flow.getTemplateType() == null || flow.getTemplateType().isEmpty()) {
            return ErrorResponse.exists("Failed to get Template Type from Request");
        }

        // Alias Null/Empty Check
        if (flow.getAlias() == null || flow.getAlias().isEmpty()) {
            return ErrorResponse.exists("Failed to create flow with empty alias name");
        }

        // Existing Alias  Check
        if (realm.getFlowByAlias(flow.getAlias()) != null) {
            logger.warn("Flow " + flow.getAlias() + " already exists");
            return ErrorResponse.exists("flow-name-exists");
        }

        ReservedCharValidator.validate(flow.getAlias());

        AuthenticationFlowModel createdModel = realm.addAuthenticationFlow(RepresentationToModel.toModel(flow));

        if (templateType.equalsIgnoreCase("login")) {
            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(createdModel.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setAuthenticator("auth-cookie");
            execution.setPriority(5);
            execution.setHidden(true);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);

            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(createdModel.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setAuthenticator("offline-authenticator");
            execution.setPriority(8);
            execution.setHidden(true);
            execution.setAuthenticatorFlow(false);
            realm.addAuthenticatorExecution(execution);

            AuthenticationFlowModel subflow = new AuthenticationFlowModel();
            subflow.setAlias("sub_" + flow.getAlias());
            subflow.setDescription(createdModel.getDescription());
            subflow.setProviderId("basic-flow");
            subflow.setTopLevel(false);
            subflow.setBuiltIn(false);
            subflow.setHidden(true);
            subflow.setTemplateType("LOGIN");
            subflow.setInstalled(true);
            subflow = realm.addAuthenticationFlow(subflow);
            execution = new AuthenticationExecutionModel();
            execution.setParentFlow(createdModel.getId());
            execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            execution.setFlowId(subflow.getId());
            execution.setPriority(10);
            execution.setAuthenticatorFlow(true);
            realm.addAuthenticatorExecution(execution);
            flow.setId(subflow.getId());
        } else {
            flow.setId(createdModel.getId());
        }

        List<AuthenticationExecutionExportRepresentation> executions = flow.getAuthenticationExecutions();

        if (executions != null && executions.size() > 0) {
            executions.forEach((exec) -> {

                String provider = exec.getAuthenticator();

                // make sure provider is one of the registered providers
                ProviderFactory f;
                if (flow.getProviderId().equals(AuthenticationFlow.CLIENT_FLOW)) {
                    f = session.getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, provider);
                } else if (flow.getProviderId().equals(AuthenticationFlow.FORM_FLOW)) {
                    f = session.getKeycloakSessionFactory().getProviderFactory(FormAction.class, provider);
                } else {
                    f = session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, provider);
                }
                if (f == null) {
                    throw new BadRequestException("No authentication provider found for id: " + provider);
                }

                AuthenticationExecutionModel execution = new AuthenticationExecutionModel();

                execution.setParentFlow(flow.getId());

                ConfigurableAuthenticatorFactory conf = (ConfigurableAuthenticatorFactory) f;
                if (conf.getRequirementChoices().length == 1)
                    execution.setRequirement(conf.getRequirementChoices()[0]);
                else
                    execution.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(exec.getRequirement()));

                execution.setAuthenticatorFlow(false);
                execution.setAuthenticator(provider);
                execution.setPriority(getNextPriority(createdModel));

                execution = realm.addAuthenticatorExecution(execution);

            });
        }


        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), createdModel.getId()).representation(createdModel).success();

        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(createdModel.getId()).build()).entity(createdModel).build();
    }

    /**
     * Create a new authentication flow
     *
     * @param flow Authentication flow representation
     * @return
     */
    @Path("/flows")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createFlow(AuthenticationFlowRepresentation flow) {
        auth.realm().requireManageRealm();

        if (flow.getAlias() == null || flow.getAlias().isEmpty()) {
            return ErrorResponse.exists("Failed to create flow with empty alias name");
        }

        if (realm.getFlowByAlias(flow.getAlias()) != null) {
            return ErrorResponse.exists("Flow " + flow.getAlias() + " already exists");
        }

        ReservedCharValidator.validate(flow.getAlias());

        AuthenticationFlowModel createdModel = realm.addAuthenticationFlow(RepresentationToModel.toModel(flow));

        flow.setId(createdModel.getId());
        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), createdModel.getId()).representation(flow).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(flow.getId()).build()).build();
    }

    /**
     * Get authentication flow for id
     *
     * @param id Flow id
     * @return
     */
    @Path("/flows/{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationFlowasTemplateRepresentation getFlow(@PathParam("id") String id) {
        auth.realm().requireViewRealm();

        AuthenticationFlowModel flow = realm.getAuthenticationFlowById(id);
        if (flow == null) {
            throw new NotFoundException("Could not find flow with id");
        }

        return ModelToRepresentation.toRepresentation(realm, flow, true);
    }

    /**
     * Update an authentication flow
     *
     * @param flow Authentication flow representation
     * @return
     */
    @Path("/flows/{id}")
    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFlow(@PathParam("id") String id, AuthenticationFlowRepresentation flow) {
        auth.realm().requireManageRealm();

        AuthenticationFlowasTemplateRepresentation existingFlow = getFlow(id);

        if (flow.getAlias() == null || flow.getAlias().isEmpty()) {
            return ErrorResponse.exists("Failed to update flow with empty alias name");
        }

        //check if updating a correct flow
        AuthenticationFlowModel checkFlow = realm.getAuthenticationFlowById(id);
        if (checkFlow == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");
        }

        //if a different flow with the same name does already exist, throw an exception
        if (realm.getFlowByAlias(flow.getAlias()) != null && !checkFlow.getAlias().equals(flow.getAlias())) {
            return ErrorResponse.exists("Flow alias name already exists");
        }

        //if the name changed
        if (checkFlow.getAlias() != null && !checkFlow.getAlias().equals(flow.getAlias())) {
            checkFlow.setAlias(flow.getAlias());
        } else if (checkFlow.getAlias() == null && flow.getAlias() != null) {
            checkFlow.setAlias(flow.getAlias());
        }

        //check if the description changed
        if (checkFlow.getDescription() != null && !checkFlow.getDescription().equals(flow.getDescription())) {
            checkFlow.setDescription(flow.getDescription());
        } else if (checkFlow.getDescription() == null && flow.getDescription() != null) {
            checkFlow.setDescription(flow.getDescription());
        }

        //update the flow
        flow.setId(existingFlow.getId());
        realm.updateAuthenticationFlow(RepresentationToModel.toModel(flow));
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(flow).success();
        return Response.accepted(flow).build();
    }

    /**
     * Delete an authentication flow
     *
     * @param id Flow id
     */
    @Path("/flows/{id}")
    @DELETE
    @NoCache
    public Response deleteFlow(@PathParam("id") String id) {
        auth.realm().requireManageRealm();

        return deleteFlow(id, true);
    }

    private Response deleteFlow(String id, boolean isTopMostLevel) {
        AuthenticationFlowModel flow = realm.getAuthenticationFlowById(id);
        if (flow == null) {
            throw new NotFoundException("Could not find flow with id");
        }
        if (flow.isBuiltIn()) {
            throw new BadRequestException("Can't delete built in flow");
        }

        if (ModelToRepresentation.checkIfFlowIsLive(realm, flow.getId())) {
            return ErrorResponse.exists("cant-delete-live-flow");
        }

        realm.getClientsStream().forEach(
                clientModel -> {
                    clientModel.getAuthenticationFlowBindingOverrides().forEach((k, v) -> {
                        if (v.equals(id)) {
                            clientModel.removeAuthenticationFlowBindingOverride(k);
                        }
                    });
                }
        );

        Iterator<Map.Entry<String, String>> realmIterator = realm.getAuthenticationFlowBindingOverrides().entrySet().iterator();
        while (realmIterator.hasNext()) {
            Map.Entry<String, String> entry = realmIterator.next();
            if (entry.getValue().contains(id)) {
                realmIterator.remove();
            }
        }

        realm.getAuthenticationExecutionsStream(id)
                .map(AuthenticationExecutionModel::getFlowId)
                .filter(Objects::nonNull)
                .forEachOrdered(flowId -> deleteFlow(flowId, false));

        realm.removeAuthenticationFlow(flow, false);

        // Use just one event for top-level flow. Using separate events won't work properly for flows of depth 2 or bigger
        if (isTopMostLevel)
            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(flow).success();
        return Response.ok(true).build();
    }

    /**
     * Copy existing authentication flow under a new name
     * <p>
     * The new name is given as 'newName' attribute of the passed JSON object
     *
     * @param flowAlias Name of the existing authentication flow
     * @param data      JSON containing 'newName' attribute
     */
    @Path("/flows/{flowAlias}/copy")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response copy(@PathParam("flowAlias") String flowAlias, Map<String, String> data) {
        auth.realm().requireManageRealm();

        String newName = data.get("newName");
        if (realm.getFlowByAlias(newName) != null) {
            return ErrorResponse.exists("New flow alias name already exists");
        }

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debug("flow not found: " + flowAlias);
            return Response.status(NOT_FOUND).build();
        }
        AuthenticationFlowModel copy = copyFlow(realm, flow, newName);

        data.put("id", copy.getId());
        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(data).success();

        return Response.status(Response.Status.CREATED).build();

    }

    public static AuthenticationFlowModel copyFlow(RealmModel realm, AuthenticationFlowModel flow, String newName) {
        AuthenticationFlowModel copy = new AuthenticationFlowModel();
        copy.setAlias(newName);
        copy.setDescription(flow.getDescription());
        copy.setProviderId(flow.getProviderId());
        copy.setBuiltIn(false);
        copy.setTopLevel(flow.isTopLevel());
        copy = realm.addAuthenticationFlow(copy);
        copy(realm, newName, flow, copy);
        return copy;
    }

    public static void copy(RealmModel realm, String newName, AuthenticationFlowModel from, AuthenticationFlowModel to) {
        realm.getAuthenticationExecutionsStream(from.getId()).forEachOrdered(execution -> {
            if (execution.isAuthenticatorFlow()) {
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());
                AuthenticationFlowModel copy = new AuthenticationFlowModel();
                copy.setAlias(newName + " " + subFlow.getAlias());
                copy.setDescription(subFlow.getDescription());
                copy.setProviderId(subFlow.getProviderId());
                copy.setBuiltIn(false);
                copy.setTopLevel(false);
                copy = realm.addAuthenticationFlow(copy);
                execution.setFlowId(copy.getId());
                copy(realm, newName, subFlow, copy);
            }
            execution.setId(null);
            execution.setParentFlow(to.getId());
            realm.addAuthenticatorExecution(execution);
        });
    }

    /**
     * Add new flow with new execution to existing flow
     *
     * @param flowAlias Alias of parent authentication flow
     * @param data      New authentication flow / execution JSON data containing 'alias', 'type', 'provider', and 'description' attributes
     */
    @Path("/flows/{flowAlias}/executions/flow")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addExecutionFlow(@PathParam("flowAlias") String flowAlias, Map<String, String> data) {
        auth.realm().requireManageRealm();

        AuthenticationFlowModel parentFlow = realm.getFlowByAlias(flowAlias);
        if (parentFlow == null) {
            return ErrorResponse.error("Parent flow doesn't exist", BAD_REQUEST);
        }
        String alias = data.get("alias");
        String type = data.get("type");
        String provider = data.get("provider");
        String description = data.get("description");


        AuthenticationFlowModel newFlow = realm.getFlowByAlias(alias);
        if (newFlow != null) {
            return ErrorResponse.exists("New flow alias name already exists");
        }
        newFlow = new AuthenticationFlowModel();
        newFlow.setAlias(alias);
        newFlow.setDescription(description);
        newFlow.setProviderId(type);
        newFlow = realm.addAuthenticationFlow(newFlow);
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlow.getId());
        execution.setFlowId(newFlow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);
        execution.setAuthenticatorFlow(true);
        execution.setAuthenticator(provider);
        execution.setPriority(getNextPriority(parentFlow));
        execution = realm.addAuthenticatorExecution(execution);

        data.put("id", execution.getId());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.AUTH_EXECUTION_FLOW).resourcePath(session.getContext().getUri()).representation(data).success();

        String addExecutionPathSegment = UriBuilder.fromMethod(AuthenticationManagementResource.class, "addExecutionFlow").build(parentFlow.getAlias()).getPath();
        return Response.created(session.getContext().getUri().getBaseUriBuilder().path(session.getContext().getUri().getPath().replace(addExecutionPathSegment, "")).path("flows").path(newFlow.getId()).build()).build();
    }

    private int getNextPriority(AuthenticationFlowModel parentFlow) {
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutionsStream(parentFlow.getId())
                .collect(Collectors.toList());
        return executions.isEmpty() ? 0 : executions.get(executions.size() - 1).getPriority() + 1;
    }

    /**
     * Add new authentication execution to a flow
     *
     * @param flowAlias Alias of parent flow
     * @param data      New execution JSON data containing 'provider' attribute
     */
    @Path("/flows/{flowAlias}/executions/execution")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addExecutionToFlow(@PathParam("flowAlias") String flowAlias, Map<String, String> data) {
        auth.realm().requireManageRealm();

        AuthenticationFlowModel parentFlow = realm.getFlowByAlias(flowAlias);
        if (parentFlow == null) {
            throw new BadRequestException("Parent flow doesn't exist");
        }
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to add execution to a built in flow");
        }
        String provider = data.get("provider");
        String requirement = data.get("requirement");

        // make sure provider is one of the registered providers
        ProviderFactory f;
        if (parentFlow.getProviderId().equals(AuthenticationFlow.CLIENT_FLOW)) {
            f = session.getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, provider);
        } else if (parentFlow.getProviderId().equals(AuthenticationFlow.FORM_FLOW)) {
            f = session.getKeycloakSessionFactory().getProviderFactory(FormAction.class, provider);
        } else {
            f = session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, provider);
        }
        if (f == null) {
            throw new BadRequestException("No authentication provider found for id: " + provider);
        }

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(parentFlow.getId());

        ConfigurableAuthenticatorFactory conf = (ConfigurableAuthenticatorFactory) f;
        if (conf.getRequirementChoices().length == 1)
            execution.setRequirement(conf.getRequirementChoices()[0]);
        else
            execution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED);

        execution.setAuthenticatorFlow(false);
        execution.setAuthenticator(provider);
        execution.setPriority(getNextPriority(parentFlow));
        if (requirement != null && !requirement.isEmpty()) {
            execution.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(requirement));
        }

        execution = realm.addAuthenticatorExecution(execution);

        data.put("id", execution.getId());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).representation(data).success();

        String addExecutionPathSegment = UriBuilder.fromMethod(AuthenticationManagementResource.class, "addExecutionToFlow").build(parentFlow.getAlias()).getPath();
        return Response.created(session.getContext().getUri().getBaseUriBuilder().path(session.getContext().getUri().getPath().replace(addExecutionPathSegment, "")).path("executions").path(execution.getId()).build()).build();
    }

    /**
     * Get authentication executions for a flow
     *
     * @param flowAlias Flow alias
     */
    @Path("/flows/{flowAlias}/executions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExecutions(@PathParam("flowAlias") String flowAlias) {
        auth.realm().requireViewRealm();

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debug("flow not found: " + flowAlias);
            return Response.status(NOT_FOUND).build();
        }
        List<AuthenticationExecutionInfoRepresentation> result = new LinkedList<>();

        int level = 0;

        recurseExecutions(flow, result, level);
        return Response.ok(result).build();
    }

    public void recurseExecutions(AuthenticationFlowModel flow, List<AuthenticationExecutionInfoRepresentation> result, int level) {
        AtomicInteger index = new AtomicInteger(0);
        realm.getAuthenticationExecutionsStream(flow.getId()).forEachOrdered(execution -> {
            AuthenticationExecutionInfoRepresentation rep = new AuthenticationExecutionInfoRepresentation();
            rep.setLevel(level);
            rep.setIndex(index.getAndIncrement());
            rep.setRequirementChoices(new LinkedList<>());

            // Checking Multiple Login options login authenticator fully configured or not.
            AuthenticatorConfigModel authenticatorConfigModel = realm.getAuthenticatorConfigById(execution.getAuthenticatorConfig());
            if (execution.getAuthenticator() != null
                    && authenticatorConfigModel != null
                    && execution.getAuthenticator().equals("multiple-login-options-authenticator")) {

                AuthenticatorConfigRepresentation authenticatorConfig = ModelToRepresentation.toRepresentation(authenticatorConfigModel);

                boolean enableMultipleLoginAuthenticator = authenticatorConfig.getConfig().entrySet().stream()
                        .anyMatch(entry -> !entry.getKey().startsWith("dependant") && entry.getValue().equals("true"));

                rep.setMultipleLoginAuthenticatorEnabled(enableMultipleLoginAuthenticator);
            }

            if (execution.isAuthenticatorFlow()) {
                AuthenticationFlowModel flowRef = realm.getAuthenticationFlowById(execution.getFlowId());
                if (AuthenticationFlow.BASIC_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.CONDITIONAL.name());
                } else if (AuthenticationFlow.FORM_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                    rep.setProviderId(execution.getAuthenticator());
                    rep.setAuthenticationConfig(execution.getAuthenticatorConfig());
                } else if (AuthenticationFlow.CLIENT_FLOW.equals(flowRef.getProviderId())) {
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                    rep.getRequirementChoices().add(AuthenticationExecutionModel.Requirement.DISABLED.name());
                }
                rep.setDisplayName(flowRef.getAlias());
                rep.setHidden(flowRef.isHidden());
                rep.setDescription(flowRef.getDescription());
                rep.setConfigurable(false);
                rep.setId(execution.getId());
                rep.setAuthenticationFlow(execution.isAuthenticatorFlow());
                rep.setRequirement(execution.getRequirement().name());
                rep.setFlowId(execution.getFlowId());
                result.add(rep);
                AuthenticationFlowModel subFlow = realm.getAuthenticationFlowById(execution.getFlowId());
                recurseExecutions(subFlow, result, level + 1);
            } else {
                String providerId = execution.getAuthenticator();
                ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
                if (factory == null) {
                    logger.warnf("Cannot find authentication provider implementation with provider ID '%s'", providerId);
                    throw new NotFoundException("Could not find authenticator provider");
                }
                rep.setDisplayName(factory.getDisplayType());
                rep.setConfigurable(factory.isConfigurable());
                for (AuthenticationExecutionModel.Requirement choice : factory.getRequirementChoices()) {
                    rep.getRequirementChoices().add(choice.name());
                }
                rep.setId(execution.getId());

                AuthenticationExecutionModel executionModel = realm.getAuthenticationExecutionById(execution.getId());
                if (executionModel != null) {
                    rep.setPriority(executionModel.getPriority());
                }
                if (factory.isConfigurable()) {
                    String authenticatorConfigId = execution.getAuthenticatorConfig();
                    if (authenticatorConfigId != null) {
                        AuthenticatorConfigModel authenticatorConfig = realm.getAuthenticatorConfigById(authenticatorConfigId);

                        if (authenticatorConfig != null) {
                            rep.setAlias(authenticatorConfig.getAlias());
                        }
                    }
                }
                rep.setHidden(executionModel.isHidden());
                rep.setRequirement(execution.getRequirement().name());

                providerId = execution.getAuthenticator();

                // encode the provider id in case the provider is a script deployed to the server to make sure it can be used as path parameters without break the URL syntax
                if (providerId.startsWith("script-")) {
                    providerId = Base32.encode(providerId.getBytes());
                }

                rep.setProviderId(providerId);
                rep.setAuthenticationConfig(execution.getAuthenticatorConfig());
                if (rep.getAuthenticationConfig() != null) {
                    rep.setConfig(realm.getAuthenticatorConfigById(rep.getAuthenticationConfig()).getConfig());
                }
                result.add(rep);
            }
        });
    }

    /**
     * Update authentication executions of a Flow
     *
     * @param flowAlias Flow alias
     * @param rep       AuthenticationExecutionInfoRepresentation
     */
    @Path("/flows/{flowAlias}/executions")
    @PUT
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateExecutions(@PathParam("flowAlias") String flowAlias, AuthenticationExecutionInfoRepresentation rep) {
        auth.realm().requireManageRealm();

        AuthenticationFlowModel flow = realm.getFlowByAlias(flowAlias);
        if (flow == null) {
            logger.debug("flow not found: " + flowAlias);
            throw new NotFoundException("flow not found");
        }

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(rep.getId());
        if (model == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        if (!model.getRequirement().name().equals(rep.getRequirement())) {
            model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));
            realm.updateAuthenticatorExecution(model);
            adminEvent.operation(OperationType.UPDATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).representation(rep).success();
            return Response.accepted(flow).build();
        }

        //executions can't have name and description updated
        if (rep.getAuthenticationFlow() == null) {
            return Response.accepted(flow).build();
        }

        //check if updating a correct flow
        AuthenticationFlowModel checkFlow = realm.getAuthenticationFlowById(rep.getFlowId());
        if (checkFlow == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");
        }

        //if a different flow with the same name does already exist, throw an exception
        if (realm.getFlowByAlias(rep.getDisplayName()) != null && !checkFlow.getAlias().equals(rep.getDisplayName())) {
            return ErrorResponse.exists("Flow alias name already exists");
        }

        //if the name changed
        if (!checkFlow.getAlias().equals(rep.getDisplayName())) {
            checkFlow.setAlias(rep.getDisplayName());
        }

        //check if the description changed
        if (!checkFlow.getDescription().equals(rep.getDescription())) {
            checkFlow.setDescription(rep.getDescription());
        }

        //update the flow
        realm.updateAuthenticationFlow(checkFlow);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).representation(rep).success();
        return Response.accepted(flow).build();
    }

    /**
     * Get Single Execution
     */
    @Path("/executions/{executionId}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExecution(final @PathParam("executionId") String executionId) {
        //http://localhost:8080/auth/admin/realms/master/authentication/executions/cf26211b-9e68-4788-b754-1afd02e59d7f
        auth.realm().requireManageRealm();

        final Optional<AuthenticationExecutionModel> model = Optional.ofNullable(realm.getAuthenticationExecutionById(executionId));
        if (!model.isPresent()) {
            logger.debugv("Could not find execution by Id: {}", executionId);
            throw new NotFoundException("Illegal execution");
        }

        return Response.ok(model.get()).build();
    }

    /**
     * Add new authentication execution
     *
     * @param execution JSON model describing authentication execution
     */
    @Path("/executions")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addExecution(AuthenticationExecutionRepresentation execution) {
        auth.realm().requireManageRealm();

        AuthenticationExecutionModel model = RepresentationToModel.toModel(realm, execution);
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to add execution to a built in flow");
        }
        model.setPriority(getNextPriority(parentFlow));
        model = realm.addAuthenticatorExecution(model);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri(), model.getId()).representation(execution).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
    }

    public AuthenticationFlowModel getParentFlow(AuthenticationExecutionModel model) {
        if (model.getParentFlow() == null) {
            throw new BadRequestException("parent flow not set on new execution");
        }
        AuthenticationFlowModel parentFlow = realm.getAuthenticationFlowById(model.getParentFlow());
        if (parentFlow == null) {
            throw new BadRequestException("execution parent flow does not exist");

        }
        return parentFlow;
    }


    @Path("/executions/priority")
    @POST
    @NoCache
    public void updateExecutionPriority(List<AuthenticationExecutionPriorityRepresentation> representations) {
        auth.realm().requireManageRealm();

        if (representations == null || representations.isEmpty())
            return;

        representations.forEach(element -> {
            AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(element.getExecutionId());
            if (model != null) {
                model.setPriority(element.getPriority());
                realm.updateAuthenticatorExecution(model);
            }
        });
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).success();
    }


    /**
     * Delete execution
     *
     * @param execution Execution id
     */
    @Path("/executions/{executionId}")
    @DELETE
    @NoCache
    public void removeExecution(@PathParam("executionId") String execution) {
        auth.realm().requireManageRealm();

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");

        }
        AuthenticationFlowModel parentFlow = getParentFlow(model);
        if (parentFlow.isBuiltIn()) {
            throw new BadRequestException("It is illegal to remove execution from a built in flow");
        }

        if (model.getFlowId() != null) {
            AuthenticationFlowModel nonTopLevelFlow = realm.getAuthenticationFlowById(model.getFlowId());
            realm.removeAuthenticationFlow(nonTopLevelFlow, false);
        }

        realm.removeAuthenticatorExecution(model);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).representation(model).success();
    }


    /**
     * Update execution with new configuration
     *
     * @param execution Execution id
     * @param json      JSON with new configuration
     * @return
     */
    @Path("/executions/{executionId}/config")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newExecutionConfig(@PathParam("executionId") String execution, AuthenticatorConfigRepresentation json) {
        auth.realm().requireManageRealm();
        if (json.getAlias() == null) {
            json.setAlias(UUID.randomUUID().toString());
        }

        ReservedCharValidator.validate(json.getAlias());

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            adminEvent.operation(OperationType.CREATE);
            session.getTransactionManager().setRollbackOnly();
            throw new NotFoundException("Illegal execution");
        } else {
            adminEvent.operation(OperationType.UPDATE);
        }

        json.setAlias(model.getId());

        AuthenticatorConfigModel config = RepresentationToModel.toModel(json);
        if (config.getAlias() == null) {
            return ErrorResponse.error("Alias missing", BAD_REQUEST);
        }
        AuthenticatorFactory f = (AuthenticatorFactory) session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, model.getAuthenticator());

        List<String> validations = validateAuthConfig(f.getConfigProperties(session), config.getConfig());
        if (!validations.isEmpty()) {
            return ErrorResponse.error(String.join("\n", validations), BAD_REQUEST);
        }

        config = realm.addAuthenticatorConfig(config);
        model.setAuthenticatorConfig(config.getId());
        realm.updateAuthenticatorExecution(model);

        json.setId(config.getId());
        adminEvent.resource(ResourceType.AUTH_EXECUTION).resourcePath(session.getContext().getUri()).representation(json).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    private List<String> validateAuthConfig(List<ProviderConfigProperty> props, Map<String, String> configToSet) {
        List<String> configTestResponses = new ArrayList<>();
        if (configToSet != null && !configToSet.isEmpty()) {
            props.forEach(p -> {
                //required field check
                if (p.isRequired()) {
                    Boolean dependsOn = null;
                    if (p.getDependsOn() != null && !p.getDependsOn().isEmpty()) {
                        dependsOn = getDependsOnConfig(p.getDependsOn(), configToSet);
                    }
                    if (dependsOn == null || dependsOn) {
                        if (!(configToSet.containsKey(p.getName())) || configToSet.get(p.getName()).isEmpty()) {
                            configTestResponses.add(p.getLabel() + " is a required field");
                        }
                    }
                }
                //Min Value of field check
                String configVal = configToSet.get(p.getName());
                if (configVal != null && isParsableInt(configVal)
                        && Integer.parseInt(configVal) < p.getMinVal()) {
                    configTestResponses.add("Minimum Value for " + p.getLabel() + " is " + p.getMinVal());
                }
            });
        }
        return configTestResponses;
    }

    private boolean isParsableInt(String val) {
        try {
            Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static boolean getDependsOnConfig(Map<String, Object> dependsOnList, Map<String, String> configuredSettings) {
        for (Map.Entry<String, Object> x : dependsOnList.entrySet()) {
            if (String.valueOf(configuredSettings.get(x.getKey())).equalsIgnoreCase(String.valueOf(x.getValue()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get execution's configuration
     *
     * @param execution Execution id
     * @param id        Configuration id
     * @deprecated Use rather {@link #getAuthenticatorConfig(String)}
     */
    @Path("/executions/{executionId}/config/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigRepresentation getAuthenticatorConfig(@PathParam("executionId") String execution, @PathParam("id") String id) {
        auth.realm().requireViewRealm();

        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");
        }
        return ModelToRepresentation.toRepresentation(config);
    }

    @Path("actions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<ActionRepresentation> getActions() {
        auth.realm().requireViewRequiredActions();

        return realm.getActionStream()
                .filter(actionModel -> !actionModel.isInBuilt())
                .filter(actionModel -> !actionModel.getIsSubWorkflow())
                .filter(actionModel -> {
                    if (realm.getMode().equals("bo")) {
                        return actionModel.getActionType() != null && !actionModel.getActionType().equals("neutral");
                    }
                    return true;
                })
                .sorted(Comparator.comparing(ActionModel::getModifiedTimestamp).reversed())
                .map(actionModel -> toRepresentation(realm, actionModel));
    }

    @Path("actions/{actionId}/action-variables")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ActionRepresentation updateActionVariables(@PathParam("actionId") String actionId, Map<String, String> data) {
        auth.realm().requireViewRequiredActions();
        logger.info("AuthenticationManagementResource:updateActionVariables");

        Optional<ActionModel> optionalActionModel = realm.getActionStream().filter(x -> x.getId().equalsIgnoreCase(actionId)).findAny();

        if (optionalActionModel.isEmpty()) {
            throw new BadRequestException("Flow not found");
        }

        ActionModel actionModel = optionalActionModel.get();
        if (data != null && data.containsKey("actionVariables")) {
            actionModel.setActionVariables(data.get("actionVariables"));
        } else
            return null;
        actionModel.setModifiedTimestamp(System.currentTimeMillis());

        realm.updateAction(actionModel);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).representation(data).success();
        return toRepresentation(realm, actionModel);
    }

    @Path("/actions/priority")
    @POST
    @NoCache
    public void updateActionsPriority(List<RequiredActionPriorityRepresentation> representations) {
        auth.realm().requireManageRealm();

        if (representations == null || representations.isEmpty())
            return;

        representations.forEach(element -> {
            RequiredActionProviderModel model = realm.getRequiredActionProviderById(element.getActionId());

            if (model != null) {
                model.setPriority(element.getPriority());
                model.setDirection(element.getDirection());
                model.setValidatorStepId(element.getValidatorStepId());
                model.setParentId(element.getParentId());
                realm.updateRequiredActionProvider(model);
            }

        });
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).success();
    }

    @Path("actions/{actionid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ActionRepresentation getAction(@PathParam("actionid") String actionId) {
        auth.realm().requireViewRequiredActions();

        ActionModel actionModel = realm.getActionById(actionId);
        if (actionModel == null) {
            return null;
        }
        return toRepresentation(realm, actionModel);
    }

    @Path("actions")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public Response addAction(Map<String, String> data) {
        auth.realm().requireManageRealm();

        String name = data.get("alias");

        //ActionFlowName Null/Empty Check
        if (name == null || name.isEmpty()) {
            return ErrorResponse.exists("Failed to create Policy with empty Action name");
        }
        if (GeneralHelper.validationForHtmlInjection(name)) {
            adminEvent.operation(OperationType.CREATE_FAILED).resource(ResourceType.ACTION).representation(data).error(HTML_INJECTED_IN_FIELDS).success();
            return ErrorResponse.error(INVALID_ACTION_NAME, BAD_REQUEST);
        }

        String actionType = data.get("actionType");
        if (actionType == null || actionType.isEmpty())
            actionType = "neutral";

        //Existing ActionFlowName Check
        String finalActionType = actionType;
        ActionModel foundAction = realm.getActionStream()
                .filter(actionModel -> {
                    if (realm.getMode().equals("bo")) {
                        return actionModel.getActionType() != null && !actionModel.getActionType().equals("neutral");
                    }
                    return true;
                })
                .filter(action -> action.getAlias().equalsIgnoreCase(name) && action.getActionType().equals(finalActionType))
                .findFirst()
                .orElse(null);
        if (foundAction != null) {
            return ErrorResponse.exists(properties.getProperty((realm.getMode().equalsIgnoreCase("bo") ? (Boolean.parseBoolean(data.get("isSubWorkflow")) ? "subworkflow" : "workflow") : "action") + "-already-exists").replace("{{0}}", name));
        }

        ActionModel requiredAction = new ActionModel();
        requiredAction.setAlias(name);
        requiredAction.setActionType(finalActionType);
        requiredAction.setIsSubWorkflow(Boolean.parseBoolean(data.get("isSubWorkflow")));
        requiredAction = realm.addAction(requiredAction);

        data.put("id", requiredAction.getId());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.ACTION).resourcePath(session.getContext().getUri()).representation(data).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(requiredAction.getId()).build()).entity(data).build();
    }

    @Path("actions/{actionid}")
    @DELETE
    public Response removeAction(@PathParam("actionid") String actionId) {
        auth.realm().requireManageRealm();

        if (realm.getRequiredActionProvidersStream().filter(action ->
                action.getProviderId().equals("bo-sub-workflow") &&
                        action.getConfig() != null &&
                        action.getConfig().get("sub-workflow") != null &&
                        action.getConfig().get("sub-workflow").equals(actionId)
        ).count() != 0) {
            return ErrorResponse.exists(properties.getProperty("sub-workflow-cannot-deleted"));
        }

        ActionModel model = realm.getActionById(actionId);
        if (model == null) {
            throw new NotFoundException("Failed to find action.");
        }

        if (model.isInBuilt()) {
            return ErrorResponse.error("Cannot remove inbuilt action", Response.Status.CONFLICT);
        }

        if (checkIfFlowIsLive(realm, model.getId())) {
            return ErrorResponse.exists(realm.getMode().equalsIgnoreCase("bo") ? properties.getProperty("workflow-cannot-deleted") : properties.getProperty("action-cannot-deleted"));
        }

        realm.removeRequiredActionProviders(model.getId());
        realm.removeAction(model);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.ACTION).resourcePath(session.getContext().getUri()).success();
        return Response.ok(true).build();
    }

    /**
     * Get unregistered required actions
     * <p>
     * Returns a list of unregistered required actions.
     */
    @Path("mfa-required-actions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<Map<String, String>> getMFARequiredActions() {
        auth.realm().requireViewRealm();

        return session.getKeycloakSessionFactory().getProviderFactories(RequiredActionProvider.class)
                .stream()
                .filter(factory -> ((RequiredActionFactory) factory).requiredActionType() == RequiredActionEnum.MFA)
                .map(factory -> {
                    RequiredActionFactory r = (RequiredActionFactory) factory;
                    Map<String, String> m = new HashMap<>();
                    m.put("name", r.getDisplayText());
                    m.put("providerId", r.getId());
                    return m;
                });
    }

    /**
     * Get unregistered required actions
     * <p>
     * Returns a list of unregistered required actions.
     */
    @Path("unregistered-required-actions/{actionid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<Map<String, String>> getUnregisteredRequiredActions(@PathParam("actionid") String actionId) {
        auth.realm().requireViewRealm();

        ActionModel actionModel = realm.getActionById(actionId);
        return session.getKeycloakSessionFactory().getProviderFactories(RequiredActionProvider.class)
                .stream()
                .filter(factory -> ((RequiredActionFactory) factory).requiredActionType() == RequiredActionEnum.ACTION)
                .filter(factory -> Arrays.asList(((RequiredActionFactory) factory).supportedModes()).contains(realm.getMode()))
                .filter(factory -> Arrays.asList(((RequiredActionFactory) factory).supportedRegions()).contains(realm.getDefaultRegion()) || Arrays.asList(((RequiredActionFactory) factory).supportedRegions()).contains("all"))
                .filter(x -> {
                    RequiredActionFactory r = (RequiredActionFactory) x;
                    return !"bo-custom-message".equals(r.getId()) || actionModel == null || !actionModel.getIsSubWorkflow();
                })
                .map(factory -> {
                    RequiredActionFactory r = (RequiredActionFactory) factory;
                    Map<String, String> m = new HashMap<>();
                    m.put("name", r.getDisplayText());
                    m.put("providerId", r.getId());
                    m.put("isBeta", String.valueOf(r.isBeta()));
                    m.put("isNew", String.valueOf(r.isNew()));
                    m.put("isValidator", String.valueOf(r.isValidator()));
                    return m;

                });
    }


    @Path("sub-workflows")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<ActionRepresentation> getSubWorkflows() {
        auth.realm().requireViewRealm();

        return realm.getActionStream()
                .filter(actionModel -> !actionModel.isInBuilt())
                .filter(actionModel -> actionModel.getIsSubWorkflow())
                .filter(actionModel -> {
                    if (realm.getMode().equals("bo")) {
                        return actionModel.getActionType() != null && !actionModel.getActionType().equals("neutral");
                    }
                    return true;
                })
                .map(actionModel -> toRepresentation(realm, actionModel));
    }

    /**
     * Register a new required actions
     *
     * @param data JSON containing 'providerId', and 'name' attributes.
     */
    @Path("register-required-action/{actionid}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void registerRequiredAction(@PathParam("actionid") String actionId, Map<String, String> data) {
        auth.realm().requireManageRealm();

        String providerId = data.get("providerId");
        String baseName = data.get("name");
        String name = baseName;
        String policyId = data.get("policyId");
        String policyType = data.get("policyType");
        if (policyType == null) {
            policyType = realm.getMode().equals("bo") ? "ciba" : "action";
        }
        ActionModel action = realm.getActionById(actionId);
        if (action.getIsSubWorkflow() && "bo-custom-message".equals(providerId)) {
            throw new BadRequestException("cannot-add-verifier-to-sub-flow");
        }
        List<String> verifierNames = realm.getRequiredActionProvidersStream(actionId).map(RequiredActionProviderModel::getName).collect(Collectors.toList());
        long existingVerifiersWithSameName = verifierNames.stream().filter(x -> x.split("(?=\\d+$)")[0].trim().equals(baseName)).count();

        if (existingVerifiersWithSameName > 0) {
            int suffix = (int) existingVerifiersWithSameName;
            String newName;
            newName = baseName + " " + suffix;
            if (verifierNames.contains(newName)) {
                do {
                    suffix++;
                    newName = baseName + " " + suffix;
                } while (verifierNames.contains(newName));
            }

            name = newName;
        }


        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setAlias(providerId);
        requiredAction.setName(name.trim());
        requiredAction.setProviderId(providerId);
        requiredAction.setDefaultAction(false);
        requiredAction.setPolicyId(policyId);
        requiredAction.setPolicyType(policyType);
        requiredAction.setDirection(data.get("direction"));
        requiredAction.setParentId(data.get("parentId"));
        requiredAction.setValidatorStepId(data.get("validatorStepId"));
        requiredAction.setId(data.get("id") != null ? data.get("id") : UUID.randomUUID().toString());
        requiredAction.setPriority(data.get("priority") != null ? Integer.parseInt(data.get("priority")) : getNextRequiredActionPriority(actionId));
        requiredAction.setEnabled(data.get("enabled") != null ? Boolean.parseBoolean(data.get("enabled")) : true);
        requiredAction.setSkippable(data.get("skippable") != null ? Boolean.parseBoolean(data.get("skippable")) : false);
        requiredAction.setValidator(Boolean.parseBoolean(data.get("isValidator")));
        requiredAction.setActionId(actionId);
        Map<String, String> config = new HashMap<>();
        try {
            config = Arrays.stream(data.get("config").split(","))
                    .map(s -> s.split("="))
                    .filter(arr -> arr.length == 2)
                    .collect(Collectors.toMap(
                            key -> key[0].replace("{", "").replace(" ", ""),
                            value -> value[1].replace("}", "")
                    ));
        } catch (Exception e) {
            logger.error("Register Required Action :: Failed to read config");
        }
        requiredAction.setConfig(config);
        requiredAction = realm.addRequiredActionProvider(requiredAction);

        if (!config.isEmpty() && requiredAction.getProviderId().equals("terms-and-conditions")) {
            RequiredActionProviderRepresentation representation = toRepresentation(session, requiredAction);
            GeneralHelper.updateTermsAndConditions(session, realm, representation, action.getAlias(), adminEvent);
        }

        if (data.get("childId") != null) {
            RequiredActionProviderModel child = realm.getRequiredActionProviderById(data.get("childId"));
            child.setParentId(requiredAction.getId());
            if (requiredAction.isValidator()) {
                child.setDirection("left");
                child.setValidatorStepId(requiredAction.getId());
                realm.updateRequiredActionProvider(child);
                updateChildActions(child, requiredAction);
            } else {
                realm.updateRequiredActionProvider(child);
            }
        }

        //to Update the Modified Timestamp of Each Action
        action.setModifiedTimestamp(System.currentTimeMillis());
        realm.updateAction(action);

        data.put("id", requiredAction.getId());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).representation(data).success();

    }

    @Path("register-required-actions/{actionid}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void registerRequiredActions(@PathParam("actionid") String actionId, List<Map<String, Object>> data) {
        auth.realm().requireManageRealm();
        if (!data.isEmpty()) {
            Map<String, String> idSwap = new HashMap<>();
            Map<String, String> parentIds = new HashMap<>();
            data.forEach(verification -> {
                boolean isDelete = verification.get("isDelete") != null &&
                        Boolean.parseBoolean(verification.get("isDelete").toString());
                if (isDelete) {
                    parentIds.put(verification.get("id").toString(), verification.get("parentId").toString());
                    return;
                }

                String parentId = parentIds.get(verification.get("parentId").toString());
                if (parentId != null) {
                    verification.put("parentId", parentId);
                }
                swapIds(verification, idSwap, true);
                verification.put("config", createValidatorPolicyIfNeeded(verification, idSwap));
                Map<String, String> verificationData = verification.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> String.valueOf(e.getValue())
                        ));
                registerRequiredAction(actionId, verificationData);
            });
        }
    }

    @Path("update-required-actions/{actionid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void updateRequiredActions(@PathParam("actionid") String actionId, List<Map<String, Object>> data) {
        auth.realm().requireManageRealm();
        if (!data.isEmpty()) {
            Map<String, String> idSwap = new HashMap<>();
            Map<String, String> parentIds = new HashMap<>();
            data.forEach(verification -> {
                boolean isDelete = verification.get("isDelete") != null && Boolean.parseBoolean(verification.get("isDelete").toString());
                RequiredActionProviderModel existingVerification = realm.getRequiredActionProviderById(verification.get("id").toString());
                if (existingVerification == null && isDelete) {
                    parentIds.put(verification.get("id").toString(), verification.get("parentId").toString());
                    return;
                }
                swapIds(verification, idSwap, true);
                if (existingVerification == null) {
                    String parentId = parentIds.get(verification.get("id").toString());
                    if (parentId != null) {
                        verification.put("parentId", parentId);
                    }
                    verification.put("config", createValidatorPolicyIfNeeded(verification, idSwap));
                    Map<String, String> verificationData = verification.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> String.valueOf(e.getValue())
                            ));
                    registerRequiredAction(actionId, verificationData);
                } else {
                    if (isDelete) {
                        removeRequiredAction(existingVerification.getId());
                        data.forEach(action -> {
                            if (existingVerification.getId().equals(action.get("parentId").toString())) {
                                action.put("parentId", existingVerification.getParentId());
                            }
                        });
                    } else {
                        updateWorkflow(actionId, existingVerification, verification);
                    }
                }
            });
        }
    }

    public void updateWorkflow(String actionId, RequiredActionProviderModel verification, Map<String, Object> updatedData) {
        verification.setName(updatedData.get("name").toString());

        Map<String, String> config = new HashMap<>();
        try {
            config = JsonSerialization.convertValue(updatedData.get("config"), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            logger.error("Update Required Action :: Failed to read config");
        }
        if (verification.isValidator()) {
            RealmPolicyProvider provider = session.getProvider(RealmPolicyProvider.class);
            String configValue = config.get("validation-condition-prompt") != null ? config.get("validation-condition-prompt") : "";

            if (config.get("validation-policy-id") == null || "".equals(config.get("validation-policy-id"))) {
                PolicyModel model = new PolicyModel();
                model.setInBuilt(false);
                model.setPolicyType("validatorPolicy");
                model.setPolicyName(UUID.randomUUID().toString());
                model.setRealmPolicy(false);
                model.setPolicyValue(configValue);
                PolicyModel policyModel = provider.addPolicy(realm, model);
                config.put("validation-policy-id", policyModel.getId());
            } else {
                RealmPolicyModel policyModel = provider.getPolicyById(realm, config.get("validation-policy-id"));
                provider.updatePolicy(realm, policyModel.getId(), policyModel.getPolicyName(),
                        configValue, policyModel.isRealmPolicy(), policyModel.getPolicyType(), policyModel.inBuilt());
            }
        }
        verification.setConfig(config);
        verification.setDirection(updatedData.get("direction").toString());
        verification.setParentId(updatedData.get("parentId").toString());
        verification.setValidatorStepId(updatedData.get("validatorStepId").toString());
        verification.setPriority(Integer.parseInt(updatedData.get("priority").toString()));
        verification.setEnabled(updatedData.get("enabled") == null || Boolean.parseBoolean(updatedData.get("enabled").toString()));
        verification.setSkippable(updatedData.get("skippable") != null && Boolean.parseBoolean(updatedData.get("skippable").toString()));
        realm.updateRequiredActionProvider(verification);
        ActionModel action = realm.getActionById(actionId);

        //to Update the Modified Timestamp of Each Action
        action.setModifiedTimestamp(System.currentTimeMillis());
        realm.updateAction(action);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).representation(verification).success();
    }

    @Path("register-required-action/import")
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.MULTIPART_FORM_DATA})
    @NoCache
    public Response importWorkflow(@QueryParam("alias") String alias, @QueryParam("isSubWorkflow") String isSubWorkflow, Map<String, Object> request) {
        auth.realm().requireManageRealm();
        logger.info("AuthenticationManagementResource:importWorkflow");

        String actionId;
        try {
            // create an action model with the name provided
            actionId = createAction(alias, false, isSubWorkflow);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            return ErrorResponse.error("html-fields-injected", BAD_REQUEST);
        }

        if (actionId == null) {
            logger.error("failed to create action");
            return ErrorResponse.error("action-name-exists", BAD_REQUEST);
        }

        // add the action variables to the created action
        List<Map<String, Object>> dataList = updateActionVariableValues(request, actionId);

        List<Map<String, Object>> customMessaageAction = dataList.stream().filter(x -> x.get("providerId").equals("bo-custom-message")).collect(Collectors.toList());
        if (!customMessaageAction.isEmpty() && Boolean.parseBoolean(isSubWorkflow)) {
            customMessaageAction.stream().forEach(customMessage -> dataList.remove(customMessage));
        }
        if (!customMessaageAction.isEmpty() && !Boolean.parseBoolean(isSubWorkflow)) {
            customMessaageAction.stream().forEach(x -> {
                Map<String, String> customMessageconfig = (Map<String, String>) x.get("config");
                if (customMessageconfig != null) {
                    customMessageconfig.put("custom-message-logo", "");
                }
            });
        }

        if (dataList == null) {
            realm.removeAction(realm.getActionById(actionId));
            return ErrorResponse.error("invalid-json", BAD_REQUEST);
        }

        Map<String, String> idSwap = new HashMap<>();
        // Original execution ids present in the imported payload. Used to detect the root
        // execution whose parentId references the SOURCE flow (not an execution in this payload),
        // so it can be re-anchored to the newly created flow instead of left dangling.
        Set<String> importedNodeIds = dataList.stream()
                .map(d -> d.get("id") == null ? null : d.get("id").toString())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // loop around the data from the request to create required actions and link it to the created action
        dataList.forEach(data -> {
            // capture the parentId before swapIds rewrites it, to detect an orphan root below
            String originalParentId = data.get("parentId") == null ? null : data.get("parentId").toString();
            // swap the ids that's provided in the data with random UUID with maintaining the relation of the tree provided
            swapIds(data, idSwap, false);
            // Re-anchor the root execution: when its parentId references the SOURCE flow (an id not
            // present among the imported executions), swapIds would map it to a phantom UUID that no
            // node owns, leaving a dangling parent that breaks the workflow editor. Point it at the
            // newly created flow instead.
            if (originalParentId != null && !"After User Confirmation".equalsIgnoreCase(originalParentId)
                    && !importedNodeIds.contains(originalParentId)) {
                data.put("parentId", actionId);
            }

            // create a policy (validator policy) if needed
            HashMap<String, String> config = new HashMap<>();
            if (data.get("providerId") != null) {
                config = createValidatorPolicyIfNeeded(data, idSwap);
            }

            String providerId = data.get("providerId").toString().replace("bo-", "").replace("-", "_");
            HashMap<String, String> configCopy = config;
            List<String> claimsKey = config.keySet().stream().filter(x -> x.contains("claims")).collect(Collectors.toList());
            claimsKey.forEach(x -> {
                String updatedClaims = Arrays.stream(configCopy.get(x).split("##"))
                        .map(claim -> claim.contains(providerId) ? claim.replace(providerId, "").replace("@", "") : claim)
                        .collect(Collectors.joining("##"));

                configCopy.put(x, updatedClaims);
            });
            config = configCopy;

            // create required action and map it to the action
            RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
            requiredAction = createRequiredAction(data, requiredAction, idSwap, actionId, config);
            //to Update the Modified Timestamp of Each Action
            ActionModel action = realm.getActionById(actionId);
            action.setModifiedTimestamp(System.currentTimeMillis());
            realm.updateAction(action);

            //to update the terms and conditions versions if the flow exists in importing file/json
            if (requiredAction.getProviderId().equals("terms-and-conditions")) {
                RequiredActionProviderRepresentation representation = toRepresentation(session, requiredAction);
                GeneralHelper.updateTermsAndConditions(session, realm, representation, alias, adminEvent);
            }

            data.put("id", requiredAction.getId());
            adminEvent.operation(OperationType.CREATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).representation(data).success();
        });

        GeneralResponse<Object> response = new GeneralResponse<>();
        response.setStatus(200);
        response.setBody(actionId);
        return Response.ok(response).build();
    }

    private RequiredActionProviderModel createRequiredAction(Map<String, Object> data, RequiredActionProviderModel requiredAction, Map<String, String> idSwap, String actionId, HashMap<String, String> config) {
        String providerId = data.get("providerId").toString();
        String name = data.get("name").toString();
        String policyType = data.get("policyType").toString();
        if (policyType == null) {
            policyType = realm.getMode().equals("bo") ? "ciba" : "action";
        }
        requiredAction.setAlias(providerId);
        requiredAction.setName(name);
        requiredAction.setProviderId(providerId);
        requiredAction.setDefaultAction(false);
        requiredAction.setPolicyType(policyType);
        requiredAction.setId(data.get("id").toString());
        requiredAction.setDirection(data.get("direction").toString());
        requiredAction.setParentId(data.get("parentId").toString());
        requiredAction.setValidatorStepId(data.get("validatorStepId").toString());
        requiredAction.setPriority(data.get("priority") != null ? Integer.parseInt(data.get("priority").toString()) : getNextRequiredActionPriority(actionId));
        requiredAction.setEnabled(data.get("enabled") != null ? Boolean.parseBoolean(data.get("enabled").toString()) : true);
        requiredAction.setSkippable(data.get("skippable") != null ? Boolean.parseBoolean(data.get("skippable").toString()) : false);
        requiredAction.setValidator(Boolean.parseBoolean(data.get("validator").toString()));
        requiredAction.setActionId(actionId);
        requiredAction.setConfig(config);
        requiredAction = realm.addRequiredActionProvider(requiredAction);

        if (data.get("childId") != null) {
            RequiredActionProviderModel child = realm.getRequiredActionProviderById(data.get("childId").toString());
            child.setParentId(requiredAction.getId());
            if (requiredAction.isValidator()) {
                child.setDirection("left");
                child.setValidatorStepId(requiredAction.getId());
                realm.updateRequiredActionProvider(child);
                updateChildActions(child, requiredAction);
            } else {
                realm.updateRequiredActionProvider(child);
            }
        }
        return requiredAction;
    }

    public List<Map<String, Object>> updateActionVariableValues(Map<String, Object> request, String actionId) {
        logger.info("AuthenticationManagementResource:updateActionVariableValues");
        List<Map<String, Object>> dataList;
        HashMap<String, String> actionVariables;
        try {
            if (request.containsKey("file")) {
                String base64Json = request.get("file").toString().replace("data:application/json;base64,", "");
                String jsonString = new String(Base64.getDecoder().decode(base64Json));
                HashMap<String, Object> req = null;
                try {
                    req = JsonSerialization.readValue(jsonString, new TypeReference<HashMap<String, Object>>() {
                    });
                } catch (IOException e) {
                    return null;
                }
                actionVariables = req.containsKey("actionVariables") ? (HashMap<String, String>) req.get("actionVariables") : new HashMap<>();
                dataList = req.containsKey("data") ? (List<Map<String, Object>>) req.get("data") : new ArrayList<>();
            } else {
                actionVariables = request.containsKey("actionVariables") ? (HashMap<String, String>) request.get("actionVariables") : new HashMap<>();
                dataList = request.containsKey("data") ? (List<Map<String, Object>>) request.get("data") : new ArrayList<>();
            }
        } catch (ClassCastException e) {
            return null;
        }

        if (!actionVariables.isEmpty()) {
            try {
                String actionVariablesDetails = actionVariables.get("actionVariables");
                if (actionVariablesDetails != null)
                    actionVariables.put("actionVariables", JsonSerialization.writeValueAsPrettyString(JsonSerialization.readValue(actionVariablesDetails, Map.class)));
            } catch (IOException e) {
                logger.error("updateActionVariableValues :: Error while serializing action variables");
            }
        }

        if (!actionVariables.containsKey("actionVariables"))
            return dataList;
        if (updateActionVariables(actionId, actionVariables) == null) {
            return null;
        }
        return dataList;
    }

    @NotNull
    private HashMap<String, String> createValidatorPolicyIfNeeded(Map<String, Object> data, Map<String, String> idSwap) {
        logger.info("AuthenticationManagementResource:createValidatorPolicyIfNeeded");
        HashMap<String, String> config = (HashMap<String, String>) data.get("config");
        if (!config.isEmpty() && config.containsKey("validation-policy-id") && config.containsKey("validation-condition-prompt")) {
            if (idSwap.get(config.get("validation-policy-id")) == null) {
                RealmPolicyProvider provider = session.getProvider(RealmPolicyProvider.class);

                PolicyModel model = new PolicyModel();
                model.setInBuilt(false);
                model.setPolicyType("validatorPolicy");
                model.setPolicyName(UUID.randomUUID().toString());
                model.setRealmPolicy(false);
                model.setPolicyValue(config.get("validation-condition-prompt"));

                PolicyModel policyModel = provider.addPolicy(realm, model);
                config.put("validation-policy-id", policyModel.getId());

                idSwap.put(config.get("validation-policy-id"), policyModel.getId());
            } else {
                config.put("validation-policy-id", idSwap.get(config.get("validation-policy-id")));
            }
        }
        return config;
    }

    private void swapIds(Map<String, Object> data, Map<String, String> idSwap, boolean isMarketplace) {
        logger.info("AuthenticationManagementResource:swapIds");
        if (idSwap.get(data.get("id").toString()) == null) {
            idSwap.put(data.get("id").toString(), UUID.randomUUID().toString());
            data.put("id", idSwap.get(data.get("id").toString()));
        } else
            data.put("id", idSwap.get(data.get("id").toString()));

        if (data.containsKey("parentId") && !("After User Confirmation").equalsIgnoreCase(data.get("parentId").toString())
                && (!isMarketplace || realm.getRequiredActionProviderById(data.get("parentId").toString()) == null)) {
            if (idSwap.get(data.get("parentId").toString()) == null) {
                idSwap.put(data.get("parentId").toString(), UUID.randomUUID().toString());
                data.put("parentId", idSwap.get(data.get("parentId").toString()));
            } else
                data.put("parentId", idSwap.get(data.get("parentId").toString()));
        }
        if (data.containsKey("validatorStepId") && !data.get("validatorStepId").equals("none") &&
                (!isMarketplace || realm.getRequiredActionProviderById(data.get("validatorStepId").toString()) == null)) {
            if (idSwap.get(data.get("validatorStepId").toString()) == null) {
                idSwap.put(data.get("validatorStepId").toString(), UUID.randomUUID().toString());
                data.put("validatorStepId", idSwap.get(data.get("validatorStepId").toString()));
            } else
                data.put("validatorStepId", idSwap.get(data.get("validatorStepId").toString()));
        }
    }

    public String createAction(String alias, boolean isMarketplace, String isSubWorkflow) {
        logger.info("AuthenticationManagementResource:createAction");
        //ActionFlowName Null/Empty Check
        if (alias == null || alias.isEmpty()) {
            return null;
        }
        if (GeneralHelper.validationForHtmlInjection(alias)) {
            adminEvent.operation(OperationType.CREATE_FAILED).resource(ResourceType.ACTION).error(HTML_INJECTED_IN_FIELDS).success();
            throw new IllegalArgumentException(HTML_INJECTED_IN_FIELDS);
        }

        String actionType = "verification";

        //Existing ActionFlowName Check
        ActionModel foundAction = realm.getActionStream()
                .filter(actionModel -> {
                    if (realm.getMode().equals("bo")) {
                        return actionModel.getActionType() != null && !actionModel.getActionType().equals("neutral");
                    }
                    return true;
                })
                .filter(action -> action.getAlias().equalsIgnoreCase(alias) && action.getActionType().equals(actionType))
                .findFirst()
                .orElse(null);
        if (foundAction != null) {
            return null;
        }

        ActionModel requiredAction = new ActionModel();
        requiredAction.setAlias(alias);
        requiredAction.setIsSubWorkflow(Boolean.parseBoolean(isSubWorkflow));
        requiredAction.setActionType(actionType);
        requiredAction.setMarketPlace(isMarketplace);
        requiredAction = realm.addAction(requiredAction);
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.ACTION).resourcePath(session.getContext().getUri()).success();

        return requiredAction.getId();
    }

    public void updateChildActions(RequiredActionProviderModel parent, RequiredActionProviderModel validator) {
        List<RequiredActionProviderModel> child = realm.getRequiredActionProvidersStream().
                filter(action -> parent.getId().equals(action.getParentId())).collect(Collectors.toList());

        if (!child.isEmpty() && child.get(0) != null) {
            child.get(0).setDirection("left");
            child.get(0).setValidatorStepId(validator.getId());
            realm.updateRequiredActionProvider(child.get(0));
            if (child.get(0).isValidator()) {
                return;
            } else {
                updateChildActions(child.get(0), validator);
            }
        }
    }

    /**
     * Register a new required actions
     *
     * @param id JSON containing 'providerId', and 'name' attributes.
     */
    @Path("register-required-actions/validator/raise-priority/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void raiseChildPriority(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        RequiredActionProviderModel node = realm.getRequiredActionProviderById(id);

        if (node == null) {
            throw new NotFoundException("Could not find Action provider");
        }

        realm.getRequiredActionProvidersStream().forEach(element -> {
            if (node.getPriority() <= element.getPriority()) {
                element.setPriority(element.getPriority() + 1);
                realm.updateRequiredActionProvider(element);
            }
        });
    }

    private int getNextRequiredActionPriority(String actionId) {
        List<RequiredActionProviderModel> actions = realm.getRequiredActionProvidersStream(actionId).collect(Collectors.toList());
        return actions.isEmpty() ? 0 : actions.get(actions.size() - 1).getPriority() + 1;
    }


    /**
     * Get required actions
     * <p>
     * Returns a stream of required actions.
     */
    @Path("required-actions/{actionid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<RequiredActionProviderRepresentation> getRequiredActions(@PathParam("actionid") String actionId) {
        auth.realm().requireViewRequiredActions();

        if (realm.getActionById(actionId) == null)
            return null;
        List<RequiredActionProviderModel> actions = realm.getRequiredActionProvidersStream(actionId).collect(Collectors.toList());
        triggerOnDemandMigration(realm, actions);

        return actions.stream().map(x -> toRepresentation(session, x));
    }

    /**
     * Migration to set parent I'd for the previous version's that not support validators
     *
     * @param actions action list
     */
    private static void triggerOnDemandMigration(RealmModel realm, List<RequiredActionProviderModel> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        AtomicReference<String> initialParentId = new AtomicReference<>("none");
        actions.forEach(action -> {
            if (action.getParentId() == null || action.getParentId().equals("none")) {
                action.setParentId(initialParentId.get());
                realm.updateRequiredActionProvider(action);
            }
            initialParentId.set(action.getId());
        });
    }

    public static RequiredActionProviderRepresentation toRepresentation(KeycloakSession session, RequiredActionProviderModel model) {
        RequiredActionProviderRepresentation rep = new RequiredActionProviderRepresentation();
        if (rep.getConfig() != null) {
            rep.getConfig().forEach((key, value) -> {
                if (rep.getConfig().containsKey(key) && GeneralHelper.validationForHtmlInjection(rep.getConfig().get(key))) {
                    throw new BadRequestException(INVALID_CONFIG_INPUT);
                }
            });
        }
        rep.setAlias(model.getAlias());
        rep.setProviderId(model.getProviderId());
        rep.setName(model.getName());
        rep.setDescription(model.getDescription());
        rep.setDefaultAction(model.isDefaultAction());
        rep.setPriority(model.getPriority());
        rep.setEnabled(model.isEnabled());
        rep.setSkippable(model.isSkippable());
        rep.setValidator(model.isValidator());
        rep.setConfig(model.getConfig());
        rep.setParentId(model.getParentId() != null ? model.getParentId() : "none");
        rep.setDirection(model.getDirection() != null ? model.getDirection() : "none");
        rep.setValidatorStepId(model.getValidatorStepId() != null ? model.getValidatorStepId() : "none");
        rep.setPolicyId(model.getPolicyId());
        rep.setId(model.getId());
        rep.setPasswordPolicy(model.getPasswordPolicy());
        rep.setPolicyId(model.getPolicyId());
        rep.setPolicyType(model.getPolicyType());

        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, model.getProviderId());
        if (factory == null) {
            logger.error("Action provider not found: " + model.getProviderId());
            throw new NotFoundException("Could not find Action provider");
        }
        rep.setConfigurable(factory.isConfigurable());
        rep.setHasPolicy(factory.hasPolicy());
        return rep;
    }

    public static ActionRepresentation toRepresentation(RealmModel realm, ActionModel model) {
        ActionRepresentation rep = new ActionRepresentation();
        rep.setAlias(model.getAlias());
        rep.setActionId(model.getId());
        rep.setLastUpdatedTimestamp(model.getModifiedTimestamp());
        rep.setLive(checkIfFlowIsLive(realm, model.getId()));
        rep.setInBuilt(model.isInBuilt());
        rep.setActionType(model.getActionType());
        rep.setActionVariables(model.getActionVariables());
        rep.setSubWorkflow(model.getIsSubWorkflow());
        rep.setMarketplace(model.getMarketPlace());
        return rep;
    }

    private static boolean checkIfFlowIsLive(RealmModel realm, String flowId) {
        return realm.getClientsStream()
                .map(ClientModel::getAuthenticationFlowBindingOverrides)
                .filter(client ->
                        realm.getMode().equalsIgnoreCase("bo") ?
                                Objects.equals(client.get("verification-action"), flowId) || Objects.equals(client.get("onboarding-action"), flowId) :
                                Objects.equals(client.get("action"), flowId) || Objects.equals(client.get("verification-action"), flowId) || Objects.equals(client.get("onboarding-action"), flowId))
                .count() > 0 || realm.getAuthenticationFlowBindingOverrides().containsValue(flowId);
    }

    /**
     * Get required action for alias
     *
     * @param alias Alias of required action
     */
    @Path("required-action/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public RequiredActionProviderRepresentation getRequiredAction(@PathParam("id") String alias) {
        auth.realm().requireViewRealm();

        RequiredActionProviderModel model = realm.getRequiredActionProviderById(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action");
        }
        RequiredActionProviderRepresentation rep = toRepresentation(session, model);
        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, rep.getProviderId());

        List<String> availableClaims = rep.getConfig().keySet().stream()
                .filter(x -> factory.isClaimsCustomizable() && x.contains("claims")).collect(Collectors.toList());

        String result = availableClaims.stream()
                .flatMap(x -> Arrays.stream(rep.getConfig().get(x).split("##")))
                .collect(Collectors.joining("##"));

        if (availableClaims.size() > 0) {
            rep.getConfig().remove(availableClaims.get(0));
            rep.getConfig().put(availableClaims.get(0), result);
        }

        return rep;
    }


    /**
     * Update required action
     *
     * @param alias Alias of required action
     * @param rep   JSON describing new state of required action
     */
    @Path("required-action/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRequiredAction(@PathParam("id") String alias, RequiredActionProviderRepresentation rep) {
        auth.realm().requireManageRealm();

        RequiredActionProviderModel model = realm.getRequiredActionProviderById(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action");
        }

        List<RequiredActionProviderModel> verifiers = realm.getRequiredActionProvidersStream(model.getActionId()).collect(Collectors.toList());
        if (verifiers.stream().anyMatch(x -> x.getName().equals(rep.getName().trim()) && !x.getId().equals(rep.getId()))) {
            throw new BadRequestException(WORKFLOW_NAME_EXISTS);
        }

        if (rep.getConfig() != null) {
            rep.getConfig().forEach((key, value) -> {
                if (rep.getConfig().containsKey(key) && GeneralHelper.validationForHtmlInjection(rep.getAlias(), rep.getConfig().get(key))) {
                    adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.REQUIRED_ACTION).representation(rep).error(HTML_INJECTED_IN_FIELDS).success();
                    throw new BadRequestException(INVALID_CONFIG_INPUT);
                }
            });
        }

        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, rep.getProviderId());
        try {
            factory.onUpdate(rep);
        } catch (IllegalArgumentException e) {
            adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).representation(rep).error(e.getMessage()).success();
            throw new BadRequestException(INVALID_CONFIG_INPUT);
        } catch (Exception e) {
            adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).representation(rep).success();
            throw new BadRequestException(INVALID_CONFIG_INPUT);
        }

        RequiredActionProviderModel update = new RequiredActionProviderModel();
        update.setId(model.getId());
        update.setName(rep.getName());
        update.setDescription(rep.getDescription());
        update.setAlias(rep.getAlias());
        update.setPolicyId(rep.getPolicyId());
        update.setPolicyType(rep.getPolicyType());
        update.setProviderId(model.getProviderId());
        update.setDefaultAction(rep.isDefaultAction());
        update.setPriority(rep.getPriority());
        update.setValidator(rep.isValidator());
        update.setValidatorStepId(rep.getValidatorStepId() != null ? rep.getValidatorStepId() : "none");
        update.setParentId(rep.getParentId() != null ? rep.getParentId() : "none");
        update.setDirection(rep.getDirection() != null ? rep.getDirection() : "none");
        update.setSkippable(rep.isSkippable());

        List<String> availableClaims = rep.getConfig().keySet().stream()
                .filter(x -> factory.isClaimsCustomizable() && (x.contains("claims") || x.contains("details"))).collect(Collectors.toList());

        String result = availableClaims.stream()
                .flatMap(x -> Arrays.stream(rep.getConfig().get(x).split("##")))
                .collect(Collectors.joining("##"));

        if (availableClaims.size() > 0) {
            rep.getConfig().remove(availableClaims.get(0));
            rep.getConfig().put(availableClaims.get(0), result);
        }

        update.setActionId(model.getActionId());
        update.setConfig(rep.getConfig());
        if (rep.getPasswordPolicy() != null)
            update.setPasswordPolicy(rep.getPasswordPolicy());

        // Have to update in Version if it is Terms and Conditions
        if (rep.getProviderId().equals("terms-and-conditions") && rep.getConfig().get(TERMS_AND_CONDITIONS_TYPE) != null) {
            GeneralHelper.updateTermsAndConditions(session, realm, rep, alias, adminEvent);
        }

        if (rep.getProviderId().equals("bo-custom-message")) {
            if (model.getConfig().isEmpty() || model.getConfig().get("custom-message-logo") != null && !model.getConfig().get("custom-message-logo").equals(rep.getConfig().get("custom-message-logo")))
                update.getConfig().put("custom-message-logo", GeneralHelper.updateCustomMessageLogo(session, rep, alias));
        }

        // perform all action's that are mapped against the validator if the validator is enabled or disabled
        if (model.isValidator() && model.isEnabled() != rep.isEnabled()) {
            updateValidatorAndAssociatedActions(realm, model.getActionId(), model.getId(), rep.isEnabled());
        }
        update.setEnabled(rep.isEnabled());
        realm.updateRequiredActionProvider(update);

        //to Update the Modified Timestamp of Each Action
        ActionModel action = realm.getActionById(model.getActionId());
        action.setModifiedTimestamp(System.currentTimeMillis());
        realm.updateAction(action);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).representation(rep).success();
    }

    private void updateValidatorAndAssociatedActions(RealmModel realm, String actionId, String validatorId, boolean enabled) {
        List<RequiredActionProviderModel> allActions = realm.getRequiredActionProvidersStream(actionId)
                .collect(Collectors.toList());

        Map<String, List<RequiredActionProviderModel>> actionsByValidatorId = allActions.stream()
                .collect(Collectors.groupingBy(a -> a.getValidatorStepId() != null ? a.getValidatorStepId() : ""));
        Map<String, List<RequiredActionProviderModel>> actionsByParentId = allActions.stream()
                .collect(Collectors.groupingBy(a -> a.getParentId() != null ? a.getParentId() : ""));

        Set<String> processedValidatorIds = new HashSet<>();
        List<RequiredActionProviderModel> actionsToUpdate = new ArrayList<>();

        updateActionRecursively(validatorId, enabled, actionsByValidatorId, actionsByParentId, processedValidatorIds, actionsToUpdate);

        actionsToUpdate.forEach(action -> {
            action.setEnabled(enabled);
            realm.updateRequiredActionProvider(action);
        });
    }

    private void updateActionRecursively(String currentId, boolean enabled,
                                         Map<String, List<RequiredActionProviderModel>> actionsByValidatorId,
                                         Map<String, List<RequiredActionProviderModel>> actionsByParentId,
                                         Set<String> processedIds,
                                         List<RequiredActionProviderModel> actionsToUpdate) {
        if (processedIds.contains(currentId)) {
            return;
        }
        processedIds.add(currentId);

        List<RequiredActionProviderModel> associatedActions = new ArrayList<>();
        associatedActions.addAll(actionsByValidatorId.getOrDefault(currentId, Collections.emptyList()));
        associatedActions.addAll(actionsByParentId.getOrDefault(currentId, Collections.emptyList()));

        for (RequiredActionProviderModel action : associatedActions) {
            if (!processedIds.contains(action.getId())) {
                actionsToUpdate.add(action);
                if (action.isValidator()) {
                    updateActionRecursively(action.getId(), enabled, actionsByValidatorId, actionsByParentId, processedIds, actionsToUpdate);
                }
            }
        }
    }

    @Path("required-action/{id}/{version}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getTermsandConditionByVersion(@PathParam("id") String alias, @PathParam("version") String version) {
        auth.realm().requireManageRealm();

        List<TermsAndConditionsModel> model = realm.getTermsAndConditionsByVersion(alias, version);
        Map<String, Object> termsAndCondition = new HashMap<>();

        if (model == null || model.size() == 0) {
            return Response.status(Response.Status.OK).entity(termsAndCondition).build();
        }

        if (model.get(0).getType().equals("Image") || model.get(0).getType().equals("PDF")) {
            String[] contentSplit = model.get(0).getContent().split("/");
            String minioFileNameName = contentSplit[contentSplit.length - 1];
            String fileName = minioFileNameName.split("\\+")[0];
            model.get(0).setContent(GeneralHelper.getPresignedObjectUrl(session.getContext(), model.get(0).getContent()));

            Long addedTimeStamp = Long.parseLong(minioFileNameName.split("\\+")[1].split("\\.")[0]);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy"); // Define date format
            String addedAt = sdf.format(new Date(addedTimeStamp));

            termsAndCondition.put("fileName", fileName);
            termsAndCondition.put("createdAt", addedAt);
        }
        termsAndCondition.put("termsAndConditions", model);

        return Response.status(Response.Status.OK).entity(termsAndCondition).build();
    }

    @Path("required-action/{id}/{version}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteTermsAndConditionByVersion(@PathParam("id") String alias, @PathParam("version") String version) {
        auth.realm().requireManageRealm();

        if (realm.getRequiredActionProviderById(alias).getConfig().get("terms_and_condition_ver") != null && realm.getRequiredActionProviderById(alias).getConfig().get("terms_and_condition_ver").equals(version)) {
            adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.REQUIRED_ACTION).error("terms-and-condtion-version-not-deleted").success();
            throw new BadRequestException("terms-and-condtion-version-not-deleted");
        }

        Map<String, String> response = new HashMap<>();
        List<TermsAndConditionsModel> termsAndConditionsVersion = realm.getTermsAndConditionsByVersion(alias, version);

        if (termsAndConditionsVersion.isEmpty()) {
            adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.REQUIRED_ACTION).error("terms-and-condition-version-not-found").success();
            throw new BadRequestException("terms-and-condition-version-not-found");
        }
        GeneralHelper.removeSecurityHeader(realm, session, termsAndConditionsVersion.get(0));

        realm.removeTermsAndConditionsVersionByVersion(alias, version);
        response.put("msg", "terms-condn-deleted-successfully");

        return Response.status(Response.Status.OK).entity(response).build();

    }

    @Path("required-action/{id}/versions")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getTermsAndConditions(@PathParam("id") String alias) {
        auth.realm().requireManageRealm();

        List<TermsAndConditionsModel> termsAndConditions = realm.getTermsAndConditionsVersions(alias);
        List<String> termsAndConditionsVersions = new ArrayList<>();

        termsAndConditions.stream().forEach(termsAndCondition -> {
            termsAndConditionsVersions.add(termsAndCondition.getVersion());
        });

        return Response.status(Response.Status.OK).entity(termsAndConditionsVersions).build();

    }

    /**
     * Delete required action
     *
     * @param alias Alias of required action
     */
    @Path("required-action/{id}")
    @DELETE
    public void removeRequiredAction(@PathParam("id") String alias) {
        auth.realm().requireManageRealm();


        RequiredActionProviderModel model = realm.getRequiredActionProviderById(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action.");
        }
        List<RequiredActionProviderModel> modelsToRemove = new ArrayList<>();
        processModelsRecursively(model, modelsToRemove);

        modelsToRemove.forEach(realm::removeRequiredActionProvider);

        // Update child action parentId
        List<RequiredActionProviderModel> childAction = realm.getRequiredActionProvidersStream().
                filter(action -> model.getId().equals(action.getParentId())).collect(Collectors.toList());

        if (!childAction.isEmpty()) {
            childAction.forEach(a -> {
                a.setParentId(model.getParentId());
                realm.updateRequiredActionProvider(a);
            });
        }

        // to Update the Modified Timestamp of Each Action
        ActionModel action = realm.getActionById(model.getActionId());
        if (action.isInBuilt()) {
            throw new ForbiddenException("Cannot remove inbuilt action");
        }
        action.setModifiedTimestamp(System.currentTimeMillis());
        realm.updateAction(action);

        if (model.getProviderId().equals("terms-and-conditions")) {
            List<TermsAndConditionsModel> termsAndConditionsVersions = realm.getTermsAndConditionsVersions(alias);
            termsAndConditionsVersions.stream().forEach(x -> {
                List<TermsAndConditionsModel> termsAndConditionsVersion = realm.getTermsAndConditionsByVersion(alias, x.getVersion());
                GeneralHelper.removeSecurityHeader(realm, session, termsAndConditionsVersion.get(0));
            });
            realm.removeTermsAndConditionsVersions(alias);
        }

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).success();
    }

    private void processModelsRecursively(RequiredActionProviderModel requiredActionProviderModel, List<RequiredActionProviderModel> modelsToRemove) {
        if (requiredActionProviderModel != null) {
            modelsToRemove.add(requiredActionProviderModel);
            if (requiredActionProviderModel.isValidator()) {
                requiredActionProviderModel.getConfig().computeIfPresent("validation-policy-id", (key, policyId) -> {
                    deletionValidationPolicy(requiredActionProviderModel, policyId);
                    return policyId;
                });
                List<RequiredActionProviderModel> childActions = realm.getRequiredActionProvidersStream()
                        .filter(m -> requiredActionProviderModel.getId().equals(m.getValidatorStepId())).collect(Collectors.toList());
                for (RequiredActionProviderModel childModel : childActions) {
                    processModelsRecursively(childModel, modelsToRemove);
                }
            }
        }
    }

    private void deletionValidationPolicy(RequiredActionProviderModel requiredActionProviderModel, String policyId) {
        RealmPolicyProvider realmPolicyProvider = session.policies();
        RealmPolicyModel policyProviderModel = realmPolicyProvider.getPolicyById(realm, policyId);
        if (policyProviderModel != null) {
            boolean isPolicyDeleted = realmPolicyProvider.removePolicy(realm, policyId);
            if (isPolicyDeleted) {
                logger.debugf("Claim Policy deleted for {0}", requiredActionProviderModel.getId());
            } else {
                logger.debugf("Claim Policy deletion is failed for {0}", requiredActionProviderModel.getId());
            }
        }
    }

    /**
     * Raise required action's priority
     *
     * @param alias Alias of required action
     */
    @Path("required-action/{actionid}/{id}/raise-priority")
    @POST
    @NoCache
    public void raiseRequiredActionPriority(@PathParam("actionid") String actionId, @PathParam("id") String alias) {
        auth.realm().requireManageRealm();

        RequiredActionProviderModel model = realm.getRequiredActionProviderById(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action.");
        }

        RequiredActionProviderModel previous = null;
        for (RequiredActionProviderModel action : realm.getRequiredActionProvidersStream(actionId).collect(Collectors.toList())) {
            if (action.getId().equals(model.getId())) {
                break;
            }
            previous = action;
        }
        if (previous == null) return;
        int tmp = previous.getPriority();
        previous.setPriority(model.getPriority());
        realm.updateRequiredActionProvider(previous);
        model.setPriority(tmp);
        realm.updateRequiredActionProvider(model);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Lower required action's priority
     *
     * @param alias Alias of required action
     */
    @Path("/required-action/{actionid}/{id}/lower-priority")
    @POST
    @NoCache
    public void lowerRequiredActionPriority(@PathParam("actionid") String actionId, @PathParam("id") String alias) {
        auth.realm().requireManageRealm();

        RequiredActionProviderModel model = realm.getRequiredActionProviderById(alias);
        if (model == null) {
            throw new NotFoundException("Failed to find required action.");
        }

        List<RequiredActionProviderModel> actions = realm.getRequiredActionProvidersStream(actionId).collect(Collectors.toList());
        int i;
        for (i = 0; i < actions.size(); i++) {
            if (actions.get(i).getId().equals(model.getId())) {
                break;
            }
        }
        if (i + 1 >= actions.size()) return;
        RequiredActionProviderModel next = actions.get(i + 1);
        int tmp = model.getPriority();
        model.setPriority(next.getPriority());
        realm.updateRequiredActionProvider(model);
        next.setPriority(tmp);
        realm.updateRequiredActionProvider(next);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.REQUIRED_ACTION).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Get authenticator provider's configuration description
     */
    @Path("config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigInfoRepresentation getAuthenticatorConfigDescription(@PathParam("providerId") String providerId) {
        auth.realm().requireViewRealm();

        ConfigurableAuthenticatorFactory factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);

        if (factory == null) {
            providerId = new String(Base32.decode(providerId));
            factory = CredentialHelper.getConfigurableAuthenticatorFactory(session, providerId);
        }

        if (factory == null) {
            throw new NotFoundException("Could not find authenticator provider");
        }

        AuthenticatorConfigInfoRepresentation rep = new AuthenticatorConfigInfoRepresentation();
        rep.setProviderId(providerId);
        rep.setName(factory.getDisplayType());
        rep.setHelpText(factory.getHelpText());
        rep.setProperties(new LinkedList<>());
        List<ProviderConfigProperty> configProperties = Optional.ofNullable(factory.getConfigProperties(session)).orElse(Collections.emptyList());
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
            rep.getProperties().add(propRep);
        }
        return rep;
    }

    private ConfigPropertyRepresentation getConfigPropertyRep(ProviderConfigProperty prop) {
        return ModelToRepresentation.toRepresentation(prop);
    }


    // Here we reused the config concept of authenticators to event listeners
    @Path("event-config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigInfoRepresentation getEventListenerConfigDescription(@PathParam("providerId") String providerId) {
        auth.realm().requireViewRealm();

        EventListenerProviderFactory factory = (EventListenerProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(EventListenerProvider.class, providerId);
        if (factory == null) {
            throw new NotFoundException("Could not find event listener provider");
        }
        ConstraintResponse constraintResponse = factory.resolveConstraints(session);
        if (constraintResponse == null) {
            constraintResponse = new ConstraintResponse();
        }
        ObjectMapper objectMapper = new ObjectMapper();

        AuthenticatorConfigInfoRepresentation rep = new AuthenticatorConfigInfoRepresentation();
        rep.setProviderId(providerId);
        rep.setName(factory.getDisplayName());
        rep.setHelpText(factory.getHelpText());
        rep.setHasTestConfig(factory.isRequiredTestConfigDetails());
        rep.setProperties(new LinkedList<>());
        rep.setConstraints(objectMapper.convertValue(constraintResponse, Map.class));
        List<ProviderConfigProperty> configProperties = Optional.ofNullable(factory.getConfigProperties(session)).orElse(Collections.emptyList());
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
            rep.getProperties().add(propRep);
        }
        return rep;
    }


    @Path("email-config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigInfoRepresentation getEmailListenerConfigDescription(@PathParam("providerId") String providerId) {
        auth.realm().requireViewRealm();

        EmailSenderProviderFactory factory = (EmailSenderProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(EmailSenderProvider.class, providerId);
        if (factory == null) {
            throw new NotFoundException("Could not find email provider");
        }
        AuthenticatorConfigInfoRepresentation rep = new AuthenticatorConfigInfoRepresentation();
        rep.setProviderId(providerId);
        rep.setName(factory.getDisplayName());
        rep.setHasTestConfig(factory.isRequiredTestConfigDetails());
        rep.setProperties(new LinkedList<>());
        List<ProviderConfigProperty> configProperties = Optional.ofNullable(factory.getConfigProperties(session)).orElse(Collections.emptyList());
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
            rep.getProperties().add(propRep);
        }
        return rep;
    }

    @Path("sms-config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigInfoRepresentation getSmsListenerConfigDescription(@PathParam("providerId") String providerId) {
        auth.realm().requireViewRealm();

        SmsProviderFactory factory = (SmsProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(SmsProvider.class, providerId);
        if (factory == null) {
            throw new NotFoundException("Could not find sms provider");
        }
        AuthenticatorConfigInfoRepresentation rep = new AuthenticatorConfigInfoRepresentation();
        rep.setProviderId(providerId);
        rep.setName(factory.getDisplayName());
        rep.setHasTestConfig(factory.isRequiredTestConfigDetails());
        rep.setProperties(new LinkedList<>());
        List<ProviderConfigProperty> configProperties = Optional.ofNullable(factory.getConfigProperties(session)).orElse(Collections.emptyList());
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
            rep.getProperties().add(propRep);
        }
        return rep;
    }

    @Path("mfa-config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigInfoRepresentation getMFAConfigDescription(@PathParam("providerId") String providerId) {
        auth.realm().requireViewRealm();

        MFAProviderFactory factory = (MFAProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(MFAProvider.class, providerId);
        if (factory == null) {
            throw new NotFoundException("Could not find mfa provider");
        }
        AuthenticatorConfigInfoRepresentation rep = new AuthenticatorConfigInfoRepresentation();
        rep.setProviderId(providerId);
        rep.setName(factory.getDisplayName());
        rep.setHasTestConfig(false);
        rep.setProperties(new LinkedList<>());
        List<ProviderConfigProperty> configProperties = Optional.ofNullable(factory.getConfigProperties(session)).orElse(Collections.emptyList());
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
            rep.getProperties().add(propRep);
        }
        return rep;
    }

    @Path("action-config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ActionConfigInfoRepresentation getActionConfigDescription(@PathParam("providerId") String providerId) {
        auth.realm().requireViewRealm();

        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, providerId);

        if (factory == null) {
            logger.error("Action provider not found: " + providerId);
            throw new NotFoundException("Could not find Action provider");
        }

        ActionConfigInfoRepresentation rep = new ActionConfigInfoRepresentation();
        rep.setProviderId(providerId);
        rep.setConfigurable(factory.isConfigurable());
        rep.setHasPolicy(factory.hasPolicy());
        rep.setHelpText(factory.getDisplayText());
        if ((Arrays.asList((factory).supportedModes()).contains("bo")) && (realm.getMode().equals("bo"))) {
            Map<String, String> info = new HashMap<>();
            info.put("description", factory.getDescription());
            if (factory.isGifRequired()) {
                info.put("gif", factory.gifUrl(session.getContext().resolveLocale(auth.adminAuth().getUser(), LocaleType.ADMIN).getLanguage()));
            }
            rep.setInfo(info);
        }
        rep.setHasTestConfig(false);
        rep.setProperties(new LinkedList<>());
        List<ProviderConfigProperty> configProperties = Optional.ofNullable(factory.getConfigProperties(session)).orElse(Collections.emptyList());
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);


            if (factory.isClaimsCustomizable() && (propRep.getName() != null && (propRep.getName().endsWith("-claims") || propRep.getName().endsWith("-details")))) {
                if (propRep.getDefaultValue() != null) {
                    String[] defaultValue = ((String) propRep.getDefaultValue()).split("##");
                    propRep.setDefaultValue(defaultValue);
//                    propRep.setDefaultValue(Arrays.stream(defaultValue).map(x -> factory.getId().replace("bo-", "").replaceAll("-", "_") + "@" + x).collect(Collectors.toList()));
                }
                if (propRep.getMapOptions() != null) {
                    List<Map> s = JsonSerialization.convertValue(propRep.getMapOptions(), List.class);
                    s.stream().forEach(x -> {
                        x.put("name", x.get("name"));
                    });
                    propRep.setMapOptions(s);
                }
                if (propRep.getGroupOptions() != null) {
                    List<List<Map<String, String>>> groupOptions = JsonSerialization.convertValue(propRep.getGroupOptions(), List.class);
                    groupOptions.forEach(innerList -> {
                        innerList.forEach(map -> {
                            map.put("name", factory.getId().replace("bo-", "").replaceAll("-", "_") + "@" + map.get("name"));
                        });
                    });
                    propRep.setGroupOptions(groupOptions);
                }
            }

            rep.getProperties().add(propRep);
        }

        rep.setShouldShowSkipOption(factory.skippable());

        return rep;
    }

    @Path("validator-action-config/action-claims")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Map<String, String> getActionConfigDescription(List<Map<String, Object>> providers) {
        auth.realm().requireViewRealm();
        Map<String, String> res = new HashMap<>();
        providers.forEach(provider -> {
            RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, provider.get("id").toString());
            RequiredActionProviderModel providerModel = new RequiredActionProviderModel();
            Map<String, String> config = new HashMap<>();
            Map<String, ?> providerConfig = (Map<String, ?>) provider.get("config");
            String providerId = provider.get("id").toString().replace('-', '_').replace("bo_", "") + '@';
            for (Map.Entry<String, ?> entry : providerConfig.entrySet()) {
                config.put(entry.getKey(), entry.getValue().toString().replace(providerId, ""));
            }
            providerModel.setConfig(config);
            try {
                res.putAll(factory.getActionClaims(session, providerModel));
            } catch (Exception e) {
            }
        });
        return res;
    }

    @Path("validator-action-config/{actionId}/provider/{validatorStepId}/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ActionConfigInfoRepresentation getActionConfigDescription(@PathParam("actionId") String actionId, @PathParam("validatorStepId") String validatorStepId) {
        auth.realm().requireViewRealm();

        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, "validator");

        if (factory == null) {
            logger.error("Action provider not found: validator");
            throw new NotFoundException("Could not find validator action provider");
        }

        ActionConfigInfoRepresentation rep = new ActionConfigInfoRepresentation();
        rep.setProviderId("validator");
        rep.setProperties(new LinkedList<>());
        List<ProviderConfigProperty> configProperties = Optional.ofNullable(factory.getConfigProperties(session, actionId, validatorStepId)).orElse(Collections.emptyList());
        for (ProviderConfigProperty prop : configProperties) {
            ConfigPropertyRepresentation propRep = getConfigPropertyRep(prop);
            rep.getProperties().add(propRep);
        }
        return rep;
    }

    @Path("validator-action-config/on-demand/{actionId}/provider/{validatorStepId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public GeneralResponse getOnDemandValidatorConfig(@PathParam("actionId") String actionId, @PathParam("validatorStepId") String validatorStepId, Map<String, Object> config) {
        auth.realm().requireViewRealm();

        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, "validator");

        if (factory == null) {
            logger.error("Action provider not found: validator");
            throw new NotFoundException("Could not find Action provider");
        }

        List<ProviderConfigProperty> result = factory.getOnDemandConfig(session, actionId, validatorStepId, (HashMap<String, Object>) config);

        if (result == null) {
            GeneralResponse generalResponse = new GeneralResponse();
            generalResponse.setStatus(400);
            return generalResponse;
        }
        GeneralResponse generalResponse = new GeneralResponse();
        generalResponse.setStatus(200);
        generalResponse.setBody(result);
        return generalResponse;
    }

    /**
     * Get configuration descriptions for all clients
     */
    @Path("per-client-config-description")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Map<String, List<ConfigPropertyRepresentation>> getPerClientConfigDescription() {
        auth.realm().requireViewClientAuthenticatorProviders();

        return session.getKeycloakSessionFactory().getProviderFactoriesStream(ClientAuthenticator.class)
                .collect(Collectors.toMap(
                        ProviderFactory::getId,
                        factory -> {
                            ClientAuthenticatorFactory clientAuthFactory = (ClientAuthenticatorFactory)
                                    CredentialHelper.getConfigurableAuthenticatorFactory(session, factory.getId());
                            return clientAuthFactory.getConfigPropertiesPerClient().stream()
                                    .map(this::getConfigPropertyRep).collect(Collectors.toList());
                        }));
    }

    /**
     * Create new authenticator configuration
     *
     * @param rep JSON describing new authenticator configuration
     * @deprecated Use {@link #newExecutionConfig(String, AuthenticatorConfigRepresentation)} instead
     */
    @Path("config")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAuthenticatorConfig(AuthenticatorConfigRepresentation rep) {
        auth.realm().requireManageRealm();

        ReservedCharValidator.validate(rep.getAlias());

        AuthenticatorConfigModel config = realm.addAuthenticatorConfig(RepresentationToModel.toModel(rep));
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.AUTHENTICATOR_CONFIG).resourcePath(session.getContext().getUri(), config.getId()).representation(rep).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    /**
     * Get authenticator configuration
     *
     * @param id Configuration id
     */
    @Path("config/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigRepresentation getAuthenticatorConfig(@PathParam("id") String id) {
        auth.realm().requireViewRealm();

        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        return ModelToRepresentation.toRepresentation(config);
    }

    /**
     * Delete authenticator configuration
     *
     * @param id Configuration id
     */
    @Path("config/{id}")
    @DELETE
    @NoCache
    public void removeAuthenticatorConfig(@PathParam("id") String id) {
        auth.realm().requireManageRealm();

        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(id);
        if (config == null) {
            throw new NotFoundException("Could not find authenticator config");

        }
        realm.getAuthenticationFlowsStream().forEach(flow -> realm.getAuthenticationExecutionsStream(flow.getId())
                .filter(exe -> Objects.equals(id, exe.getAuthenticatorConfig()))
                .forEachOrdered(exe -> {
                    exe.setAuthenticatorConfig(null);
                    realm.updateAuthenticatorExecution(exe);
                }));

        realm.removeAuthenticatorConfig(config);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.AUTHENTICATOR_CONFIG).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Update authenticator configuration
     *
     * @param id  Configuration id
     * @param rep JSON describing new state of authenticator configuration
     */
    @Path("config/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public Response updateAuthenticatorConfig(@PathParam("id") String id, AuthenticatorConfigRepresentation rep) {
        auth.realm().requireManageRealm();

        ReservedCharValidator.validate(rep.getAlias());
        AuthenticatorConfigModel exists = realm.getAuthenticatorConfigById(id);
        if (exists == null) {
            throw new NotFoundException("Could not find authenticator config");
        }

        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(exists.getAlias());
        if (model != null) {
            AuthenticatorFactory f = (AuthenticatorFactory) session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, model.getAuthenticator());
            List<String> validations = validateAuthConfig(f.getConfigProperties(session), rep.getConfig());
            if (!validations.isEmpty()) {
                return ErrorResponse.error(String.join("\n", validations), BAD_REQUEST);
            }
        }

        exists.setAlias(rep.getAlias());
        exists.setConfig(RepresentationToModel.removeEmptyString(rep.getConfig()));
        realm.updateAuthenticatorConfig(exists);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.AUTHENTICATOR_CONFIG).resourcePath(session.getContext().getUri()).representation(rep).success();
        return Response.accepted(rep).build();
    }

    @Path("event-config/{providerid}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createEventConfig(@PathParam("providerid") String id, EventConfigRepresentation rep) {
        auth.realm().requireManageRealm();
        EventListenerProviderFactory providerFactory = (EventListenerProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(EventListenerProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        if (providerFactory.isRequiredTestConfigDetails()) {
            boolean result = providerFactory.testConfig(session, rep.getConfig());
            if (!result) {
                return Response.status(BAD_REQUEST).build();
            }
        }

        rep.setEventProviderId(id);
        EventConfigModel config = realm.addEventConfig(RepresentationToModel.toModel(rep));
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.EVENT_CONFIG).resourcePath(session.getContext().getUri(), config.getId()).representation(rep).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    @Path("test-event-config/{providerid}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AuditTestResponse testEventConfig(@PathParam("providerid") String id, Map<String, String> config) {
        auth.realm().requireManageRealm();

        EventListenerProviderFactory providerFactory = (EventListenerProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(EventListenerProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        boolean result = providerFactory.testConfig(session, config);
        if (result) {
            adminEvent.operation(OperationType.CONFIG_CHECK_SUCCESS).resource(ResourceType.APACHE_CONFIG).representation(config).error("Your account has enabled for own whatsApp provider and attempt to delete only available whatsApp provider is not allowed.").success();
        } else {
            adminEvent.operation(OperationType.CONFIG_CHECK_FAILED).resource(ResourceType.APACHE_CONFIG_ERROR).representation(config).error("Invalid config details for Apache Kafka").success();
        }

        return result ? new AuditTestResponse(true, null) : new AuditTestResponse(false, "config-is-not-valid-audit");
    }

    /**
     * Get authenticator configuration
     *
     * @param id Configuration id
     */
    @Path("event-config/{providerid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public EventConfigRepresentation getEventConfig(@PathParam("providerid") String id) {
        auth.realm().requireViewRealm();

        EventConfigModel config = realm.getEventConfigByProviderId(id);
        if (config == null) {
            return new EventConfigRepresentation();
        }
        return ModelToRepresentation.toRepresentation(config);
    }

    /**
     * Delete authenticator configuration
     *
     * @param id Configuration id
     */
    @Path("event-config/{providerid}")
    @DELETE
    @NoCache
    public void removeEventConfig(@PathParam("providerid") String id) {
        auth.realm().requireManageRealm();

        EventConfigModel config = realm.getEventConfigByProviderId(id);
        if (config == null) {
            throw new NotFoundException("Could not find event config");
        }

        realm.removeEventConfig(config);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.EVENT_CONFIG).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Update authenticator configuration
     *
     * @param id  Configuration id
     * @param rep JSON describing new state of authenticator configuration
     */
    @Path("event-config/{providerid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void updateEventConfig(@PathParam("providerid") String id, EventConfigRepresentation rep) throws Exception {
        auth.realm().requireManageRealm();

        EventConfigModel exists = realm.getEventConfigByProviderId(id);
        if (exists == null) {
            throw new NotFoundException("Could not find authenticator config");
        }

        EventListenerProviderFactory providerFactory = (EventListenerProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(EventListenerProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        if (providerFactory.isRequiredTestConfigDetails()) {
            boolean result = providerFactory.testConfig(session, rep.getConfig());
            if (!result) {
                throw new Exception("Invalid config details");
            }
        }
        session.getKeycloakSessionFactory().getProviderFactory(EventListenerProvider.class, id);

        exists.setConfig(RepresentationToModel.removeEmptyString(rep.getConfig()));
        realm.updateEventConfig(exists);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.EVENT_CONFIG).resourcePath(session.getContext().getUri()).representation(rep).success();
    }


    /**
     * Create SMS configuration
     *
     * @param id ProviderId
     */
    @Path("sms-config/{providerid}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSmsConfig(@PathParam("providerid") String id, SmsConfigRepresentation rep) {
        auth.realm().requireManageRealm();
        SmsProviderFactory providerFactory = (SmsProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(SmsProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }
        List<String> configuredSmsSenders = realm.getSmsSenderStream().collect(Collectors.toList());

        if (configuredSmsSenders.contains(id)) {
            return ErrorResponse.exists("The Sms Sender " + id + " is already Created");
        }

        List<String> validations = providerFactory.validateConfig(session, rep.getConfig());
        if (!validations.isEmpty()) {
            return ErrorResponse.error(String.join("\n", validations), BAD_REQUEST);
        }

        rep.setSmsProviderId(id);
        SmsConfigModel config = realm.addSmsConfig(RepresentationToModel.toModel(rep));
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.SMS_CONFIG).resourcePath(session.getContext().getUri(), config.getId()).representation(rep).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    @Path("test-sms-config/{providerid}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AuditTestResponse testSmsConfig(@PathParam("providerid") String id, Map<String, String> config) {
        auth.realm().requireManageRealm();

        SmsProviderFactory providerFactory = (SmsProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(SmsProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        List<ProviderConfigProperty> providerConfigProperties = providerFactory.getConfigProperties(session);

        SmsConfigModel fromConfig = realm.getSmsConfigByProviderId(id);
        if (fromConfig != null) {
            providerConfigProperties.forEach(x -> {
                if (x.isSecret() && config.containsKey(x.getName())) {
                    String value = config.get(x.getName());
                    if (value.equals(ComponentRepresentation.SECRET_VALUE)) {
                        if (fromConfig.getConfig() != null && fromConfig.getConfig().containsKey(x.getName())) {
                            config.put(x.getName(), fromConfig.getConfig().get(x.getName()));
                        }
                    }
                }
            });
        }

        SmsSenderProviderFactory smsSenderProviderFactory = (SmsSenderProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(SmsSenderProvider.class, "default");

        ConfigTestResponse result = smsSenderProviderFactory.testConfig(session, id, config);

        return new AuditTestResponse(result.isSuccess(), result.getErrorMessage());
    }


    @Path("counter")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> authCounters() {
        auth.realm().requireManageRealm();

        Map<String, Integer> counter = new HashMap<>();
        counter.put("onBoardingCount", Integer.parseInt(String.valueOf(getOnboardingFlows().count())));
        counter.put("loginCount", Integer.parseInt(String.valueOf(getLoginFlows().count() + getDirectGrantFlows().count())));
        counter.put("recoveryCount", Integer.parseInt(String.valueOf(getRecoveryFlows().count())));
        counter.put("mfaCount", Integer.parseInt(String.valueOf(realm.getMFAProvidersStream().count())));
        counter.put("socialCount", Integer.parseInt(String.valueOf(realm.getIdentityProvidersStream().filter(x -> !x.isInBuilt()).count())));
        counter.put("actionCount", Integer.parseInt(String.valueOf(getActions().count())));
        counter.put("enterpriseCount", realm.getComponentsStream(realm.getId(), "org.keycloak.storage.UserStorageProvider").collect(Collectors.toList()).size());
        return counter;
    }

    /**
     * Get SMS configuration
     *
     * @param id ProviderId
     */
    @Path("sms-config/{providerid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public SmsConfigRepresentation getSmsConfig(@PathParam("providerid") String id) {
        auth.realm().requireViewRealm();

        SmsConfigModel config = realm.getSmsConfigByProviderId(id);
        if (config == null) {
            return new SmsConfigRepresentation();
        }
        return ModelToRepresentation.toRepresentation(session, config);
    }

    /**
     * Delete SMS configuration
     *
     * @param id Configuration id
     */
    @Path("sms-config/{providerid}")
    @DELETE
    @NoCache
    public void removeSmsConfig(@PathParam("providerid") String id) throws Exception {
        auth.realm().requireManageRealm();

        SmsConfigModel config = realm.getSmsConfigByProviderId(id);
        if (config == null) {
            throw new NotFoundException("Could not find sms config");
        }

        if (config.getSmsProviderId().equals("whatsapp")) {
            if (realm.isEnabledOwnWhatsappProvider() || realm.getName().equals(Config.getAdminRealm())) {
                adminEvent.operation(OperationType.DELETE_FAILED).resource(ResourceType.WHATSAPP_CONFIG).representation(config).error("Your account has enabled for own whatsApp provider and attempt to delete only available whatsApp provider is not allowed.").success();
                throw new Exception("cannot-remove-check-logs");
            }
        }

        if ((realm.isEnabledOwnSmsProvider() || realm.getName().equals(Config.getAdminRealm())) && realm.getSmsSenderStream().filter(x -> !x.equals("whatsapp")).count() == 1) {
            adminEvent.operation(OperationType.DELETE_FAILED).resource(ResourceType.SMS_CONFIG).representation(config).error("Your account has enabled for own sms provider and attempt to delete only available sms provider is not allowed. If required try to setup alternative sms provider and try to delete again.").success();
            throw new Exception("cannot-remove-check-logs");
        }

        realm.removeSmsConfig(config);
        List<String> removedSMSSenders = realm.getSmsSenderStream().filter(x -> !x.equals(id)).collect(Collectors.toList());
        realm.setSmsSenders(new HashSet<>(removedSMSSenders));
        if (config.getSmsProviderId().equals("whatsapp")) {
            adminEvent.operation(OperationType.DELETE).resource(ResourceType.WHATSAPP_CONFIG).resourcePath(session.getContext().getUri()).success();
        } else {
            adminEvent.operation(OperationType.DELETE).resource(ResourceType.SMS_CONFIG).resourcePath(session.getContext().getUri()).success();
        }
    }

    /**
     * Update SMS configuration
     *
     * @param id  Configuration id
     * @param rep JSON describing new state of SMS configuration
     */
    @Path("sms-config/{providerid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response updateSmsConfig(@PathParam("providerid") String id, SmsConfigRepresentation rep) throws Exception {
        auth.realm().requireManageRealm();

        SmsConfigModel exists = realm.getSmsConfigByProviderId(id);
        if (exists == null) {
            throw new NotFoundException("Could not find sms config");
        }

        SmsProviderFactory providerFactory = (SmsProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(SmsProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        List<String> validations = providerFactory.validateConfig(session, rep.getConfig());
        if (!validations.isEmpty()) {
            return ErrorResponse.error(String.join("\n", validations), BAD_REQUEST);
        }
        Map<String, String> updatedConfig = GeneralHelper.removeInvalidSecrets(providerFactory.getConfigProperties(session), exists.getConfig(), rep.getConfig());
        exists.setConfig(RepresentationToModel.removeEmptyString(updatedConfig));
        realm.updateSmsConfig(exists);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.SMS_CONFIG).resourcePath(session.getContext().getUri()).representation(rep).success();

        return Response.accepted(rep).build();
    }

    /**
     * Create Email Configuration
     *
     * @param id ProviderId
     */
    @Path("email-config/{providerid}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createEmailConfig(@PathParam("providerid") String id, EmailConfigRepresentation rep) {
        auth.realm().requireManageRealm();
        EmailSenderProviderFactory providerFactory = (EmailSenderProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(EmailSenderProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }
        Set<String> configuredEmailSenders = realm.getEmailSenderStream().collect(Collectors.toSet());

        if (configuredEmailSenders.contains(id)) {
            return ErrorResponse.exists("The Email Sender " + id + " is already Created");
        }

        List<String> validations = providerFactory.validateConfig(session, rep.getConfig());
        if (!validations.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", validations));
        }

        rep.setEmailProviderId(id);
        EmailConfigModel config = realm.addEmailConfig(RepresentationToModel.toModel(rep));

        //updating realm email sender table
        configuredEmailSenders.add(id);
        realm.setEmailSenders(configuredEmailSenders);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.EMAIL_CONFIG).resourcePath(session.getContext().getUri(), config.getId()).representation(rep).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    /**
     * Testing Email Connection of given providerId
     *
     * @param id
     * @param config
     * @return
     */
    @Path("test-email-config/{providerid}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AuditTestResponse testEmailConfig(@PathParam("providerid") String id, Map<String, String> config) {
        auth.realm().requireManageRealm();
        session.setAttribute("userId", auth.adminAuth().getUser().getId());
        config.put("adminEvent", "true");
        config.put("userId", String.valueOf(session.getAttribute("userId")));

        EmailSenderProviderFactory providerFactory = (EmailSenderProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(EmailSenderProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        List<ProviderConfigProperty> providerConfigProperties = providerFactory.getConfigProperties(session);

        EmailConfigModel fromConfig = realm.getEmailConfigByProviderId(id);
        if (fromConfig != null) {
            providerConfigProperties.forEach(x -> {
                if (x.isSecret() && config.containsKey(x.getName())) {
                    String value = config.get(x.getName());
                    if (value.equals(ComponentRepresentation.SECRET_VALUE)) {
                        if (fromConfig.getConfig() != null && fromConfig.getConfig().containsKey(x.getName())) {
                            config.put(x.getName(), fromConfig.getConfig().get(x.getName()));
                        }
                    }
                }
            });
        }

        ConfigTestResponse result = providerFactory.testConfig(session, auth.adminAuth().getUser(), config);

        if (result.isSuccess() == false) {
            GeneralHelper.adminEvent(session, OperationType.NOTIFICATION_ERROR, ResourceType.NOTIFICATION);
        } else {
            GeneralHelper.adminEvent(session, OperationType.NOTIFICATION, ResourceType.NOTIFICATION);
        }

        return new AuditTestResponse(result.isSuccess(), "Configuration is not valid " + result.getErrorMessage());
    }

    /**
     * Get Email Configuration of providerId
     *
     * @param id
     * @return
     */
    @Path("email-config/{providerid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public EmailConfigRepresentation getEmailConfig(@PathParam("providerid") String id) {
        auth.realm().requireViewRealm();

        EmailConfigModel config = realm.getEmailConfigByProviderId(id);
        if (config == null) {
            return new EmailConfigRepresentation();
        }
        return ModelToRepresentation.toRepresentation(session, config);
    }

    /**
     * Delete the Email Configuration
     *
     * @param id
     */
    @Path("email-config/{providerid}")
    @DELETE
    @NoCache
    public void removeEmailConfig(@PathParam("providerid") String id) throws Exception {
        auth.realm().requireManageRealm();

        EmailConfigModel config = realm.getEmailConfigByProviderId(id);
        if (config == null) {
            throw new NotFoundException("Could not find email config");
        }

        if ((realm.isEnabledOwnEmailProvider() || realm.getName().equals(Config.getAdminRealm())) && realm.getEmailSenderStream().count() == 1) {
            adminEvent.operation(OperationType.DELETE_FAILED).resource(ResourceType.EMAIL_CONFIG).representation(config).error("Your account has enabled for own email provider and attempt to delete only available email provider is not allowed. If required try to setup alternative email provider and try to delete again.").success();
            throw new Exception("cannot-remove-check-logs");
        }

        realm.removeEmailConfig(config);
        List<String> removedEmailSenders = realm.getEmailSenderStream().filter(x -> !x.equals(id)).collect(Collectors.toList());
        realm.setEmailSenders(new HashSet<>(removedEmailSenders));

        //updating realm email sender table
        Set<String> configuredEmailSenders = realm.getEmailSenderStream().collect(Collectors.toSet());
        configuredEmailSenders.remove(id);
        realm.setEmailSenders(configuredEmailSenders);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.EMAIL_CONFIG).representation(config).success();
    }

    /**
     * Update Email configuration
     *
     * @param id  Configuration id
     * @param rep JSON describing new state of authenticator configuration
     */
    @Path("email-config/{providerid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void updateEmailConfig(@PathParam("providerid") String id, EmailConfigRepresentation rep) throws Exception {
        auth.realm().requireManageRealm();

        EmailConfigModel exists = realm.getEmailConfigByProviderId(id);
        if (exists == null) {
            throw new NotFoundException("Could not find email config");
        }

        EmailSenderProviderFactory providerFactory = (EmailSenderProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(EmailSenderProvider.class, id);
        if (providerFactory == null) {
            throw new GeneralException("Could not find provider factory");
        }

        List<String> validations = providerFactory.validateConfig(session, rep.getConfig());
        if (!validations.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", validations));
        }
        Map<String, String> updatedConfig = GeneralHelper.removeInvalidSecrets(providerFactory.getConfigProperties(session), exists.getConfig(), rep.getConfig());
        exists.setConfig(RepresentationToModel.removeEmptyString(updatedConfig));
        realm.updateEmailConfig(exists);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.EMAIL_CONFIG).resourcePath(session.getContext().getUri()).representation(rep).success();
    }

    /**
     * Create MFA configuration
     *
     * @param id
     * @param rep
     * @return
     */
    @Path("mfa-config/{providerid}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createMFAConfig(@PathParam("providerid") String id, MFAConfigRepresentation rep) {
        auth.realm().requireManageRealm();

        MFAConfigModel config = realm.getMFAConfigByProviderId(id);
        if (config != null) {
            return ErrorResponse.exists("The MFA Provider " + id + " is already configured");
        }

        MFAProviderFactory providerFactory = (MFAProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(MFAProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        List<String> validations = providerFactory.validateConfig(session, rep.getConfig());
        if (validations != null && !validations.isEmpty()) {
            return ErrorResponse.error(String.join("\n", validations), BAD_REQUEST);
        }

        rep.setMfaProviderId(id);
        rep.setId(id);
        config = realm.addMFAConfig(RepresentationToModel.toModel(rep));
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.MFA_CONFIG).resourcePath(session.getContext().getUri(), config.getId()).representation(rep).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    /**
     * Get MFA Configuration of given providerId
     *
     * @param id
     * @return
     */
    @Path("mfa-config/{providerid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public MFAConfigRepresentation getMFAConfig(@PathParam("providerid") String id) {
        auth.realm().requireViewRealm();

        MFAConfigModel config = realm.getMFAConfigByProviderId(id);
        if (config == null) {
            return new MFAConfigRepresentation();
        }
        return ModelToRepresentation.toRepresentation(session, config);
    }

    /**
     * Delete MFA configuration
     *
     * @param id Configuration id
     */
    @Path("mfa-config/{providerid}")
    @DELETE
    @NoCache
    public void removeMFAConfig(@PathParam("providerid") String id) {
        auth.realm().requireManageRealm();

        MFAConfigModel config = realm.getMFAConfigByProviderId(id);
        if (config == null) {
            throw new NotFoundException("Could not find mfa config");
        }
        String mfaConnId = config.getMfaProviderId();

        List<ClientModel> configuredMfas = session.getContext().getRealm().getClientsStream().collect(Collectors.toList());
        for (ClientModel configuredMfa : configuredMfas) {
            if (configuredMfa.getMFAConnections().get(mfaConnId) != null && configuredMfa.getMFAConnections().get(mfaConnId) == true) {
                throw new ClientErrorException("It is not possible to delete MFA since it is used in our applications", CONFLICT);
            }
        }

        if (realm.getMFAConnections().get(mfaConnId) != null && realm.getMFAConnections().get(mfaConnId) == true) {
            throw new ClientErrorException("It is not possible to delete the MFA since it is used in the workspace authentication settings", CONFLICT);
        }

        //remove the MFA Configs
        realm.removeMFAConfig(config);

        //Remove the MFA Links of clients,if mfa is removed
        List<ClientModel> clients = realm.getClientsStream().collect(Collectors.toList());
        for (ClientModel cli : clients) {
            cli.removeMFAConnections(mfaConnId);
        }

        MFAConfigRepresentation rep = new MFAConfigRepresentation();
        rep.setId(id);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.MFA_CONFIG).resourcePath(session.getContext().getUri()).representation(rep).success();
    }

    /**
     * Update MFA configuration
     *
     * @param id  Configuration id
     * @param rep JSON describing new state of authenticator configuration
     */
    @Path("mfa-config/{providerid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public Response updateMFAConfig(@PathParam("providerid") String id, MFAConfigRepresentation rep) throws Exception {
        auth.realm().requireManageRealm();

        MFAConfigModel exists = realm.getMFAConfigByProviderId(id);
        if (exists == null) {
            throw new NotFoundException("Could not find mfa config");
        }

        MFAProviderFactory providerFactory = (MFAProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(MFAProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        List<String> validations = providerFactory.validateConfig(session, rep.getConfig());
        if (validations != null && !validations.isEmpty()) {
            return ErrorResponse.error(String.join("\n", validations), BAD_REQUEST);
        }
        Map<String, String> updatedConfig = GeneralHelper.removeInvalidSecrets(providerFactory.getConfigProperties(session), exists.getConfig(), rep.getConfig());
        exists.setConfig(RepresentationToModel.removeEmptyString(updatedConfig));
        realm.updateMFAConfig(exists);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.MFA_CONFIG).resourcePath(session.getContext().getUri()).representation(rep).success();
        return Response.accepted(rep).build();
    }

    /**
     * Uninstall Marketplace Integration
     *
     * @param id Configuration id
     */
    @Path("marketplace/{providerid}")
    @DELETE
    @NoCache
    public void removeMarketplaceConfig(@PathParam("providerid") String id) {
        auth.realm().requireManageRealm();

        MarketplaceConfigModel marketplaceConfigModel = new MarketplaceConfigModel();
        marketplaceConfigModel.setProviderId(id);

        String partner = id.split("-")[0].toLowerCase();
        if (realm.getMarketplaceConfigsStream().filter(x -> x.getProviderId().equalsIgnoreCase(partner + "-template")).count() == 0) {
            Set<String> realmAttributes = realm.getAttributes().keySet();
            for (String attribute : realmAttributes) {
                if (attribute.toLowerCase().contains(partner)) {
                    realm.removeAttribute(attribute);
                }
            }
        }

        realm.removeMarketplaceConfig(marketplaceConfigModel);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.MARKETPLACE).resourcePath(session.getContext().getUri()).success();
    }

    //Secret is hidden in  SOCIAL,LDAP,EMAIL,SMS,MFA
    @POST
    @Path("common/showSecret")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> showHiddenSecret(Map<String, String> providerDetails) {
        //if reqBody contains both keys,then only we can procees
        if (providerDetails.containsKey("providerFactory") && providerDetails.containsKey("providerId")) {
            String provFactory = providerDetails.get("providerFactory");
            String provId = providerDetails.get("providerId");

            switch (provFactory) {
                case "EmailSenderProviderFactory":
                    EmailSenderProviderFactory emlPf = (EmailSenderProviderFactory) getProviderFactory(session, EmailSenderProvider.class, provId);

                    if (emlPf == null) {
                        throw new NotFoundException("Could not find provider factory");
                    }

                    EmailConfigModel emlConfig = realm.getEmailConfigByProviderId(provId);
                    if (emlConfig == null) {
                        throw new NotFoundException("Could not find Email config");
                    }
                    return GeneralHelper.showSecret(emlPf.getConfigProperties(session), emlConfig.getConfig());

                case "SmsProviderFactory":
                    SmsProviderFactory smsPf = (SmsProviderFactory) getProviderFactory(session, SmsProvider.class, provId);
                    if (smsPf == null) {
                        throw new NotFoundException("Could not find provider factory");
                    }

                    SmsConfigModel smsConfig = realm.getSmsConfigByProviderId(provId);
                    if (smsConfig == null) {
                        throw new NotFoundException("Could not find sms config");
                    }
                    return GeneralHelper.showSecret(smsPf.getConfigProperties(session), smsConfig.getConfig());

                case "MFAProviderFactory":
                    MFAProviderFactory mfaPf = (MFAProviderFactory) getProviderFactory(session, MFAProvider.class, provId);
                    if (mfaPf == null) {
                        throw new NotFoundException("Could not find provider factory");
                    }

                    MFAConfigModel mfaConfig = realm.getMFAConfigByProviderId(provId);
                    if (mfaConfig == null) {
                        throw new NotFoundException("Could not find MFA config");
                    }
                    return GeneralHelper.showSecret(mfaPf.getConfigProperties(session), mfaConfig.getConfig());
                case "IdentityProviderFactory":
                    IdentityProviderFactory iPf = (IdentityProviderFactory) getProviderFactory(session, IdentityProvider.class, provId);

                    if (iPf == null) {
                        iPf = (IdentityProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(SocialIdentityProvider.class, provId);
                    }
                    if (iPf == null) {
                        throw new NotFoundException("Could not find provider factory");
                    }
                    IdentityProviderModel ipConfig = realm.getIdentityProviderByAlias(provId);
                    if (ipConfig == null) {
                        throw new NotFoundException("Could not find sms config");
                    }
                    return GeneralHelper.showSecret(null, ipConfig.getConfig());

            }
        }
        return new HashMap<>();
    }

    public static <T extends Provider> ProviderFactory<?> getProviderFactory(KeycloakSession session, Class<T> clazz, String provId) {
        return session.getKeycloakSessionFactory().getProviderFactory(clazz, provId);
    }

    //Marketplace ezto-verify

    /**
     * to fetch all the marketplace templates that's available (the json is removed from the model in order to reduce the payload)
     *
     * @param search          string used to search the templates
     * @param category        used for filter based on the category of the template
     * @param publishedStatus used for filter based on the published status (true/false)
     * @param filterBy        user for filter as ascending or descending or lastUpdated
     */
    @Path("marketplace/templates")
    @GET
    @NoCache
    public Response getAllMarketplaceTemplates(
            @QueryParam("search") String search,
            @QueryParam("filterByCategory") String category,
            @QueryParam("filterByPublishedStatus") String publishedStatus,
            @QueryParam("filterBy") String filterBy,
            @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResult
    ) {
        auth.realm().requireManageRealm();

        RealmModel currentRealm = realm;
        List<EztovrfyMarketplaceTemplateModel> models;

        boolean isNotAdminRealm = !realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm());
        if (isNotAdminRealm) {
            if (isLicenseAvailable()) return Response.status(204, "No Valid Subscriptions").build();
            currentRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
        }

        try {
            // gets the template model based on search and tenant
            models = getEztovrfyMarketplaceTemplateModels(search, currentRealm, realm, isNotAdminRealm);
            // filters the models based on the filter conditions and category
            models = filterEztovrfyMarketplaceTemplateModels(category, filterBy, models);
            // returns only the published templates to sub tenant
            if (StringUtil.isNotBlank(publishedStatus)) {
                models = models.stream().filter(model -> publishedStatus.equalsIgnoreCase(String.valueOf(model.isPublished()))).collect(Collectors.toList());
            }

            // add marketplace as true to all templates
            models.forEach(x -> {
                x.setMarketplace(true);
            });

            // Pagination
            if (firstResult == null || firstResult < 0) firstResult = 0;
            if (maxResult == null || maxResult <= 0) maxResult = models.size();

            int toIndex = Math.min(firstResult + maxResult, models.size());
            if (firstResult >= models.size()) {
                models = Collections.emptyList();
            } else {
                models = models.subList(firstResult, toIndex);
            }

            return Response.ok(Collections.unmodifiableList(models)).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            logger.info("failed to get templates, error occurred");
            return ErrorResponse.error("failed-to-get-templates", CONFLICT);
        }
    }

    /**
     * @param category
     * @param filterBy
     * @param models
     * @return
     */
    private List<EztovrfyMarketplaceTemplateModel> filterEztovrfyMarketplaceTemplateModels(String category, String filterBy, List<EztovrfyMarketplaceTemplateModel> models) {
        if (StringUtil.isNotBlank(filterBy)) {
            if ("asc".equalsIgnoreCase(filterBy)) {
                models = models.stream().sorted(Comparator.comparing(EztovrfyMarketplaceTemplateModel::getWorkflowName)).collect(Collectors.toList());
            } else if ("dsc".equalsIgnoreCase(filterBy)) {
                models = models.stream().sorted(Comparator.comparing(EztovrfyMarketplaceTemplateModel::getWorkflowName).reversed()).collect(Collectors.toList());
            } else if ("lastUpdated".equalsIgnoreCase(filterBy)) {
                models = models.stream().sorted(Comparator.comparing(EztovrfyMarketplaceTemplateModel::getModifiedTimestamp).reversed()).collect(Collectors.toList());
            }
        }

        if (StringUtil.isNotBlank(category)) {
            if (category.equalsIgnoreCase("recommended")) {
                String recommendedCategories = realm.getAttribute("recommendedMarketplaceCategories");
                if (StringUtil.isNotBlank(recommendedCategories)) {
                    List<String> categories = Arrays.stream(recommendedCategories.split(",")).collect(Collectors.toList());
                    models = models.stream().filter(model -> categories.contains(model.getWorkflowCategory())).collect(Collectors.toList());
                }
            } else
                models = models.stream().filter(model -> category.equalsIgnoreCase(model.getWorkflowCategory())).collect(Collectors.toList());
        }

        return models;
    }

    /**
     * @param search
     * @param currentRealm
     * @param realm
     * @param isNotAdminRealm
     * @return
     */
    private static List<EztovrfyMarketplaceTemplateModel> getEztovrfyMarketplaceTemplateModels(String search, RealmModel currentRealm, RealmModel realm, boolean isNotAdminRealm) {
        List<EztovrfyMarketplaceTemplateModel> models;
        if (StringUtil.isNotBlank(search)) {
            models = currentRealm.searchEztovrfyMarketplaceTemplateParams(search.toLowerCase(), search);
        } else {
            models = currentRealm.getEztovrfyMarketplaceTemplateStream().collect(Collectors.toList());
        }
        if (isNotAdminRealm) {
            models = models.stream()
                    .filter(EztovrfyMarketplaceTemplateModel::isPublished)
                    .filter(x -> StringUtil.isBlank(x.getCountry()) || x.getCountry().contains(realm.getDefaultRegion()))
                    .collect(Collectors.toList());
        }
        return models;
    }

    /**
     * get the specific marketplace template based on the id
     *
     * @param templateId unique identifier of the template
     */
    @Path("marketplace/template/{templateId}")
    @GET
    @NoCache
    public Response getMarketplaceTemplate(@PathParam("templateId") String templateId) {
        auth.realm().requireManageRealm();
        RealmModel currentRealm = realm;
        if (!currentRealm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            currentRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
        }
        Optional<EztovrfyMarketplaceTemplateModel> optionalModel = currentRealm.getEztovrfyMarketplaceTemplateStream().filter(x -> x.getId().equals(templateId)).findFirst();
        if (optionalModel.isEmpty())
            return ErrorResponse.error("template-not-found", BAD_REQUEST);

        EztovrfyMarketplaceTemplateModel model = optionalModel.get();
        HashMap<String, Object> workflowJson = new HashMap<>();
        ActionModel actionModel = currentRealm.getActionById(model.getWorkflowId());
        ActionRepresentation actionRep = null;
        if (actionModel != null)
            actionRep = toRepresentation(currentRealm, actionModel);
        if (actionRep != null) {
            workflowJson.put("actionVariables", actionRep);
        }
        List<RequiredActionProviderRepresentation> requiredActionsStream = getRequiredActionsByRealm(currentRealm, model.getWorkflowId());
        if (!requiredActionsStream.isEmpty()) {
            workflowJson.put("data", requiredActionsStream);
        }
        JSONObject jsonObject = new JSONObject(workflowJson);

        model.setWorkflowJson(jsonObject.toString());
        model.setMarketplace(true);
        return Response.ok(model).build();
    }

    private List<RequiredActionProviderRepresentation> getRequiredActionsByRealm(RealmModel realm, String actionId) {
        List<RequiredActionProviderModel> actions = realm.getRequiredActionProvidersStream(actionId).collect(Collectors.toList());
        triggerOnDemandMigration(realm, actions);

        return actions.stream().map(x -> toRepresentation(session, x)).collect(Collectors.toList());
    }

    /**
     * creates a new marketplace template
     *
     * @param data a json object - EztovrfyMarketplaceTemplateModel rep, workflowName is mandatory and unique
     */
    @Path("marketplace/create")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public Response createMarketplaceTemplate(EztovrfyMarketplaceTemplateModel data) {
        auth.realm().requireManageRealm();
        if (!realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            return ErrorResponse.error("Unauthorised", Response.Status.UNAUTHORIZED);
        }
        if (data == null) {
            return ErrorResponse.exists(ERROR_REQ_BODY_NOT_FOUND);
        }

        String workflowName = data.getWorkflowName();

        if (workflowName == null || workflowName.isEmpty()) {
            logger.error("workflow creation failed, workflow name not provided");
            return ErrorResponse.errors(Collections.singletonList(new ErrorRepresentation("failed-to-create-workflow-with-empty-name")), BAD_REQUEST);
        }
        if (GeneralHelper.validationForHtmlInjection(workflowName)) {
            adminEvent.operation(OperationType.CREATE_FAILED).resource(ResourceType.ACTION).representation(data).error(HTML_INJECTED_IN_FIELDS).success();
            return ErrorResponse.error(INVALID_ACTION_NAME, BAD_REQUEST);
        }

        if (realm.getEztovrfyMarketplaceTemplateStream().anyMatch(x -> x.getWorkflowName().equalsIgnoreCase(workflowName))) {
            logger.error("marketplace-template-creation-error");
            return ErrorResponse.errors(Collections.singletonList(new ErrorRepresentation("marketplace-template-creation-error")), BAD_REQUEST);
        }

        String actionId = createAction(workflowName, true, "false");
        if (actionId == null) {
            logger.error("failed to create workflow");
            return ErrorResponse.error("failed-to-create-workflow", BAD_REQUEST);
        }
        realm.getActionById(actionId).setMarketPlace(true);

        uploadImageToMinio(data);

        data.setWorkflowId(actionId);

        data = realm.addEztovrfyMarketplaceTemplate(data);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.ACTION).resourcePath(session.getContext().getUri()).representation(data).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(data.getId()).build()).entity(data).build();
    }

    private void uploadImageToMinio(EztovrfyMarketplaceTemplateModel data) {
        String bucketName = realm.getName() + "-avatars";
        if (data.getImageURL() != null) {
            try {
                byte[] byteArray = Base64.getDecoder().decode(data.getImageURL());
                String objectName = "public/marketplace/ezto-vrfy/workflow-template/" + data.getWorkflowName() + GeneralHelper.detectFileType(byteArray);
                String minioFileUrl = GeneralHelper.uploadToMinIO(bucketName, objectName, byteArray);
                data.setImageURL(minioFileUrl);
            } catch (Exception e) {
                data.setImageURL(getDefaultLogoUrl());
                logger.error("failed to set image");
                logger.error(e.getMessage());
                logger.error(e.getStackTrace());
            }
        } else {
            data.setImageURL(getDefaultLogoUrl());
        }
    }

    /**
     * updates a marketplace template
     *
     * @param templateId unique UUID of the template
     * @param data       json rep of EztovrfyMarketplaceTemplateModel
     */
    @Path("marketplace/template/{templateId}")
    @PUT
    @NoCache
    public Response updateMarketplaceTemplate(@PathParam("templateId") String templateId, EztovrfyMarketplaceTemplateModel data) {
        auth.realm().requireManageRealm();
        if (!realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            return ErrorResponse.error("Unauthorised", Response.Status.UNAUTHORIZED);
        }
        if (data == null) {
            return ErrorResponse.exists(ERROR_REQ_BODY_NOT_FOUND);
        }

        if (StringUtil.isBlank(templateId)) {
            return ErrorResponse.exists("template-id-not-found");
        }

        if (realm.getEztovrfyMarketplaceTemplateStream().anyMatch(x -> x.getWorkflowName().equalsIgnoreCase(data.getWorkflowName()) && !x.getWorkflowId().equalsIgnoreCase(data.getWorkflowId()))) {
            logger.error("marketplace-template-creation-error");
            return ErrorResponse.errors(Collections.singletonList(new ErrorRepresentation("marketplace-template-with-same-name")), BAD_REQUEST);
        }

        EztovrfyMarketplaceTemplateModel template = null;
        Optional<EztovrfyMarketplaceTemplateModel> optionalTemplate = realm.getEztovrfyMarketplaceTemplateStream().filter(x -> x.getId().equals(templateId)).findFirst();

        if (!optionalTemplate.isPresent()) {
            return ErrorResponse.error(ERROR_REQ_BODY_NOT_FOUND, BAD_REQUEST);
        }
        template = optionalTemplate.get();

        if (template.getWorkflowName() == null || !template.getWorkflowName().equals(data.getWorkflowName())) {
            ActionModel actionModel = realm.getActionById(data.getWorkflowId());
            if (actionModel == null) {
                return ErrorResponse.errors(Collections.singletonList(new ErrorRepresentation("marketplace-template-creation-error")), BAD_REQUEST);
            }
            actionModel.setAlias(data.getWorkflowName());
            realm.updateAction(actionModel);
            template.setWorkflowName(data.getWorkflowName());
        }
        if (template.getWorkflowCategory() == null || !template.getWorkflowCategory().equals(data.getWorkflowCategory())) {
            template.setWorkflowCategory(data.getWorkflowCategory());
        }
        if (template.getCountry() == null || !template.getCountry().equals(data.getCountry())) {
            template.setCountry(data.getCountry());
        }
        if (template.getWorkflowDescription() == null || !template.getWorkflowDescription().equals(data.getWorkflowDescription())) {
            template.setWorkflowDescription(data.getWorkflowDescription());
        }
        if (template.getTemplateTag() == null || !template.getTemplateTag().equals(data.getTemplateTag())) {
            template.setTemplateTag(data.getTemplateTag());
        }
        if (template.getImageURL() == null || !template.getImageURL().equals(data.getImageURL())) {
            template.setImageURL(data.getImageURL());
            uploadImageToMinio(template);
        }
        if (!(template.isPublished() && data.isPublished())) {
            template.setPublished(data.isPublished());
        }

        realm.removeEztovrfyMarketplaceTemplate(template);
        realm.addEztovrfyMarketplaceTemplate(template);

        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.ACTION).resourcePath(session.getContext().getUri()).representation(data).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(template.getId()).build()).entity(data).build();
    }

    /**
     * @param templateId
     */
    @Path("marketplace/template/{templateId}")
    @DELETE
    @NoCache
    public Response removeMarketplaceTemplate(@PathParam("templateId") String templateId) {
        auth.realm().requireManageRealm();
        if (!realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            return ErrorResponse.error("Unauthorised", Response.Status.UNAUTHORIZED);
        }
        Optional<EztovrfyMarketplaceTemplateModel> optionalModel = realm.getEztovrfyMarketplaceTemplateStream().filter(x -> x.getId().equals(templateId)).findFirst();
        if (optionalModel.isEmpty()) {
            return ErrorResponse.errors(Collections.singletonList(new ErrorRepresentation("template-id-not-found")), BAD_REQUEST);
        }

        Optional<ActionModel> actionModel = realm.getActionStream().filter(x -> x.getMarketPlace() && x.getAlias().equalsIgnoreCase(optionalModel.get().getWorkflowName())).findAny();
        if (!actionModel.isEmpty()) {
            realm.getClientsStream().forEach(client -> {
                Map<String, String> bindingOverrides = client.getAuthenticationFlowBindingOverrides();
                if (bindingOverrides == null || bindingOverrides.isEmpty()) {
                    return;
                }
                if (actionModel.get().getId().equals(bindingOverrides.get("action"))) {
                    client.setAuthenticationFlowBindingOverride("action", null);
                }
                if (actionModel.get().getId().equals(bindingOverrides.get("verification-action"))) {
                    client.setAuthenticationFlowBindingOverride("verification-action", null);
                }
                if (actionModel.get().getId().equals(bindingOverrides.get("onboarding-action"))) {
                    client.setAuthenticationFlowBindingOverride("onboarding-action", null);
                }
            });
            realm.removeAction(actionModel.get());
        }
        GeneralHelper.deleteTemplateAction(realm, optionalModel.get().getWorkflowId());
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.ACTION).resourcePath(session.getContext().getUri()).representation(optionalModel.get()).success();
        return realm.removeEztovrfyMarketplaceTemplate(optionalModel.get()) ? Response.ok(new HashMap<String, String>() {{
            put("status", "200");
            put("message", "deleted-successfully");
        }}).build() : ErrorResponse.error("failed-to-delete-template", BAD_REQUEST);
    }

    /**
     * fetches all the available categories for marketplace
     *
     * @return all the available marketplace categories
     */
    @Path("marketplace/categories")
    @GET
    @NoCache
    public Response getAllMarketplaceCategories(
            @QueryParam("search") String search,
            @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResult
    ) {
        auth.realm().requireManageRealm();
        RealmModel currentRealm = realm;
        if (!currentRealm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            if (isLicenseAvailable()) return Response.status(204, "No Valid Subscriptions").build();
            currentRealm = session.realms().getRealmByName(org.keycloak.Config.getAdminRealm());
        }

        List<EztovrfyMarketplaceCategoryModel> categories;

        // filtering based on search
        categories = currentRealm.getEztovrfyMarketplaceCategoriesStream()
                .filter(category -> {
                    if (StringUtil.isBlank(search)) {
                        return true;
                    }
                    String lowerSearch = search.toLowerCase();
                    return category.getCategory().toLowerCase().contains(lowerSearch);
                })
                .collect(Collectors.toList());

        // Pagination handling
        if (firstResult == null || firstResult < 0) firstResult = 0;
        if (maxResult == null || maxResult <= 0) maxResult = categories.size();

        int toIndex = Math.min(firstResult + maxResult, categories.size());
        if (firstResult >= categories.size()) {
            categories = Collections.emptyList();
        } else {
            categories = categories.subList(firstResult, toIndex);
        }

        // Return an unmodifiable list
        return Response.ok(Collections.unmodifiableList(categories)).build();
    }

    /**
     * creates a new marketplace category
     *
     * @param data a json, name is a mandatory and unique field
     * @return object with the details of the category created and all the available categories is returned in order to reduce redundant api calls from frontend
     */
    @Path("marketplace/category/create")
    @POST
    @NoCache
    public Response createMarketplaceCategory(Map<String, String> data) {
        auth.realm().requireManageRealm();
        if (!realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            return ErrorResponse.error("Unauthorised", Response.Status.UNAUTHORIZED);
        }
        if (data == null || data.isEmpty()) {
            return ErrorResponse.error(ERROR_REQ_BODY_NOT_FOUND, BAD_REQUEST);
        }

        String categoryName = data.get("name");
        if (StringUtil.isBlank(categoryName))
            return ErrorResponse.error("name-is-mandatory", BAD_REQUEST);

        if (realm.getEztovrfyMarketplaceCategoriesStream().anyMatch(x -> x.getCategory().equalsIgnoreCase(categoryName))) {
            return ErrorResponse.errors(Collections.singletonList(new ErrorRepresentation("category-already-exists")), CONFLICT);
        }

        EztovrfyMarketplaceCategoryModel eztovrfyMarketplaceCategoryModel = new EztovrfyMarketplaceCategoryModel();
        eztovrfyMarketplaceCategoryModel.setCategory(categoryName);
        eztovrfyMarketplaceCategoryModel.setRealmId(realm.getId());

        eztovrfyMarketplaceCategoryModel = realm.addEztovrfyMarketplaceCategory(eztovrfyMarketplaceCategoryModel);

        data.put("id", eztovrfyMarketplaceCategoryModel.getId());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.ACTION).resourcePath(session.getContext().getUri()).representation(data).success();
        HashMap<String, Object> response = new HashMap<>();
        response.put("selectedCategory", data);
        response.put("categories", realm.getEztovrfyMarketplaceCategoriesStream().collect(Collectors.toList()));
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(eztovrfyMarketplaceCategoryModel.getId()).build()).entity(response).build();
    }

    /**
     * @param name name of the category that needs to be deleted (name is an unique field and a category associated with a template cant be deleted)
     * @return object with all the available categories after deletion
     */
    @Path("marketplace/category/{name}")
    @DELETE
    @NoCache
    public Response removeMarketplaceCategory(@PathParam("name") String name) {
        auth.realm().requireManageRealm();
        if (!realm.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
            return ErrorResponse.error("Unauthorised", Response.Status.UNAUTHORIZED);
        }
        Optional<EztovrfyMarketplaceCategoryModel> optionalModel = realm.getEztovrfyMarketplaceCategoriesStream().filter(x -> x.getCategory().equals(name)).findFirst();
        if (optionalModel.isEmpty())
            return ErrorResponse.error("category-not-found", BAD_REQUEST);
        if (realm.getEztovrfyMarketplaceTemplateStream().anyMatch(x -> name.equalsIgnoreCase(x.getWorkflowCategory()))) {
            return ErrorResponse.errors(Collections.singletonList(new ErrorRepresentation("cant-delete-category-that-is-live")), BAD_REQUEST);
        }
        if (!realm.removeEztovrfyMarketplaceCategory(optionalModel.get())) {
            return ErrorResponse.errors(Collections.singletonList(new ErrorRepresentation("failed-to-delete-template")), BAD_REQUEST);
        }
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.ACTION).resourcePath(session.getContext().getUri()).representation(optionalModel.get()).success();
        HashMap<String, Object> response = new HashMap<>();
        response.put("categories", realm.getEztovrfyMarketplaceCategoriesStream().collect(Collectors.toList()));
        return Response.ok(response).build();
    }

    private boolean isLicenseAvailable() {
        return false;
    }

}

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

import javax.ws.rs.core.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.json.JSONObject;
import org.keycloak.Config;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.admin.AuthorizationService;
import org.keycloak.ciba.*;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.marketplace.MarketplaceException;
import org.keycloak.marketplace.template.MarketplaceTemplateProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProviderFactory;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.oidc.OIDCClientSecretConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.par.endpoints.ParEndpoint;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.*;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.*;
import org.keycloak.services.clientregistration.ClientRegistrationTokenUtils;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.managers.*;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.theme.Theme;
import org.keycloak.urls.UrlType;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.*;
import org.keycloak.validation.ValidationUtil;
import org.keycloak.vault.DefaultVaultRawSecret;
import org.keycloak.vault.VaultProvider;
import org.keycloak.vault.VaultResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.qlikverify.helpers.GeneralHelper.isMaximumHookFailureReached;
import static java.lang.Boolean.TRUE;
import static org.keycloak.authentication.authenticators.utils.Constants.*;
import static org.keycloak.models.AuthenticationFlowBindings.CIBA_AUTHORIZER;
import static org.keycloak.models.CibaConfig.*;
import static org.keycloak.protocol.oidc.OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD;
import static org.keycloak.protocol.oidc.utils.PkceUtils.generateCodeVerifier;
import static org.keycloak.utils.GeneralHelper.setUpAppTypes;
import static org.keycloak.utils.GeneralHelper.setUpConfigs;


/**
 * Base resource class for managing one particular client of a realm.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 * @resource Clients
 */
public class ClientResource {
    protected static final Logger logger = Logger.getLogger(ClientResource.class);
    private static final String LOG_TAG = "EZTO:CLIENT_RESOURCE: ";
    protected RealmModel realm;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    protected ClientModel client;
    protected KeycloakSession session;
    private String ENCRYPTION_ENABLE = "encryption-enable";
    private String FAILED_HOOK_ATTEMPTS = "failed_hook_attempts";
    private Map<String, Object> encrytionDependencyMap = new HashMap<>();

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected HttpHeaders headers;

    public ClientResource(RealmModel realm, AdminPermissionEvaluator auth, ClientModel clientModel, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.client = clientModel;
        this.session = session;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT);
    }

    @Path("protocol-mappers")
    public ProtocolMappersResource getProtocolMappers() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.clients().requireManage(client);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.clients().requireView(client);
        ProtocolMappersResource mappers = new ProtocolMappersResource(realm, client, auth, adminEvent, manageCheck, viewCheck);
        ResteasyProviderFactory.getInstance().injectProperties(mappers);
        return mappers;
    }

    /**
     * Update the client
     *
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(final ClientRepresentation rep) {
        auth.clients().requireConfigure(client);

        try {
            GeneralHelper.validationsForClientUpdation(realm, adminEvent, rep.getClientId(), rep.getName());

            if(GeneralHelper.validationForHtmlInjection(rep.getName())){
                adminEvent.operation(OperationType.UPDATE_FAILED).representation(rep).error(HTML_INJECTED_IN_FIELDS).success();
                return ErrorResponse.error(INVALID_APP_NAME, Status.BAD_REQUEST);
            }

            if (rep.getName() != null) {
                if(!"workflows".equals(rep.getAttributes().get("clientType")) && !rep.getName().matches("^[\\p{L}\\p{M}\\p{Z}\\-]+$")) {
                    return ErrorResponse.exists("invalid-client-name");
                } else if ("workflows".equals(rep.getAttributes().get("clientType")) && !rep.getName().matches("^[a-zA-Z0-9\\- ]+$")) {
                    return ErrorResponse.exists("invalid-client-name");
                }
                String finalAppName = GeneralHelper.checkAppName(rep.getName());
                rep.setName(finalAppName);
            }

            session.setAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED, Boolean.FALSE);
            session.clientPolicy().triggerOnEvent(new AdminClientUpdateContext(rep, client, auth.adminAuth()));

            if ((rep.getAppType().equals("high_secure")) || (rep.getAppType().equals("browser_based"))) {
                if ((!rep.getOverrideAuthentication() || !rep.getAttributes().get("inviteOnly").equalsIgnoreCase("true"))
                        && realm.getUserInviteClientId() != null
                        && realm.getUserInviteClientId().equals(client.getClientId())) {
                    return ErrorResponse.exists("Sorry! Authentication settings can't be changed" +
                            " because they conflict with workspace user onboarding application.");
                }
            }

            if (client.getAttribute("isInBuilt") != null &&
                    client.getAttribute("isInBuilt").equalsIgnoreCase("true") &&
                    !rep.getName().equalsIgnoreCase(client.getName())) {
                return ErrorResponse.exists("can't change name for Default Application");
            }

            if (rep.getAppType().equals(Constants.IWS)) {
                if (rep.getAttributes().get("iws.credtype").equalsIgnoreCase("iws_admin_set_pass")) {
                    String adminUsername = rep.getAttributes().get("iws.admin.user");
                    String adminPass = rep.getAttributes().get("iws.admin.pass");
                    if (!(adminUsername != null && adminPass != null && !adminUsername.isEmpty() && !adminPass.isEmpty())) {
                        return ErrorResponse.error("Admin Credentials are missing for the app", Response.Status.BAD_REQUEST);
                    }
                } else if (rep.getAttributes().get("iws.credtype").equalsIgnoreCase("iws_user_set_pass")) {
                    if (!TextUtils.isEmpty(rep.getAttributes().get("iws.admin.user"))) {
                        rep.getAttributes().remove("iws.admin.user");
                    }
                    if (!TextUtils.isEmpty(rep.getAttributes().get("iws.admin.pass"))) {
                        rep.getAttributes().remove("iws.admin.pass");
                    }
                }
            }

            if(!GeneralHelper.updateApplicationRoleName(session, realm, client, rep)){
                adminEvent.operation(OperationType.UPDATE_FAILED).representation(rep).error("Failed to update application name").success();
                return ErrorResponse.error("Failed to update application name", Response.Status.BAD_REQUEST);
            }

            updateClientFromRep(rep, client, session);
            //QV-1807 Add support to add Normal apps to SSO dashboard
            String roleName = "sso-" + client.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-role";
            if (client.getProtocol().equals("saml")) {
                roleName = "sso-" + client.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-" + client.getAttribute("sso_type") + "-role";
            }

            String iwsEnableAttribute = client.getAttribute("iws.enable");
            if (iwsEnableAttribute != null && iwsEnableAttribute.equalsIgnoreCase("true")) {
                roleName = "iws-" + client.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-role";
            }

            if (client.getAttribute("sso.show") != null && client.getAttribute("sso.show").equalsIgnoreCase("true")) {
                if (!(iwsEnableAttribute != null && iwsEnableAttribute.equalsIgnoreCase("true")) && realm.getRole(roleName) == null) {
                    realm.addRole(roleName);
                }
                ActionModel finalActModel = null;
                Optional<ActionModel> actionModel = realm.getActionStream().filter(x -> x.getAlias().equals(client.getClientId() + "_action")).findFirst();
                if (!actionModel.isPresent()) {
                    finalActModel = new ActionModel();
                    finalActModel.setAlias(client.getClientId() + "_action");
                    finalActModel.setInBuilt(true);
                    finalActModel = realm.addAction(finalActModel);
                } else {
                    finalActModel = actionModel.get();
                }

                PolicyModel triggerPolicyModel = (PolicyModel) session.policies().getPolicyByName(realm, client.getClientId() + "_template_trigger_policy");
                if (triggerPolicyModel == null) {
                    triggerPolicyModel = new PolicyModel();
                    triggerPolicyModel.setPolicyName(client.getClientId() + "_template_trigger_policy");
                    triggerPolicyModel.setPolicyType("action");
                    triggerPolicyModel.setPolicyValue("triggerpolicy(true) and decision()");
                    triggerPolicyModel.setRealmPolicy(false);
                    triggerPolicyModel.setInBuilt(true);
                    triggerPolicyModel = session.policies().addPolicy(realm, triggerPolicyModel);
                }

                Optional<RequiredActionProviderModel> providerModel = realm.getRequiredActionProvidersStream(finalActModel.getId()).filter(x -> x.getProviderId().equals("sso-user-role-check")).findFirst();
                if (!providerModel.isPresent()) {
                    RequiredActionProviderModel userRoleRA = new RequiredActionProviderModel();
                    userRoleRA.setAlias("sso-user-role-check");
                    userRoleRA.setName("sso-user-role-check");
                    userRoleRA.setProviderId("sso-user-role-check");
                    userRoleRA.setDefaultAction(false);
                    userRoleRA.setPolicyId(triggerPolicyModel.getId());
                    userRoleRA.setPriority(0);
                    userRoleRA.setEnabled(true);
                    userRoleRA.setActionId(finalActModel.getId());
                    realm.addRequiredActionProvider(userRoleRA);
                }
            } else {
                if (client.getAttribute("isSSO") == null) {
                    RoleModel roleModel = realm.getRole(roleName);
                    if (roleModel != null) {
                        realm.removeRole(roleModel);
                    }
                    Optional<ActionModel> actionModel = realm.getActionStream().filter(x -> x.getAlias().equals(client.getClientId() + "_action")).findFirst();
                    if (actionModel.isPresent()) {
                        Optional<RequiredActionProviderModel> providerModel = realm.getRequiredActionProvidersStream(actionModel.get().getId()).filter(x -> x.getProviderId().equals("sso-user-role-check")).findFirst();
                        if (providerModel.isPresent()) {
                            realm.removeRequiredActionProvider(providerModel.get());
                        }
                    }
                }
            }
            // QV-1807 Add support to add Normal apps to SSO dashboard
            ValidationUtil.validateClient(session, client, false, r -> {
                session.getTransactionManager().setRollbackOnly();
                throw new ErrorResponseException(
                        Errors.INVALID_INPUT,
                        r.getAllLocalizedErrorsAsString(AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale())),
                        Response.Status.BAD_REQUEST);
            });

            session.clientPolicy().triggerOnEvent(new AdminClientUpdatedContext(rep, client, auth.adminAuth()));

            if (!(boolean) session.getAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED)) {
                logger.debugv("Removing the previous rotation info for client {0}{1}, if there is", client.getClientId(), client.getName());
                OIDCClientSecretConfigWrapper.fromClientModel(client).removeClientSecretRotationInfo();
            }
            session.removeAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED);

            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client already exists");
        } catch (IllegalArgumentException e) {
            logger.errorv("Error while updating the app - IllegalArgumentException() :: /n {0}", e.getMessage());
            return ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        } catch (RuntimeException err) {
            logger.errorv("Error while updating the app - RuntimeException() :: /n {0}", err.getMessage());
            throw new ErrorResponseException("general_error", err.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    /*@GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation getClientTry() {
        auth.clients().requireView(client);

        ClientRepresentation representation = ModelToRepresentation.toRepresentation(client, session);

        representation.setAccess(auth.clients().getAccess(client));

        return representation;
    }
*/
    @Path("try")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClient(@QueryParam("redirectUrl") String redirectUrl) {
        auth.clients().requireView(client);
        ClientRepresentation representation = ModelToRepresentation.toRepresentation(client, session);
        String url = getOIDCUrl(client, session, realm, redirectUrl);
        Map<String, String> object = new HashMap<>();
        object.put("oidcurl", url);
        return Response.ok(object).build();
    }

    /**
     * Get representation of the client
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation getClient() {
        try {
            session.clientPolicy().triggerOnEvent(new AdminClientViewContext(client, auth.adminAuth()));
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        auth.clients().requireView(client);

        ClientRepresentation representation = ModelToRepresentation.toRepresentation(client, session);

        try {
            UrlBean urlBean = new UrlBean(realm, session.theme().getTheme(Theme.Type.LOGIN), session.getContext().getUri().getBaseUri(), session.getContext().getAuthServerUrl());
            representation.setRegistrationUrl(urlBean.getRegistrationUrl() + "?client_id=" + client.getClientId());
        } catch (IOException e) {
            logger.error(LOG_TAG + "error occurred at getClient(), " + e.getMessage());
        }
        representation.setAccess(auth.clients().getAccess(client));

        return representation;
    }

    /**
     * Get representation of certificate resource
     *
     * @param attributePrefix
     * @return
     */
    @Path("certificates/{attr}")
    public ClientAttributeCertificateResource getCertficateResource(@PathParam("attr") String attributePrefix) {
        return new ClientAttributeCertificateResource(realm, auth, client, session, attributePrefix, adminEvent);
    }

    @GET
    @NoCache
    @Path("installation/providers/{providerId}")
    public Response getInstallationProvider(@PathParam("providerId") String providerId) {
        auth.clients().requireView(client);

        ClientInstallationProvider provider = session.getProvider(ClientInstallationProvider.class, providerId);
        if (provider == null) throw new NotFoundException("Unknown Provider");
        return provider.generateInstallation(session, realm, client, session.getContext().getUri().getBaseUri());
    }

    /**
     * Delete the client
     */
    @DELETE
    @NoCache
    public Response deleteClient() {
        auth.clients().requireManage(client);

        if (client == null) {
            throw new NotFoundException("Could not find client");
        }

        //check if client is set as User Onboarding Application
        if (realm.getUserInviteClientId() != null && realm.getUserInviteClientId().equals(client.getClientId())) {
            return ErrorResponse.exists("We can't delete the application since it's used as User Onboarded Application");
        }

        if(realm.getMode().equalsIgnoreCase("bo") && client.getAttribute("isInBuilt") != null && client.getAttribute("isInBuilt").equals("true")){
            return ErrorResponse.exists("Can't delete an inbuilt application");
        }

        try {
            session.clientPolicy().triggerOnEvent(new AdminClientUnregisterContext(client, auth.adminAuth()));
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        if (new ClientManager(new RealmManager(session)).removeClient(session, realm, client)) {
            ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, "max-clients");
            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(ModelToRepresentation.toLogRepresentation(client)).success();
        } else {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Could not delete client",
                    Response.Status.BAD_REQUEST);
        }
        return Response.noContent().build();
    }


    /**
     * Generate a new secret for the client
     *
     * @return
     */
    @Path("client-secret")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CredentialRepresentation regenerateSecret() {
        try {
            auth.clients().requireConfigure(client);

            logger.debug("regenerateSecret");
            session.setAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED, Boolean.FALSE);

            ClientRepresentation representation = ModelToRepresentation.toRepresentation(client, session);
            ClientSecretRotationContext secretRotationContext = new ClientSecretRotationContext(
                    representation, client, client.getSecret());

            String secret = client.isSandboxMode() != null && client.isSandboxMode() ? KeycloakModelUtils.generateSandboxSecret(client).getChallengeResponse() : KeycloakModelUtils.generateSecret(client).getChallengeResponse();

            session.clientPolicy().triggerOnEvent(secretRotationContext);

            CredentialRepresentation rep = new CredentialRepresentation();
            rep.setType(CredentialRepresentation.SECRET);
            rep.setValue(secret);

            if (!(boolean) session.getAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED)) {
                logger.debugv("Removing the previous rotation info for client {0}{1}, if there is", client.getClientId(), client.getName());
                OIDCClientSecretConfigWrapper.fromClientModel(client).removeClientSecretRotationInfo();
            }

            adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(rep).success();
            session.removeAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED);

            return rep;
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(),
                    Response.Status.BAD_REQUEST);
        }
    }

    public static String getOIDCUrl(ClientModel client, KeycloakSession session, RealmModel realm, String redirectUrl, Map<String, String> attributes) {
        String url = getOIDCUrl(client, session, realm, redirectUrl);
        if (attributes != null && attributes.size() > 0) {
            String mapped = attributes.keySet().stream().map(key -> key + "=" + attributes.get(key)).collect(Collectors.joining("&", "", ""));
            url += "&" + mapped;
        }
        return url;
    }

    public static String getParUrl(ClientModel clientModel, KeycloakSession session, RealmModel realm, String redirectUrl) {
        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        String parEndpoint = ParEndpoint.parUrl(backendUriInfo.getBaseUriBuilder()).build(realm.getName()).toString();

        return parEndpoint;
    }

    public static String getOIDCUrl(ClientModel client, KeycloakSession session, RealmModel realm, String redirectUrl) {
        ClientRepresentation clientRepresentation = ModelToRepresentation.toRepresentation(client, session);

        String response = null;
        if (clientRepresentation.getAppType() == Constants.BROWSER_BASED || clientRepresentation.getAppType() == Constants.IWS) {
            response = "&response_type=token&response_mode=fragment";
        } else if (clientRepresentation.getAppType() == Constants.HIGH_SECURE || clientRepresentation.getAppType() == Constants.CIBA) {
            response = "&response_type=code&response_mode=query";
        } else if (clientRepresentation.getAppType() == Constants.M2M || clientRepresentation.getAppType() == Constants.REST_API) {
            String baseUrl = session.getContext().getUri().getBaseUri().toString();
            return baseUrl + "realms/" + realm.getName() + "/protocol/openid-connect/token";
        }

        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        UriBuilder frontendUriBuilder = RealmsResource.protocolUrl(frontendUriInfo);
        String baseUrl = frontendUriBuilder.clone().path(OIDCLoginProtocolService.class, "auth").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString();
        String authUrl = ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth";
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            redirectUrl = getFormattedUri(String.valueOf(client.getRedirectUris().toArray()[0]), authUrl);
        }

        String url = baseUrl
                + "?client_id=" + client.getClientId()
                + "&redirect_uri=" + redirectUrl
                + "&scope=openid"
                + response
                + "&nonce=" + UUID.randomUUID();

        String pkceMethod = client.getAttribute(PKCE_CODE_CHALLENGE_METHOD);

        if (client.getAttribute(PKCE_CODE_CHALLENGE_METHOD) != null) {
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = null;
            if (pkceMethod.equalsIgnoreCase("S256")) {
                try {
                    codeChallenge = generateCodeChallange(codeVerifier);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            } else if (pkceMethod.equalsIgnoreCase("plain")) {
                codeChallenge = codeVerifier;
            }

            if (codeChallenge != null) {
                url += "&code_challenge=" + codeChallenge;
                url += "&code_challenge_method=" + pkceMethod;
            }
        }

        if (client.getName().equalsIgnoreCase(Constants.ADMIN_CONSOLE_CLIENT_ID)) {
            url += "&try_url=true";
        }

        return url;
    }

    private static String getFormattedUri(String url, String authUrl) {
        if (url.endsWith("*")) {
            url = url.replace("*", "");
        }
        if (url.length() == 0) {
            url = "{replace_with_redirect_url}";
            return url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        return authUrl + url;
    }

    static String generateCodeChallange(String codeVerifier) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = codeVerifier.getBytes("US-ASCII");
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes, 0, bytes.length);
        byte[] digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }


    /**
     * Generate a new registration access token for the client
     *
     * @return
     */
    @Path("registration-access-token")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ClientRepresentation regenerateRegistrationAccessToken() {
        auth.clients().requireManage(client);

        String token = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, realm, client, RegistrationAuth.AUTHENTICATED);

        ClientRepresentation rep = ModelToRepresentation.toRepresentation(client, session);
        rep.setRegistrationAccessToken(token);

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(rep).success();
        return rep;
    }

    /**
     * Get the client secret
     *
     * @return
     */
    @Path("client-secret")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public CredentialRepresentation getClientSecret() {
        auth.clients().requireView(client);

        logger.debug("getClientSecret");
        String secret = client.isSandboxMode() != null && client.isSandboxMode() ? client.getSandboxSecret() : client.getSecret();
        UserCredentialModel model = UserCredentialModel.secret(secret);
        if (model == null) throw new NotFoundException("Client does not have a secret");
        return ModelToRepresentation.toRepresentation(model);
    }

    /**
     * Base path for managing the scope mappings for the client
     *
     * @return
     */
    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.clients().requireManage(client);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.clients().requireView(client);
        return new ScopeMappedResource(realm, auth, client, session, adminEvent, manageCheck, viewCheck);
    }

    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(session, session.getContext().getUri(), realm, auth, client, adminEvent);
    }


    /**
     * Get default client scopes.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-client-scopes")
    public Stream<ClientScopeRepresentation> getDefaultClientScopes() {
        return getDefaultClientScopes(true);
    }

    private Stream<ClientScopeRepresentation> getDefaultClientScopes(boolean defaultScope) {
        auth.clients().requireView(client);

        return client.getClientScopes(defaultScope).values().stream().map(ClientResource::toRepresentation);
    }


    @PUT
    @NoCache
    @Path("default-client-scopes/{clientScopeId}")
    public void addDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId, true);
    }

    private void addDefaultClientScope(String clientScopeId, boolean defaultScope) {
        auth.clients().requireManage(client);

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new javax.ws.rs.NotFoundException("Client scope not found");
        }
        if (defaultScope && clientScope.isDynamicScope()) {
            throw new ErrorResponseException("invalid_request", "Can't assign a Dynamic Scope to a Client as a Default Scope", Response.Status.BAD_REQUEST);
        }
        client.addClientScope(clientScope, defaultScope);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.CLIENT_SCOPE_CLIENT_MAPPING).resourcePath(session.getContext().getUri()).success();
    }


    @DELETE
    @NoCache
    @Path("default-client-scopes/{clientScopeId}")
    public void removeDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        auth.clients().requireManage(client);

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new javax.ws.rs.NotFoundException("Client scope not found");
        }
        client.removeClientScope(clientScope);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.CLIENT_SCOPE_CLIENT_MAPPING).resourcePath(session.getContext().getUri()).success();
    }


    /**
     * Get optional client scopes.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("optional-client-scopes")
    public Stream<ClientScopeRepresentation> getOptionalClientScopes() {
        return getDefaultClientScopes(false);
    }

    @PUT
    @NoCache
    @Path("optional-client-scopes/{clientScopeId}")
    public void addOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId, false);
    }

    @DELETE
    @NoCache
    @Path("optional-client-scopes/{clientScopeId}")
    public void removeOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        removeDefaultClientScope(clientScopeId);
    }

    @Path("evaluate-scopes")
    public ClientScopeEvaluateResource clientScopeEvaluateResource() {
        return new ClientScopeEvaluateResource(session, session.getContext().getUri(), realm, auth, client, clientConnection);
    }

    /**
     * Get a user dedicated to the service account
     *
     * @return
     */
    @Path("service-account-user")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getServiceAccountUser() {
        auth.clients().requireView(client);

        UserModel user = session.users().getServiceAccount(client);
        if (user == null) {
            if (client.isServiceAccountsEnabled()) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(client);
                user = session.users().getServiceAccount(client);
            } else {
                throw new BadRequestException("Service account not enabled for the client '" + client.getClientId() + "'");
            }
        }

        return ModelToRepresentation.toRepresentation(session, realm, user);
    }

    /**
     * Push the client's revocation policy to its admin URL
     * <p>
     * If the client has an admin URL, push revocation policy to it.
     */
    @Path("push-revocation")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public GlobalRequestResult pushRevocation() {
        auth.clients().requireConfigure(client);

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).resource(ResourceType.CLIENT).success();
        return new ResourceAdminManager(session).pushClientRevocationPolicy(realm, client);

    }

    /**
     * Get application session count
     * <p>
     * Returns a number of user sessions associated with this client
     * <p>
     * {
     * "count": number
     * }
     *
     * @return
     */
    @Path("session-count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getApplicationSessionCount() {
        auth.clients().requireView(client);

        Map<String, Long> map = new HashMap<>();
        map.put("count", session.sessions().getActiveUserSessions(client.getRealm(), client));
        return map;
    }

    /**
     * Get user sessions for client
     * <p>
     * Returns a list of user sessions associated with this client
     *
     * @param firstResult Paging offset
     * @param maxResults  Maximum results size (defaults to 100)
     * @return
     */
    @Path("user-sessions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserSessionRepresentation> getUserSessions(@QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults) {
        auth.clients().requireView(client);

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;
        return session.sessions().getUserSessionsStream(client.getRealm(), client, firstResult, maxResults)
                .map(ModelToRepresentation::toRepresentation);
    }

    /**
     * Get application offline session count
     * <p>
     * Returns a number of offline user sessions associated with this client
     * <p>
     * {
     * "count": number
     * }
     *
     * @return
     */
    @Path("offline-session-count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getOfflineSessionCount() {
        auth.clients().requireView(client);

        Map<String, Long> map = new HashMap<>();
        map.put("count", session.sessions().getOfflineSessionsCount(client.getRealm(), client));
        return map;
    }

    /**
     * Get offline sessions for client
     * <p>
     * Returns a list of offline user sessions associated with this client
     *
     * @param firstResult Paging offset
     * @param maxResults  Maximum results size (defaults to 100)
     * @return
     */
    @Path("offline-sessions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserSessionRepresentation> getOfflineUserSessions(@QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults) {
        auth.clients().requireView(client);

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        return session.sessions().getOfflineUserSessionsStream(client.getRealm(), client, firstResult, maxResults)
                .map(this::toUserSessionRepresentation);
    }

    /**
     * Register a cluster node with the client
     * <p>
     * Manually register cluster node to this client - usually it's not needed to call this directly as adapter should handle
     * by sending registration request to Keycloak
     *
     * @param formParams
     */
    @Path("nodes")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void registerNode(Map<String, String> formParams) {
        auth.clients().requireConfigure(client);

        String node = formParams.get("node");
        if (node == null) {
            throw new BadRequestException("Node not found in params");
        }

        ReservedCharValidator.validate(node);

        if (logger.isDebugEnabled()) logger.debug("Register node: " + node);
        client.registerNode(node, Time.currentTime());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.CLUSTER_NODE).resourcePath(session.getContext().getUri(), node).success();
    }

    /**
     * Unregister a cluster node from the client
     *
     * @param node
     */
    @Path("nodes/{node}")
    @DELETE
    @NoCache
    public void unregisterNode(final @PathParam("node") String node) {
        auth.clients().requireConfigure(client);

        if (logger.isDebugEnabled()) logger.debug("Unregister node: " + node);

        Integer time = client.getRegisteredNodes().get(node);
        if (time == null) {
            throw new NotFoundException("Client does not have node ");
        }
        client.unregisterNode(node);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.CLUSTER_NODE).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Test if registered cluster nodes are available
     * <p>
     * Tests availability by sending 'ping' request to all cluster nodes.
     *
     * @return
     */
    @Path("test-nodes-available")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GlobalRequestResult testNodesAvailable() {
        auth.clients().requireConfigure(client);

        logger.debug("Test availability of cluster nodes");
        GlobalRequestResult result = new ResourceAdminManager(session).testNodesAvailability(realm, client);
        adminEvent.operation(OperationType.ACTION).resource(ResourceType.CLUSTER_NODE).resourcePath(session.getContext().getUri()).representation(result).success();
        return result;
    }

    @Path("/authz")
    public AuthorizationService authorization() {
        ProfileHelper.requireFeature(Profile.Feature.AUTHORIZATION);

        AuthorizationService resource = new AuthorizationService(this.session, this.client, this.auth, adminEvent);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     * @return
     */
    @Path("management/permissions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ManagementPermissionReference getManagementPermissions() {
        auth.roles().requireView(client);

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (!permissions.clients().isPermissionsEnabled(client)) {
            return new ManagementPermissionReference();
        }
        return toMgmtRef(client, permissions);
    }

    public static ManagementPermissionReference toMgmtRef(ClientModel client, AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.clients().resource(client).getId());
        ref.setScopePermissions(permissions.clients().getPermissions(client));
        return ref;
    }


    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     * @return initialized manage permissions reference
     */
    @Path("management/permissions")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public ManagementPermissionReference setManagementPermissionsEnabled(ManagementPermissionReference ref) {
        auth.clients().requireManage(client);
        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.clients().setPermissionsEnabled(client, ref.isEnabled());
        if (ref.isEnabled()) {
            return toMgmtRef(client, permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }

    /**
     * Invalidate the rotated secret for the client
     *
     * @return
     */
    @Path("client-secret/rotated")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response invalidateRotatedSecret() {
        try {
            auth.clients().requireConfigure(client);

            logger.debug("delete rotated secret");

            OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientModel(client);

            CredentialRepresentation rep = new CredentialRepresentation();
            rep.setType(CredentialRepresentation.SECRET);
            rep.setValue(wrapper.getClientRotatedSecret());

            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(rep).success();

            wrapper.removeClientSecretRotated();

            return Response.noContent().build();
        } catch (RuntimeException rte) {
            throw new ErrorResponseException(rte.getCause().getMessage(), rte.getMessage(),
                    Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get the rotated client secret
     *
     * @return
     */
    @Path("client-secret/rotated")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public CredentialRepresentation getClientRotatedSecret() {
        auth.clients().requireView(client);

        logger.debug("getClientRotatedSecret");
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientModel(client);
        if (!wrapper.hasRotatedSecret())
            throw new NotFoundException("Client does not have a rotated secret");
        else {
            UserCredentialModel model = UserCredentialModel.secret(wrapper.getClientRotatedSecret());
            return ModelToRepresentation.toRepresentation(model);
        }
    }

    @Path("trx/tutorial")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Map<String,List<CibaTutorialRepresentation>> getTutorial() {
        auth.realm().requireViewRealm();
        Set<CibaTutorialProvider> providers = session.getAllProviders(CibaTutorialProvider.class);
        if (providers.size() == 0)
            return new HashMap<>();

        List<CibaTutorialRepresentation> cibaOnboardingTutorialRepresentations =
                providers.stream().sorted(Comparator.comparing(CibaTutorialProvider::order)).map(x -> {
                            CibaTutorialRepresentation cibaTutorialRepresentation = new CibaTutorialRepresentation();
                            cibaTutorialRepresentation.setDisplayName(x.getDisplayName());
                            cibaTutorialRepresentation.setDescription(x.getDescription());
                            cibaTutorialRepresentation.setCommand(x.setUserModel(auth.adminAuth().getUser()).getCommand(client, "onboarding"));
                            return cibaTutorialRepresentation;
                        })
                        .collect(Collectors.toList());

        List<CibaTutorialRepresentation> cibaVerificationTutorialRepresentations =
                providers.stream().sorted(Comparator.comparing(CibaTutorialProvider::order)).map(x -> {
                            CibaTutorialRepresentation cibaTutorialRepresentation = new CibaTutorialRepresentation();
                            cibaTutorialRepresentation.setDisplayName(x.getDisplayName());
                            cibaTutorialRepresentation.setDescription(x.getDescription());
                            cibaTutorialRepresentation.setCommand(x.setUserModel(auth.adminAuth().getUser()).getCommand(client, "verification"));
                            return cibaTutorialRepresentation;
                        })
                        .collect(Collectors.toList());

        Map<String, List<CibaTutorialRepresentation>> cibaTutorialRepresentations = new HashMap<>();
        cibaTutorialRepresentations.put("onboarding", cibaOnboardingTutorialRepresentations);
        cibaTutorialRepresentations.put("verification", cibaVerificationTutorialRepresentations);
        return cibaTutorialRepresentations;
    }

    @Path("test/transaction/{actionFlow}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public String getTransactionResponse(final @PathParam("actionFlow") String actionFlow) {
        auth.realm().requireViewRealm();
        Set<CibaTutorialProvider> providers = session.getAllProviders(CibaTutorialProvider.class);

        Map<String, Object> representation = new HashMap<>();
        representation.put("clientId", client.getClientId());

        try {
            Map<String, Object> curlCommand = providers.stream().sorted(Comparator.comparing(CibaTutorialProvider::order))
                    .map(x -> x.setUserModel(auth.adminAuth().getUser()).getCommand(client, actionFlow)).collect(Collectors.toList()).get(0);

            String curl = "";
            if (curlCommand.get("data") != null) {
                Map data = JsonSerialization.convertValue(curlCommand.get("data"), Map.class);
                if (Boolean.parseBoolean(client.getAttribute("hidden_ua_multi_version"))) {
                    curl = (String) data.get("v0");
                } else {
                    curl = (String) data.get("v1");
                }
            }

            boolean isEncryptionEnabled = Boolean.parseBoolean(client.getAttribute("enable_ciba_request_encryption"));
            if (isEncryptionEnabled) {
                curl = GeneralHelper.encryptReqBody(realm, client, curl);
            }

            Map<String, Object> parsedRequest = GeneralHelper.parseCurl(curl);
            Map<String, String> headers = (Map<String, String>) parsedRequest.get("header");

            OkHttpClient client = HttpUtility.getUnsafeOkHttpClient();
            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
            String content = parsedRequest.get("body").toString();
            RequestBody body = RequestBody.create(mediaType, content.getBytes());

            okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                    .url(String.valueOf(parsedRequest.get("url")))
                    .method(String.valueOf(parsedRequest.get("method")).toUpperCase(), body);

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            okhttp3.Response response;
            response = client.newCall(requestBuilder.build()).execute();

            if (!response.isSuccessful()) {
                JSONObject errorJson = null;
                if(response.body() != null) {
                    errorJson = new JSONObject(response.body().string());
                }
                adminEvent.operation(OperationType.APP_TRANSACTION_TEST_SUCCESS).resource(ResourceType.CLIENT).representation(representation).success();
                return errorJson != null ? errorJson.toString(4) : "Error occured";
            }

            adminEvent.operation(OperationType.APP_TRANSACTION_TEST_SUCCESS).resource(ResourceType.CLIENT).representation(representation).success();
            JSONObject transactionResponse = new JSONObject(response.body().string());

            Map<String, String> authConfig = realm.getCibaAuthConfigByClientId(this.client.getClientId(), realm.getId()).getConfig();

            HashMap<String, Object> testConfig = new HashMap<>();
            testConfig.put(AUTH_WAY_MESSAGE, getAuthWayMessage(authConfig, transactionResponse));
            testConfig.put(RESULT_DELIVERY_MODE_MESSAGE, getResultDeliveryModeMessage(transactionResponse.get("result_delivery_mode").toString()));
            transactionResponse.put(TEST_CONFIG, testConfig);

            return transactionResponse.toString(4);
        } catch (IOException e) {
            logger.error("ERROR WHILE TESTING APPLICATION: " + e.getMessage());
            adminEvent.operation(OperationType.APP_TRANSACTION_TEST_FAILED).resource(ResourceType.CLIENT).representation(representation).error(e.getMessage()).success();
            return e.getMessage();
        }
        catch (Exception e) {
            logger.error("ERROR WHILE TESTING APPLICATION: " + e.getMessage());
            adminEvent.operation(OperationType.APP_TRANSACTION_TEST_FAILED).resource(ResourceType.CLIENT).representation(representation).error(e.getMessage()).success();
            return "Error occured";
        }
    }


    @Path(("test/api-transaction"))
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAPITransactionResponse(Map<String, Object> data) {
        auth.realm().requireViewRealm();
        String responseBody;
        Map<String, String> apiResponse = new HashMap<>();

        try {
            Map<String,Object> reqBody = (Map<String,Object>) data.get("data");
            String body = JsonSerialization.writeValueAsPrettyString(reqBody);

            OkHttpClient okHttpClient = HttpUtility.getUnsafeOkHttpClient();
            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json");
            RequestBody requestBody = RequestBody.create(mediaType, body.getBytes(StandardCharsets.UTF_8));

            okhttp3.Request.Builder reqBuilder = new okhttp3.Request.Builder()
                    .url(ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth/realms/" + realm.getName() + "/protocol/openid-connect/transaction/")
                    .method("POST", requestBody);
            reqBuilder.addHeader("api-version", "1");
            reqBuilder.addHeader("Authorization", BasicAuthHelper.createHeader(client.getClientId(), client.getSecret()));

            okhttp3.Response response;
            response = okHttpClient.newCall(reqBuilder.build()).execute();
            responseBody = response.body().string();
            apiResponse.put("response", responseBody);
            return Response.status(200).entity(JsonSerialization.readValue(responseBody, Map.class)).build();
        } catch (Exception e) {
            logger.error("Error while testing the API :: getAPITransactionResponse :: " + e.getMessage());
        }
        return Response.status(Status.BAD_REQUEST).build();
    }

    @Path("test-ocr/api-transaction")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOCRAPITransactionResponse(MultipartFormDataInput formDataInput) {
        auth.realm().requireViewRealm();

        try {
            OkHttpClient okHttpClient = HttpUtility.getUnsafeOkHttpClient();
            MultipartBody.Builder builder = new MultipartBody.Builder();

            formDataInput.getFormDataMap().forEach((key, value) -> {
                if("file".equalsIgnoreCase(key)) {
                    try {
                        InputStream inputStream = formDataInput.getFormDataMap().get(key).get(0).getBody(InputStream.class, null);
                        String ext = formDataInput.getFormDataMap().get(key).get(0).getMediaType().getSubtype();
                        File file = GeneralHelper.createTempFileWithType(inputStream, ext);
                        String contentType = Files.probeContentType(file.toPath());
                        if (contentType == null) {
                            throw new IllegalArgumentException("Unsupported file type");
                        }
                        builder.addFormDataPart("file", file.getName(), RequestBody.create(okhttp3.MediaType.parse(contentType), file));
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Error while testing OCR");
                    }
                } else {
                    try {
                        builder.addFormDataPart(key, formDataInput.getFormDataMap().get(key).get(0).getBodyAsString());
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Error while testing OCR");
                    }
                }
            });

            builder.setType(MultipartBody.FORM);
            RequestBody requestBody = builder.build();
            okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                    .url(ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth/realms/" + realm.getName() + "/protocol/openid-connect/transaction/")
                    .method("POST", requestBody);
            requestBuilder.addHeader("api-version", "1");
            requestBuilder.addHeader("Authorization", BasicAuthHelper.createHeader(client.getClientId(), client.getSecret()));
            okhttp3.Request request = requestBuilder.build();

            okhttp3.Response response = okHttpClient.newCall(request).execute();
            String resBody = response.body().string();
            return Response.status(200).entity(resBody).build();
        } catch (Exception e) {
            logger.error("ClientResource :: getOCRAPITransactionResponse :: Error while testing OCR API :: " + e.getMessage());
            return Response.status(400).entity(e.getMessage()).build();
        }
    }


    @Path("test/check")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public HashMap<String, Object> getTestTransactionPossibility(final @PathParam("actionFlow") String actionFlow) {
        CibaNotificationConfigModel notificationConfigModel = realm.getCibaNotificationConfigByClientId(this.client.getClientId(), realm.getId());
        String authWay = session.getContext().getClient().getAuthenticationFlowBindingOverride(CIBA_AUTHORIZER);
        HashMap<String, Object> testPossible = new HashMap<>();
        testPossible.put("test_possible", notificationConfigModel != null ? isTestPossible(notificationConfigModel.getConfig()) :
                (authWay.equals("api-authorizer")));
        testPossible.put("flow_possible", !authWay.contains("twilio-ivr-ciba-authorizer"));
        return testPossible;
    }


    @Path("test/result/{actionFlow}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public String getResultResponse(final @PathParam("actionFlow") String actionFlow, String authReqId) {
        auth.realm().requireViewRealm();
        Set<CibaTutorialProvider> providers = session.getAllProviders(CibaTutorialProvider.class);

        Map<String, Object> representation = new HashMap<>();
        representation.put("clientId", client.getClientId());

        Map<String, Object> curlCommand = providers.stream().sorted(Comparator.comparing(CibaTutorialProvider::order))
                .map(x -> x.setUserModel(auth.adminAuth().getUser()).getCommand(client, actionFlow)).collect(Collectors.toList()).get(2);

        try {
            String curl = "";
            if (curlCommand.get("data") != null) {
                Map data = JsonSerialization.convertValue(curlCommand.get("data"), Map.class);
                if (Boolean.parseBoolean(client.getAttribute("hidden_ua_multi_version"))) {
                    curl = (String) data.get("v0");
                } else {
                    curl = (String) data.get("v1");
                }
            }
            JSONObject json = new JSONObject(authReqId);
            curl = curl.replace("{auth_req_id from step1}", json.getString("authRequestId"));
            curl = curl.replace("{auth_req_id received to your hook url}", json.getString("authRequestId"));

            Map<String, Object> parsedRequest = GeneralHelper.parseCurl(curl);
            Map<String, String> headers = (Map<String, String>) parsedRequest.get("header");

            OkHttpClient client = HttpUtility.getUnsafeOkHttpClient();
            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
            String content = parsedRequest.get("body").toString();
            RequestBody body = RequestBody.create(mediaType, content.getBytes());
            okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                    .url(String.valueOf(parsedRequest.get("url")))
                    .method(String.valueOf(parsedRequest.get("method")).toUpperCase(), body);

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            okhttp3.Response response;
            response = client.newCall(requestBuilder.build()).execute();

            if (!response.isSuccessful()) {
                JSONObject errorJson = null;
                if(response.body() != null) {
                    errorJson = new JSONObject(response.body().string());
                }
                adminEvent.operation(OperationType.APP_RESULT_TEST_SUCCESS).resource(ResourceType.CLIENT).representation(representation).success();
                return errorJson != null ? errorJson.toString(4) : "Error occured";
            }

            adminEvent.operation(OperationType.APP_RESULT_TEST_SUCCESS).resource(ResourceType.CLIENT).representation(representation).success();
            JSONObject resultResponse = new JSONObject(response.body().string());
            return resultResponse.toString(4);
        } catch (Exception e) {
            logger.error("Error while testing the application: " + e.getMessage());
            adminEvent.operation(OperationType.APP_RESULT_TEST_FAILED).resource(ResourceType.CLIENT).representation(representation).error(e.getMessage()).success();
            return "Error occured";
        }
    }

    @Path("trx-notification-config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigInfoRepresentation getCibaNotificationConfigDescription(@PathParam("providerId") String providerId) {
        auth.realm().requireViewRealm();

        CibaNotificationProviderFactory factory = (CibaNotificationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaNotificationProvider.class, providerId);
        if (factory == null) {
            throw new NotFoundException("Could not find backoffice notification provider");
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

    @Path("dead-letter-hook-config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigInfoRepresentation getDeadLetterHookNotificationConfigDescription(@PathParam("providerId") String providerId) {
        auth.realm().requireViewRealm();

        CibaDeadLetterHookNotificationProviderFactory factory = (CibaDeadLetterHookNotificationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaDeadLetterHookNotificationProvider.class, providerId);
        if (factory == null) {
            throw new NotFoundException("Could not find backoffice notification provider");
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


    /**
     * Create MFA configuration
     *
     * @param id
     * @param rep
     * @return
     */
    @Path("trx-notification-config/{providerid}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createCibaNotificationConfig(@PathParam("providerid") String id, CibaNotificationConfigRepresentation rep) {
        auth.realm().requireManageRealm();

        CibaNotificationConfigModel config = realm.getCibaNotificationConfigByProviderId(id, client.getClientId(), realm.getId());
        if (config != null) {
            return ErrorResponse.exists("The Backoffice Notification Provider " + id + " is already configured");
        }
        if(rep.getConfig() != null){
            rep.getConfig().forEach((key, value)-> {
                if(rep.getConfig().containsKey(key) && rep.getConfig().get(key) != null && GeneralHelper.validationForHtmlInjection(rep.getConfig().get(key))){
                    Map<String, Object> representation = new HashMap<>();
                    representation.put("clientId", client.getClientId());
                    representation.put("config", rep.getConfig());
                    adminEvent.operation(OperationType.CREATE_FAILED).resource(ResourceType.BO_NOTIFICATION_CONFIG).representation(representation).error(HTML_INJECTED_IN_FIELDS).success();
                    throw new javax.ws.rs.BadRequestException(INVALID_CONFIG_INPUT);
                }
            });
        }

        rep.setProviderId(id);
        CibaNotificationConfigModel cibaNotificationConfigModel = RepresentationToModel.toModel(rep);
        cibaNotificationConfigModel.setClientId(client.getClientId());
        cibaNotificationConfigModel.setRealmId(realm.getId());

        String notificationEndpoint = rep.getConfig().get("notification-endpoint");
        String resultWay = rep.getConfig().containsKey("hook-way") ? rep.getConfig().get("hook-way") : rep.getConfig().get("queue-way");
        if(resultWay != null){
            if ((resultWay.equals("push") || resultWay.equals("ping")) && notificationEndpoint != null) {
                if (!(notificationEndpoint.startsWith("http://") || notificationEndpoint.startsWith("https://"))) {
                    throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "invalid-hook-url", Response.Status.BAD_REQUEST);                }
            }
        }

        config = realm.addCibaNotificationConfig(cibaNotificationConfigModel);
        Map<String, Object> representation = new HashMap<>();
        representation.put("clientId", client.getClientId());
        representation.put("config", rep.getConfig());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.BO_NOTIFICATION_CONFIG).resourcePath(session.getContext().getUri(), config.getId()).representation(representation).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    @Path("dead-letter-hook-notification-config/{providerid}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createDeadLetterHookNotificationConfig(@PathParam("providerid") String id, DeadLetterHookNotificationConfigRepresentation rep) {
        auth.realm().requireManageRealm();

        CibaDeadLetterHookConfigModel config = realm.getDeadLetterHookNotificationConfigByProviderId(id, client.getClientId(), realm.getId());
        if (config != null) {
            return ErrorResponse.exists("The Backoffice Notification Provider " + id + " is already configured");
        }
        if(rep.getConfig() != null){
            rep.getConfig().forEach((key, value)-> {
                if(rep.getConfig().containsKey(key) && rep.getConfig().get(key) != null && GeneralHelper.validationForHtmlInjection(rep.getConfig().get(key))){
                    Map<String, Object> representation = new HashMap<>();
                    representation.put("clientId", client.getClientId());
                    representation.put("config", rep.getConfig());
                    adminEvent.operation(OperationType.CREATE_FAILED).resource(ResourceType.BO_NOTIFICATION_CONFIG).representation(representation).error(HTML_INJECTED_IN_FIELDS).success();
                    throw new javax.ws.rs.BadRequestException(INVALID_CONFIG_INPUT);
                }
            });
        }

        rep.setProviderId(id);
        CibaDeadLetterHookConfigModel cibaResultHookConfigModel = RepresentationToModel.toModel(rep);
        cibaResultHookConfigModel.setClientId(client.getClientId());
        cibaResultHookConfigModel.setRealmId(realm.getId());

        String notificationEndpoint = rep.getConfig().get("notification-endpoint");

        if (!(notificationEndpoint.startsWith("http://") || notificationEndpoint.startsWith("https://"))) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "invalid-hook-url", Response.Status.BAD_REQUEST);
        }

        config = realm.addCibaResultHookNotificationConfig(cibaResultHookConfigModel);
        Map<String, Object> representation = new HashMap<>();
        representation.put("clientId", client.getClientId());
        representation.put("config", rep.getConfig());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.BO_NOTIFICATION_CONFIG).resourcePath(session.getContext().getUri(), config.getId()).representation(representation).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    /**
     * Get MFA Configuration of given providerId
     *
     * @param id
     * @return
     */
    @Path("trx-notification-config/{providerid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public CibaNotificationConfigRepresentation getCibaNotificationConfig(@PathParam("providerid") String id) {
        auth.realm().requireViewRealm();

        CibaNotificationConfigModel config = realm.getCibaNotificationConfigByProviderId(id, client.getClientId(), realm.getId());
        if (config == null) {
            return new CibaNotificationConfigRepresentation();
        }
        return ModelToRepresentation.toRepresentation(session, config);
    }

    @Path("dead-letter-hook-notification-config/{providerid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public DeadLetterHookNotificationConfigRepresentation getDeadLetterHookNotificationConfig(@PathParam("providerid") String id) {
        auth.realm().requireViewRealm();

        CibaDeadLetterHookConfigModel config = realm.getDeadLetterHookNotificationConfigByProviderId(id, client.getClientId(), realm.getId());
        if (config == null) {
            return new DeadLetterHookNotificationConfigRepresentation();
        }
        return ModelToRepresentation.toRepresentation(session, config);
    }

    /**
     * Delete MFA configuration
     *
     * @param id Configuration id
     */
    @Path("trx-notification-config/{providerid}")
    @DELETE
    @NoCache
    public void removeCibaNotificationConfig(@PathParam("providerid") String id) {
        auth.realm().requireManageRealm();

        CibaNotificationConfigModel config = realm.getCibaNotificationConfigByProviderId(id, client.getClientId(), realm.getId());
        if (config == null) {
            throw new NotFoundException("Could not find backoffice notification config");
        }

        //remove the MFA Configs
        realm.removeCibaNotificationConfig(config);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.BO_NOTIFICATION_CONFIG).resourcePath(session.getContext().getUri()).success();
    }

    @Path("dead-letter-hook-notification-config/{providerid}")
    @DELETE
    @NoCache
    public void removeCibaResultHookNotificationConfig(@PathParam("providerid") String id) {
        auth.realm().requireManageRealm();

        CibaDeadLetterHookConfigModel config = realm.getDeadLetterHookNotificationConfigByProviderId(id, client.getClientId(), realm.getId());
        if (config == null) {
            throw new NotFoundException("Could not find backoffice notification config");
        }

        realm.removeDeadLetterHookNotificationConfig(config);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.BO_NOTIFICATION_CONFIG).resourcePath(session.getContext().getUri()).success();
    }

    @Path("trx-notification-config/ondemand/{providerid}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public GeneralResponse getCibaOnDemandNotConfig(@PathParam("providerid") String id, HashMap<String, Object> config) {
        auth.realm().requireViewRealm();
        UserModel user;

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            logger.info("getCibaOnDemandNotConfig::AuthResult is null");
            user = auth.adminAuth().getUser();
        } else {
            user = authResult.getUser();
        }
        Boolean isAccountCreator = user.getFirstAttribute("qv_is_account_creator") != null && Boolean.parseBoolean(user.getFirstAttribute("qv_is_account_creator"));

        if (config.get("notification-response-way") != null && config.get("notification-response-way").equals("encrypted") && (!Config.getAdminRealm().equals(auth.adminAuth().getRealm().getName())) && !isAccountCreator){
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName("secret-key");
            property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            property.setHelpText("Not Authorized to view secret");
            property.setLabel("secret-key-label");
            List<ProviderConfigProperty> providerConfigProperties = new ArrayList<>();
            providerConfigProperties.add(property);
            GeneralResponse generalResponse = new GeneralResponse();
            generalResponse.setStatus(403);
            generalResponse.setBody(providerConfigProperties);
            return generalResponse;
        }

        CibaNotificationProviderFactory cibaAuthProviderFactory = (CibaNotificationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaNotificationProvider.class, id);
        List<ProviderConfigProperty> result = cibaAuthProviderFactory.getOnDemandConfig(session, config);
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

    @Path("dead-letter-hook-notification-config/ondemand/{providerid}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public GeneralResponse getDeadLetterHookOnDemandNotConfig(@PathParam("providerid") String id, HashMap<String, Object> config) {
        auth.realm().requireViewRealm();
        UserModel user;

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            logger.info("getCibaOnDemandNotConfig::AuthResult is null");
            user = auth.adminAuth().getUser();
        } else {
            user = authResult.getUser();
        }
        Boolean isAccountCreator = user.getFirstAttribute("qv_is_account_creator") != null && Boolean.parseBoolean(user.getFirstAttribute("qv_is_account_creator"));

        if (config.get("notification-response-way") != null && config.get("notification-response-way").equals("encrypted") && (!Config.getAdminRealm().equals(auth.adminAuth().getRealm().getName())) && !isAccountCreator){
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName("secret-key");
            property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            property.setHelpText("Not Authorized to view secret");
            property.setLabel("secret-key-label");
            List<ProviderConfigProperty> providerConfigProperties = new ArrayList<>();
            providerConfigProperties.add(property);
            GeneralResponse generalResponse = new GeneralResponse();
            generalResponse.setStatus(403);
            generalResponse.setBody(providerConfigProperties);
            return generalResponse;
        }

        CibaDeadLetterHookNotificationProviderFactory cibaResultHookProviderFactory = (CibaDeadLetterHookNotificationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaDeadLetterHookNotificationProvider.class, id);
        List<ProviderConfigProperty> result = cibaResultHookProviderFactory.getOnDemandConfig(session, config);
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
     * Update MFA configuration
     *
     * @param id  Configuration id
     * @param rep JSON describing new state of authenticator configuration
     */
    @Path("trx-notification-config/{providerid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void updateCibaNotificationConfig(@PathParam("providerid") String id, CibaNotificationConfigRepresentation rep) throws Exception {
        auth.realm().requireManageRealm();
        CibaNotificationConfigModel exists = realm.getCibaNotificationConfigByProviderId(id, client.getClientId(), realm.getId());
        if (exists == null) {
            exists = realm.getCibaNotificationConfigByClientId(client.getClientId(), realm.getId());
            if (exists != null) {
                realm.removeCibaNotificationConfig(exists);
            }
            createCibaNotificationConfig(id, rep);
            client.setAttribute(CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, id);
            return;
        }
        if(rep.getConfig() != null){
            rep.getConfig().forEach((key, value)-> {
                if(rep.getConfig().containsKey(key) && rep.getConfig().get(key) != null && GeneralHelper.validationForHtmlInjection(rep.getConfig().get(key))){
                    Map<String, Object> representation = new HashMap<>();
                    representation.put("clientId", client.getClientId());
                    representation.put("config", rep.getConfig());
                    adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.BO_NOTIFICATION_CONFIG).representation(representation).error(HTML_INJECTED_IN_FIELDS).success();
                    throw new javax.ws.rs.BadRequestException(INVALID_CONFIG_INPUT);
                }
            });
        }

        String notificationEndpoint = rep.getConfig().get("notification-endpoint");
        String resultWay = rep.getConfig().containsKey("hook-way") ? rep.getConfig().get("hook-way") : rep.getConfig().get("queue-way");
        final String existingNotificationEndpoint = exists.getConfig().get("notification-endpoint");

        if(existingNotificationEndpoint == null && notificationEndpoint == null) {
            resetClientHookFailures();
        }

        if (existingNotificationEndpoint != null &&
                !existingNotificationEndpoint.equalsIgnoreCase(notificationEndpoint)) {
            resetClientHookFailures();
        }

        if(resultWay != null){
            if ((resultWay.equals("push") || resultWay.equals("ping")) && notificationEndpoint != null) {
                if (!(notificationEndpoint.startsWith("http://") || notificationEndpoint.startsWith("https://"))) {
                    throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "invalid-hook-url", Response.Status.BAD_REQUEST);                }
            }
        }

        CibaNotificationProviderFactory providerFactory = (CibaNotificationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaNotificationProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        Map<String, String> updatedConfig = GeneralHelper.removeInvalidSecrets(providerFactory.getConfigProperties(session), exists.getConfig(), rep.getConfig());
        exists.setConfig(RepresentationToModel.removeEmptyString(updatedConfig));
        realm.updateCibaNotificationConfig(exists);
        String authFlowId = client.getAttribute(CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT);
        if (!authFlowId.equals(id)) {
            client.setAttribute(CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, id);
        }
        Map<String, Object> representation = new HashMap<>();
        representation.put("clientId", client.getClientId());
        representation.put("config", rep.getConfig());
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.BO_NOTIFICATION_CONFIG).resourcePath(session.getContext().getUri()).representation(representation).success();
    }

    @Path("dead-letter-hook-notification-config/{providerid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void updateDeadLetterHookNotificationConfig(@PathParam("providerid") String id, DeadLetterHookNotificationConfigRepresentation rep) throws Exception {
        auth.realm().requireManageRealm();
        CibaDeadLetterHookConfigModel exists = realm.getDeadLetterHookNotificationConfigByProviderId(id, client.getClientId(), realm.getId());
        if (exists == null) {
            exists = realm.getDeadLetterHookNotificationConfigByClientId(client.getClientId(), realm.getId());
            if (exists != null) {
                realm.removeDeadLetterHookNotificationConfig(exists);
            }
            createDeadLetterHookNotificationConfig(id, rep);
            client.setAttribute(CIBA_BACKCHANNEL_DEAD_LETTER_HOOK_DELIVERY_MODE_PER_CLIENT, id);
            return;
        }
        if(rep.getConfig() != null){
            rep.getConfig().forEach((key, value)-> {
                if(rep.getConfig().containsKey(key) && rep.getConfig().get(key) != null && GeneralHelper.validationForHtmlInjection(rep.getConfig().get(key))){
                    Map<String, Object> representation = new HashMap<>();
                    representation.put("clientId", client.getClientId());
                    representation.put("config", rep.getConfig());
                    adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.BO_NOTIFICATION_CONFIG).representation(representation).error(HTML_INJECTED_IN_FIELDS).success();
                    throw new javax.ws.rs.BadRequestException(INVALID_CONFIG_INPUT);
                }
            });
        }

        String notificationEndpoint = rep.getConfig().get("notification-endpoint");
        final String existingNotificationEndpoint = exists.getConfig().get("notification-endpoint");

        if(existingNotificationEndpoint == null && notificationEndpoint == null) {
            resetClientHookFailures();
        }

        if (existingNotificationEndpoint != null &&
                !existingNotificationEndpoint.equalsIgnoreCase(notificationEndpoint)) {
            resetClientHookFailures();
        }

        if (notificationEndpoint != null && !(notificationEndpoint.startsWith("http://") || notificationEndpoint.startsWith("https://"))) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "invalid-hook-url", Response.Status.BAD_REQUEST);
        }


        CibaDeadLetterHookNotificationProviderFactory providerFactory = (CibaDeadLetterHookNotificationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaDeadLetterHookNotificationProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        Map<String, String> updatedConfig = GeneralHelper.removeInvalidSecrets(providerFactory.getConfigProperties(session), exists.getConfig(), rep.getConfig());
        exists.setConfig(RepresentationToModel.removeEmptyString(updatedConfig));
        realm.updateCibaResultHookNotificationConfig(exists);
        String authFlowId = client.getAttribute(CIBA_BACKCHANNEL_DEAD_LETTER_HOOK_DELIVERY_MODE_PER_CLIENT);
        if (rep.getConfig().get("enable") != null && Boolean.parseBoolean(rep.getConfig().get("enable"))) {
            client.setAttribute(CIBA_BACKCHANNEL_DEAD_LETTER_HOOK_DELIVERY_MODE_PER_CLIENT, id);
        } else {
            client.setAttribute(CIBA_BACKCHANNEL_DEAD_LETTER_HOOK_DELIVERY_MODE_PER_CLIENT, "");
        }
        Map<String, Object> representation = new HashMap<>();
        representation.put("clientId", client.getClientId());
        representation.put("config", rep.getConfig());
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.BO_NOTIFICATION_CONFIG).resourcePath(session.getContext().getUri()).representation(representation).success();
    }

    private void resetClientHookFailures() {
        if (!client.isEnabled() && isMaximumHookFailureReached(session, realm, client)) {
            client.setEnabled(true);
        }
        client.removeAttribute(FAILED_HOOK_ATTEMPTS);
        client.removeAttribute("hook_disabled_mail_sent");
    }


    @Path("trx-auth-config-description/{providerId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public AuthenticatorConfigInfoRepresentation getCibaAuthConfigDescription(@PathParam("providerId") String providerId) {
        auth.realm().requireViewRealm();

        CibaAuthProviderFactory factory = (CibaAuthProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaAuthProvider.class, providerId);
        if (factory == null) {
            throw new NotFoundException("Could not find sms provider");
        }
        AuthenticatorConfigInfoRepresentation rep = new AuthenticatorConfigInfoRepresentation();
        rep.setProviderId(providerId);
        rep.setName(factory.getDisplayName());
        rep.setTemplateSupported(factory.isTemplateSupported());

        rep.setHasTestConfig(false);
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

    @Path("trx-auth-config/ondemand/{providerid}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public GeneralResponse getCibaOnDemandConfig(@PathParam("providerid") String id, HashMap<String, Object> config) {
        auth.realm().requireViewRealm();
        UserModel user;

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            logger.info("getCibaOnDemandConfig::AuthResult is null");
            user = auth.adminAuth().getUser();
        } else {
            user = authResult.getUser();
        }
        Boolean isAccountCreator = user.getFirstAttribute("qv_is_account_creator") != null && Boolean.parseBoolean(user.getFirstAttribute("qv_is_account_creator"));

        if (!Config.getAdminRealm().equals(auth.adminAuth().getRealm().getName()) && !isAccountCreator && (config.get("integration-type") != null && !config.get("integration-type").equals("ezto-verify-app"))) {
            encrytionDependencyMap.put(ENCRYPTION_ENABLE, "true");
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName("secret-key");
            property.setType(ProviderConfigProperty.NOTIFICATION_WARN);
            property.setHelpText("Not Authorized to view secret");
            property.setLabel("secret-key-label");
            property.setDependsOn(encrytionDependencyMap);
            List<ProviderConfigProperty> providerConfigProperties = new ArrayList<>();
            providerConfigProperties.add(property);
            GeneralResponse generalResponse = new GeneralResponse();
            generalResponse.setStatus(403);
            generalResponse.setBody(providerConfigProperties);
            return generalResponse;
        }

        CibaAuthProviderFactory cibaAuthProviderFactory = (CibaAuthProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaAuthProvider.class, id);
        List<ProviderConfigProperty> result = cibaAuthProviderFactory.getOnDemandConfig(session, config);
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
     * Create MFA configuration
     *
     * @param id
     * @param rep
     * @return
     */
    @Path("trx-auth-config/{providerid}")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createCibaAuthConfig(@PathParam("providerid") String id, CibaAuthConfigRepresentation rep) {
        auth.realm().requireManageRealm();

        CibaAuthConfigModel config = realm.getCibaAuthConfigByProviderId(id, client.getClientId(), realm.getId());
        if (config != null) {
            return ErrorResponse.exists("The Ciba Auth Provider " + id + " is already configured");
        }
        if(rep.getConfig() != null){
            rep.getConfig().forEach((key, value)-> {
                if(rep.getConfig().containsKey(key) && rep.getConfig().get(key) != null && GeneralHelper.validationForHtmlInjection(rep.getConfig().get(key))){
                    Map<String, Object> representation = new HashMap<>();
                    representation.put("clientId", client.getClientId());
                    representation.put("config", rep.getConfig());
                    adminEvent.operation(OperationType.CREATE_FAILED).resource(ResourceType.BO_AUTH_CONFIG).representation(representation).error(HTML_INJECTED_IN_FIELDS).success();
                    throw new javax.ws.rs.BadRequestException(INVALID_CONFIG_INPUT);
                }
            });
        }

        rep.setProviderId(id);
        CibaAuthConfigModel cibaAuthConfigModel = RepresentationToModel.toModel(rep);
        cibaAuthConfigModel.setClientId(client.getClientId());
        cibaAuthConfigModel.setRealmId(realm.getId());

        CibaAuthProviderFactory providerFactory = (CibaAuthProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaAuthProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }
        Map<String, String> hiddenConfig = providerFactory.getHiddenConfig(session);
        if (hiddenConfig != null) {
            cibaAuthConfigModel.getConfig().putAll(hiddenConfig);
        }
        config = realm.addCibaAuthConfig(cibaAuthConfigModel);
        client.setAttribute(CIBA_EXPIRES_IN, rep.getConfig().get("verify-expiry-time"));
        providerFactory.onSave(session, (HashMap<String, String>) cibaAuthConfigModel.getConfig());
        Map<String, Object> representation = new HashMap<>();
        representation.put("clientId", client.getClientId());
        representation.put("config", rep.getConfig());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.BO_AUTH_CONFIG).resourcePath(session.getContext().getUri(), config.getId()).representation(representation).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(config.getId()).build()).build();
    }

    /**
     * Get MFA Configuration of given providerId
     *
     * @param id
     * @return
     */
    @Path("trx-auth-config/{providerid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public CibaAuthConfigRepresentation getCibaAuthConfig(@PathParam("providerid") String id) {
        auth.realm().requireViewRealm();

        CibaAuthConfigModel config = realm.getCibaAuthConfigByProviderId(id, client.getClientId(), realm.getId());
        if (config == null) {
            return new CibaAuthConfigRepresentation();
        }

        return ModelToRepresentation.toRepresentation(session, config);
    }

    /**
     * Delete MFA configuration
     *
     * @param id Configuration id
     */
    @Path("trx-auth-config/{providerid}")
    @DELETE
    @NoCache
    public void removeCibaAuthConfig(@PathParam("providerid") String id) {
        auth.realm().requireManageRealm();
        CibaAuthProviderFactory providerFactory = (CibaAuthProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaAuthProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }
        CibaAuthConfigModel config = realm.getCibaAuthConfigByProviderId(id, client.getClientId(), realm.getId());
        if (config == null) {
            throw new NotFoundException("Could not find trx auth config");
        }

        //remove the MFA Configs
        realm.removeCibaAuthConfig(config);
        providerFactory.onDelete(session, (HashMap<String, String>) config.getConfig());
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.BO_AUTH_CONFIG).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Update MFA configuration
     *
     * @param id  Configuration id
     * @param rep JSON describing new state of authenticator configuration
     */
    @Path("trx-auth-config/{providerid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public void updateCibaAuthConfig(@PathParam("providerid") String id, CibaAuthConfigRepresentation rep) throws Exception {
        auth.realm().requireManageRealm();

        CibaAuthConfigModel exists = realm.getCibaAuthConfigByProviderId(id, client.getClientId(), realm.getId());
        if(rep.getConfig() != null){
            rep.getConfig().forEach((key, value)-> {
                if(rep.getConfig().containsKey(key) && rep.getConfig().get(key) != null && GeneralHelper.validationForHtmlInjection(rep.getConfig().get(key))){
                    Map<String, Object> representation = new HashMap<>();
                    representation.put("clientId", client.getClientId());
                    representation.put("config", rep.getConfig());
                    adminEvent.operation(OperationType.UPDATE_FAILED).resource(ResourceType.BO_AUTH_CONFIG).representation(representation).error(HTML_INJECTED_IN_FIELDS).success();
                    throw new javax.ws.rs.BadRequestException(INVALID_CONFIG_INPUT);
                }
            });
        }
        if (exists == null) {
            exists = realm.getCibaAuthConfigByClientId(client.getClientId(), realm.getId());
            if (exists != null)
                realm.removeCibaAuthConfig(exists);
            createCibaAuthConfig(id, rep);
            client.setAuthenticationFlowBindingOverride(CIBA_AUTHORIZER, id);
            return;
        }

        CibaAuthProviderFactory providerFactory = (CibaAuthProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(CibaAuthProvider.class, id);
        if (providerFactory == null) {
            throw new NotFoundException("Could not find provider factory");
        }

        Map<String, String> updatedConfig = GeneralHelper.removeInvalidSecrets(providerFactory.getConfigProperties(session), exists.getConfig(), rep.getConfig());

        if(rep.getConfig().get(INTEGRATION_TYPE) != null && rep.getConfig().get(INTEGRATION_TYPE).equals(SDK) && rep.getConfig().get("deep-linking").equals("true")){
            if(!(rep.getConfig().get("app-store-link").startsWith("https://") && rep.getConfig().get("play-store-link").startsWith("https://"))){
                throw new ErrorResponseException("invalid-app-store-url", "invalid-app-store-url", Response.Status.BAD_REQUEST);
            }
        }

        providerFactory.onUpdate(session, (HashMap<String, String>) exists.getConfig(), (HashMap<String, String>) updatedConfig);
        exists.setConfig(RepresentationToModel.removeEmptyString(updatedConfig));
        client.setAttribute(CIBA_EXPIRES_IN, rep.getConfig().get("verify-expiry-time"));
        realm.updateCibaAuthConfig(exists);
        String authFlowId = client.getAuthenticationFlowBindingOverride(CIBA_AUTHORIZER);
        if (!authFlowId.equals(id)) {
            client.setAuthenticationFlowBindingOverride(CIBA_AUTHORIZER, id);
        }

        Map<String, Object> representation = new HashMap<>();
        representation.put("clientId", client.getClientId());
        representation.put("config", rep.getConfig());
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.BO_AUTH_CONFIG).resourcePath(session.getContext().getUri()).representation(representation).success();
    }

    @Path("/duplicate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response duplicateClient(Map<String, String> clientDetails) {
        ClientModel appToBeDuplicated = realm.getClientByClientId(clientDetails.get("clientId"));
        String newAppName = clientDetails.get("newAppName");
        try {
            GeneralHelper.validationsForClientCreation(realm, adminEvent, newAppName);

            newAppName = GeneralHelper.checkAppName(newAppName);
            ClientRepresentation newRep = new ClientRepresentation();
            newRep.setName(newAppName);

            newRep.setAppType(GeneralHelper.getAppType(appToBeDuplicated));
            if (newRep.getAppType().equals(Constants.CIBA) && !session.rootBrand().isCibaFeatureEnabled(realm)) {
                return ErrorResponse.error("License doesn't support this app type CIBA. Check with sales", Response.Status.BAD_REQUEST);
            }
            newRep.setEnabled(appToBeDuplicated.isEnabled());
            newRep.setProtocol(appToBeDuplicated.getProtocol());

            newRep.setConsentRequired(appToBeDuplicated.isConsentRequired());
            Map<String, String> attributes = appToBeDuplicated.getAttributes();
            newRep.setAttributes(attributes);
            newRep.setSandboxMode(appToBeDuplicated.isSandboxMode());

            newRep.setAuthenticationFlowBindingOverrides(appToBeDuplicated.getAuthenticationFlowBindingOverrides());
            session.clientPolicy().triggerOnEvent(new AdminClientRegisterContext(newRep, auth.adminAuth()));

            ClientModel newApp = ClientManager.createClient(session, realm, newRep, true);
            if(!newApp.getRedirectUris().isEmpty()) {
                List<String> redirectUris = new ArrayList<>(newApp.getRedirectUris());
                newRep.setRedirectUris(redirectUris);
                newApp.setRedirectUris(appToBeDuplicated.getRedirectUris());
                RepresentationToModel.addRedirectUrisAndSetupWebOrigins(newApp, newRep);
            }
            newApp.setOverrideAuthentication(appToBeDuplicated.isOverrideAuthentication());
            setUpConfigs(realm, appToBeDuplicated, newApp);
            setUpAppTypes(newApp, newRep);

            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), newApp.getId()).representation(newRep).success();

            ValidationUtil.validateClient(session, newApp, true, r -> {
                session.getTransactionManager().setRollbackOnly();
                throw new ErrorResponseException(
                        Errors.INVALID_INPUT,
                        r.getAllLocalizedErrorsAsString(AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale())),
                        Response.Status.BAD_REQUEST);
            });

            session.getContext().setClient(newApp);
            session.clientPolicy().triggerOnEvent(new AdminClientRegisteredContext(newApp, auth.adminAuth()));

            GeneralResponse response = new GeneralResponse();
            response.setStatus(200);
            response.setBody(newApp.getClientId());
            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(newApp.getClientId()).build()).entity(response).build();
        } catch (IllegalArgumentException e) {
            logger.error(String.format("Error while duplicating the app with name %s : %s", newAppName, e.getMessage()));
            return ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (ClientRegistrationPolicyException e) {
            logger.error(String.format("Error while duplicating the app with name %s : %s", newAppName, e.getMessage()));
            return ErrorResponse.error(e.getDetailMessage(), Response.Status.BAD_REQUEST);
        } catch (RuntimeException e) {
            logger.error(String.format("Error while duplicating the app with name %s : %s", newAppName, e.getMessage()));
            return ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (ClientPolicyException e) {
            e.printStackTrace();
            logger.error(String.format("Error while duplicating the app %s : %s", appToBeDuplicated.getClientId(), e.getErrorDetail()));
            return ErrorResponse.error("Failed to duplicate application", Response.Status.BAD_REQUEST);
        }
    }

    @Path("/change-sandbox-mode")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response changeSandboxMode(Map<String, Object> clientDetails) {
        String clientId = clientDetails.get("clientId").toString();
        ClientModel client = realm.getClientByClientId(clientId);

        if(client == null) {
            return ErrorResponse.error("Application not found", Response.Status.BAD_REQUEST);
        }

        boolean isSandboxMode = Boolean.parseBoolean(clientDetails.get("sandboxMode").toString());

        client.setSandboxMode(isSandboxMode);

        GeneralResponse response = new GeneralResponse();
        response.setStatus(200);
        response.setBody(clientId);
        return Response.ok(session.getContext().getUri().getAbsolutePathBuilder().path(clientId).build()).entity(response).build();
    }

    private void updateClientFromRep(ClientRepresentation rep, ClientModel client, KeycloakSession session) throws ModelDuplicateException {
        UserModel serviceAccount = this.session.users().getServiceAccount(client);
        if (TRUE.equals(rep.isServiceAccountsEnabled())) {
            if (serviceAccount == null) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            }
        } else {
            if (serviceAccount != null) {
                new UserManager(session).removeUser(realm, serviceAccount);
            }
        }

        if (rep.getClientId() != null && !rep.getClientId().equals(client.getClientId())) {
            new ClientManager(new RealmManager(session)).clientIdChanged(client, rep);
        }

        if (rep.isFullScopeAllowed() != null && rep.isFullScopeAllowed() != client.isFullScopeAllowed()) {
            auth.clients().requireManage(client);
        }

        if ((rep.isBearerOnly() != null && rep.isBearerOnly()) || (rep.isPublicClient() != null && rep.isPublicClient())) {
            rep.setAuthorizationServicesEnabled(false);
        }

        updateSSO(rep, client, session);
        RepresentationToModel.updateClient(rep, client);
        RepresentationToModel.updateClientProtocolMappers(rep, client);
        updateAuthorizationSettings(rep);
        if (rep.getAppType().equals(Constants.IWS)) {

            VaultProvider vaultProvider = getVaultProvider(session, client);
            boolean hasCred = vaultProvider.hasSecret(false);
            if (rep.getAttributes().get("iws.credtype").equalsIgnoreCase("iws_admin_set_pass")) {
                String adminUsername = rep.getAttributes().get("iws.admin.user");
                String adminPass = rep.getAttributes().get("iws.admin.pass");
                //here add to vault
                if (hasCred) {
                    VaultResponse vaultResponse = vaultProvider.obtainSecret(false);
                    try {
                        String secret = new String(vaultResponse.getSecret().get().get().array(), StandardCharsets.UTF_8);
                        String maskedPass = org.keycloak.authentication.authenticators.utils.GeneralHelper.maskString(secret, 0, secret.length(), '*');
                        if (maskedPass.equalsIgnoreCase(adminPass) && adminUsername.equalsIgnoreCase(vaultResponse.getVaultSecretId()))
                            return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                boolean isCreated = vaultProvider.writeUserSecret(adminUsername, DefaultVaultRawSecret.forBuffer(Optional.of(ByteBuffer.wrap(String.valueOf(adminPass).getBytes(StandardCharsets.UTF_8)))), false);
                if (!isCreated) {
                    logger.info("Cannot create admin creds for the IWS application");
                }
                try {
                    client.setAttribute("iws.admin.pass", org.keycloak.authentication.authenticators.utils.GeneralHelper.maskString(adminPass, 0, adminPass.length(), '*'));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (hasCred) {
                    vaultProvider.removeSecret(false);
                    rep.getAttributes().remove("iws.admin.user");
                    rep.getAttributes().remove("iws.admin.pass");
                    client.removeAttribute("iws.admin.user");
                    client.removeAttribute("iws.admin.pass");
                }
            }
        }
    }

    private VaultProvider getVaultProvider(KeycloakSession kcSession, ClientModel clientModel) {
        VaultProvider vaultProvider = kcSession.getProvider(VaultProvider.class, "winlogon-vault");
        vaultProvider.setClientModel(clientModel);
        return vaultProvider;

    }

    private void updateSSO(ClientRepresentation clientRepresentation, ClientModel clientModel, KeycloakSession session) {
        String ssoString = clientModel.getAttribute("isSSO");
        if (ssoString != null && Boolean.parseBoolean(ssoString)) {
            String templateId = clientModel.getAttribute("templateId");
            if (templateId != null) {
                MarketplaceTemplateProvider marketplaceProvider = session.getProvider(MarketplaceTemplateProvider.class, templateId);
                if (marketplaceProvider != null) {
                    Map<String, String> config = new HashMap<>();
                    MarketplaceTemplateProviderFactory providerFactory = (MarketplaceTemplateProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(MarketplaceTemplateProvider.class, templateId);
                    config.putAll(clientRepresentation.getAttributes().entrySet().stream().filter(x -> x.getKey().startsWith("edit_")).collect(Collectors.toMap(e -> e.getKey().replaceAll("edit_", ""), e -> e.getValue())));
                    List<String> errors = providerFactory.validateConfig(session, config, true);
                    if (errors.size() > 0)
                        throw new ErrorResponseException(
                                Errors.INVALID_CONFIG,
                                String.join("\n", errors),
                                Response.Status.BAD_REQUEST);

                    config.putAll(clientRepresentation.getAttributes().entrySet().stream().filter(x -> x.getKey().startsWith("edit_")).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
                    try {
                        marketplaceProvider.update(session, clientRepresentation, clientModel, config);
                    } catch (MarketplaceException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void updateAuthorizationSettings(ClientRepresentation rep) {
        if (Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) {
            if (TRUE.equals(rep.getAuthorizationServicesEnabled())) {
                authorization().enable(false);
            } else {
                authorization().disable();
            }
        }
    }

    /**
     * Converts the specified {@link UserSessionModel} into a {@link UserSessionRepresentation}.
     *
     * @param userSession the model to be converted.
     * @return a reference to the constructed representation.
     */
    private UserSessionRepresentation toUserSessionRepresentation(final UserSessionModel userSession) {
        UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(userSession);

        // Update lastSessionRefresh with the timestamp from clientSession
        Map.Entry<String, AuthenticatedClientSessionModel> result = userSession.getAuthenticatedClientSessions().entrySet().stream()
                .filter(entry -> Objects.equals(client.getId(), entry.getKey()))
                .findFirst().orElse(null);
        if (result != null) {
            rep.setLastAccess(Time.toMillis(result.getValue().getTimestamp()));
        }
        return rep;
    }

    private static ClientScopeRepresentation toRepresentation(ClientScopeModel clientScopeModel) {
        ClientScopeRepresentation rep = new ClientScopeRepresentation();
        rep.setId(clientScopeModel.getId());
        rep.setName(clientScopeModel.getName());
        return rep;
    }


    private boolean isTestPossible(Map<String, String> config){
        String hookWay = config.get(HOOK_WAY);
        return (!(SDK).equals(realm.getCibaAuthConfigByClientId(this.client.getClientId(), realm.getId()).getConfig().get(CIBA_NOTIFY))
                && !session.getContext().getClient().getAuthenticationFlowBindingOverride(CIBA_AUTHORIZER).equals(AZURE_B2C_CIBA_AUTHORIZER)
                && realm.getCibaNotificationConfigByClientId(this.client.getClientId(), realm.getId()).getCibaNotificationProviderId().equals(HOOK_NOTIFIER)
                && (hookWay.equals(POLL) || hookWay.equals(PING) || hookWay.equals(PUSH)));
    }
    private String getAuthWayMessage(Map<String, String> authConfig, JSONObject transactionResponse){
        String authWay = session.getContext().getClient().getAuthenticationFlowBindingOverride(CIBA_AUTHORIZER);

        if (authWay == null) {
            return null;
        }

        switch (authWay) {
            case TWILIO_IVR_CIBA_AUTHORIZER:
                return AUTH_WAY_IVR_MSG;
            case CIBA_DEFAULT_MANAGED_AUTH:
                if (authConfig.containsKey(CIBA_NOTIFY)) {
                    switch (authConfig.get(CIBA_NOTIFY)) {
                        case EMAIL:
                            return AUTH_WAY_EMAIL_MSG;
                        case SMS:
                            return AUTH_WAY_SMS_MSG;
                        case WHATSAPP:
                            return AUTH_WAY_WHATSAPP_MSG;
                        case LINK:
                            return AUTH_WAY_LINK_MSG;
                        default:
                            return null;
                    }
                }
                break;
            case CIBA_DESKTOP_SDK:
                return transactionResponse.has(TRANSACTION_LINK) ? AUTH_WAY_LINK_MSG : AUTH_WAY_DESKTOP_MSG;
            case CIBA_MOBILE_SDK:
                return (transactionResponse.has("qr") || (authConfig.containsKey(PUSH_WAY) && authConfig.get(PUSH_WAY).equals(QR))) ? AUTH_WAY_QR_MSG : AUTH_WAY_MOBILE_MSG;
        }

        return null;
    }

    private String getResultDeliveryModeMessage(String authWay){
        switch (authWay){
            case HOOK_POLL:
                return NOTIFICATION_WAY_POLL_MSG;
            case HOOK_PUSH:
                return NOTIFICATION_WAY_PUSH_MSG;
            case HOOK_PING:
                return NOTIFICATION_WAY_PING_MSG;
            case HOOK_MOBILE_SDK:
                return NOTIFICATION_WAY_MOBILE_MSG;
            case QUEUE_KAFKA:
                return NOTIFICATION_WAY_KAFKA_MSG;
            default:
                return null;
        }
    }
}

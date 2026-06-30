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

import io.swagger.models.Model;
import org.apache.commons.io.FilenameUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.Query;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.admin.AuthorizationService;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.events.Errors;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.forms.sso.SSOProvider;
import org.keycloak.forms.sso.freemarker.FreeMarkerSSOProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientRegisteredContext;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;
import org.keycloak.services.managers.*;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.sso.SSOResponse;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.GeneralHelper;
import org.keycloak.validation.ValidationUtil;
import org.keycloak.vault.DefaultVaultRawSecret;
import org.keycloak.vault.VaultProvider;

import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.keycloak.models.Constants.HIGH_SECURE;
import static org.keycloak.utils.GeneralHelper.*;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 * Base resource class for managing a realm's clients.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 * @resource Clients
 */
public class ClientsResource {
    protected static final Logger logger = Logger.getLogger(ClientsResource.class);
    protected RealmModel realm;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public ClientsResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT);

    }

    /**
     * Get clients belonging to the realm.
     * <p>
     * If a client can't be retrieved from the storage due to a problem with the underlying storage,
     * it is silently removed from the returned list.
     * This ensures that concurrent modifications to the list don't prevent callers from retrieving this list.
     *
     * @param clientId     filter by clientId
     * @param viewableOnly filter clients that cannot be viewed in full by admin
     * @param search       whether this is a search query or a getClientById query
     * @param firstResult  the first result
     * @param maxResults   the max results to return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<ClientRepresentation> getClients(@QueryParam("clientId") String clientId,
                                                   @QueryParam("clientName") String clientName,
                                                   @QueryParam("viewableOnly") @DefaultValue("false") boolean viewableOnly,
                                                   @QueryParam("search") @DefaultValue("false") boolean search,
                                                   @QueryParam("q") String searchQuery,
                                                   @QueryParam("first") Integer firstResult,
                                                   @QueryParam("max") Integer maxResults,
                                                   @QueryParam("type") String type,
                                                   @QueryParam("orderBy") String orderBy,
                                                   @QueryParam("sandboxMode") String sandboxMode,
                                                   @QueryParam("isApiClient")  @DefaultValue("false") boolean isApiClient) {
        auth.clients().requireList();

        boolean canView = auth.clients().canView();
        Stream<ClientModel> clientModels;
        firstResult = (firstResult == null || firstResult < 0) ? 0 : firstResult;
        maxResults = (maxResults == null || maxResults < 0) ? Constants.DEFAULT_MAX_RESULTS : maxResults;

        List<String> defClients = new ArrayList<>(Constants.defaultClients);
        List<String> inBClients = new ArrayList<>(Constants.inBuildClients);
        if (realm.getMode().equals("wiam")) {
            defClients.removeAll(inBClients);
        }
        clientModels = realm.getClientsStream(orderBy);

        if ((clientId == null || clientId.trim().equals("")) && (clientName == null || clientName.trim().equals(""))) {
            clientModels = canView
                    ? clientModels
                    .filter(c -> !(defClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                    .filter(c -> c.getAttribute("isSSO") == null)
                    : realm.getClientsStream(orderBy)
                    .filter(c -> !defClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT))
                    .filter(c -> c.getAttribute("isSSO") == null);
            auth.clients().requireList();
        }
        else if (search) {
            if (clientId != null && clientId.length() > 0) {
                clientModels = canView
                        ? realm.searchClientByClientIdStream(clientId, -1, -1)
                        .filter(c -> !(defClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                        .filter(c -> c.getAttribute("isSSO") == null)
                        : realm.searchClientByClientIdStream(clientId, -1, -1)
                        .filter(c -> !(defClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                        .filter(c -> c.getAttribute("isSSO") == null);
            } else if (clientName != null && clientName.length() > 0) {
                clientModels = canView
                        ? realm.searchClientByClientNameStream(clientName, -1, -1)
                        .filter(c -> !(defClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                        .filter(c -> c.getAttribute("isSSO") == null)
                        : realm.searchClientByClientNameStream(clientName, -1, -1)
                        .filter(c -> !(defClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                        .filter(c -> c.getAttribute("isSSO") == null);
            }
        } else {
            ClientModel client = realm.getClientByClientId(clientId);
            if (client != null) {
                clientModels = Stream.of(client);
            }
            clientModels = clientModels.filter(c -> !(defClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                    .filter(c -> c.getAttribute("isSSO") == null);
        }

        if (type != null && !type.isEmpty()) {
            clientModels = clientModels.filter(
                    clientModel -> {
                        if (type.equalsIgnoreCase("in-built")) {
                            return ModelToRepresentation.toRepresentation(clientModel, session).getAttributes().get(
                                    "isInBuilt") != null && ModelToRepresentation.toRepresentation(clientModel, session).getAttributes().get(
                                    "isInBuilt").equalsIgnoreCase("true");
                        } else if (type.equalsIgnoreCase("high-secure")) {
                            return ModelToRepresentation.toRepresentation(clientModel, session).getAppType() != null
                                    && ModelToRepresentation.toRepresentation(clientModel, session).getAppType()
                                    .equalsIgnoreCase(HIGH_SECURE)
                                    && ModelToRepresentation.toRepresentation(clientModel, session).getAttributes().get(
                                    "isInBuilt") == null;
                        } else {
                            return ModelToRepresentation.toRepresentation(clientModel, session).getAppType() != null
                                    && !(clientModel.getAttributes().containsKey("isInBuilt") && clientModel.getAttribute("isInBuilt").equals("true"))
                                    && ModelToRepresentation.toRepresentation(clientModel, session).getAppType()
                                    .equalsIgnoreCase(type.replace("-","_"));
                        }
                    }
            );
        }

        if (!isApiClient) {
            clientModels = clientModels.filter(clientModel -> !"api-authorizer".equals(clientModel.
                    getAuthenticationFlowBindingOverrides().get("ciba_auth")));
        } else {
            clientModels = clientModels.filter(clientModel -> "api-authorizer".equals(clientModel.
                    getAuthenticationFlowBindingOverrides().get("ciba_auth")));
            clientModels = clientModels.map(clientModel -> {
                clientModel.setAttribute("client-secret", clientModel.getSecret());
                return clientModel;
            });
            sandboxMode = null;
        }

        if(sandboxMode != null && !sandboxMode.isEmpty()){
            clientModels = GeneralHelper.getAppBySandboxMode(session, clientModels, sandboxMode);
        }

        clientModels = clientModels.skip(firstResult).limit(maxResults);

        if ("asc".equals(orderBy))
            clientModels = clientModels.sorted(Comparator.comparing((ClientModel c) -> c.getName().toLowerCase()));
        else if ("desc".equals(orderBy)) {
            clientModels = clientModels.sorted(Comparator.comparing((ClientModel c) -> c.getName().toLowerCase())
                    .reversed());
        }

        Stream<ClientRepresentation> s = clientModels.map(
                c -> {
                    ClientRepresentation representation = null;
                    if (canView || auth.clients().canView(c)) {
                        representation = ModelToRepresentation.toRepresentation(c, session);
                        representation.setAccess(auth.clients().getAccess(c));
                    } else if (!viewableOnly && auth.clients().canView(c)) {
                        representation = new ClientRepresentation();
                        representation.setId(c.getId());
                        representation.setClientId(c.getClientId());
                        representation.setDescription(c.getDescription());
                    }
                    return representation;
                }).filter(Objects::nonNull);

        if (!canView) {
            s = paginatedStream(s, firstResult, maxResults);
        }

        return s;
    }

    @Path("count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Long getClientsCount(@QueryParam("clientId") String clientId,
                                @QueryParam("clientName") String clientName,
                                @QueryParam("search") @DefaultValue("false") boolean search,
                                @QueryParam("type") String type,
                                @QueryParam("sandboxMode") String sandboxMode,
                                @QueryParam("isApiClient") @DefaultValue("false") boolean isApiClient) {

        Stream<ClientModel> clientModels = Stream.empty();
        List<String> defClients = new ArrayList<>(Constants.defaultClients);
        List<String> inBClients = new ArrayList<>(Constants.inBuildClients);
        if (realm.getMode().equals("wiam")) {
            defClients.removeAll(inBClients);
        }


        if ((clientId == null || clientId.trim().equals("")) && (clientName == null || clientName.trim().equals(""))) {
            clientModels = realm.getClientsStream()
                    .filter(c -> !(defClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                    .filter(c -> c.getAttribute("isSSO") == null);
            auth.clients().requireList();
        } else if (search) {
            if (clientId != null && clientId.length() > 0) {
                clientModels = realm.searchClientByClientIdStream(clientId, -1, -1)
                        .filter(c -> !(defClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                        .filter(c -> c.getAttribute("isSSO") == null);
            } else if (clientName != null && clientName.length() > 0) {
                clientModels = realm.searchClientByClientNameStream(clientName, -1, -1)
                        .filter(c -> !(defClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                        .filter(c -> c.getAttribute("isSSO") == null);
            }
        } else {
            ClientModel client = realm.getClientByClientId(clientId);
            if (client != null) {
                clientModels = Stream.of(client);
            }
        }
        if (type != null && !type.isEmpty()) {
            clientModels = clientModels.filter(
                    clientModel -> {
                        if (type.equalsIgnoreCase("in-built")) {
                            return ModelToRepresentation.toRepresentation(clientModel, session).getAttributes().get(
                                    "isInBuilt") != null && ModelToRepresentation.toRepresentation(clientModel, session).getAttributes().get(
                                    "isInBuilt").equalsIgnoreCase("true");
                        } else if (type.equalsIgnoreCase("high-secure")) {
                            return ModelToRepresentation.toRepresentation(clientModel, session).getAppType() != null
                                    && ModelToRepresentation.toRepresentation(clientModel, session).getAppType()
                                    .equalsIgnoreCase(HIGH_SECURE)
                                    && ModelToRepresentation.toRepresentation(clientModel, session).getAttributes().get(
                                    "isInBuilt") == null;
                        } else {
                            return ModelToRepresentation.toRepresentation(clientModel, session).getAppType() != null
                                    && !(clientModel.getAttributes().containsKey("isInBuilt") && clientModel.getAttribute("isInBuilt").equals("true"))
                                    && ModelToRepresentation.toRepresentation(clientModel, session).getAppType()
                                    .equalsIgnoreCase(type.replace("-","_"));
                        }
                    }
            );
        }

        if(sandboxMode != null && !sandboxMode.isEmpty()){
            clientModels = GeneralHelper.getAppBySandboxMode(session, clientModels, sandboxMode);
        }

        if (!isApiClient) {
            clientModels = clientModels.filter(clientModel -> !"api-authorizer".equals(clientModel.
                    getAuthenticationFlowBindingOverrides().get("ciba_auth")));
        } else {
            clientModels = clientModels.filter(clientModel -> "api-authorizer".equals(clientModel.
                    getAuthenticationFlowBindingOverrides().get("ciba_auth")));
        }

        return clientModels.count();
    }

    @POST
    @Path("{client_id}/upload_picture")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveImageAndReturnUrl(@PathParam("client_id") String clientId,
                                          @QueryParam("type") String type,
                                          final MultipartFormDataInput input) throws IOException {
        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();

        InputPart file;
        String fileName = clientId;
        if(formDataMap.containsKey(fileName)) {
            file = formDataMap.get(clientId).get(0);
        } else {
            return ErrorResponse.error("Missing Parameters", Response.Status.BAD_REQUEST);
        }

        // checking file extension
        String extension = FilenameUtils.getExtension(getFileName(file.getHeaders()));
        if (!checkForCorrectExtension(extension))
            return ErrorResponse.error("Unsupported File Type", Response.Status.BAD_REQUEST);

        fileName = KeycloakModelUtils.generateId() + "." + extension;
        String fileContentType = file.getHeaders().get("Content-Type").get(0);

        if(extension.equals("svg") && (!GeneralHelper.isValidSvgFile(file))){
            return ErrorResponse.error("Upload a proper image file", Response.Status.BAD_REQUEST);
        }

        // converting file into InputStream
        InputStream inputStream = file.getBody(InputStream.class, null);

        InputStream imageStream = file.getBody(InputStream.class, null);
        BufferedImage image = ImageIO.read(imageStream);

        if (image == null && !extension.equals("svg")) {
            return ErrorResponse.error("Upload a proper image file", Response.Status.BAD_REQUEST);
        }

        // Form a ObjectPath to store object in specified path
        String objectPath = "public/client-logos/" + type + "/";
        String url = session.getProvider(AvatarProvider.class, "avatar-storage-minio")
                .saveImgAndReturnUrl(realm.getName(), objectPath, fileName, fileContentType, inputStream);
        String returningUrl = "{\"url\": \"" + url + "\"}";
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().build()).entity(returningUrl).type(MediaType.APPLICATION_JSON).build();
    }

    @Path("sso/apps")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public SSOResponse getSSOApps(@Context HttpRequest req) {
        if (session.getContext().getRealm().getMode().equalsIgnoreCase("ciam"))
            throw new NotFoundException();
        auth.clients().canList();
        FreeMarkerSSOProvider ssoProvider = (FreeMarkerSSOProvider) session.getProvider(SSOProvider.class).setRealm(realm).setUriInfo(session.getContext().getUri());
        List<String> tokens = req.getHttpHeaders().getRequestHeader(AUTHORIZATION);
        if (tokens != null) {
            AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
            if (authResult != null) {
                Auth auth = new Auth(realm, authResult.getToken(), authResult.getUser(), authResult.getClient(),
                        authResult.getSession(), true);
                ssoProvider.setAuth(auth);
            } else {
                ssoProvider.setAdminPermissionEvaluator(auth);
            }
        }

        return new SSOResponse() {{
            setSsoApps(ssoProvider.getSSO());
            setUser(ModelToRepresentation.toBriefRepresentation(auth.adminAuth().getUser()));
            setRealm(ModelToRepresentation.toBriefRepresentation(realm, false, session));
            setBranding(session.rootBrand().getAdminBrandAttributes(realm));
        }};
    }


    @Path("sso/count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Long getSSOCount(@QueryParam("clientId") String clientId,
                            @QueryParam("clientName") String clientName,
                            @QueryParam("search") @DefaultValue("false") boolean search) {
        if (session.getContext().getRealm().getMode().equalsIgnoreCase("ciam"))
            throw new NotFoundException();
        Stream<ClientModel> clientModels = Stream.empty();

        if ((clientId == null || clientId.trim().equals("")) && (clientName == null || clientName.trim().equals(""))) {
            clientModels = realm.getClientsStream()
                    .filter(c -> c.getAttribute("isSSO") != null);
            auth.clients().requireList();
        } else if (search) {
            if (clientId != null && clientId.length() > 0) {
                clientModels = realm.searchClientByClientIdStream(clientId, -1, -1)
                        .filter(c -> c.getAttribute("isSSO") != null);
            } else if (clientName != null && clientName.length() > 0) {
                clientModels = realm.searchClientByClientNameStream(clientName, -1, -1)
                        .filter(c -> c.getAttribute("isSSO") != null);
            }
        } else {
            ClientModel client = realm.getClientByClientId(clientId);
            if (client != null) {
                clientModels = Stream.of(client);
            }
        }

        return clientModels.count();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Path("sso")
    public Stream<ClientRepresentation> getSSOClients(@QueryParam("clientId") String clientId,
                                                      @QueryParam("clientName") String clientName,
                                                      @QueryParam("viewableOnly") @DefaultValue("false") boolean viewableOnly,
                                                      @QueryParam("search") @DefaultValue("false") boolean search,
                                                      @QueryParam("first") Integer firstResult,
                                                      @QueryParam("max") Integer maxResults,
                                                      @QueryParam("orderBy") String orderBy) {
        if (session.getContext().getRealm().getMode().equalsIgnoreCase("ciam"))
            throw new NotFoundException();

        boolean canView = auth.clients().canView();
        Stream<ClientModel> clientModels = Stream.empty();
        firstResult = (firstResult == null || firstResult < 0) ? 0 : firstResult;
        maxResults = (maxResults == null || maxResults < 0) ? Constants.DEFAULT_MAX_RESULTS : maxResults;

        List<String> defClients = new ArrayList<>(Constants.defaultClients);
        List<String> inBClients = new ArrayList<>(Constants.inBuildClients);
        defClients.removeAll(inBClients);

        if ((clientId == null || clientId.trim().equals("")) && (clientName == null || clientName.trim().equals(""))) {
            clientModels = canView
                    ? realm.getClientsStream(orderBy)
                    .filter(c -> c.getAttribute("isSSO") != null)
                    .skip(firstResult)
                    .limit(maxResults)
                    : realm.getClientsStream(orderBy)
                    .filter(c -> c.getAttribute("isSSO") != null);
            auth.clients().requireList();
        } else if (search) {
            if (clientId != null && clientId.length() > 0) {
                clientModels = canView
                        ? realm.searchClientByClientIdStream(clientId, -1, -1)
                        .filter(c -> c.getAttribute("isSSO") != null)
                        .skip(firstResult)
                        .limit(maxResults)
                        : realm.searchClientByClientIdStream(clientId, -1, -1)
                        .filter(c -> c.getAttribute("isSSO") != null);
            } else if (clientName != null && clientName.length() > 0) {
                clientModels = canView
                        ? realm.searchClientByClientNameStream(clientName, -1, -1)
                        .filter(c -> c.getAttribute("isSSO") != null)
                        .skip(firstResult)
                        .limit(maxResults)
                        : realm.searchClientByClientNameStream(clientName, -1, -1)
                        .filter(c -> c.getAttribute("isSSO") != null);
            }
        } else {
            ClientModel client = realm.getClientByClientId(clientId);
            if (client != null) {
                clientModels = Stream.of(client);
            }
        }

        if ("asc".equals(orderBy))
            clientModels = clientModels.sorted(Comparator.comparing((ClientModel c) -> c.getName().toLowerCase()));
        else if ("desc".equals(orderBy)) {
            clientModels = clientModels.sorted(Comparator.comparing((ClientModel c) -> c.getName().toLowerCase())
                    .reversed());
        }

        Stream<ClientRepresentation> s = clientModels
                .map(c -> {
                    ClientRepresentation representation = null;
                    if (canView || auth.clients().canView(c)) {
                        representation = ModelToRepresentation.toRepresentation(c, session);
                        representation.setAccess(auth.clients().getAccess(c));
                    } else if (!viewableOnly && auth.clients().canView(c)) {
                        representation = new ClientRepresentation();
                        representation.setId(c.getId());
                        representation.setClientId(c.getClientId());
                        representation.setDescription(c.getDescription());
                    }

                    return representation;
                })
                .filter(Objects::nonNull);

        if (!canView) {
            if (firstResult != null && firstResult > 0) {
                s = s.skip(firstResult);
            }
            if (maxResults != null && maxResults > 0) {
                s = s.limit(maxResults);
            }
        }

        return s;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Path("sso/{client_id}/metadata")
    public Map<String, Object> getSSOMetadata(@PathParam("client_id") String clientId) throws ProcessingException, CertificateEncodingException, NoSuchAlgorithmException, ProcessingException {
        if (session.getContext().getRealm().getMode().equalsIgnoreCase("ciam"))
            throw new NotFoundException();

        Map<String, Object> metadata = new HashMap<>();

        ClientModel clientModel = realm.getClientById(clientId);
        if (clientModel == null) {
            throw new NotFoundException("Client Not Found");
        }
        List<String> signingKeys = session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256)
                .sorted(SamlService::compareKeys)
                .map(key -> {
                    return PemUtils.encodeCertificate(key.getCertificate());
                })
                .collect(Collectors.toList());

        UriInfo uriInfo = session.getContext().getUri();

        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN CERTIFICATE-----\n").append(signingKeys.get(0)).append("\n-----END CERTIFICATE-----");
        metadata.put("x509", builder.toString());
        metadata.put("loginEndpoint", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString() + "/" + clientModel.getClientId());
        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        UriBuilder frontendUriBuilder = RealmsResource.protocolUrl(frontendUriInfo);
        metadata.put("logoutEndpoint", frontendUriBuilder.clone().path(OIDCLoginProtocolService.class, "logout").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString() + "?redirect_uri=" + uriInfo.getBaseUri().toString() + "realms/" + realm.getName() + "/sso");
        metadata.put("logoutEndpointPost", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString() + "/" + clientModel.getClientId());
        metadata.put("entityId", RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString());
        metadata.put("thumbPrint_sha_1", getThumbPrint(XMLSignatureUtil.getX509CertificateFromKeyInfoString(signingKeys.get(0)), "SHA-1"));
        metadata.put("thumbPrint_sha_256", getThumbPrint(XMLSignatureUtil.getX509CertificateFromKeyInfoString(signingKeys.get(0)), "SHA-256"));
        metadata.put("thumbPrint_sha_384", getThumbPrint(XMLSignatureUtil.getX509CertificateFromKeyInfoString(signingKeys.get(0)), "SHA-384"));
        metadata.put("thumbPrint_sha_512", getThumbPrint(XMLSignatureUtil.getX509CertificateFromKeyInfoString(signingKeys.get(0)), "SHA-512"));
        metadata.put("x509DownloadUrl", "/realms/" + realm.getName() + "/sso/download");
        metadata.put("metaDataDownloadUrl", "/realms/" + realm.getName() + "/sso/" + clientId + "/download-metadata");
        metadata.put("scriptDownloadUrl", clientModel.getAttribute("hasScript") != null && clientModel.getAttribute("hasScript").equals("true") ? "/realms/" + realm.getName() + "/sso/" + clientId + "/script/download" : null);
        return metadata;
    }


    private String getThumbPrint(X509Certificate cert, String algorithm)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        return hexify(digest);
    }

    private String hexify(byte bytes[]) {

        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        StringBuffer buf = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; ++i) {
            buf.append(Character.toUpperCase(hexDigits[(bytes[i] & 0xf0) >> 4]));
            buf.append(Character.toUpperCase(hexDigits[bytes[i] & 0x0f]));
            if (!(i + 1 >= bytes.length)) {
                buf.append(":");
            }
        }

        return buf.toString();
    }

    private AuthorizationService getAuthorizationService(ClientModel clientModel) {
        return new AuthorizationService(session, clientModel, auth, adminEvent);
    }

    /**
     * Create a new client
     * <p>
     * Client's client_id must be unique!
     *
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClient(final ClientRepresentation rep) {
        auth.clients().requireManage();

        try {
            GeneralHelper.validationsForClientCreation(realm, adminEvent, rep.getName());
            rep.setName(GeneralHelper.checkAppName(rep.getName()));

            if (rep.getAppType().equals(HIGH_SECURE) || rep.getAppType().equals(Constants.BROWSER_BASED) || rep.getAppType().equals(Constants.REST_API) || rep.getAppType().equals(Constants.M2M)) {
                if (!session.rootBrand().isOIDCEnabled(realm)) {
                    return ErrorResponse.error("License doesnt support this app type " + rep.getAppType() + ". Check with sales", Response.Status.BAD_REQUEST);
                }
            }


            if (rep.getAppType().equals(Constants.IWS)) {
                if (!session.rootBrand().isIWSFeatureEnabled(realm)) {
                    return ErrorResponse.error("License doesnt support this app type IWS. Check with sales", Response.Status.BAD_REQUEST);
                }


                if (rep.getAttributes().get("iws.credtype").equalsIgnoreCase("iws_admin_set_pass")) {
                    String adminUsername = rep.getAttributes().get("iws.admin.user");
                    String adminPass = rep.getAttributes().get("iws.admin.pass");
                    if (!(adminUsername != null && adminPass != null && !adminUsername.isEmpty() && !adminPass.isEmpty())) {
                        return ErrorResponse.error("Admin Credentials are missing for the app", Response.Status.BAD_REQUEST);
                    }
                }
            }

            if (rep.getAppType().equals(Constants.CIBA)) {
                if (!session.rootBrand().isCibaFeatureEnabled(realm)) {
                    return ErrorResponse.error("License doesnt support this app type IWS. Check with sales", Response.Status.BAD_REQUEST);
                }
            }

            ClientModel clientModel = null;
            session.clientPolicy().triggerOnEvent(new AdminClientRegisterContext(rep, auth.adminAuth()));

            try {
                clientModel = ClientManager.createClient(session, realm, rep, true);
            } catch (ClientRegistrationPolicyException e) {
                return ErrorResponse.error(e.getDetailMessage(), Response.Status.BAD_REQUEST);
            } catch (RuntimeException re) {
                return ErrorResponse.error(re.getMessage(), Response.Status.BAD_REQUEST);
            }
            setUpAppTypes(clientModel, rep);
            if (rep.getAppType().equals(Constants.IWS)) {
                clientModel.setAttribute("iws.enable", "true");
                if (!rep.getAttributes().containsKey("iws.credtype")) {
                    clientModel.setAttribute("iws.credtype", "iws_user_set_pass");
                }

                clientModel.setAttribute("vault", "true");
                clientModel.setAttribute("sso.show", "true");


                if (rep.getAttributes().get("iws.credtype").equalsIgnoreCase("iws_admin_set_pass")) {
                    String adminUsername = rep.getAttributes().get("iws.admin.user");
                    String adminPass = rep.getAttributes().get("iws.admin.pass");
                    //here add to vault
                    VaultProvider vaultProvider = getVaultProvider(session, clientModel);
                    boolean isCreated = vaultProvider.writeUserSecret(adminUsername, DefaultVaultRawSecret.forBuffer(Optional.of(ByteBuffer.wrap(String.valueOf(adminPass).getBytes(StandardCharsets.UTF_8)))), false);
                    if (!isCreated) {
                        logger.info("Cannot create admin creds for the IWS application");
                    }
                    try {
                        clientModel.setAttribute("iws.admin.pass", org.keycloak.authentication.authenticators.utils.GeneralHelper.maskString(adminPass, 0, adminPass.length(), '*'));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //This required to identity who is doing the access
                clientModel.addProtocolMapper(HardcodedClaim.create("App Name Mapper", "app_name", clientModel.getName(), ProviderConfigProperty.STRING_TYPE, true, false));

                String roleName = "sso-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-role";
                if (clientModel.getProtocol().equals("saml")) {
                    roleName = "sso-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-" + clientModel.getAttribute("sso_type") + "-role";
                }

                String iwsEnableAttribute = clientModel.getAttribute("iws.enable");
                if (iwsEnableAttribute != null && iwsEnableAttribute.equalsIgnoreCase("true")) {
                    roleName = "iws-" + clientModel.getName().trim().toLowerCase(Locale.ROOT).replace(" ", "_") + "-role";
                }

                if (clientModel.getAttribute("sso.show") != null && clientModel.getAttribute("sso.show").equalsIgnoreCase("true")) {
                    if (realm.getRole(roleName) == null) {
                        realm.addRole(roleName);
                    }
                    ActionModel finalActModel = null;
                    ClientModel finalClientModel = clientModel;
                    Optional<ActionModel> actionModel = realm.getActionStream().filter(x -> x.getAlias().equals(finalClientModel.getClientId() + "_action")).findFirst();
                    if (!actionModel.isPresent()) {
                        finalActModel = new ActionModel();
                        finalActModel.setAlias(clientModel.getClientId() + "_action");
                        finalActModel.setInBuilt(true);
                        finalActModel = realm.addAction(finalActModel);
                    } else {
                        finalActModel = actionModel.get();
                    }

                    PolicyModel triggerPolicyModel = (PolicyModel) session.policies().getPolicyByName(realm, clientModel.getClientId() + "_template_trigger_policy");
                    if (triggerPolicyModel == null) {
                        triggerPolicyModel = new PolicyModel();
                        triggerPolicyModel.setPolicyName(clientModel.getClientId() + "_template_trigger_policy");
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
                }
            }

            if (TRUE.equals(rep.isServiceAccountsEnabled())) {
                UserModel serviceAccount = session.users().getServiceAccount(clientModel);

                if (serviceAccount == null) {
                    new ClientManager(new RealmManager(session)).enableServiceAccount(clientModel);
                }
            }

            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), clientModel.getId()).representation(rep).success();

            if (TRUE.equals(rep.getAuthorizationServicesEnabled())) {
                AuthorizationService authorizationService = getAuthorizationService(clientModel);

                authorizationService.enable(true);

                ResourceServerRepresentation authorizationSettings = rep.getAuthorizationSettings();

                if (authorizationSettings != null) {
                    authorizationService.resourceServer().importSettings(authorizationSettings);
                }
            }

            ValidationUtil.validateClient(session, clientModel, true, r -> {
                session.getTransactionManager().setRollbackOnly();
                throw new ErrorResponseException(
                        Errors.INVALID_INPUT,
                        r.getAllLocalizedErrorsAsString(AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale())),
                        Response.Status.BAD_REQUEST);
            });

            session.getContext().setClient(clientModel);
            session.clientPolicy().triggerOnEvent(new AdminClientRegisteredContext(clientModel, auth.adminAuth()));

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(clientModel.getId()).build()).build();
        } catch (IllegalArgumentException e) {
            logger.error("Error while creating the app with name %s : %s" + rep.getName() + e.getMessage());
            return ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client " + rep.getClientId() + " already exists");
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Base path for managing a specific client.
     *
     * @param id id of client (not client-id)
     * @return
     */
    @Path("{id}")
    public ClientResource getClient(final @PathParam("id") String id) {

        ClientModel clientModel = realm.getClientById(id);
        if (clientModel == null) {
            // we do this to make sure somebody can't phish ids
            clientModel = realm.getClientByClientId(id);
        }
        if (clientModel == null) {
            // we do this to make sure somebody can't phish ids
            if (auth.clients().canList()) throw new NotFoundException("Could not find client");
            else throw new ForbiddenException();
        }

        session.getContext().setClient(clientModel);

        ClientResource clientResource = new ClientResource(realm, auth, clientModel, session, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientResource);
        return clientResource;
    }

    private VaultProvider getVaultProvider(KeycloakSession kcSession, ClientModel clientModel) {
        VaultProvider vaultProvider = kcSession.getProvider(VaultProvider.class, "winlogon-vault");
        vaultProvider.setClientModel(clientModel);
        return vaultProvider;

    }
}

package org.keycloak.services.resources.sso;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.UriUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.downloads.ConfigDownloadProvider;
import org.keycloak.downloads.DownloadProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.sso.SSOProvider;
import org.keycloak.forms.sso.freemarker.FreeMarkerSSOProvider;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.locale.LocaleUpdaterProvider;
import org.keycloak.marketplace.template.SSOBean;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.protocol.saml.IDPMetadataDescriptor;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.representations.IDToken;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.AbstractSecuredLocalService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.theme.Theme;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.w3c.dom.Element;
import static org.apache.http.util.TextUtils.isBlank;
import static org.keycloak.models.Constants.OFFICE_365_SCRIPT;
import static org.keycloak.services.resources.account.AccountFormService.ACCOUNT_MGMT_FORWARDED_ERROR_NOTE;

/**
 * Created by st on 29/03/17.
 */
public class SSOConsole extends AbstractSecuredLocalService {
    private static final Logger logger = Logger.getLogger(SSOConsole.class);

    private static Set<String> VALID_PATHS = new HashSet<>();

    static {
        for (Method m : SSOConsole.class.getMethods()) {
            Path p = m.getAnnotation(Path.class);
            if (p != null) {
                VALID_PATHS.add(p.value());
            }
        }
    }

    @Context
    protected KeycloakSession session;

    private final AppAuthManager authManager;
    private final RealmModel realm;
    private final ClientModel client;
    private final Theme theme;
    private EventStoreProvider eventStore;
    private EventBuilder eventBuilder;
    private AdminEventBuilder adminEvent;
    private SSOProvider ssoProvider;

    private Auth auth;

    public SSOConsole(RealmModel realm, ClientModel client, Theme theme) {
        super(realm, client);
        this.realm = realm;
        this.client = client;
        this.theme = theme;
        this.authManager = new AppAuthManager();
    }

    public void init() {
        eventStore = session.getProvider(EventStoreProvider.class);

        ssoProvider = session.getProvider(SSOProvider.class).setRealm(realm).setUriInfo(session.getContext().getUri()).setHttpHeaders(headers);

        this.eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());


        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm);
        if (authResult != null) {
            stateChecker = (String) session.getAttribute("state_checker");
            auth = new Auth(realm, authResult.getToken(), authResult.getUser(), client, authResult.getSession(), true);
            ssoProvider.setStateChecker(stateChecker);
        }
        try {
            this.adminEvent = new AdminEventBuilder(realm, new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client), session, clientConnection);
        }
        catch (Exception err)
        {
            // Here we ignore it
        }
        String requestOrigin = UriUtils.getOrigin(session.getContext().getUri().getBaseUri());

        String origin = headers.getRequestHeaders().getFirst("Origin");
        if (origin != null && !requestOrigin.equals(origin)) {
            if (!origin.equalsIgnoreCase("null")) {
                throw new ForbiddenException();
            }
        }

        if (!request.getHttpMethod().equals("GET")) {
            String referrer = headers.getRequestHeaders().getFirst("Referer");
            if (referrer != null && !requestOrigin.equals(UriUtils.getOrigin(referrer))) {
                throw new ForbiddenException();
            }
        }

        if (authResult != null) {
            UserSessionModel userSession = authResult.getSession();
            if (userSession != null) {
                AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
                asm.setAuthSessionCookie(userSession.getId(), realm);
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
                if (clientSession == null) {
                    clientSession = session.sessions().createClientSession(userSession.getRealm(), client, userSession);
                }
                auth.setClientSession(clientSession);
            }

            ssoProvider.setUser(auth.getUser());
        }

        if (auth != null) {
            ((FreeMarkerSSOProvider) ssoProvider).setAuth(auth);
            ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionScopeParameter(auth.getClientSession(), session);
            IDToken idToken = new TokenManager().responseBuilder(realm, client, new EventBuilder(realm, session,
                            session.getContext().getConnection()),
                    session,
                    auth.getSession(),
                    clientSessionCtx).accessToken(authResult.getToken()).generateIDToken().getIdToken();
            idToken.issuedFor(client.getClientId());
            ((FreeMarkerSSOProvider) ssoProvider).setIdToken(session.tokens().encodeAndEncrypt(idToken));
        }
    }

    private Response forwardToPage(String path) {
        if (auth != null) {
            try {
                if (!auth.hasOneOfRealmRole(AdminRoles.ADMIN, "user"))
                    throw new ForbiddenException();
            } catch (ForbiddenException e) {
                return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
            }

            setReferrerOnPage();

            UserSessionModel userSession = auth.getSession();

            String tabId = session.getContext().getUri().getQueryParameters().getFirst(Constants.TAB_ID);
            if (tabId != null) {
                AuthenticationSessionModel authSession = new AuthenticationSessionManager(session).getAuthenticationSessionByIdAndClient(realm, userSession.getId(), client, tabId);
                if (authSession != null) {
                    String forwardedError = authSession.getAuthNote(ACCOUNT_MGMT_FORWARDED_ERROR_NOTE);
                    if (forwardedError != null) {
                        try {
                            FormMessage errorMessage = JsonSerialization.readValue(forwardedError, FormMessage.class);
                            ssoProvider.setError(Response.Status.INTERNAL_SERVER_ERROR, errorMessage.getMessage(), errorMessage.getParameters());
                            authSession.removeAuthNote(ACCOUNT_MGMT_FORWARDED_ERROR_NOTE);
                        } catch (IOException ioe) {
                            throw new RuntimeException(ioe);
                        }
                    }
                }
            }

            String locale = session.getContext().getUri().getQueryParameters().getFirst(LocaleSelectorProvider.KC_LOCALE_PARAM);
            if (locale != null) {
                LocaleUpdaterProvider updater = session.getProvider(LocaleUpdaterProvider.class);
                updater.updateUsersLocale(auth.getUser(), locale);
            }

            return ssoProvider.createResponse();
        } else {
            return login(path);
        }
    }

    /**
     * Get account information.
     *
     * @return
     */
    @Path("/")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response ssoPage() {
        return forwardToPage(null);
    }

    @Path("/download")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response download() {
        if(auth == null)
            return login(null);

        try {
            if (!auth.hasOneOfRealmRole(AdminRoles.ADMIN, "user"))
                throw new ForbiddenException();
        } catch (ForbiddenException e) {
            if(adminEvent != null)
            {
                adminEvent.resource(ResourceType.CLIENT).operation(OperationType.DOWNLOAD_FAILED).error(Messages.NO_ACCESS).success();
            }
            return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
        }

        List<String> signingKeys = session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256)
                .sorted(SamlService::compareKeys)
                .map(key -> {
                    return PemUtils.encodeCertificate(key.getCertificate());
                })
                .collect(Collectors.toList());

        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN CERTIFICATE-----\n").append(signingKeys.get(0)).append("\n-----END CERTIFICATE-----");

        Response.ResponseBuilder response = Response.ok();
        response.header("Content-Disposition", "attachment; filename=\"certificate.pem\"");
        response.entity(builder.toString());

        if(adminEvent != null)
        {
            adminEvent.resource(ResourceType.CLIENT).operation(OperationType.DOWNLOAD).detail("file", "certificate.pem").success();
        }
        return response.build();
    }

    @Path("/downloads/download/{appname}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response downloadApp(@PathParam("appname") String appName) throws URISyntaxException {
        if(auth == null)
            return login(null);

        try {
            if (!auth.hasOneOfRealmRole(AdminRoles.ADMIN, "user"))
                throw new ForbiddenException();
        } catch (ForbiddenException e) {
            if(adminEvent != null)
            {
                adminEvent.resource(ResourceType.CLIENT).operation(OperationType.DOWNLOAD_FAILED).detail("appname", appName).success();
            }
            return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
        }

        DownloadProvider provider = session.getProvider(DownloadProvider.class,appName);
        if(provider == null) {
            if(adminEvent != null)
            {
                adminEvent.resource(ResourceType.CLIENT).operation(OperationType.DOWNLOAD_FAILED).detail("appname", appName).success();
            }
            return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
        }


        if(adminEvent != null)
        {
            adminEvent.resource(ResourceType.CLIENT).operation(OperationType.DOWNLOAD).detail("appname", appName).success();
        }
        return Response.status(302).location(
                new URI(provider.getDownloadDetails().getDownloadUrl())
        ).build();
    }

    @Path("/{clientId}/download-metadata")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getMetaData(@PathParam("clientId") String clientId){
        if(auth == null)
            return login(null);

        try {
            if (!auth.hasOneOfRealmRole(AdminRoles.ADMIN, "user"))
                throw new ForbiddenException();
        } catch (ForbiddenException e) {
            return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
        }
        ClientModel clientModel = realm.getClientById(clientId);
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        String loginEndpoint =  RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString() + "/" + clientModel.getClientId();
        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        UriBuilder frontendUriBuilder = RealmsResource.protocolUrl(frontendUriInfo);
        String logoutRedirect = frontendUriBuilder.clone().path(OIDCLoginProtocolService.class, "logout").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString() + "?redirect_uri=" + uriInfo.getBaseUri().toString() + "realms/" + realm.getName() + "/sso";
        try {
            List<Element> signingKeys = session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256)
                    .sorted(SamlService::compareKeys)
                    .map(key -> {
                        try {
                            return IDPMetadataDescriptor
                                    .buildKeyInfoElement(key.getKid(), PemUtils.encodeCertificate(key.getCertificate()));
                        } catch (ParserConfigurationException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());



            Response response = Response.ok().entity(IDPMetadataDescriptor.getIDPDescriptor(new URI(loginEndpoint),new URI(loginEndpoint),new URI(loginEndpoint),new URI(logoutRedirect),
                    RealmsResource.protocolUrl(uriInfo).path(SamlService.ARTIFACT_RESOLUTION_SERVICE_PATH).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL),
                    RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString(),
                    true,
                    signingKeys)).build();
            if(adminEvent != null)
            {
                adminEvent.resource(ResourceType.CLIENT).operation(OperationType.DOWNLOAD).detail("file", "download-metadata.xml").success();
            }
            return response;
        } catch (Exception ex) {
            logger.error("Cannot generate IdP metadata", ex);
            if(adminEvent != null)
            {
                adminEvent.resource(ResourceType.CLIENT).operation(OperationType.DOWNLOAD_FAILED).error(ex.getMessage()).success();
            }
            return Response.serverError().entity("Cannot generate IdP metadata").build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @NoCache
    @Path("/{client_id}/script/download")
    public Response getSSOScript(@PathParam("client_id") String clientId) throws Exception {

        if(auth == null)
            return login(null);

        try {
            if (!auth.hasOneOfRealmRole(AdminRoles.ADMIN, "user"))
                throw new ForbiddenException();
        } catch (ForbiddenException e) {
            if(adminEvent != null)
            {
                adminEvent.resource(ResourceType.CLIENT).operation(OperationType.DOWNLOAD_FAILED).error(Messages.NO_ACCESS).success();
            }
            return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
        }

        ClientModel clientModel = realm.getClientById(clientId);
        if (clientModel == null) {
            if(adminEvent != null)
            {
                adminEvent.resource(ResourceType.CLIENT).operation(OperationType.DOWNLOAD_FAILED).error("Client Not Found").success();
            }
            throw new NotFoundException("Client Not Found");
        }
        List<String> signingKeys = session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256)
                .sorted(SamlService::compareKeys)
                .map(key -> {
                    return PemUtils.encodeCertificate(key.getCertificate());
                })
                .collect(Collectors.toList());
        UriInfo uriInfo = session.getContext().getUri();
        String scriptUrl = clientModel.getAttribute("script_url");
        HashMap<String,String> mappers = new HashMap<>();
        mappers.put("@domainName",clientModel.getAttribute("edit_domain_name"));
        mappers.put("@entityId",RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString());
        mappers.put("@logoutEndpoint",RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString() + "/" + clientModel.getClientId());
        mappers.put("@loginEndpoint",RealmsResource.protocolUrl(uriInfo).build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL).toString() + "/" + clientModel.getClientId());
        mappers.put("@x509certificate",signingKeys.get(0));
        String script = getText(scriptUrl, mappers);
        String fileName = clientModel.getName().toLowerCase().trim().replace(" ","_")+".ps1";
        Response.ResponseBuilder response = Response.ok();
        response.header("Content-Disposition", "attachment; filename=\""+clientModel.getName().toLowerCase().trim().replace(" ","_")+".ps1\"");

        response.entity(script);
        if(adminEvent != null)
        {
            adminEvent.resource(ResourceType.CLIENT).operation(OperationType.DOWNLOAD).detail("file", fileName).success();
        }
        return response.build();
    }



    @Override
    protected Set<String> getValidPaths() {
        return SSOConsole.VALID_PATHS;
    }

    @Override
    protected URI getBaseRedirectUri() {
        return Urls.ssoBase(session.getContext().getUri().getBaseUri()).path("/").build(realm.getName());
    }

    private void setReferrerOnPage() {
        String[] referrer = getReferrer();
        if (referrer != null) {
            ssoProvider.setReferrer(referrer);
        }
    }

    private String getText(String url, HashMap<String,String> mappers) throws Exception {
        URL website = new URL(url);
        URLConnection connection = website.openConnection();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            String finalInputLine = inputLine;
            Optional<String> stringOptional = mappers.keySet().stream().filter(x -> finalInputLine.contains(x)).map(x->finalInputLine.replace(x,mappers.get(x))).findFirst();
            if(stringOptional.isPresent())
            {
                response.append(stringOptional.get()+"\n");
            }
            else {
                response.append(inputLine+"\n");
            }
        }

        in.close();

        return response.toString();
    }

    private String[] getReferrer() {
        String referrer = session.getContext().getUri().getQueryParameters().getFirst("referrer");
        if (referrer == null) {
            return null;
        }

        String referrerUri = session.getContext().getUri().getQueryParameters().getFirst("referrer_uri");

        ClientModel referrerClient = realm.getClientByClientId(referrer);
        if (referrerClient != null) {
            if (referrerUri != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(session, referrerUri, referrerClient);
            } else {
                referrerUri = ResolveRelative.resolveRelativeUri(session, referrerClient.getRootUrl(), referrerClient.getBaseUrl());
            }

            if (referrerUri != null) {
                String referrerName = referrerClient.getName();
                if (Validation.isBlank(referrerName)) {
                    referrerName = referrer;
                }
                return new String[]{referrerName, referrerUri};
            }
        } else if (referrerUri != null) {
            if (client != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(session, referrerUri, client);

                if (referrerUri != null) {
                    return new String[]{referrer, referrerUri};
                }
            }
        }

        return null;
    }

}

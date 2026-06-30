package org.keycloak.services.resources.download;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.common.util.UriUtils;
import org.keycloak.downloads.ConfigDownloadProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.AbstractSecuredLocalService;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.sso.SSOConsole;
import org.keycloak.theme.Theme;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class DownloadConsole extends AbstractSecuredLocalService {
    private static final Logger logger = Logger.getLogger(SSOConsole.class);

    private static Set<String> VALID_PATHS = new HashSet<>();

    static {
        for (Method m : DownloadConsole.class.getMethods()) {
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
    private EventStoreProvider eventStore;
    private EventBuilder eventBuilder;
    private AdminEventBuilder adminEvent;
    private Auth auth;

    public DownloadConsole(RealmModel realm, ClientModel client, Theme theme) {
        super(realm, client);
        this.realm = realm;
        this.client = client;
        this.authManager = new AppAuthManager();
    }

    public void init() {
        eventStore = session.getProvider(EventStoreProvider.class);

        this.eventBuilder = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());

        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm);
        if (authResult != null) {
            stateChecker = (String) session.getAttribute("state_checker");
            auth = new Auth(realm, authResult.getToken(), authResult.getUser(), client, authResult.getSession(), true);
            this.adminEvent = new AdminEventBuilder(realm, new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client), session, clientConnection);
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
        }
    }

    @Path("/")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response ssoPage() {
        return forwardToPage(null);
    }

    private Response forwardToPage(String path) {
        if (auth != null) {
            try {
                if (!auth.hasOneOfRealmRole(AdminRoles.ADMIN,AdminRoles.REALM_ADMIN))
                    throw new ForbiddenException();
            } catch (ForbiddenException e) {
                return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
            }

            return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
        } else {
            return login(path);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @NoCache
    @Path("/reports/{file_name}")
    public Response getSSOScript(@PathParam("file_name") String fileName) throws Exception {

        if(auth == null)
            return login(null);

        try {
            if (!auth.hasOneOfRealmRole(AdminRoles.ADMIN, "user"))
                throw new ForbiddenException();
        } catch (ForbiddenException e) {
            return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
        }
        AvatarProvider avatarProvider = session.getProvider(AvatarProvider.class, "avatar-storage-minio");
        if(adminEvent != null)
        {
            adminEvent.resource(ResourceType.REPORT).operation(OperationType.DOWNLOAD).detail("file", fileName).success();
        }
        return Response.ok(avatarProvider.loadFile(realm.getName(),fileName)).header("Content-Disposition", "attachment; filename="+fileName).build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @NoCache
    @Path("/provconf/{config}/{client_id}")
    public Response getConfigDownload(@PathParam("config") String config,@PathParam("client_id") String clientId) throws Exception {

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

        session.getContext().setRealm(auth.getRealm());

        ConfigDownloadProvider downloadProvider = session.getProvider(ConfigDownloadProvider.class, config);

        Response.ResponseBuilder response = Response.ok();
        response.header("Content-Disposition", "attachment; filename="+downloadProvider.getFileName()+"");

        response.entity(downloadProvider.getDownloadFile(clientModel));
        return response.build();
    }

    @Override
    protected Set<String> getValidPaths() {
        return VALID_PATHS;
    }

    @Override
    protected URI getBaseRedirectUri() {
        return Urls.downloadBase(session.getContext().getUri().getBaseUri()).path("/").build(realm.getName());
    }
}

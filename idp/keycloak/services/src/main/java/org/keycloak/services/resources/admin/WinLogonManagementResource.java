package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.TokenVerifier;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.AESHelper;
import org.keycloak.vault.DefaultVaultRawSecret;
import org.keycloak.vault.VaultProvider;
import org.keycloak.vault.VaultRawSecret;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.userprofile.UserProfileContext.USER_API;

public class WinLogonManagementResource {
    private static final Logger logger = Logger.getLogger(WinLogonManagementResource.class);

    protected RealmModel realm;

    private AdminPermissionEvaluator auth;

    private AdminEventBuilder adminEvent;
    private UserModel user;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected KeycloakSession session;

    @Context
    protected HttpHeaders headers;

    public WinLogonManagementResource(RealmModel realm, UserModel user, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.user = user;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

        @POST
    @Path("secret/take")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserWinLogonSecret(HashMap<String, Object> request) {
        auth.users().requireWinLoginUser(auth.adminAuth().getUser());

        String authUserId = auth.adminAuth().getUser().getId();
        if(!authUserId.equals(user.getId()))
        {
            throw new ForbiddenException();
        }

        Object clientId = request.get("client_id");

        if (clientId == null || String.valueOf(clientId).isEmpty())
            return Response.status(Response.Status.BAD_REQUEST).entity(new GeneralResponse<String>() {{
                setStatus(400);
                setBody("client_id is mandatory");
            }}).build();

        ClientModel clientModel = realm.getClientByClientId(String.valueOf(clientId));
        if (clientModel == null)
            return Response.status(Response.Status.BAD_REQUEST).entity(new GeneralResponse<String>() {{
                setStatus(404);
                setBody("Request client is not found");
            }}).build();

        VaultProvider vaultProvider = session.getProvider(VaultProvider.class, "winlogon-vault");
        vaultProvider.setUserModel(user).setClientModel(clientModel).setAdditionalPaths(request.get("additional_paths") != null ? ((ArrayList<String>)request.get("additional_paths")).toArray(new String[0]) : new String[]{});;
        VaultRawSecret rawSecret = vaultProvider.obtainUserSecret(false);
        if (rawSecret == null || !rawSecret.get().isPresent())
            return Response.status(Response.Status.NOT_FOUND).build();

        String secret = new String(rawSecret.get().get().array(), StandardCharsets.UTF_8);
            AuthenticationManager.AuthResult authResult = AppAuthManager.verifyIdentityToken(session, realm,
                    session.getContext().getUri(),
                session.getContext().getConnection(), true, false, null, false,
                session.getContext().getRequestHeaders().getHeaderString("Authorization").split(" ")[1],
                session.getContext().getRequestHeaders(),
                new TokenVerifier.TokenTypeCheck(TokenUtil.TOKEN_TYPE_BEARER));

        if(authResult != null)
        {
            String key = authResult.getSession().getId().replaceAll("-","");
            String encodedBase64Key = AESHelper.encodeKey(key);
            try {
                secret = AESHelper.encrypt(secret, encodedBase64Key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        AdminEventBuilder adminEventBuilder = adminEvent.authRealm(realm).resource(ResourceType.WINLOGON).representation(new HashMap<Object, Object>(){{
            put("UserId",user.getId());
            put("ClientId",clientId);
        }});
        adminEventBuilder.operation(OperationType.READ).success();
        String finalSecret = secret;
        return Response.ok(new GeneralResponse<String>() {{
            setStatus(200);
            setBody(finalSecret);
        }}).build();
    }

    @POST
    @Path("secret")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response writeUserSecret(HashMap<String, Object> request) {
        auth.users().requireWinLogonAdmin(auth.adminAuth().getUser());

        Object clientId = request.get("client_id");

        if (clientId == null || String.valueOf(clientId).isEmpty())
            return Response.status(Response.Status.BAD_REQUEST).entity(new GeneralResponse<String>() {{
                setStatus(400);
                setBody("client_id is mandatory");
            }}).build();

        ClientModel clientModel = realm.getClientByClientId(String.valueOf(clientId));
        if (clientModel == null)
            return Response.status(Response.Status.BAD_REQUEST).entity(new GeneralResponse<String>() {{
                setStatus(404);
                setBody("Request client is not found");
            }}).build();

        Object secret = request.get("secret");

        if (secret == null || String.valueOf(secret).isEmpty())
            return Response.status(Response.Status.BAD_REQUEST).entity(new GeneralResponse<String>() {{
                setStatus(400);
                setBody("secret is mandatory");
            }}).build();

        VaultProvider vaultProvider = session.getProvider(VaultProvider.class, "winlogon-vault");
        vaultProvider.setUserModel(user).setClientModel(clientModel).setAdditionalPaths(request.get("additional_paths") != null ? ((ArrayList<String>)request.get("additional_paths")).toArray(new String[0]) : new String[]{});
        boolean result = vaultProvider.writeUserSecret(DefaultVaultRawSecret.forBuffer(Optional.of(ByteBuffer.wrap(String.valueOf(secret).getBytes(StandardCharsets.UTF_8)))),false);
        AdminEventBuilder adminEventBuilder = adminEvent.authRealm(realm).resource(ResourceType.WINLOGON).representation(new HashMap<Object, Object>(){{
            put("UserId",user.getId());
            put("ClientId",clientId);
        }});
        if(result) {
            adminEventBuilder.operation(OperationType.CREATE).success();
        }
        else
        {
            adminEventBuilder.operation(OperationType.FAILED).success();
        }

        return result ? Response.ok().entity(new GeneralResponse<String>() {{
            setStatus(200);
            setBody("Secret updated successfully");
        }}).build() : Response.status(Response.Status.NOT_MODIFIED).entity(new GeneralResponse<String>() {{
            setStatus(304);
            setBody("Secret failed to update");
        }}).build();
    }


}

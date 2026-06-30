package com.qlikverify.extensions.admin.controllers.extension;

import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.extensions.admin.misc.helpers.LogHelper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.TokenVerifier;
import org.keycloak.forms.sso.SSOProvider;
import org.keycloak.forms.sso.freemarker.FreeMarkerSSOProvider;
import org.keycloak.forms.sso.freemarker.models.UrlBean;
import org.keycloak.marketplace.template.SSOBean;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.sso.SSOResponse;
import org.keycloak.theme.Theme;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.AESHelper;
import org.keycloak.vault.VaultProvider;
import org.keycloak.vault.VaultResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.qlikverify.extensions.admin.misc.models.Constants.AUTHORIZATION;
import static org.keycloak.models.Constants.SSO_MANAGEMENT_CLIENT_ID;
import static org.keycloak.models.utils.ModelToRepresentation.getWorkspaceUrl;

public class BPluginExtensionAuthCheckResource extends BaseAuthCheckResource {

    private static final Logger logger = Logger.getLogger(BPluginExtensionAuthCheckResource.class);

    public static final String IWS_CREDTYPE = "iws.credtype";
    public static final String IWS_ADMIN_SET_PASS = "iws_admin_set_pass";
    public static final String IWS_USER_SET_PASS = "iws_user_set_pass";

    public BPluginExtensionAuthCheckResource(KeycloakSession session) {
        super(session);
    }

    @POST
    @Path("{realmid}/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSSOApps(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmid") String realmId, @PathParam("type") String requestType) {
        switch (requestType) {
            case "saf_apps":
                return getSafariSSOApps(req, realmId);
            case "apps":
                return getSSOApps(req, realmId);
            case "creds":
                return getCreds(req, realmId);
            default:
                Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    private Response getCreds(HttpRequest req, String realmId) {
        RealmModel realmModel = session.realms().getRealmByName(realmId);
        if (realmModel == null) {
            LogHelper.getInstance().info("Realm is not available : " + realmId);
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }
        session.getContext().setRealm(realmModel);
        List<String> tokens = req.getHttpHeaders().getRequestHeader(AUTHORIZATION);
        if (tokens.isEmpty())
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

        AuthenticationManager.AuthResult authResult = AppAuthManager.verifyIdentityToken(session, realmModel, session.getContext().getUri(), session.getContext().getConnection(), true, false, null, false, tokens.get(0), session.getContext().getRequestHeaders(), new TokenVerifier.TokenTypeCheck(TokenUtil.TOKEN_TYPE_BEARER));
        if (authResult == null) {
            AppAuthManager.expireIdentityCookie(realmModel, session.getContext().getUri(), session.getContext().getConnection());
            AppAuthManager.expireOldIdentityCookie(realmModel, session.getContext().getUri(), session.getContext().getConnection());
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        Auth auth = new Auth(realmModel, authResult.getToken(), authResult.getUser(), authResult.getClient(), authResult.getSession(), true);
        VaultProvider vaultProvider = getVaultProvider(session,auth.getClient(),auth.getUser());
        logger.info(authResult.getSession().getId());
        VaultResponse vaultResponse = vaultProvider.obtainSecret(false);
        if (vaultResponse == null)
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

        String secret = new String(vaultResponse.getSecret().get().get().array(), StandardCharsets.UTF_8);
        Optional<UserSessionModel> sessionModel = session.sessions().getUserSessionsStream(realmModel,realmModel.getClientByClientId(SSO_MANAGEMENT_CLIENT_ID)).filter(x->x.getUser().getId().equals(authResult.getUser().getId())).sorted(Comparator.comparingInt(UserSessionModel::getLastSessionRefresh).reversed()).findFirst();
        if(!sessionModel.isPresent())
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

        String key = sessionModel.get().getId().replaceAll("-","");
        String encodedBase64Key = AESHelper.encodeKey(key);
        String encodedPwd = null;
        try {
            encodedPwd = AESHelper.encrypt(secret, encodedBase64Key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String encodedUser = null;
        try {
            encodedUser = AESHelper.encrypt(vaultResponse.getVaultSecretId(),encodedBase64Key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String finalEncodedUser = encodedUser;
        String finalEncodedPwd = encodedPwd;

        GeneralResponse generalResponse = new GeneralResponse();
        Map<String, String> body = new HashMap<>();
        body.put("appUser", finalEncodedUser);
        body.put("appPwd", finalEncodedPwd);
        body.put("script", auth.getClient().getAttribute("iws.script"));
        generalResponse.setBody(body);

        return Response.status(Response.Status.OK.getStatusCode()).entity(generalResponse).build();
    }


    private Response getSafariSSOApps(HttpRequest req, String realmId) {
        RealmModel realmModel = session.realms().getRealmByName(realmId);
        if (realmModel == null) {
            LogHelper.getInstance().info("Realm is not available : " + realmId);
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }
        session.getContext().setRealm(realmModel);
        List<String> tokens = req.getHttpHeaders().getRequestHeader(AUTHORIZATION);
        if (tokens.isEmpty())
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

        String[] token = tokens.get(0).split("/");
        if(token.length != 3)
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

        String realmName = token[0];
        if(!realmName.equals(realmModel.getName()))
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

        String userId = token[1];
        String sessionId = token[2];

        Optional<UserSessionModel> sessionModel = session.sessions().getUserSessionsStream(realmModel,realmModel.getClientByClientId(SSO_MANAGEMENT_CLIENT_ID)).sorted(Comparator.comparingInt(UserSessionModel::getLastSessionRefresh).reversed()).filter(x->x.getUser().getId().equals(userId) && x.getId().equals(sessionId)).findFirst();
        if(!sessionModel.isPresent())
            return  Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

        Auth auth = new Auth(realmModel, null, sessionModel.get().getUser(), realmModel.getClientByClientId(SSO_MANAGEMENT_CLIENT_ID) , sessionModel.get(), true);

        FreeMarkerSSOProvider ssoProvider = (FreeMarkerSSOProvider) session.getProvider(SSOProvider.class).setRealm(realmModel).setUriInfo(session.getContext().getUri());
        ssoProvider.setAuth(auth);
        List<SSOBean> beans = ssoProvider.getSSO();

        SSOResponse ssoResponse = new SSOResponse();
        ssoResponse.setSsoApps(beans);
        ssoResponse.setUser(ModelToRepresentation.toBriefRepresentation(sessionModel.get().getUser()));
        ssoResponse.setRealm(getRepresentation(realmModel));
        ssoResponse.setBranding(session.rootBrand().getAdminBrandAttributes(realmModel));

        return Response.status(Response.Status.OK.getStatusCode()).entity(ssoResponse).build();
    }


    private Response getSSOApps(HttpRequest req, String realmId) {
        RealmModel realmModel = session.realms().getRealmByName(realmId);
        if (realmModel == null) {
            LogHelper.getInstance().info("Realm is not available : " + realmId);
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }
        session.getContext().setRealm(realmModel);
        List<String> tokens = req.getHttpHeaders().getRequestHeader(AUTHORIZATION);
        if (tokens.isEmpty())
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

        AuthenticationManager.AuthResult authResult = AppAuthManager.verifyIdentityToken(session, realmModel, session.getContext().getUri(), session.getContext().getConnection(), true, false, null, true, tokens.get(0), session.getContext().getRequestHeaders(), new TokenVerifier.TokenTypeCheck(TokenUtil.TOKEN_TYPE_KEYCLOAK_ID));
        if (authResult == null) {
            AppAuthManager.expireIdentityCookie(realmModel, session.getContext().getUri(), session.getContext().getConnection());
            AppAuthManager.expireOldIdentityCookie(realmModel, session.getContext().getUri(), session.getContext().getConnection());
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        Auth auth = new Auth(realmModel, authResult.getToken(), authResult.getUser(), authResult.getClient(), authResult.getSession(), true);
        FreeMarkerSSOProvider ssoProvider = (FreeMarkerSSOProvider) session.getProvider(SSOProvider.class).setRealm(realmModel).setUriInfo(session.getContext().getUri());
        ssoProvider.setAuth(auth);
        List<SSOBean> beans = ssoProvider.getSSO();

        SSOResponse ssoResponse = new SSOResponse();
        ssoResponse.setSsoApps(beans);
        ssoResponse.setUser(ModelToRepresentation.toBriefRepresentation(authResult.getUser()));
        ssoResponse.setRealm(getRepresentation(realmModel));
        ssoResponse.setBranding(session.rootBrand().getAdminBrandAttributes(realmModel));

        return Response.status(Response.Status.OK.getStatusCode()).entity(ssoResponse).build();
    }

    private RealmRepresentation getRepresentation(RealmModel realm)
    {
        Theme theme = null;
        try {
            theme = session.theme().getTheme(Theme.Type.SSO);
        } catch (IOException e) {
            e.printStackTrace();
        }
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        for (Map.Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
            baseUriBuilder.queryParam(e.getKey(), e.getValue().toArray());
        }
        URI baseQueryUri = baseUriBuilder.build();
        UrlBean urlBean = new UrlBean(realm, theme, uriInfo.getBaseUri(), baseQueryUri, uriInfo.getRequestUri());
        String workspaceUrl = getWorkspaceUrl(session, realm);
        RealmRepresentation rep = new RealmRepresentation();
        rep.setDisplayName(realm.getDisplayName() != null && realm.getDisplayName().length() > 0 ? realm.getDisplayName() : realm.getName().toUpperCase());
        rep.setWorkspace(workspaceUrl);
        rep.setHasCustomDomain(realm.getCustomDomainMappings().filter(Objects::nonNull).filter(x->x.isActive()).count() > 0 ? true : false);
        rep.setRealmLogo(realm.getRealmLogo() != null ? ModelToRepresentation.getLogoAbsURL(realm.getRealmLogo()) : workspaceUrl+urlBean.getResourcesPath()+"/img/eztoauth-logo.svg");
        rep.setDisplayNameHtml(realm.getDisplayNameHtml());
        rep.setEnabled(realm.isEnabled());
        return rep;
    }

    private VaultProvider getVaultProvider(KeycloakSession kcSession,ClientModel clientModel, UserModel user) {

        String iswCredType = clientModel.getAttribute(IWS_CREDTYPE);
        VaultProvider vaultProvider = kcSession.getProvider(VaultProvider.class, "winlogon-vault");
        if (iswCredType.equals(IWS_USER_SET_PASS)) {
            vaultProvider.setUserModel(user).setClientModel(clientModel);
        } else if (iswCredType.equals(IWS_ADMIN_SET_PASS)) {
            vaultProvider.setClientModel(clientModel);
        }

        return vaultProvider;

    }
}

package com.qlikverify.extensions.admin.controllers.offline;

import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.extensions.admin.misc.helpers.LogHelper;
import com.qlikverify.services.actiontoken.OfflineActionTokenValidation;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.authentication.authenticators.utils.GeneralHelper;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.*;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

public class OfflineAuthCheckResource extends BaseAuthCheckResource {

    public OfflineAuthCheckResource(KeycloakSession session) {
        super(session);
    }

    @POST
    @Path("{realmname}/desktop/token")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOneTimeToken(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmname") String realmname) {
        RealmModel realmModel = session.realms().getRealmByName(realmname);
        if (realmModel == null) {
            LogHelper.getInstance().info("Realm is not available : " + realmname);
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }
        session.getContext().setRealm(realmModel);

        String tokenString = AppAuthManager.extractAuthorizationHeaderToken(req.getHttpHeaders());
        if (tokenString == null) throw new NotAuthorizedException("Bearer");
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new NotAuthorizedException("Bearer token format error");
        }
        String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotAuthorizedException("Unknown realm in token");
        }

        if(!realm.getId().equals(realmModel.getId()))
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(session.getContext().getConnection())
                .setHeaders(req.getHttpHeaders())
                .authenticate();

        if (authResult == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        ClientModel clientModel = realmModel.getClientByClientId("sso-desktop");

        if (clientModel == null)
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

        if (!authResult.getClient().getClientId().equals(clientModel.getClientId()))
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();


        long tokenExpiry = GeneralHelper.getExpiryDurationInSec("5m");

        String offlineToken = OfflineActionTokenValidation.generateToken(session,clientModel.getClientId(),authResult.getUser().getId(),tokenExpiry);
        Map<String, String> ssoOffToken = new HashMap<>();
        ssoOffToken.put("sso_off_token", offlineToken);

        return Response.status(Response.Status.OK).entity(ssoOffToken).build();
    }
}

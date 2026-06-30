package com.qlikverify.extensions.admin.controllers.realm;

import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.extensions.admin.misc.helpers.LogHelper;
import com.qlikverify.extensions.admin.misc.requests.CheckTenantExistsRequest;
import com.qlikverify.extensions.admin.misc.requests.CreateRealmRequest;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.util.Optional;

import static com.qlikverify.extensions.admin.misc.models.Constants.STATE;

public class RealmAuthCheckResource extends BaseAuthCheckResource {

    public RealmAuthCheckResource(KeycloakSession session) {
        super(session);
    }

    @Path("/")
    @OPTIONS
    public Response accountPreflight(@Context HttpRequest req) {
        return Cors.add(req, Response.ok()).auth().preflight().build();
    }

    @Path("isTenantExists")
    @OPTIONS
    public Response tenantPreflight(@Context HttpRequest req) {
        return Cors.add(req, Response.ok()).auth().preflight().build();
    }

     @GET
     @Path("getAllTenants")
     @Produces(MediaType.APPLICATION_JSON)
     public Response getRealms(@Context HttpRequest req, @Context HttpResponse res) {
         LogHelper.getInstance().info("getRealms");
         Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
         return new RealmFunctions(session, null).getRealms();
     }

     @POST
     @Path("isTenantExists")
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces(MediaType.APPLICATION_JSON)
     public Response isTenantExists(@Context HttpRequest req, @Context HttpResponse res, CheckTenantExistsRequest request) throws Exception{
         LogHelper.getInstance().info("isTenantExists");
         Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
         return new RealmFunctions(session, null).isTenantExists(request);
     }
 
     @POST
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces(MediaType.APPLICATION_JSON)
     public Response checkTenantExists(@Context HttpRequest req, @Context HttpResponse res,CreateRealmRequest request) throws Exception{
         LogHelper.getInstance().info("checkTenantExists");
         Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
         return new RealmFunctions(session, null).checkTenantExists(request);
     }

     @GET
     @Path("{realm}/getTenantURL")
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces(MediaType.APPLICATION_JSON)
     public Response getWorkspaceUrl(@Context HttpRequest req, @Context HttpResponse res,final @PathParam("realm") String name){
         LogHelper.getInstance().info("getWorkspaceURL");
         Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
         return new RealmFunctions(session, null).getWorkspaceURL(name);
     }

    @GET
    @Path("aad")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResponseFromAAD(@Context HttpRequest req, @Context HttpResponse res) throws Exception{
        LogHelper.getInstance().info("getResponseFromAAD");
        Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
        UriInfo reqUri = session.getContext().getUri();
        Optional<String> optionalScheme = Optional.ofNullable(session)
                .map(KeycloakSession::getContext)
                .map(KeycloakContext::getRequestHeaders)
                .map(reqHeaders -> reqHeaders.getRequestHeader("X-Forwarded-Scheme"))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0));
        String scheme = "http";
        if(optionalScheme.isPresent())
            scheme = optionalScheme.get();
        String redirectUrl = scheme + "://" +
                reqUri.getRequestUri().getAuthority() +
                reqUri.getRequestUri().getPath();

        MultivaluedMap queryParams = reqUri.getQueryParameters();
        String code = String.valueOf(queryParams.getFirst("code"));

        String state = String.valueOf(queryParams.getFirst(STATE));
        String[] realmPlusConnId = state.split("_");
        String realmId = realmPlusConnId[0];
        String connId = realmPlusConnId[1];

        return new RealmFunctions(session,null).storeADTokenInDB(code,realmId,connId,redirectUrl);
    }

    @GET
    @Path("google_ws")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResponseFromGoogleWS(@Context HttpRequest req, @Context HttpResponse res) throws Exception{
        LogHelper.getInstance().info("getResponseFromGoogleWS");
        Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
        UriInfo reqUri = session.getContext().getUri();
        String redirectUrl = reqUri.getRequestUri().getScheme() + "://" +
                reqUri.getRequestUri().getAuthority() +
                reqUri.getRequestUri().getPath();

        MultivaluedMap queryParams = reqUri.getQueryParameters();
        String code = String.valueOf(queryParams.getFirst("code"));

        String state = String.valueOf(queryParams.getFirst(STATE));
        String[] realmPlusConnId = state.split("_");
        String realmId = realmPlusConnId[0];
        String connId = realmPlusConnId[1];

        return new RealmFunctions(session,null).storeGoogleWSTokenInDB(code,realmId,connId,redirectUrl);
    }

    @GET
    @Path("okta_ws")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResponseFromOktaWS(@Context HttpRequest req, @Context HttpResponse res) throws Exception{
        LogHelper.getInstance().info("getResponseFromOktaWS");
        Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
        UriInfo reqUri = session.getContext().getUri();
        String redirectUrl = reqUri.getRequestUri().getScheme() + "://" +
                reqUri.getRequestUri().getAuthority() +
                reqUri.getRequestUri().getPath();

        MultivaluedMap queryParams = reqUri.getQueryParameters();
        String code = String.valueOf(queryParams.getFirst("code"));

        String state = String.valueOf(queryParams.getFirst(STATE));
        String[] realmPlusConnId = state.split("_");
        String realmId = realmPlusConnId[0];
        String connId = realmPlusConnId[1];

        return new RealmFunctions(session,null).storeOktaWSTokenInDB(code,realmId,connId,redirectUrl);
    }


}

package com.qlikverify.extensions.admin.controllers.marketplace;

import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.extensions.admin.misc.helpers.LogHelper;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class MarketplaceAuthCheckResource extends BaseAuthCheckResource {

    public MarketplaceAuthCheckResource(KeycloakSession session) {
        super(session);
    }

    @Path("/")
    @OPTIONS
    public Response accountPreflight(@Context HttpRequest req) {
        return Cors.add(req, Response.ok()).auth().preflight().build();
    }

    @GET
    @Path("categories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMarketplaceCategories(@Context HttpRequest req, @Context HttpResponse res){
        LogHelper.getInstance().info("getMarketplaceCategories");
        Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
        return new MarketplaceFunctions(session).getAllMarketplaceCategories();
    }

    @GET
    @Path("getAllMarketplaceTemplates/{category}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMarketPlaceTemplates(@Context HttpRequest req, @Context HttpResponse res,@PathParam("category") String category) {
        LogHelper.getInstance().info("getMarketplaceTemplatesByCategory");
        Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
        return new MarketplaceFunctions(session).getMarketplaceTemplatesByCategory(category);
    }

    @GET
    @Path("getMarketplaceTemplate/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMarketPlaceTemplateById(@Context HttpRequest req, @Context HttpResponse res, @PathParam("id") String id) {
        LogHelper.getInstance().info("getMarketplaceTemplateById");
        Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
        return new MarketplaceFunctions(session).getMarketplaceTemplateById(id);
    }

    @GET
    @Path("searchMarketplaceTemplates")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchMarketplaceTemplates(@Context HttpRequest req, @Context HttpResponse res,@QueryParam("pattern") String pattern){
        LogHelper.getInstance().info("searchMarketplaceTemplates");
        Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
        return new MarketplaceFunctions(session).searchMarketplaceTemplates(pattern);
    }


}

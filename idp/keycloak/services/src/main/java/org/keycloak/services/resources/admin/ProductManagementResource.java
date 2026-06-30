package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ProductManagementResource {
    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(ProductManagementResource.class);

    public ProductManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProducts(){
        auth.realm().requireManageRealm();
        return Response.status(Response.Status.FORBIDDEN).entity("{\"message\": \"Product management is disabled.\"}").build();
    }

    @GET
    @NoCache
    @Path("{pricing_id}/update")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateStage(@PathParam("pricing_id") String pricingId,
                                @QueryParam("redirectUrl") String redirectUrl){
        auth.realm().requireManageRealm();
        return Response.status(Response.Status.FORBIDDEN).entity("{\"message\": \"Product updates are disabled.\"}").build();
    }
}

package com.qlikverify.extensions.admin.controllers.license.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qlikverify.billingAddress.spi.BillingAddressRepresentation;
import com.qlikverify.billingAddress.spi.BillingAddressService;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.Config;
import org.keycloak.common.Version;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.representations.idm.license.*;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

public class LicenseManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(LicenseManagementResource.class);

    public LicenseManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTH_FLOW);
    }

    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLicense() {
        auth.realm().requireManageRealm();
        return Response.status(Response.Status.FORBIDDEN).entity("{\"message\": \"License creation is disabled.\"}").build();
    }

    @Path("/check")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public CheckLicenseRepresentation checkLicense() {
        CheckLicenseRepresentation response = new CheckLicenseRepresentation();
        response.setStatus(200);
        CheckLicenseBodyRepresentation body = new CheckLicenseBodyRepresentation();
        body.setActivated(true);
        body.setDays(-1);
        body.setMessage("lifetime-license-validity");
        body.setRecurrings(new ArrayList<>());
        body.setCredits(new ArrayList<>());
        body.setWalletBalance(99999.0);
        body.setWalletCurrency("USD");
        body.setWalletThreshold(0.0);
        body.setWalletId("bypassed-wallet");
        response.setBody(body);
        return response;
    }

    @Path("/payment-link/{amount}")
    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAddMoneyLink(@PathParam("amount") String amount){
        auth.realm().requireManageRealm();
        return Response.status(Response.Status.FORBIDDEN).entity("{\"message\": \"Payment links are disabled.\"}").build();
    }

    @Path("/products")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<CustomProductModel> getProducts(){
        auth.realm().requireManageRealm();
        return new ArrayList<>();
    }

    @Path("/pricing/{pricingId}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getProductsPricing(@PathParam("pricingId") String pricingId, @QueryParam("pricingType") String pricingType, @QueryParam("provider") String provider){
        auth.realm().requireManageRealm();
        return new HashMap<>();
    }

    @Path("/products/{productId}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Object getProductDetails(@PathParam("productId") String productId){
        auth.realm().requireManageRealm();
        return new HashMap<>();
    }

    @Path("/products/create")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object createProduct(CustomProductModel productModel){
        auth.realm().requireManageRealm();
        return Response.status(Response.Status.FORBIDDEN).entity("Product creation is disabled").build();
    }

    @Path("/pricing/create")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object createPricing(Object pricingModel, @QueryParam("addToCustomSubtenant") boolean addToCustomSubtenant){
        auth.realm().requireManageRealm();
        return Response.status(Response.Status.FORBIDDEN).entity("Pricing creation is disabled").build();
    }

    @Path("/change-pricing/{tenantId}")
    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Object changePricing(@PathParam("tenantId") String tenantId, @QueryParam("pricingType") String pricingType){
        auth.realm().requireManageRealm();
        return Response.status(Response.Status.FORBIDDEN).entity("Pricing change is disabled").build();
    }

    @Path("/products")
    @PUT
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Object updateproduct(CustomProductModel productModel){
        auth.realm().requireManageRealm();
        return Response.status(Response.Status.FORBIDDEN).entity("Product update is disabled").build();
    }

    @Path("/pricing/{priceId}")
    @PUT
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Object updatePricing(@PathParam("priceId") String priceId, Object pricingModel){
        auth.realm().requireManageRealm();
        return Response.status(Response.Status.FORBIDDEN).entity("Pricing update is disabled").build();
    }
}

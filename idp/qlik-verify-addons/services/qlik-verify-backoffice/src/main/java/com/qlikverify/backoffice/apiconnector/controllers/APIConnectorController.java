package com.qlikverify.backoffice.apiconnector.controllers;


import com.qlikverify.backoffice.apiconnector.service.APIConnectorServiceImpl;
import com.qlikverify.backoffice.controllers.AuthCheckResource;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public class APIConnectorController {
    KeycloakSession session;
    APIConnectorServiceImpl apiConnectorServiceImpl;
    AuthCheckResource authCheckResource;

    public APIConnectorController(KeycloakSession session) {
        this.session = session;
        this.apiConnectorServiceImpl = new APIConnectorServiceImpl(session);
        this.authCheckResource = new AuthCheckResource(session);
    }

    @Path("{realmId}/sync")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sync(@PathParam("realmId") String realmId) {
        return apiConnectorServiceImpl.syncProvidersAndVerifiers(session, realmId);
    }

    @Path("{realmId}/providers")
    @GET
    @NoCache
    public Response getProviders(@PathParam("realmId") String realmId,
                                 @QueryParam("first") Integer firstResult,
                                 @QueryParam("max") Integer max,
                                 @QueryParam("search") String search) {
        return apiConnectorServiceImpl.getProviders(realmId, firstResult, max, search);
    }

    @Path("{realmId}/api-config")
    @GET
    @NoCache
    public Response getApiConfig(@PathParam("realmId") String realmId) {
        return apiConnectorServiceImpl.getApiConfig(realmId);
    }

    @Path("{realmId}/verifiers")
    @GET
    @NoCache
    public Response getVerifiers(@PathParam("realmId") String realmId,
                                 @QueryParam("first") Integer firstResult,
                                 @QueryParam("last") Integer lastResult,
                                 @QueryParam("max") Integer max,
                                 @QueryParam("search") String search,
                                 @QueryParam("sort") String sortBy,
                                 @QueryParam("category") String category,
                                 @QueryParam("enabled") String enabled) {
        return apiConnectorServiceImpl.getVerifiers(realmId, firstResult, lastResult, max, search, sortBy, category, enabled);
    }

    @Path("{realmId}/categories")
    @GET
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCategories(@PathParam("realmId") String realmId) {
        return Response.ok(apiConnectorServiceImpl.getVerifierCategories()).build();
    }

    @Path("{realmId}/verifiers-name")
    @GET
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllVerifiersName(@PathParam("realmId") String realmId, @QueryParam("verifierName") String verifierName) {
        return Response.ok(apiConnectorServiceImpl.getAllVerifiersName(verifierName)).build();
    }

    @Path("{realmId}/update-verifier")
    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVerifier(@PathParam("realmId") String realmId, Map<String, String> data) {
        return apiConnectorServiceImpl.updateVerifier(data, realmId);
    }


    @Path("{realmId}/edit-providers")
    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editProviders(@PathParam("realmId") String realmId, List<String> providerIds) {
        return apiConnectorServiceImpl.editProvider(realmId, providerIds);
    }


    @Path("{realmId}/edit-verifiers")
    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editVerifiers(@PathParam("realmId") String realmId, List<String> verifierIds) {
        return apiConnectorServiceImpl.editVerifier(realmId, verifierIds);
    }

    @Path("{realmId}/api-logs")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAPILogs(@PathParam("realmId") String realmId,
                               @QueryParam("filterByTimestamp") String filterByTimestamp,
                               @QueryParam("filterByVerifierName") String filterByVerifierName,
                               @QueryParam("requestId") String requestId,
                               @QueryParam("isPricingLogs") boolean isPricingLogs,
                               @QueryParam("eventType") List<String> eventTypes,
                               @QueryParam("page") @DefaultValue("0") int page,
                               @QueryParam("size") @DefaultValue("5") int size) {
        return apiConnectorServiceImpl.getAPILogs(realmId, filterByTimestamp, filterByVerifierName, requestId, isPricingLogs, eventTypes, page, size);
    }
}

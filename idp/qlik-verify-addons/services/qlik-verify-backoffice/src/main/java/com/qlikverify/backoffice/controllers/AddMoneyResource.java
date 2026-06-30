package com.qlikverify.backoffice.controllers;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.actionToken.BackgroundProcessorActionTokenVerifier;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;

public class AddMoneyResource {
    protected final KeycloakSession session;

    public AddMoneyResource(KeycloakSession session) {
        this.session = session;
    }

    @Path("/payment-link/{realmId}/{amount}")
    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public HashMap<String, Object> getAddMoneyLink(@PathParam("realmId") String realmId, @PathParam("amount") String amount){
        HashMap<String, Object> licenseResponse;
        RealmModel realm = session.realms().getRealm(realmId);
        if(realm == null){
            licenseResponse = new HashMap<>();
            licenseResponse.put("status", 400);
            licenseResponse.put("message", "Realm not available");
            return licenseResponse;
        }
        session.getContext().setRealm(realm);
        String token = session.getContext().getRequestHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        if(token == null){
            licenseResponse = new HashMap<>();
            licenseResponse.put("status", 400);
            licenseResponse.put("message", "Unauthorized request");
            return licenseResponse;
        }
        BackgroundProcessorActionTokenVerifier verifier = new BackgroundProcessorActionTokenVerifier(session, session.getContext().getRealm());
        JsonWebToken webToken = verifier.validateToken(token.split(" ")[1], realmId, false);
        if(webToken == null){
            licenseResponse = new HashMap<>();
            licenseResponse.put("status", 400);
            licenseResponse.put("message", "Unauthorized request");
            return licenseResponse;
        }
        if(!webToken.getIssuer().equals(realmId)){
            licenseResponse = new HashMap<>();
            licenseResponse.put("status", 400);
            licenseResponse.put("message", "Unauthorized request");
            return licenseResponse;
        }

        licenseResponse = new HashMap<>();
        licenseResponse.put("status", 403);
        licenseResponse.put("message", "Payment gateway is disabled / bypassed.");

        return licenseResponse;
    }
}
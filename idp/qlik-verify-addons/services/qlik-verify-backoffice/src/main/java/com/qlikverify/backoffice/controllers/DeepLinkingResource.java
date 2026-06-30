package com.qlikverify.backoffice.controllers;

import com.qlikverify.backoffice.jpa.DeepLinkEntity;
import com.qlikverify.extensions.admin.controllers.actionToken.EztoActionTokenVerifier;
import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import org.jboss.resteasy.spi.HttpRequest;
import javax.ws.rs.core.Response;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.JsonWebToken;
import java.util.Optional;
import java.util.UUID;

public class DeepLinkingResource extends BaseAuthCheckResource {
    private static final Logger logger = Logger.getLogger(DeepLinkingResource.class);
    KeycloakSession session;
    protected EntityManager em;
    public DeepLinkingResource(KeycloakSession session) {
        super(session);
        this.session = session;
    }

    @GET
    @Path("/payload/{id}/{realmId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPayloadByRequestId(@PathParam("id") String requestId, @PathParam("realmId") String realmId, @Context HttpRequest req) {
        RealmModel realm = session.realms().getRealm(realmId);
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        EztoActionTokenVerifier eztoActionTokenVerifier = new EztoActionTokenVerifier(session, realm);
        String actionToken = req.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        if(actionToken == null) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        JsonWebToken jsonWebToken = eztoActionTokenVerifier.validateToken(actionToken.split(" ")[1],
                realm.getId(),
                false);
        if (jsonWebToken == null || jsonWebToken.isExpired()) {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        DeepLinkEntity entity = getPayload(requestId);
        if(entity == null){
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();
        }
        return Response.status(Response.Status.OK).entity(entity.getPayload()).build();
    }

    public DeepLinkEntity getPayload(String requestId){
        TypedQuery<DeepLinkEntity> query = em.createNamedQuery("getPayloadByDeepLink", DeepLinkEntity.class);
        query.setParameter("trxId", requestId);

        Optional<DeepLinkEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }


    public void addPayload(String payload, String trxId){
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        DeepLinkEntity deepLinkEntity = new DeepLinkEntity();
        deepLinkEntity.setId(UUID.randomUUID().toString());
        deepLinkEntity.setPayload(payload);
        deepLinkEntity.setTrxId(trxId);
        em.persist(deepLinkEntity);
    }


}

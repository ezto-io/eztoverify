package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

public class SubscriptionManagementResource {
    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(SubscriptionManagementResource.class);

    public SubscriptionManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/update-subscription")
    public Response updateSubscription(Map<String, Object> updateOptions) {
        auth.realm().requireManageRealm();
        return Response.status(Response.Status.FORBIDDEN).entity("{\"message\": \"Subscription management is disabled.\"}").type(MediaType.APPLICATION_JSON).build();
    }
}

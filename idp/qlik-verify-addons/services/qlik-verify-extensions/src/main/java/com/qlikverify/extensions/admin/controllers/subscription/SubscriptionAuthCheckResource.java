package com.qlikverify.extensions.admin.controllers.subscription;

import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.extensions.admin.misc.helpers.LogHelper;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.exceptions.LicenseException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SubscriptionAuthCheckResource extends BaseAuthCheckResource {

    public SubscriptionAuthCheckResource(KeycloakSession session) {
        super(session);
    }

    @GET
    @Path("subscription/{realmid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSubscription(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmid") String realmId) {
        RealmModel realmModel = session.realms().getRealm(realmId);
        if (realmModel == null) {
            LogHelper.getInstance().info("Realm is not available : " + realmId);
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        return Response.status(Response.Status.OK.getStatusCode()).build();
    }

    @GET
    @Path("subscription/{realmid}/update_usage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSubscriptionUsage(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmid") String realmId) {
        RealmModel realmModel = session.realms().getRealm(realmId);
        if (realmModel == null) {
            LogHelper.getInstance().info("Realm is not available : " + realmId);
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        Map<String, String> subscriptionUsage = new HashMap<>(realmModel.getSubscriptionUsage());
        if(subscriptionUsage == null || subscriptionUsage.isEmpty()){
            LogHelper.getInstance().info("Subscription not available");
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        subscriptionUsage.put("authentication", String.valueOf(0));
        realmModel.updateSubscription(subscriptionUsage);
        return Response.status(Response.Status.OK.getStatusCode()).build();
    }

   /* @GET
    @Path("resetmonthly/{realmid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetUsage(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmid") String realmId) {
        RealmModel realmModel = session.realms().getRealm(realmId);
        if (realmModel == null) {
            LogHelper.getInstance().info("Realm is not available : " + realmId);
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        try {
            Map<String, String> usage = new HashMap<>();
            usage.putAll(realmModel.getSubscriptionUsage());
            CheckLicenseResponse checkLicenseResponse = session.getLicenseManager().getSubscription(realmModel, true);

            checkLicenseResponse.getBody().getConfig().forEach(x -> {
                if (usage.containsKey(x.getMetadata().getPrId())
                        && x.getMetadata().getReset() != null && x.getMetadata().getReset().equalsIgnoreCase("monthly")) {
                    try {
                        int usageCount = Integer.parseInt(usage.get(x.getMetadata().getPrId()));
                        if(usageCount > 0) {
                            session.getLicenseManager().sendUsageToStripe(realmModel, x.getSubscriptionId(),checkLicenseResponse.getBody().getNextBillingDate(), usageCount);
                            usage.put(x.getMetadata().getPrId(),"0");
                        }
                    } catch (IOException | LicenseException e) {
                        LogHelper.getInstance().error("Error Occurred on sendUsageToStripe " + e.getMessage());
                    }
                }
            });
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession innerSession) {
                    RealmModel innerRealm = innerSession.realms().getRealm(realmModel.getId());
                    innerRealm.updateSubscription(usage);
                }
            });
            session.getLicenseManager().getSubscription(realmModel, true);
            return Response.status(Response.Status.OK.getStatusCode()).build();
        } catch (IOException e) {
            LogHelper.getInstance().error("Error Occurred on resetUsage " + e.getMessage());
        }

        return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();

    }

    @GET
    @Path("dailyusage/{realmid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendUsage(@Context HttpRequest req, @Context HttpResponse res, @PathParam("realmid") String realmId) {
        RealmModel realmModel = session.realms().getRealm(realmId);
        if (realmModel == null) {
            LogHelper.getInstance().info("Realm is not available : " + realmId);
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        try {
            Map<String, String> usage = new HashMap<>();
            usage.putAll(realmModel.getSubscriptionUsage());
            CheckLicenseResponse checkLicenseResponse = session.getLicenseManager().getSubscription(realmModel, true);
            checkLicenseResponse.getBody().getConfig().forEach(x -> {
                if (usage.containsKey(x.getMetadata().getPrId())) {
                    int usageCount = Integer.parseInt(usage.get(x.getMetadata().getPrId()));
                    if (usageCount > 0) {
                        try {
                            session.getLicenseManager().sendUsageToStripe(realmModel, x.getSubscriptionId(),checkLicenseResponse.getBody().getNextBillingDate(), usageCount);
                            //usage.put(x.getMetadata().getPrId(),"0");
                        } catch (IOException | LicenseException e) {
                            LogHelper.getInstance().error("Error Occurred on sendUsageToStripe " + e.getMessage());
                        }
                    }
                }
            });
            session.getLicenseManager().getSubscription(realmModel, true);
            return Response.status(Response.Status.OK.getStatusCode()).build();
        } catch (IOException e) {
            LogHelper.getInstance().error("Error Occurred on sendUsage " + e.getMessage());
        }

        return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();
    }*/
}

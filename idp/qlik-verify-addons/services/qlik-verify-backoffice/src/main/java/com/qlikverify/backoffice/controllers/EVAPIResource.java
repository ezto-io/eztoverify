package com.qlikverify.backoffice.controllers;

import com.qlikverify.backoffice.services.EVAPIResourceService;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.Config;
import org.keycloak.events.EventCollection;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEventCollection;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.AdminEventTotalRepresentation;
import org.keycloak.representations.idm.EventTotalRepresentation;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.utils.GeneralHelper;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class EVAPIResource {

    private static final Logger logger = Logger.getLogger(EVAPIResource.class);

    KeycloakSession session;
    AdminAuth adminAuth;
    AdminPermissionEvaluator auth;
    RealmModel realm;
    AdminEventBuilder adminEvent;
    EVAPIResourceService apiResourceService;
    public static final String[] allowedDayFormats = new String[] { "7d","15d","30d","45d","60d","75d","90d"};
    public EVAPIResource (KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent){
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
        this.apiResourceService = new EVAPIResourceService();
    }

    @Path("/workflow-pricing/{realmId}/{actionId}")
    @GET
    @NoCache
    public Response getWorkflowPricing(@PathParam("actionId") String actionId, @PathParam("realmId") String realmId) {
        auth.realm().requireManageRealm();
        RealmModel realm = session.realms().getRealm(realmId);
//        return CostCalculationForWorkFlowService.calculateCost(session, realm, actionId);
        return Response.status(200).entity(null).build();
    }


    /**
     * Get events
     *
     * Returns all events, or filters them based on URL query parameters listed here
     *
     * @param types The types of events to return
     * @param client App or oauth client name
     * @param user User id
     * @param ipAddress IP address
     * @param firstResult Paging offset
     * @param maxResults Maximum results size (defaults to 100)
     * @return
     */
    @Path("events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public EventTotalRepresentation getEvents(@QueryParam("type") List<String> types, @QueryParam("client") String client,
                                              @QueryParam("user") String user, @QueryParam("last") String days,
                                              @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
                                              @QueryParam("max") Integer maxResults, @QueryParam("request") String request) {
        auth.realm().requireViewEvents();

        if(days != null && !Arrays.stream(allowedDayFormats).anyMatch(x->x.equals(days)))
        {
            return EventTotalRepresentation.empty();
        }

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(realm.getId());
        if (realm.getMode().equalsIgnoreCase("bo")){
            List<EventType> eventTypes = Arrays.stream(EventType.values()).filter(type ->
                            (type.toString().startsWith("EZ_") || type.toString().startsWith("NOTIFICATION")) && (!type.toString().startsWith("EZ_LIVELINESS")) ).
                    collect(Collectors.toList());

            EventType[] eventTypes1 = eventTypes.toArray(new EventType[eventTypes.size()]);
            query.type(eventTypes1);

        }
        else{
            List<EventType> eventTypes = Arrays.stream(EventType.values()).filter(type -> {
                return !type.toString().startsWith("EZ_");
            }).collect(Collectors.toList());

            EventType[] eventTypes1 = eventTypes.toArray(new EventType[eventTypes.size()]);
            query.type(eventTypes1);
        }

        if (client != null) {
            query.client(client);
        }

        if (types != null && !types.isEmpty()) {
            EventType[] t = new EventType[types.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = EventType.valueOf(types.get(i));
            }
            query.type(t);
        }

        if (user != null) {
            query.user(user);
        }

        LocalDate today = LocalDate.now();
        Instant endOfDay = today.atStartOfDay(ZoneId.of("UTC")).plusDays(1).minusSeconds(1).toInstant();
        query.toDate(Date.from(endOfDay));

        query.fromDate(GeneralHelper.getReverseDate(days != null ? days : "7d"));

        if (ipAddress != null) {
            query.ipAddress(ipAddress);
        }
        if (firstResult != null) {
            query.firstResult(firstResult);
        }
        if (maxResults != null) {
            query.maxResults(maxResults);
        } else {
            query.maxResults(Constants.DEFAULT_MAX_RESULTS);
        }

        if(request != null)
        {
            query.request(request);
        }

        EventCollection eventCollection = query.getResult();

        return new EventTotalRepresentation(){{
            setRepresentation(eventCollection.getEvents().stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList()));
            setTotal(eventCollection.getTotal());
        }};
    }

    /**
     * Get admin events
     *
     * Returns all admin events, or filters events based on URL query parameters listed here
     *
     * @param operationTypes
     * @param authRealm
     * @param authClient
     * @param authUser user id
     * @param authIpAddress
     * @param resourcePath
     * @param firstResult
     * @param maxResults Maximum results size (defaults to 100)
     * @return
     */
    @Path("admin-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public AdminEventTotalRepresentation getEvents(@QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
                                                   @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
                                                   @QueryParam("resourcePath") String resourcePath, @QueryParam("last") String days, @QueryParam("first") Integer firstResult,
                                                   @QueryParam("max") Integer maxResults,
                                                   @QueryParam("resourceTypes") List<String> resourceTypes) {
        auth.realm().requireViewEvents();

        if(days != null && !Arrays.stream(allowedDayFormats).anyMatch(x->x.equals(days)))
        {
            return AdminEventTotalRepresentation.empty();
        }

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        AdminEventQuery query = eventStore.createAdminQuery().realm(realm.getId());

        if (authRealm != null) {
            query.authRealm(authRealm);
        }

        if (authClient != null) {
            query.authClient(authClient);
        }

        if (authUser != null) {
            query.authUser(authUser);
        }

        if (authIpAddress != null) {
            query.authIpAddress(authIpAddress);
        }

        if (resourcePath != null) {
            query.resourcePath(resourcePath);
        }

        if (operationTypes != null && !operationTypes.isEmpty()) {
            OperationType[] t = new OperationType[operationTypes.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = OperationType.valueOf(operationTypes.get(i));
            }
            query.operation(t);
        }

        if (resourceTypes != null && !resourceTypes.isEmpty()) {
            ResourceType[] t = new ResourceType[resourceTypes.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = ResourceType.valueOf(resourceTypes.get(i));
            }
            query.resourceType(t);

        } else{
            if(realm.getMode().equals("bo") && !Config.getAdminRealm().equals(realm.getName()) && !Config.getAdminRealm().equals(auth.adminAuth().getRealm().getName())) {
                List<ResourceType> requiredResourceTypes = Arrays.stream(ResourceType.values()).filter(type ->
                                (!type.toString().equals("PRICING") && !type.toString().equals("PRODUCTS")))
                        .collect(Collectors.toList());

                ResourceType[] eventTypes1 = requiredResourceTypes.toArray(new ResourceType[requiredResourceTypes.size()]);
                query.resourceType(eventTypes1);
            }
        }


        LocalDate today = LocalDate.now();
        Instant endOfDay = today.atStartOfDay(ZoneId.of("UTC")).plusDays(1).minusSeconds(1).toInstant();
        query.toTime(Date.from(endOfDay));

        query.fromTime(GeneralHelper.getReverseDate(days != null ? days : "7d"));

        if (firstResult != null) {
            query.firstResult(firstResult);
        }
        if (maxResults != null) {
            query.maxResults(maxResults);
        } else {
            query.maxResults(Constants.DEFAULT_MAX_RESULTS);
        }

        AdminEventCollection eventCollection = query.getResult();

        return new AdminEventTotalRepresentation(){{
            setRepresentation(eventCollection.getEvents().stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList()));
            setTotal(eventCollection.getTotal());
        }};
    }

    /**
     * Delete all events
     *
     */
    @Path("events")
    @DELETE
    public void clearEvents() {
        auth.realm().requireManageEvents();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clear(realm);
    }

    /**
     * Delete all admin events
     *
     */
    @Path("admin-events")
    @DELETE
    public void clearAdminEvents() {
        auth.realm().requireManageEvents();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin(realm);
    }

    /**
     *
     * @param firstResult pagination for first index
     * @param maxResults pagination for maximum data
     * @param search search based on exact request ID
     * @return list of unique transaction records based on request ids
     */
    @Path("audit-flow")
    @GET
    public Response getAuditFlowDetails(@QueryParam("first") Integer firstResult,
                                        @QueryParam("max") Integer maxResults, @QueryParam("search") String search){
        return apiResourceService.getAllAuditFlows(session, realm, firstResult, maxResults, search);
    }

    /**
     *
     * @param transactionId to get specific transaction details
     * @return the data related to the transaction id
     */
    @Path("audit-flow/{transactionId}")
    @GET
    public Response getTransactionDetails(@PathParam("transactionId") String transactionId){
        return apiResourceService.getTransactionDetails(session, realm, transactionId);
    }
}

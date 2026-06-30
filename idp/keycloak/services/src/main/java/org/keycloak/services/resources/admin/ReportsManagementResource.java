package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ReportModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ReportRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class ReportsManagementResource {
    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(ActivityManagementResource.class);
    public static final String[] allowedDayFormats = new String[] { "7d","15d","30d","45d","60d","75d","90d"};

    public ReportsManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTH_FLOW);
    }

    @Path("events")
    @GET
    @NoCache
    public Stream<ReportRepresentation> getEvents(@QueryParam("first") Integer firstResult,
                                                  @QueryParam("max") Integer maxResults) {
        auth.realm().requireViewEvents();

        return realm.getReports().map(x-> ModelToRepresentation.toRepresentation(session,realm,x)).sorted(Comparator.comparing(ReportRepresentation::getCreatedTimestamp).reversed()).limit(10);
    }

    @Path("events/generate")
    @GET
    @NoCache
    public Response getEvents(@QueryParam("type") List<String> types, @QueryParam("client") String client,
                              @QueryParam("user") String user, @QueryParam("last") String days,
                              @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
                              @QueryParam("max") Integer maxResults) throws Exception {
        auth.realm().requireViewEvents();

        if(!realm.getId().equals(auth.adminAuth().getRealm().getId()))
            throw new Exception("Not Authorized to generate reports. Only specific workspace owners are allowed");

        String isReportProgress = realm.getAttribute("isReportProgress");

        if(isReportProgress != null)
            throw new Exception("Already report generation is in progress. Please try once current one is completed.");

        realm.setAttribute("isReportProgress","true");

        if (days != null && !Arrays.stream(allowedDayFormats).anyMatch(x -> x.equals(days))) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        HashMap<String,Object> reportQuery = new HashMap<>();
        reportQuery.put("type",types);
        reportQuery.put("client",client);
        reportQuery.put("user",user);
        reportQuery.put("last",days);
        reportQuery.put("ipAddress",ipAddress);
        reportQuery.put("first",firstResult);
        reportQuery.put("max",maxResults);

        ReportModel reportModel = realm.addReport(new ReportModel(){{
            setRealmId(realm.getId());
            setReportType("log");
            setRequestedBy(auth.adminAuth().getUser().getId());
            setStatus("in-progress");
        }});

        adminEvent.operation(OperationType.TRIGGER).resource(ResourceType.REPORT).representation(reportQuery).detail("report",reportModel.getId()).detail("report_type","log").authUser(auth.adminAuth().getUser())
                .authClient(auth.adminAuth().getClient())
                .authRealm(auth.adminAuth().getRealm()).success();
        return Response.status(Response.Status.CREATED).entity(new HashMap<String,Integer>(){{
            put("status",204);
        }}).build();

    }


    @Path("admin-events/generate")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents(@QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
                              @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
                              @QueryParam("resourcePath") String resourcePath, @QueryParam("last") String days, @QueryParam("first") Integer firstResult,
                              @QueryParam("max") Integer maxResults,
                              @QueryParam("resourceTypes") List<String> resourceTypes) throws Exception {
        auth.realm().requireViewEvents();

        if(!realm.getId().equals(auth.adminAuth().getRealm().getId()))
            throw new Exception("Not Authorized to generate reports. Only specific workspace owners are allowed");

        String isReportProgress = realm.getAttribute("isReportProgress");

        if(isReportProgress != null)
            throw new Exception("Already report generation is in progress. Please try once current one is completed.");

        realm.setAttribute("isReportProgress","true");

        if (days != null && !Arrays.stream(allowedDayFormats).anyMatch(x -> x.equals(days))) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        HashMap<String,Object> reportQuery = new HashMap<>();
        reportQuery.put("operationTypes",operationTypes);
        reportQuery.put("resourceTypes",resourceTypes);
        reportQuery.put("authRealm",authRealm);
        reportQuery.put("authClient",authClient);
        reportQuery.put("authUser",authUser);
        reportQuery.put("authIpAddress",authIpAddress);
        reportQuery.put("resourcePath",resourcePath);
        reportQuery.put("last",days);
        reportQuery.put("first",firstResult);
        reportQuery.put("max",maxResults);

        ReportModel reportModel = realm.addReport(new ReportModel(){{
            setRealmId(realm.getId());
            setReportType("admin_log");
            setRequestedBy(auth.adminAuth().getUser().getId());
            setStatus("in-progress");
        }});

        adminEvent.operation(OperationType.TRIGGER).resource(ResourceType.REPORT).representation(reportQuery).detail("report",reportModel.getId()).detail("report_type","admin_log").authUser(auth.adminAuth().getUser())
                .authClient(auth.adminAuth().getClient())
                .authRealm(auth.adminAuth().getRealm()).success();
        return Response.status(Response.Status.CREATED).entity(new HashMap<String,Integer>(){{
            put("status",204);
        }}).build();
    }


    @Path("api-logs/generate")
    @GET
    @NoCache
    public Response getAPILogs(@QueryParam("filterByTimestamp") String filterByTimestamp,
                               @QueryParam("filterByVerifierName") String filterByVerifierName,
                               @QueryParam("requestId") String requestId,
                               @QueryParam("isPricingLogs") boolean isPricingLogs,
                               @QueryParam("eventType") List<String> eventTypes,
                               @QueryParam("page") @DefaultValue("0") int page,
                               @QueryParam("size") @DefaultValue("5") int size) throws Exception {

        auth.realm().requireViewEvents();

        if(!realm.getId().equals(auth.adminAuth().getRealm().getId()))
            throw new Exception("Not Authorized to generate reports. Only specific workspace owners are allowed");

        String isReportProgress = realm.getAttribute("isReportProgress");

        if(isReportProgress != null)
            throw new Exception("Already report generation is in progress. Please try once current one is completed.");

        realm.setAttribute("isReportProgress","true");

        HashMap<String,Object> reportQuery = new HashMap<>();
        reportQuery.put("filterByTimestamp",filterByTimestamp);
        reportQuery.put("filterByVerifierName", filterByVerifierName);
        reportQuery.put("requestId",requestId);
        reportQuery.put("isPricingLogs",isPricingLogs);
        reportQuery.put("eventType",eventTypes);
        reportQuery.put("page",page);
        reportQuery.put("size",size);

        ReportModel reportModel = realm.addReport(new ReportModel(){{
            setRealmId(realm.getId());
            setReportType(isPricingLogs ? "pricing_log" : "api_log");
            setRequestedBy(auth.adminAuth().getUser().getId());
            setStatus("in-progress");
        }});

        adminEvent.operation(OperationType.TRIGGER).resource(ResourceType.REPORT).representation(reportQuery).detail("report",reportModel.getId()).detail("report_type",isPricingLogs ? "pricing_log" : "api_log").authUser(auth.adminAuth().getUser())
                .authClient(auth.adminAuth().getClient())
                .authRealm(auth.adminAuth().getRealm()).success();
        return Response.status(Response.Status.CREATED).entity(new HashMap<String,Integer>(){{
            put("status",204);
        }}).build();

    }


}

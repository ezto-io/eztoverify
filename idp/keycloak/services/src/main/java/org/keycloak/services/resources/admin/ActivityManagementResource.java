
package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.Config;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RootTenantActivityRepresentation;
import org.keycloak.representations.idm.TenantActivityRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(ActivityManagementResource.class);

    public ActivityManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTH_FLOW);
    }


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTenantActivity(@QueryParam("dateFrom") String dateFrom,
                                      @QueryParam("dateTo") String dateTo,
                                      @QueryParam("getSpecificTenant") Boolean getSpecificTenant) {

        auth.realm().requireManageRealm();

        if((Config.getAdminRealm().equalsIgnoreCase(realm.getName())) && (getSpecificTenant == null || !getSpecificTenant)) {  //checks for tenant isRootTenant
            RootTenantActivityRepresentation rep = new RootTenantActivityRepresentation();
            rep.setTenantsCount(session.realms().getRealmsCount() - 1);
            rep.setUsersCount(session.users().getAllUsersCountExcludeAdmin());
            rep.setAppsCount(getAppsCount(realm).intValue());
            rep.setLoginSuccessCount(returnEventsCount(null,null,EventType.LOGIN,null));
            rep.setSignupsCount(getAllTenantsSignupsCount());
            rep.setHistory(getDateWiseRecords(dateFrom,dateTo,true,null, ResourceType.WORKSPACE,OperationType.CREATE));
            rep.setTopPerformingTenants(getTopPerformingTenants());
            return Response.ok().entity(rep).build();
        }

        TenantActivityRepresentation rep = new TenantActivityRepresentation();
        rep.setUsersCount(session.users().getUsersCount(realm));
        rep.setAppsCount(getAppsCount(realm).intValue());
        rep.setLoginSuccessCount(returnEventsCount(null,null,EventType.LOGIN,realm));
        rep.setSignupsCount(getTotalSignupsCount());
        rep.setHistory(getDateWiseRecords(dateFrom,dateTo,false,EventType.LOGIN,null,null));

        return Response.ok().entity(rep).build();
    }

    private List<HashMap<String,String>> getDateWiseRecords(String dateFrom, String dateTo, boolean isAdminEvt, EventType evtType, ResourceType rsType, OperationType opType){
        List<HashMap<String,String>> dateWiseRecords = new ArrayList<>();
        HashMap<String,String> datewiseRecord = new HashMap<>();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date from = null;
        Date to = null;

        if (dateFrom != null) {
            try {
                from = df.parse(dateFrom);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(From)', expected format is yyyy-MM-dd");
            }
        }

        if (dateTo != null) {
            try {
                to = df.parse(dateTo);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(To)', expected format is yyyy-MM-dd");
            }
        }
        Calendar cal = Calendar.getInstance(); // initialize calendar obj with current date
        for (int i =0 ; i < 30 ; i++){
            Long timeInMs = cal.getTime().getTime() / 1000;
            String date = String.valueOf( timeInMs - (timeInMs % 86400));
            datewiseRecord.put(date,"0");
            cal.add(Calendar.DATE, -1);
        }

        EventStoreProvider evtStore = session.getProvider(EventStoreProvider.class);
        List<Object[]> records = evtStore.getDatewiseRecords(realm.getId(), isAdminEvt, evtType,rsType,opType,from,to);
        records.forEach( rec -> {
            if(datewiseRecord.containsKey(rec[0].toString())){
                datewiseRecord.put(rec[0].toString(), rec[1].toString());
            }
        });

        dateWiseRecords.add(datewiseRecord);
        return dateWiseRecords;
    }

    private List<String> getTopPerformingTenants(){
        EventStoreProvider evtStore = session.getProvider(EventStoreProvider.class);
        return evtStore.getTopPerformingTenants(EventType.LOGIN, 5);
    }

    private Long getAppsCount(RealmModel realm){

        return realm.getClientsStream()
                .filter(c -> !(Constants.defaultClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                .filter(c -> !(Constants.inBuildClients).contains(c.getClientId()))
                .count();
    }

    private int getTotalSignupsCount(){
        return returnEventsCount(null,null, EventType.REGISTER,realm)
                + returnAdminEventsCount(null,null,ResourceType.USER,OperationType.CREATE,realm);
    }
    private int getAllTenantsSignupsCount(){
        return returnEventsCount(null,null, EventType.REGISTER,null)
                + returnAdminEventsCount(null,null,ResourceType.USER,OperationType.CREATE,null);
    }

    private int returnEventsCount(String dateFrom,String dateTo,EventType evtType,RealmModel realmModel){
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        EventQuery query = eventStore.createQuery();
        if(realmModel != null){
            query.realm(realmModel.getId());
        }
        if (dateFrom != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date from = null;
            try {
                from = df.parse(dateFrom);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(From)', expected format is yyyy-MM-dd");
            }
            query.fromDate(from);
        }

        if (dateTo != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date to = null;
            try {
                to = df.parse(dateTo);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(To)', expected format is yyyy-MM-dd");
            }
            query.toDate(to);
        }

        query.type(evtType);

        return (int) query.getResult().getTotal();

    }

    private int returnAdminEventsCount(String dateFrom, String dateTo, ResourceType rsType, OperationType opType,RealmModel realmModel){
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        AdminEventQuery query = eventStore.createAdminQuery();

        if(realmModel != null){
            query.realm(realmModel.getId());
        }

        if (dateFrom != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date from = null;
            try {
                from = df.parse(dateFrom);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(From)', expected format is yyyy-MM-dd");
            }
            query.fromTime(from);
        }
        if (dateTo != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date to = null;
            try {
                to = df.parse(dateTo);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(To)', expected format is yyyy-MM-dd");
            }
            query.toTime(to);
        }
        if(rsType != null){
            query.resourceType(rsType);
        }
        if(opType != null){
            query.operation(opType);
        }
        return query.getResult().getTotal();
    }

}

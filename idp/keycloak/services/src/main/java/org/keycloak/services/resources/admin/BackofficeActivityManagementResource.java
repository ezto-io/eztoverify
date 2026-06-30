
package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.Config;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.BoActivityRepresentation;
import org.keycloak.representations.idm.BoAppActivityRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BackofficeActivityManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(BackofficeActivityManagementResource.class);

    public BackofficeActivityManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.AUTH_FLOW);
    }

    private EventType[] rejectedEvents = {EventType.EZ_TRANSACTION_UNAUTHORIZED, EventType.EZ_TRANSACTION_UNAUTHORIZED_ERROR,
                                                EventType.EZ_TRANSACTION_NOT_PERMITTED, EventType.EZ_TRANSACTION_NOT_PERMITTED_ERROR,
                                                EventType.EZ_TRANSACTION_COMPLETED_ERROR, EventType.EZ_TRANSACTION_ERROR};


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response geTransactionsCount(@QueryParam("dateFrom") String dateFrom,
                                        @QueryParam("dateTo") String dateTo,
                                        @QueryParam("getSpecificTenant") Boolean getSpecificTenant) {

        auth.realm().requireManageRealm();
        boolean isAll = (Config.getAdminRealm().equalsIgnoreCase(realm.getName())) && (getSpecificTenant == null || !getSpecificTenant);
        BoActivityRepresentation representation = new BoActivityRepresentation();

        representation.setTransactions(getTotalTransactions(isAll ? null : realm, null));
        representation.setUniqueUsers(getUniqueUsers(isAll ? null : realm, null));
        representation.setRejected(getTotalRejected(isAll ? null : realm, null));
        representation.setCompleted(getTotalCompleted(isAll ? null : realm, null));
        representation.setPending(getTotalExpired(isAll ? null : realm, null) + getTotalPending(representation.getTransactions(), representation.getCompleted(), representation.getRejected()));
        representation.setApps(getAppsCount(realm, isAll));
        representation.setAppsStats(getAppStats(realm, isAll));
        return Response.ok().entity(representation).build();
    }

    private int getTotalPending(int transactions, int completed, int rejected){
        return transactions - (completed + rejected);
    }
    private List<BoAppActivityRepresentation> getAppStats(RealmModel realm, boolean isAll) {
        if (isAll) {
            List<BoAppActivityRepresentation> list = new ArrayList<>();
            session.realms().getRealmsStream().map(x -> list.addAll(getAppStats(x))).collect(Collectors.toList());
            return list;
        }

        return getAppStats(realm);
    }

    private List<BoAppActivityRepresentation> getAppStats(RealmModel realm) {
        List<BoAppActivityRepresentation> list = new ArrayList<>();
        realm.getClientsStream()
                .filter(c -> !(Constants.defaultClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                .filter(c -> !(Constants.inBuildClients).contains(c.getClientId()))
                .forEach(x -> list.add(getAppStat(realm, x)));
        return list;
    }

    private BoAppActivityRepresentation getAppStat(RealmModel realm, ClientModel client) {
        BoAppActivityRepresentation boAppActivityRepresentation = new BoAppActivityRepresentation();
        boAppActivityRepresentation.setAppId(client.getId());
        if (Boolean.TRUE.equals(client.isSandboxMode())) {
            boAppActivityRepresentation.setSandboxAppId(client.getSandboxClientId());
        }
        boAppActivityRepresentation.setAppName(client.getName());
        boAppActivityRepresentation.setLive(client.isEnabled() && !client.isSandboxMode());
        boAppActivityRepresentation.setIsAPIApp("api-authorizer".equals(client.getAuthenticationFlowBindingOverrides().get("ciba_auth")));
        boAppActivityRepresentation.setRejected(getTotalRejected(realm, client));
        boAppActivityRepresentation.setTransactions(getTotalTransactions(realm, client));
        boAppActivityRepresentation.setCompleted(getTotalCompleted(realm, client));
        boAppActivityRepresentation.setUniqueUsers(getUniqueUsers(realm, client));
        boAppActivityRepresentation.setExpired(getTotalExpired(realm, client));
        boAppActivityRepresentation.setPending(getTotalPending(boAppActivityRepresentation.getTransactions(), boAppActivityRepresentation.getCompleted(), boAppActivityRepresentation.getRejected()));
        return boAppActivityRepresentation;
    }

    private List<HashMap<String, String>> getDateWiseRecords(String dateFrom, String dateTo, boolean isAdminEvt, EventType evtType, ResourceType rsType, OperationType opType) {
        List<HashMap<String, String>> dateWiseRecords = new ArrayList<>();
        HashMap<String, String> datewiseRecord = new HashMap<>();
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
        for (int i = 0; i < 30; i++) {
            Long timeInMs = cal.getTime().getTime() / 1000;
            String date = String.valueOf(timeInMs - (timeInMs % 86400));
            datewiseRecord.put(date, "0");
            cal.add(Calendar.DATE, -1);
        }

        EventStoreProvider evtStore = session.getProvider(EventStoreProvider.class);
        List<Object[]> records = evtStore.getDatewiseRecords(realm.getId(), isAdminEvt, evtType, rsType, opType, from, to);
        records.forEach(rec -> {
            if (datewiseRecord.containsKey(rec[0].toString())) {
                datewiseRecord.put(rec[0].toString(), rec[1].toString());
            }
        });

        dateWiseRecords.add(datewiseRecord);
        return dateWiseRecords;
    }

    private List<String> getTopPerformingTenants() {
        EventStoreProvider evtStore = session.getProvider(EventStoreProvider.class);
        return evtStore.getTopPerformingTenants(EventType.LOGIN, 5);
    }

    private Long getAppsCount(RealmModel realm, boolean isAll) {

        if (isAll)
            return session.realms().getRealmsStream().mapToLong(x -> x.getClientsStream()
                    .filter(c -> !(Constants.defaultClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                    .filter(c -> !(Constants.inBuildClients).contains(c.getClientId()))
                    .count()).sum();

        return realm.getClientsStream()
                .filter(c -> !(Constants.defaultClients.contains(c.getClientId()) || c.getClientId().contains(Constants.REALM_AS_CLIENT)))
                .filter(c -> !(Constants.inBuildClients).contains(c.getClientId()))
                .count();
    }

    private int getTotalTransactions(RealmModel realm, ClientModel client) {
        return returnEventsCount(null, null, realm, client, EventType.EZ_TRANSACTION_REQUEST_COMPLETED);
    }

    private int getTotalRejected(RealmModel realm, ClientModel client) {
        return returnEventsCount(null, null, realm, client, rejectedEvents);
    }

    private int getTotalCompleted(RealmModel realm, ClientModel client) {
        return returnEventsCount(null, null, realm, client, EventType.EZ_TRANSACTION_COMPLETED);
    }

    private int getUniqueUsers(RealmModel realm, ClientModel client) {
        return returnEventsCount(null, null, realm, client, EventType.EZ_TRANSACTION_USER_ADD);
    }

    private int getTotalExpired(RealmModel realm, ClientModel client){
        return returnEventsCount(null, null, realm, client, EventType.EZ_TRANSACTION_EXPIRED, EventType.EZ_TRANSACTION_EXPIRED_ERROR);
    }

    private int returnEventsCount(String dateFrom, String dateTo, RealmModel realmModel, ClientModel clientModel, EventType... evtType) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        EventQuery query = eventStore.createQuery();
        if (realmModel != null) {
            query.realm(realmModel.getId());
        }
        if (clientModel != null) {
            query.client(clientModel.getId());
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

        return (int) Math.floor(query.getEventsCount());

    }

}

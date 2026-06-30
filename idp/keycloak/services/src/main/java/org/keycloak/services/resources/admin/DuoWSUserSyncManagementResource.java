package org.keycloak.services.resources.admin;

import com.duosecurity.client.Admin;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DuoWSUserSyncManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    private static final Logger logger = Logger.getLogger(DuoWSUserSyncManagementResource.class);

    public DuoWSUserSyncManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

    @Path("/connections/{connection_id}/status")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse getStatus(@PathParam("connection_id") String connectionId,@QueryParam("trigger_id") String triggerId) {
        auth.realm().requireManageRealm();

        if(triggerId == null && realm.getAttribute(connectionId) != null){
            triggerId = realm.getAttribute(connectionId);
        }
        String finalTriggerId = triggerId;

        if(finalTriggerId == null){
            return new GeneralResponse() {{
                setStatus(404);
                setBody(new HashMap<String,String>() {{
                    put("message", "trigger-id-not-found");
                }});
            }};
        }

        EventRepresentation eventRepresentation = getSyncStatus(realm,connectionId,finalTriggerId);
        if(eventRepresentation != null){
            return new GeneralResponse() {{
                setStatus(200);
                setBody(eventRepresentation);
            }};
        }
        return new GeneralResponse() {{
            setStatus(200);
            setBody(new HashMap<String,String>(){{
                put("message","Sync In Progress");
                put("triggererId", finalTriggerId);
            }});
        }};
    }

    @Path("/connections/{connection_id}/trigger")
    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse triggerSync(@PathParam("connection_id") String connectionId) {
        auth.realm().requireManageRealm();
        UserModel authUser = auth.adminAuth().getUser();
        if(!checkPermissionToTriggerSync(authUser)){
            return new GeneralResponse() {{
                setStatus(400);
                setBody(new HashMap<String,String>() {{
                    put("message", "you-dont-have-permissions-to-trigger-sync");
                }});
            }};
        }

        //check any sync was already running
        if(Boolean.parseBoolean(realm.getAttribute("aadSyncStarted")) || Boolean.parseBoolean(realm.getAttribute("gwsSyncStarted")) ||
                Boolean.parseBoolean(realm.getAttribute("owsSyncStarted")) || Boolean.parseBoolean(realm.getAttribute("dwsSyncStarted"))){
            return new GeneralResponse() {{
                setStatus(400);
                setBody(new HashMap<String,String>() {{
                    put("message", "directory-sync-is-already-running-please-try-after-some-time");
                }});
            }};
        }


        UserSyncConfigModel userSyncConfigModel = realm.getUserSyncConfigByProviderId(connectionId);
        Map<String, String> config = userSyncConfigModel.getConfig();
        String hasError = ensureTokenIsLive(userSyncConfigModel,config);
        if(!TextUtils.isBlank(hasError)) {
            return new GeneralResponse() {{
                setStatus(400);
                setBody(new HashMap<String,String>() {{
                    put("message", "error-occured-on-sync-please-check-config");
                }});
            }};
        }

        String triggerId = KeycloakModelUtils.generateId();

        HashMap<String,String> dataToBeSent = new HashMap<>();
        dataToBeSent.put("connectionId", connectionId);
        dataToBeSent.put("triggerId", triggerId);
        dataToBeSent.put("createdTime",String.valueOf(System.currentTimeMillis()));
        realm.setAttribute("dwsSyncStarted","true");
        realm.setAttribute(connectionId, triggerId);
        adminEvent.operation(OperationType.TRIGGER).resource(ResourceType.DUO_WS_SYNC)
                .realm(realm).authUser(authUser).representation(dataToBeSent).success();

        return new GeneralResponse() {{
            setStatus(201);
            setBody(new HashMap<String,String>() {{
                put("triggererId", triggerId);
            }});
        }};
    }

    @Path("/connections/{connection_id}/trigger/cancel")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse cancelSync(@PathParam("connection_id") String connectionId) {
        auth.realm().requireManageRealm();
        UserModel authUser = auth.adminAuth().getUser();

        Set<String> collect = session.getContext().getRealm().getEventsListenersStream().collect(Collectors.toSet());
        if (!collect.contains("sync-cancel")) {
            collect.add("sync-cancel");
            session.getContext().getRealm().setEventsListeners(collect);
        }

        HashMap<String,String> dataToBeSent = new HashMap<>();
        dataToBeSent.put("connectionId", connectionId);
        dataToBeSent.put("jobId", session.getContext().getRealm().getAttribute(connectionId + "-jobId"));
        dataToBeSent.put("tenant_id", session.getContext().getRealm().getId());
        adminEvent.operation(OperationType.TRIGGER).resource(ResourceType.SYNC_CANCEL)
                .realm(realm).authUser(authUser).representation(dataToBeSent).success();

        // removing attributes once task is canceled
        realm.removeAttribute("dwsSyncStarted");
        realm.removeAttribute(connectionId);
        return new GeneralResponse() {{
            setStatus(200);
            setBody(new HashMap<String,String>() {{
                put("message", "directory-sync-was-cancelled");
            }});
        }};
    }

    @Path("/connections/{connection_id}/login")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse getLoginUrl(@PathParam("connection_id") String connectionId) {
        auth.realm().requireManageRealm();

        if(!checkPermissionToTriggerSync(auth.adminAuth().getUser())){
            GeneralResponse response = new GeneralResponse();
            response.setStatus(400);
            response.setBody((new HashMap<String,String>() {{
                put("message", "you-dont-have-permissions-to-authorize-with-duo");
            }}));

            return response;
        }

        String clientId= EnvironmentReader.getValue("AAD_CLIENT_ID");
        String clientSecret = EnvironmentReader.getValue("AAD_CLIENT_SECRET");

        if(clientSecret == null || clientId == null)
            return new GeneralResponse() {{
                setStatus(400);
                setBody("Invalid Configuration");
            }};

        String finalBaseUrl = getFinalBaseUrl(connectionId, clientId);
        return new GeneralResponse() {{
            setStatus(200);
            setBody(finalBaseUrl);
        }};
    }

    private String getFinalBaseUrl(String connectionId, String clientId) {
        String rootUrl = ModelToRepresentation.getDashboardUrl();

        rootUrl += "/auth/realms/api/qv_realm/aad";

        String baseUrl = "https://login.microsoftonline.com/organizations/oauth2/v2.0/authorize";
        baseUrl += "?client_id="+ clientId;
        baseUrl += "&redirect_uri="+rootUrl;
        baseUrl += "&scope=offline_access profile openid email Directory.AccessAsUser.All Directory.Read.All Group.Read.All Mail.Read Member.Read.Hidden User.Read User.Read.All User.ReadBasic.All";
        baseUrl += "&response_type=code";
        baseUrl += "&response_mode=query";
        baseUrl += "&state="+realm.getId()+"_"+ connectionId;
        baseUrl += "&nonce=qk0cwgsg1go";
        baseUrl += "&prompt=login";

        return baseUrl;
    }

    @Path("/connections/{connection_id}/synchistory")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse getSyncHistory(@PathParam("connection_id") String connectionId) {
        auth.realm().requireManageRealm();
        Stream<EventRepresentation> syncHistory = getSyncHistory(realm,connectionId);
        return new GeneralResponse() {{
            setStatus(200);
            setBody(syncHistory);
        }};
    }

    @Path("/connections/{connection_id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse getConnection(@PathParam("connection_id") String connectionId) {
        auth.realm().requireManageRealm();
        UserSyncConfigModel userSyncConfigModel = realm.getUserSyncConfigByProviderId(connectionId);
        if(userSyncConfigModel == null){
            return new GeneralResponse() {{
                setStatus(404);
                setBody(new HashMap<String,String>() {{
                    put("message", "connection-not-found");
                }});
            }};
        }
        return new GeneralResponse() {{
            setStatus(200);
            setBody(ModelToRepresentation.toRepresentation(userSyncConfigModel));
        }};
    }

    public String ensureTokenIsLive(UserSyncConfigModel syncConfig,Map<String, String> config) {
        String NO_CREDS = "No config available, Please check settings";
        boolean hasCreds = config.containsKey("apiHostName") &&
                                config.containsKey("integrationKey") &&
                                config.containsKey("secretKey");

        if(hasCreds){
            try {
                // Prepare request.
                Admin request = new
                        Admin.AdminBuilder("GET", config.get("apiHostName"), "/admin/v1/users").build();
                String limit = "1";
                request.addParam("offset", "0");
                request.addParam("limit", limit);
                request.signRequest(config.get("integrationKey") , config.get("secretKey"));
                // try execute
                request.executeJSONRequest();
            } catch (Exception e){
               logger.info(e.getMessage());
                return e.getMessage();
            }
            return "";
        }
        return NO_CREDS;
    }

    private EventRepresentation getSyncStatus(RealmModel realm,String connId,String triggerId){
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        EventQuery query = eventStore.createQuery();
        if(triggerId != null){
            query.user(triggerId);
        }
        if(connId!= null){
            query.client(connId);
        }
        if(realm != null){
            query.realm(realm.getId());
        }
        query.type(EventType.DUO_WS_SYNC_SUCCESS);

        EventRepresentation evtRep = null;
        if(query.getResultStream().count() > 0){
            evtRep = ModelToRepresentation.toRepresentation(query.getResultList().get(0));
        }

        return evtRep;
    }




    private Stream<EventRepresentation> getSyncHistory(RealmModel realm,String connId){
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        EventQuery query = eventStore.createQuery();
        if(realm != null){
            query.realm(realm.getId());
        }
        if(connId != null){
            query.client(connId);
        }
        query.type(EventType.DUO_WS_SYNC_SUCCESS);

        return query.getResultStream().map(ModelToRepresentation::toRepresentation);
    }

    private boolean checkPermissionToTriggerSync(UserModel user){

        if(user == null)
            return false;

        RoleModel role = getRoleModel(realm);
        return user.hasRole(role);
    }

    private RoleModel getRoleModel(RealmModel realm) {
        RoleModel roleModel;
        if(realm.getName().equalsIgnoreCase(Config.getAdminRealm())){
            roleModel = realm.getRole("admin");
        } else {
            ClientModel realmMgmtClient =  realm.getClientByClientId("realm-management");
            roleModel = realmMgmtClient.getRole("workspace-admin");
        }
        return roleModel;
    }
}

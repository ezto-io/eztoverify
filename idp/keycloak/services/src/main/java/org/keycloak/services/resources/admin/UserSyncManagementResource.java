package org.keycloak.services.resources.admin;

import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.UserSyncConnectionRepresentation;
import org.keycloak.representations.idm.UserSyncConfigRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.theme.Theme;
import org.keycloak.utils.StringUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserSyncManagementResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;


    protected static final Logger logger = Logger.getLogger(AzureAdUserSyncManagementResource.class);

    public UserSyncManagementResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

    @Path("/available-syncs")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getAvailableUsersSyncs() {
        auth.realm().requireViewRealm();

        return new ArrayList(Constants.availableSyncOptions);
    }

    @Path("/connections")
    @POST
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse createConnection(UserSyncConnectionRepresentation connection) {
        auth.realm().requireManageRealm();
        UserSyncConfigModel userSyncConfigModel = new UserSyncConfigModel();

        userSyncConfigModel.getConfig().putAll(connection.getSyncConfig());
        String mapperType = connection.getSyncConfig().get("mapperType");
        if(TextUtils.isBlank(mapperType)){
            mapperType = "username";
        }
        userSyncConfigModel.getConfig().put("mapperType", mapperType);

        String domainName = connection.getSyncConfig().get("domainName");
        if(!TextUtils.isBlank(domainName)){
            userSyncConfigModel.getConfig().put("domainName",domainName);
        }

        userSyncConfigModel.setName(connection.getName());
        userSyncConfigModel.setEnrollmentOption(connection.getEnrollmentOption());
        userSyncConfigModel.setProviderId(connection.getProviderId());
        userSyncConfigModel = realm.addUserSyncConfigModel(userSyncConfigModel);
        UserSyncConfigModel finalUserSyncConfigModel = userSyncConfigModel;
        return new GeneralResponse() {{
            setStatus(200);
            setBody(finalUserSyncConfigModel);
        }};
    }

    @Path("/connections/{connection_id}")
    @PUT
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse updateConnection(@PathParam("connection_id") String connectionId, UserSyncConnectionRepresentation connection) {
        auth.realm().requireManageRealm();
        UserSyncConfigModel userSyncConfigModel = realm.getUserSyncConfigByProviderId(connectionId);
        if(userSyncConfigModel == null){
            return new GeneralResponse() {{
                setStatus(404);
                setBody(new HashMap<String,String>() {{
                    put("message", "Connection Not Found");
                }});
            }};
        }
        if (connection.getProviderId() == null ){
            connection.setProviderId(userSyncConfigModel.getId());
        }
        realm.updateUserSyncConfigModel(repToModel(connection, userSyncConfigModel));
        return new GeneralResponse() {{
            setStatus(200);
            setBody(new HashMap<String,String>() {{
                put("message", "Connection Updated Successfully");
            }});
        }};
    }

    @Path("/connections/{connection_id}")
    @DELETE
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse deleteConnection(@PathParam("connection_id") String connectionId) {
        auth.realm().requireManageRealm();
        UserSyncConfigModel userSyncConfigModel = realm.getUserSyncConfigByProviderId(connectionId);
        if(userSyncConfigModel == null){
            return new GeneralResponse() {{
                setStatus(404);
                setBody(new HashMap<String,String>() {{
                    put("message", "Connection Not Found");
                }});
            }};
        }
        realm.removeUserSyncConfigModel(userSyncConfigModel);
        return new GeneralResponse() {{
            setStatus(200);
            setBody(new HashMap<String,String>() {{
                put("message", "Connection Deleted Successfully");
            }});
        }};
    }

    @Path("/connections")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GeneralResponse getUserSyncConnections(
            @QueryParam("search") String search,
            @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults) {
        auth.realm().requireManageRealm();

        firstResult = firstResult == null || firstResult < 0 ? 0 : firstResult;
        maxResults = maxResults == null || maxResults < 0 ? Constants.DEFAULT_MAX_RESULTS : maxResults;

        Stream<UserSyncConfigModel> userSyncStream = realm.getUserSyncConfigsStream();
        userSyncStream = StringUtil.isNotBlank(search)? userSyncStream.filter(x->x.getName().toLowerCase().contains(search.toLowerCase())) : userSyncStream;
        List<UserSyncConfigRepresentation> connections = userSyncStream.skip(firstResult).limit(maxResults)
                .map(config -> ModelToRepresentation.toRepresentation(config))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new GeneralResponse() {{
            setStatus(200);
            setBody(connections);
        }};
    }

    @Path("/connections/count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getUserSyncConnectionsCount(
            @QueryParam("search") String search){
        auth.realm().requireManageRealm();
        Stream<UserSyncConfigModel> userSyncStream = realm.getUserSyncConfigsStream();
        Long searchResults = StringUtil.isNotBlank(search) ? userSyncStream.filter(x->x.getName().toLowerCase().contains(search.toLowerCase())).count() : userSyncStream.count();

        HashMap<String, Long> userSyncConnectionsCount = new HashMap<>();
        userSyncConnectionsCount.put("count",searchResults);

        return userSyncConnectionsCount;
    }

    @Path("azure_ad")
    public AzureAdUserSyncManagementResource syncADUsers() {
        AzureAdUserSyncManagementResource resource = new AzureAdUserSyncManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("google_ws")
    public GoogleWSUserSyncManagementResource syncGoogleUsers() {
        GoogleWSUserSyncManagementResource resource = new GoogleWSUserSyncManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    @Path("okta_ws")
    public OktaWSUserSyncManagementResource syncOktasers() {
        OktaWSUserSyncManagementResource resource = new OktaWSUserSyncManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }
    @Path("duo_ws")
    public DuoWSUserSyncManagementResource syncDuoUsers() {
        DuoWSUserSyncManagementResource resource = new DuoWSUserSyncManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    private UserSyncConfigModel repToModel(UserSyncConnectionRepresentation conn,UserSyncConfigModel oldModel){
        UserSyncConfigModel model = new UserSyncConfigModel();
        model.setId(conn.getProviderId());
        model.setEnrollmentOption(conn.getEnrollmentOption());
        model.setName(conn.getName());

        Map<String,String> syncConfig = oldModel.getConfig();
        conn.getSyncConfig().forEach((confKey,confVal) -> {
            syncConfig.put(confKey,confVal);
        });
        model.setConfig(syncConfig);
        return model;
    }


}

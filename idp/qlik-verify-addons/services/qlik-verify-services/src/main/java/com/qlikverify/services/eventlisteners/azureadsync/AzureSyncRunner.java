package com.qlikverify.services.eventlisteners.azureadsync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.qlikverify.services.helpers.GeneralHelper;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.actiontoken.userinvite.UserInviteLinkActionToken;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.storage.user.SyncHistory;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.keycloak.models.Constants.*;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_PERSISTENT_NAME_ID_FOR;


public class AzureSyncRunner {
    private static final Logger log = Logger.getLogger(AzureADSyncListenerProvider.class);

    private static Executor executor = Executors.newCachedThreadPool();
    private static int year = Calendar.getInstance().get(Calendar.YEAR);

    private static String USER_PROPS = "id,accountEnabled,userPrincipalName,mail,givenName,surname,onPremisesImmutableId";
    private static String OFC_IMT_ID= String.format("%s.*", SAML_PERSISTENT_NAME_ID_FOR);

    public static void syncFromAAD(KeycloakSession session, String realmId, String representation, Object result, String userId) {
        String eventKey = "AD_SYNC_KEY_"  + realmId;
        ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);
        ExecutorService executorService = Executors.newScheduledThreadPool(1);

        /** Making a asynchronous thread using seperate executorService */
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        HashMap<String,String> configData = new HashMap<>();
                        RealmModel realm = innerSession.realms().getRealm(realmId);
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        try {
                            configData= JsonSerialization.readValue(representation, HashMap.class);
                        } catch (JsonProcessingException e) {
                            eventBuilder.event(EventType.AZURE_SYNC_ERROR);
                            eventBuilder.realm(realm);
                            eventBuilder.error(e.getMessage());
                            log.error(e.getMessage());
                            e.printStackTrace();
                        } catch (IOException e) {
                            eventBuilder.event(EventType.AZURE_SYNC_ERROR);
                            eventBuilder.realm(realm);
                            eventBuilder.error(e.getMessage());
                            log.error(e.getMessage());
                            e.printStackTrace();
                        }
                        String connId = configData.get("connectionId");

                        Gson gson = new Gson();
                        Map<String, Object> resultMap = new HashMap<>();
                        try{
                            resultMap = JsonSerialization.readValue(result.toString(), Map.class);
                        } catch (Exception e){
                            eventBuilder.event(EventType.AZURE_SYNC_ERROR);
                            eventBuilder.realm(realm);
                            eventBuilder.error(e.getMessage());
                            log.error(e.getMessage());
                        }

                        ArrayList<Group> groupsCurrentPage = new ArrayList<>();
                        ArrayList<User> usersCurrentPage = new ArrayList<>();
                        ArrayList<Group> groupsNextPage = new ArrayList<>();
                        ArrayList<User> usersNextPage = new ArrayList<>();

                        ArrayList<String> groupsCurrentPagelist = gson.fromJson(resultMap.get("groupsCurrentPage").toString(), ArrayList.class);
                        ArrayList<String> usersCurrentPagelist = gson.fromJson(resultMap.get("usersCurrentPage").toString(), ArrayList.class);
                        ArrayList<String> groupsNextPageList = gson.fromJson(resultMap.get("groupsNextPage").toString(), ArrayList.class);
                        ArrayList<String> usersNextPageList = gson.fromJson(resultMap.get("usersNextPage").toString(), ArrayList.class);
                        HashMap<String, Object> usersRoles = gson.fromJson(gson.toJson(resultMap.get("usersRoles")), HashMap.class);


                        ArrayList<Group> finalGroupsCurrentPage = groupsCurrentPage;
                        groupsCurrentPagelist.stream().forEach(x -> finalGroupsCurrentPage.add(gson.fromJson(x, Group.class)));
                        groupsCurrentPage = finalGroupsCurrentPage;

                        ArrayList<User> finalUsersCurrentPage = usersCurrentPage;
                        usersCurrentPagelist.stream().forEach(x -> finalUsersCurrentPage.add(gson.fromJson(x, User.class)));
                        usersCurrentPage = finalUsersCurrentPage;

                        if(groupsNextPageList != null){
                            ArrayList<Group> finalGroupsNextPage = groupsNextPage;
                            groupsNextPageList.stream().forEach(x -> finalGroupsNextPage.add(gson.fromJson(x, Group.class)));
                            groupsNextPage = finalGroupsNextPage;
                        }

                        if(usersNextPageList != null){
                            ArrayList<User> finalUsersNextPage = usersNextPage;
                            usersNextPageList.stream().forEach(x -> finalUsersNextPage.add(gson.fromJson(x, User.class)));
                            usersNextPage = finalUsersNextPage;
                        }

                        UserSyncConfigModel userSyncConfigModel = realm.getUserSyncConfigByProviderId(connId);
                        Map<String, String> connConfig = userSyncConfigModel.getConfig();

                        SyncHistory syncHistory = new SyncHistory();
                        syncHistory.setCreatedTime(Long.parseLong(configData.get("createdTime")));

                        // setting mapperType
                        String mapperType = connConfig.get("mapperType");
                        if(TextUtils.isBlank(mapperType)){
                            mapperType = "username";
                        }

                        IAuthenticationProvider authProvider = new IAuthenticationProvider() {
                            @Override
                            public CompletableFuture<String> getAuthorizationTokenAsync(URL requestUrl) {
                                CompletableFuture<String> future = new CompletableFuture<>();
                                future.complete(connConfig.get("access_token"));
                                return future;
                            }
                        };

                        GraphServiceClient graphClient = GraphServiceClient
                                .builder()
                                .authenticationProvider(authProvider)
                                .buildClient();
                        try{
                            //create groups
                            boolean hasNextPageinGrps = false;
                            do{
                                groupsCurrentPage.forEach(group -> {
                                    List<GroupModel> groupModels = innerSession.groups().searchForGroupByNameStream(realm,group.displayName,-1,-1).collect(Collectors.toList());
                                    if(groupModels.size() == 0){
                                        GroupModel groupModel = realm.createGroup(KeycloakModelUtils.generateId(),group.displayName);
                                        groupModel.setSingleAttribute(SYNCED_FROM,AZURE_AD);
                                    }
                                    else
                                        groupModels.get(0).setSingleAttribute(SYNCED_FROM,AZURE_AD);

                                });

                                if(!groupsNextPage.isEmpty()){
                                    groupsCurrentPage = groupsNextPage;
                                    hasNextPageinGrps = true;
                                } else {
                                    hasNextPageinGrps = false;
                                }
                            } while (hasNextPageinGrps);

                            //create Users
                            boolean hasNextPageinUser = false;
                            do{

                                //deleting users that deleted in azure
                                deleteUsers(usersCurrentPage,realm,innerSession,mapperType,syncHistory);

                                String finalMapperType = mapperType;
                                usersCurrentPage.forEach(user -> {
                                    UserModel currentUser = null;
                                    if(finalMapperType.equalsIgnoreCase("email")) {
                                        if(user.mail != null){
                                            currentUser = innerSession.users().getUserByEmail(realm,user.mail);
                                        }
                                    } else {
                                        currentUser = innerSession.users().getUserByUsername(realm,user.userPrincipalName);
                                    }

                                    try{
                                        if(currentUser == null){
                                            UserModel imported = importUserFromDirectory(graphClient,innerSession,realm,user, userSyncConfigModel.getEnrollmentOption(), finalMapperType, (ArrayList<String>) usersRoles.get(user.id));
                                            if(imported != null){
                                                syncHistory.increaseAdded();
                                            }
                                            currentUser = imported;
                                        } else {
                                            if(updateUserFromDirectory(graphClient,innerSession,currentUser,realm,user, userSyncConfigModel.getEnrollmentOption(), finalMapperType, (ArrayList<String>) usersRoles.get(user.id))){
                                                syncHistory.increaseUpdated();
                                            }
                                        }
                                    } catch (ModelDuplicateException e){
                                        syncHistory.increaseFailed();
                                    }
                                    AdminEventBuilder adminEvent = new AdminEventBuilder(realm, new AuthDetails(), innerSession, innerSession.getContext().getConnection());
                                    adminEvent.realm(realm);
                                    List<String> userRoles = (List<String>) usersRoles.get(user.id);
                                    if(userRoles != null && !userRoles.isEmpty()) {
                                        for (String userRole : userRoles) {
                                            GeneralHelper.createAndAddRole(innerSession, realm, adminEvent, currentUser, "A-" + userRole);
                                        }
                                    }
                                });

                                if(!usersNextPage.isEmpty()){
                                    usersCurrentPage = usersNextPage;
                                    hasNextPageinUser = true;
                                } else {
                                    hasNextPageinUser = false;
                                }
                            } while (hasNextPageinUser);

                            do{
                                //deleting Groups that deleted in azure
                                deleteGroups(groupsCurrentPage,innerSession,realm);
                                if(!groupsNextPage.isEmpty()){
                                    groupsCurrentPage = groupsNextPage;
                                    hasNextPageinGrps = true;
                                } else {
                                    hasNextPageinGrps = false;
                                }
                            } while (hasNextPageinGrps);


                            syncHistory.setCompletedTime(System.currentTimeMillis());
                            innerSession.getContext().setRealm(realm);
                            eventBuilder.event(EventType.AZURE_SYNC_SUCCESS);
                            eventBuilder.realm(realm);
                            eventBuilder.client(connId);
                            eventBuilder.user(configData.get("triggerId"));
                            eventBuilder.detail("triggeredUserId", userId);
                            setEventDetails(eventBuilder,syncHistory);
                            eventBuilder.success();

                            //remove the realmAttribute
                            realm.removeAttribute("aadSyncStarted");
                            realm.removeAttribute(connId);
                            clusterProvider.removeListener(eventKey);//remove listener after completion of the task
                        } catch ( RuntimeException e ) {
                            log.error(e.getMessage());
                            realm.removeAttribute("aadSyncStarted");
                            realm.removeAttribute(connId);
                            clusterProvider.removeListener(eventKey);
                        }
                    }
                });
            }
        });

        if(clusterProvider.getListener(eventKey) == null) {
            clusterProvider.registerListener(eventKey,
                    new ADSyncClusterListener(session.getKeycloakSessionFactory(), executorService));
            log.info("Listener Registered for the key " + eventKey);
        }
    }


    private static void setEventDetails(EventBuilder evtBuilder,SyncHistory syncHistory){
        evtBuilder.detail("syncProvider", "Azure AD");
        evtBuilder.detail("created_time",syncHistory.getCreatedTime().toString());
        evtBuilder.detail("completed_time",syncHistory.getCompletedTime().toString());
        evtBuilder.detail("importedCnt",String.valueOf(syncHistory.getAdded()));
        evtBuilder.detail("updatedCnt", String.valueOf(syncHistory.getUpdated()));
        evtBuilder.detail("deletedCnt",String.valueOf(syncHistory.getRemoved()));
        evtBuilder.detail("totalSync", String.valueOf(syncHistory.getAdded() + syncHistory.getUpdated() + syncHistory.getRemoved()));
        evtBuilder.detail("failed", String.valueOf(syncHistory.getFailed()));
        evtBuilder.detail("status",syncHistory.getStatus());
    }

    private static UserModel importUserFromDirectory(GraphServiceClient graphClient, KeycloakSession session, RealmModel realm, User user, String enrollmentOption, String mapperType, ArrayList<String> userRoles) throws ModelDuplicateException{
        String userName = "";
        if(mapperType.equalsIgnoreCase("email") && user.mail != null){
           userName = user.mail;
        } else if(mapperType.equalsIgnoreCase("username")){
            userName = user.userPrincipalName;
        }

        if(!TextUtils.isBlank(userName)){
            UserModel importedUser = session.users().addUser(realm,userName);
            importedUser.setEmail(userName);
            if(user.givenName != null){
                importedUser.setFirstName(user.givenName);
            }
            if(user.surname != null){
                importedUser.setLastName(user.surname);
            }
            if(user.onPremisesImmutableId != null){
                importedUser.setSingleAttribute(OFC_IMT_ID, user.onPremisesImmutableId);
            }
            if(user.accountEnabled != null){
                importedUser.setEnabled(user.accountEnabled);
            }

            importedUser.setSingleAttribute(SYNCED_FROM,AZURE_AD);
            if(userRoles != null){
                importedUser.setAttribute(SYNCED_ROLES, userRoles);
            }

            enrollUser(session,realm,enrollmentOption,importedUser);
            addGroupMembership(graphClient,user,realm,importedUser);
            return importedUser;
        }

        return null;
    }

    private static boolean updateUserFromDirectory(GraphServiceClient graphClient, KeycloakSession session, UserModel currentUser, RealmModel realm, User user, String enrollmentOption, String mapperType, ArrayList<String> userRoles) throws ModelDuplicateException{
        boolean isUpdated = false;
        if((user.givenName != null) && (!user.givenName.equals(currentUser.getFirstName()))){
            isUpdated = true;
            currentUser.setFirstName(user.givenName);
        }
        if((user.surname != null) && (!user.surname.equals(currentUser.getLastName()))){
            isUpdated = true;
            currentUser.setLastName(user.surname);
        }
        if((user.accountEnabled != null) && (!currentUser.isEnabled())){
            isUpdated = true;
            currentUser.setEnabled(user.accountEnabled);
        }
        if((user.onPremisesImmutableId != null) && (currentUser.getFirstAttribute(OFC_IMT_ID) == null && !currentUser.getFirstAttribute(OFC_IMT_ID).equals(user.onPremisesImmutableId))){
            isUpdated = true;
            currentUser.setSingleAttribute(OFC_IMT_ID, user.onPremisesImmutableId);
        }
        if((mapperType.equalsIgnoreCase("username") && user.mail != null &&
                session.users().getUserByEmail(realm,user.mail) == null) && (!user.mail.equals(currentUser.getEmail()))) {
            isUpdated = true;
            currentUser.setEmail(user.mail);
        }

        currentUser.setSingleAttribute(SYNCED_FROM, AZURE_AD);
        if(userRoles != null){
            currentUser.setAttribute(SYNCED_ROLES, userRoles);
        }

        currentUser.getGroupsStream().filter(groupModel -> {
            String groupAttribute = groupModel.getFirstAttribute(SYNCED_FROM);
            return groupAttribute != null && groupAttribute.equals(AZURE_AD);
        }).forEach(currentUser::leaveGroup);

        enrollUser(session,realm,enrollmentOption,currentUser);
        addGroupMembership(graphClient,user,realm,currentUser);
        return isUpdated;
    }

    private static void enrollUser(KeycloakSession session,RealmModel realm,String enrollmentOption,UserModel user){
       if(user.getEmail() != null) {

           if(enrollmentOption.equalsIgnoreCase("trustEmail")){
               user.setEmailVerified(true);
           }

           if(!user.isEmailVerified() && enrollmentOption.equalsIgnoreCase("sendEnrollmentLink")){
               if (user != null && user.getEmail() != null) {
                   session.getContext().setRealm(realm);
                   URI uriInfo = UriBuilder.fromUri( ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth").build();
                   int validityInSecs = realm.getActionTokenGeneratedByUserLifespan(UserInviteLinkActionToken.TOKEN_TYPE);
                   int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
                   UserInviteLinkActionToken token = new UserInviteLinkActionToken(user.getId(),user.getEmail(), absoluteExpirationInSecs,Constants.SSO_MANAGEMENT_CLIENT_ID);
                   UriBuilder builder = Urls.actionTokenBuilder(uriInfo, token.serialize(session, realm, ""),
                           Constants.SSO_MANAGEMENT_CLIENT_ID, "");
                   String link = builder.build(realm.getName()).toString();
                   String workspaceUrl =ModelToRepresentation.getWorkspaceUrl(session, realm);
                   //String url = Urls.ssoPage(session.getContext().getUri().getBaseUri(), realm.getName()).toString();
                   Map<String, Object> attributes = new HashMap<>();
                   attributes.put("tenant", getRealmName(realm));
                   attributes.put("user_name", user.getUsername());
                   attributes.put("year", String.valueOf(year));
                   attributes.put("link", link);
                   attributes.put("link_expiry",formatDuration(validityInSecs));
                   attributes.put("workspace",workspaceUrl);
                   attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                   try {
                       session.getProvider(EmailTemplateProvider.class)
                               .setRealm(realm)
                               .send(user.getEmail(), "welcomeUserSubject", "welcome-user-email-link.ftl", attributes);

                       //after sending email, we can set invite attributes
                       user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Invited");
                       user.setSingleAttribute(UserModel.USER_INVITE_CLIENT, Constants.SSO_MANAGEMENT_CLIENT_ID);
                   } catch (EmailException e) {
                       log.error(e);
                   } catch (ConfigException e) {
                       EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
                       eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                       eventBuilder.error(e.getMessage());
                   }
               }
           }

       }
    }

    private static void addGroupMembership(GraphServiceClient graphClient, User user, RealmModel realm, UserModel userModel){
        GroupCollectionPage memberOf = graphClient.users(user.id).memberOfAsGroup().buildRequest().get();
        memberOf.getCurrentPage().forEach(g -> {
            List<GroupModel> grps = realm.searchForGroupByNameStream(g.displayName, -1,-1).collect(Collectors.toList());
            if(grps.size() > 0){
                GroupModel model = grps.get(0);
                if (model != null && !userModel.isMemberOf(model)) {
                    userModel.joinGroup(model);
                }
            }
        });
    }

    private static Map<String, Object> formatDuration(long valueInSeconds) {
        Map<String, Object> timeDuration= new HashMap<>();
        String unitKey = "seconds";
        long value = valueInSeconds;

        if (value > 0 && value % 60 == 0) {
            unitKey = "minutes";
            value = value / 60;
            if (value % 60 == 0) {
                unitKey = "hours";
                value = value / 60;
                if (value % 24 == 0) {
                    unitKey = "days";
                    value = value / 24;
                }
            }
        }
        timeDuration.put("value",value);
        timeDuration.put("unit_key",unitKey);
        return timeDuration;
    }

    private static String getUnitTextFromMessages(String unitKey, long value) {
        if(value == 1){
            unitKey = unitKey.substring(0, unitKey.length() - 1);
        }
        return unitKey;
    }

    private static String getRealmName(RealmModel realm){
        return !TextUtils.isEmpty(realm.getDisplayName()) ? realm.getDisplayName() : realm.getName();
    }


    private static class ADSyncClusterListener implements ClusterListener {

        private final KeycloakSessionFactory sessionFactory;
        private final ExecutorService adSyncThread;

        public ADSyncClusterListener(KeycloakSessionFactory sessionFactory,ExecutorService adSyncThread) {
            this.sessionFactory = sessionFactory;
            this.adSyncThread = adSyncThread;
        }

        @Override
        public void eventReceived(ClusterEvent event) {
            final ADSyncCancelEvent fedEvent = (ADSyncCancelEvent) event;

            ADSyncClusterListener listener = this;
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                @Override
                public void run(KeycloakSession session) {
                    if (fedEvent.isCancelSyncTask()) {
                        log.info("ADSyncClusterListener: Received Event" + fedEvent.toString());
                        adSyncThread.shutdownNow();
                        session.getProvider(ClusterProvider.class).removeListener(fedEvent.getTaskKey());
                    }
                }
            });
        }
    }


    public static class ADSyncCancelEvent implements ClusterEvent {
        private String realmId;
        private String taskKey;
        private boolean cancelSyncTask;


        public ADSyncCancelEvent(String realmId, String taskKey, boolean cancelSyncTask) {
            this.realmId = realmId;
            this.taskKey = taskKey;
            this.cancelSyncTask = cancelSyncTask;
        }

        public String getRealmId() {
            return realmId;
        }

        public void setRealmId(String realmId) {
            this.realmId = realmId;
        }

        public String getTaskKey() {
            return taskKey;
        }

        public void setTaskKey(String taskKey) {
            this.taskKey = taskKey;
        }

        public boolean isCancelSyncTask() {
            return cancelSyncTask;
        }

        public void setCancelSyncTask(boolean cancelSyncTask) {
            this.cancelSyncTask = cancelSyncTask;
        }


        @Override
        public String toString() {
            return "ADSyncEvent{" +
                    "realmId='" + realmId + '\'' +
                    ", cancelSyncTask=" + cancelSyncTask +
                    '}';
        }
    }

    private static void deleteUsers(ArrayList<User> UsersCurrentPage, RealmModel realmModel, KeycloakSession session, String mapperType, SyncHistory syncHistory) {

        //Getting list of users from realm that already imported from Azure
        List<UserModel> listOfUser = session.users()
                .searchForUserByUserAttributeStream(realmModel, SYNCED_FROM, AZURE_AD, true)
                .collect(Collectors.toList());

        if (listOfUser.isEmpty()) {
            log.info("AzureSyncRunner :: deleteUsers :: User list that already imported from Azure were empty");
            return;
        }

        log.info("AzureSyncRunner :: deleteUsers :: triggered");

        Map<String, String> azureUsersMap = new HashMap<>();
        int deletedUserCount = 0;

        //Getting list of users from Azure
        UsersCurrentPage.forEach(user -> {
            if (mapperType.equalsIgnoreCase("email")) {
                azureUsersMap.put(user.mail, "true");
            } else if (mapperType.equalsIgnoreCase("username")) {
                azureUsersMap.put(user.userPrincipalName, "true");
            }
        });

        //Deleting user that present in QV and deleted in Azure
        try{
            for (UserModel userModel : listOfUser) {
                if (mapperType.equalsIgnoreCase("email")) {
                    if (azureUsersMap.get(userModel.getEmail()) == null) {
                        deletedUserCount = deleteIfUserNotAvailable(realmModel, session, userModel) ? deletedUserCount+1 : deletedUserCount;
                    }
                } else if (mapperType.equalsIgnoreCase("username")) {
                    if (azureUsersMap.get(userModel.getUsername()) == null) {
                        deletedUserCount = deleteIfUserNotAvailable(realmModel, session, userModel) ? deletedUserCount+1 : deletedUserCount;
                    }
                }
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }

        log.info("AzureSyncRunner :: deleteUsers :: Deleted user count = "+deletedUserCount);
        syncHistory.setRemoved(deletedUserCount);

    }

    private static boolean  deleteIfUserNotAvailable(RealmModel realm, KeycloakSession session, UserModel userModel) {

        UserManager userManager = new UserManager(session);

        // Remove the Realm Logo from MINIO
        AvatarProvider avatarProvider = session.getProvider(AvatarProvider.class, "avatar-storage-minio");
        if (!avatarProvider.removeAvatarImage(realm.getName(), userModel.getId())) {
            log.error("Something went wrong on deleting User Avatar for the user " + userModel.getId() + " from Minio");
        }

        return userManager.removeUser(realm, userModel);
    }

    private static void deleteGroups(ArrayList<Group> groupsCurrentPage, KeycloakSession session, RealmModel realm){

        //Getting list of Groups from realm that already imported from Azure
        List<GroupModel> listOfGroups = session.groups().getGroupsStream(realm)
                .filter(groupModel -> {
                    String groupAttribute = groupModel.getFirstAttribute(SYNCED_FROM);
                    return groupAttribute != null && groupAttribute.equals(AZURE_AD);
                })
                .collect(Collectors.toList());

        if(listOfGroups.size() == 0){
            log.info("AzureSyncRunner :: deleteGroups :: Groups list that already imported from Azure were empty");
            return;
        }

        log.info("AzureSyncRunner :: deleteGroups :: triggered");

        Map<String,String> azureGroupsMap = new HashMap<>();
        int deletedGroupCount = 0;

        //Getting list of Groups from Azure
        groupsCurrentPage.forEach(group -> {
            azureGroupsMap.put(group.displayName,"true");

        });

        //Deleting Groups that present in QV and not in Azure
        try {
            for(GroupModel groupModel : listOfGroups){

                if (azureGroupsMap.get(groupModel.getName()) == null && !session.users().getGroupMembersStream(realm, groupModel).findAny().isPresent()) {
                    deletedGroupCount = realm.removeGroup(groupModel) ? deletedGroupCount+1 : deletedGroupCount;
                }
            }

        }catch (Exception e){
            log.error(e.getMessage());
        }

        log.info("AzureSyncRunner :: deleteGroups :: Deleted Groups count = "+deletedGroupCount);

    }

}

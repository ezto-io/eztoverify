package com.qlikverify.services.eventlisteners.googlewssync;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.model.Groups;
import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.Users;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.qlikverify.services.helpers.GeneralHelper;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.actiontoken.userinvite.UserInviteLinkActionToken;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
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
import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.keycloak.models.Constants.*;


public class GoogleWSSyncRunner {
    private static final Logger log = Logger.getLogger(GoogleWSSyncListenerProvider.class);
    private static final String LOG_TAG = "EZTO_GOOGLE_SYNC_RUNNER: ";

    private static Executor executor = Executors.newCachedThreadPool();
    private static int year = Calendar.getInstance().get(Calendar.YEAR);

    public static void syncFromGWS(KeycloakSession session, String realmId, Object groupList, Object representation, String userId) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            resultMap = JsonSerialization.readValue(groupList.toString(), Map.class);
        } catch (IOException e) {
            log.error(LOG_TAG + "syncFromGWS: failed to serialize object");
        }

        Map<String, Object> finalResultMap = resultMap;
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        RealmModel realm = innerSession.realms().getRealm(realmId);
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        HashMap<String,String> configData = new HashMap<>();
                        try {
                            configData= JsonSerialization.readValue((String) representation, HashMap.class);
                        } catch (IOException e) {
                            eventBuilder.event(EventType.GOOGLE_WS_SYNC_ERROR);
                            eventBuilder.realm(realm);
                            eventBuilder.error(e.getMessage());
                            log.error(e.getMessage());
                        }
                        String connId = configData.get("connectionId");
                        UserSyncConfigModel userSyncConfigModel = realm.getUserSyncConfigByProviderId(connId);
                        Map<String, String> connConfig = userSyncConfigModel.getConfig();

                        SyncHistory syncHistory = new SyncHistory();
                        syncHistory.setCreatedTime(Long.parseLong(configData.get("createdTime")));

                        // setting mapperType
                        String mapperType = connConfig.get("mapperType");
                        if(TextUtils.isBlank(mapperType)){
                            mapperType = "username";
                        }

                        GoogleCredentials credentials =
                                GoogleCredentials.newBuilder()
                                        .setAccessToken(new AccessToken(connConfig.get("access_token"), null))
                                        .build();

                        try{
                            Directory directory =
                                    new Directory.Builder(
                                            GoogleNetHttpTransport.newTrustedTransport(),
                                            GsonFactory.getDefaultInstance(),
                                            new HttpCredentialsAdapter(credentials))
                                            .setApplicationName("sync_test")
                                            .build();
                            boolean hasNextPageinGrps = false;
                            Groups groups = GsonFactory.getDefaultInstance().fromString((String) finalResultMap.get("groups"), Groups.class);
                            if(groups.getGroups() != null) {
                                do {
                                    groups.getGroups().forEach(group -> {
                                        List<GroupModel> groupModels = innerSession.groups().searchForGroupByNameStream(realm, group.getName(), -1, -1).collect(Collectors.toList());
                                        if (groupModels.size() == 0) {
                                            GroupModel groupModel = realm.createGroup(KeycloakModelUtils.generateId(), group.getName());
                                            groupModel.setSingleAttribute(SYNCED_FROM, GOOGLE_WS);
                                        } else {
                                            groupModels.get(0).setSingleAttribute(SYNCED_FROM, GOOGLE_WS);
                                        }
                                    });
                                    if (groups.getNextPageToken() != null) {
                                        groups = directory.groups().list().setCustomer("my_customer").setPageToken(groups.getNextPageToken()).execute();
                                        hasNextPageinGrps = true;
                                    } else {
                                        hasNextPageinGrps = false;
                                    }
                                } while (hasNextPageinGrps);
                            }

                            //create Users
                            Users users = directory.users().list().setCustomer("my_customer").execute();
                            boolean hasNextPageinUser = false;
                            do{
                                //deleting users that deleted in Google
                                deleteUsers(users,realm,innerSession,mapperType,syncHistory);
                                String finalMapperType = mapperType;
                                users.getUsers().forEach(user -> {
                                    UserModel currentUser = null;
                                    if(finalMapperType.equalsIgnoreCase("email")) {
                                        if(user.getPrimaryEmail() != null){
                                            currentUser = innerSession.users().getUserByEmail(realm,user.getPrimaryEmail());
                                        }
                                    } else {
                                        currentUser = innerSession.users().getUserByUsername(realm, user.getName().getFullName());
                                    }

                                    try{
                                        HashMap<String, Object> googleUserRoles =(HashMap<String, Object>) finalResultMap.get("userRoles");
                                        if(currentUser == null){
                                            UserModel imported = importUserFromDirectory(directory,innerSession,realm,user, userSyncConfigModel.getEnrollmentOption(), finalMapperType, (List<String>) googleUserRoles.get(user.getId()));
                                            if(imported != null){
                                                syncHistory.increaseAdded();
                                            }
                                            currentUser = imported;
                                        } else {
                                            if(updateUserFromDirectory(directory,innerSession,currentUser,realm,user, userSyncConfigModel.getEnrollmentOption(), finalMapperType, (List<String>) googleUserRoles.get(user.getId()))){
                                                syncHistory.increaseUpdated();
                                            }
                                        }
                                        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, new AuthDetails(), innerSession, innerSession.getContext().getConnection());
                                        adminEvent.realm(realm);
                                        List<String> userRoles = (List<String>) googleUserRoles.get(user.getId());
                                        for(String userRole : userRoles){
                                            GeneralHelper.createAndAddRole(innerSession, realm, adminEvent, currentUser, "G-" + userRole);
                                        }
                                    } catch (ModelDuplicateException | IOException e){
                                        syncHistory.increaseFailed();
                                    }
                                });

                                if(users.getNextPageToken() != null){
                                    users = directory.users().list().setCustomer("my_customer").setPageToken(groups.getNextPageToken()).execute();
                                    hasNextPageinUser = true;
                                } else {
                                    hasNextPageinUser = false;
                                }
                            } while (hasNextPageinUser);

                            //delete groups
                            do{
                                //deleting Groups that deleted in Google
                                deleteGroups(groups,innerSession,realm);
                                if(groups.getNextPageToken() != null){
                                    groups = directory.groups().list().setCustomer("my_customer").setPageToken(groups.getNextPageToken()).execute();
                                    hasNextPageinGrps = true;
                                }
                                else{
                                    hasNextPageinGrps = false;
                                }
                            }while (hasNextPageinGrps);


                            syncHistory.setCompletedTime(System.currentTimeMillis());
                            innerSession.getContext().setRealm(realm);
                            eventBuilder.event(EventType.GOOGLE_WS_SYNC_SUCCESS);
                            eventBuilder.realm(realm);
                            eventBuilder.client(connId);
                            eventBuilder.user(configData.get("triggerId"));
                            eventBuilder.detail("triggeredUserId", userId);
                            setEventDetails(eventBuilder,syncHistory);
                            eventBuilder.success();

                            //remove the realmAttribute
                            realm.removeAttribute("gwsSyncStarted");
                            realm.removeAttribute(connId);

                        } catch (RuntimeException | GeneralSecurityException | IOException e ) {
                            log.error(e.getMessage());
                            realm.removeAttribute("gwsSyncStarted");
                            realm.removeAttribute(connId);
                        }
                    }
                });
            }
        });
    }


    private static void setEventDetails(EventBuilder evtBuilder,SyncHistory syncHistory){
        evtBuilder.detail("syncProvider", "Google Workspace");
        evtBuilder.detail("created_time",syncHistory.getCreatedTime().toString());
        evtBuilder.detail("completed_time",syncHistory.getCompletedTime().toString());
        evtBuilder.detail("importedCnt",String.valueOf(syncHistory.getAdded()));
        evtBuilder.detail("updatedCnt", String.valueOf(syncHistory.getUpdated()));
        evtBuilder.detail("deletedCnt",String.valueOf(syncHistory.getRemoved()));
        evtBuilder.detail("totalSync", String.valueOf(syncHistory.getAdded() + syncHistory.getUpdated() + syncHistory.getRemoved()));
        evtBuilder.detail("failed", String.valueOf(syncHistory.getFailed()));
        evtBuilder.detail("status",syncHistory.getStatus());
    }

    private static UserModel importUserFromDirectory(Directory service, KeycloakSession session, RealmModel realm, User user, String enrollmentOption, String mapperType, List<String> userRoles) throws ModelDuplicateException, IOException {
        String userName = "";
        if(mapperType.equalsIgnoreCase("email") && user.getPrimaryEmail() != null){
           userName = user.getPrimaryEmail();
        } else if(mapperType.equalsIgnoreCase("username")){
            userName = user.getName().getFullName();
        }

        if(!TextUtils.isBlank(userName)){
            UserModel importedUser = session.users().addUser(realm,userName);
            if(mapperType.equalsIgnoreCase("email")){
                importedUser.setEmail(user.getPrimaryEmail());
            }

            if(user.getName().getGivenName() != null){
                importedUser.setFirstName(user.getName().getGivenName());
            }
            if(user.getName().getFamilyName() != null){
                importedUser.setLastName(user.getName().getFamilyName());
            }
            if(user.getArchived() != null){
                importedUser.setEnabled(!user.getArchived());
            }
            importedUser.setSingleAttribute(SYNCED_FROM,GOOGLE_WS);
            if(userRoles != null && !userRoles.isEmpty())
                importedUser.setAttribute(SYNCED_ROLES, userRoles);

            enrollUser(session,realm,enrollmentOption,importedUser);
            addGroupMenmbership(service,user,realm,importedUser);
            return importedUser;
        }

        return null;
    }

    private static boolean updateUserFromDirectory(Directory service, KeycloakSession session, UserModel currentUser, RealmModel realm, User user, String enrollmentOption, String mapperType, List<String> userRoles) throws ModelDuplicateException, IOException {
        boolean isUpdated = false;
        if((user.getName().getGivenName() != null) && (!user.getName().getGivenName().equals(currentUser.getFirstName()))){
            isUpdated = true;
            currentUser.setFirstName(user.getName().getGivenName());
        }
        if((user.getName().getFamilyName() != null) && (!user.getName().getFamilyName().equals(currentUser.getLastName()))){
            isUpdated = true;
            currentUser.setLastName(user.getName().getFamilyName());
        }
        if((user.getArchived() != null) && (!user.getArchived().equals(currentUser.isEnabled()))){
            isUpdated = true;
            currentUser.setEnabled(!user.getArchived());
        }
        if((mapperType.equalsIgnoreCase("username") && user.getPrimaryEmail() != null &&
                session.users().getUserByEmail(realm,user.getPrimaryEmail()) == null) && (!user.getPrimaryEmail().equals(currentUser.getEmail()))) {
            isUpdated = true;
            currentUser.setEmail(user.getPrimaryEmail());
        }
        currentUser.setSingleAttribute(SYNCED_FROM, GOOGLE_WS);
        if(userRoles != null && !userRoles.isEmpty())
            currentUser.setAttribute(SYNCED_ROLES, userRoles);

        currentUser.getGroupsStream().filter(groupModel -> {
            String groupAttribute = groupModel.getFirstAttribute(SYNCED_FROM);
            return groupAttribute != null && groupAttribute.equals(GOOGLE_WS);
        }).forEach(currentUser::leaveGroup);

        enrollUser(session,realm,enrollmentOption,currentUser);
        addGroupMenmbership(service,user,realm,currentUser);
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

    private static void addGroupMenmbership(Directory service,User user,RealmModel realm,UserModel userModel) throws IOException {
        Groups groups = service.groups().list().setUserKey(user.getId()).execute();
        if(groups.getGroups() != null){
            groups.getGroups().forEach(g -> {
                List<GroupModel> grps = realm.searchForGroupByNameStream(g.getName(), -1,-1).collect(Collectors.toList());
                if(grps.size() > 0){
                    GroupModel model = grps.get(0);
                    if (model != null && !userModel.isMemberOf(model)) {
                        userModel.joinGroup(model);
                    }
                }
            });
        }
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
    private static void deleteUsers(Users users, RealmModel realmModel, KeycloakSession session, String mapperType, SyncHistory syncHistory) {

        //Getting list of users from realm that already imported from google
        List<UserModel> listOfUser = session.users()
                .searchForUserByUserAttributeStream(realmModel, SYNCED_FROM, GOOGLE_WS, true)
                .collect(Collectors.toList());

        if (listOfUser.isEmpty()) {
            log.info("GoogleSyncRunner :: deleteUsers :: User list that already imported from Google were empty");
            return;
        }

        log.info("GoogleSyncRunner :: deleteUsers :: triggered");

        Map<String, String> gooleUsersMap = new HashMap<>();
        int deletedUserCount = 0;

        //Getting list of users from Google
        users.getUsers().forEach(user -> {
            if (mapperType.equalsIgnoreCase("email")) {
                gooleUsersMap.put(user.getPrimaryEmail(), "true");
            } else if (mapperType.equalsIgnoreCase("username")) {
                gooleUsersMap.put(user.getName().getFullName(), "true");
            }
        });

        //Deleting user that present in QV and deleted in Google
        try{
            for (UserModel userModel : listOfUser) {
                if (mapperType.equalsIgnoreCase("email")) {
                    if (gooleUsersMap.get(userModel.getEmail()) == null) {
                        deletedUserCount = deleteIfUserNotAvailable(realmModel, session, userModel) ? deletedUserCount+1 : deletedUserCount;
                    }
                } else if (mapperType.equalsIgnoreCase("username")) {
                    if (gooleUsersMap.get(userModel.getUsername()) == null) {
                        deletedUserCount = deleteIfUserNotAvailable(realmModel, session, userModel) ? deletedUserCount+1 : deletedUserCount;
                    }
                }
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }

        log.info("GoogleSyncRunner :: deleteUsers :: Deleted user count = "+deletedUserCount);
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

    private static void deleteGroups(Groups groups, KeycloakSession session, RealmModel realm){

        //Getting list of Groups from realm that already imported from Google
        List<GroupModel> listOfGroups = session.groups().getGroupsStream(realm)
                .filter(groupModel -> {
                    String groupAttribute = groupModel.getFirstAttribute(SYNCED_FROM);
                    return groupAttribute != null && groupAttribute.equals(GOOGLE_WS);
                })
                .collect(Collectors.toList());

        if(listOfGroups.size() == 0){
            log.info("GoogleWSSyncRunner :: deleteGroups :: Groups list that already imported from Google were empty");
            return;
        }

        log.info("GoogleWSSyncRunner :: deleteGroups :: triggered");

        Map<String,String> googleGroupsMap = new HashMap<>();
        int deletedGroupCount = 0;

        //Getting list of Groups from Google
        groups.getGroups().forEach(group -> {
            googleGroupsMap.put(group.getName(),"true");

        });

        //Deleting Groups that present in QV and deleted in Google
        try {
            for(GroupModel groupModel : listOfGroups){

                if (googleGroupsMap.get(groupModel.getName()) == null && !session.users().getGroupMembersStream(realm, groupModel).findAny().isPresent()) {
                    deletedGroupCount = realm.removeGroup(groupModel) ? deletedGroupCount+1 : deletedGroupCount;
                }
            }

        }catch (Exception e){
            log.error(e.getMessage());
        }

        log.info("GoogleSyncRunner :: deleteGroups :: Deleted Groups count = " + deletedGroupCount);

    }

}


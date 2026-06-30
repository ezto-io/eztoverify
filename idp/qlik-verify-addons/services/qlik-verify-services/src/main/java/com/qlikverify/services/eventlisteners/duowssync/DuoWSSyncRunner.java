package com.qlikverify.services.eventlisteners.duowssync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.qlikverify.services.helpers.GeneralHelper;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.json.JSONException;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.actiontoken.userinvite.UserInviteLinkActionToken;
import org.keycloak.avatar.AvatarProvider;
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

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.keycloak.util.JsonSerialization;

import static org.keycloak.models.Constants.*;


public class DuoWSSyncRunner {
    private static final Logger log = Logger.getLogger(DuoWSSyncListenerProvider.class);

    private static Executor executor = Executors.newCachedThreadPool();
    private static int year = Calendar.getInstance().get(Calendar.YEAR);

    public static void syncFromDWS(KeycloakSession session, String realmId, String representation, Object result, String userId) {

        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        RealmModel realm = innerSession.realms().getRealm(realmId);
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        Gson gson = new Gson();
                        Map<String, Object> resultMap = new HashMap<>();
                        try{
                            resultMap = JsonSerialization.readValue(result.toString(), Map.class);
                        } catch (Exception e){
                            eventBuilder.event(EventType.DUO_WS_SYNC_ERROR);
                            eventBuilder.realm(realm);
                            eventBuilder.error(e.getMessage());
                            log.error(e.getMessage());
                        }

                        JSONObject grpMetadata;
                        JSONArray groups = new JSONArray();

                        JSONArray users = new JSONArray();
                        JSONObject usrMetadata;

                        JSONArray tempUsers = new JSONArray();
                        JSONArray tempGroups = new JSONArray();

                        ArrayList groupsMap = (ArrayList) resultMap.get("groups");
                        groupsMap.stream().forEach(x -> tempGroups.put(gson.fromJson(x.toString(), JSONObject.class)));

                        HashMap<String, Object> hashMap = new HashMap<>();
                        for(int i =0; i< tempGroups.length(); i++){
                            hashMap.clear();
                            try{
                                Iterator<String> keys = tempGroups.getJSONObject(i).keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    Object value = tempGroups.getJSONObject(i).get(key);
                                    hashMap.put(key, value);
                                }
                            } catch (Exception e){
                                eventBuilder.event(EventType.DUO_WS_SYNC_ERROR);
                                eventBuilder.realm(realm);
                                eventBuilder.error(e.getMessage());
                                log.error(e.getMessage());
                            }
                            groups.put(new JSONObject(hashMap));
                        }

                        grpMetadata = gson.fromJson(resultMap.get("grpMetadata").toString(), JSONObject.class);

                        ArrayList usersMap = (ArrayList) resultMap.get("users");
                        usersMap.stream().forEach(x -> tempUsers.put(gson.fromJson(x.toString(), JSONObject.class)));

                        for(int i =0; i< tempUsers.length(); i++){
                            hashMap.clear();
                            try{
                                Iterator<String> keys = tempUsers.getJSONObject(i).keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    Object value = tempUsers.getJSONObject(i).get(key);
                                    hashMap.put(key, value);
                                }
                            } catch (Exception e){
                                eventBuilder.event(EventType.DUO_WS_SYNC_ERROR);
                                eventBuilder.realm(realm);
                                eventBuilder.error(e.getMessage());
                                log.error(e.getMessage());
                            }
                            users.put(new JSONObject(hashMap));
                        }

                        usrMetadata = gson.fromJson(resultMap.get("userMetadata").toString(), JSONObject.class);

                        HashMap<String,String> configData = new HashMap<>();

                        try {
                            configData= JsonSerialization.readValue(representation, HashMap.class);
                        } catch (JsonProcessingException e) {
                            eventBuilder.event(EventType.DUO_WS_SYNC_ERROR);
                            eventBuilder.realm(realm);
                            eventBuilder.error(e.getMessage());
                            log.error(e.getMessage());
                        } catch (IOException e) {
                            eventBuilder.event(EventType.DUO_WS_SYNC_ERROR);
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
                        try {
                            //create groups

                                // iterate users and print them
                            if(groups != null) {
                                for (int i = 0; i < groups.length(); i++) {
                                    JSONObject grp = groups.getJSONObject(i);
                                    List<GroupModel> groupModels = innerSession.groups().searchForGroupByNameStream(realm, String.valueOf(grp.get("name")), -1, -1).collect(Collectors.toList());
                                    if (groupModels.size() == 0) {
                                        GroupModel groupModel = realm.createGroup(KeycloakModelUtils.generateId(), String.valueOf(grp.get("name")));
                                        groupModel.setSingleAttribute(SYNCED_FROM, DUO_WS);
                                    } else
                                        groupModels.get(0).setSingleAttribute(SYNCED_FROM, DUO_WS);
                                }
                            }


                            //create users
                            String finalMapperType = mapperType;

                            if(users != null) {
                                //deleting users that deleted in duo
                                deleteUsers(users, realm, innerSession, mapperType, syncHistory);

                                for (int i = 0; i < users.length(); i++) {
                                    JSONObject user = users.getJSONObject(i);
                                    UserModel currentUser = null;
                                    if (finalMapperType.equalsIgnoreCase("email")) {
                                        if (user.get("email") != null) {
                                            currentUser = innerSession.users().getUserByEmail(realm, String.valueOf(user.get("email")));
                                        }
                                    } else {
                                        currentUser = innerSession.users().getUserByUsername(realm, String.valueOf(user.get("username")));
                                    }


                                    try {
                                        String role = "user";
                                        if(user.has("role"))
                                            role = user.get("role").toString();
                                        if (currentUser == null) {
                                            UserModel imported = importUserFromDirectory(innerSession, realm, user, userSyncConfigModel.getEnrollmentOption(), finalMapperType, role);
                                            if (imported != null) {
                                                syncHistory.increaseAdded();
                                            }
                                            currentUser = imported;
                                        } else {
                                            if(updateUserFromDirectory(innerSession, currentUser, realm, user, userSyncConfigModel.getEnrollmentOption(), finalMapperType, role)){
                                                syncHistory.increaseUpdated();
                                            }
                                        }
                                        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, new AuthDetails(), innerSession, innerSession.getContext().getConnection());
                                        adminEvent.realm(realm);
                                        GeneralHelper.createAndAddRole(innerSession, realm, adminEvent, currentUser, "D-" + role);
                                    } catch (ModelDuplicateException | IOException e) {
                                        syncHistory.increaseFailed();
                                    }
                                }
                            }

                            //deleting  groups

                            if(groups != null){
                                //deleting Groups that deleted in duo
                                deleteGroups(groups, innerSession, realm);
                            }


                            syncHistory.setCompletedTime(System.currentTimeMillis());
                            innerSession.getContext().setRealm(realm);
                            eventBuilder.event(EventType.DUO_WS_SYNC_SUCCESS);
                            eventBuilder.realm(realm);
                            eventBuilder.client(connId);
                            eventBuilder.user(configData.get("triggerId"));
                            eventBuilder.detail("triggeredUserId", userId);
                            setEventDetails(eventBuilder,syncHistory);
                            eventBuilder.success();


                            //remove the realmAttribute
                            realm.removeAttribute("dwsSyncStarted");
                            realm.removeAttribute(connId);

                        } catch (JSONException | RuntimeException e) {
                            log.error(e.getMessage());
                            realm.removeAttribute("dwsSyncStarted");
                            realm.removeAttribute(connId);
                        } catch (Exception e) {
                            log.error(e.getMessage());
                            realm.removeAttribute("dwsSyncStarted");
                            realm.removeAttribute(connId);
                        }
                    }
                });
            }
        });
    }


    private static void setEventDetails(EventBuilder evtBuilder,SyncHistory syncHistory){
        evtBuilder.detail("syncProvider", "Duo Workspace");
        evtBuilder.detail("created_time",syncHistory.getCreatedTime().toString());
        evtBuilder.detail("completed_time",syncHistory.getCompletedTime().toString());
        evtBuilder.detail("importedCnt",String.valueOf(syncHistory.getAdded()));
        evtBuilder.detail("updatedCnt", String.valueOf(syncHistory.getUpdated()));
        evtBuilder.detail("deletedCnt",String.valueOf(syncHistory.getRemoved()));
        evtBuilder.detail("totalSync", String.valueOf(syncHistory.getAdded() + syncHistory.getUpdated() + syncHistory.getRemoved()));
        evtBuilder.detail("failed", String.valueOf(syncHistory.getFailed()));
        evtBuilder.detail("status",syncHistory.getStatus());
    }

    private static UserModel importUserFromDirectory(KeycloakSession session, RealmModel realm, JSONObject user, String enrollmentOption, String mapperType, String role) throws ModelDuplicateException, IOException, JSONException {
        String userName = "";
        if(mapperType.equalsIgnoreCase("email") && !user.isNull("email")){
           userName = String.valueOf(user.get("email"));
        } else if(mapperType.equalsIgnoreCase("username")){
            userName = String.valueOf(user.get("username"));
        }

        if(!TextUtils.isBlank(userName)){
            UserModel importedUser = session.users().addUser(realm,userName);
            importedUser.setEnabled(true);
            if(mapperType.equalsIgnoreCase("email") && !user.isNull("email")){
                importedUser.setEmail(String.valueOf(user.get("email")));
            }
            if(!user.isNull("firstname")){
                importedUser.setFirstName(String.valueOf(user.get("firstname")));
            }
            if(!user.isNull("lastname")){
                importedUser.setLastName(String.valueOf(user.get("lastname")));
            }

            importedUser.setSingleAttribute(SYNCED_FROM, DUO_WS);
            importedUser.setSingleAttribute(SYNCED_ROLES, role);

            enrollUser(session,realm,enrollmentOption,importedUser);
            addGroupMenmbership(user,realm,importedUser);
            return importedUser;
        }

        return null;
    }

    private static boolean updateUserFromDirectory(KeycloakSession session, UserModel currentUser, RealmModel realm, JSONObject user, String enrollmentOption, String mapperType, String role) throws ModelDuplicateException, IOException, JSONException {
        boolean isUpdated = false;
        currentUser.setEnabled(true);
        if((!user.isNull("firstname")) && (!currentUser.getFirstName().equals(user.get("firstname").toString()))){
            isUpdated = true;
            currentUser.setFirstName(String.valueOf(user.get("firstname")));
        }
        if((!user.isNull("lastname")) && (!currentUser.getLastName().equals(user.get("lastname").toString()))){
            isUpdated = true;
            currentUser.setLastName(String.valueOf(user.get("lastname")));
        }
        if((mapperType.equalsIgnoreCase("username") && (!user.isNull("email") &&
                session.users().getUserByEmail(realm,String.valueOf(user.get("email"))) == null)) && (!user.get("email").toString().equals(currentUser.getEmail()))) {
            isUpdated = true;
            currentUser.setEmail(String.valueOf(user.get("email")));
        }
        currentUser.setSingleAttribute(SYNCED_FROM, DUO_WS);
        currentUser.setSingleAttribute(SYNCED_ROLES, role);

        currentUser.getGroupsStream().filter(groupModel -> {
            String groupAttribute = groupModel.getFirstAttribute(SYNCED_FROM);
            return groupAttribute != null && groupAttribute.equals(DUO_WS);
        }).forEach(currentUser::leaveGroup);


        enrollUser(session,realm,enrollmentOption,currentUser);
        addGroupMenmbership(user,realm,currentUser);
        return isUpdated;
    }

    private static void enrollUser(KeycloakSession session,RealmModel realm,String enrollmentOption,UserModel user){
        EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
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
                       eventBuilder.event(EventType.DUO_WS_SYNC_ERROR);
                       eventBuilder.realm(realm);
                       eventBuilder.error(e.getMessage());
                       log.error(e.getMessage());
                   } catch (ConfigException e) {
                       eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                       eventBuilder.error(e.getMessage());
                       log.error(e.getMessage());
                   }
               }
           }

       }
    }

    private static void addGroupMenmbership(JSONObject user,RealmModel realm,UserModel userModel) throws IOException, JSONException {
        JSONArray groups = new JSONArray();
        if(!user.has("groups"))
            return;
        groups.put((JSONObject) user.get("groups"));
        if(groups.length() > 0){
            for (int i=0; i<groups.length();i++){
                JSONObject grp = groups.getJSONObject(i);
                try{
                    List<GroupModel> grps = realm.searchForGroupByNameStream(String.valueOf(grp.get("name")), -1,-1).collect(Collectors.toList());
                    if(grps.size() > 0) {
                        GroupModel model = grps.get(0);
                        if (model != null && !userModel.isMemberOf(model)) {
                            userModel.joinGroup(model);
                        }
                    }
                } catch (Exception e){
                    log.error(e.getMessage());
                }

            }
        }
    }

    private static  Map<String, Object> formatDuration(long valueInSeconds) {
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

    private static void deleteUsers(JSONArray users, RealmModel realmModel, KeycloakSession session, String mapperType, SyncHistory syncHistory) throws JSONException {

        //Getting list of users from realm that already imported from duo
        List<UserModel> listOfUser = session.users()
                .searchForUserByUserAttributeStream(realmModel, SYNCED_FROM, DUO_WS, true)
                .collect(Collectors.toList());

        if (listOfUser.isEmpty()) {
            log.info("DuoSyncRunner :: deleteUsers :: User list that already imported from Duo were empty");
            return;
        }

        log.info("DuoSyncRunner :: deleteUsers :: triggered");

        Map<String, String> duoUsersMap = new HashMap<>();
        int deletedUserCount = 0;

        //Getting list of users from Duo
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            if (mapperType.equalsIgnoreCase("email")) {
                String email = (String) user.get("email");
                if(email != null)
                    duoUsersMap.put(email, "true");
            } else if (mapperType.equalsIgnoreCase("username")) {
                String userName = (String) user.get("username");
                if(userName != null)
                    duoUsersMap.put(userName, "true");
            }
        }

        //Deleting user that present in QV and deleted in Duo
        try{
            for (UserModel userModel : listOfUser) {
                if (mapperType.equalsIgnoreCase("email")) {
                    if (duoUsersMap.get(userModel.getEmail()) == null) {
                        deletedUserCount = deleteIfUserNotAvailable(realmModel, session, userModel) ? deletedUserCount+1 : deletedUserCount;
                    }
                } else if (mapperType.equalsIgnoreCase("username")) {
                    if (duoUsersMap.get(userModel.getUsername()) == null) {
                        deletedUserCount = deleteIfUserNotAvailable(realmModel, session, userModel) ? deletedUserCount+1 : deletedUserCount;
                    }
                }
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }

        log.info("DuoSyncRunner :: deleteUsers :: Deleted user count = "+deletedUserCount);
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

    private static void deleteGroups(JSONArray groups, KeycloakSession session, RealmModel realm) throws JSONException {

        //Getting list of Groups from realm that already imported from Duo
        List<GroupModel> listOfGroups = session.groups().getGroupsStream(realm)
                .filter(groupModel -> {
                    String groupAttribute = groupModel.getFirstAttribute(SYNCED_FROM);
                    return groupAttribute != null && groupAttribute.equals(DUO_WS);
                })
                .collect(Collectors.toList());
        if(listOfGroups.size() == 0){
            log.info("DuoSyncRunner :: deleteGroups :: Groups list that already imported from Duo were empty");
            return;
        }

        log.info("DuoSyncRunner :: deleteGroups :: triggered");

        Map<String,String> duoGroupsMap = new HashMap<>();
        int deletedGroupCount = 0;

        //Getting list of Groups from duo
        for (int i = 0; i < groups.length(); i++) {
            JSONObject grp = groups.getJSONObject(i);
            String groupName = (String) grp.get("name");
            if(groupName != null) {
                duoGroupsMap.put(groupName, "true");
            }
        }

        //Deleting Groups that that were empty in QV and deleted in Duo
        try {
            for(GroupModel groupModel : listOfGroups){

                if (duoGroupsMap.get(groupModel.getName()) == null && !session.users().getGroupMembersStream(realm, groupModel).findAny().isPresent()) {
                    deletedGroupCount = realm.removeGroup(groupModel) ? deletedGroupCount+1 : deletedGroupCount;
                }
            }

        }catch (Exception e){
            log.error(e.getMessage());
        }

        log.info("DuoSyncRunner :: deleteGroups :: Deleted Groups count = "+deletedGroupCount);

    }
}

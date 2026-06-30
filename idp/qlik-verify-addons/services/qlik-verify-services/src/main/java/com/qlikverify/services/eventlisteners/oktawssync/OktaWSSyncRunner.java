package com.qlikverify.services.eventlisteners.oktawssync;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.directory.model.Groups;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.client.Clients;
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
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.exceptions.ConfigException;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.RoleContainerResource;
import org.keycloak.services.resources.admin.RoleResource;
import org.keycloak.storage.user.SyncHistory;
import org.keycloak.util.JsonSerialization;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.GroupApi;
import org.openapitools.client.api.UserApi;
import org.openapitools.client.model.Group;
import org.openapitools.client.model.GroupProfile;
import org.openapitools.client.model.User;
import org.openapitools.client.model.UserProfile;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import com.google.api.client.json.gson.GsonFactory;

import static org.keycloak.models.Constants.*;


public class OktaWSSyncRunner {
    private static final Logger log = Logger.getLogger(OktaWSSyncListenerProvider.class);

    private static Executor executor = Executors.newCachedThreadPool();
    private static int year = Calendar.getInstance().get(Calendar.YEAR);

    public static void syncFromOWS(KeycloakSession session, String realmId, String representation, Object result, String userId) {

        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        RealmModel realm = innerSession.realms().getRealm(realmId);
                        EventBuilder eventBuilder = new EventBuilder(realm, innerSession, null);
                        List<Group> groupList = new ArrayList<>();
                        List<User> userList = new ArrayList<>();
                        HashMap<String, List<String>> userRolesMap = new HashMap<>();


                        try{
                            Map<String, Object> resultMap = JsonSerialization.readValue(result.toString(), Map.class);
                            Gson gson = new Gson();

                            ArrayList groupsMap = (ArrayList) resultMap.get("groups");
                            groupsMap.stream().forEach(x -> groupList.add(gson.fromJson(x.toString(), Group.class)));

                            ArrayList usersMap = (ArrayList) resultMap.get("users");
                            usersMap.stream().forEach(x -> userList.add(gson.fromJson(x.toString(), User.class)));

                            userRolesMap = (HashMap<String, List<String>>) resultMap.get("userRoles");

                        } catch (Exception e){
                            eventBuilder.event(EventType.OKTA_WS_SYNC_ERROR);
                            eventBuilder.realm(realm);
                            eventBuilder.error(e.getMessage());
                            log.error(e.getMessage());
                        }


                        HashMap<String, String> configData = new HashMap<>();

                        try {
                            configData = JsonSerialization.readValue(representation, HashMap.class);
                        } catch (JsonProcessingException e) {
                            eventBuilder.event(EventType.OKTA_WS_SYNC_ERROR);
                            eventBuilder.realm(realm);
                            eventBuilder.error(e.getMessage());
                            log.error(e.getMessage());
                        } catch (IOException e) {
                            eventBuilder.event(EventType.OKTA_WS_SYNC_ERROR);
                            eventBuilder.realm(realm);
                            eventBuilder.error(e.getMessage());
                            log.error(e.getMessage());
                        }
                        String connId = configData.get("connectionId");
                        UserSyncConfigModel userSyncConfigModel = realm.getUserSyncConfigByProviderId(connId);
                        Map<String, String> connConfig = userSyncConfigModel.getConfig();

                        String domainName = userSyncConfigModel.getConfig().get("domainName");

                        String apiToken = userSyncConfigModel.getConfig().get("apiToken");
                        String oktaOrgUrl = "https://" + domainName + ".okta.com";
                        ApiClient client = Clients.builder()
                                .setOrgUrl(oktaOrgUrl)
                                .setClientCredentials(new TokenClientCredentials(apiToken))
                                .build();

                        SyncHistory syncHistory = new SyncHistory();
                        syncHistory.setCreatedTime(Long.parseLong(configData.get("createdTime")));

                        // setting mapperType
                        String mapperType = connConfig.get("mapperType");
                        if (TextUtils.isBlank(mapperType)) {
                            mapperType = "username";
                        }
                        


                        try {

                            //create groups
                            createGroups(groupList, realm, innerSession);

                            //create Users
                            createUser(client, userList, realm, innerSession, mapperType, syncHistory, userSyncConfigModel, userRolesMap);

                            //deleting Groups that deleted in okta only if users of that groups were empty in QV
                            deleteGroups(groupList,innerSession,realm);

                            syncHistory.setCompletedTime(System.currentTimeMillis());
                            innerSession.getContext().setRealm(realm);
                            eventBuilder.event(EventType.OKTA_WS_SYNC_SUCCESS);
                            eventBuilder.realm(realm);
                            eventBuilder.client(connId);
                            eventBuilder.user(configData.get("triggerId"));
                            eventBuilder.detail("triggeredUserId", userId);
                            setEventDetails(eventBuilder, syncHistory);
                            eventBuilder.success();

                            //remove the realmAttribute
                            realm.removeAttribute("owsSyncStarted");
                            realm.removeAttribute(connId);

                        } catch (Exception e) {
                            log.error(e.getMessage());
                            realm.removeAttribute("owsSyncStarted");
                            realm.removeAttribute(connId);
                        }
                    }
                });
            }
        });
    }

    private static void createGroups(List<Group> groups, RealmModel realm, KeycloakSession innerSession) {


        groups.forEach(group -> {
            GroupProfile groupProfile = group.getProfile();
            if (groupProfile != null) {
                List<GroupModel> groupModels = innerSession.groups().searchForGroupByNameStream(realm, groupProfile.getName(), -1, -1).collect(Collectors.toList());
                if (groupModels.size() == 0) {
                    GroupModel groupModel = realm.createGroup(KeycloakModelUtils.generateId(), group.getProfile().getName());
                    groupModel.setSingleAttribute(SYNCED_FROM,OKTA_WS);
                }
                else{
                    groupModels.get(0).setSingleAttribute(SYNCED_FROM,OKTA_WS);
                }

            } else {
                realm.removeAttribute("owsSyncStarted");
                log.error("OktaWSSyncRunner :: createGroups :: Group profile is null");
                throw new RuntimeException("Group profile is null");
            }
        });
    }

    private static void createUser(ApiClient apiClient, List<User> users, RealmModel realm,
                                   KeycloakSession innerSession, String mapperType,
                                   SyncHistory syncHistory, UserSyncConfigModel userSyncConfigModel, HashMap<String, List<String>> userRolesMap) {

        //deleting users that deleted in okta
        deleteUsers(users,realm,innerSession,mapperType,syncHistory);

        users.forEach(user -> {
                    UserProfile userProfile = user.getProfile();
                    if (userProfile != null) {
                        UserModel currentUser = null;
                        if (mapperType.equalsIgnoreCase("email")) {
                            if (userProfile.getEmail() != null) {
                                currentUser = innerSession.users().getUserByEmail(realm, userProfile.getEmail());
                            }
                        } else {
                            currentUser = innerSession.users().getUserByUsername(realm, userProfile.getLogin());
                        }

                        try {
                            if (currentUser == null) {
                                UserModel imported = importUserFromDirectory(apiClient, innerSession, realm, user, userSyncConfigModel.getEnrollmentOption(), mapperType, userRolesMap.get(user.getId()));
                                if (imported != null) {
                                    syncHistory.increaseAdded();
                                }
                                currentUser = imported;
                            } else {
                                 if(updateUserFromDirectory(apiClient, innerSession, currentUser, realm, user, userSyncConfigModel.getEnrollmentOption(), mapperType, userRolesMap.get(user.getId()))){
                                     syncHistory.increaseUpdated();
                                 }
                            }
                        } catch (ModelDuplicateException | IOException e) {
                            realm.removeAttribute("owsSyncStarted");
                            syncHistory.increaseFailed();
                            log.error(e.getMessage());
                        }
                        AdminEventBuilder adminEvent = new AdminEventBuilder(realm, new AuthDetails(), innerSession, innerSession.getContext().getConnection());
                        adminEvent.realm(realm);
                        List<String> userRoles = userRolesMap.get(user.getId());
                        for(String userRole : userRoles){
                            GeneralHelper.createAndAddRole(innerSession, realm, adminEvent, currentUser, "O-" + userRole);
                        }

                    } else {
                        realm.removeAttribute("owsSyncStarted");
                        log.error("OktaWSSyncRunner :: createUser :: user profile is null");
                        throw new RuntimeException("User profile is null");
                    }
                }
        );

    }

    private static void setEventDetails(EventBuilder evtBuilder, SyncHistory syncHistory) {
        evtBuilder.detail("syncProvider", "Okta Workspace");
        evtBuilder.detail("created_time", syncHistory.getCreatedTime().toString());
        evtBuilder.detail("completed_time", syncHistory.getCompletedTime().toString());
        evtBuilder.detail("importedCnt", String.valueOf(syncHistory.getAdded()));
        evtBuilder.detail("updatedCnt", String.valueOf(syncHistory.getUpdated()));
        evtBuilder.detail("deletedCnt",String.valueOf(syncHistory.getRemoved()));
        evtBuilder.detail("totalSync", String.valueOf(syncHistory.getAdded() + syncHistory.getUpdated() + syncHistory.getRemoved()));
        evtBuilder.detail("failed", String.valueOf(syncHistory.getFailed()));
        evtBuilder.detail("status", syncHistory.getStatus());
    }

    private static UserModel importUserFromDirectory(ApiClient apiClient, KeycloakSession session, RealmModel realm, User user, String enrollmentOption, String mapperType, List<String> userRoles) throws ModelDuplicateException, IOException {
        String userName = "";
        UserProfile userProfile = user.getProfile();
        if (userProfile != null) {
            if (mapperType.equalsIgnoreCase("email") && userProfile.getEmail() != null) {
                userName = userProfile.getEmail();
            } else if (mapperType.equalsIgnoreCase("username")) {
                userName = userProfile.getLogin();
            }

            if (!TextUtils.isBlank(userName)) {
                UserModel importedUser = session.users().addUser(realm, userName);
                importedUser.setEmail(userName);
                if (userProfile.getFirstName() != null) {
                    importedUser.setFirstName(userProfile.getFirstName());
                }
                if (userProfile.getLastName() != null) {
                    importedUser.setLastName(userProfile.getLastName());
                }
                if (user.getStatus() != null) {
                    // TODO :: if QV's admin was not active in OKTA it will disable admin user on QV so tenet will lost, so until found solution , every user will active
                    importedUser.setEnabled(true);
                }
                importedUser.setSingleAttribute(SYNCED_FROM,OKTA_WS);
                if(userRoles != null && !userRoles.isEmpty())
                    importedUser.setAttribute(SYNCED_ROLES, userRoles);

                enrollUser(session, realm, enrollmentOption, importedUser);
                addGroupMembership(apiClient, realm, importedUser,user.getId());
                return importedUser;
            }
        } else {
            log.error("OktaWSSyncRunner :: importUserFromDirectory :: user profile is null");
            realm.removeAttribute("owsSyncStarted");
            throw new RuntimeException("User profile is null");
        }

        return null;
    }

    private static boolean updateUserFromDirectory(ApiClient apiClient, KeycloakSession session, UserModel currentUser, RealmModel realm, User user, String enrollmentOption, String mapperType, List<String> userRoles) throws ModelDuplicateException, IOException {

        UserProfile userProfile = user.getProfile();
        boolean isUpdated = false;
        if (userProfile != null) {
            if ((userProfile.getFirstName() != null) && (!userProfile.getFirstName().equals(currentUser.getFirstName()))) {
                isUpdated = true;
                currentUser.setFirstName(userProfile.getFirstName());
            }
            if ((userProfile.getLastName() != null) && (!userProfile.getLastName().equals(currentUser.getLastName()))) {
                isUpdated = true;
                currentUser.setLastName(userProfile.getLastName());
            }
            if (user.getStatus() != null && !currentUser.isEnabled()) {
                isUpdated = true;
                // TODO :: if QV's admin was not active in OKTA it will disable admin user on QV so tenet will lost, so until found solution , every user will active
                currentUser.setEnabled(true);
            }
            if ((mapperType.equalsIgnoreCase("username") && userProfile.getEmail() != null &&
                    session.users().getUserByEmail(realm, userProfile.getEmail()) == null) && (!currentUser.getEmail().equals(userProfile.getEmail()))) {
                isUpdated = true;
                currentUser.setEmail(userProfile.getEmail());
            }

            currentUser.setSingleAttribute(SYNCED_FROM, OKTA_WS);
            if(userRoles != null && !userRoles.isEmpty())
                currentUser.setAttribute(SYNCED_ROLES, userRoles);

            currentUser.getGroupsStream().filter(groupModel -> {
                String groupAttribute = groupModel.getFirstAttribute(SYNCED_FROM);
                return groupAttribute != null && groupAttribute.equals(OKTA_WS);
            }).forEach(currentUser::leaveGroup);

            enrollUser(session, realm, enrollmentOption, currentUser);
            addGroupMembership(apiClient, realm, currentUser,user.getId());
            return isUpdated;
        } else {
            log.error("OktaWSSyncRunner :: updateUserFromDirectory :: user profile is null");
            realm.removeAttribute("owsSyncStarted");
            throw new RuntimeException("User profile is null");
        }
    }

    private static void enrollUser(KeycloakSession session, RealmModel realm, String enrollmentOption, UserModel user) {
        if (user.getEmail() != null) {

            if (enrollmentOption.equalsIgnoreCase("trustEmail")) {
                user.setEmailVerified(true);
            }

            if (!user.isEmailVerified() && enrollmentOption.equalsIgnoreCase("sendEnrollmentLink")) {
                if (user.getEmail() != null) {
                    session.getContext().setRealm(realm);
                    URI uriInfo = UriBuilder.fromUri(ModelToRepresentation.getWorkspaceUrl(session, realm) + "/auth").build();
                    int validityInSecs = realm.getActionTokenGeneratedByUserLifespan(UserInviteLinkActionToken.TOKEN_TYPE);
                    int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
                    UserInviteLinkActionToken token = new UserInviteLinkActionToken(user.getId(), user.getEmail(), absoluteExpirationInSecs, Constants.SSO_MANAGEMENT_CLIENT_ID);
                    UriBuilder builder = Urls.actionTokenBuilder(uriInfo, token.serialize(session, realm, ""),
                            Constants.SSO_MANAGEMENT_CLIENT_ID, "");
                    String link = builder.build(realm.getName()).toString();
                    String workspaceUrl = ModelToRepresentation.getWorkspaceUrl(session, realm);
                    //String url = Urls.ssoPage(session.getContext().getUri().getBaseUri(), realm.getName()).toString();
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("tenant", getRealmName(realm));
                    attributes.put("user_name", user.getUsername());
                    attributes.put("year", String.valueOf(year));
                    attributes.put("link", link);
                    attributes.put("link_expiry", formatDuration(validityInSecs));
                    attributes.put("workspace", workspaceUrl);
                    attributes.put("variant", EnvironmentReader.getValue("VARIANT", "ezto verify"));
                    try {
                        session.getProvider(EmailTemplateProvider.class)
                                .setRealm(realm)
                                .send(user.getEmail(), "welcomeUserSubject", "welcome-user-email-link.ftl", attributes);

                        //after sending email, we can set invite attributes
                        user.setSingleAttribute(UserModel.USER_INVITE_STATUS, "Invited");
                        user.setSingleAttribute(UserModel.USER_INVITE_CLIENT, Constants.SSO_MANAGEMENT_CLIENT_ID);
                    } catch (EmailException e) {
                        realm.removeAttribute("owsSyncStarted");
                        log.error(e);
                    } catch (ConfigException e) {
                        realm.removeAttribute("owsSyncStarted");
                        EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
                        eventBuilder.event(EventType.EMAIL_CONFIG_ERROR);
                        eventBuilder.error(e.getMessage());
                    }
                }
            }

        }
    }

    private static void addGroupMembership(ApiClient apiClient, RealmModel realm, UserModel userModel,String userId)  {
        UserApi userApi = new UserApi(apiClient);
        List<Group> groups = userApi.listUserGroups(userId);
        if (!groups.isEmpty()) {
            groups.forEach(g -> {
                GroupProfile groupProfile = g.getProfile();
                if (groupProfile != null) {
                    List<GroupModel> groupsFromQV = realm.searchForGroupByNameStream(g.getProfile().getName(), -1, -1).collect(Collectors.toList());
                    if (groupsFromQV.size() > 0) {
                        GroupModel model = groupsFromQV.get(0);
                        if (model != null && !userModel.isMemberOf(model)) {
                            userModel.joinGroup(model);
                        }
                    }
                } else {
                    log.error("OktaWSSyncRunner :: addGroupMembership :: group profile is null");
                    throw new RuntimeException("group profile is null");
                }
            });
        }
    }

    private static Map<String, Object> formatDuration(long valueInSeconds) {
        Map<String, Object> timeDuration = new HashMap<>();
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
        timeDuration.put("value", value);
        timeDuration.put("unit_key", unitKey);
        return timeDuration;
    }

    private static String getUnitTextFromMessages(String unitKey, long value) {
        if (value == 1) {
            unitKey = unitKey.substring(0, unitKey.length() - 1);
        }
        return unitKey;
    }

    private static String getRealmName(RealmModel realm) {
        return !TextUtils.isEmpty(realm.getDisplayName()) ? realm.getDisplayName() : realm.getName();
    }

    private static void deleteUsers(List<User> users, RealmModel realmModel, KeycloakSession session, String mapperType,SyncHistory syncHistory) {

        //Getting list of users from realm that already imported from Okta
        List<UserModel> listOfUser = session.users()
                .searchForUserByUserAttributeStream(realmModel, SYNCED_FROM, OKTA_WS, true)
                .collect(Collectors.toList());

        if (listOfUser.isEmpty()) {
            log.info("OktaSyncRunner :: deleteUsers :: User list that already imported from okta were empty");
            return;
        }

        log.info("OktaSyncRunner :: deleteUsers :: triggered");

        Map<String, String> oktaUsersMap = new HashMap<>();
        int deletedUserCount = 0;

        //Getting list of users from okta
        users.forEach(user -> {
                UserProfile userProfile = user.getProfile();
                if (userProfile != null) {
                    if (mapperType.equalsIgnoreCase("email")) {
                        oktaUsersMap.put(userProfile.getEmail(), "true");
                    } else if (mapperType.equalsIgnoreCase("username")) {
                        oktaUsersMap.put(userProfile.getLogin(), "true");
                    }
                } else {
                    log.error("OktaWSSyncRunner :: deleteUsers :: Group profile is null");
                    throw new RuntimeException("User profile is null");
                }
                });


        //Deleting user that present in QV and deleted in okta
        try{
            for (UserModel userModel : listOfUser) {
                if (mapperType.equalsIgnoreCase("email")) {
                    if (oktaUsersMap.get(userModel.getEmail()) == null) {
                        deletedUserCount = deleteIfUserNotAvailable(realmModel, session, userModel) ? deletedUserCount+1 : deletedUserCount;
                    }
                } else if (mapperType.equalsIgnoreCase("username")) {
                    if (oktaUsersMap.get(userModel.getUsername()) == null) {
                        deletedUserCount = deleteIfUserNotAvailable(realmModel, session, userModel) ? deletedUserCount+1 : deletedUserCount;
                    }
                }
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }

        log.info("OktaSyncRunner :: deleteUsers :: Deleted user count = "+deletedUserCount);
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

    private static void deleteGroups(List<Group> groups,KeycloakSession session,RealmModel realm){

        //Getting list of Groups from realm that already imported from Okta
        List<GroupModel> listOfGroups = session.groups().getGroupsStream(realm)
                .filter(groupModel -> {
                    String groupAttribute = groupModel.getFirstAttribute(SYNCED_FROM);
                    return groupAttribute != null && groupAttribute.equals(OKTA_WS);
                })
                .collect(Collectors.toList());

        if(listOfGroups.size() == 0){
            log.info("OktaSyncRunner :: deleteGroups :: Groups list that already imported from okta were empty");
           return;
        }

        log.info("OktaSyncRunner :: deleteGroups :: triggered");

        Map<String,String> oktaGroupsMap = new HashMap<>();
        int deletedGroupCount = 0;

        //Getting list of Groups from okta
        groups.forEach(group -> {
            GroupProfile groupProfile = group.getProfile();
            if (groupProfile != null) {
                oktaGroupsMap.put(groupProfile.getName(),"true");
            } else {
                log.error("OktaWSSyncRunner :: deleteGroups :: Group profile is null");
                throw new RuntimeException("Group profile is null");
            }
        });

        //Deleting Groups that that were empty in QV and deleted in okta
        try {
            for(GroupModel groupModel : listOfGroups){

                if (oktaGroupsMap.get(groupModel.getName()) == null && !session.users().getGroupMembersStream(realm, groupModel).findAny().isPresent()) {
                    deletedGroupCount = realm.removeGroup(groupModel) ? deletedGroupCount+1 : deletedGroupCount;
                }
            }

        }catch (Exception e){
            log.error(e.getMessage());
        }

        log.info("OktaSyncRunner :: deleteGroups :: Deleted Groups count = "+deletedGroupCount);

    }
}

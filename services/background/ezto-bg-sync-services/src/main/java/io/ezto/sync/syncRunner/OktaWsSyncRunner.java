package io.ezto.sync.syncRunner;

import com.google.gson.Gson;
import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.client.Clients;
import io.ezto.helpers.JsonHelper;
import io.ezto.sync.utility.UserSyncResult;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.GroupApi;
import org.openapitools.client.api.RoleAssignmentApi;
import org.openapitools.client.api.UserApi;
import org.openapitools.client.model.Group;
import org.openapitools.client.model.Role;
import org.openapitools.client.model.User;

import java.util.*;

public class OktaWsSyncRunner {
    public static UserSyncResult syncFromOktaWS(String domainName, String apiToken, String realmId, String representation, String providerId, String userId) {

        String oktaOrgUrl = "https://" + domainName + ".okta.com";

        try{
            ApiClient client = Clients.builder()
                    .setOrgUrl(oktaOrgUrl)
                    .setClientCredentials(new TokenClientCredentials(apiToken))
                    .build();

            GroupApi groupApi = new GroupApi(client);
            List<Group> groups = groupApi.listGroups("", "", "", null, "", "");


            UserApi userApi = new UserApi(client);
            List<User> users = userApi.listUsers(null, null, null, null, null, null, null);

            RoleAssignmentApi roleAssignmentApi = new RoleAssignmentApi(client);
            HashMap<String, List<String>> userRoles = new HashMap<>();
            users.forEach(user -> {
                List<Role> oktaRolesList = roleAssignmentApi.listAssignedRolesForUser(users.get(0).getId(),"");
                List<String> roles = new ArrayList<>();
                oktaRolesList.forEach(role -> roles.add(Objects.requireNonNull(role.getType()).getValue()));
                userRoles.put(user.getId(), roles);
            });


            Gson gson = new Gson();
            List<String> groupsList = new ArrayList<>();
            groups.stream().forEach(x -> groupsList.add(gson.toJson(x)));

            List<String> usersList = new ArrayList<>();
            users.stream().forEach(x -> usersList.add(gson.toJson(x)));

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("groups", groupsList);
            resultMap.put("users", usersList);
            resultMap.put("userRoles", userRoles);

            return new UserSyncResult(JsonHelper.getInstance().getObjectMapper().writeValueAsString(resultMap), realmId, representation, providerId, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}

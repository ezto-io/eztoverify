package io.ezto.sync.syncRunner;

import com.google.gson.Gson;
import io.ezto.helpers.JsonHelper;
import io.ezto.sync.utility.UserSyncResult;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.UserCollectionPage;

public class AzureSyncRunner {
    private static String USER_PROPS = "id,accountEnabled,userPrincipalName,mail,givenName,surname,onPremisesImmutableId";

    public static UserSyncResult syncFromAzureWS(String accessToken, String realmId, String representation, String providerId, String userId) {

        try {
            IAuthenticationProvider authProvider = new IAuthenticationProvider() {
                @Override
                public CompletableFuture<String> getAuthorizationTokenAsync(URL requestUrl) {
                    CompletableFuture<String> future = new CompletableFuture<>();
                    future.complete(accessToken);
                    return future;
                }
            };

            GraphServiceClient graphClient = GraphServiceClient
                    .builder()
                    .authenticationProvider(authProvider)
                    .buildClient();

            GroupCollectionPage groups = graphClient.groups().buildRequest().get();
            UserCollectionPage users = graphClient.users().buildRequest().select(USER_PROPS).get();
            Map<String, Object> result = new HashMap<>();
            Gson gson = new Gson();

            ArrayList<String> userListCurrentPage = new ArrayList<>();
            users.getCurrentPage().stream().forEach(user -> userListCurrentPage.add(gson.toJson(user)));

            ArrayList<String> groupListCurrentPage = new ArrayList<>();
            groups.getCurrentPage().stream().forEach(group -> groupListCurrentPage.add(gson.toJson(group)));

            boolean hasNextPage = false;
            do{
                if(users.getNextPage() != null){
                    users = users.getNextPage().buildRequest().get();
                    users.getCurrentPage().stream().forEach(user -> userListCurrentPage.add(gson.toJson(user)));
                    if(users.getNextPage() != null) {
                        users = users.getNextPage().buildRequest().get();
                        hasNextPage = true;
                    } else {
                        hasNextPage = false;
                    }
                } else {
                    hasNextPage = false;
                }
            } while (hasNextPage);

            do{
                if(groups.getNextPage() != null){
                    groups = groups.getNextPage().buildRequest().get();
                    groups.getCurrentPage().stream().forEach(group -> groupListCurrentPage.add(gson.toJson(group)));
                    if(groups.getNextPage() != null) {
                        groups = groups.getNextPage().buildRequest().get();
                        hasNextPage = true;
                    } else {
                        hasNextPage = false;
                    }
                } else {
                    hasNextPage = false;
                }
            } while (hasNextPage);

            UserCollectionPage azureUsersWithRoles = graphClient.users().buildRequest().select(USER_PROPS).expand("appRoleAssignments").get();
            HashMap<String, Object> userRoles = new HashMap<>();
            if(azureUsersWithRoles != null) {
                azureUsersWithRoles.getCurrentPage();
                azureUsersWithRoles.getCurrentPage().forEach(user -> {
                    List<String> roles = new ArrayList<>();
                    if (user.appRoleAssignments != null) {
                        user.appRoleAssignments.getCurrentPage().forEach(x -> roles.add(x.resourceDisplayName));
                    }
                    userRoles.put(user.id, roles);
                });
            }

            result.put("groupsCurrentPage", gson.toJson(groupListCurrentPage));
            result.put("usersCurrentPage", gson.toJson(userListCurrentPage));
            result.put("groupsNextPage", new ArrayList<String>());
            result.put("usersNextPage", new ArrayList<String>());
            result.put("usersRoles", userRoles);

            return new UserSyncResult(JsonHelper.getInstance().getObjectMapper().writeValueAsString(result), realmId, representation, providerId, userId);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}

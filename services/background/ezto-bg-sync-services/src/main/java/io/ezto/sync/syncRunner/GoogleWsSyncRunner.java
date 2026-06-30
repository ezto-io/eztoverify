package io.ezto.sync.syncRunner;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.Groups;
import com.google.api.services.admin.directory.model.Users;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import io.ezto.helpers.JsonHelper;
import io.ezto.sync.utility.UserSyncResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleWsSyncRunner {

    public static UserSyncResult syncFromGWS(String accessToken, String realmId, String representation, String providerId, String userId) {

        GoogleCredentials credentials =
                GoogleCredentials.newBuilder()
                        .setAccessToken(new AccessToken(accessToken, null))
                        .build();

        try {
            Directory directory =
                    new Directory.Builder(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            GsonFactory.getDefaultInstance(),
                            new HttpCredentialsAdapter(credentials))
                            .setApplicationName("sync_test")
                            .build();

            Groups groups = directory.groups().list().setCustomer("my_customer").execute();
            String groupsString = GsonFactory.getDefaultInstance().toString(groups);

            HashMap<String, List<String>> userRoles = new HashMap<>();
            Users users = directory.users().list().setCustomer("my_customer").execute();
            users.getUsers().forEach(user ->
                userRoles.put(user.getId(), (Boolean.TRUE.equals(user.getIsAdmin()) ? List.of("admin") : List.of("user")))
            );
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("groups", groupsString);
            resultMap.put("userRoles", userRoles);

            return new UserSyncResult(JsonHelper.getInstance().getObjectMapper().writeValueAsString(resultMap), realmId, representation, providerId, userId);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

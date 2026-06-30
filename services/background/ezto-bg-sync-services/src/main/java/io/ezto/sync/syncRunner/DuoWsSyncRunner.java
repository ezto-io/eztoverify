package io.ezto.sync.syncRunner;

import com.google.gson.Gson;
import io.ezto.helpers.JsonHelper;
import io.ezto.sync.utility.UserSyncResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.duosecurity.client.Admin;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DuoWsSyncRunner {
    public static UserSyncResult syncFromDuoWS(String apiHostName, String secretKey, String integrationKey, String realmId, String representation, String providerId, String userId) {


        try {
            boolean hasNextPageinGrps = true;
            String grpLimit = "10";
            int grpOffset = 0;

            JSONArray groups = null;
            JSONObject grpMetadata = null;

            JSONArray users = null;
            JSONArray adminUsers = null;
            JSONObject usrMetadata = null;
            JSONObject adminUsersMetadata = null;

            JSONArray group = null;
            JSONObject groupMetadata = null;

            //for groups
            while (hasNextPageinGrps) {
                Admin grpRequest = new
                        Admin.AdminBuilder("GET", apiHostName, "/admin/v1/groups").build();
                grpRequest.addParam("offset", grpOffset);
                grpRequest.addParam("limit", grpLimit);
                grpRequest.signRequest(integrationKey, secretKey);

                JSONObject resp = (JSONObject) grpRequest.executeJSONRequest();
                 groups = resp.getJSONArray("response");
                 grpMetadata = resp.getJSONObject("metadata");

                if (!grpMetadata.isNull("next_offset")) {
                    grpOffset = grpMetadata.getInt("next_offset");

                    grpRequest =  new
                            Admin.AdminBuilder("GET", apiHostName, "/admin/v1/groups").build();
                    grpRequest.addParam("offset",  Integer.toString(grpOffset));
                    grpRequest.addParam("limit", grpLimit);
                    grpRequest.signRequest(integrationKey, secretKey);
                } else {
                    hasNextPageinGrps = false;
                }
            }

            // for users
            boolean hasNextPageinUser = true;
            String userLimit = "10";
            int userOffset = 0;
            while (hasNextPageinUser) {
                Admin userRequest = new Admin.AdminBuilder("GET", apiHostName, "/admin/v1/users").build();

                userRequest.addParam("offset", userOffset);
                userRequest.addParam("limit", userLimit);
                userRequest.signRequest(integrationKey, secretKey);

                JSONObject resp = (JSONObject) userRequest.executeJSONRequest();
                users = resp.getJSONArray("response");
                usrMetadata = resp.getJSONObject("metadata");

                hasNextPageinUser = check(usrMetadata, apiHostName, "/admin/v1/users", userLimit, integrationKey, secretKey);
            }

            // for admin users
            hasNextPageinUser = true;
            while (hasNextPageinUser) {
                Admin adminUserRequest = new Admin.AdminBuilder("GET", apiHostName, "/admin/v1/admins").build();
                adminUserRequest.addParam("offset", userOffset);
                adminUserRequest.addParam("limit", userLimit);
                adminUserRequest.signRequest(integrationKey, secretKey);

                JSONObject adminRep = (JSONObject) adminUserRequest.executeJSONRequest();
                adminUsers = adminRep.getJSONArray("response");
                adminUsersMetadata = adminRep.getJSONObject("metadata");

                hasNextPageinUser = check(usrMetadata, apiHostName, "/admin/v1/admins", userLimit, integrationKey, secretKey);
            }

            Map<String, Object> resultMap = new HashMap<>();

            Gson gson = new Gson();
            ArrayList<String> groupsString = new ArrayList<>();
            for(int i =0; i<groups.length(); i++){
                groupsString.add(gson.toJson(groups.getJSONObject(i)));
            }
            ArrayList<String> usersString = new ArrayList<>();
            for(int i =0; i<users.length(); i++){
                usersString.add(gson.toJson(users.getJSONObject(i)));
            }
            for(int i =0; i<adminUsers.length(); i++){
                usersString.add(gson.toJson(adminUsers.getJSONObject(i)));
            }


            resultMap.put("groups", groupsString);
            resultMap.put("grpMetadata", gson.toJson(usrMetadata));
            resultMap.put("users", usersString);
            resultMap.put("userMetadata", gson.toJson(usrMetadata));
            resultMap.put("adminUsersMetadata", gson.toJson(adminUsersMetadata));

        return new UserSyncResult(JsonHelper.getInstance().getObjectMapper().writeValueAsString(resultMap), realmId, representation, providerId, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean check(JSONObject usrMetadata, String apiHostName, String uri,  String userLimit, String integrationKey, String secretKey){
        int userOffset = 0;

        try {
            if (!usrMetadata.isNull("next_offset")) {
                userOffset = usrMetadata.getInt("next_offset");

                Admin userRequest = new Admin.AdminBuilder("GET", apiHostName, uri).build();
                userRequest.addParam("offset", Integer.toString(userOffset));
                userRequest.addParam("limit", userLimit);
                userRequest.signRequest(integrationKey, secretKey);
                return true;
            } else {
                return false;
            }
        } catch (UnsupportedEncodingException | JSONException e) {
            throw new RuntimeException(e);
        }
    }
}

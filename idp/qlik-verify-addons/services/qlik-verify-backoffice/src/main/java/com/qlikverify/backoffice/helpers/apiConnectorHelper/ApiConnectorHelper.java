package com.qlikverify.backoffice.helpers.apiConnectorHelper;

import com.qlikverify.backoffice.apiconnector.models.AccessTokenModel;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.keycloak.EnvironmentReader;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

public class ApiConnectorHelper {
    public String getToken() {
        String formData = "client_id=" + EnvironmentReader.getValue("EZTO_AUTH_CLIENT_ID","")+
                "&grant_type=client_credentials" +
                "&client_secret=" + EnvironmentReader.getValue("EZTO_AUTH_CLIENT_SECRET","");
        RequestBody formBody = RequestBody.create(formData, MediaType.parse("application/x-www-form-urlencoded"));
        Request.Builder requestBuilder = new Request.Builder().url(EnvironmentReader.getValue("EZTO_AUTH_TOKEN_URL", "")).post(formBody);
        Request request = requestBuilder.build();
        okhttp3.Response response;
        OkHttpClient okHttpClient = new OkHttpClient();
        try{
            response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                assert response.body() != null;
                AccessTokenModel accessTokenModel = null;
                accessTokenModel = JsonSerialization.readValue(response.body().string(), AccessTokenModel.class);
                return accessTokenModel.getAccessToken();
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}

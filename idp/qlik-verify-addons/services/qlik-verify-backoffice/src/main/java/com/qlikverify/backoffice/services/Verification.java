package com.qlikverify.backoffice.services;

import com.qlikverify.backoffice.helpers.apiConnectorHelper.ApiConnectorHelper;
import okhttp3.*;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.qlikverify.backoffice.Constants.BO_TRAN;

public class Verification {

    private static final Logger logger = Logger.getLogger(Verification.class);
    private static final ApiConnectorHelper apiConnectorHelper = new ApiConnectorHelper();

    public static Map<String, Object> doVerification(RequiredActionContext context, Map<String,Object> userDetails, String verifierName, String providerName,
                                                     Map<String, Object> pathVariables, Map<String, String> queryParams, String stripeId ) throws IOException {

        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .build();

        MediaType mediaType = MediaType.parse("application/json");
        Response response;

        try {
            Map<String, Object> requestBody = getRequestBody(context, userDetails, providerName, stripeId, pathVariables, queryParams);

            RequestBody body = RequestBody.create(mediaType, JsonSerialization.writeValueAsPrettyString(requestBody));

            logger.info("===============================================================================================");
            logger.info(String.format("REQUEST TO API CONNECTOR FROM WORKFLOW :: request ID :: %s :: workspace name :: %s :: request url :: %s", context.getRequestId(), context.getRealm().getName(), EnvironmentReader.getValue("API_CONNECTOR_HOST_URL") + "/api/" + verifierName + "/verify"));
            logger.info("===============================================================================================");

            String token = apiConnectorHelper.getToken();

            Request request = new Request.Builder()
                    .url(EnvironmentReader.getValue("API_CONNECTOR_HOST_URL") + "/api/" + verifierName + "/verify")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .build();

            response = client.newCall(request).execute();

            Map<String, Object>  resBody= JsonSerialization.readValue(response.body().string(), Map.class);
            resBody.put("status_code", response.code());

            logger.info("===============================================================================================");
            logger.info(String.format("RESPONSE FROM API CONNECTOR FROM WORKFLOW :: request ID :: %s :: workspace name :: %s :: response code :: %s :: response body :: %s", context.getRequestId(), context.getRealm().getName(), response.code(), resBody));
            logger.info("===============================================================================================");
            return resBody;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<String, Object> getRequestBody(RequiredActionContext context, Map<String, Object> userDetails, String providerName,
                                                      String stripeId, Map<String, Object> pathVariables, Map<String, String> queryParams) {
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("userDetails", userDetails);
        reqBody.put("workspaceId", context.getRealm().getId());
        reqBody.put("requestId", context.getRequestId());
        reqBody.put("fromEzto", true);
        reqBody.put("providerName", providerName);
        reqBody.put("pricing", getPricingDetails(context, providerName, stripeId));

        if (pathVariables != null) {
            reqBody.put("pathVariables", pathVariables);
        }

        if (queryParams != null) {
            reqBody.put("queryParams", queryParams);
        }

        return reqBody;

    }

    private static Map<String, Object> getPricingDetails(RequiredActionContext context, String providerName, String stripeId) {
        Map<String, Object> pricingDetails = new HashMap<>();
        pricingDetails.put("currency", "USD");
        pricingDetails.put("amount", "0.00");
        return pricingDetails;
    }

}

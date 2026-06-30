package com.qlikverify.backoffice.services.livelinessVerificationService;

import okhttp3.*;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.util.JsonSerialization;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.keycloak.sms.utility.HttpUtility.getUnsafeOkHttpClient;

public class LivelinessVerificationServiceImpl implements LivelinessVerificationService{
    private static final Logger logger = Logger.getLogger(LivelinessVerificationServiceImpl.class);

    @Override
    public Map<String, Object> generateFaceTemplate(String imageData, String urlPath, String dataToBeSent){

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .build();


            MediaType mimeType = MediaType.parse("application/json" + "; charset=utf-8");
            RequestBody requestBody = RequestBody.create(mimeType, dataToBeSent);

            Request request = new Request.Builder()
                    .url(EnvironmentReader.getValue("LIVELINESS_URL") + "/live" + urlPath)
                    .method("POST",requestBody)
                    .addHeader("Content-Type","application/json")
                    .build();

            Response response = client.newCall(request).execute();


        logger.info("------------External Request-----------------");
        logger.info(dataToBeSent);
        logger.info("------------External Request-----------------");

            if(response.code() == 500)
                return null;
            if(!response.isSuccessful())
                return  null;
            String resBody = response.body().string();
            logger.info("------------External Response-----------------");
            logger.info(response.code());
            logger.info("------------External Response-----------------");
            return JsonSerialization.readValue(resBody, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }

    @Override
    public Map<String, Object> verifyLiveliness(String urlPath, String dataForRequestBody){

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .build();

            MediaType mimeType = MediaType.parse("application/json" + "; charset=utf-8");
            RequestBody requestBody = RequestBody.create(mimeType,dataForRequestBody);

            Request request = new Request.Builder()
                    .url(EnvironmentReader.getValue("LIVELINESS_URL") + "/live" + urlPath)
                    .method("POST",requestBody)
                    .addHeader("Content-Type","application/json")
                    .build();

            Response response = client.newCall(request).execute();

            logger.info("------------External Request-----------------");
            logger.info(dataForRequestBody);
            logger.info("------------External Request-----------------");

            if(response.code() == 500) {
                logger.error("LIVENESS FAILED:  " + response.code() + "::" + response.body() != null ? response.body().string() : "LIVENESS FAILED DUE TO SOME ERRORS");
                return null;
            }
            if(!response.isSuccessful()) {
                logger.error("LIVENESS FAILED:  " + response.code() + "::" + response.body() != null ? response.body().string() : "LIVENESS FAILED DUE TO SOME ERRORS");
                return null;
            }
            String resBody = response.body().string();
            logger.info("------------External Response-----------------");
            logger.info(response.code());
            logger.info("------------External Response-----------------");
            return JsonSerialization.readValue(resBody, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }
}

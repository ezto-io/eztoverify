package org.keycloak.sms.utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpUtility {

    Logger LOG = LoggerFactory.getLogger(HttpUtility.class);

    public Response callRequest(String mediaType, String method, String url, String body, String auth) {
        return callRequest(mediaType, method, url, body, auth, new HashMap<>(), 3);
    }

    public Response callRequest(String mediaType, String method, String url, String body, String auth, Map<String,
            String> headers, int retry) {
        return callRequest(mediaType, method, url, body, auth, headers, retry, 1000);
    }

    public Response callRequest(String mediaType, String method, String url, String body, String auth, Map<String,
            String> headers, int retry, boolean closeResponse) {
        return callRequest(mediaType, method, url, body, auth, headers, retry, 1000, closeResponse);
    }

    public Response callRequest(File file, String url, HashMap<String, Object> userDetails) throws IOException {
        OkHttpClient client = new OkHttpClient();
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        userDetails.forEach((key, value)-> builder.addFormDataPart(key, String.valueOf(userDetails.get(key))));
        builder.addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse(Files.probeContentType(file.toPath()))));
        RequestBody requestBody = builder.build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        LOG.info("------------External Request-----------------");
        LOG.info("Request to " + request);
        LOG.info("------------External Request-----------------");

        Response response = null;
        try {
            response = client.newCall(request).execute();
            LOG.info("------------External Response-----------------");
            LOG.info(response != null ? String.valueOf(response.code()) : "UNKWOWN");
            LOG.info("------------External Response-----------------");
        } catch (IOException e) {
            e.printStackTrace();
            return response;
        }
        return response;
    }

    public static OkHttpClient getUnsafeOkHttpClient() {
        // Default 120s timeouts — shared by ~40 external integrations (SMS, webhooks, elastic,
        // video-kyc, IdP brokers). Do NOT lower globally; use the bounded overload for in-JVM /
        // self-loopback calls instead (see getUnsafeOkHttpClient(long, long)).
        return getUnsafeOkHttpClient(120, 120);
    }

    /**
     * Bounded-timeout variant for internal / self-loopback HTTP calls. Keeps timeouts well under
     * the JTA transaction timeout so a slow self-call cannot let the reaper close the request
     * EntityManager mid-flow. No-retry usage is the caller's responsibility (pass retry=0).
     */
    public static OkHttpClient getUnsafeOkHttpClient(long connectTimeoutSec, long readTimeoutSec) {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }};

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient.Builder().sslSocketFactory(sslSocketFactory,
                    (X509TrustManager) trustAllCerts[0]).connectTimeout(connectTimeoutSec, TimeUnit.SECONDS).readTimeout(readTimeoutSec,
                    TimeUnit.SECONDS).hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            }).build();

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Response callRequest(String mediaType, String method, String url, String body, String auth, Map<String, String> headers, int retry, long retryIn) {
        RetryStartegyHelper retryStartegyHelper = new RetryStartegyHelper(retry, retryIn);
        while (retryStartegyHelper.shouldRetry()) {
            OkHttpClient client = getUnsafeOkHttpClient();
            RequestBody requestBody = null;
            if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE") || method.equals("PATCH")) {
                if (mediaType.equals("application/x-www-form-urlencoded")) {
                    requestBody = createFormBody(body);
                } else {
                    MediaType mimeType = MediaType.parse(mediaType + "; charset=utf-8");
                    requestBody = RequestBody.create(mimeType, body);
                }
            }
            Request.Builder requestBuilder = method.equals("GET") ? new Request.Builder().url(url).get() : new Request.Builder().url(url).method(method, requestBody);
            Request request = null;
            if (auth != null && !auth.isEmpty()) {
                requestBuilder.addHeader("Authorization", auth);
            }
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(requestBuilder::addHeader);
            }
            request = requestBuilder.build();
            LOG.info("------------External Request-----------------");
            LOG.info(request.toString());
            LOG.info("------------External Request-----------------");
            try {
                Response resultResponse = client.newCall(request).execute();
                LOG.info("------------External Response-----------------");
                LOG.info(resultResponse != null ? String.valueOf(resultResponse.code()) : "UNKNOWN");
                LOG.info("------------External Response-----------------");
                if (!resultResponse.isSuccessful()) {
                    resultResponse.close();
                    try {
                        retryStartegyHelper.errorOccured();
                    } catch (Exception err) {
                        err.printStackTrace();
                        return resultResponse;
                    }
                } else {
                    retryStartegyHelper.stopRetry();
                    return resultResponse;
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    retryStartegyHelper.errorOccured();
                } catch (Exception err) {
                    err.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    public Response callRequest(String mediaType, String method, String url, String body, String auth, Map<String, String> headers, int retry, long retryIn, boolean closeResponse) {
        RetryStartegyHelper retryStartegyHelper = new RetryStartegyHelper(retry, retryIn);
        while (retryStartegyHelper.shouldRetry()) {
            OkHttpClient client = getUnsafeOkHttpClient();
            RequestBody requestBody = null;
            if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE") || method.equals("PATCH")) {
                if (mediaType.equals("application/x-www-form-urlencoded")) {
                    requestBody = createFormBody(body);
                } else {
                    MediaType mimeType = MediaType.parse(mediaType + "; charset=utf-8");
                    requestBody = RequestBody.create(mimeType, body);
                }
            }
            Request.Builder requestBuilder = method.equals("GET") ? new Request.Builder().url(url).get() : new Request.Builder().url(url).method(method, requestBody);
            Request request = null;
            if (auth != null && !auth.isEmpty()) {
                requestBuilder.addHeader("Authorization", auth);
            }
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(requestBuilder::addHeader);
            }
            request = requestBuilder.build();
            LOG.info("------------External Request-----------------");
            LOG.info(request.toString());
            LOG.info("------------External Request-----------------");
            try {
                Response resultResponse = client.newCall(request).execute();
                LOG.info("------------External Response-----------------");
                LOG.info(resultResponse != null ? String.valueOf(resultResponse.code()) : "UNKNOWN");
                LOG.info("------------External Response-----------------");
                if (!resultResponse.isSuccessful()) {
                    if (closeResponse) {
                        resultResponse.close();
                    }
                    try {
                        retryStartegyHelper.errorOccured();
                    } catch (Exception err) {
                        err.printStackTrace();
                        return resultResponse;
                    }
                } else {
                    retryStartegyHelper.stopRetry();
                    return resultResponse;
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    retryStartegyHelper.errorOccured();
                } catch (Exception err) {
                    err.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    private RequestBody createFormBody(String body) {
        FormBody.Builder formBuilder = new FormBody.Builder();

        if (isJsonArray(body)) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, String>> bodyList = mapper.readValue(body, new TypeReference<List<Map<String, String>>>() {
                });

                for (Map<String, String> item : bodyList) {
                    String key = item.get("key");
                    String value = item.get("value");
                    if (key != null && value != null) {
                        formBuilder.add(key, value);
                    }
                }
            } catch (IOException e) {
                LOG.error("Failed to parse JSON array body", e);
            }
        } else {
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    formBuilder.add(keyValue[0], keyValue[1]);
                }
            }
        }

        return formBuilder.build();
    }

    private boolean isJsonArray(String str) {
        str = str.trim();
        return str.startsWith("[") && str.endsWith("]");
    }

}

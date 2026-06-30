package io.ezto.helpers.utility;

import io.ezto.helpers.RetryStartegyHelper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class HttpUtility {

    private static final Logger LOG = LoggerFactory.getLogger(HttpUtility.class);
    public Response callRequest(String mediaType, String method, String url, String body, String auth) {
        return callRequest(mediaType, method, url, body, auth, new HashMap<>(), 3, 10000);
    }

    /**
     * callRequest
     * @param mediaType
     * @param method
     * @param url
     * @param body
     * @param auth
     * @param headers
     * @param retry
     * @param retryIn in milliseconds
     * @return
     */
    public Response callRequest(String mediaType, String method, String url, String body, String auth, Map<String,
            String> headers, int retry, long retryIn){
        RetryStartegyHelper retryStartegyHelper = new RetryStartegyHelper(retry, retryIn);
        while (retryStartegyHelper.shouldRetry()) {
            OkHttpClient client = getUnsafeOkHttpClient();
            MediaType MIMEType = MediaType.parse(mediaType + "; charset=utf-8");

            RequestBody requestBody = createRequestBody(method, MIMEType, body);

            Request.Builder requestBuilder = addHeaderToRequestBuilder(auth, headers, method, url, requestBody);
            Request request = null;
            request = requestBuilder.build();
            LOG.info("------------External Request-----------------");
            LOG.info(request.toString());
            LOG.info("------------External Request-----------------");
            Response resultResponse = null;
            try {
                resultResponse = client.newCall(request).execute();
                LOG.info("------------External Response-----------------");
                LOG.info(resultResponse != null ? String.valueOf(resultResponse.code()) : "UNKWOWN" );
                LOG.info("------------External Response-----------------");
                if(!validateResponse(resultResponse, retryStartegyHelper)){
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
            } finally {
                if(resultResponse != null){
                    resultResponse.close();
                }
            }
        }
        return null;
    }

    public Request.Builder addHeaderToRequestBuilder(String auth, Map<String, String> headers, String method, String url, RequestBody requestBody){
        Request.Builder requestBuilder = method.equals("GET") ? new Request.Builder().url(url).get() : new Request.Builder().url(url).method(method, requestBody);

        if (auth != null && auth.length() > 0) {
            requestBuilder.addHeader("Authorization", auth);
        }
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((k, v) ->
                requestBuilder.addHeader(k, v)
            );
        }
        return requestBuilder;
    }

    public RequestBody createRequestBody(String method, MediaType MIMEType, String body){
        RequestBody requestBody = null;
        if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
            requestBody = RequestBody.create(MIMEType, body);
        }
        return requestBody;
    }

    public boolean validateResponse(Response resultResponse, RetryStartegyHelper retryStartegyHelper){
        if (!resultResponse.isSuccessful()) {
            resultResponse.close();
            try {
                retryStartegyHelper.errorOccured();
            } catch (Exception err) {
                err.printStackTrace();
                return false;
            }
        } else {
            retryStartegyHelper.stopRetry();
            return false;
        }
        return true;
    }

    public static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    // NOP
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    // NOP
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

            return new OkHttpClient.Builder().sslSocketFactory(sslSocketFactory,
                    (X509TrustManager) trustAllCerts[0]).connectTimeout(120, TimeUnit.SECONDS).readTimeout(120,
                    TimeUnit.SECONDS).hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            }).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

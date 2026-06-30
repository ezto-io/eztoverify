package com.qlikverify.extensions.admin.controllers.webPush;

import com.fasterxml.jackson.databind.JsonNode;
import com.qlikverify.extensions.admin.misc.base.BaseAuthCheckResource;
import com.qlikverify.extensions.admin.misc.helpers.LogHelper;
import liquibase.pro.packaged.E;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.EnvironmentReader;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper;
import org.keycloak.services.resources.Cors;
import org.keycloak.sms.utility.HttpUtility;
import org.keycloak.urls.UrlType;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;

import javax.json.Json;
import javax.net.ssl.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebPushAuthCheckResource extends BaseAuthCheckResource {
    private static final Logger logger = Logger.getLogger(WebPushAuthCheckResource.class);

    public WebPushAuthCheckResource(KeycloakSession session) {
        super(session);
    }

    @Path("{client}/{realm}/register")
    @OPTIONS
    public Response accountPreflight(@Context HttpRequest req, @Context HttpResponse res) {
        return Cors.add(req, Response.ok()).auth().preflight().build();
    }

    @POST
    @Path("{client}/{realm}/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerWebPush(@Context HttpRequest req, @Context HttpResponse res, final @PathParam("client") String clientId, final @PathParam("realm") String realm) {
        boolean isV1 = false;
        HttpHeaders httpHeaders = req.getHttpHeaders();
        MultivaluedMap<String, String> requestHeaders = httpHeaders.getRequestHeaders();
        if (!requestHeaders.isEmpty() && requestHeaders.containsKey("api-version")) {
            isV1 = Integer.parseInt(requestHeaders.getFirst("api-version")) == 1;
        }

        LogHelper.getInstance().info("registerWebPush");
        if(!session.getContext().getConnection().isBrowserRequest())
        {
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        Cors.add(req).allowedOrigins(Cors.ACCESS_CONTROL_ALLOW_ORIGIN_WILDCARD).auth().build(res);
        List<String> orgins = req.getHttpHeaders().getRequestHeader("origin");
        if(orgins.isEmpty())
        {
            GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR, "Request origin is should not be null");
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        String requestOrigin = req.getHttpHeaders().getRequestHeader("origin").get(0);
        if (requestOrigin == null) {
            GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR, "Request origin is should not be null");
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        RealmModel realmModel = session.realms().getRealmByName(realm);
        if (realmModel == null) {
            GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR, "Workspace not found");
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        session.getContext().setRealm(realmModel);

        ClientModel clientModel = realmModel.getClientByClientId(clientId);
        if (clientModel == null) {
            GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR,
                    String.format("Application with ID '%s' not found", clientId));
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        session.getContext().setClient(clientModel);

        String cibaAuthorizer = clientModel.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        CibaAuthProvider cibaAuthProvider = session.getProvider(CibaAuthProvider.class, cibaAuthorizer);

        if (cibaAuthProvider == null) {
            GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR,
                    "CibaAuthorizer not found");
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }
        CibaAuthConfigModel cibaAuthConfigModel = realmModel.getCibaAuthConfigByProviderId(cibaAuthorizer, clientModel.getClientId(), realmModel.getId());
        if (cibaAuthConfigModel == null) {
            GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR,
                    String.format("CibaAuthConfigModel for application '%s' not found", clientId));
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        if (!cibaAuthorizer.equalsIgnoreCase("azure-b2c-ciba-authorizer")) {
            String notifyType = cibaAuthConfigModel.getConfig().get("ciba-notify");
            if (notifyType == null) {
                GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR,
                        String.format("Notification type for application '%s' not set", clientId));
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }

            if (!notifyType.equals("SDK")) {
                GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR, "Notification way should be set as 'SDK'");
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }
        }

        List<String> originUrl = Optional.ofNullable(cibaAuthConfigModel.getConfig().get("whitelist_host"))
                .map(urls -> Arrays.stream(urls.split("##")))
                .orElseGet(Stream::empty)
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .collect(Collectors.toList());

        if (originUrl != null && originUrl.stream().noneMatch(url -> url.equals(requestOrigin))) {
            GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR,
                    String.format("Request origin '%s' is not found in the whitelisted urls", requestOrigin));
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        Boolean isCheckHttpsCert = Boolean.valueOf(cibaAuthConfigModel.getConfig().get("whitelist_host_restrict_https"));

        if (isCheckHttpsCert) {
            String recCert = getServerCertificate(requestOrigin);

            if(recCert == null) {
                GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR,
                        "Unable to retrieve the server certificate");
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }

            String webPubKey = cibaAuthConfigModel.getConfig().get("whitelist_host_pub_key");
            webPubKey = webPubKey.trim().replaceAll("\\s+", "");
            if (!recCert.equals(webPubKey)) {
                GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR,
                        "Host public key doesn't match");
                return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
            }
        }

        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        String authUrl = CibaGrantType.authorizationUrl(backendUriInfo.getBaseUriBuilder()).build(realmModel.getName()).toString();
        JsonNode jsonNode = null;
        try {
            jsonNode = JsonSerialization.mapper.readTree(req.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (jsonNode == null) {
            GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR,
                    "Error occurred while parsing request body");
            return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
        }

        Map<String, Object> data = convertJsonNodeToMap(jsonNode);
        OkHttpClient client = HttpUtility.getUnsafeOkHttpClient();
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");

        String content = "";
        if (!isV1) {
            content = "app_id=" + clientModel.getClientId() + "&app_secret=" + clientModel.getSecret();
        }
        Object rec = null;
        final String REQUEST = "request";
        if (data.get("metadata") != null) {
            Map nestedMap = JsonSerialization.convertValue(data.get("metadata"), Map.class);
            Object nestedRequest = nestedMap.get(REQUEST);

            if (nestedRequest instanceof String) {
                rec = nestedRequest;
            } else {
                logger.error("Failed to parse request body. Use serialized request body");
            }
        } else {
            rec = data.get(REQUEST);
        }
        if (rec != null) {
            content += "&request=" + rec;
        }
        RequestBody body = RequestBody.create(mediaType, content.getBytes());

        Request.Builder reqBuilder = new Request.Builder()
                .url(authUrl)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Web-SDK", "true")
                .addHeader("User-Agent", requestHeaders.getFirst("User-Agent"))
                .method("POST", body);

        if (isV1) {
            reqBuilder
                    .addHeader("Authorization", BasicAuthHelper.createHeader(clientModel.getClientId(),clientModel.getSecret()))
                    .addHeader("API-version", "1");
        }

        Request request = reqBuilder.build();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Map<String, String> resObj = JsonSerialization.readValue(response.body().string(), Map.class);
                Map<String, String> callBackResponse = new HashMap<>();
                callBackResponse.put("trxId", resObj.get("request_id"));
                callBackResponse.put("url", resObj.get("transaction_link"));
                callBackResponse.put("openInNewTab", resObj.get("open_in_new_tab"));
                callBackResponse.put("pollUrl", EnvironmentReader.getValue("SOCKET_URL"));
                logger.info("SOCKET_URL" + EnvironmentReader.getValue("SOCKET_URL"));
                return Response.ok(callBackResponse).build();
            }
        } catch (IOException e) {
            logger.error("Web push failed:" + e.getMessage());
            GeneralHelper.auditEvent(session, EventType.EZ_WEB_SDK_ERROR,
                    "Web push failed:" + e.getMessage());
        }

        return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();
    }

    private String getServerCertificate(String url) {
        try {
            // Replace "example.com" and 443 with the actual domain and port
            String hostname = url.replace("https://", "");
            int port = 443;

            // Create an SSLContext with default settings
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // Initialize SSLContext with default TrustManager
            sslContext.init(null, getDefaultTrustManager(), null);

            // Create an SSLSocketFactory
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Create an SSLSocket with the SSLSocketFactory
            try (SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(hostname, port)) {
                // Connect to the server
                sslSocket.startHandshake();

                // Retrieve the server certificates
                Certificate[] certificates = sslSocket.getSession().getPeerCertificates();
                X509Certificate endEntityCert = (X509Certificate) certificates[0];
                return Base64.getEncoder().encodeToString(endEntityCert.getEncoded());
            }

        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static TrustManager[] getDefaultTrustManager() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        // Create a TrustManagerFactory with the default trust manager algorithm
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        // Initialize the TrustManagerFactory with the default trust store
        tmf.init((KeyStore) null);

        // Obtain the default X509TrustManager from the TrustManagerFactory
        TrustManager trustManager = tmf.getTrustManagers()[0];

        return new TrustManager[]{trustManager};
    }

    private static Map<String, Object> convertJsonNodeToMap(JsonNode jsonNode) {
        Map<String, Object> resultMap = new HashMap<>();

        // Iterate through the fields in the JsonNode and add them to the HashMap
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            resultMap.put(entry.getKey(), convertJsonNodeToObject(entry.getValue()));
        }

        return resultMap;
    }

    private static Object convertJsonNodeToObject(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            return convertJsonNodeToMap(jsonNode);
        } else {
            return jsonNode.asText(); // Assuming simple values like strings
        }
    }

}

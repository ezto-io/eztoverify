/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quarkus.runtime.integration.web;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.models.Body;
import org.keycloak.common.models.IPRequest;
import org.keycloak.common.models.IPResponse;
import org.keycloak.common.util.Resteasy;
import org.keycloak.geo.GeoReader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.services.filters.AbstractRequestFilter;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Objects;

/**
 * <p>This filter is responsible for managing the request lifecycle as well as setting up the necessary context to process incoming
 * requests.
 *
 * <p>The filter itself runs in a event loop and should delegate to worker threads any blocking code (for now, all requests are handled
 * as blocking).
 */
public class QuarkusRequestFilter extends AbstractRequestFilter implements Handler<RoutingContext> {

    private static final Handler<AsyncResult<Object>> EMPTY_RESULT = result -> {
        // we don't really care about the result because any exception thrown should be handled by the parent class
    };

    @Override
    public void handle(RoutingContext context) {
        // our code should always be run as blocking until we don't provide a better support for running non-blocking code
        // in the event loop
        context.vertx().executeBlocking(createBlockingHandler(context), false, EMPTY_RESULT);
    }

    private Handler<Promise<Object>> createBlockingHandler(RoutingContext context) {
        return promise -> {
            KeycloakSessionFactory sessionFactory = getSessionFactory();
            KeycloakSession session = sessionFactory.create();

            configureContextualData(context, createClientConnection(context.request()), session);
            configureEndHandler(context, session);
            KeycloakTransactionManager tx = session.getTransactionManager();

            try {
                tx.begin();
                context.next();
                promise.tryComplete();
            } catch (Throwable cause) {
                promise.fail(cause);
                // re-throw so that the any exception is handled from parent
                throw new RuntimeException(cause);
            } finally {
                if (!context.response().headWritten()) {
                    // make sure the session is closed in case the handler is not called
                    // it might happen that, for whatever reason, downstream handlers do not end the response or
                    // no data was written to the response
                    close(session);
                }
            }
        };
    }

    /**
     * Creates a handler to close the {@link KeycloakSession} before the response is written to response but after Resteasy
     * is done with processing its output.
     */
    private void configureEndHandler(RoutingContext context, KeycloakSession session) {
        context.addHeadersEndHandler(event -> {
            try {
                close(session);
            } catch (Throwable cause) {
                if (cause instanceof ModelDuplicateException) {
                    constraintViolationErrorResponse(context.response());
                } else {
                    unexpectedErrorResponse(context.response());
                }
            }
        });
    }

    private void constraintViolationErrorResponse(HttpServerResponse response) {
        response.headers().clear();
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
        response.putHeader(HttpHeaderNames.CONTENT_LENGTH, "0");
        response.setStatusCode(HttpResponseStatus.CONFLICT.code());
        response.putHeader(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        // writes an empty buffer to replace any data previously written
        response.write(Buffer.buffer(""));
    }

    private void unexpectedErrorResponse(HttpServerResponse response) {
        response.headers().clear();
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
        response.putHeader(HttpHeaderNames.CONTENT_LENGTH, "0");
        response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        response.putHeader(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        // writes an empty buffer to replace any data previously written
        response.write(Buffer.buffer(""));
    }

    private void configureContextualData(RoutingContext context, ClientConnection connection, KeycloakSession session) {
        Resteasy.pushContext(ClientConnection.class, connection);
        Resteasy.pushContext(KeycloakSession.class, session);
        // quarkus-resteasy changed and clears the context map before dispatching
        // need to push keycloak contextual objects into the routing context for retrieving it later
        context.put(KeycloakSession.class.getName(), session);
        context.put(ClientConnection.class.getName(), connection);
    }

    protected void close(KeycloakSession session) {
        KeycloakTransactionManager tx = session.getTransactionManager();

        try {
            if (tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }
        } finally {
            session.close();
        }
    }

    private ClientConnection createClientConnection(HttpServerRequest request) {
        return new ClientConnection() {
            @Override
            public String getRemoteAddr() {
                String ipAddress = getRequestIP(request);
                return ipAddress;
            }

            @Override
            public String getRemoteHost() {
                return request.remoteAddress().host();
            }

            @Override
            public int getRemotePort() {
                return request.remoteAddress().port();
            }

            @Override
            public String getLocalAddr() {
                return request.localAddress().host();
            }

            @Override
            public int getLocalPort() {
                return request.localAddress().port();
            }

            @Override
            public String getClientOs() {
                return getClientOS(request);
            }

            @Override
            public String getClientBrowser() {
                return getClientBrowserName(request);
            }

            @Override
            public boolean isBrowserRequest() {
                if (checkIsApiReqHeader(request)) {
                    return false;
                }
                String browser = getClientBrowser();
                return !browser.startsWith("UnKnown");
            }

            @Override
            public IPResponse getClientLocation() {
                String ipAddress = getRequestIP(request);
                System.out.println("Incoming ip" + ipAddress);
                // For testing purpose
                if (Objects.equals(ipAddress, "127.0.0.1")) {
                    return new IPResponse() {{
                        setStatus(200);
                        setBody(new Body() {{
                                    setCity("local");
                                    setContinent("local");
                                    setCountry("local");
                                    setIpAddress("127.0.0.1");
                                    setLatitude(0.0);
                                    setLongitude(0.0);
                                    setZoneId(String.valueOf(ZoneId.systemDefault()));
                                }}
                        );
                    }};
                }
                return getClientLocation(ipAddress);
            }

            @Override
            public IPResponse getClientLocation(String ipAddress) {
                IPResponse ipResponse = null;
                if (ipAddress == null) {
                    ipResponse = new IPResponse();
                    ipResponse.setStatus(404);
                    return ipResponse;
                }

                IPRequest ipRequest = new IPRequest();
                ipRequest.setIp(ipAddress);
                try {
                    ipResponse = ipAddressFinder(ipRequest);
                } catch (IOException | GeoIp2Exception | NullPointerException e) {
                    ipResponse = new IPResponse();
                    ipResponse.setStatus(404);
                    ipResponse.setBody(new Body() {{
                        setZoneId(String.valueOf(ZoneId.systemDefault()));
                    }});
                }
                return ipResponse;
            }
        };
    }

    private IPResponse ipAddressFinder(IPRequest ipRequest) throws IOException, GeoIp2Exception, NullPointerException {
        String ip = ipRequest.getIp();
        Body response = GeoReader.getInstance().getCityResponse(ip);
        IPResponse ipResponse = new IPResponse();
        ipResponse.setStatus(200);
        ipResponse.setBody(response);
        return ipResponse;
    }

    private String getRequestIP(HttpServerRequest request) {
        return request.getHeader("X-Real-IP") != null && !request.getHeader("X-Real-IP").isEmpty() ? request.getHeader("X-Real-IP") : request.remoteAddress().host();
    }

    private String getClientOS(HttpServerRequest request) {
        final String browserDetails = request.getHeader("User-Agent");

        if (browserDetails == null)
            return "Unknown";

        //=================OS=======================
        final String lowerCaseBrowser = browserDetails.toLowerCase();
        if (lowerCaseBrowser.contains("windows")) {
            return "Windows";
        } else if (lowerCaseBrowser.contains("ipad")) {
            return "iPad";
        } else if (lowerCaseBrowser.contains("iphone")) {
            return "iPhone";
        } else if (lowerCaseBrowser.contains("mac")) {
            return "Mac";
        } else if (lowerCaseBrowser.contains("x11")) {
            return "Unix";
        } else if (lowerCaseBrowser.contains("android")) {
            return "Android";
        } else {
            return "UnKnown, More-Info: " + browserDetails;
        }
    }

    private boolean checkIsApiReqHeader(HttpServerRequest request) {
        final String isApiRequest = request.getHeader("isApiRequest");
        return isApiRequest != null && Boolean.parseBoolean(isApiRequest);
    }

    private String getClientBrowserName(HttpServerRequest request) {
        final String browserDetails = request.getHeader("User-Agent");
        if (browserDetails == null)
            return "UnKnown";

        final String userAgent = browserDetails.toLowerCase();

        String browser = "";

        //===============Browser===========================
        if (userAgent.contains("msie")) {
            String substring = browserDetails.substring(browserDetails.indexOf("MSIE")).split(";")[0];
            browser = substring.split(" ")[0].replace("MSIE", "IE") + "-" + substring.split(" ")[1];
        } else if (userAgent.contains("twilio")) {
            browser = "twilio";
        } else if (userAgent.contains("safari") && userAgent.contains("version")) {
            browser = (browserDetails.substring(browserDetails.indexOf("Safari")).split(" ")[0]).split(
                    "/")[0] + "-" + (browserDetails.substring(
                    browserDetails.indexOf("Version")).split(" ")[0]).split("/")[1];
        } else if (userAgent.contains("opr") || userAgent.contains("opera")) {
            if (userAgent.contains("opera"))
                browser = (browserDetails.substring(browserDetails.indexOf("Opera")).split(" ")[0]).split(
                        "/")[0] + "-" + (browserDetails.substring(
                        browserDetails.indexOf("Version")).split(" ")[0]).split("/")[1];
            else if (userAgent.contains("opr"))
                browser = ((browserDetails.substring(browserDetails.indexOf("OPR")).split(" ")[0]).replace("/",
                        "-")).replace(
                        "OPR", "Opera");
        } else if (userAgent.contains("edg")) {
            browser = "Microsoft Edge";
        } else if (userAgent.contains("chrome")) {
            browser = (browserDetails.substring(browserDetails.indexOf("Chrome")).split(" ")[0]).replace("/", "-");
        } else if ((userAgent.indexOf("mozilla/7.0") > -1) || (userAgent.indexOf("netscape6") != -1) || (userAgent.indexOf(
                "mozilla/4.7") != -1) || (userAgent.indexOf("mozilla/4.78") != -1) || (userAgent.indexOf(
                "mozilla/4.08") != -1) || (userAgent.indexOf("mozilla/3") != -1)) {
            //browser=(userAgent.substring(userAgent.indexOf("MSIE")).split(" ")[0]).replace("/", "-");
            browser = "Netscape-?";
        } else if (userAgent.contains("firefox")) {
            browser = (browserDetails.substring(browserDetails.indexOf("Firefox")).split(" ")[0]).replace("/", "-");
        } else if (userAgent.contains("rv")) {
            browser = "IE";
        } else if (userAgent.contains("applewebkit")) {
            browser = "Safari-AppleWebKit";
        } else {
            browser = "UnKnown, More-Info: " + browserDetails;
        }

        return browser;
    }
}

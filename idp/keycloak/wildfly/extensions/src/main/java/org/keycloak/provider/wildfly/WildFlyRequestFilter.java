/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.provider.wildfly;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import org.keycloak.common.ClientConnection;
import org.keycloak.geo.GeoReader;
import org.keycloak.common.models.Body;
import org.keycloak.common.models.IPRequest;
import org.keycloak.common.models.IPResponse;
import org.keycloak.services.filters.AbstractRequestFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class WildFlyRequestFilter extends AbstractRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws UnsupportedEncodingException {
        servletRequest.setCharacterEncoding("UTF-8");
        ClientConnection clientConnection = createConnection((HttpServletRequest) servletRequest);

        filter(clientConnection, (session) -> {
            try {
                filterChain.doFilter(servletRequest, servletResponse);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private ClientConnection createConnection(HttpServletRequest request) {
        return new ClientConnection() {
            @Override
            public String getRemoteAddr() {
                return request.getRemoteAddr();
            }

            @Override
            public String getRemoteHost() {
                return request.getRemoteHost();
            }

            @Override
            public int getRemotePort() {
                return request.getRemotePort();
            }

            @Override
            public String getLocalAddr() {
                return request.getLocalAddr();
            }

            @Override
            public int getLocalPort() {
                return request.getLocalPort();
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
                String browser = getClientBrowser();
                return !browser.startsWith("UnKnown");
            }

            @Override
            public IPResponse getClientLocation() {
                String ipAddress = getRequestIP(request);
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
                } catch (IOException | GeoIp2Exception e) {
                    e.printStackTrace();
                    ipResponse = new IPResponse();
                    ipResponse.setStatus(404);
                }
                return ipResponse;
            }
        };


    }

    private IPResponse ipAddressFinder(IPRequest ipRequest) throws IOException, GeoIp2Exception {
        String ip = ipRequest.getIp();
        Body response = GeoReader.getInstance().getCityResponse(ip);
        IPResponse ipResponse = new IPResponse();
        ipResponse.setStatus(200);
        ipResponse.setBody(response);
        return ipResponse;
    }

    private String getRequestIP(HttpServletRequest request) {
        return request.getHeader("X-Real-IP") != null && !request.getHeader("X-Real-IP").isEmpty() ? request.getHeader("X-Real-IP") : request.getRemoteAddr();
    }

    private String getClientOS(HttpServletRequest request) {
        final String browserDetails = request.getHeader("User-Agent");

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

    private String getClientBrowserName(HttpServletRequest request) {
        final String browserDetails = request.getHeader("User-Agent");
        if (browserDetails == null)
            return "UnKnown, More-Info:";

        final String user = browserDetails.toLowerCase();

        String browser = "";

        //===============Browser===========================
        if (user.contains("msie")) {
            String substring = browserDetails.substring(browserDetails.indexOf("MSIE")).split(";")[0];
            browser = substring.split(" ")[0].replace("MSIE", "IE") + "-" + substring.split(" ")[1];
        } else if (user.contains("safari") && user.contains("version")) {
            browser = (browserDetails.substring(browserDetails.indexOf("Safari")).split(" ")[0]).split(
                    "/")[0] + "-" + (browserDetails.substring(
                    browserDetails.indexOf("Version")).split(" ")[0]).split("/")[1];
        } else if (user.contains("opr") || user.contains("opera")) {
            if (user.contains("opera"))
                browser = (browserDetails.substring(browserDetails.indexOf("Opera")).split(" ")[0]).split(
                        "/")[0] + "-" + (browserDetails.substring(
                        browserDetails.indexOf("Version")).split(" ")[0]).split("/")[1];
            else if (user.contains("opr"))
                browser = ((browserDetails.substring(browserDetails.indexOf("OPR")).split(" ")[0]).replace("/",
                        "-")).replace(
                        "OPR", "Opera");
        } else if (user.contains("chrome")) {
            browser = (browserDetails.substring(browserDetails.indexOf("Chrome")).split(" ")[0]).replace("/", "-");
        } else if ((user.indexOf("mozilla/7.0") > -1) || (user.indexOf("netscape6") != -1) || (user.indexOf(
                "mozilla/4.7") != -1) || (user.indexOf("mozilla/4.78") != -1) || (user.indexOf(
                "mozilla/4.08") != -1) || (user.indexOf("mozilla/3") != -1)) {
            //browser=(userAgent.substring(userAgent.indexOf("MSIE")).split(" ")[0]).replace("/", "-");
            browser = "Netscape-?";

        } else if (user.contains("firefox")) {
            browser = (browserDetails.substring(browserDetails.indexOf("Firefox")).split(" ")[0]).replace("/", "-");
        } else if (user.contains("rv")) {
            browser = "IE";
        } else {
            browser = "UnKnown, More-Info: " + browserDetails;
        }

        return browser;
    }
}

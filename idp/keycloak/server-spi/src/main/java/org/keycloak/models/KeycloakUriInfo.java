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
package org.keycloak.models;

import org.jboss.logging.Logger;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class KeycloakUriInfo implements UriInfo {

    private final UriInfo delegate;
    private final String hostname;
    private final String scheme;
    private final int port;
    private final String contextPath;

    private URI absolutePath;
    private URI requestURI;
    private URI baseURI;
    private KeycloakSession session;

    private static final Logger logger = Logger.getLogger(KeycloakUriInfo.class);

    public KeycloakUriInfo(KeycloakSession session, UrlType type, UriInfo delegate) {
        this.delegate = delegate;

        HostnameProvider hostnameProvider = session.getProvider(HostnameProvider.class);
        this.session = session;
        this.scheme = hostnameProvider.getScheme(delegate, type);
        this.hostname = hostnameProvider.getHostname(delegate, type);
        this.port = hostnameProvider.getPort(delegate, type);
        this.contextPath = hostnameProvider.getContextPath(delegate, type);
    }

    public UriInfo getDelegate() {
        return delegate;
    }

    private UriBuilder initUriBuilder(UriBuilder b) {
        // On-prem / single-host: DASHBOARD_URL set means do NOT rewrite the host
        // to dashboard.<env>.<root-dns> or force https. Keep the actual request
        // scheme/host/port so the user stays on the host they are already on.
        String dashboardUrlOverride = System.getenv("DASHBOARD_URL");
        if (dashboardUrlOverride != null && !dashboardUrlOverride.isEmpty()) {
            b.scheme(scheme);
            b.host(hostname);
            b.port(port);
            b.replacePath(contextPath);
            return b;
        }

        String env = System.getenv("ENV");
        if (env == null || env.isEmpty()) {
            b.scheme(scheme);
            b.host(hostname);
            b.port(port);
            b.replacePath(contextPath);
            return b;
        }

        String tenantName = getTenantName();
        if (tenantName == null) {
            b.scheme(scheme);
            b.host(hostname);
            b.port(port);
        } else {
            b.scheme("https");
            String hostName = getHostname(this.getAbsolutePath().getAuthority());
            if (hostName != null) {
                b.host(hostName);
            } else {
                if (tenantName != null && tenantName.equalsIgnoreCase(org.keycloak.Config.getAdminRealm())) {
                    tenantName = "dashboard";
                }

                if (env.equalsIgnoreCase("prod")) {
                    b.host(tenantName + "." + System.getenv("ROOT_DNS_NAME"));
                } else {
                    String host = tenantName + "." + env + "." + System.getenv("ROOT_DNS_NAME");
                    b.host(host);
                }
            }
        }
        b.replacePath(contextPath);
        return b;
    }

    private String getTenantName() {
        if (session == null || session.getContext() == null || session.getContext().getRealm() == null
                || session.getContext().getRealm().getName() == null)
            return null;

        return session.getContext().getRealm().getName();
    }

    private RealmModel getRealm() {
        if (session == null || session.getContext() == null || session.getContext().getRealm() == null)
            return null;
        return session.getContext().getRealm();
    }

    private String getHostname(String hostname) {
        RealmModel realmModel = getRealm();
        if (realmModel == null)
            return null;
        boolean isDNSEnabled = realmModel.getCustomDomainMappings().filter(x-> x.isActive()).count() > 0 ? true : false;

        if(!isDNSEnabled || hostname.isEmpty() || hostname == null) {
            return null;
        }

        if(isDNSEnabled && realmModel.getCustomDomainMappings().filter(x -> x.isActive()).map(x -> x.getCustomDomain()).collect(Collectors.toList()).contains(hostname)) {
            return realmModel.getCustomDomainMappingByDomain(hostname).getCustomDomain();
        }

        return null;
    }

    @Override
    public URI getRequestUri() {
        if (requestURI == null) {
            requestURI = delegate.getRequestUri();
        }
        return requestURI;
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return UriBuilder.fromUri(getRequestUri());
    }

    @Override
    public URI getAbsolutePath() {
        if (absolutePath == null) {
            absolutePath = delegate.getAbsolutePath();
        }
        return absolutePath;
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return UriBuilder.fromUri(getAbsolutePath());
    }

    @Override
    public URI getBaseUri() {
        if (baseURI == null) {
            baseURI = initUriBuilder(delegate.getBaseUriBuilder()).build();
        }
        return baseURI;
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return UriBuilder.fromUri(getBaseUri());
    }

    @Override
    public URI resolve(URI uri) {
        return getBaseUri().resolve(uri);
    }

    @Override
    public URI relativize(URI uri) {
        URI from = this.getRequestUri();
        URI to = uri;
        if (uri.getScheme() == null && uri.getHost() == null) {
            to = this.getBaseUriBuilder().replaceQuery(null).path(uri.getPath()).replaceQuery(uri.getQuery())
                    .fragment(uri.getFragment()).build(new Object[0]);
        }

        return ResteasyUriBuilder.relativize(from, to);
    }

    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    public String getPath(boolean decode) {
        return delegate.getPath(decode);
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return delegate.getPathSegments();
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        return delegate.getPathSegments(decode);
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return delegate.getPathParameters();
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        return delegate.getPathParameters(decode);
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return delegate.getQueryParameters();
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return delegate.getQueryParameters(decode);
    }

    @Override
    public List<String> getMatchedURIs() {
        return delegate.getMatchedURIs();
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        return delegate.getMatchedURIs(decode);
    }

    @Override
    public List<Object> getMatchedResources() {
        return delegate.getMatchedResources();
    }
}

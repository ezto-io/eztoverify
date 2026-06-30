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

package org.keycloak.protocol.oidc;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.crypto.KeyType;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.endpoints.*;
import org.keycloak.protocol.oidc.ext.OIDCExtProvider;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.CacheControlUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.keycloak.utils.GeneralHelper.addCustomSupportMessageDetails;

/**
 * Resource class for the oauth/openid connect token service
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCLoginProtocolService {

    private static final Logger logger = Logger.getLogger(OIDCLoginProtocolService.class);

    private final RealmModel realm;
    private final TokenManager tokenManager;
    private final EventBuilder event;
    private final OIDCProviderConfig providerConfig;

    @Context
    private KeycloakSession session;

    @Context
    private HttpHeaders headers;

    @Context
    private HttpRequest request;

    @Context
    private ClientConnection clientConnection;

    public OIDCLoginProtocolService(RealmModel realm, EventBuilder event, OIDCProviderConfig providerConfig) {
        this.realm = realm;
        this.tokenManager = new TokenManager();
        this.event = event;
        this.providerConfig = providerConfig;
    }

    public static UriBuilder tokenServiceBaseUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return tokenServiceBaseUrl(baseUriBuilder);
    }

    public static UriBuilder tokenServiceBaseUrl(UriBuilder baseUriBuilder) {
        return baseUriBuilder.path(RealmsResource.class).path("{realm}/protocol/" + OIDCLoginProtocol.LOGIN_PROTOCOL);
    }

    public static UriBuilder authUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return authUrl(baseUriBuilder);
    }

    public static UriBuilder authUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "auth");
    }

    public static UriBuilder registrationsUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "registrations");
    }

    public static UriBuilder tokenUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "token");
    }

    public static UriBuilder backofficetokenUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "result");
    }

    public static UriBuilder certsUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "certs");
    }

    public static UriBuilder userInfoUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "issueUserInfo");
    }

    public static UriBuilder tokenIntrospectionUrl(UriBuilder baseUriBuilder) {
        return tokenUrl(baseUriBuilder).path(TokenEndpoint.class, "introspect");
    }

    public static UriBuilder logoutUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return logoutUrl(baseUriBuilder);
    }

    public static UriBuilder logoutUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "logout");
    }

    public static UriBuilder tokenRevocationUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "revoke");
    }

    /**
     * Authorization endpoint
     */
    @Path("auth")
    public Object auth() {
        AuthorizationEndpoint endpoint = new AuthorizationEndpoint(realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    /**
     * Registration endpoint
     */
    @Path("registrations")
    public Object registrations() {
        AuthorizationEndpoint endpoint = new AuthorizationEndpoint(realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint.register();
    }

    /**
     * Forgot-Credentials endpoint
     */
    @Path("forgot-credentials")
    public Object forgotCredentialsPage() {
        AuthorizationEndpoint endpoint = new AuthorizationEndpoint(realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint.forgotCredentials();
    }

    /**
     * Token endpoint
     */
    @Path("token")
    public Object token() {
        TokenEndpoint endpoint = new TokenEndpoint(tokenManager, realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    @Path("login-status-iframe.html")
    public Object getLoginStatusIframe() {
        LoginStatusIframeEndpoint endpoint = new LoginStatusIframeEndpoint();
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    @Path("3p-cookies")
    public Object thirdPartyCookiesCheck() {
        ThirdPartyCookiesIframeEndpoint endpoint = new ThirdPartyCookiesIframeEndpoint();
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    @OPTIONS
    @Path("certs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersionPreflight() {
        return Cors.add(request, Response.ok()).allowedMethods("GET").preflight().auth().build();
    }

    @GET
    @Path("certs")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response certs() {
        checkSsl();

        JWK[] jwks = session.keys().getKeysStream(realm)
                .filter(k -> k.getStatus().isEnabled() && k.getPublicKey() != null)
                .map(k -> {
                    JWKBuilder b = JWKBuilder.create().kid(k.getKid()).algorithm(k.getAlgorithmOrDefault());
                    List<X509Certificate> certificates = Optional.ofNullable(k.getCertificateChain())
                            .filter(certs -> !certs.isEmpty())
                            .orElseGet(() -> Collections.singletonList(k.getCertificate()));
                    if (k.getType().equals(KeyType.RSA)) {
                        return b.rsa(k.getPublicKey(), certificates, k.getUse());
                    } else if (k.getType().equals(KeyType.EC)) {
                        return b.ec(k.getPublicKey());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toArray(JWK[]::new);

        JSONWebKeySet keySet = new JSONWebKeySet();
        keySet.setKeys(jwks);

        Response.ResponseBuilder responseBuilder = Response.ok(keySet).cacheControl(CacheControlUtil.getDefaultCacheControl());
        return Cors.add(request, responseBuilder).allowedOrigins("*").auth().build();
    }

    @Path("userinfo")
    public Object issueUserInfo() {
        UserInfoEndpoint endpoint = new UserInfoEndpoint(tokenManager, realm);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    /* old deprecated logout endpoint needs to be removed in the future
     * https://issues.redhat.com/browse/KEYCLOAK-2940 */
    @Path("logout")
    public Object logout() {
        LogoutEndpoint endpoint = new LogoutEndpoint(tokenManager, realm, event, providerConfig);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    @Path("revoke")
    public Object revoke() {
        TokenRevocationEndpoint endpoint = new TokenRevocationEndpoint(realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    @Path("oauth/oob")
    @GET
    public Response installedAppUrnCallback(final @QueryParam("code") String code, final @QueryParam("error") String error, final @QueryParam("error_description") String errorDescription) {
        LoginFormsProvider forms = session.getProvider(LoginFormsProvider.class);
        if (code != null) {
            return forms.setClientSessionCode(code).createCode();
        } else {
            return forms.setError(error).createCode();
        }
    }

    @Path("ext/{extension}")
    public Object resolveExtension(@PathParam("extension") String extension) {
        OIDCExtProvider provider = session.getProvider(OIDCExtProvider.class, extension);
        if (provider != null) {
            provider.setEvent(event);
            return provider;
        }
        throw new NotFoundException();
    }

    @Path("transaction")
    public Object resolveBackofficeExtension() {
        OIDCExtProvider provider = session.getProvider(OIDCExtProvider.class, "transaction");
        if (provider != null) {
            provider.setEvent(event);
            return provider;
        }
        throw new NotFoundException();
    }

    @Path("transaction/result")
    public Object result() {
        if (!request.getHttpMethod().equals("POST")) {
            LoginFormsProvider formsProvider = session.getProvider(LoginFormsProvider.class);
            formsProvider.setError("Method not allowed")
                    .setAttribute("errorMessage", "method-not-allowed");
            Map<String, Object> attributes = addCustomSupportMessageDetails(null, realm, new HashMap<>());
            attributes.forEach(formsProvider::setAttribute);
            Response form = formsProvider.createForm("backoffice-error.ftl");
            throw new NotAllowedException(Response.status(Response.Status.METHOD_NOT_ALLOWED).entity(form.getEntity()).type(MediaType.TEXT_HTML).build());
        }
        TokenEndpoint endpoint = new TokenEndpoint(tokenManager, realm, event, "urn:openid:params:grant-type:ciba");
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https")
                && realm.getSslRequired().isRequired(clientConnection)) {
            Cors cors = Cors.add(request).auth().allowedMethods(request.getHttpMethod()).auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.INVALID_REQUEST, "HTTPS required",
                    Response.Status.FORBIDDEN);
        }
    }

}

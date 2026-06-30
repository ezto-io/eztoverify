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

package org.keycloak.protocol.oidc.endpoints;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.constants.APIErrorCodes;
import com.qlikverify.helpers.GeneralHelper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.authorization.AuthorizationTokenService;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.component.ComponentModel;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.*;
import org.keycloak.models.utils.AuthenticationFlowResolver;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.*;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.device.DeviceGrantType;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.BackofficeTokenResponse;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest.Metadata;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ResourceOwnerPasswordCredentialsContext;
import org.keycloak.services.clientpolicy.context.ServiceAccountTokenRequestContext;
import org.keycloak.services.clientpolicy.context.TokenRefreshContext;
import org.keycloak.services.clientpolicy.context.TokenRequestContext;
import org.keycloak.services.managers.*;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.util.AuthorizationContextUtil;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.util.MtlsHoKTokenUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.AESHelper;
import org.keycloak.utils.ProfileHelper;
import org.keycloak.utils.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.keycloak.models.AuthenticationFlowBindings.ACTION_BINDING;
import static org.keycloak.models.CibaConfig.OIDC_CIBA_GRANT_ENABLED;
import static org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper.updateResult;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TokenEndpoint {

    private static final Logger logger = Logger.getLogger(TokenEndpoint.class);
    private MultivaluedMap<String, String> formParams;
    private ClientModel client;
    private Map<String, String> clientAuthAttributes;
    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_SERVER_URL);
    private static final String DEFAULT_ACCESS_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_ACCESS_KEY);
    private static final String DEFAULT_SECRET_KEY = EnvironmentReader.getValue(AvatarConstants.MINIO_SECRET_KEY);

    private enum Action {
        AUTHORIZATION_CODE, REFRESH_TOKEN, PASSWORD, CLIENT_CREDENTIALS, TOKEN_EXCHANGE, PERMISSION, OAUTH2_DEVICE_CODE, CIBA
    }

    @Context
    private KeycloakSession session;

    @Context
    private HttpRequest request;

    @Context
    private HttpResponse httpResponse;

    @Context
    private HttpHeaders headers;

    @Context
    private ClientConnection clientConnection;

    private final TokenManager tokenManager;
    private final RealmModel realm;
    private final EventBuilder event;

    private Action action;

    private String grantType;

    private Cors cors;

    public TokenEndpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event) {
        this(tokenManager, realm, event, null);
    }

    public TokenEndpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event, String grantType) {
        this.tokenManager = tokenManager;
        this.realm = realm;
        this.event = event;
        this.grantType = grantType;
    }

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @POST
    public Response processGrantRequest() {
        cors = Cors.add(request).auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        MultivaluedMap<String, String> formParameters = request.getDecodedFormParameters();

        if (grantType != null && grantType.equals("urn:openid:params:grant-type:ciba") && formParameters != null) {

            List<String> appIds = null;
            List<String> appSecrets = null;
            String version = "0";
            List<String> expectedVersions = List.of("0", "1");

            if (request.getHttpHeaders() != null) {
                version = request.getHttpHeaders().getHeaderString("api-version");
                if (version == null || (version != null && !expectedVersions.contains(version)))
                    version = "0";
            }

            switch (version) {
                case "1": {
                    if (request.getHttpHeaders() == null)
                        throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Invalid app credentials", Response.Status.UNAUTHORIZED);

                    String authorizationHeader = request.getHttpHeaders().getHeaderString("Authorization");

                    if (authorizationHeader == null || authorizationHeader.isEmpty())
                        throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Invalid app credentials", Response.Status.UNAUTHORIZED);

                    String[] secrets = BasicAuthHelper.parseHeader(authorizationHeader);
                    appIds = Arrays.asList(secrets[0]);
                    appSecrets = Arrays.asList(secrets[1]);
                }
                break;
                default: {
                    if (version.equalsIgnoreCase("0") && !GeneralHelper.isV0Supported(realm)) {
                        throw new ErrorResponseException(APIErrorCodes.ez_transaction_1000.value, "Authentication method not supported. Please use v1 authentication.", Response.Status.UNAUTHORIZED);
                    }
                    if (!formParameters.containsKey("app_id") || !formParameters.containsKey("app_secret")) {
                        throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Invalid app credentials", Response.Status.UNAUTHORIZED);
                    }
                    appIds = formParameters.remove("app_id");
                    appSecrets = formParameters.remove("app_secret");
                }
                break;
            }

            if (appIds == null || appSecrets == null)
                throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Invalid app credentials", Response.Status.UNAUTHORIZED);

            ClientModel client = realm.getClientByClientId(appIds.get(0));

            formParameters.put("client_id", appIds);
            formParameters.put("client_secret", appSecrets);
        }

        if (formParameters == null) {
            formParameters = new MultivaluedHashMap<>();
        }

        formParams = formParameters;
        grantType = grantType == null ? formParams.getFirst(OIDCLoginProtocol.GRANT_TYPE_PARAM) : grantType;

        // https://tools.ietf.org/html/rfc6749#section-5.1
        // The authorization server MUST include the HTTP "Cache-Control" response header field
        // with a value of "no-store" as well as the "Pragma" response header field with a value of "no-cache".
        MultivaluedMap<String, Object> outputHeaders = httpResponse.getOutputHeaders();
        outputHeaders.putSingle("Cache-Control", "no-store");
        outputHeaders.putSingle("Pragma", "no-cache");

        checkSsl();
        checkRealm();
        checkGrantType();

        if (!action.equals(Action.PERMISSION)) {
            checkClient();
            checkParameterDuplicated();
        }

        switch (action) {
            case AUTHORIZATION_CODE:
                return codeToToken();
            case REFRESH_TOKEN:
                return refreshTokenGrant();
            case PASSWORD:
                return resourceOwnerPasswordCredentialsGrant();
            case CLIENT_CREDENTIALS:
                return clientCredentialsGrant();
            case TOKEN_EXCHANGE:
                return tokenExchange();
            case PERMISSION:
                return permissionGrant();
            case OAUTH2_DEVICE_CODE:
                return oauth2DeviceCodeToToken();
            case CIBA:
                return cibaGrant();
        }

        throw new RuntimeException("Unknown action " + action);
    }

    @Path("introspect")
    public Object introspect() {
        TokenIntrospectionEndpoint tokenIntrospectionEndpoint = new TokenIntrospectionEndpoint(this.realm, this.event);

        ResteasyProviderFactory.getInstance().injectProperties(tokenIntrospectionEndpoint);

        return tokenIntrospectionEndpoint;
    }

    @OPTIONS
    public Response preflight() {
        if (logger.isDebugEnabled()) {
            logger.debugv("CORS preflight from: {0}", headers.getRequestHeaders().getFirst("Origin"));
        }
        return Cors.add(request, Response.ok()).auth().preflight().allowedMethods("POST", "OPTIONS").build();
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), "access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    private void checkClient() {
        AuthorizeClientUtil.ClientAuthResult clientAuth;
        try {
            clientAuth = AuthorizeClientUtil.authorizeClient(session, event, cors);
        } catch (WebApplicationException wae) {
            OAuth2ErrorRepresentation errorRep = (OAuth2ErrorRepresentation) wae.getResponse().getEntity();
            throw new ErrorResponseException(errorRep.getError(), errorRep.getErrorDescription(), Response.Status.UNAUTHORIZED);
        }
        client = clientAuth.getClient();
        clientAuthAttributes = clientAuth.getClientAuthAttributes();

        cors.allowedOrigins(session, client);

        if (client.isBearerOnly()) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }


    }

    private void checkGrantType() {
        if (grantType == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Missing form parameter: " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
        }

        if (grantType.equals(OAuth2Constants.AUTHORIZATION_CODE)) {
            event.event(EventType.CODE_TO_TOKEN);
            action = Action.AUTHORIZATION_CODE;
        } else if (grantType.equals(OAuth2Constants.REFRESH_TOKEN)) {
            event.event(EventType.REFRESH_TOKEN);
            action = Action.REFRESH_TOKEN;
        } else if (grantType.equals(OAuth2Constants.PASSWORD)) {
            event.event(EventType.LOGIN);
            action = Action.PASSWORD;
        } else if (grantType.equals(OAuth2Constants.CLIENT_CREDENTIALS)) {
            event.event(EventType.CLIENT_LOGIN);
            action = Action.CLIENT_CREDENTIALS;
        } else if (grantType.equals(OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)) {
            event.event(EventType.TOKEN_EXCHANGE);
            action = Action.TOKEN_EXCHANGE;
        } else if (grantType.equals(OAuth2Constants.UMA_GRANT_TYPE)) {
            event.event(EventType.PERMISSION_TOKEN);
            action = Action.PERMISSION;
        } else if (grantType.equals(OAuth2Constants.DEVICE_CODE_GRANT_TYPE)) {
            event.event(EventType.OAUTH2_DEVICE_CODE_TO_TOKEN);
            action = Action.OAUTH2_DEVICE_CODE;
        } else if (grantType.equals(OAuth2Constants.CIBA_GRANT_TYPE)) {
            event.event(EventType.AUTHREQID_TO_TOKEN);
            action = Action.CIBA;
        } else {
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNSUPPORTED_GRANT_TYPE,
                    "Unsupported " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
        }

        event.detail(Details.GRANT_TYPE, grantType);
    }

    private void checkParameterDuplicated() {
        for (String key : formParams.keySet()) {
            if (formParams.get(key).size() != 1) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "duplicated parameter",
                        Response.Status.BAD_REQUEST);
            }
        }
    }

    public Response codeToToken() {
        String code = formParams.getFirst(OAuth2Constants.CODE);
        if (code == null) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Missing parameter: " + OAuth2Constants.CODE, Response.Status.BAD_REQUEST);
        }

        OAuth2CodeParser.ParseResult parseResult = OAuth2CodeParser.parseCode(session, code, realm, event);
        if (parseResult.isIllegalCode()) {
            AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();

            // Attempt to use same code twice should invalidate existing clientSession
            if (clientSession != null) {
                clientSession.detachFromUserSession();
            }

            event.error(Errors.INVALID_CODE);

            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code not valid", Response.Status.BAD_REQUEST);
        }

        AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();

        if (parseResult.isExpiredCode()) {
            event.error(Errors.EXPIRED_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code is expired", Response.Status.BAD_REQUEST);
        }

        UserSessionModel userSession = null;
        if (clientSession != null) {
            userSession = clientSession.getUserSession();
        }

        if (userSession == null) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User session not found", Response.Status.BAD_REQUEST);
        }


        UserModel user = userSession.getUser();
        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User not found", Response.Status.BAD_REQUEST);
        }

        event.user(userSession.getUser());

        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User disabled", Response.Status.BAD_REQUEST);
        }

        OAuth2Code codeData = parseResult.getCodeData();
        String redirectUri = codeData.getRedirectUriParam();
        String redirectUriParam = formParams.getFirst(OAuth2Constants.REDIRECT_URI);

        // KEYCLOAK-4478 Backwards compatibility with the adapters earlier than KC 3.4.2
        if (redirectUriParam != null && redirectUriParam.contains("session_state=") && !redirectUri.contains("session_state=")) {
            redirectUriParam = KeycloakUriBuilder.fromUri(redirectUriParam)
                    .replaceQueryParam(OAuth2Constants.SESSION_STATE, null)
                    .build().toString();
        }

        if (redirectUri != null && !redirectUri.equals(URLDecoder.decode(redirectUriParam, StandardCharsets.UTF_8))) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Incorrect redirect_uri", Response.Status.BAD_REQUEST);
        }

        if (!client.getClientId().equals(clientSession.getClient().getClientId())) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Auth error", Response.Status.BAD_REQUEST);
        }

        if (!client.isStandardFlowEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Client not allowed to exchange code", Response.Status.BAD_REQUEST);
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Session not active", Response.Status.BAD_REQUEST);
        }

        // https://tools.ietf.org/html/rfc7636#section-4.6
        String codeVerifier = formParams.getFirst(OAuth2Constants.CODE_VERIFIER);
        String codeChallenge = codeData.getCodeChallenge();
        String codeChallengeMethod = codeData.getCodeChallengeMethod();
        String authUserId = user.getId();
        String authUsername = user.getUsername();
        if (authUserId == null) {
            authUserId = "unknown";
        }
        if (authUsername == null) {
            authUsername = "unknown";
        }

        if (codeChallengeMethod != null && !codeChallengeMethod.isEmpty()) {
            PkceUtils.checkParamsForPkceEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername, event, cors);
        } else {
            // PKCE Activation is OFF, execute the codes implemented in KEYCLOAK-2604
            PkceUtils.checkParamsForPkceNotEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername, event, cors);
        }

        try {
            session.clientPolicy().triggerOnEvent(new TokenRequestContext(formParams, parseResult));
        } catch (ClientPolicyException cpe) {
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        updateClientSession(clientSession);
        updateUserSessionFromClientAuth(userSession);

        // Compute client scopes again from scope parameter. Check if user still has them granted
        // (but in code-to-token request, it could just theoretically happen that they are not available)
        String scopeParam = codeData.getScope();
        Supplier<Stream<ClientScopeModel>> clientScopesSupplier = () -> TokenManager.getRequestedClientScopes(scopeParam, client);
        if (!TokenManager.verifyConsentStillAvailable(session, user, client, clientScopesSupplier.get())) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, "Client no longer has requested consent from user", Response.Status.BAD_REQUEST);
        }

        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, scopeParam, session);

        // Set nonce as an attribute in the ClientSessionContext. Will be used for the token generation
        clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, codeData.getNonce());

        return createTokenResponse(user, userSession, clientSessionCtx, scopeParam, true);
    }

    public Response createTokenResponse(UserModel user, UserSessionModel userSession, ClientSessionContext clientSessionCtx,
                                        String scopeParam, boolean code) {
        AccessTokenResponse res = getAccessTokenResponse(user, userSession, clientSessionCtx, scopeParam, code);
        event.success();
        return cors.builder(Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE)).build();
    }

    private AccessTokenResponse getAccessTokenResponse(UserModel user, UserSessionModel userSession, ClientSessionContext clientSessionCtx, String scopeParam, boolean code) {
        AccessToken token = tokenManager.createClientAccessToken(session, realm, client, user, userSession, clientSessionCtx);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager
                .responseBuilder(realm, client, event, session, userSession, clientSessionCtx).accessToken(token);
        if (OIDCAdvancedConfigWrapper.fromClientModel(client).isUseRefreshToken()) {
            responseBuilder.generateRefreshToken();
        }

        checkMtlsHoKToken(responseBuilder, OIDCAdvancedConfigWrapper.fromClientModel(client).isUseRefreshToken());

        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        AccessTokenResponse res = null;
        if (code) {
            try {
                res = responseBuilder.build();
            } catch (RuntimeException re) {
                if ("can not get encryption KEK".equals(re.getMessage())) {
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                            "can not get encryption KEK", Status.BAD_REQUEST);
                } else {
                    throw re;
                }
            }
        } else {
            res = responseBuilder.build();
        }
        return res;
    }

    public Response createTokenResponse(KeycloakSession session, ClientModel clientModel, Cors cors, UserModel user, UserSessionModel userSession, ClientSessionContext clientSessionCtx,
                                        String scopeParam, boolean code) {
        if (this.session == null) {
            this.session = session;
        }
        if (this.client == null) {
            this.client = clientModel;
        }

        if (this.cors == null) {
            this.cors = cors;
        }
        AccessTokenResponse res = getAccessTokenResponse(user, userSession, clientSessionCtx, scopeParam, code);
        String cibaOIDCGrant = client.getAttribute(OIDC_CIBA_GRANT_ENABLED);
        boolean isCiba = cibaOIDCGrant != null && Boolean.parseBoolean(cibaOIDCGrant);
        if (isCiba) {

            CibaConfig cibaConfig = realm.getCibaPolicy();
            String notProviderId = cibaConfig.getBackchannelTokenDeliveryMode(client);
            CibaNotificationConfigModel configModel = realm.getCibaNotificationConfigByProviderId(notProviderId, client.getClientId(), realm.getId());
            String responseNotificationWay = configModel.getConfig().get("notification-response-way");
            if (res.getError() != null && responseNotificationWay.equals("token")) {
                event.success();
                return cors.builder(Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE)).build();
            }
            BackofficeTokenResponse backofficeTokenResponse = new BackofficeTokenResponse();
//            List<String> claimsRequiredForClient = new ArrayList<>();
//            HashMap<String, String> claimsToBeSent = new HashMap<>();
            boolean skipUserAttrs = Boolean.parseBoolean(this.formParams.getFirst("skip_user_attrs"));
//            boolean isClaimsEmpty = false;
            backofficeTokenResponse.setError(res.getError());
            backofficeTokenResponse.setErrorDescription(res.getErrorDescription());

            String requestId = userSession.getNote("requestId");
            TrxModel trxModel = realm.getTrx(requestId);
            if (res.getError() == null) {
//                claimsRequiredForClient = GeneralHelper.getClaimsForCurrentClient(realm, clientModel);
//
//                isClaimsEmpty = true;
//                List<String> claimsForClient = claimsRequiredForClient;
//                trxModel.getAttributes().keySet().forEach(x -> {
//                    if (x.startsWith("sp@") && !claimsForClient.contains(x.split("@")[1]) && !x.equalsIgnoreCase("sp@completed_dynamic_form")) {
//                        claimsForClient.add(x.replace("sp@", ""));
//                    }
//                    if (x.equals("liveness_completed")) {
//                        claimsForClient.add(x);
//                    }
//                });
//                if (!skipUserAttrs) {
//                    claimsToBeSent = GeneralHelper.getValuesFromUserMetadate(claimsForClient, trxModel);
//                }

                processEndResult(trxModel.getAttributes(), backofficeTokenResponse);
//                processResult(trxModel.getAttributes(), backofficeTokenResponse);
            }

            if (backofficeTokenResponse.getResult().containsKey("error_code")) {
                List errorObject = new ArrayList();
                try {
                    errorObject = JsonSerialization.readValue(backofficeTokenResponse.getResult().get("error_code").toString(), List.class);
                } catch (Exception e) {
                    logger.error("TokenEndpoint :: createTokenResponse :: " + e.getMessage(), e);
                }

                List<Map<String, String>> errorsList = new ArrayList<>();

                if (errorObject instanceof List) {
                    errorObject.stream().forEach(x -> {
                        if (x instanceof String) {
                            try {
                                Map<String, String> a = JsonSerialization.readValue(String.valueOf(x), Map.class);
                                errorsList.add(a);
                            } catch (Exception e) {
                                logger.error("TokenEndpoint :: createTokenResponse :: " + x + "::" + e.getMessage(), e);
                            }
                        } else {
                            errorsList.add((Map<String, String>) x);
                        }
                    });
                }
                backofficeTokenResponse.getResult().put("error_code", errorsList);
            }

            if (!backofficeTokenResponse.getResult().keySet().contains("t1_request_id")) {
                if ("v0".equals(configModel.getConfig().get("result-version"))) {
                    backofficeTokenResponse.getResult().keySet().removeIf(x -> x.contains("error_code") || x.contains("_error_code"));
                } else {
                    if (backofficeTokenResponse.getResult().keySet().contains("error_code")) {
                        backofficeTokenResponse.getResult().put("error", backofficeTokenResponse.getResult().get("error_code"));
                        backofficeTokenResponse.getResult().remove("error_code");
                    }
                    backofficeTokenResponse.getResult().keySet().removeIf(x -> x.contains("_failure_reason") || x.contains("_error_code"));
                }
            }
            backofficeTokenResponse.getResult().remove("include_qr_payload");
            backofficeTokenResponse.getResult().remove("linkExp");
            backofficeTokenResponse.getResult().remove("hidden_ua_message");
            backofficeTokenResponse.getResult().remove("status_code");
            backofficeTokenResponse.getResult().keySet().removeIf(x -> x.contains("ik@"));

            backofficeTokenResponse.setRequestId(requestId);
            backofficeTokenResponse.setResult("user_id", userSession.getUser().getUsername());

            updateResult(session, realm, client, user, requestId);
            if (backofficeTokenResponse.getResult().containsKey("request_id")) {
                backofficeTokenResponse.getResult().remove("request_id");
            }
            String encryptedResponse = null;
            boolean isEncrypted = responseNotificationWay != null && responseNotificationWay.equals("encrypted");
            if (isEncrypted) {
                String secretKey = null;
                HashMap<String, Object> config = new HashMap<>();
                config.put("secret-keys", configModel.getConfig().get("secret-keys"));
                ComponentRepresentation componentRepresentation = getEncryptionComponent(session, config);
                if (componentRepresentation != null) {
                    secretKey = componentRepresentation.getConfig().getFirst("secret");
                }
                try {
                    encryptedResponse = JsonSerialization.writeValueAsString(backofficeTokenResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    encryptedResponse = AESHelper.encrypt(encryptedResponse, secretKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String finalEncryptedResponse = encryptedResponse;
                try {
                    encryptedResponse = JsonSerialization.writeValueAsString(new HashMap<String, String>() {{
                        put("data", finalEncryptedResponse);
                    }});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            realm.removeTrx(requestId);
            return cors.builder(Response.ok(isEncrypted ? encryptedResponse : backofficeTokenResponse).type(MediaType.APPLICATION_JSON_TYPE)).build();
        }
        event.success();
        return cors.builder(Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE)).build();
    }

    public Response createTokenResponse(KeycloakSession session, ClientModel clientModel, UserModel user, UserSessionModel userSession, ClientSessionContext clientSessionCtx,
                                        String scopeParam, boolean code) {
        if (this.session == null) {
            this.session = session;
        }
        if (this.client == null) {
            this.client = clientModel;
        }

        AccessTokenResponse res = getAccessTokenResponse(user, userSession, clientSessionCtx, scopeParam, code);
        String cibaOIDCGrant = client.getAttribute(OIDC_CIBA_GRANT_ENABLED);
        boolean isCiba = cibaOIDCGrant != null && Boolean.parseBoolean(cibaOIDCGrant);
        if (isCiba) {
            CibaConfig cibaConfig = realm.getCibaPolicy();
            String notProviderId = cibaConfig.getBackchannelTokenDeliveryMode(client);
            CibaNotificationConfigModel configModel = realm.getCibaNotificationConfigByProviderId(notProviderId, client.getClientId(), realm.getId());
            String responseNotificationWay = configModel.getConfig().get("notification-response-way");
            if (responseNotificationWay == null) {
                responseNotificationWay = "plaintext";
            }
            if (res.getError() == null && responseNotificationWay.equals("token")) {
                event.success();
                return Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE).build();
            }
            BackofficeTokenResponse backofficeTokenResponse = new BackofficeTokenResponse();
//            List<String> claimsRequiredForClient = new ArrayList<>();
//            HashMap<String, String> claimsToBeSent = new HashMap<>();
//            boolean isClaimsEmpty = false;
            backofficeTokenResponse.setError(res.getError());
            backofficeTokenResponse.setErrorDescription(res.getErrorDescription());
            String requestId = userSession.getNote("requestId");
            TrxModel trxModel = realm.getTrx(requestId);
            if (res.getError() == null) {
//                claimsRequiredForClient = GeneralHelper.getClaimsForCurrentClient(realm, clientModel);

//                isClaimsEmpty = true;
//                List<String> claimsForClient = claimsRequiredForClient;
//                trxModel.getAttributes().keySet().forEach(x -> {
//                    if (x.startsWith("sp@") && !claimsForClient.contains(x.split("@")[1]) && !x.equalsIgnoreCase("sp@completed_dynamic_form")) {
//                        claimsForClient.add(x.replace("sp@", ""));
//                    }
//                });
//                claimsToBeSent = GeneralHelper.getValuesFromUserMetadate(claimsForClient, trxModel);
//
//                if (!isClaimsEmpty) {
//                    claimsToBeSent = GeneralHelper.getValuesFromUserMetadate(claimsRequiredForClient, trxModel);
//                }
                processEndResult(trxModel.getAttributes(), backofficeTokenResponse);
//               processResult(claimsToBeSent, backofficeTokenResponse);
            }

            if (backofficeTokenResponse.getResult().containsKey("error_code")) {
                List errorObject = new ArrayList();
                try {
                    errorObject = JsonSerialization.readValue(backofficeTokenResponse.getResult().get("error_code").toString(), List.class);
                } catch (Exception e) {
                    logger.error("TokenEndpoint :: createTokenResponse :: " + e.getMessage(), e);
                }

                List<Map<String, String>> errorsList = new ArrayList<>();

                if (errorObject instanceof List) {
                    errorObject.stream().forEach(x -> {
                        if (x instanceof String) {
                            try {
                                Map<String, String> a = JsonSerialization.readValue(String.valueOf(x), Map.class);
                                errorsList.add(a);
                            } catch (Exception e) {
                                logger.error("TokenEndpoint :: createTokenResponse :: " + x + "::" + e.getMessage(), e);
                            }
                        } else {
                            errorsList.add((Map<String, String>) x);
                        }
                    });
                }
                backofficeTokenResponse.getResult().put("error_code", errorsList);
            }

            if (!backofficeTokenResponse.getResult().keySet().contains("t1_request_id")) {
                if ("v0".equals(configModel.getConfig().get("result-version"))) {
                    backofficeTokenResponse.getResult().keySet().removeIf(x -> x.contains("error_code") || x.contains("_error_code"));
                } else {
                    if (backofficeTokenResponse.getResult().keySet().contains("error_code")) {
                        backofficeTokenResponse.getResult().put("error", backofficeTokenResponse.getResult().get("error_code"));
                        backofficeTokenResponse.getResult().remove("error_code");
                    }
                    backofficeTokenResponse.getResult().keySet().removeIf(x -> x.contains("_failure_reason") || x.contains("_error_code"));
                }
            }

            backofficeTokenResponse.getResult().remove("include_qr_payload");
            backofficeTokenResponse.getResult().remove("linkExp");
            backofficeTokenResponse.getResult().remove("hidden_ua_message");
            backofficeTokenResponse.getResult().remove("status_code");
            backofficeTokenResponse.getResult().keySet().removeIf(x -> x.contains("ik@"));

            backofficeTokenResponse.setRequestId(requestId);
            backofficeTokenResponse.setResult("user_id", userSession.getUser().getUsername());

//            updateResult(session, realm, client, user, requestId);

            String encryptedResponse = null;
            boolean isEncrypted = responseNotificationWay != null && responseNotificationWay.equals("encrypted");
            if (isEncrypted) {
                String secretKey = null;
                HashMap<String, Object> config = new HashMap<>();
                config.put("secret-keys", configModel.getConfig().get("secret-keys"));
                ComponentRepresentation componentRepresentation = getEncryptionComponent(session, config);
                if (componentRepresentation != null) {
                    secretKey = componentRepresentation.getConfig().getFirst("secret");
                }
                try {
                    encryptedResponse = JsonSerialization.writeValueAsString(backofficeTokenResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    encryptedResponse = AESHelper.encrypt(encryptedResponse, secretKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String finalEncryptedResponse = encryptedResponse;
                try {
                    encryptedResponse = JsonSerialization.writeValueAsString(new HashMap<String, String>() {{
                        put("data", finalEncryptedResponse);
                    }});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            realm.removeTrx(requestId);
            return Response.ok(isEncrypted ? encryptedResponse : backofficeTokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
        } else {
            event.success();
        }
        return Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * This method has the logic where the final claims are processed.
     * If the flow is web2device all the claims from the device will be sent to the web without any change.
     * If the flow is web the end result i.e, claims will be processed as expected.
     * 'ik@' (internal key) format is used to differentiate the keys that needs to be removed.
     * @param claims
     * @param backofficeTokenResponse
     */
    private static void processEndResult(Map<String, String> claims, BackofficeTokenResponse backofficeTokenResponse) {
        boolean isWeb2Device = claims.containsKey("ik@t1_request_id");

        if (isWeb2Device) {
            claims.forEach((key, value) -> backofficeTokenResponse.setResult(key, claims.get(key)));
            return;
        }

        claims.keySet().stream()
                .filter(key -> !key.contains("ik@"))
                .forEach(key -> {
                    String resultKey = (key.contains("@") ? key.split("@")[1] : key);
                    backofficeTokenResponse.setResult(resultKey, claims.get(key));
                });
    }


    private static void processResult(Map<String, String> claimsToBeSent, BackofficeTokenResponse backofficeTokenResponse) {
        //here only verified result will be sent to the user and if any key already exists, it will be overridden
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, String> entry : claimsToBeSent.entrySet()) {
            if (entry.getKey().contains("@") && claimsToBeSent.keySet().contains(entry.getKey().split("@")[1])) {
                keysToRemove.add(entry.getKey().split("@")[1]);
            }
        }

        claimsToBeSent.keySet().removeAll(keysToRemove);

        for (Map.Entry<String, String> entry : claimsToBeSent.entrySet()) {
            if (entry.getKey().contains("@")) {
                backofficeTokenResponse.setResult(entry.getKey().split("@")[1], entry.getValue());
            } else {
                backofficeTokenResponse.setResult(entry.getKey(), entry.getValue());
            }
        }
    }

    private ComponentRepresentation getEncryptionComponent(KeycloakSession session, HashMap<String, Object> config) {
        ComponentModel optionalComponentModel = session.getContext().getRealm().getComponent(String.valueOf(config.get("secret-keys")));
        if (optionalComponentModel == null) {
            return null;
        }

        return ModelToRepresentation.toRepresentation(session, optionalComponentModel, true);
    }

    private void getAndSetClaims(BackofficeTokenResponse backofficeTokenResponse, List<String> claims, String realmName, String trxId, String verificationId) {

        String objectName = trxId + "/" + verificationId + ".json";

        ObjectMapper objectMapper = new ObjectMapper();

        MinioClient minioClient = MinioClient.builder().endpoint(DEFAULT_SERVER_URL).credentials(DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY).build();
        try {
            minioClient.ignoreCertCheck();
            InputStream object = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(realmName).object(objectName).build());
            String minioData = new String(object.readAllBytes(), StandardCharsets.UTF_8);
            if (StringUtil.isBlank(minioData))
                return;
            HashMap<String, Object> retrievedData = objectMapper.readValue(minioData, HashMap.class);

            if (claims.stream().anyMatch(retrievedData::containsKey))
                retrievedData.forEach(backofficeTokenResponse::setResult);

        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (io.minio.errors.ErrorResponseException e) {
            logger.error("object not found in Minio");
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkMtlsHoKToken(TokenManager.AccessTokenResponseBuilder responseBuilder, boolean useRefreshToken) {
        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
        if (OIDCAdvancedConfigWrapper.fromClientModel(client).isUseMtlsHokToken()) {
            AccessToken.CertConf certConf = MtlsHoKTokenUtil.bindTokenWithClientCertificate(request, session);
            if (certConf != null) {
                responseBuilder.getAccessToken().setCertConf(certConf);
                if (useRefreshToken) {
                    responseBuilder.getRefreshToken().setCertConf(certConf);
                }
            } else {
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                        "Client Certification missing for MTLS HoK Token Binding", Response.Status.BAD_REQUEST);
            }
        }
    }

    public Response refreshTokenGrant() {
        String refreshToken = formParams.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "No refresh token", Response.Status.BAD_REQUEST);
        }

        try {
            session.clientPolicy().triggerOnEvent(new TokenRefreshContext(formParams));
        } catch (ClientPolicyException cpe) {
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        AccessTokenResponse res;
        try {
            // KEYCLOAK-6771 Certificate Bound Token
            TokenManager.RefreshResult result = tokenManager.refreshAccessToken(session, session.getContext().getUri(), clientConnection, realm, client, refreshToken, event, headers, request);
            res = result.getResponse();

            if (!result.isOfflineToken()) {
                UserSessionModel userSession = session.sessions().getUserSession(realm, res.getSessionState());
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
                updateClientSession(clientSession);
                updateUserSessionFromClientAuth(userSession);
            }

        } catch (OAuthErrorException e) {
            logger.trace(e.getMessage(), e);
            // KEYCLOAK-6771 Certificate Bound Token
            if (MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC.equals(e.getDescription())) {
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.UNAUTHORIZED);
            } else {
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
            }
        }

        event.success();

        return cors.builder(Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).build();
    }

    private void updateClientSession(AuthenticatedClientSessionModel clientSession) {

        if (clientSession == null) {
            ServicesLogger.LOGGER.clientSessionNull();
            return;
        }

        String adapterSessionId = formParams.getFirst(AdapterConstants.CLIENT_SESSION_STATE);
        if (adapterSessionId != null) {
            String adapterSessionHost = formParams.getFirst(AdapterConstants.CLIENT_SESSION_HOST);
            logger.debugf("Adapter Session '%s' saved in ClientSession for client '%s'. Host is '%s'", adapterSessionId, client.getClientId(), adapterSessionHost);

            String oldClientSessionState = clientSession.getNote(AdapterConstants.CLIENT_SESSION_STATE);
            if (!adapterSessionId.equals(oldClientSessionState)) {
                clientSession.setNote(AdapterConstants.CLIENT_SESSION_STATE, adapterSessionId);
            }

            String oldClientSessionHost = clientSession.getNote(AdapterConstants.CLIENT_SESSION_HOST);
            if (!Objects.equals(adapterSessionHost, oldClientSessionHost)) {
                clientSession.setNote(AdapterConstants.CLIENT_SESSION_HOST, adapterSessionHost);
            }
        }
    }

    private void updateUserSessionFromClientAuth(UserSessionModel userSession) {
        for (Map.Entry<String, String> attr : clientAuthAttributes.entrySet()) {
            userSession.setNote(attr.getKey(), attr.getValue());
        }
    }

    public Response resourceOwnerPasswordCredentialsGrant() {
        event.detail(Details.AUTH_METHOD, "oauth_credentials");

        if (!client.isDirectAccessGrantsEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Client not allowed for direct access grants", Response.Status.BAD_REQUEST);
        }

        if (client.isConsentRequired()) {
            event.error(Errors.CONSENT_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Client requires user consent", Response.Status.BAD_REQUEST);
        }

        try {
            session.clientPolicy().triggerOnEvent(new ResourceOwnerPasswordCredentialsContext(formParams));
        } catch (ClientPolicyException cpe) {
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        String scope = getRequestedScopes();

        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setAction(AuthenticatedClientSessionModel.Action.AUTHENTICATE.name());
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        AuthenticationFlowModel flow = AuthenticationFlowResolver.resolveDirectGrantFlow(authSession);
        String flowId = flow.getId();
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setAuthenticationSession(authSession)
                .setFlowId(flowId)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(request);
        Response challenge = processor.authenticateOnly();
        if (challenge != null) {
            // Remove authentication session as "Resource Owner Password Credentials Grant" is single-request scoped authentication
            new AuthenticationSessionManager(session).removeAuthenticationSession(realm, authSession, false);
            cors.build(httpResponse);
            return challenge;
        }
        processor.evaluateRequiredActionTriggers(client.getAuthenticationFlowBindingOverride(ACTION_BINDING));
        UserModel user = authSession.getAuthenticatedUser();
        if (user.getRequiredActionsStream().count() > 0 || authSession.getRequiredActions().size() > 0) {
            // Remove authentication session as "Resource Owner Password Credentials Grant" is single-request scoped authentication
            new AuthenticationSessionManager(session).removeAuthenticationSession(realm, authSession, false);
            event.error(Errors.RESOLVE_REQUIRED_ACTIONS);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Account is not fully set up", Response.Status.BAD_REQUEST);

        }

        // Bypassed license check
        AuthenticationManager.setClientScopesInSession(authSession);

        ClientSessionContext clientSessionCtx = processor.attachSession();
        UserSessionModel userSession = processor.getUserSession();
        updateUserSessionFromClientAuth(userSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager
                .responseBuilder(realm, client, event, session, userSession, clientSessionCtx).generateAccessToken();
        if (OIDCAdvancedConfigWrapper.fromClientModel(client).isUseRefreshToken()) {
            responseBuilder.generateRefreshToken();
        }

        String scopeParam = clientSessionCtx.getClientSession().getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        // TODO : do the same as codeToToken()
        AccessTokenResponse res = responseBuilder.build();


        event.success();
        AuthenticationManager.logSuccess(session, authSession);

        return cors.builder(Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).build();
    }

    public Response clientCredentialsGrant() {
        if (client.isBearerOnly()) {
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Bearer-only client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (client.isPublicClient()) {
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Public client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (!client.isServiceAccountsEnabled()) {
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Client not enabled to retrieve service account", Response.Status.UNAUTHORIZED);
        }

        // Bypassed license check

        UserModel clientUser = session.users().getServiceAccount(client);

        if (clientUser == null || client.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, ServiceAccountConstants.CLIENT_ID_PROTOCOL_MAPPER) == null) {
            // May need to handle bootstrap here as well
            logger.debugf("Service account user for client '%s' not found or default protocol mapper for service account not found. Creating now", client.getClientId());
            new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            clientUser = session.users().getServiceAccount(client);
        }

        String clientUsername = clientUser.getUsername();
        event.detail(Details.USERNAME, clientUsername);
        event.user(clientUser);

        if (!clientUser.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "User '" + clientUsername + "' disabled", Response.Status.UNAUTHORIZED);
        }

        String scope = getRequestedScopes();

        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setAuthenticatedUser(clientUser);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        // persisting of userSession by default
        UserSessionModel.SessionPersistenceState sessionPersistenceState = UserSessionModel.SessionPersistenceState.PERSISTENT;

        boolean useRefreshToken = OIDCAdvancedConfigWrapper.fromClientModel(client).isUseRefreshTokenForClientCredentialsGrant();
        if (!useRefreshToken) {
            // we don't want to store a session hence we mark it as transient, see KEYCLOAK-9551
            sessionPersistenceState = UserSessionModel.SessionPersistenceState.TRANSIENT;
        }

        UserSessionModel userSession = session.sessions().createUserSession(authSession.getParentSession().getId(), realm, clientUser, clientUsername,
                clientConnection.getRemoteAddr(), ServiceAccountConstants.CLIENT_AUTH, false, null, null, sessionPersistenceState);
        event.session(userSession);

        AuthenticationManager.setClientScopesInSession(authSession);
        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, userSession, authSession);

        // Notes about client details
        userSession.setNote(ServiceAccountConstants.CLIENT_ID, client.getClientId());
        userSession.setNote(ServiceAccountConstants.CLIENT_HOST, clientConnection.getRemoteHost());
        userSession.setNote(ServiceAccountConstants.CLIENT_ADDRESS, clientConnection.getRemoteAddr());

        try {
            session.clientPolicy().triggerOnEvent(new ServiceAccountTokenRequestContext(formParams, clientSessionCtx.getClientSession()));
        } catch (ClientPolicyException cpe) {
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        updateUserSessionFromClientAuth(userSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSessionCtx)
                .generateAccessToken();

        // Make refresh token generation optional, see KEYCLOAK-9551
        if (useRefreshToken) {
            responseBuilder = responseBuilder.generateRefreshToken();
        } else {
            responseBuilder.getAccessToken().setSessionState(null);
        }

        checkMtlsHoKToken(responseBuilder, useRefreshToken);

        String scopeParam = clientSessionCtx.getClientSession().getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        // TODO : do the same as codeToToken()
        AccessTokenResponse res = responseBuilder.build();

        event.success();

        return cors.builder(Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).build();
    }

    private String getRequestedScopes() {
        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        boolean validScopes;
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            AuthorizationRequestContext authorizationRequestContext = AuthorizationContextUtil.getAuthorizationRequestContextFromScopes(session, scope);
            validScopes = TokenManager.isValidScope(scope, authorizationRequestContext, client);
        } else {
            validScopes = TokenManager.isValidScope(scope, client);
        }

        if (!validScopes) {
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, "Invalid scopes: " + scope,
                    Status.BAD_REQUEST);
        }

        return scope;
    }

    public Response tokenExchange() {
        ProfileHelper.requireFeature(Profile.Feature.TOKEN_EXCHANGE);

        event.detail(Details.AUTH_METHOD, "token_exchange");
        event.client(client.getClientId());

        TokenExchangeContext context = new TokenExchangeContext(
                session,
                formParams,
                cors,
                realm,
                event,
                client,
                clientConnection,
                headers,
                tokenManager,
                clientAuthAttributes);

        return session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(TokenExchangeProvider.class)
                .sorted((f1, f2) -> f2.order() - f1.order())
                .map(f -> session.getProvider(TokenExchangeProvider.class, f.getId()))
                .filter(p -> p.supports(context))
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException("No token exchange provider available"))
                .exchange(context);
    }

    public Response permissionGrant() {
        event.detail(Details.AUTH_METHOD, "oauth_credentials");

        String accessTokenString = null;
        String authorizationHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer")) {
            accessTokenString = new AppAuthManager().extractAuthorizationHeaderToken(headers);
        }

        // we allow public clients to authenticate using a bearer token, where the token should be a valid access token.
        // public clients don't have secret and should be able to obtain a RPT by providing an access token previously issued by the server
        if (accessTokenString != null) {
            AccessToken accessToken = Tokens.getAccessToken(session);

            if (accessToken == null) {
                try {
                    // In case the access token is invalid because it's expired or the user is disabled, identify the client
                    // from the access token anyway in order to set correct CORS headers.
                    AccessToken invalidToken = new JWSInput(accessTokenString).readJsonContent(AccessToken.class);
                    ClientModel client = realm.getClientByClientId(invalidToken.getIssuedFor());
                    cors.allowedOrigins(session, client);
                    event.client(client.getClientId());
                } catch (JWSInputException ignore) {
                }
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Invalid bearer token", Status.UNAUTHORIZED);
            }

            ClientModel client = realm.getClientByClientId(accessToken.getIssuedFor());

            session.getContext().setClient(client);

            cors.allowedOrigins(session, client);
            event.client(client.getClientId());
        }

        String claimToken = null;

        // claim_token is optional, if provided we just grab it from the request
        if (formParams.containsKey("claim_token")) {
            claimToken = formParams.get("claim_token").get(0);
        }

        String claimTokenFormat = formParams.getFirst("claim_token_format");

        if (claimToken != null && claimTokenFormat == null) {
            claimTokenFormat = AuthorizationTokenService.CLAIM_TOKEN_FORMAT_ID_TOKEN;
        }

        String subjectToken = formParams.getFirst("subject_token");

        if (accessTokenString == null) {
            // in case no bearer token is provided, we force client authentication
            checkClient();

            // if a claim token is provided, we check if the format is a OpenID Connect IDToken and assume the token represents the identity asking for permissions
            if (AuthorizationTokenService.CLAIM_TOKEN_FORMAT_ID_TOKEN.equalsIgnoreCase(claimTokenFormat)) {
                accessTokenString = claimToken;
            } else if (subjectToken != null) {
                accessTokenString = subjectToken;
            } else {
                // Clients need to authenticate in order to obtain a RPT from the server.
                // In order to support cases where the client is obtaining permissions on its on behalf, we issue a temporary access token
                accessTokenString = AccessTokenResponse.class.cast(clientCredentialsGrant().getEntity()).getToken();
            }
        }

        AuthorizationTokenService.KeycloakAuthorizationRequest authorizationRequest = new AuthorizationTokenService.KeycloakAuthorizationRequest(session.getProvider(AuthorizationProvider.class),
                tokenManager, event, this.request, cors, clientConnection);

        authorizationRequest.setTicket(formParams.getFirst("ticket"));
        authorizationRequest.setClaimToken(claimToken);
        authorizationRequest.setClaimTokenFormat(claimTokenFormat);
        authorizationRequest.setPct(formParams.getFirst("pct"));

        String rpt = formParams.getFirst("rpt");

        if (rpt != null) {
            AccessToken accessToken = session.tokens().decode(rpt, AccessToken.class);
            if (accessToken == null) {
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, "invalid_rpt", "RPT signature is invalid", Status.FORBIDDEN);
            }

            authorizationRequest.setRpt(accessToken);
        }

        authorizationRequest.setScope(formParams.getFirst("scope"));
        String audienceParam = formParams.getFirst("audience");
        authorizationRequest.setAudience(audienceParam);
        authorizationRequest.setSubjectToken(accessTokenString);

        event.detail(Details.AUDIENCE, audienceParam);

        String submitRequest = formParams.getFirst("submit_request");

        authorizationRequest.setSubmitRequest(submitRequest == null ? true : Boolean.valueOf(submitRequest));

        // permissions have a format like RESOURCE#SCOPE1,SCOPE2
        List<String> permissions = formParams.get("permission");

        if (permissions != null) {
            event.detail(Details.PERMISSION, String.join("|", permissions));
            for (String permission : permissions) {
                String[] parts = permission.split("#");
                String resource = parts[0];

                if (parts.length == 1) {
                    authorizationRequest.addPermission(resource);
                } else {
                    String[] scopes = parts[1].split(",");
                    authorizationRequest.addPermission(parts[0], scopes);
                }
            }
        }

        Metadata metadata = new Metadata();

        String responseIncludeResourceName = formParams.getFirst("response_include_resource_name");

        if (responseIncludeResourceName != null) {
            metadata.setIncludeResourceName(Boolean.parseBoolean(responseIncludeResourceName));
        }

        String responsePermissionsLimit = formParams.getFirst("response_permissions_limit");

        if (responsePermissionsLimit != null) {
            metadata.setLimit(Integer.parseInt(responsePermissionsLimit));
        }

        metadata.setResponseMode(formParams.getFirst("response_mode"));

        authorizationRequest.setMetadata(metadata);

        Response authorizationResponse = AuthorizationTokenService.instance().authorize(authorizationRequest);

        event.success();

        return authorizationResponse;
    }

    public Response oauth2DeviceCodeToToken() {
        DeviceGrantType deviceGrantType = new DeviceGrantType(formParams, client, session, this, realm, event, cors);
        return deviceGrantType.oauth2DeviceFlow();
    }

    public Response cibaGrant() {
        CibaGrantType grantType = new CibaGrantType(formParams, client, session, this, realm, event, cors);
        return grantType.cibaGrant();
    }

    public static class TokenExchangeSamlProtocol extends SamlProtocol {

        final SamlClient samlClient;

        public TokenExchangeSamlProtocol(SamlClient samlClient) {
            this.samlClient = samlClient;
        }

        @Override
        protected Response buildAuthenticatedResponse(AuthenticatedClientSessionModel clientSession, String redirectUri,
                                                      Document samlDocument, JaxrsSAML2BindingBuilder bindingBuilder)
                throws ConfigurationException, ProcessingException, IOException {
            JaxrsSAML2BindingBuilder.PostBindingBuilder builder = bindingBuilder.postBinding(samlDocument);

            Element assertionElement;
            if (samlClient.requiresEncryption()) {
                assertionElement = DocumentUtil.getElement(builder.getDocument(), new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ENCRYPTED_ASSERTION.get()));
            } else {
                assertionElement = DocumentUtil.getElement(builder.getDocument(), new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()));
            }
            if (assertionElement == null) {
                return Response.status(Status.BAD_REQUEST).build();
            }
            String assertion = DocumentUtil.getNodeAsString(assertionElement);
            return Response.ok(assertion, MediaType.APPLICATION_XML_TYPE).build();
        }

        @Override
        protected Response buildErrorResponse(boolean isPostBinding, String destination, JaxrsSAML2BindingBuilder binding, Document document) throws ConfigurationException, ProcessingException, IOException {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }
}

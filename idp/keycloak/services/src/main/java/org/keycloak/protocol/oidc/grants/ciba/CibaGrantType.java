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
 *
 */

package org.keycloak.protocol.oidc.grants.ciba;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.protocol.oidc.grants.ciba.channel.CIBAAuthenticationRequest;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context.BackchannelTokenRequestContext;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.CibaRootEndpoint;
import org.keycloak.protocol.oidc.grants.ciba.util.UserTransactionStatus;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.utils.ProfileHelper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.keycloak.protocol.oidc.grants.ciba.util.GeneralHelper.updateResult;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CibaGrantType {

    private static final Logger logger = Logger.getLogger(CibaGrantType.class);

    public static final String IS_CONSENT_REQUIRED = "is_consent_required";
    public static final String LOGIN_HINT = "login_hint";
    public static final String LOGIN_HINT_TOKEN = "user";
    public static final String TRANSACTION_REQUEST = "request";
    public static final String BINDING_MESSAGE = "message";
    public static final String AUTH_REQ_ID = "auth_req_id";
    public static final String TOKEN_DELIVERY_MODE = "result_delivery_mode";
    public static final String TOKEN_ACCESS_DETAILS = "result_access_details";
    public static final String CLIENT_NOTIFICATION_TOKEN = "client_notification_token";
    public static final String REQUESTED_EXPIRY = "requested_expiry";
    public static final String USER_CODE = "user_code";


    public static final String REQUEST = OIDCLoginProtocol.REQUEST_PARAM;
    public static final String REQUEST_URI = OIDCLoginProtocol.REQUEST_URI_PARAM;
    /**
     * Prefix used to store additional params from the original authentication callback response into {@link AuthenticationSessionModel} note to be available later in Authenticators, RequiredActions etc. Prefix is used to
     * prevent collisions with internally used notes.
     *
     * @see AuthenticationSessionModel#getClientNote(String)
     */
    public static final String ADDITIONAL_CALLBACK_PARAMS_PREFIX = "ciba_callback_response_param_";
    /**
     * Prefix used to store additional params from the backchannel authentication request into {@link AuthenticationSessionModel} note to be available later in Authenticators, RequiredActions etc. Prefix is used to
     * prevent collisions with internally used notes.
     *
     * @see AuthenticationSessionModel#getClientNote(String)
     */
    public static final String ADDITIONAL_BACKCHANNEL_REQ_PARAMS_PREFIX = "ciba_backchannel_request_param_";

    public static UriBuilder authorizationUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = OIDCLoginProtocolService.tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "resolveBackofficeExtension").resolveTemplate("extension", CibaRootEndpoint.PROVIDER_ID, false).path(CibaRootEndpoint.class, "authorize");
    }

    public static UriBuilder authenticationUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = OIDCLoginProtocolService.tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "resolveBackofficeExtension").resolveTemplate("extension", CibaRootEndpoint.PROVIDER_ID, false).path(CibaRootEndpoint.class, "authenticate");
    }

    public static UriBuilder redirectUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = OIDCLoginProtocolService.tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "resolveBackofficeExtension").resolveTemplate("extension", CibaRootEndpoint.PROVIDER_ID, false).path(CibaRootEndpoint.class, "redirect");
    }

    private final MultivaluedMap<String, String> formParams;
    private final ClientModel client;
    private final KeycloakSession session;
    private final TokenEndpoint tokenEndpoint;
    private final RealmModel realm;
    private final EventBuilder event;
    private final Cors cors;

    public CibaGrantType(MultivaluedMap<String, String> formParams, ClientModel client, KeycloakSession session,
                         TokenEndpoint tokenEndpoint, RealmModel realm, EventBuilder event, Cors cors) {
        this.formParams = formParams;
        this.client = client;
        this.session = session;
        this.tokenEndpoint = tokenEndpoint;
        this.realm = realm;
        this.event = event;
        this.cors = cors;
    }

    public CibaGrantType(MultivaluedMap<String, String> formParams, ClientModel client, KeycloakSession session,
                         TokenEndpoint tokenEndpoint, RealmModel realm, EventBuilder event) {
        this(formParams, client, session, tokenEndpoint, realm, event, null);
    }

    public Response cibaGrant(String jwe) {
        ProfileHelper.requireFeature(Profile.Feature.CIBA);

        if (!realm.getCibaPolicy().isOIDCCIBAGrantEnabled(client)) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT,
                    "Client not allowed OIDC CIBA Grant", Response.Status.BAD_REQUEST);
        }

        if (jwe == null) {
            event.error(Errors.INVALID_CODE);
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(OAuthErrorException.INVALID_REQUEST, "Missing parameter: " + AUTH_REQ_ID);
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRep).build();
        }

        logger.tracev("CIBA Grant :: authReqId = {0}", jwe);

        CIBAAuthenticationRequest request;

        try {
            request = CIBAAuthenticationRequest.deserialize(session, jwe);
        } catch (Exception e) {
            logger.warnf("illegal format of auth_req_id : e.getMessage() = %s", e.getMessage());
            // Auth Req ID has not put onto cache, no need to remove Auth Req ID.
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(OAuthErrorException.INVALID_GRANT, "Invalid Auth Req ID");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRep).build();
        }

        request.setClient(client);
        try {
            session.clientPolicy().triggerOnEvent(new BackchannelTokenRequestContext(request, formParams));
        } catch (ClientPolicyException cpe) {
            event.error(cpe.getError());
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(OAuthErrorException.INVALID_GRANT, cpe.getErrorDetail());
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRep).build();
        }

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel deviceCode = store.getByDeviceCode(realm, request.getId());

        UserSessionModel userSession = createUserSession(request, deviceCode.getAdditionalParams(), deviceCode);
        UserModel user = userSession.getUser();

        if (deviceCode == null) {
            // Auth Req ID has not put onto cache, no need to remove Auth Req ID.
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(OAuthErrorException.INVALID_GRANT, "Invalid " + AUTH_REQ_ID);
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRep).build();
        }

        if (!request.getIssuedFor().equals(client.getClientId())) {
            logDebug("invalid client.", request);
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(OAuthErrorException.INVALID_GRANT, "unauthorized client");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRep).build();
            // the client sending this Auth Req ID does not match the client to which keycloak had issued Auth Req ID.
        }

        UserTransactionModel userTransactionDetails = getUserTransactionDetails(session, deviceCode, user);

        if (userTransactionDetails == null && session.getContext().getRealm().hasTrxValueAttribute(deviceCode.getClientNotificationToken(), "sp@web_to_device") && session.getContext().getRealm().hasTrxValueAttribute(deviceCode.getClientNotificationToken(), "ik@t1_request_id")) {
            userTransactionDetails = session.getContext().getRealm().getUserTransactionDetails(session.getContext().getRealm().getTrxValue(deviceCode.getClientNotificationToken(), "ik@t1_request_id"), user.getUsername());
        }

        if (deviceCode.isExpired()) {
            updateResult(session, realm, client, user, deviceCode.getClientNotificationToken());
            logDebug("expired.", request);
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(OAuthErrorException.EXPIRED_TOKEN, "authentication timed out");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRep).build();
        }

        if (!store.isPollingAllowed(deviceCode)) {
            logDebug("polling.", request);
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(OAuthErrorException.SLOW_DOWN, "too early to access");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRep).build();
        }

        if (deviceCode.isDenied()) {
            logDebug("denied.", request);
            if (session.getContext().getRealm().hasTrxValueAttribute(deviceCode.getClientNotificationToken(), "sp@error")) {
                session.getContext().getRealm().removeTrxAttribute(deviceCode.getClientNotificationToken(), "access_denied");
            } else {
                session.getContext().getRealm().addAttribute(deviceCode.getClientNotificationToken(), "sp@error", userTransactionDetails.getStatus().equals(UserTransactionStatus.USER_CANCELLED.getDisplayName()) ? "User cancelled the process." : "The Flow has been discontinued");
            }
        }

        if (deviceCode.isPending() && userTransactionDetails != null && userTransactionDetails.getStatus().equals(UserTransactionStatus.ACCESS_DENIED.getDisplayName())) {
            store.deny(realm, request.getAuthResultId());
            deviceCode = store.getByDeviceCode(realm, request.getId());
            if (deviceCode.isDenied()) {
                logDebug("denied.", request);
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "User is not authorized", Response.Status.BAD_REQUEST);
            }
        }

        boolean isFlowCompleted = true;
        String trxId = deviceCode.getClientNotificationToken();
        TrxModel trx = realm.getTrx(trxId);
        if (trx != null && trx.getAttributes() != null && !trx.getAttributes().containsKey("ik@t1_request_id")) {
            if (trx.getAttributes().containsKey("liveness_completed")) {
                isFlowCompleted = Boolean.parseBoolean(trx.getAttributes().get("liveness_completed"));
            }
            if (trx.getAttributes().containsKey("ik@video_kyc_completed")) {
                isFlowCompleted = isFlowCompleted && Boolean.parseBoolean(trx.getAttributes().get("ik@video_kyc_completed"));
            }
        }

        // get corresponding Authentication Channel Result entry
        if ((deviceCode.isPending() && !deviceCode.isDenied()) || !isFlowCompleted) {
            logDebug("not yet authenticated by Authentication Device or auth_req_id has already been used to get tokens.", request);
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(OAuthErrorException.VERIFICATION_PENDING, "The verification request is still pending as the end-user hasn't yet been verified.");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRep).build();
        }

        if (trx != null) {
//            trx.getAttributes().keySet().stream().filter(x -> x.startsWith("sp@validator_")).forEach(x -> realm.removeTrxAttribute(trxId, x));
            List<String> s = trx.getAttributes().keySet().stream().filter(x -> x.startsWith("sp@validator_")).collect(Collectors.toList());
            s.forEach(x -> realm.removeTrxAttribute(trxId, x));
        }

        store.removeDeviceCode(realm, request.getId());

        // Compute client scopes again from scope parameter. Check if user still has them granted
        // (but in code-to-token request, it could just theoretically happen that they are not available)
        String scopeParam = request.getScope();

        if (!TokenManager
                .verifyConsentStillAvailable(session,
                        user, client, TokenManager.getRequestedClientScopes(scopeParam, client))) {
            event.error(Errors.NOT_ALLOWED);
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(OAuthErrorException.INVALID_SCOPE, "Client no longer has requested consent from user");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRep).build();
        }

        ClientSessionContext clientSessionCtx = DefaultClientSessionContext
                .fromClientSessionAndScopeParameter(userSession.getAuthenticatedClientSessionByClient(client.getId()), scopeParam, session);

        int authTime = Time.currentTime();
        userSession.setNote(AuthenticationManager.AUTH_TIME, String.valueOf(authTime));
        userSession.setNote("requestId", deviceCode.getClientNotificationToken());
        return tokenEndpoint.createTokenResponse(session, client, user, userSession, clientSessionCtx, scopeParam, true);

    }

    public Response cibaGrant() {
        ProfileHelper.requireFeature(Profile.Feature.CIBA);

        if (!realm.getCibaPolicy().isOIDCCIBAGrantEnabled(client)) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT,
                    "Client not allowed OIDC CIBA Grant", Response.Status.BAD_REQUEST);
        }

        String jwe = formParams.getFirst(AUTH_REQ_ID);

        if (jwe == null) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Missing parameter: " + AUTH_REQ_ID, Response.Status.BAD_REQUEST);
        }

        logger.tracev("CIBA Grant :: authReqId = {0}", jwe);

        CIBAAuthenticationRequest request;

        try {
            request = CIBAAuthenticationRequest.deserialize(session, jwe);
        } catch (Exception e) {
            logger.warnf("illegal format of auth_req_id : e.getMessage() = %s", e.getMessage());
            // Auth Req ID has not put onto cache, no need to remove Auth Req ID.
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Invalid Auth Req ID", Response.Status.BAD_REQUEST);
        }

        request.setClient(client);
        try {
            session.clientPolicy().triggerOnEvent(new BackchannelTokenRequestContext(request, formParams));
        } catch (ClientPolicyException cpe) {
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel deviceCode = store.getByDeviceCode(realm, request.getId());

        if (deviceCode == null) {
            // Auth Req ID has not put onto cache, no need to remove Auth Req ID.
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Invalid " + AUTH_REQ_ID, Response.Status.BAD_REQUEST);
        }
        UserSessionModel userSession = createUserSession(request, deviceCode.getAdditionalParams(), deviceCode);
        UserModel user = userSession.getUser();


        if (!request.getIssuedFor().equals(client.getClientId())) {
            logDebug("invalid client.", request);
            // the client sending this Auth Req ID does not match the client to which keycloak had issued Auth Req ID.
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "unauthorized client", Response.Status.BAD_REQUEST);
        }

        UserTransactionModel userTransactionDetails = getUserTransactionDetails(session, deviceCode, user);

        if (deviceCode.isExpired()) {
            updateResult(session, realm, client, user, deviceCode.getClientNotificationToken());
            if (userTransactionDetails != null && !userTransactionDetails.getStatus().equals(UserTransactionStatus.USER_CANCELLED.getDisplayName())) {
                realm.updateTransactionStatus(deviceCode.getClientNotificationToken(), user.getUsername(), UserTransactionStatus.EXPIRED.getDisplayName());
            }
            logDebug("expired.", request);
            throw new CorsErrorResponseException(cors, OAuthErrorException.EXPIRED_TOKEN, "authentication timed out", Response.Status.BAD_REQUEST);
        }

        if (!store.isPollingAllowed(deviceCode)) {
            logDebug("polling.", request);
            throw new CorsErrorResponseException(cors, OAuthErrorException.SLOW_DOWN, "too early to access", Response.Status.BAD_REQUEST);
        }

        if (deviceCode.isDenied()) {
            logDebug("denied.", request);
            if (session.getContext().getRealm().hasTrxValueAttribute(deviceCode.getClientNotificationToken(), "sp@error")) {
                session.getContext().getRealm().removeTrxAttribute(deviceCode.getClientNotificationToken(), "access_denied");
            } else {
                session.getContext().getRealm().addAttribute(deviceCode.getClientNotificationToken(), "sp@error", userTransactionDetails.getStatus().equals(UserTransactionStatus.USER_CANCELLED.getDisplayName()) ? "User cancelled the process." : "The Flow has been discontinued");
            }
        }


        if (deviceCode.isPending() && userTransactionDetails != null && userTransactionDetails.getStatus().equals(UserTransactionStatus.ACCESS_DENIED.getDisplayName())) {
            store.deny(realm, request.getAuthResultId());
            deviceCode = store.getByDeviceCode(realm, request.getId());
            if (deviceCode.isDenied()) {
                logDebug("denied.", request);
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "User is not authorized", Response.Status.BAD_REQUEST);
            }
        }

        boolean isFlowCompleted = true;
        String trxId = deviceCode.getClientNotificationToken();
        TrxModel trx = realm.getTrx(trxId);

        if (trx != null && trx.getAttributes() != null && !trx.getAttributes().containsKey("ik@t1_request_id")) {
            if (trx.getAttributes().containsKey("liveness_completed")) {
                isFlowCompleted = Boolean.parseBoolean(trx.getAttributes().get("liveness_completed"));
            }
            if (trx.getAttributes().containsKey("ik@video_kyc_completed")) {
                isFlowCompleted = isFlowCompleted && Boolean.parseBoolean(trx.getAttributes().get("ik@video_kyc_completed"));
            }
        }
        // get corresponding Authentication Channel Result entry
        if ((deviceCode.isPending() && !deviceCode.isDenied()) || !isFlowCompleted) {
            logDebug("not yet authenticated by Authentication Device or auth_req_id has already been used to get tokens.", request);
            throw new CorsErrorResponseException(cors, OAuthErrorException.VERIFICATION_PENDING, "The verification request is still pending as the end-user hasn't yet been verified.", Response.Status.BAD_REQUEST);
        }

        List<String> s = trx.getAttributes().keySet().stream().filter(x -> x.startsWith("sp@validator_")).collect(Collectors.toList());
        s.forEach(x -> realm.removeTrxAttribute(trxId, x));

        store.removeDeviceCode(realm, request.getId());

        // Compute client scopes again from scope parameter. Check if user still has them granted
        // (but in code-to-token request, it could just theoretically happen that they are not available)
        String scopeParam = request.getScope();

        if (!TokenManager
                .verifyConsentStillAvailable(session,
                        user, client, TokenManager.getRequestedClientScopes(scopeParam, client))) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, "Client no longer has requested consent from user", Response.Status.BAD_REQUEST);
        }

        ClientSessionContext clientSessionCtx = DefaultClientSessionContext
                .fromClientSessionAndScopeParameter(userSession.getAuthenticatedClientSessionByClient(client.getId()), scopeParam, session);

        int authTime = Time.currentTime();
        userSession.setNote(AuthenticationManager.AUTH_TIME, String.valueOf(authTime));
        userSession.setNote("requestId", deviceCode.getClientNotificationToken());
        return tokenEndpoint.createTokenResponse(session, client, cors, user, userSession, clientSessionCtx, scopeParam, true);

    }

    private UserSessionModel createUserSession(CIBAAuthenticationRequest request, Map<String, String> additionalParams, OAuth2DeviceCodeModel deviceCode) {
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm);
        // here Client Model of CD(Consumption Device) needs to be used to bind its Client Session with User Session.
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setAction(AuthenticatedClientSessionModel.Action.AUTHENTICATE.name());
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, request.getScope());
        if (additionalParams != null) {
            for (String paramName : additionalParams.keySet()) {
                authSession.setClientNote(ADDITIONAL_CALLBACK_PARAMS_PREFIX + paramName, (String) additionalParams.get(paramName));
            }
        }
        if (request.getOtherClaims() != null) {
            for (String paramName : request.getOtherClaims().keySet()) {
                authSession.setClientNote(ADDITIONAL_BACKCHANNEL_REQ_PARAMS_PREFIX + paramName, request.getOtherClaims().get(paramName).toString());
            }
        }

        UserModel user = session.users().getUserById(realm, request.getSubject());
        if (user != null) {
            authSession.setAuthenticatedUser(user);
        }

        if (user != null && !user.isEnabled()) {
            logger.error(Errors.USER_DISABLED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User disabled", Response.Status.BAD_REQUEST);
        }

        logger.debugf("CIBA Grant :: user model found. user.getId() = %s, user.getEmail() = %s, user.getUsername() = %s.", user.getId(), user.getEmail(), user.getUsername());

        AuthenticationManager.setClientScopesInSession(authSession);

        ClientSessionContext context = AuthenticationProcessor
                .attachSession(authSession, null, session, realm, session.getContext().getConnection(), event);
        UserSessionModel userSession = context.getClientSession().getUserSession();

        if (userSession == null) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "User session is not found", Response.Status.BAD_REQUEST);
        }

        // authorization (consent)
        UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());
        if (grantedConsent == null) {
            grantedConsent = new UserConsentModel(client);
            session.users().addConsent(realm, user.getId(), grantedConsent);
            if (logger.isTraceEnabled()) {
                grantedConsent.getGrantedClientScopes().forEach(i -> logger.tracef("CIBA Grant :: Consent granted. %s", i.getName()));
            }
        }

        boolean updateConsentRequired = false;

        for (String clientScopeId : authSession.getClientScopes()) {
            ClientScopeModel clientScope = KeycloakModelUtils.findClientScopeById(realm, client, clientScopeId);
            if (clientScope != null && !grantedConsent.isClientScopeGranted(clientScope) && clientScope.isDisplayOnConsentScreen()) {
                grantedConsent.addGrantedClientScope(clientScope);
                updateConsentRequired = true;
            }
        }

        if (updateConsentRequired) {
            session.users().updateConsent(realm, user.getId(), grantedConsent);
            if (logger.isTraceEnabled()) {
                grantedConsent.getGrantedClientScopes().forEach(i -> logger.tracef("CIBA Grant :: Consent updated. %s", i.getName()));
            }
        }

        event.detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);
        event.detail(Details.CODE_ID, userSession.getId());
        event.session(userSession.getId());
        event.user(user);
        logger.debugf("Successfully verified Authe Req Id '%s'. User session: '%s', client: '%s'", request, userSession.getId(), client.getId());

        return userSession;
    }

    private static void logDebug(String message, CIBAAuthenticationRequest request) {
        logger.infof("CIBA Grant :: authentication channel %s clientId = %s, authResultId = %s", message, request.getIssuedFor(), request.getAuthResultId());
    }

    /**
     * Retrieves the user transaction details based on the provided session, device code, and user.
     * This method first attempts to fetch the transaction details using the device code.
     * If unsuccessful, it checks for specific attributes and tries to fetch details using an alternative request ID (t1_request_id).
     *
     * @param session    The KeycloakSession object containing the current session information.
     * @param deviceCode The OAuth2DeviceCodeModel object containing the device code information.
     * @param user       The UserModel object representing the user for whom transaction details are being retrieved.
     * @return A UserTransactionModel object containing the user's transaction details, or null if no details are found.
     */
    private static UserTransactionModel getUserTransactionDetails(KeycloakSession session, OAuth2DeviceCodeModel deviceCode, UserModel user) {
        UserTransactionModel userTransactionModel = session
                .getContext()
                .getRealm()
                .getUserTransactionDetails(deviceCode.getClientNotificationToken(), user.getUsername());

        if (userTransactionModel == null
                && session.getContext().getRealm().hasTrxValueAttribute(deviceCode.getClientNotificationToken(), "sp@web_to_device")
                && session.getContext().getRealm().hasTrxValueAttribute(deviceCode.getClientNotificationToken(), "ik@t1_request_id")) {
            userTransactionModel = session
                    .getContext()
                    .getRealm()
                    .getUserTransactionDetails(session.getContext().getRealm().getTrxValue(deviceCode.getClientNotificationToken(), "ik@t1_request_id"), user.getUsername());
        }

        return userTransactionModel;
    }

}

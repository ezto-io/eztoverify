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
package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jetbrains.annotations.Nullable;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.TokenVerifier.TokenTypeCheck;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.ConsoleDisplayMode;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionContextResult;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.locale.LocaleType;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.oidc.BackchannelLogoutResponse;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.LogoutRequestContext;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.AuthorizationContextUtil;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.services.util.P3PHelper;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.common.util.ServerCookie.SameSiteAttributeValue;
import static org.keycloak.models.Constants.*;
import static org.keycloak.models.UserSessionModel.CORRESPONDING_SESSION_ID;
import static org.keycloak.protocol.oidc.grants.device.DeviceGrantType.isOAuth2DeviceVerificationFlow;
import static org.keycloak.services.util.CookieHelper.getCookie;

/**
 * Stateless object that manages authentication
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationManager {
    public static final String SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS = "SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS";
    public static final String END_AFTER_REQUIRED_ACTIONS = "END_AFTER_REQUIRED_ACTIONS";
    public static final String INVALIDATE_ACTION_TOKEN = "INVALIDATE_ACTION_TOKEN";

    /**
     * Auth session note, which indicates if user session will be persistent (Saved to real persistent store) or
     * transient (transient session will be scoped to single request and hence there is no need to save it in the underlying store)
     */
    public static final String USER_SESSION_PERSISTENT_STATE = "USER_SESSION_PERSISTENT_STATE";

    /**
     * Auth session note on client logout state (when logging out)
     */
    public static final String CLIENT_LOGOUT_STATE = "logout.state.";

    // userSession note with authTime (time when authentication flow including requiredActions was finished)
    public static final String AUTH_TIME = "AUTH_TIME";
    // clientSession note with flag that clientSession was authenticated through SSO cookie
    public static final String SSO_AUTH = "SSO_AUTH";

    // authSession note with flag that is true if user is forced to re-authenticate by client (EG. in case of OIDC client by sending "prompt=login")
    public static final String FORCED_REAUTHENTICATION = "FORCED_REAUTHENTICATION";

    protected static final Logger logger = Logger.getLogger(AuthenticationManager.class);

    public static final String FORM_USERNAME = "username";
    // used for auth login
    public static final String KEYCLOAK_IDENTITY_COOKIE = "KEYCLOAK_IDENTITY";
    // used solely to determine is user is logged in
    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";
    public static final String KEYCLOAK_REMEMBER_ME = "KEYCLOAK_REMEMBER_ME";

    // ** Logout related notes **/
    // Flag in the logout session to specify if we use "system" client or real client
    public static final String LOGOUT_WITH_SYSTEM_CLIENT = "LOGOUT_WITH_SYSTEM_CLIENT";
    // Protocol of the client, which initiated logout
    public static final String KEYCLOAK_LOGOUT_PROTOCOL = "KEYCLOAK_LOGOUT_PROTOCOL";
    // Filled in case that logout was triggered with "initiating idp"
    public static final String LOGOUT_INITIATING_IDP = "LOGOUT_INITIATING_IDP";

    // Parameter of LogoutEndpoint
    public static final String INITIATING_IDP_PARAM = "initiating_idp";

    private static final TokenTypeCheck VALIDATE_IDENTITY_COOKIE = new TokenTypeCheck(TokenUtil.TOKEN_TYPE_KEYCLOAK_ID);

    public static boolean isSessionValid(RealmModel realm, UserSessionModel userSession) {
        if (userSession == null) {
            logger.debug("No user session");
            return false;
        }
        int currentTime = Time.currentTime();

        // Additional time window is added for the case when session was updated in different DC and the update to current DC was postponed
        int maxIdle = userSession.isRememberMe() && realm.getSsoSessionIdleTimeoutRememberMe() > 0 ?
                realm.getSsoSessionIdleTimeoutRememberMe() : realm.getSsoSessionIdleTimeout();
        int maxLifespan = userSession.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0 ?
                realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan();

        boolean sessionIdleOk = maxIdle > currentTime - userSession.getLastSessionRefresh() - SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS;
        boolean sessionMaxOk = maxLifespan > currentTime - userSession.getStarted();
        return sessionIdleOk && sessionMaxOk;
    }

    public static boolean isOfflineSessionValid(RealmModel realm, UserSessionModel userSession) {
        if (userSession == null) {
            logger.debug("No offline user session");
            return false;
        }
        int currentTime = Time.currentTime();
        // Additional time window is added for the case when session was updated in different DC and the update to current DC was postponed
        int maxIdle = realm.getOfflineSessionIdleTimeout() + SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS;

        // KEYCLOAK-7688 Offline Session Max for Offline Token
        if (realm.isOfflineSessionMaxLifespanEnabled()) {
            int max = userSession.getStarted() + realm.getOfflineSessionMaxLifespan();
            return userSession.getLastSessionRefresh() + maxIdle > currentTime && max > currentTime;
        } else {
            return userSession.getLastSessionRefresh() + maxIdle > currentTime;
        }
    }

    public static boolean expireUserSessionCookie(KeycloakSession session, UserSessionModel userSession, RealmModel realm, UriInfo uriInfo, HttpHeaders headers, ClientConnection connection) {
        try {
            // check to see if any identity cookie is set with the same session and expire it if necessary
            Cookie cookie = CookieHelper.getCookie(headers.getCookies(), KEYCLOAK_IDENTITY_COOKIE);
            if (cookie == null) return true;
            String tokenString = cookie.getValue();

            TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenString, AccessToken.class)
                    .realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()))
                    .checkActive(false)
                    .checkTokenType(false)
                    .withChecks(VALIDATE_IDENTITY_COOKIE);

            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();

            SignatureVerifierContext signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
            verifier.verifierContext(signatureVerifier);

            AccessToken token = verifier.verify().getToken();
            UserSessionModel cookieSession = session.sessions().getUserSession(realm, token.getSessionState());
            if (cookieSession == null || !cookieSession.getId().equals(userSession.getId())) return true;
            expireIdentityCookie(realm, uriInfo, connection);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public static void backchannelLogout(KeycloakSession session, UserSessionModel userSession, boolean logoutBroker) {
        backchannelLogout(
                session,
                session.getContext().getRealm(),
                userSession,
                session.getContext().getUri(),
                session.getContext().getConnection(),
                session.getContext().getRequestHeaders(),
                logoutBroker
        );
    }

    public static BackchannelLogoutResponse backchannelLogout(KeycloakSession session, RealmModel realm,
                                                              UserSessionModel userSession, UriInfo uriInfo,
                                                              ClientConnection connection, HttpHeaders headers,
                                                              boolean logoutBroker) {
        return backchannelLogout(session, realm, userSession, uriInfo, connection, headers, logoutBroker, false);
    }

    /**
     * @param session
     * @param realm
     * @param userSession
     * @param uriInfo
     * @param connection
     * @param headers
     * @param logoutBroker
     * @param offlineSession
     * @return BackchannelLogoutResponse with logout information
     */
    public static BackchannelLogoutResponse backchannelLogout(KeycloakSession session, RealmModel realm,
                                                              UserSessionModel userSession, UriInfo uriInfo,
                                                              ClientConnection connection, HttpHeaders headers,
                                                              boolean logoutBroker,
                                                              boolean offlineSession) {
        BackchannelLogoutResponse backchannelLogoutResponse = new BackchannelLogoutResponse();

        if (userSession == null) {
            backchannelLogoutResponse.setLocalLogoutSucceeded(true);
            return backchannelLogoutResponse;
        }
        UserModel user = userSession.getUser();
        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }

        logger.debugv("Logging out: {0} ({1}) offline: {2}", user.getUsername(), userSession.getId(),
                userSession.isOffline());
        boolean expireUserSessionCookieSucceeded =
                expireUserSessionCookie(session, userSession, realm, uriInfo, headers, connection);

        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession =
                createOrJoinLogoutSession(session, realm, asm, userSession, false);

        boolean userSessionOnlyHasLoggedOutClients = false;
        try {
            backchannelLogoutResponse = backchannelLogoutAll(session, realm, userSession, logoutAuthSession, uriInfo,
                    headers, logoutBroker);
            userSessionOnlyHasLoggedOutClients =
                    checkUserSessionOnlyHasLoggedOutClients(realm, userSession, logoutAuthSession);
        } finally {
            logger.tracef("Removing logout session '%s' after backchannel logout", logoutAuthSession.getParentSession().getId());
            RootAuthenticationSessionModel rootAuthSession = logoutAuthSession.getParentSession();
            rootAuthSession.removeAuthenticationSessionByTabId(logoutAuthSession.getTabId());
        }

        userSession.setState(UserSessionModel.State.LOGGED_OUT);

        if (offlineSession) {
            new UserSessionManager(session).revokeOfflineUserSession(userSession);

            // Check if "online" session still exists and remove it too
            String onlineUserSessionId = userSession.getNote(CORRESPONDING_SESSION_ID);
            UserSessionModel onlineUserSession = (onlineUserSessionId != null) ?
                    session.sessions().getUserSession(realm, onlineUserSessionId) :
                    session.sessions().getUserSession(realm, userSession.getId());

            if (onlineUserSession != null) {
                session.sessions().removeUserSession(realm, onlineUserSession);
            }
        } else {
            session.sessions().removeUserSession(realm, userSession);
        }
        backchannelLogoutResponse
                .setLocalLogoutSucceeded(expireUserSessionCookieSucceeded && userSessionOnlyHasLoggedOutClients);
        return backchannelLogoutResponse;
    }

    public static AuthenticationSessionModel createOrJoinLogoutSession(KeycloakSession session, RealmModel realm, final AuthenticationSessionManager asm, UserSessionModel userSession, boolean browserCookie) {
        AuthenticationSessionModel logoutSession = session.getContext().getAuthenticationSession();
        if (logoutSession != null && AuthenticationSessionModel.Action.LOGGING_OUT.name().equals(logoutSession.getAction())) {
            return logoutSession;
        }

        ClientModel client = session.getContext().getRealm().getClientByClientId(realm.getMode().equals("wiam") ? Constants.SSO_MANAGEMENT_CLIENT_ID : ADMIN_CONSOLE_CLIENT_ID);
        String authSessionId;
        RootAuthenticationSessionModel rootLogoutSession = null;
        boolean browserCookiePresent = false;

        // Try to lookup current authSessionId from browser cookie. If doesn't exist, use the same as current userSession
        if (browserCookie) {
            rootLogoutSession = asm.getCurrentRootAuthenticationSession(realm);
        }
        if (rootLogoutSession != null) {
            authSessionId = rootLogoutSession.getId();
            browserCookiePresent = true;
        } else if (userSession != null) {
            authSessionId = userSession.getId();
            rootLogoutSession = session.authenticationSessions().getRootAuthenticationSession(realm, authSessionId);
        } else {
            authSessionId = KeycloakModelUtils.generateId();
        }

        if (rootLogoutSession == null) {
            rootLogoutSession = session.authenticationSessions().createRootAuthenticationSession(realm, authSessionId);
        }
        if (browserCookie && !browserCookiePresent) {
            // Update cookie if needed
            asm.setAuthSessionCookie(authSessionId, realm);
        }

        // See if we have logoutAuthSession inside current rootSession. Create new if not
        Optional<AuthenticationSessionModel> found = rootLogoutSession.getAuthenticationSessions().values().stream()
                .filter(authSession -> AuthenticationSessionModel.Action.LOGGING_OUT.name().equals(authSession.getAction()))
                .findFirst();

        AuthenticationSessionModel logoutAuthSession;
        if (found.isPresent()) {
            logoutAuthSession = found.get();
            logger.tracef("Found existing logout session for client '%s'. Authentication session id: %s", client.getClientId(), rootLogoutSession.getId());
        } else {
            logoutAuthSession = rootLogoutSession.createAuthenticationSession(client);
            logoutAuthSession.setAction(AuthenticationSessionModel.Action.LOGGING_OUT.name());
            session.getContext().setClient(client);
            logger.tracef("Creating logout session for client '%s'. Authentication session id: %s", client.getClientId(), rootLogoutSession.getId());
        }
        session.getContext().setAuthenticationSession(logoutAuthSession);

        logoutAuthSession.setAction(AuthenticationSessionModel.Action.LOGGING_OUT.name());
        return logoutAuthSession;
    }

    private static BackchannelLogoutResponse backchannelLogoutAll(KeycloakSession session, RealmModel realm,
                                                                  UserSessionModel userSession, AuthenticationSessionModel logoutAuthSession, UriInfo uriInfo,
                                                                  HttpHeaders headers, boolean logoutBroker) {
        BackchannelLogoutResponse backchannelLogoutResponse = new BackchannelLogoutResponse();

        for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
            Response clientSessionLogoutResponse =
                    backchannelLogoutClientSession(session, realm, clientSession, logoutAuthSession, uriInfo, headers);

            String backchannelLogoutUrl =
                    OIDCAdvancedConfigWrapper.fromClientModel(clientSession.getClient()).getBackchannelLogoutUrl();

            BackchannelLogoutResponse.DownStreamBackchannelLogoutResponse downStreamBackchannelLogoutResponse =
                    new BackchannelLogoutResponse.DownStreamBackchannelLogoutResponse();
            downStreamBackchannelLogoutResponse.setWithBackchannelLogoutUrl(backchannelLogoutUrl != null);

            if (clientSessionLogoutResponse != null) {
                downStreamBackchannelLogoutResponse.setResponseCode(clientSessionLogoutResponse.getStatus());
            } else {
                downStreamBackchannelLogoutResponse.setResponseCode(null);
            }
            backchannelLogoutResponse.addClientResponses(downStreamBackchannelLogoutResponse);
        }
        if (logoutBroker) {
            String brokerId = userSession.getNote(Details.IDENTITY_PROVIDER);
            if (brokerId != null) {
                IdentityProvider identityProvider = IdentityBrokerService.getIdentityProvider(session, realm, brokerId);
                try {
                    identityProvider.backchannelLogout(session, userSession, uriInfo, realm);
                } catch (Exception e) {
                    logger.warn("Exception at broker backchannel logout for broker " + brokerId, e);
                    backchannelLogoutResponse.setLocalLogoutSucceeded(false);
                }
            }
        }

        return backchannelLogoutResponse;
    }

    /**
     * Checks that all sessions have been removed from the user session. The list of logged out clients is determined from
     * the {@code logoutAuthSession} auth session notes.
     *
     * @param realm
     * @param userSession
     * @param logoutAuthSession
     * @return {@code true} when all clients have been logged out, {@code false} otherwise
     */
    private static boolean checkUserSessionOnlyHasLoggedOutClients(RealmModel realm,
                                                                   UserSessionModel userSession, AuthenticationSessionModel logoutAuthSession) {
        final Map<String, AuthenticatedClientSessionModel> acs = userSession.getAuthenticatedClientSessions();
        Set<AuthenticatedClientSessionModel> notLoggedOutSessions = acs.entrySet().stream()
                .filter(me -> !Objects.equals(AuthenticationSessionModel.Action.LOGGED_OUT, getClientLogoutAction(logoutAuthSession, me.getKey())))
                .filter(me -> !Objects.equals(AuthenticationSessionModel.Action.LOGGED_OUT.name(), me.getValue().getAction()))
                .filter(me -> !Objects.equals(AuthenticationSessionModel.Action.LOGGING_OUT.name(), me.getValue().getAction()))
                .filter(me -> Objects.nonNull(me.getValue().getProtocol()))   // Keycloak service-like accounts
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        boolean allClientsLoggedOut = notLoggedOutSessions.isEmpty();

        if (!allClientsLoggedOut) {
            logger.warnf("Some clients have been not been logged out for user %s in %s realm: %s",
                    userSession.getUser().getUsername(), realm.getName(),
                    notLoggedOutSessions.stream()
                            .map(AuthenticatedClientSessionModel::getClient)
                            .map(ClientModel::getClientId)
                            .sorted()
                            .collect(Collectors.joining(", "))
            );
        } else if (logger.isDebugEnabled()) {
            logger.debugf("All clients have been logged out for user %s in %s realm, session %s",
                    userSession.getUser().getUsername(), realm.getName(), userSession.getId());
        }

        return allClientsLoggedOut;
    }

    /**
     * Logs out the given client session and records the result into {@code logoutAuthSession} if set.
     *
     * @param session
     * @param realm
     * @param clientSession
     * @param logoutAuthSession auth session used for recording result of logout. May be {@code null}
     * @param uriInfo
     * @param headers
     * @return {@code http status OK} if the client was or is already being logged out, {@code null} if it is
     * not known how to log it out and no request is made, otherwise the response of the logout request.
     */
    private static Response backchannelLogoutClientSession(KeycloakSession session, RealmModel realm,
                                                           AuthenticatedClientSessionModel clientSession, AuthenticationSessionModel logoutAuthSession,
                                                           UriInfo uriInfo, HttpHeaders headers) {
        UserSessionModel userSession = clientSession.getUserSession();
        ClientModel client = clientSession.getClient();

        if (client.isFrontchannelLogout()
                || AuthenticationSessionModel.Action.LOGGED_OUT.name().equals(clientSession.getAction())) {
            return null;
        }

        final AuthenticationSessionModel.Action logoutState = getClientLogoutAction(logoutAuthSession, client.getId());

        if (logoutState == AuthenticationSessionModel.Action.LOGGED_OUT
                || logoutState == AuthenticationSessionModel.Action.LOGGING_OUT) {
            return Response.ok().build();
        }

        if (!client.isEnabled()) {
            return null;
        }

        try {
            setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGING_OUT);

            String authMethod = clientSession.getProtocol();
            if (authMethod == null) return Response.ok().build(); // must be a keycloak service like account

            logger.debugv("backchannel logout to: {0}", client.getClientId());
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, authMethod);
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo);

            Response clientSessionLogout = protocol.backchannelLogout(userSession, clientSession);

            setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGED_OUT);

            return clientSessionLogout;
        } catch (Exception ex) {
            ServicesLogger.LOGGER.failedToLogoutClient(ex);
            return Response.serverError().build();
        }
    }

    private static Response frontchannelLogoutClientSession(KeycloakSession session, RealmModel realm,
                                                            AuthenticatedClientSessionModel clientSession, AuthenticationSessionModel logoutAuthSession,
                                                            UriInfo uriInfo, HttpHeaders headers) {
        UserSessionModel userSession = clientSession.getUserSession();
        ClientModel client = clientSession.getClient();

        if (!client.isFrontchannelLogout() || AuthenticationSessionModel.Action.LOGGED_OUT.name().equals(clientSession.getAction())) {
            return null;
        }

        final AuthenticationSessionModel.Action logoutState = getClientLogoutAction(logoutAuthSession, client.getId());

        if (logoutState == AuthenticationSessionModel.Action.LOGGED_OUT || logoutState == AuthenticationSessionModel.Action.LOGGING_OUT) {
            return null;
        }

        try {
            session.clientPolicy().triggerOnEvent(new LogoutRequestContext());
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        try {
            setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGING_OUT);

            String authMethod = clientSession.getProtocol();
            if (authMethod == null) return null; // must be a keycloak service like account

            logger.debugv("frontchannel logout to: {0}", client.getClientId());
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, authMethod);
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo);

            Response response = protocol.frontchannelLogout(userSession, clientSession);
            if (response != null) {
                logger.debug("returning frontchannel logout request to client");
                // setting this to logged out cuz I'm not sure protocols can always verify that the client was logged out or not

                if (!AuthenticationSessionModel.Action.LOGGING_OUT.name().equals(clientSession.getAction())) {
                    setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGED_OUT);
                }

                return response;
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToLogoutClient(e);
        }

        return null;
    }

    /**
     * Sets logout state of the particular client into the {@code logoutAuthSession}
     *
     * @param logoutAuthSession logoutAuthSession. May be {@code null} in which case this is a no-op.
     * @param clientUuid        Client. Must not be {@code null}
     * @param action
     */
    public static void setClientLogoutAction(AuthenticationSessionModel logoutAuthSession, String clientUuid, AuthenticationSessionModel.Action action) {
        if (logoutAuthSession != null && clientUuid != null) {
            logoutAuthSession.setAuthNote(CLIENT_LOGOUT_STATE + clientUuid, action.name());
        }
    }

    /**
     * Returns the logout state of the particular client as per the {@code logoutAuthSession}
     *
     * @param logoutAuthSession logoutAuthSession. May be {@code null} in which case this is a no-op.
     * @param clientUuid        Internal ID of the client. Must not be {@code null}
     * @return State if it can be determined, {@code null} otherwise.
     */
    public static AuthenticationSessionModel.Action getClientLogoutAction(AuthenticationSessionModel logoutAuthSession, String clientUuid) {
        if (logoutAuthSession == null || clientUuid == null) {
            return null;
        }

        String state = logoutAuthSession.getAuthNote(CLIENT_LOGOUT_STATE + clientUuid);
        return state == null ? null : AuthenticationSessionModel.Action.valueOf(state);
    }

    /**
     * Logout all clientSessions of this user and client
     *
     * @param session
     * @param realm
     * @param user
     * @param client
     * @param uriInfo
     * @param headers
     */
    public static void backchannelLogoutUserFromClient(KeycloakSession session, RealmModel realm, UserModel user, ClientModel client, UriInfo uriInfo, HttpHeaders headers) {
        session.sessions().getUserSessionsStream(realm, user)
                .map(userSession -> userSession.getAuthenticatedClientSessionByClient(client.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()) // collect to avoid concurrent modification.
                .forEach(clientSession -> {
                    backchannelLogoutClientSession(session, realm, clientSession, null, uriInfo, headers);
                    clientSession.setAction(AuthenticationSessionModel.Action.LOGGED_OUT.name());
                    TokenManager.dettachClientSession(clientSession);
                });
    }

    public static Response browserLogout(KeycloakSession session,
                                         RealmModel realm,
                                         UserSessionModel userSession,
                                         UriInfo uriInfo,
                                         ClientConnection connection,
                                         HttpHeaders headers, EventBuilder event) {
        if (userSession == null) return null;

        if (logger.isDebugEnabled()) {
            UserModel user = userSession.getUser();
            logger.debugv("Logging out: {0} ({1})", user.getUsername(), userSession.getId());
        }

        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }

        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(session, realm, asm, userSession, true);

        Response response = browserLogoutAllClients(userSession, session, realm, headers, uriInfo, logoutAuthSession);
        if (response != null) {
            return response;
        }

        String brokerId = userSession.getNote(Details.IDENTITY_PROVIDER);
        String initiatingIdp = logoutAuthSession.getAuthNote(AuthenticationManager.LOGOUT_INITIATING_IDP);
        if (brokerId != null && !brokerId.equals(initiatingIdp)) {
            IdentityProvider identityProvider = IdentityBrokerService.getIdentityProvider(session, realm, brokerId);
            response = identityProvider.keycloakInitiatedBrowserLogout(session, userSession, uriInfo, realm);
            if (response != null) {
                return response;
            }
        }

        return finishBrowserLogout(session, realm, userSession, uriInfo, connection, headers, event);
    }

    private static Response browserLogoutAllClients(UserSessionModel userSession, KeycloakSession session, RealmModel realm, HttpHeaders headers, UriInfo uriInfo, AuthenticationSessionModel logoutAuthSession) {
        Map<Boolean, List<AuthenticatedClientSessionModel>> acss = userSession.getAuthenticatedClientSessions().values().stream()
                .filter(clientSession -> !Objects.equals(AuthenticationSessionModel.Action.LOGGED_OUT.name(), clientSession.getAction()))
                .filter(clientSession -> clientSession.getProtocol() != null)
                .collect(Collectors.partitioningBy(clientSession -> clientSession.getClient().isFrontchannelLogout()));

        final List<AuthenticatedClientSessionModel> backendLogoutSessions = acss.get(false) == null ? Collections.emptyList() : acss.get(false);
        backendLogoutSessions.forEach(acs -> backchannelLogoutClientSession(session, realm, acs, logoutAuthSession, uriInfo, headers));

        final List<AuthenticatedClientSessionModel> redirectClients = acss.get(true) == null ? Collections.emptyList() : acss.get(true);
        for (AuthenticatedClientSessionModel nextRedirectClient : redirectClients) {
            Response response = frontchannelLogoutClientSession(session, realm, nextRedirectClient, logoutAuthSession, uriInfo, headers);
            if (response != null) {
                return response;
            }
        }

        return null;
    }

    public static Response finishBrowserLogout(KeycloakSession session, RealmModel realm,
                                               UserSessionModel userSession, UriInfo uriInfo,
                                               ClientConnection connection, HttpHeaders headers, EventBuilder event) {
        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(session, realm, asm, userSession, true);

        checkUserSessionOnlyHasLoggedOutClients(realm, userSession, logoutAuthSession);

        // For resolving artifact we don't need any cookie, all details are stored in session storage so we can remove
        expireIdentityCookie(realm, uriInfo, connection);
        expireRememberMeCookie(realm, uriInfo, connection);

        String method = userSession.getNote(KEYCLOAK_LOGOUT_PROTOCOL);
        LoginProtocol protocol = session.getProvider(LoginProtocol.class, method);
        protocol.setRealm(realm)
                .setHttpHeaders(headers)
                .setUriInfo(uriInfo)
                .setEventBuilder(event);


        Response response = protocol.finishBrowserLogout(userSession, logoutAuthSession);

        // It may be possible that there are some client sessions that are still in LOGGING_OUT state
        long numberOfUnconfirmedSessions = userSession.getAuthenticatedClientSessions().values().stream()
                .filter(clientSessionModel -> CommonClientSessionModel.Action.LOGGING_OUT.name().equals(clientSessionModel.getAction()))
                .count();

        // If logout flow end up correctly there should be at maximum 1 client session in LOGGING_OUT action, if there are more, something went wrong
        if (numberOfUnconfirmedSessions > 1) {
            logger.warnf("There are more than one clientSession in logging_out state. Perhaps some client did not finish logout flow correctly.");
        }

        // By setting LOGGED_OUT_UNCONFIRMED state we are saying that anybody who will turn the last client session into
        // LOGGED_OUT action can remove UserSession
        if (numberOfUnconfirmedSessions >= 1) {
            userSession.setState(UserSessionModel.State.LOGGED_OUT_UNCONFIRMED);
        } else {
            userSession.setState(UserSessionModel.State.LOGGED_OUT);
        }

        // Do not remove user session, it will be removed when last clientSession will be logged out
        if (numberOfUnconfirmedSessions < 1) {
            session.sessions().removeUserSession(realm, userSession);
        }

        logger.tracef("Removing logout session '%s'.", logoutAuthSession.getParentSession().getId());
        session.authenticationSessions().removeRootAuthenticationSession(realm, logoutAuthSession.getParentSession());

        return response;
    }

    public static void finishUnconfirmedUserSession(KeycloakSession session, RealmModel realm, UserSessionModel userSessionModel) {
        if (userSessionModel.getAuthenticatedClientSessions().values().stream().anyMatch(cs -> !CommonClientSessionModel.Action.LOGGED_OUT.name().equals(cs.getAction()))) {
            logger.warnf("UserSession with id %s is removed while there are still some user sessions that are not logged out properly.", userSessionModel.getId());
            if (logger.isTraceEnabled()) {
                logger.trace("Client sessions with their states:");
                userSessionModel.getAuthenticatedClientSessions().values()
                        .forEach(clientSessionModel -> logger.tracef("Client session for clientId: %s has action: %s", clientSessionModel.getClient().getClientId(), clientSessionModel.getAction()));
            }
        }

        session.sessions().removeUserSession(realm, userSessionModel);
    }


    public static IdentityCookieToken createIdentityToken(KeycloakSession keycloakSession, RealmModel realm, UserModel user, UserSessionModel session, String issuer) {
        IdentityCookieToken token = new IdentityCookieToken();
        token.id(KeycloakModelUtils.generateId());
        token.issuedNow();
        token.subject(user.getId());
        token.issuer(issuer);
        token.type(TokenUtil.TOKEN_TYPE_KEYCLOAK_ID);

        if (session != null) {
            token.setSessionState(session.getId());
        }

        if (session != null && session.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0) {
            token.expiration(Time.currentTime() + realm.getSsoSessionMaxLifespanRememberMe());
        } else if (realm.getSsoSessionMaxLifespan() > 0) {
            token.expiration(Time.currentTime() + realm.getSsoSessionMaxLifespan());
        }

        String stateChecker = (String) keycloakSession.getAttribute("state_checker");
        if (stateChecker == null) {
            stateChecker = Base64Url.encode(SecretGenerator.getInstance().randomBytes());
            keycloakSession.setAttribute("state_checker", stateChecker);
        }
        token.getOtherClaims().put("state_checker", stateChecker);

        return token;
    }

    public static void createLoginCookie(KeycloakSession keycloakSession, RealmModel realm, UserModel user, UserSessionModel session, UriInfo uriInfo, ClientConnection connection) {
        String cookiePath = getIdentityCookiePath(realm, uriInfo);
        String issuer = Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName());
        IdentityCookieToken identityCookieToken = createIdentityToken(keycloakSession, realm, user, session, issuer);
        String encoded = keycloakSession.tokens().encode(identityCookieToken);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);
        int maxAge = NewCookie.DEFAULT_MAX_AGE;
        if (session != null && session.isRememberMe()) {
            maxAge = realm.getSsoSessionMaxLifespanRememberMe() > 0 ? realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan();
        }
        logger.debugv("Create login cookie - name: {0}, path: {1}, max-age: {2}", KEYCLOAK_IDENTITY_COOKIE, cookiePath, maxAge);
        CookieHelper.addCookie(KEYCLOAK_IDENTITY_COOKIE, encoded, cookiePath, null, null, maxAge, secureOnly, true, SameSiteAttributeValue.NONE);
        //builder.cookie(new NewCookie(cookieName, encoded, cookiePath, null, null, maxAge, secureOnly));// todo httponly , true);

        String sessionCookieValue = realm.getName() + "/" + user.getId();
        if (session != null) {
            sessionCookieValue += "/" + session.getId();
        }
        // THIS SHOULD NOT BE A HTTPONLY COOKIE!  It is used for OpenID Connect Iframe Session support!
        // Max age should be set to the max lifespan of the session as it's used to invalidate old-sessions on re-login
        int sessionCookieMaxAge = session.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0 ? realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan();
        CookieHelper.addCookie(KEYCLOAK_SESSION_COOKIE, sessionCookieValue, cookiePath, null, null, sessionCookieMaxAge, secureOnly, false, SameSiteAttributeValue.NONE);
        P3PHelper.addP3PHeader();
    }

    public static void createRememberMeCookie(RealmModel realm, String username, UriInfo uriInfo, ClientConnection connection) {
        String path = getIdentityCookiePath(realm, uriInfo);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);
        // remember me cookie should be persistent (hardcoded to 365 days for now)
        //NewCookie cookie = new NewCookie(KEYCLOAK_REMEMBER_ME, "true", path, null, null, realm.getCentralLoginLifespan(), secureOnly);// todo httponly , true);
        try {
            CookieHelper.addCookie(KEYCLOAK_REMEMBER_ME, "username:" + URLEncoder.encode(username, "UTF-8"), path, null, null, 31536000, secureOnly, true);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to urlencode", e);
        }
    }

    public static String getRememberMeUsername(RealmModel realm, ClientModel client, HttpHeaders headers) {
        if (client.isRememberMe()) {
            Cookie cookie = headers.getCookies().get(AuthenticationManager.KEYCLOAK_REMEMBER_ME);
            if (cookie != null) {
                String value = cookie.getValue();
                String[] s = value.split(":");
                if (s[0].equals("username") && s.length == 2) {
                    try {
                        return URLDecoder.decode(s[1], "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Failed to urldecode", e);
                    }
                }
            }
        }
        return null;
    }

    public static void expireIdentityCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debug("Expiring identity cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        expireCookie(realm, KEYCLOAK_IDENTITY_COOKIE, path, true, connection, SameSiteAttributeValue.NONE);
        expireCookie(realm, KEYCLOAK_SESSION_COOKIE, path, false, connection, SameSiteAttributeValue.NONE);

        String oldPath = getOldCookiePath(realm, uriInfo);
        expireCookie(realm, KEYCLOAK_IDENTITY_COOKIE, oldPath, true, connection, SameSiteAttributeValue.NONE);
        expireCookie(realm, KEYCLOAK_SESSION_COOKIE, oldPath, false, connection, SameSiteAttributeValue.NONE);
    }

    public static void expireOldIdentityCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debug("Expiring old identity cookie with wrong path");

        String oldPath = getOldCookiePath(realm, uriInfo);
        expireCookie(realm, KEYCLOAK_IDENTITY_COOKIE, oldPath, true, connection, SameSiteAttributeValue.NONE);
        expireCookie(realm, KEYCLOAK_SESSION_COOKIE, oldPath, false, connection, SameSiteAttributeValue.NONE);
    }


    public static void expireRememberMeCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debug("Expiring remember me cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        String cookieName = KEYCLOAK_REMEMBER_ME;
        expireCookie(realm, cookieName, path, true, connection, null);
    }

    public static void expireOldAuthSessionCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debugv("Expire {1} cookie .", AuthenticationSessionManager.AUTH_SESSION_ID);

        String oldPath = getOldCookiePath(realm, uriInfo);
        expireCookie(realm, AuthenticationSessionManager.AUTH_SESSION_ID, oldPath, true, connection, SameSiteAttributeValue.NONE);
    }

    protected static String getIdentityCookiePath(RealmModel realm, UriInfo uriInfo) {
        return getRealmCookiePath(realm, uriInfo);
    }

    public static String getRealmCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName());
        // KEYCLOAK-5270
        return uri.getRawPath() + "/";
    }

    public static String getOldCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName());
        return uri.getRawPath();
    }

    public static String getAccountCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.accountUrl(uriInfo.getBaseUriBuilder()).build(realm.getName());
        return uri.getRawPath();
    }

    public static void expireCookie(RealmModel realm, String cookieName, String path, boolean httpOnly, ClientConnection connection, SameSiteAttributeValue sameSite) {
        logger.debugf("Expiring cookie: %s path: %s", cookieName, path);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);
        ;
        CookieHelper.addCookie(cookieName, "", path, null, "Expiring cookie", 0, secureOnly, httpOnly, sameSite);
    }

    public AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm) {
        return authenticateIdentityCookie(session, realm, true);
    }

    public static AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm, boolean checkActive) {
        Cookie cookie = CookieHelper.getCookie(session.getContext().getRequestHeaders().getCookies(), KEYCLOAK_IDENTITY_COOKIE);
        String tokenString = cookie != null ? cookie.getValue() : session.getContext().getRequestHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);

        if (tokenString == null || tokenString.isEmpty()) {
            logger.debugv("Could not find cookie: {0}", KEYCLOAK_IDENTITY_COOKIE);
            return null;
        }

        AuthResult authResult = verifyIdentityToken(session, realm, session.getContext().getUri(), session.getContext().getConnection(), checkActive, false, null, true, tokenString, session.getContext().getRequestHeaders(), VALIDATE_IDENTITY_COOKIE);
        if (authResult == null) {
            expireIdentityCookie(realm, session.getContext().getUri(), session.getContext().getConnection());
            expireOldIdentityCookie(realm, session.getContext().getUri(), session.getContext().getConnection());
            return null;
        }
        authResult.getSession().setLastSessionRefresh(Time.currentTime());
        return authResult;
    }


    public static Response redirectAfterSuccessfulFlow(KeycloakSession session, RealmModel realm, UserSessionModel userSession,
                                                       ClientSessionContext clientSessionCtx,
                                                       HttpRequest request, UriInfo uriInfo, ClientConnection clientConnection,
                                                       EventBuilder event, AuthenticationSessionModel authSession) {
        LoginProtocol protocolImpl = session.getProvider(LoginProtocol.class, authSession.getProtocol());
        protocolImpl.setRealm(realm)
                .setHttpHeaders(request.getHttpHeaders())
                .setUriInfo(uriInfo)
                .setEventBuilder(event);
        return redirectAfterSuccessfulFlow(session, realm, userSession, clientSessionCtx, request, uriInfo, clientConnection, event, authSession, protocolImpl);

    }

    public static Response redirectAfterSuccessfulFlow(KeycloakSession session, RealmModel realm, UserSessionModel userSession,
                                                       ClientSessionContext clientSessionCtx,
                                                       HttpRequest request, UriInfo uriInfo, ClientConnection clientConnection,
                                                       EventBuilder event, AuthenticationSessionModel authSession, LoginProtocol protocol) {
        Cookie sessionCookie = getCookie(request.getHttpHeaders().getCookies(), AuthenticationManager.KEYCLOAK_SESSION_COOKIE);
        if (sessionCookie != null) {

            String[] split = sessionCookie.getValue().split("/");
            if (split.length >= 3) {
                String oldSessionId = split[2];
                if (!oldSessionId.equals(userSession.getId())) {
                    UserSessionModel oldSession = session.sessions().getUserSession(realm, oldSessionId);
                    if (oldSession != null) {
                        logger.debugv("Removing old user session: session: {0}", oldSessionId);
                        session.sessions().removeUserSession(realm, oldSession);
                    }
                }
            }
        }

        // Updates users locale if required
        session.getContext().resolveLocale(userSession.getUser(), LocaleType.ADMIN);

        // refresh the cookies!
        createLoginCookie(session, realm, userSession.getUser(), userSession, uriInfo, clientConnection);
        if (userSession.getState() != UserSessionModel.State.LOGGED_IN)
            userSession.setState(UserSessionModel.State.LOGGED_IN);
        if (userSession.isRememberMe()) {
            createRememberMeCookie(realm, userSession.getLoginUsername(), uriInfo, clientConnection);
        } else {
            expireRememberMeCookie(realm, uriInfo, clientConnection);
        }

        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();

        // Update userSession note with authTime. But just if flag SSO_AUTH is not set
        boolean isSSOAuthentication = AuthenticatorUtil.isSSOAuthentication(authSession);
        if (isSSOAuthentication) {
            clientSession.setNote(SSO_AUTH, "true");
            authSession.removeAuthNote(SSO_AUTH);
        } else {
            int authTime = Time.currentTime();
            userSession.setNote(AUTH_TIME, String.valueOf(authTime));
            clientSession.removeNote(SSO_AUTH);
        }

        // The user has successfully logged in and we can clear his/her previous login failure attempts.
        logSuccess(session, authSession);

        return protocol.authenticated(authSession, userSession, clientSessionCtx);

    }

    public static String getSessionIdFromSessionCookie(KeycloakSession session) {
        Cookie cookie = getCookie(session.getContext().getRequestHeaders().getCookies(), KEYCLOAK_SESSION_COOKIE);
        if (cookie == null || "".equals(cookie.getValue())) {
            logger.debugv("Could not find cookie: {0}", KEYCLOAK_SESSION_COOKIE);
            return null;
        }

        String[] parts = cookie.getValue().split("/", 3);
        if (parts.length != 3) {
            logger.debugv("Cannot parse session value from: {0}", KEYCLOAK_SESSION_COOKIE);
            return null;
        }
        return parts[2];
    }

    public static boolean isSSOAuthentication(AuthenticatedClientSessionModel clientSession) {
        String ssoAuth = clientSession.getNote(SSO_AUTH);
        return Boolean.parseBoolean(ssoAuth);
    }


    public static Response nextActionAfterAuthentication(KeycloakSession session, AuthenticationSessionModel authSession,
                                                         ClientConnection clientConnection,
                                                         HttpRequest request, UriInfo uriInfo, EventBuilder event) {
        Response requiredAction = actionRequired(session, authSession, request, event);
        if (requiredAction != null) return requiredAction;
        return finishedRequiredActions(session, authSession, null, clientConnection, request, uriInfo, event);

    }


    public static Response redirectToRequiredActions(KeycloakSession session, RealmModel realm, AuthenticationSessionModel authSession, UriInfo uriInfo, String requiredAction) {
        // redirect to non-action url so browser refresh button works without reposting past data
        ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authSession);
        accessCode.setAction(AuthenticationSessionModel.Action.REQUIRED_ACTIONS.name());
        authSession.setAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH, LoginActionsService.REQUIRED_ACTION);
        authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, requiredAction);

        UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(LoginActionsService.REQUIRED_ACTION);

        if (requiredAction != null) {
            uriBuilder.queryParam(Constants.EXECUTION, requiredAction);
        }

        uriBuilder.queryParam(Constants.CLIENT_ID, authSession.getClient().getClientId());
        uriBuilder.queryParam(Constants.TAB_ID, authSession.getTabId());

        if (uriInfo.getQueryParameters().containsKey(LoginActionsService.AUTH_SESSION_ID)) {
            uriBuilder.queryParam(LoginActionsService.AUTH_SESSION_ID, authSession.getParentSession().getId());

        }

        URI redirect = uriBuilder.build(realm.getName());
        return Response.status(302).location(redirect).build();

    }


    public static Response finishedRequiredActions(KeycloakSession session, AuthenticationSessionModel authSession, UserSessionModel userSession,
                                                   ClientConnection clientConnection, HttpRequest request, UriInfo uriInfo, EventBuilder event) {
        String actionTokenKeyToInvalidate = authSession.getAuthNote(INVALIDATE_ACTION_TOKEN);
        if (actionTokenKeyToInvalidate != null) {
            ActionTokenKeyModel actionTokenKey = DefaultActionTokenKey.from(actionTokenKeyToInvalidate);

            if (actionTokenKey != null) {
                ActionTokenStoreProvider actionTokenStore = session.getProvider(ActionTokenStoreProvider.class);
                actionTokenStore.put(actionTokenKey, null); // Token is invalidated
            }
        }

        if (authSession.getAuthNote(END_AFTER_REQUIRED_ACTIONS) != null) {
            LoginFormsProvider infoPage = session.getProvider(LoginFormsProvider.class).setAuthenticationSession(authSession)
                    .setSuccess(Messages.ACCOUNT_UPDATED);
            if (authSession.getAuthNote(SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS) != null) {
                if (authSession.getRedirectUri() != null) {
                    infoPage.setAttribute("pageRedirectUri", authSession.getRedirectUri());
                }

            } else {
                infoPage.setAttribute(Constants.SKIP_LINK, true);
            }
            Response response = infoPage
                    .createInfoPage();

            new AuthenticationSessionManager(session).removeAuthenticationSession(authSession.getRealm(), authSession, true);

            return response;
        }
        RealmModel realm = authSession.getRealm();

        ClientSessionContext clientSessionCtx = AuthenticationProcessor.attachSession(authSession, userSession, session, realm, clientConnection, event);
        userSession = clientSessionCtx.getClientSession().getUserSession();

        event.event(EventType.LOGIN);
        event.session(userSession);
        event.success();

        // to block the flow if a blacklisted authentication happens
        if(session.getContext().getAuthenticationSession() != null && session.getContext().getAuthenticationSession().getAuthNote("blackListingBlockFlow") != null && Boolean.parseBoolean(session.getContext().getAuthenticationSession().getAuthNote("blackListingBlockFlow")))
            throw new ErrorPageException(session, Response.Status.UNAUTHORIZED, "Access Denied");

        // add authentication policy or trigger after login success
        RealmModel realmModel = session.getContext().getRealm();
        Optional<ComponentModel> componentModel = realmModel.getComponentsStream(realmModel.getId()).filter(x -> x.getProviderId().equalsIgnoreCase("authentication-policy-checker")).findFirst();
        session.subPolicies().addAuthenticationPolicy(realmModel, componentModel.isPresent());
        ClientRegistrationPolicyManager.triggerAfterRegister(session, RegistrationAuth.AUTHENTICATED, "authentication-policy-checker");

        return redirectAfterSuccessfulFlow(session, realm, userSession, clientSessionCtx, request, uriInfo, clientConnection, event, authSession);
    }

    // Return null if action is not required. Or the name of the requiredAction in case it is required.
    // Order of triggering Actions --> 1.License 2.SSORoleCheck 3.MFA 4.InbuiltActions 5.IWS
    public static String nextRequiredAction(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                            final HttpRequest request, final EventBuilder event) {
        final RealmModel realm = authSession.getRealm();
        final UserModel user = authSession.getAuthenticatedUser();
        final ClientModel client = authSession.getClient();
        
        //here we clear all the required actions
        List<String> toBeRemovedActions = user.getRequiredActionsStream().collect(Collectors.toList());
        toBeRemovedActions.forEach(user::removeRequiredAction);

        addLicenseAction(session, authSession, request, event, realm, user, client);
        addPreferredLoginLocaleAction(session, authSession, request, event, realm, user, client);


        if(realm.getMode().equals("wiam")) {
            boolean isSSOApp = client.getAttribute("isSSO") != null && client.getAttribute("isSSO").equalsIgnoreCase("true");
            if (!isSSOApp) {
                isSSOApp = client.getAttribute("sso.show") != null && client.getAttribute("sso.show").equalsIgnoreCase("true");
            }

            if (isSSOApp) {
                addSSOAction(session, authSession, request, event, realm, user, client);
            }
        }

        List<String> defClients = new ArrayList<>(Constants.defaultClients);
        List<String> inBClients = new ArrayList<>(Constants.inBuildClients);
        defClients.removeAll(inBClients);
        
        RealmModel masterRealm = session.realms().getRealmByName(Config.getAdminRealm());
        Boolean disableMfa = masterRealm.getAttribute("disableMfa", false);
        if (!defClients.contains(client.getClientId()) && !disableMfa) {
            addMFAAction(session, authSession, request, event, realm, user, client);
        }

        // Add Inbuilt Actions (applicable for inbuilt clients)
        addInBuiltActions(session, authSession, request, event, realm, user, client);
        if(realm.getMode().equals("wiam")) {
            String iswApp = client.getAttribute("iws.enable");
            if (iswApp != null && iswApp.equals("true")) {
                addIWSAction(session, authSession, request, event, realm, user, client);
            }
        }

        Optional<String> reqAction = user.getRequiredActionsStream().findFirst();
        if (reqAction.isPresent()) {
            return reqAction.get();
        }
        if (!authSession.getRequiredActions().isEmpty()) {
            return authSession.getRequiredActions().iterator().next();
        }

        String kcAction = authSession.getClientNote(Constants.KC_ACTION);
        if (kcAction != null) {
            return kcAction;
        }

        if (client.isConsentRequired() || isOAuth2DeviceVerificationFlow(authSession)) {

            UserConsentModel grantedConsent = getEffectiveGrantedConsent(session, authSession);

            // See if any clientScopes need to be approved on consent screen
            List<AuthorizationDetails> clientScopesToApprove = getClientScopesToApproveOnConsentScreen(grantedConsent, session);
            if (!clientScopesToApprove.isEmpty()) {
                return CommonClientSessionModel.Action.OAUTH_GRANT.name();
            }

            String consentDetail = (grantedConsent != null) ? Details.CONSENT_VALUE_PERSISTED_CONSENT : Details.CONSENT_VALUE_NO_CONSENT_REQUIRED;
            event.detail(Details.CONSENT, consentDetail);
        } else {
            event.detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        }
        return null;
    }

    @Nullable
    private static void addInBuiltActions(KeycloakSession session, AuthenticationSessionModel authSession, HttpRequest request, EventBuilder event, RealmModel realm, UserModel user, ClientModel client) {
        String actionId =
                client.isOverrideAuthentication()  ?
                        client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.ACTION_BINDING)
                : realm.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.ACTION_BINDING);
        Optional<ActionModel> actionModel = realm.getActionStream().filter(x -> x.getAlias().equalsIgnoreCase(client.getClientId() + "_action")).findFirst();
        if (actionModel.isPresent()) {
            if (actionId == null || !(actionModel.get().getId().equals(actionId))) {
                evaluateRequiredActionTriggers(session, authSession, request, event, realm, user, actionModel.get().getId());
            }
        }
        evaluateRequiredActionTriggers(session, authSession, request, event, realm, user, actionId);
    }

    private static void addIWSAction(KeycloakSession session, AuthenticationSessionModel authSession, HttpRequest request, EventBuilder event, RealmModel realm, UserModel user, ClientModel client) {
        RequiredActionProviderModel providerModel = realm.getRequiredActionProviderByAlias(RealmModel.RequiredAction.IWS_ACTION.getName());
        if (providerModel == null) {
            RequiredActionProviderModel iwsAction = new RequiredActionProviderModel();
            iwsAction.setEnabled(true);
            iwsAction.setAlias(RealmModel.RequiredAction.IWS_ACTION.getName());
            iwsAction.setName("ISW Action");
            iwsAction.setProviderId(RealmModel.RequiredAction.IWS_ACTION.getName());
            iwsAction.setDefaultAction(false);
            iwsAction.setPriority(6000);
            iwsAction.setActionId(RealmModel.RequiredAction.IWS_ACTION.getName());
            realm.addRequiredActionProvider(iwsAction);
        }
        if (user.getRequiredActionsStream().filter(x -> x.equals(RealmModel.RequiredAction.IWS_ACTION.getName())).collect(Collectors.toList()).isEmpty()) {
            user.addRequiredAction(RealmModel.RequiredAction.IWS_ACTION.getName());
        }

        evaluateRequiredActionTriggers(session, authSession, request, event, realm, user, RealmModel.RequiredAction.IWS_ACTION.getName());
    }

    private static void addSSOAction(KeycloakSession session, AuthenticationSessionModel authSession, HttpRequest request, EventBuilder event, RealmModel realm, UserModel user, ClientModel client) {
        Optional<ActionModel> optionalActionModel = realm.getActionStream().filter(x -> x.getAlias().equalsIgnoreCase(Constants.SSO_MANAGEMENT_CLIENT_ID + "_template_action")).findFirst();
        if (!optionalActionModel.isPresent())
        {

            PolicyModel triggerPolicyModel = new PolicyModel();

            triggerPolicyModel.setPolicyName(Constants.SSO_MANAGEMENT_CLIENT_ID + "_template_trigger_policy");
            triggerPolicyModel.setPolicyType("action");
            triggerPolicyModel.setPolicyValue("triggerpolicy(true) and decision()");
            triggerPolicyModel.setRealmPolicy(false);
            triggerPolicyModel.setInBuilt(true);

            triggerPolicyModel = session.policies().addPolicy(realm, triggerPolicyModel);

            ActionModel actionModel = new ActionModel();
            actionModel.setAlias(Constants.SSO_MANAGEMENT_CLIENT_ID + "_template_action");
            actionModel.setInBuilt(true);
            actionModel = realm.addAction(actionModel);

            RequiredActionProviderModel userRoleRA = new RequiredActionProviderModel();
            userRoleRA.setAlias("sso-user-role-check");
            userRoleRA.setName("sso-user-role-check");
            userRoleRA.setProviderId("sso-user-role-check");
            userRoleRA.setDefaultAction(false);
            userRoleRA.setPolicyId(triggerPolicyModel.getId());
            userRoleRA.setPriority(0);
            userRoleRA.setEnabled(true);
            userRoleRA.setActionId(actionModel.getId());
            realm.addRequiredActionProvider(userRoleRA);
            optionalActionModel = Optional.of((actionModel));
        }

        evaluateRequiredActionTriggers(session, authSession, request, event, realm, user, optionalActionModel.get().getId());
    }

    private static void addMFAAction(KeycloakSession session, AuthenticationSessionModel authSession, HttpRequest request, EventBuilder event, RealmModel realm, UserModel user, ClientModel client) {
        RequiredActionProviderModel providerModel = realm.getRequiredActionProviderByAlias(RealmModel.RequiredAction.MFA_ACTION.getName());
        if (providerModel == null) {
            RequiredActionProviderModel mfaAction = new RequiredActionProviderModel();
            mfaAction.setEnabled(true);
            mfaAction.setAlias(RealmModel.RequiredAction.MFA_ACTION.getName());
            mfaAction.setName("MFA Action");
            mfaAction.setProviderId(RealmModel.RequiredAction.MFA_ACTION.getName());
            mfaAction.setDefaultAction(false);
            mfaAction.setPriority(5000);
            mfaAction.setActionId(RealmModel.RequiredAction.MFA_ACTION.getName());
            realm.addRequiredActionProvider(mfaAction);
        }
        if (client.isStandardFlowEnabled() || client.isImplicitFlowEnabled()) {
            if (user.getRequiredActionsStream().filter(x -> x.equals(RealmModel.RequiredAction.MFA_ACTION.getName())).collect(Collectors.toList()).isEmpty()) {
                user.addRequiredAction(RealmModel.RequiredAction.MFA_ACTION.getName());
            }
        }

        evaluateRequiredActionTriggers(session, authSession, request, event, realm, user, RealmModel.RequiredAction.MFA_ACTION.getName());
    }

    private static void addLicenseAction(KeycloakSession session, AuthenticationSessionModel authSession, HttpRequest request, EventBuilder event, RealmModel realm, UserModel user, ClientModel client) {
        // License required action is completely bypassed in this free/open version
    }

    private static void addPreferredLoginLocaleAction(KeycloakSession session, AuthenticationSessionModel authSession, HttpRequest request, EventBuilder event, RealmModel realm, UserModel user, ClientModel client) {
        RequiredActionProviderModel providerModel = realm.getRequiredActionProviderByAlias(RealmModel.RequiredAction.PREFERRED_LOGIN_LOCALE.getName());
        if (providerModel == null) {
            RequiredActionProviderModel preferredLoginLocale = getPreferredLoginLocaleRequiredActionProviderModel();
            realm.addRequiredActionProvider(preferredLoginLocale);
        }
        if (client.isStandardFlowEnabled() || client.isImplicitFlowEnabled()) {
            if (user.getRequiredActionsStream().noneMatch(x -> x.equals(RealmModel.RequiredAction.PREFERRED_LOGIN_LOCALE.getName())) && realm.getPreferredLoginLocalesStream().count() > 1) {
                user.addRequiredAction(RealmModel.RequiredAction.PREFERRED_LOGIN_LOCALE.getName());
            }
            evaluateRequiredActionTriggers(session, authSession, request, event, realm, user, RealmModel.RequiredAction.PREFERRED_LOGIN_LOCALE.getName());
        }
    }
    
    private static RequiredActionProviderModel getPreferredLoginLocaleRequiredActionProviderModel() {
        RequiredActionProviderModel preferredLoginLocale = new RequiredActionProviderModel();
        preferredLoginLocale.setEnabled(true);
        preferredLoginLocale.setAlias(RealmModel.RequiredAction.PREFERRED_LOGIN_LOCALE.getName());
        preferredLoginLocale.setName("Preferred-locale-RA");
        preferredLoginLocale.setProviderId(RealmModel.RequiredAction.PREFERRED_LOGIN_LOCALE.getName());
        preferredLoginLocale.setDefaultAction(false);
        preferredLoginLocale.setPriority(-1);
        preferredLoginLocale.setActionId(RealmModel.RequiredAction.PREFERRED_LOGIN_LOCALE.getName());
        return preferredLoginLocale;
    }

    private static UserConsentModel getEffectiveGrantedConsent(KeycloakSession session, AuthenticationSessionModel authSession) {
        // https://tools.ietf.org/html/draft-ietf-oauth-device-flow-15#section-5.4
        // The spec says "The authorization server SHOULD display information about the device",
        // so we ignore existing persistent consent to display the consent screen always.
        if (isOAuth2DeviceVerificationFlow(authSession)) {
            return null;
        }


        // If prompt=consent, we ignore existing persistent consent
        String prompt = authSession.getClientNote(OIDCLoginProtocol.PROMPT_PARAM);
        if (TokenUtil.hasPrompt(prompt, OIDCLoginProtocol.PROMPT_VALUE_CONSENT)) {
            return null;
        } else {
            final RealmModel realm = authSession.getRealm();
            final UserModel user = authSession.getAuthenticatedUser();
            final ClientModel client = authSession.getClient();

            return session.users().getConsentByClient(realm, user.getId(), client.getId());
        }
    }


    public static Response actionRequired(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                          final HttpRequest request, final EventBuilder event) {
        final RealmModel realm = authSession.getRealm();
        final UserModel user = authSession.getAuthenticatedUser();
        final ClientModel client = authSession.getClient();

        if(user == null){
            return session.getProvider(LoginFormsProvider.class).setError(Messages.ERROR_OCCERRED_CONTACT_ADMIN).createErrorPage(Response.Status.UNAUTHORIZED);
        }

        evaluateRequiredActionTriggers(session, authSession, request, event, realm, user, client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.ACTION_BINDING));

        logger.debugv("processAccessCode: go to oauth page?: {0}", client.isConsentRequired());

        event.detail(Details.CODE_ID, authSession.getParentSession().getId());

        Stream<String> requiredActions = user.getRequiredActionsStream();
        Response action = executionActions(session, authSession, request, event, realm, user, requiredActions, client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.ACTION_BINDING));
        if (action != null) return action;

        // executionActions() method should remove any duplicate actions that might be in the clientSession
        action = executionActions(session, authSession, request, event, realm, user, authSession.getRequiredActions().stream(), client.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.ACTION_BINDING));
        if (action != null) return action;

        // https://tools.ietf.org/html/draft-ietf-oauth-device-flow-15#section-5.4
        // The spec says "The authorization server SHOULD display information about the device",
        // so the consent is required when running a verification flow of OAuth 2.0 Device Authorization Grant.
        if (client.isConsentRequired() || isOAuth2DeviceVerificationFlow(authSession)) {

            UserConsentModel grantedConsent = getEffectiveGrantedConsent(session, authSession);

            List<AuthorizationDetails> authorizationDetails = getClientScopesToApproveOnConsentScreen(grantedConsent, session);

            // Skip grant screen if everything was already approved by this user
            if (authorizationDetails.size() > 0) {
                String execution = AuthenticatedClientSessionModel.Action.OAUTH_GRANT.name();

                ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authSession);
                accessCode.setAction(AuthenticatedClientSessionModel.Action.REQUIRED_ACTIONS.name());
                authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution);

                return session.getProvider(LoginFormsProvider.class)
                        .setAuthenticationSession(authSession)
                        .setExecution(execution)
                        .setClientSessionCode(accessCode.getOrGenerateCode())
                        .setAccessRequest(authorizationDetails)
                        .createOAuthGrant();
            } else {
                String consentDetail = (grantedConsent != null) ? Details.CONSENT_VALUE_PERSISTED_CONSENT : Details.CONSENT_VALUE_NO_CONSENT_REQUIRED;
                event.detail(Details.CONSENT, consentDetail);
            }
        } else {
            event.detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        }
        return null;

    }

    private static List<AuthorizationDetails> getClientScopesToApproveOnConsentScreen(UserConsentModel grantedConsent, KeycloakSession session) {
        // Client Scopes to be displayed on consent screen
        List<AuthorizationDetails> clientScopesToDisplay = new LinkedList<>();

        // AuthorizationDetails are going to be returned regardless of the Dynamic Scope feature state
        for (AuthorizationDetails authDetails : getClientScopeModelStream(session).collect(Collectors.toList())) {
            ClientScopeModel clientScope = authDetails.getClientScope();
            if (clientScope == null || !clientScope.isDisplayOnConsentScreen()) {
                continue;
            }

            // we need to add dynamic scopes with params to the scopes to consent every time for now
            if (grantedConsent == null || !grantedConsent.isClientScopeGranted(clientScope) || isDynamicScopeWithParam(authDetails)) {
                clientScopesToDisplay.add(authDetails);
            }
        }

        return clientScopesToDisplay;
    }

    private static boolean isDynamicScopeWithParam(AuthorizationDetails authorizationDetails) {
        boolean dynamicScopeWithParam = authorizationDetails.getClientScope().isDynamicScope()
                && authorizationDetails.getAuthorizationDetails() != null;
        if (dynamicScopeWithParam) {
            logger.debugf("Scope %1s is a dynamic scope with param: %2s",
                    authorizationDetails.getAuthorizationDetails().getScopeNameFromCustomData(),
                    authorizationDetails.getDynamicScopeParam());
        }
        return dynamicScopeWithParam;
    }

    private static List<ClientScopeModel> getOptionalClientScopesToApproveOnConsentScreen(RealmModel realm, UserConsentModel grantedConsent, ClientModel clientModel) {
        // Client Scopes to be displayed on consent screen
        List<ClientScopeModel> clientScopesToDisplay = new LinkedList<>();
        Map<String, ClientScopeModel> clientScopeModelMap = clientModel.getClientScopes(false, false);
        for (Map.Entry<String, ClientScopeModel> clientScopeId : clientScopeModelMap.entrySet()) {
            ClientScopeModel clientScope = clientScopeId.getValue();

            if (clientScope == null || !clientScope.isDisplayOnConsentScreen()) {
                continue;
            }

            // Check if consent already granted by user
            if (grantedConsent == null || !grantedConsent.isClientScopeGranted(clientScope)) {
                clientScopesToDisplay.add(clientScope);
            }
        }

        return clientScopesToDisplay;
    }


    private static Stream<AuthorizationDetails> getClientScopeModelStream(KeycloakSession session) {
        AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();
        //if Dynamic Scopes are enabled, get the scopes from the AuthorizationRequestContext, passing the session and scopes as parameters
        // then concat a Stream with the ClientModel, as it's discarded in the getAuthorizationRequestContext method
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            return AuthorizationContextUtil.getAuthorizationRequestsStreamFromScopesWithClient(session, authSession.getClientNote(OAuth2Constants.SCOPE));
        }
        // if dynamic scopes are not enabled, we retain the old behaviour, but the ClientScopes will be wrapped in
        // AuthorizationRequest objects to standardize the code handling these.
        return authSession.getClientScopes().stream()
                .map(scopeId -> KeycloakModelUtils.findClientScopeById(authSession.getRealm(), authSession.getClient(), scopeId))
                .map(AuthorizationDetails::new);
    }


    public static void setClientScopesInSession(AuthenticationSessionModel authSession) {
        ClientModel client = authSession.getClient();
        UserModel user = authSession.getAuthenticatedUser();

        // todo scope param protocol independent
        String scopeParam = authSession.getClientNote(OAuth2Constants.SCOPE);

        Set<String> requestedClientScopes = TokenManager.getRequestedClientScopes(scopeParam, client)
                .map(ClientScopeModel::getId).collect(Collectors.toSet());

        authSession.setClientScopes(requestedClientScopes);
    }

    public static RequiredActionProvider createRequiredAction(RequiredActionContextResult context) {
        String display = context.getAuthenticationSession().getAuthNote(OAuth2Constants.DISPLAY);
        if (display == null) return context.getFactory().create(context.getSession());

        if (context.getFactory() instanceof DisplayTypeRequiredActionFactory) {
            RequiredActionProvider provider = ((DisplayTypeRequiredActionFactory) context.getFactory()).createDisplay(context.getSession(), display);
            if (provider != null) return provider;
        }
        // todo create a provider for handling lack of display support
        if (OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(display)) {
            context.getAuthenticationSession().removeAuthNote(OAuth2Constants.DISPLAY);
            throw new AuthenticationFlowException(AuthenticationFlowError.DISPLAY_NOT_SUPPORTED, ConsoleDisplayMode.browserContinue(context.getSession(), context.getUriInfo().getRequestUri().toString()));

        } else {
            return context.getFactory().create(context.getSession());
        }
    }


    protected static Response executionActions(KeycloakSession session, AuthenticationSessionModel authSession,
                                               HttpRequest request, EventBuilder event, RealmModel realm, UserModel user,
                                               Stream<String> requiredActions, String actionId) {
        Optional<Response> response;
        if ("bo".equals(realm.getMode())) {

            List<String> requiredActionsList = requiredActions.collect(Collectors.toList());

            Map<String, List<RequiredActionProviderModel>> subWorkflowActions = getSubWorkflowActions(session, realm, requiredActionsList.stream(), actionId, user);

            List<RequiredActionProviderModel> sortedRequiredActions = sortRequiredActionsByPriority(session, realm, requiredActionsList.stream(), actionId).collect(Collectors.toList());

            List<RequiredActionProviderModel> finalSortedRequiredActions = new ArrayList<>();

            sortedRequiredActions.forEach(x -> {
                // Add the current action to the final list
                finalSortedRequiredActions.add(x);

                // Retrieve sub-workflow actions for the current action
                List<RequiredActionProviderModel> requiredActionProviderModels = subWorkflowActions.get(x.getId());
                if (requiredActionProviderModels != null && !requiredActionProviderModels.isEmpty()) {
                    processSubWorkflowActions(session, realm, subWorkflowActions, requiredActionProviderModels, x, finalSortedRequiredActions, user);
                }
            });
            response = processActions(session, authSession, request, event, realm, user, finalSortedRequiredActions);
        } else {
            response = sortRequiredActionsByPriority(session, realm, requiredActions, actionId)
                    .map(model -> executeAction(session, authSession, model, request, event, realm, user, false))
                    .filter(Objects::nonNull).findFirst();
        }
        if (response.isPresent())
            return response.get();

        String kcAction = authSession.getClientNote(Constants.KC_ACTION);
        if (kcAction != null) {
            Optional<RequiredActionProviderModel> requiredAction = realm.getRequiredActionProvidersStream(actionId)
                    .filter(m -> Objects.equals(m.getProviderId(), kcAction))
                    .findFirst();
            if (requiredAction.isPresent()) {
                return executeAction(session, authSession, requiredAction.get(), request, event, realm, user, true);
            }

            logger.debugv("Requested action {0} not configured for realm", kcAction);
            setKcActionStatus(kcAction, RequiredActionContext.KcActionStatus.ERROR, authSession);
        }

        return null;
    }

    private static void processSubWorkflowActions(KeycloakSession session, RealmModel realm,
                                                  Map<String, List<RequiredActionProviderModel>> subWorkflowActions,
                                                  List<RequiredActionProviderModel> requiredActionProviderModels,
                                                  RequiredActionProviderModel parentAction,
                                                  List<RequiredActionProviderModel> finalSortedRequiredActions, UserModel user) {
        RequiredActionProviderModel firstRequiredActionModel = entityToModel(requiredActionProviderModels.get(0), realm);
        firstRequiredActionModel.setParentId(parentAction.getId());

        if ((firstRequiredActionModel.getValidatorStepId() == null || firstRequiredActionModel.getValidatorStepId().equals("none")) &&
                (parentAction.getValidatorStepId() != null || !parentAction.getValidatorStepId().equals("none"))) {
            firstRequiredActionModel.setValidatorStepId(parentAction.getValidatorStepId());
        }

        if ((firstRequiredActionModel.getDirection() == null || firstRequiredActionModel.getDirection().equals("none"))
                && (parentAction.getDirection() != null || parentAction.getDirection().equals("none"))) {
            firstRequiredActionModel.setDirection(parentAction.getDirection());
        }

        finalSortedRequiredActions.add(firstRequiredActionModel);

        requiredActionProviderModels.forEach(providerModel -> {
            // Clone the model with updated parentId and add it to the final list
            RequiredActionProviderModel requiredActionModel = entityToModel(providerModel, realm);

            if ((requiredActionModel.getValidatorStepId() == null || requiredActionModel.getValidatorStepId().equals("none"))
                    && (parentAction.getValidatorStepId() != null || !parentAction.getValidatorStepId().equals("none"))) {
                requiredActionModel.setValidatorStepId(parentAction.getValidatorStepId());
            }

            if ((requiredActionModel.getDirection() == null || requiredActionModel.getDirection().equals("none"))
                    && (parentAction.getDirection() != null || parentAction.getDirection().equals("none"))) {
                requiredActionModel.setDirection(parentAction.getDirection());
            }

            // Add the action to the final sorted list
            finalSortedRequiredActions.add(requiredActionModel);

            // If the current provider is also a "bo-sub-workflow", recursively fetch its sub-workflow actions
            if ("bo-sub-workflow".equals(requiredActionModel.getProviderId())) {
                String subWorkflowId = requiredActionModel.getConfig().get("sub-workflow");

                List<String> userRequiredActions = user
                        .getRequiredActionsStream()
                        .collect(Collectors.toList());

                List<RequiredActionProviderModel> enabledSubWorkflowActions = realm
                        .getRequiredActionProvidersStream(subWorkflowId)
                        .filter(RequiredActionProviderModel::isEnabled)
                        .filter(action -> userRequiredActions.contains(action.getId())) // Filter actions found in both
                        .collect(Collectors.toList());

                if (!enabledSubWorkflowActions.isEmpty()) {
                    subWorkflowActions.put(subWorkflowId, enabledSubWorkflowActions);

                    // Recursively process sub-workflow actions if any
                    processSubWorkflowActions(session, realm, subWorkflowActions, enabledSubWorkflowActions, requiredActionModel, finalSortedRequiredActions, user);
                }
            }
        });
    }

    private static RequiredActionProviderModel entityToModel(RequiredActionProviderModel requiredActionProviderModel, RealmModel realm) {

        String policyType = requiredActionProviderModel.getPolicyType();
        if (policyType == null) {
            if (realm.getMode().equals("bo")) {
                policyType = "ciba";
            } else {
                policyType = "action";
            }
        }
        RequiredActionProviderModel model = new RequiredActionProviderModel();
        model.setId(requiredActionProviderModel.getId());
        model.setProviderId(requiredActionProviderModel.getProviderId());
        model.setAlias(requiredActionProviderModel.getAlias());
        model.setPolicyId(requiredActionProviderModel.getPolicyId());
        model.setPolicyType(policyType);
        model.setEnabled(requiredActionProviderModel.isEnabled());
        model.setValidator(requiredActionProviderModel.isValidator());
        model.setDefaultAction(requiredActionProviderModel.isDefaultAction());
        model.setParentId(requiredActionProviderModel.getParentId());
        model.setDirection(requiredActionProviderModel.getDirection());
        model.setValidatorStepId(requiredActionProviderModel.getValidatorStepId());
        model.setPriority(requiredActionProviderModel.getPriority());
        model.setName(requiredActionProviderModel.getName());
        model.setDescription(requiredActionProviderModel.getDescription());
        model.setActionId(requiredActionProviderModel.getActionId());
        model.setSkippable(requiredActionProviderModel.isSkippable());
        Map<String, String> config = new HashMap<>();
        if (requiredActionProviderModel.getConfig() != null) config.putAll(requiredActionProviderModel.getConfig());
        model.setConfig(config);
        return model;
    }

    private static Optional<Response> processActions(KeycloakSession session, AuthenticationSessionModel authSession,
                                                     HttpRequest request, EventBuilder event, RealmModel realm, UserModel user,
                                                     List<RequiredActionProviderModel> flowActionsList) {
        String lastValidatorId = authSession.getAuthNote("validator.last.execution.id");

        for (RequiredActionProviderModel action : flowActionsList) {
            if (!action.isEnabled()) {
                continue;
            }
            if (!Validation.isBlank(lastValidatorId) && action.getValidatorStepId().equals(lastValidatorId)) {
                Optional<RequiredActionProviderModel> nextAction = findNextEnabledAction(flowActionsList, lastValidatorId);
                if (nextAction.isPresent()) {
                    RequiredActionProviderModel nextActionModel = nextAction.get();
                    if (nextActionModel.isValidator()) {
                        return processValidator(session, authSession, request, event, realm, user, flowActionsList, nextActionModel);
                    } else {
                        Response actionResponse = executeAction(session, authSession, nextActionModel, request, event, realm, user, false);
                        if (actionResponse != null) {
                            return Optional.of(actionResponse);
                        }
                        // Check for next action with this action as parent
                        Optional<RequiredActionProviderModel> nextChildAction = findNextEnabledChildAction(flowActionsList, nextActionModel.getId());
                        if (nextChildAction.isPresent() && nextChildAction.get().isValidator()) {
                            return processValidator(session, authSession, request, event, realm, user, flowActionsList, nextChildAction.get());
                        } else {
                            flowActionsList.remove(0);
                            return processActions(session, authSession, request, event, realm, user,  flowActionsList);
                        }
                    }
                }
            } else if (!action.isValidator()) {
                Response actionResponse = executeAction(session, authSession, action, request, event, realm, user, false);
                if (actionResponse != null) {
                    return Optional.of(actionResponse);
                }
            } else {
                return processValidator(session, authSession, request, event, realm, user, flowActionsList, action);
            }
        }
        return Optional.empty();
    }

    private static Optional<RequiredActionProviderModel> findNextEnabledAction(List<RequiredActionProviderModel> flowActionsList, String validatorId) {
        return flowActionsList.stream()
                .filter(model -> model.getValidatorStepId().equals(validatorId)).findFirst();
    }

    private static Optional<RequiredActionProviderModel> findNextEnabledChildAction(List<RequiredActionProviderModel> flowActionsList, String parentId) {
        return flowActionsList.stream()
                .filter(model -> model.getParentId() != null && model.getParentId().equals(parentId))
                .findFirst();
    }

    private static Optional<Response> processValidator(KeycloakSession session, AuthenticationSessionModel authSession,
                                                         HttpRequest request, EventBuilder event, RealmModel realm, UserModel user,
                                                         List<RequiredActionProviderModel> actions, RequiredActionProviderModel validator) {

        authSession.setAuthNote("validator.last.execution.id", validator.getId());
        Response validatorActionResponse = executeAction(session, authSession, validator, request, event, realm, user, false);

        String validatorResult = Boolean.TRUE.toString();

        validatorResult = authSession.getAuthNote("validator.last.execution.result") != null ? authSession.getAuthNote("validator.last.execution.result") : validatorResult;

        String direction;
        if(!validatorResult.equals("validator_rule_failed")) {
            direction = Boolean.parseBoolean(validatorResult) ? "LEFT" : "RIGHT";
        } else {
            direction = "none";
        }

        EventBuilder eventBuilder = new EventBuilder(realm, session, session.getContext().getConnection());
        eventBuilder.detail("requestId", session.getContext().getAuthenticationSession().getAuthNote("request"));
        eventBuilder.client(session.getContext().getAuthenticationSession().getClient().getClientId());

        boolean validatorExecuted = actions.stream().anyMatch(model -> {
            String validatorPolicyId = model.getConfig().get("validation-policy-id");
            String attributeKey = "sp@validator_" + validatorPolicyId;
            TrxModel trxModel = session.getContext().getRealm().getTrx(authSession.getAuthNote("request"));
            return (trxModel.getAttributes().containsKey(attributeKey) && authSession.getAuthNote("validator_" + model.getId()) != null);
        });

        if(!validatorExecuted) {
            if (direction.equals("LEFT")) {
                eventBuilder.event(EventType.EZ_VALIDATOR).detail("Validator Condition: ", validator.getConfig().get("validation-condition-prompt") + VALIDATOR_PASSED).success();
                authSession.setAuthNote("validator_" + validator.getId(), "audited");
            } else {
                eventBuilder.event(EventType.EZ_VALIDATOR).error(validator.getConfig().get("validation-condition-prompt") + VALIDATOR_FAILED);
                authSession.setAuthNote("validator_" + validator.getId(), "audited");
            }
        }
//        event.event(EventType.EZ_VALIDATOR).error(validator.getConfig().get("validation-condition-prompt") + "has been " + (direction.equals("LEFT") ? "Passed" : "Filed"));
//        auditEvent(context, EventType.EZ_TRANSACTION_ATTRIBUTES, JsonSerialization.writeValueAsString(context.getRealm().getTrx(context.getRequestId()).getAttributes()));

        // Find actions to remove that are not part of the direction
        actions.removeIf(model -> model.getId().equals(validator.getId()));
        List<Map<String, String>> toRemove = new ArrayList<>();
        List<Map<String, String>> notToRemove = new ArrayList<>();
        actions.stream().filter(model -> validator.getId().equals(model.getValidatorStepId()) && !direction.equalsIgnoreCase(model.getDirection())).map(model -> {
            if (model.isValidator()) {
                toRemove(actions, toRemove, model);
            }
            Map<String, String> actionToRemove = new HashMap<>();
            actionToRemove.put("id", model.getId());
            actionToRemove.put("direction", model.getDirection());
            actionToRemove.put("validatorStepId", model.getValidatorStepId());
            actionToRemove.put("parentId",model.getParentId());
            return actionToRemove;
        }).forEach(toRemove::add);

        actions.stream().filter(model -> validator.getId().equals(model.getValidatorStepId()) && direction.equalsIgnoreCase(model.getDirection())).map(model -> {
            if (model.isValidator()) {
                toRemove(actions, notToRemove, model);
            }
            Map<String, String> actionNotToRemove = new HashMap<>();
            actionNotToRemove.put("id", model.getId());
            actionNotToRemove.put("direction", model.getDirection());
            actionNotToRemove.put("validatorStepId", model.getValidatorStepId());
            actionNotToRemove.put("parentId",model.getParentId());
            return actionNotToRemove;
        }).forEach(notToRemove::add);

        List<String> actionsList = new ArrayList<>();
        actions.stream().map(action -> action.getId()).forEach(actionsList::add);

        actions.removeIf(requiredActionProviderModel -> {
            List<Map<String, String>> toRemoveOrNot = new ArrayList<>();
            if((!requiredActionProviderModel.getAlias().equals("remove")) && (requiredActionProviderModel != null) && ((toRemove.stream().filter(x -> x.get("id").equals(requiredActionProviderModel.getId())).count() != 0 && notToRemove.stream().filter(x -> x.get("id").equals(requiredActionProviderModel.getId())).count() == 0) )) {
                    List<Map<String, String>> modelInToRemove = toRemove.stream().filter(x -> x.get("id").equals(requiredActionProviderModel.getId())).collect(Collectors.toList());
                    toRemoveOrNot = modelInToRemove.stream().filter(x -> {
                        return x.get("id").equals(requiredActionProviderModel.getId())
                                && x.get("direction").equals(requiredActionProviderModel.getDirection())
                                && x.get("validatorStepId").equals(requiredActionProviderModel.getValidatorStepId())
                                && x.get("parentId").equals(requiredActionProviderModel.getParentId());
                    }).collect(Collectors.toList());

                if(toRemoveOrNot.isEmpty()) {
                    return false;
                }

                    authSession.removeRequiredAction(requiredActionProviderModel.getId());
                UserModel authenticatedUser = authSession.getAuthenticatedUser();

                if (requiredActionProviderModel.getProviderId().equals("bo-sub-workflow") && !toRemoveOrNot.isEmpty()) {
                    processSubWorkflowActions(session, realm, actions, actionsList, requiredActionProviderModel, user, toRemove, notToRemove);
                }

                if (authenticatedUser != null) {
                    authenticatedUser.removeRequiredAction(requiredActionProviderModel.getId());
                }
                return true;
            }
            return false;
        });

        actions.removeIf(action -> action.getAlias().equals("remove"));

        return processActions(session, authSession, request, event, realm, user, actions);
    }


    private static void processSubWorkflowActions(KeycloakSession session, RealmModel realm,
                                                  List<RequiredActionProviderModel> actions,
                                                  List<String> actionsList,
                                                  RequiredActionProviderModel requiredActionProviderModel,
                                                  UserModel user,
                                                  List<Map<String, String>> toRemove,
                                                  List<Map<String, String>> notToRemove) {
        int index = actions.indexOf(requiredActionProviderModel) + 1;

        if (index != -1) {
            Iterator<RequiredActionProviderModel> iterator = actions.listIterator(index);

            List<String> subWorkflowActionsToBeRemoved = getSubWorkflowActionsToBeRemoved(session, realm, actionsList.stream(), requiredActionProviderModel, user);

            if (!subWorkflowActionsToBeRemoved.isEmpty()) {
                while (iterator.hasNext()) {
                    RequiredActionProviderModel action = iterator.next();

                    if (subWorkflowActionsToBeRemoved.contains(action.getId())) {
                        action.setAlias("remove"); // Mark for removal
                    } else if (action.getProviderId().equals("bo-sub-workflow") && ((toRemove.stream().filter(x -> x.get("id").equals(action.getId())).count() != 0 && notToRemove.stream().filter(x -> x.get("id").equals(action.getId())).count() == 0) )) {
                        // Recursive call for nested sub-workflow
                        processSubWorkflowActions(session, realm, actions, actionsList, action, user, toRemove, notToRemove);
                    } else {
                        break; // Stop if it's not a sub-workflow or an action to remove
                    }
                }
            }
        }
    }


    private static void toRemove(List<RequiredActionProviderModel> actions, List<Map<String, String>> toRemove, RequiredActionProviderModel requiredActionProviderModel) {
        actions.stream().filter(model -> model.getValidatorStepId().equalsIgnoreCase(requiredActionProviderModel.getId())).map(model ->{
                if(model.isValidator()) {
                    toRemove(actions, toRemove, model);
                }
            Map<String, String> actionToRemove = new HashMap<>();
            actionToRemove.put("id", model.getId());
            actionToRemove.put("direction", model.getDirection());
            actionToRemove.put("validatorStepId", model.getValidatorStepId());
            actionToRemove.put("parentId",model.getParentId());
            return actionToRemove;
        }).forEach(toRemove::add);
    }

    private static Response executeAction(KeycloakSession session, AuthenticationSessionModel authSession, RequiredActionProviderModel model,
                                          HttpRequest request, EventBuilder event, RealmModel realm, UserModel user, boolean kcActionExecution) {
        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, model.getProviderId());
        if (factory == null) {
            throw new RuntimeException("Unable to find factory for Required Action: " + model.getProviderId() + " did you forget to declare it in a META-INF/services file?");
        }
        RequiredActionContextResult context = new RequiredActionContextResult(authSession, realm, event, session, request, user, factory, model);
        RequiredActionProvider actionProvider = null;
        try {
            actionProvider = createRequiredAction(context);
        } catch (AuthenticationFlowException e) {
            if (e.getResponse() != null) {
                return e.getResponse();
            }
            throw e;
        }

        if (kcActionExecution) {
            if (actionProvider.initiatedActionSupport() == InitiatedActionSupport.NOT_SUPPORTED) {
                logger.debugv("Requested action {0} does not support being invoked with kc_action", factory.getId());
                setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.ERROR, authSession);
                return null;
            } else if (!model.isEnabled()) {
                logger.debugv("Requested action {0} is disabled and can't be invoked with kc_action", factory.getId());
                setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.ERROR, authSession);
                return null;
            } else {
                authSession.setClientNote(Constants.KC_ACTION_EXECUTING, factory.getId());
            }
        }

        actionProvider.requiredActionChallenge(context);

        if (context.getStatus() == RequiredActionContext.Status.FAILURE) {
            LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, context.getAuthenticationSession().getProtocol());
            protocol.setRealm(context.getRealm())
                    .setHttpHeaders(context.getHttpRequest().getHttpHeaders())
                    .setUriInfo(context.getUriInfo())
                    .setEventBuilder(event);
            Response response = protocol.sendError(context.getAuthenticationSession(), Error.CONSENT_DENIED);
            event.error(Errors.REJECTED_BY_USER);
            return response;
        } else if (context.getStatus() == RequiredActionContext.Status.CHALLENGE) {
            authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, model.getProviderId());
            authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION_MODEL_ID, model.getId());
            return context.getChallenge();
        } else if (context.getStatus() == RequiredActionContext.Status.SUCCESS) {
            event.clone().event(EventType.CUSTOM_REQUIRED_ACTION).detail(Details.CUSTOM_REQUIRED_ACTION, factory.getId()).success();
            if(!model.getProviderId().equals("bo-sub-workflow")) {
                // don't have to perform the same action twice, so remove it from both the user and session required actions
                authSession.getAuthenticatedUser().removeRequiredAction(factory.getId());
                authSession.removeRequiredAction(factory.getId());
                // for compatibility with actions mapped by model id's
                authSession.getAuthenticatedUser().removeRequiredAction(model.getId());
                authSession.removeRequiredAction(model.getId());
                setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.SUCCESS, authSession);
            }

        }
        return null;
    }

    private static Map<String, List<RequiredActionProviderModel>> getSubWorkflowActions(KeycloakSession session, RealmModel realm, Stream<String> requiredActions, String actionId, UserModel user) {

        Map<String, List<RequiredActionProviderModel>> subWorkflowActions = new HashMap<>();

         requiredActions.forEach(action -> {
                    Optional<RequiredActionProviderModel> model;

                    model = realm.getRequiredActionProvidersStream(actionId)
                            .filter(actionProvider -> actionProvider.getActionId().equals(actionId))
                            .filter(actionProvider -> actionProvider.getId().equalsIgnoreCase(action) || actionProvider.getAlias().equalsIgnoreCase(action))
                            .findFirst();

                    if(model.isPresent() && model.get().getProviderId().equals("bo-sub-workflow")) {
                        List<RequiredActionProviderModel> actions = realm.getRequiredActionProvidersStream(model.get().getConfig().get("sub-workflow")).collect(Collectors.toList());
                        List<String> userRequiredActions = user.getRequiredActionsStream().collect(Collectors.toList());
                        List<RequiredActionProviderModel> listOfActionsToBeAdded = new ArrayList<>();
                        actions.stream().forEach(act -> {
                            if(userRequiredActions.contains(act.getId())) {
                                listOfActionsToBeAdded.add(act);
                            }
                        });
                        subWorkflowActions.put(action, listOfActionsToBeAdded);

                    }
                });

         return subWorkflowActions;
    }

    private static List<String> getSubWorkflowActionsToBeRemoved(KeycloakSession session, RealmModel realm, Stream<String> requiredActions, RequiredActionProviderModel model, UserModel user) {

            List<RequiredActionProviderModel> actions = realm.getRequiredActionProvidersStream(model.getConfig().get("sub-workflow")).collect(Collectors.toList());
            List<String> userRequiredActions = user.getRequiredActionsStream().collect(Collectors.toList());
            List<String> listOfActionsToBeRemoved = new ArrayList<>();
            actions.stream().forEach(act -> {
                if(userRequiredActions.contains(act.getId())) {
                    listOfActionsToBeRemoved.add(act.getId());
                }
            });
            return listOfActionsToBeRemoved;

    }

    private static Stream<RequiredActionProviderModel> sortRequiredActionsByPriority(KeycloakSession session, RealmModel realm,
                                                                                     Stream<String> requiredActions, String actionId) {
        return requiredActions.map(action -> {
                    Optional<RequiredActionProviderModel> model;

                    RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory()
                            .getProviderFactory(RequiredActionProvider.class, action);
                    if (factory == null) {
                        factory = (RequiredActionFactory) session.getKeycloakSessionFactory()
                                .getProviderFactory(RequiredActionProvider.class, realm.getRequiredActionProviderById(action).getAlias());
                        if (factory == null) {
                            logger.warnv("Could not find configuration for Required Action {0}, did you forget to register it?", action);
                            return null;
                        }
                    }

                    model = realm.getRequiredActionProvidersStream(actionId)
                            .filter(actionProvider -> actionProvider.getActionId().equals(actionId))
                            .filter(actionProvider -> actionProvider.getId().equalsIgnoreCase(action) || actionProvider.getAlias().equalsIgnoreCase(action))
                            .findFirst();

                    if(!model.isPresent() && !factory.isConfigurable()) {
                        model = Optional.ofNullable(realm.getRequiredActionProviderByAlias(action));
                    }
                    if (!model.isPresent()) {
                        logger.warnv("Could not find configuration for Required Action {0}, did you forget to register it?", action);
                        return null;
                    }
                    return model.get();
                })
                .filter(Objects::nonNull)
                .filter(RequiredActionProviderModel::isEnabled)
                .sorted(RequiredActionProviderModel.RequiredActionComparator.SINGLETON);
    }

    public static void evaluateRequiredActionTriggers(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                                      final HttpRequest request, final EventBuilder event,
                                                      final RealmModel realm, final UserModel user, final String actionId) {
        if (actionId == null)
            return;
        authSession.setAuthNote(CURRENT_ACTION_ID, actionId);

        // see if any required actions need triggering against the user, i.e. a qv-licence check, update password ...


        // Cache to store sub-workflow actions for each unique sub-workflow ID
        Map<String, List<RequiredActionProviderModel>> actionsInSubWorkflow = new HashMap<>();

        evaluateActions(session, authSession, request, event, realm, user, actionId, actionsInSubWorkflow);

    }

    private static void evaluateActions(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                        final HttpRequest request, final EventBuilder event, final RealmModel realm,
                                        final UserModel user, final String actionId,
                                        Map<String, List<RequiredActionProviderModel>> actionsInSubWorkflow) {

        // Partition the actions into bo-sub-workflow and regular actions
        realm.getRequiredActionProvidersStream(actionId)
                .filter(RequiredActionProviderModel::isEnabled)
                .forEachOrdered(f -> {
                    // If the action is a bo-sub-workflow
                    if ("bo-sub-workflow".equals(f.getProviderId())) {
                        // Evaluate the main bo-sub-workflow action
                        evaluateRequiredAction(session, authSession, request, event, realm, user, f);

                        // Retrieve sub-workflow actions, using the cache if available
                        String subWorkflowId = f.getConfig().get("sub-workflow");
                        List<RequiredActionProviderModel> subWorkflowActions = actionsInSubWorkflow.computeIfAbsent(subWorkflowId, id ->
                                realm.getRequiredActionProvidersStream(id)
                                        .filter(RequiredActionProviderModel::isEnabled)
                                        .collect(Collectors.toList())
                        );

                        // Recursively evaluate each sub-workflow action
                        subWorkflowActions.forEach(subFlow -> {
                            if("bo-sub-workflow".equals(subFlow.getProviderId())) {
                                evaluateRequiredAction(session, authSession, request, event, realm, user, subFlow);
                                evaluateActions(session, authSession, request, event, realm, user, subFlow.getConfig().get("sub-workflow"), new HashMap<>());
                            } else {
                                evaluateRequiredAction(session, authSession, request, event, realm, user, subFlow);
                            }

                        }
                        );
                    } else {
                        // Directly evaluate non-bo-sub-workflow actions
                        evaluateRequiredAction(session, authSession, request, event, realm, user, f);
                    }
                });
    }

    private static void evaluateRequiredAction(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                               final HttpRequest request, final EventBuilder event, final RealmModel realm,
                                               final UserModel user, RequiredActionProviderModel model) {
        RequiredActionFactory factory = toRequiredActionFactory(session, model);
        if (factory == null) {
            return;
        }
        RequiredActionProvider provider = factory.create(session);
        RequiredActionContextResult result = new RequiredActionContextResult(authSession, realm, event, session, request, user, factory, model) {
            @Override
            public void challenge(Response response) {
                throw new RuntimeException("Not allowed to call challenge() within evaluateTriggers()");
            }

            @Override
            public void failure() {
                throw new RuntimeException("Not allowed to call failure() within evaluateTriggers()");
            }

            @Override
            public void success() {
                throw new RuntimeException("Not allowed to call success() within evaluateTriggers()");
            }

            @Override
            public void ignore() {
                throw new RuntimeException("Not allowed to call ignore() within evaluateTriggers()");
            }
        };

        provider.evaluateTriggers(result);
    }

    private static RequiredActionFactory toRequiredActionFactory(KeycloakSession session, RequiredActionProviderModel model) {
        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(RequiredActionProvider.class, model.getProviderId());
        if (factory == null) {
            logger.warn("Unable to find factory for Required Action: "
                    + model.getProviderId() + " did you forget to declare it in a META-INF/services file? Bypassed/Skipped gracefully.");
            return null;
        }
        return factory;
    }

    public static AuthResult verifyIdentityToken(KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, boolean checkActive, boolean checkTokenType,
                                                 String checkAudience, boolean isCookie, String tokenString, HttpHeaders headers, Predicate<? super AccessToken>... additionalChecks) {
        try {
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenString, AccessToken.class)
                    .withDefaultChecks()
                    .realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()))
                    .checkActive(checkActive)
                    .checkTokenType(checkTokenType)
                    .withChecks(additionalChecks);

            if (checkAudience != null) {
                verifier.audience(checkAudience);
            }

            // Check token revocation in case of access token
            if (checkTokenType) {
                verifier.withChecks(new TokenManager.TokenRevocationCheck(session));
            }

            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();

            SignatureVerifierContext signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
            verifier.verifierContext(signatureVerifier);

            AccessToken token = verifier.verify().getToken();
            if (checkActive) {
                if (!token.isActive() || token.getIssuedAt() < realm.getNotBefore()) {
                    logger.debugf("Identity cookie expired. Token expiration: %d, Current Time: %d. token issued at: %d, realm not before: %d", token.getExp(), Time.currentTime(), token.getIssuedAt(), realm.getNotBefore());
                    return null;
                }
            }

            UserSessionModel userSession = null;
            UserModel user = null;
            if (token.getSessionState() == null) {
                user = TokenManager.lookupUserFromStatelessToken(session, realm, token);
                if (!isUserValid(session, realm, user, token)) {
                    return null;
                }
            } else {
                userSession = session.sessions().getUserSession(realm, token.getSessionState());
                if (userSession != null) {
                    user = userSession.getUser();
                    if (!isUserValid(session, realm, user, token)) {
                        return null;
                    }
                }
            }

            if (token.getSessionState() != null && !isSessionValid(realm, userSession)) {
                // Check if accessToken was for the offline session.
                if (!isCookie) {
                    UserSessionModel offlineUserSession = session.sessions().getOfflineUserSession(realm, token.getSessionState());
                    if (isOfflineSessionValid(realm, offlineUserSession)) {
                        user = offlineUserSession.getUser();
                        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
                        if (!isClientValid(offlineUserSession, client, token)) {
                            return null;
                        }
                        return new AuthResult(user, offlineUserSession, token, client);
                    }
                }

                if (userSession != null)
                    backchannelLogout(session, realm, userSession, uriInfo, connection, headers, true);
                logger.debug("User session not active");
                return null;
            }

            session.setAttribute("state_checker", token.getOtherClaims().get("state_checker"));

            ClientModel client;
            if (isCookie) {
                client = null;
            } else {
                client = realm.getClientByClientId(token.getIssuedFor());
                if (!isClientValid(userSession, client, token)) {
                    return null;
                }
            }
            return new AuthResult(user, userSession, token, client);
        } catch (VerificationException e) {
            logger.debugf("Failed to verify identity token: %s", e.getMessage());
        }
        return null;
    }

    // Verify client and whether clientSession exists
    private static boolean isClientValid(UserSessionModel userSession, ClientModel client, AccessToken token) {
        if (client == null || !client.isEnabled()) {
            logger.debugf("Identity token issued for unknown or disabled client '%s'", token.getIssuedFor());
            return false;
        }

        if (token.getIssuedAt() < client.getNotBefore()) {
            logger.debug("Client notBefore newer than token");
            return false;
        }

        // User session may not exists for example during client credentials auth
        if (userSession == null) return true;

        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
        if (clientSession == null) {
            logger.debugf("Client session for client '%s' not present in user session '%s'", client.getClientId(), userSession.getId());
            return false;
        }
        return true;
    }

    private static boolean isUserValid(KeycloakSession session, RealmModel realm, UserModel user, AccessToken token) {
        if (user == null || !user.isEnabled()) {
            logger.debug("Unknown user in identity token");
            return false;
        }

        int userNotBefore = session.users().getNotBeforeOfUser(realm, user);
        if (token.getIssuedAt() < userNotBefore) {
            logger.debug("User notBefore newer than token");
            return false;
        }

        return true;
    }

    public enum AuthenticationStatus {
        SUCCESS, ACCOUNT_TEMPORARILY_DISABLED, ACCOUNT_DISABLED, ACTIONS_REQUIRED, INVALID_USER, INVALID_CREDENTIALS, MISSING_PASSWORD, MISSING_TOTP, FAILED
    }

    public static class AuthResult {
        private final UserModel user;
        private final UserSessionModel session;
        private final AccessToken token;
        private final ClientModel client;

        public AuthResult(UserModel user, UserSessionModel session, AccessToken token, ClientModel client) {
            this.user = user;
            this.session = session;
            this.token = token;
            this.client = client;
        }

        public UserSessionModel getSession() {
            return session;
        }

        public UserModel getUser() {
            return user;
        }

        public AccessToken getToken() {
            return token;
        }

        public ClientModel getClient() {
            return client;
        }
    }

    public static void setKcActionStatus(String executedProviderId, RequiredActionContext.KcActionStatus status, AuthenticationSessionModel authSession) {
        if (executedProviderId.equals(authSession.getClientNote(Constants.KC_ACTION))) {
            authSession.setClientNote(Constants.KC_ACTION_STATUS, status.name().toLowerCase());
            authSession.removeClientNote(Constants.KC_ACTION);
            authSession.removeClientNote(Constants.KC_ACTION_EXECUTING);
        }
    }

    public static void logSuccess(KeycloakSession session, AuthenticationSessionModel authSession) {
        RealmModel realm = session.getContext().getRealm();
        if (realm.isBruteForceProtected()) {
            UserModel user = lookupUserForBruteForceLog(session, realm, authSession);
            if (user != null) {
                BruteForceProtector bruteForceProtector = session.getProvider(BruteForceProtector.class);
                bruteForceProtector.successfulLogin(realm, user, session.getContext().getConnection());
            }
        }
    }

    public static UserModel lookupUserForBruteForceLog(KeycloakSession session, RealmModel realm, AuthenticationSessionModel authenticationSession) {
        UserModel user = authenticationSession.getAuthenticatedUser();
        if (user != null) return user;

        String username = authenticationSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
        if (username != null) {
            return KeycloakModelUtils.findUserByNameOrEmail(session, realm, username);
        }

        return null;
    }

}

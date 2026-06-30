/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.grants.ciba.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.ciba.CibaNotificationProvider;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;

import java.util.HashMap;
import java.util.Map;

import static org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status.CANCELLED;

public class BackchannelAuthenticationCallbackEndpoint extends AbstractCibaEndpoint {

    private static final Logger logger = Logger.getLogger(BackchannelAuthenticationCallbackEndpoint.class);

    @Context
    private HttpRequest httpRequest;

    public BackchannelAuthenticationCallbackEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
    }

    @Path("/")
    @POST
    @NoCache
    public Response processAuthenticationChannelResult(AuthenticationChannelResponse response) {
        event.event(EventType.EZ_TRANSACTION);
        BackchannelAuthCallbackContext ctx = verifyAuthenticationRequest(httpRequest.getHttpHeaders());
        AccessToken bearerToken = ctx.bearerToken;
        OAuth2DeviceCodeModel deviceModel = ctx.deviceModel;

        Status status = response.getStatus();

        if (status == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid authentication status",
                    Response.Status.BAD_REQUEST);
        }
        boolean isSuccess = false;
        switch (status) {
            case SUCCEED:
                approveRequest(bearerToken, response.getAdditionalParams());
                isSuccess = true;
                break;
            case CANCELLED:
            case UNAUTHORIZED:
                denyRequest(bearerToken, status);
                break;
        }

        // Call the notification endpoint
        ClientModel client = session.getContext().getClient();
        event.client(client.getId());
        CibaConfig cibaConfig = realm.getCibaPolicy();
        String notProviderId = cibaConfig.getBackchannelTokenDeliveryMode(client);
        CibaNotificationProvider cibaNotificationProvider = session.getProvider(CibaNotificationProvider.class, notProviderId);
        CibaNotificationConfigModel configModel = realm.getCibaNotificationConfigByProviderId(notProviderId, client.getClientId(), realm.getId());
        if (isSuccess)
            cibaNotificationProvider.setEventBuilder(event).sendNotification(session, realm, client, cibaConfig, deviceModel, configModel, httpRequest, null);
        else
            cibaNotificationProvider.setEventBuilder(event).sendError(session, realm, client, cibaConfig, deviceModel, configModel, httpRequest, new HashMap<String, String>() {{
                put("error", "access_denied");
                put("error_desc","User is not authorized");
            }});
        /*if (.equals(CibaConfig.CIBA_PING_MODE)) {
            sendClientNotificationRequest(client, cibaConfig, deviceModel);
        }

        if (cibaConfig.getBackchannelTokenDeliveryMode(client).equals(CibaConfig.CIBA_PUSH_MODE)) {
            sendClientPushRequest(client, cibaConfig, deviceModel);
        }*/

        return Response.ok(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private BackchannelAuthCallbackContext verifyAuthenticationRequest(HttpHeaders headers) {
        String rawBearerToken = AppAuthManager.extractAuthorizationHeaderTokenOrReturnNull(headers);

        if (rawBearerToken == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.UNAUTHORIZED);
        }

        AccessToken bearerToken;

        try {
            bearerToken = TokenVerifier.createWithoutSignature(session.tokens().decode(rawBearerToken, AccessToken.class))
                    .withDefaultChecks()
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()))
                    .checkActive(true)
                    .audience(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()))
                    .verify().getToken();
        } catch (Exception e) {
            event.error(Errors.INVALID_TOKEN);
            // authentication channel id format is invalid or it has already been used
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.FORBIDDEN);
        }

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel deviceCode = store.getByUserCode(realm, bearerToken.getId());

        if (deviceCode == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.FORBIDDEN);
        }

        if (!deviceCode.isPending()) {
            cancelRequest(bearerToken.getId());
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.FORBIDDEN);
        }

        ClientModel issuedFor = realm.getClientByClientId(bearerToken.getIssuedFor());

        if (issuedFor == null || !issuedFor.isEnabled()) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid token recipient",
                    Response.Status.BAD_REQUEST);
        }

        if (!deviceCode.getClientId().equals(issuedFor.getClientId())) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Token recipient mismatch",
                    Response.Status.BAD_REQUEST);
        }

        session.getContext().setClient(issuedFor);
        event.client(issuedFor.getClientId());

        return new BackchannelAuthCallbackContext(bearerToken, deviceCode);
    }

    private void cancelRequest(String authResultId) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        OAuth2DeviceCodeModel userCode = store.getByUserCode(realm, authResultId);
        store.removeDeviceCode(realm, userCode.getDeviceCode());
        store.removeUserCode(realm, authResultId);
    }

    private void approveRequest(AccessToken authReqId, Map<String, String> additionalParams) {
        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        store.approve(realm, authReqId.getId(), "fake", additionalParams);
    }

    private void denyRequest(AccessToken authReqId, Status status) {
        if (CANCELLED.equals(status)) {
            event.error(Errors.NOT_ALLOWED);
        } else {
            event.error(Errors.CONSENT_DENIED);
        }

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);

        store.deny(realm, authReqId.getId());
    }

    private class BackchannelAuthCallbackContext {

        private final AccessToken bearerToken;
        private final OAuth2DeviceCodeModel deviceModel;

        private BackchannelAuthCallbackContext(AccessToken bearerToken, OAuth2DeviceCodeModel deviceModel) {
            this.bearerToken = bearerToken;
            this.deviceModel = deviceModel;
        }

    }
}

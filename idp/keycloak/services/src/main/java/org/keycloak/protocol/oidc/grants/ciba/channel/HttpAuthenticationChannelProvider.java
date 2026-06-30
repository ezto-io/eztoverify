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
package org.keycloak.protocol.oidc.grants.ciba.channel;

import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.checkerframework.checker.units.qual.C;
import org.keycloak.ciba.AuthenticationChannelRequest;
import org.keycloak.ciba.CibaAuthProviderFactory;
import org.keycloak.ciba.models.UserData;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resources.Cors;
import org.keycloak.util.TokenUtil;

import static org.keycloak.models.AuthenticationFlowBindings.CIBA_AUTHORIZER;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class HttpAuthenticationChannelProvider implements AuthenticationChannelProvider {

    public static final String AUTHENTICATION_CHANNEL_ID = "authentication_channel_id";

    protected KeycloakSession session;
    protected MultivaluedMap<String, String> formParams;
    protected RealmModel realm;
    protected Map<String, String> clientAuthAttributes;
    protected Cors cors;

    public HttpAuthenticationChannelProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    @Override
    public boolean requestAuthentication(CIBAAuthenticationRequest request, UserData infoUsedByAuthenticator, String requestId, EventBuilder event) {
        // Creates JWT formatted/JWS signed/JWE encrypted Authentication Channel ID by the same manner in creating auth_req_id.
        // Authentication Channel ID binds Backchannel Authentication Request with Authentication by Authentication Device (AD).
        // JWE serialized Authentication Channel ID works as a bearer token. It includes client_id 
        // that can be used on Authentication Channel Callback Endpoint to recognize the Consumption Device (CD)
        // that sent Backchannel Authentication Request.

        // The following scopes should be displayed on AD:
        // 1. scopes specified explicitly as query parameter in the authorization request
        // 2. scopes specified implicitly as default client scope in keycloak

        //checkAuthenticationChannel();

        ClientModel client = request.getClient();

        AuthenticationChannelRequest channelRequest = new AuthenticationChannelRequest();

        channelRequest.setScope(request.getScope());
        channelRequest.setBindingMessage(request.getBindingMessage());
        channelRequest.setLoginHint(infoUsedByAuthenticator);
        channelRequest.setConsentRequired(client.isConsentRequired());
        channelRequest.setAcrValues(request.getAcrValues());
        channelRequest.setAdditionalParameters(request.getOtherClaims());
        channelRequest.getAdditionalParameters().put("requestId",request.getClientNotificationToken());

        String token = createBearerToken(request, client);

        String cibaAuthorizer = client.getAuthenticationFlowBindingOverride(CIBA_AUTHORIZER);

        if (cibaAuthorizer == null) {
            event.error("CIBA Authorizer is not setup properly.");
            throw new RuntimeException("CIBA Authorizer is not setup properly.");
        }

        CibaAuthProvider cibaAuthProvider = session.getProvider(CibaAuthProvider.class, cibaAuthorizer);
        if (cibaAuthorizer == null) {
            event.error("CIBA Authorizer is not found.");
            throw new RuntimeException("CIBA Authorizer is not found.");
        }

        UserModel userModel = session.users().getUserById(realm, infoUsedByAuthenticator.getUserId());
        if (userModel == null) {
            event.error("User is not found");
            throw new RuntimeException("User is not found");
        }
        CibaStateModel cibaStateModel = new CibaStateModel();
        cibaStateModel.setCibaToken(token);
        cibaStateModel.setUserId(userModel.getUsername());
        cibaStateModel.setRequestId(request.getClientNotificationToken());
        cibaStateModel = session.getContext().getRealm().addCibaState(cibaStateModel);

        CibaAuthConfigModel cibaAuthConfigModel = session.getContext().getRealm().getCibaAuthConfigByProviderId(cibaAuthorizer, client.getClientId(),realm.getId());
        cibaAuthProvider.setEventBuilder(event);
        return cibaAuthProvider.onAuthorize(client,realm,userModel, cibaAuthConfigModel, channelRequest, cibaStateModel);
    }

    private String createBearerToken(CIBAAuthenticationRequest request, ClientModel client) {
        AccessToken bearerToken = new AccessToken();

        bearerToken.type(TokenUtil.TOKEN_TYPE_BEARER);
        bearerToken.issuer(request.getIssuer());
        bearerToken.id(request.getAuthResultId());
        bearerToken.issuedFor(client.getClientId());
        bearerToken.audience(request.getIssuer());
        bearerToken.exp(request.getExp());
        bearerToken.subject(request.getSubject());

        return session.tokens().encode(bearerToken);
    }

   /* protected void checkAuthenticationChannel() {
        if (httpAuthenticationChannelUri == null) {
            throw new RuntimeException("Authentication Channel Request URI not set properly.");
        }
        if (!httpAuthenticationChannelUri.startsWith("http://") && !httpAuthenticationChannelUri.startsWith("https://")) {
            throw new RuntimeException("Authentication Channel Request URI not set properly.");
        }
    }*/

    /**
     * Extension point to allow subclass to override this method in order to add data to post to decoupled server.
     */
    /*protected SimpleHttp completeDecoupledAuthnRequest(SimpleHttp simpleHttp, AuthenticationChannelRequest channelRequest) {
        return simpleHttp;
    }*/
    @Override
    public void close() {

    }
}

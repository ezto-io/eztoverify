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

package org.keycloak.protocol.oidc.grants.ciba.endpoints.request;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorResponseException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class BackchannelAuthenticationEndpointRequestParserProcessor {

    public static BackchannelAuthenticationEndpointRequest parseRequest(KeycloakSession session, ClientModel client, MultivaluedMap<String, String> requestParams, CibaConfig config, EventBuilder event) {
        try {
            BackchannelAuthenticationEndpointRequest request = new BackchannelAuthenticationEndpointRequest();

            BackchannelAuthenticationEndpointRequestBodyParser parser = new BackchannelAuthenticationEndpointRequestBodyParser(requestParams,session.getContext().getRealm());
            parser.parseRequest(request,client);

            if (parser.getInvalidRequestMessage() != null) {
                request.invalidRequestMessage = parser.getInvalidRequestMessage();
                return request;
            }

            return request;

        } catch (IllegalArgumentException e){
            throw new IllegalArgumentException(e);
        } catch (ErrorResponseException e) {
            event.error(e.getErrorDescription());
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, e.getErrorDescription(), Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }
}

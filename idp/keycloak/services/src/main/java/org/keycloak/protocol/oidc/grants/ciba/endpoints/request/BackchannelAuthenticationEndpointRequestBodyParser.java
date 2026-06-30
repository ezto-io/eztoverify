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

import io.swagger.v3.core.util.Json;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.OAuthErrorException;
import org.keycloak.ciba.models.BindingMessage;
import org.keycloak.ciba.models.TransactionRequest;
import org.keycloak.ciba.models.UserData;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.ComponentResource;
import org.keycloak.services.util.RSAHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.AESHelper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parse the parameters from request body
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
class BackchannelAuthenticationEndpointRequestBodyParser extends BackchannelAuthenticationEndpointRequestParser {

    private final MultivaluedMap<String, String> requestParams;

    private String invalidRequestMessage = null;
    private RealmModel realmModel;

    public BackchannelAuthenticationEndpointRequestBodyParser(MultivaluedMap<String, String> requestParams, RealmModel realmModel) {
        this.requestParams = requestParams;
        this.realmModel = realmModel;
    }

    @Override
    protected String getParameter(String paramName) {
        checkDuplicated(requestParams, paramName);
        return requestParams.getFirst(paramName);
    }

    @Override
    protected BindingMessage getBindingMessage(String paramName) throws IOException {
        String param = requestParams.getFirst(paramName);
        if (param == null)
            return BindingMessage.newMessage(realmModel);

        return JsonSerialization.readValue(param, BindingMessage.class);
    }

    @Override
    protected UserData getUser(String paramName) throws IOException {
        String param = requestParams.getFirst(paramName);
        if (param == null)
            return new UserData();

        return JsonSerialization.readValue(param, UserData.class);
    }

    @Override
    protected Integer getIntParameter(String paramName) {
        checkDuplicated(requestParams, paramName);
        String paramVal = requestParams.getFirst(paramName);
        return paramVal == null ? null : Integer.valueOf(paramVal);
    }

    public String getInvalidRequestMessage() {
        return invalidRequestMessage;
    }

    @Override
    protected Set<String> keySet() {
        return requestParams.keySet();
    }

    @Override
    protected TransactionRequest getRequest(String paramName, ClientModel clientModel) throws IOException {
        String param = requestParams.getFirst(paramName);
        if (param == null)
            return TransactionRequest.newMessage(realmModel);

        String isEncryptionEnabled = clientModel.getAttribute("enable_ciba_request_encryption");
        if (isEncryptionEnabled != null && Boolean.parseBoolean(isEncryptionEnabled)) {
            ComponentModel componentModel = realmModel.getComponent(clientModel.getAttribute("ciba_encryption_id"));
            if (componentModel == null)
                throw new IOException("Encryption key is not set");

            try {
                param = AESHelper.decrypt(param, componentModel.get("secret"));
            } catch (IllegalArgumentException e){
                throw new IllegalArgumentException("Encryption is enabled under developers tab, but the request body is not encrypted.");
            }catch (Exception e) {
                throw new IOException(e.getMessage());
            }
        }
        try {
            JSONObject requestJson = new JSONObject(param);
        } catch (JSONException e) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, e.getMessage(), Response.Status.BAD_REQUEST);
        }

        return JsonSerialization.readValue(param, TransactionRequest.class);
    }

    private void checkDuplicated(MultivaluedMap<String, String> requestParams, String paramName) {
        if (invalidRequestMessage == null) {
            if (requestParams.get(paramName) != null) {
                invalidRequestMessage = "duplicated parameter";
            }
        }
    }

}

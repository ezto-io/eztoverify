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
package org.keycloak.forms.login.freemarker.model;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.OrderedModel;
import org.keycloak.rar.AuthorizationDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantBean {

    private static OrderedModel.OrderedModelComparator<ClientScopeEntry> COMPARATOR_INSTANCE = new OrderedModel.OrderedModelComparator<>();

    private List<ClientScopeEntry> clientScopesRequested = new ArrayList<>();
    private List<ClientScopeEntry> optionalClientScopes = new ArrayList<>();
    private String code;
    private ClientModel client;
    private ClientScopeEntry clientRootScope;

    public OAuthGrantBean(String code, ClientModel client, List<AuthorizationDetails> clientScopesRequested) {
        this.code = code;
        this.client = client;
        Map<String, ClientScopeModel> noDefaultClientScopes = client.getClientScopes(false, false);
        for (AuthorizationDetails clientScope : clientScopesRequested) {
            if (clientScope.getClientScope().getName().equals(client.getName())) {
                clientRootScope = new ClientScopeEntry(clientScope.getClientScope().getId(), clientScope.getClientScope().getConsentScreenText(), clientScope.getClientScope().getDescription(), clientScope.getClientScope().getGuiOrder(), false,clientScope);
            } else {
                boolean isDefault = !noDefaultClientScopes.containsKey(clientScope.getClientScope().getName());
                if (isDefault) {
                    this.clientScopesRequested.add(new ClientScopeEntry(clientScope.getClientScope().getId(),clientScope.getClientScope().getConsentScreenText(), clientScope.getClientScope().getDescription(), clientScope.getClientScope().getGuiOrder(), isDefault,clientScope));
                } else {
                    this.optionalClientScopes.add(new ClientScopeEntry(clientScope.getClientScope().getId(),clientScope.getClientScope().getConsentScreenText(), clientScope.getClientScope().getDescription(), clientScope.getClientScope().getGuiOrder(), isDefault,clientScope));
                }
            }
        }

        this.clientScopesRequested.sort(COMPARATOR_INSTANCE);
        this.optionalClientScopes.sort(COMPARATOR_INSTANCE);
    }

    public String getCode() {
        return code;
    }

    public ClientScopeEntry getClientRootScope() {
        return clientRootScope;
    }

    public String getClient() {
        return client.getClientId();
    }

    public String getClientName(){
        return client.getName();
    }


    public List<ClientScopeEntry> getClientScopesRequested() {
        return clientScopesRequested;
    }

    public List<ClientScopeEntry> getOptionalClientScopes() {
        return optionalClientScopes;
    }

    // Converting ClientScopeModel due the freemarker limitations. It's not able to read "getConsentScreenText" default method defined on interface
    public static class ClientScopeEntry implements OrderedModel {

        private final String consentScreenText;
        private final String description;
        private final String guiOrder;
        private final String isDefault;
        private final String id;
        private final String dynamicScopeParameter;

        public ClientScopeEntry(String id, String consentScreenText, String description, String guiOrder, boolean isDefault, AuthorizationDetails authorizationDetails) {
            this.id = id;
            this.consentScreenText = consentScreenText;
            this.guiOrder = guiOrder;
            this.dynamicScopeParameter = authorizationDetails.getDynamicScopeParam();
            this.description = description;
            this.isDefault = String.valueOf(isDefault);
        }


        public String getDescription() {
            return description;
        }

        public String getConsentScreenText() {
            return consentScreenText;
        }

        @Override
        public String getGuiOrder() {
            return guiOrder;
        }

        public String getDynamicScopeParameter() {
            return dynamicScopeParameter;
        }

        public String getIsDefault() {
            return isDefault;
        }

        public String getId() {
            return id;
        }
    }
}

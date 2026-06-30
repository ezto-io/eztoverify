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
 */

package org.keycloak.ciba;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.OAuth2Constants;
import org.keycloak.ciba.models.BindingMessage;
import org.keycloak.ciba.models.UserData;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthenticationChannelRequest {

    @JsonProperty("binding_message")
    private BindingMessage bindingMessage;

    @JsonProperty("login_hint")
    private UserData loginHint;

    @JsonProperty("is_consent_required")
    private Boolean consentRequired;

    @JsonProperty(OAuth2Constants.ACR_VALUES)
    private String acrValues;

    private Map<String, Object> additionalParameters = new HashMap<>();

    private String scope;

    public BindingMessage getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(BindingMessage bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public void setLoginHint(UserData loginHint) {
        this.loginHint = loginHint;
    }

    public UserData getLoginHint() {
        return loginHint;
    }

    public void setConsentRequired(Boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public Boolean getConsentRequired() {
        return consentRequired;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(Map<String, Object> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    @JsonAnySetter
    public void setAdditionalParameter(String name, String value) {
        additionalParameters.put(name, value);
    }
}

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

package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedClient extends AbstractRevisioned implements InRealm {
    protected String clientId;
    protected String name;
    protected String description;
    protected String realm;
    protected Set<String> redirectUris = new HashSet<>();
    protected boolean enabled;
    protected boolean alwaysDisplayInConsole;
    protected String clientAuthenticatorType;
    protected String secret;
    protected String registrationToken;
    protected String protocol;
    protected Map<String, String> attributes = new HashMap<>();
    protected Map<String, String> authFlowBindings = new HashMap<>();
    protected Map<String, Boolean> appConnections = new HashMap<>();
    protected Map<String, Boolean> mfaConnections = new HashMap<>();
    protected boolean publicClient;
    protected boolean fullScopeAllowed;
    protected boolean frontchannelLogout;
    protected int notBefore;
    protected Set<String> scope = new HashSet<>();
    protected Set<String> webOrigins = new HashSet<>();
    protected Set<ProtocolMapperModel> protocolMappers = new HashSet<>();
    protected boolean surrogateAuthRequired;
    protected String managementUrl;
    protected String rootUrl;
    protected String baseUrl;
    protected List<String> defaultRoles = new LinkedList<>();
    protected boolean bearerOnly;
    protected boolean consentRequired;
    protected boolean standardFlowEnabled;
    protected boolean implicitFlowEnabled;
    protected boolean directAccessGrantsEnabled;
    protected boolean serviceAccountsEnabled;
    protected int nodeReRegistrationTimeout;
    protected Map<String, Integer> registeredNodes;
    protected List<String> defaultClientScopesIds;
    protected List<String> optionalClientScopesIds;
    protected String mfaPolicyId;
    protected String mfaPolicyType;
    protected Boolean registrationAllowed;
    protected Boolean resetPasswordAllowed;
    protected Boolean rememberMe;
    protected Boolean overrideAuthentication;
    protected Long lastUpdatedTimestamp;
    protected Boolean sandboxMode;
    protected String sandboxClientId;
    protected String sandboxSecret;

    public CachedClient(Long revision, RealmModel realm, ClientModel model) {
        super(revision, model.getId());
        clientAuthenticatorType = model.getClientAuthenticatorType();
        secret = model.getSecret();
        registrationToken = model.getRegistrationToken();
        clientId = model.getClientId();
        name = model.getName();
        description = model.getDescription();
        this.realm = realm.getId();
        enabled = model.isEnabled();
        alwaysDisplayInConsole = model.isAlwaysDisplayInConsole();
        protocol = model.getProtocol();
        attributes.putAll(model.getAttributes());
        authFlowBindings.putAll(model.getAuthenticationFlowBindingOverrides());
        appConnections.putAll(model.getApplicationsConnections());
        mfaConnections.putAll(model.getMFAConnections());
        notBefore = model.getNotBefore();
        frontchannelLogout = model.isFrontchannelLogout();
        publicClient = model.isPublicClient();
        fullScopeAllowed = model.isFullScopeAllowed();
        redirectUris.addAll(model.getRedirectUris());
        webOrigins.addAll(model.getWebOrigins());
        scope.addAll(model.getScopeMappingsStream().map(RoleModel::getId).collect(Collectors.toSet()));
        protocolMappers.addAll(model.getProtocolMappersStream().collect(Collectors.toSet()));
        surrogateAuthRequired = model.isSurrogateAuthRequired();
        managementUrl = model.getManagementUrl();
        rootUrl = model.getRootUrl();
        baseUrl = model.getBaseUrl();
        defaultRoles.addAll(model.getDefaultRolesStream().collect(Collectors.toList()));
        bearerOnly = model.isBearerOnly();
        consentRequired = model.isConsentRequired();
        standardFlowEnabled = model.isStandardFlowEnabled();
        implicitFlowEnabled = model.isImplicitFlowEnabled();
        directAccessGrantsEnabled = model.isDirectAccessGrantsEnabled();
        serviceAccountsEnabled = model.isServiceAccountsEnabled();

        nodeReRegistrationTimeout = model.getNodeReRegistrationTimeout();
        registeredNodes = new TreeMap<>(model.getRegisteredNodes());

        defaultClientScopesIds = new LinkedList<>();
        for (ClientScopeModel clientScope : model.getClientScopes(true).values()) {
            defaultClientScopesIds.add(clientScope.getId());
        }
        optionalClientScopesIds = new LinkedList<>();
        for (ClientScopeModel clientScope : model.getClientScopes(false).values()) {
            optionalClientScopesIds.add(clientScope.getId());
        }

        mfaPolicyId = model.getMfaPolicyId();
        mfaPolicyType = model.getMfaPolicyType() == null ? "action" : model.getMfaPolicyType();
        registrationAllowed = model.isRegistrationAllowed();
        resetPasswordAllowed = model.isResetPasswordAllowed();
        rememberMe = model.isRememberMe();
        overrideAuthentication = model.isOverrideAuthentication();
        lastUpdatedTimestamp = model.getLastUpdatedTimestamp();
        sandboxMode = model.isSandboxMode();
        sandboxClientId = model.getSandboxClientId();
        sandboxSecret = model.getSandboxSecret();
    }

    public void updateCachedClient(ClientModel model){
        clientAuthenticatorType = model.getClientAuthenticatorType();
        secret = model.getSecret();
        registrationToken = model.getRegistrationToken();
        clientId = model.getClientId();
        name = model.getName();
        description = model.getDescription();
        enabled = model.isEnabled();
        alwaysDisplayInConsole = model.isAlwaysDisplayInConsole();
        protocol = model.getProtocol();

        attributes.clear();
        attributes.putAll(model.getAttributes());

        authFlowBindings.clear();
        authFlowBindings.putAll(model.getAuthenticationFlowBindingOverrides());

        appConnections.clear();
        appConnections.putAll(model.getApplicationsConnections());

        mfaConnections.clear();
        mfaConnections.putAll(model.getMFAConnections());

        notBefore = model.getNotBefore();
        frontchannelLogout = model.isFrontchannelLogout();
        publicClient = model.isPublicClient();
        fullScopeAllowed = model.isFullScopeAllowed();

        redirectUris.clear();
        redirectUris.addAll(model.getRedirectUris());

        webOrigins.clear();
        webOrigins.addAll(model.getWebOrigins());

        scope.clear();
        scope.addAll(model.getScopeMappingsStream().map(RoleModel::getId).collect(Collectors.toSet()));

        protocolMappers.clear();
        protocolMappers.addAll(model.getProtocolMappersStream().collect(Collectors.toSet()));

        surrogateAuthRequired = model.isSurrogateAuthRequired();
        managementUrl = model.getManagementUrl();
        rootUrl = model.getRootUrl();
        baseUrl = model.getBaseUrl();

        defaultRoles.clear();
        defaultRoles.addAll(model.getDefaultRolesStream().collect(Collectors.toList()));

        bearerOnly = model.isBearerOnly();
        consentRequired = model.isConsentRequired();
        standardFlowEnabled = model.isStandardFlowEnabled();
        implicitFlowEnabled = model.isImplicitFlowEnabled();
        directAccessGrantsEnabled = model.isDirectAccessGrantsEnabled();
        serviceAccountsEnabled = model.isServiceAccountsEnabled();

        nodeReRegistrationTimeout = model.getNodeReRegistrationTimeout();
        registeredNodes = new TreeMap<>(model.getRegisteredNodes());

        defaultClientScopesIds.clear();
        for (ClientScopeModel clientScope : model.getClientScopes(true).values()) {
            defaultClientScopesIds.add(clientScope.getId());
        }

        optionalClientScopesIds.clear();
        for (ClientScopeModel clientScope : model.getClientScopes(false).values()) {
            optionalClientScopesIds.add(clientScope.getId());
        }

        mfaPolicyId = model.getMfaPolicyId();
        mfaPolicyType = model.getMfaPolicyType() == null ? "action" : model.getMfaPolicyType();
        registrationAllowed = model.isRegistrationAllowed();
        resetPasswordAllowed = model.isResetPasswordAllowed();
        rememberMe = model.isRememberMe();
        overrideAuthentication = model.isOverrideAuthentication();
        lastUpdatedTimestamp = model.getLastUpdatedTimestamp();
        sandboxMode = model.isSandboxMode();
        sandboxClientId = model.getSandboxClientId();
        sandboxSecret = model.getSandboxSecret();
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRealm() {
        return realm;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAlwaysDisplayInConsole() {
        return alwaysDisplayInConsole;
    }

    public String getClientAuthenticatorType() {
        return clientAuthenticatorType;
    }

    public String getSecret() {
        return secret;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public Set<String> getScope() {
        return scope;
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public String getProtocol() {
        return protocol;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public Set<ProtocolMapperModel> getProtocolMappers() {
        return protocolMappers;
    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public boolean isStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public boolean isImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public boolean isDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public Map<String, Integer> getRegisteredNodes() {
        return registeredNodes;
    }

    public List<String> getDefaultClientScopesIds() {
        return defaultClientScopesIds;
    }

    public List<String> getOptionalClientScopesIds() {
        return optionalClientScopesIds;
    }

    public Map<String, String> getAuthFlowBindings() {
        return authFlowBindings;
    }

    public Map<String,Boolean> getAppConnections() { return  appConnections; }

    public Map<String, Boolean> getMfaConnections() {
        return mfaConnections;
    }

    public String getMfaPolicyId() {
        return mfaPolicyId;
    }

    public String getMfaPolicyType() {
        return mfaPolicyType;
    }

    public Boolean getRegistrationAllowed() {
        return registrationAllowed;
    }

    public void setRegistrationAllowed(Boolean registrationAllowed) {
        this.registrationAllowed = registrationAllowed;
    }

    public Boolean getResetPasswordAllowed() {
        return resetPasswordAllowed;
    }

    public void setResetPasswordAllowed(Boolean resetPasswordAllowed) {
        this.resetPasswordAllowed = resetPasswordAllowed;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
    public Boolean getOverrideAuthentication() {
        return overrideAuthentication;
    }

    public void setOverrideAuthentication(Boolean overrideAuthentication) {
        this.overrideAuthentication = overrideAuthentication;
    }

    public Long getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(Long lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    public Boolean getSandboxMode() {
        return sandboxMode;
    }

    public void setSandboxMode(Boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    public String getSandboxClientId() {
        return sandboxClientId;
    }

    public void setSandboxClientId(String sandboxClientId) {
        this.sandboxClientId = sandboxClientId;
    }

    public String getSandboxSecret() {
        return sandboxSecret;
    }

    public void setSandboxSecret(String sandboxSecret) {
        this.sandboxSecret = sandboxSecret;
    }
}

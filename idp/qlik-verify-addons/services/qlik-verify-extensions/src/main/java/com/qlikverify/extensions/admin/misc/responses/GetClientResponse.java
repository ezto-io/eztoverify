package com.qlikverify.extensions.admin.misc.responses;

import org.keycloak.models.ProtocolMapperModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GetClientResponse {
    private String appName;
    private String id;
    private String name;
    private boolean enabled;
    private String appAuthenticatorType;
    private boolean bearerOnly;
    private boolean consentRequired;
    private boolean standardFlowEnabled;
    private boolean implicitFlowEnabled;
    private boolean directAccessGrantsEnabled;
    private boolean serviceAccountsEnabled;
    private boolean publicApp;
    private Map<String, String> authenticationFlowBindingOverrides;
    private Map<String, String> attributes;
    private Set<String> webOrigins;
    private String baseUrl;
    private String rootUrl;
    private List<String> defaultRoles;
    private String description;
    private boolean frontchannelLogout;
    private boolean fullScopeAllowed;
    private int nodeReRegistrationTimeout;
    private int notBefore;
    private String protocol;
    private Set<ProtocolMapperModel> protocolMappers;
    private Set<String> redirectUris;
    private Map<String, Integer> registeredNodes;
    private String registrationAccessToken;
    private boolean surrogateAuthRequired;

    public GetClientResponse(String appName, String id, String name, boolean enabled, String appAuthenticatorType, boolean bearerOnly, boolean consentRequired, boolean standardFlowEnabled, boolean implicitFlowEnabled, boolean directAccessGrantsEnabled, boolean serviceAccountsEnabled, boolean publicApp, Map<String, String> authenticationFlowBindingOverrides, Map<String, String> attributes, Set<String> webOrigins, String baseUrl, String rootUrl, List<String> defaultRoles, String description, boolean frontchannelLogout, boolean fullScopeAllowed, int nodeReRegistrationTimeout, int notBefore, String protocol, Set<ProtocolMapperModel> protocolMappers, Set<String> redirectUris, Map<String, Integer> registeredNodes, String registrationAccessToken, boolean surrogateAuthRequired) {
        this.appName = appName;
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.appAuthenticatorType = appAuthenticatorType;
        this.bearerOnly = bearerOnly;
        this.consentRequired = consentRequired;
        this.standardFlowEnabled = standardFlowEnabled;
        this.implicitFlowEnabled = implicitFlowEnabled;
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
        this.serviceAccountsEnabled = serviceAccountsEnabled;
        this.publicApp = publicApp;
        this.authenticationFlowBindingOverrides = authenticationFlowBindingOverrides;
        this.attributes = attributes;
        this.webOrigins = webOrigins;
        this.baseUrl = baseUrl;
        this.rootUrl = rootUrl;
        this.defaultRoles = defaultRoles;
        this.description = description;
        this.frontchannelLogout = frontchannelLogout;
        this.fullScopeAllowed = fullScopeAllowed;
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
        this.notBefore = notBefore;
        this.protocol = protocol;
        this.protocolMappers = protocolMappers;
        this.redirectUris = redirectUris;
        this.registeredNodes = registeredNodes;
        this.registrationAccessToken = registrationAccessToken;
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAppAuthenticatorType() {
        return appAuthenticatorType;
    }

    public void setAppAuthenticatorType(String appAuthenticatorType) {
        this.appAuthenticatorType = appAuthenticatorType;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public boolean isStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        this.standardFlowEnabled = standardFlowEnabled;
    }

    public boolean isImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        this.implicitFlowEnabled = implicitFlowEnabled;
    }

    public boolean isDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    public boolean isPublicApp() {
        return publicApp;
    }

    public void setPublicApp(boolean publicApp) {
        this.publicApp = publicApp;
    }

    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return authenticationFlowBindingOverrides;
    }

    public void setAuthenticationFlowBindingOverrides(Map<String, String> authenticationFlowBindingOverrides) {
        this.authenticationFlowBindingOverrides = authenticationFlowBindingOverrides;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(Set<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public void setFrontchannelLogout(boolean frontchannelLogout) {
        this.frontchannelLogout = frontchannelLogout;
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(boolean fullScopeAllowed) {
        this.fullScopeAllowed = fullScopeAllowed;
    }

    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public void setNodeReRegistrationTimeout(int nodeReRegistrationTimeout) {
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Set<ProtocolMapperModel> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(Set<ProtocolMapperModel> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public Map<String, Integer> getRegisteredNodes() {
        return registeredNodes;
    }

    public void setRegisteredNodes(Map<String, Integer> registeredNodes) {
        this.registeredNodes = registeredNodes;
    }

    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }
}

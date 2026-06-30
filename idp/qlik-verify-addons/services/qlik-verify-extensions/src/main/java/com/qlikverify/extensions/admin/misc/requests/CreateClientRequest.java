package com.qlikverify.extensions.admin.misc.requests;

import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

import java.util.List;
import java.util.Map;

public class CreateClientRequest {
    private String appName;
    protected String id;
    protected String clientId;
    protected String name;
    protected String description;
    protected String rootUrl;
    protected String adminUrl;
    protected String baseUrl;
    protected Boolean surrogateAuthRequired;
    protected Boolean enabled;
    protected String appAuthenticatorType;
    protected String secret;
    protected String registrationAccessToken;
    protected String[] defaultRoles;
    protected List<String> redirectUris;
    protected List<String> webOrigins;
    protected Integer notBefore;
    protected Boolean bearerOnly;
    protected Boolean consentRequired;
    protected Boolean standardFlowEnabled;
    protected Boolean implicitFlowEnabled;
    protected Boolean directAccessGrantsEnabled;
    protected Boolean serviceAccountsEnabled;
    protected Boolean authorizationServicesEnabled;
    @Deprecated
    protected Boolean directGrantsOnly;
    protected Boolean publicApp;
    protected Boolean frontchannelLogout;
    protected String protocol;
    protected Map<String, String> attributes;
    protected Map<String, String> authenticationFlowBindingOverrides;
    protected Boolean fullScopeAllowed;
    protected Integer nodeReRegistrationTimeout;
    protected Map<String, Integer> registeredNodes;
    protected List<ProtocolMapperRepresentation> protocolMappers;

    @Deprecated
    protected String appTemplate;
    @Deprecated
    private Boolean useTemplateConfig;
    @Deprecated
    private Boolean useTemplateScope;
    @Deprecated
    private Boolean useTemplateMappers;

    protected List<String> defaultClientScopes;
    protected List<String> optionalClientScopes;

    private ResourceServerRepresentation authorizationSettings;
    private Map<String, Boolean> access;
    protected String origin;

    public CreateClientRequest() {
        serviceAccountsEnabled = true;
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Boolean getSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(Boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getAppAuthenticatorType() {
        return appAuthenticatorType;
    }

    public void setAppAuthenticatorType(String appAuthenticatorType) {
        this.appAuthenticatorType = appAuthenticatorType;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

    public String[] getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(String[] defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(List<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    public Integer getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Integer notBefore) {
        this.notBefore = notBefore;
    }

    public Boolean getBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(Boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    public Boolean getConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(Boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public Boolean getStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public void setStandardFlowEnabled(Boolean standardFlowEnabled) {
        this.standardFlowEnabled = standardFlowEnabled;
    }

    public Boolean getImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public void setImplicitFlowEnabled(Boolean implicitFlowEnabled) {
        this.implicitFlowEnabled = implicitFlowEnabled;
    }

    public Boolean getDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public void setDirectAccessGrantsEnabled(Boolean directAccessGrantsEnabled) {
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
    }

    public Boolean getServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public void setServiceAccountsEnabled(Boolean serviceAccountsEnabled) {
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    public Boolean getAuthorizationServicesEnabled() {
        return authorizationServicesEnabled;
    }

    public void setAuthorizationServicesEnabled(Boolean authorizationServicesEnabled) {
        this.authorizationServicesEnabled = authorizationServicesEnabled;
    }

    @Deprecated
    public Boolean getDirectGrantsOnly() {
        return directGrantsOnly;
    }

    @Deprecated
    public void setDirectGrantsOnly(Boolean directGrantsOnly) {
        this.directGrantsOnly = directGrantsOnly;
    }

    public Boolean getPublicApp() {
        return publicApp;
    }

    public void setPublicApp(Boolean publicApp) {
        this.publicApp = publicApp;
    }

    public Boolean getFrontchannelLogout() {
        return frontchannelLogout;
    }

    public void setFrontchannelLogout(Boolean frontchannelLogout) {
        this.frontchannelLogout = frontchannelLogout;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return authenticationFlowBindingOverrides;
    }

    public void setAuthenticationFlowBindingOverrides(Map<String, String> authenticationFlowBindingOverrides) {
        this.authenticationFlowBindingOverrides = authenticationFlowBindingOverrides;
    }

    public Boolean getFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(Boolean fullScopeAllowed) {
        this.fullScopeAllowed = fullScopeAllowed;
    }

    public Integer getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public void setNodeReRegistrationTimeout(Integer nodeReRegistrationTimeout) {
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
    }

    public Map<String, Integer> getRegisteredNodes() {
        return registeredNodes;
    }

    public void setRegisteredNodes(Map<String, Integer> registeredNodes) {
        this.registeredNodes = registeredNodes;
    }

    public List<ProtocolMapperRepresentation> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(List<ProtocolMapperRepresentation> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    @Deprecated
    public String getAppTemplate() {
        return appTemplate;
    }

    @Deprecated
    public void setAppTemplate(String appTemplate) {
        this.appTemplate = appTemplate;
    }

    public Boolean getUseTemplateConfig() {
        return useTemplateConfig;
    }

    @Deprecated
    public void setUseTemplateConfig(Boolean useTemplateConfig) {
        this.useTemplateConfig = useTemplateConfig;
    }

    @Deprecated
    public Boolean getUseTemplateScope() {
        return useTemplateScope;
    }

    @Deprecated
    public void setUseTemplateScope(Boolean useTemplateScope) {
        this.useTemplateScope = useTemplateScope;
    }

    @Deprecated
    public Boolean getUseTemplateMappers() {
        return useTemplateMappers;
    }

    @Deprecated
    public void setUseTemplateMappers(Boolean useTemplateMappers) {
        this.useTemplateMappers = useTemplateMappers;
    }

    public List<String> getDefaultClientScopes() {
        return defaultClientScopes;
    }

    public void setDefaultClientScopes(List<String> defaultClientScopes) {
        this.defaultClientScopes = defaultClientScopes;
    }

    public List<String> getOptionalClientScopes() {
        return optionalClientScopes;
    }

    public void setOptionalClientScopes(List<String> optionalClientScopes) {
        this.optionalClientScopes = optionalClientScopes;
    }

    public ResourceServerRepresentation getAuthorizationSettings() {
        return authorizationSettings;
    }

    public void setAuthorizationSettings(ResourceServerRepresentation authorizationSettings) {
        this.authorizationSettings = authorizationSettings;
    }

    public Map<String, Boolean> getAccess() {
        return access;
    }

    public void setAccess(Map<String, Boolean> access) {
        this.access = access;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}

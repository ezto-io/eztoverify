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

package org.keycloak.representations.info;

import org.keycloak.representations.idm.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ServerInfoRepresentation {

    private SystemInfoRepresentation systemInfo;
    private MemoryInfoRepresentation memoryInfo;
    private ProfileInfoRepresentation profileInfo;
    private BrandRepresentation brandInfo;
    private String rootAccountName;
    private String variant;
    private String debuggerUrl;
    private boolean isCibaFeatureEnabled;
    private boolean isIWSEnabled;
    private boolean isOIDCEnabled;
    private boolean thirdPartyReqEnabled;
    private Map<String, List<ThemeInfoRepresentation>> themes;
    private List<Map<String, String>> socialProviders;
    private List<Map<String, String>> identityProviders;
    private List<Map<String, String>> clientImporters;
    private Map<String, SpiInfoRepresentation> providers;
    private Map<String, List<ProtocolMapperTypeRepresentation>> protocolMapperTypes;
    private Map<String, List<ProtocolMapperRepresentation>> builtinProtocolMappers;
    private Map<String, List<ClientInstallationRepresentation>> clientInstallations;
    private Map<String, List<ComponentTypeRepresentation>> componentTypes;

    private List<PasswordPolicyTypeRepresentation> passwordPolicies;

    private List<UsernamePolicyTypeRepresentation> usernamePolicies;

    private List<ActionPolicyTypeRepresentation> actionPolicies;

    private List<EventListenerRepresentation> eventListeners;

    private List<EmailProviderRepresentation> emailProviders;

    private List<CibaAuthProviderRepresentation> cibaAuthProviders;

    private List<CibaNotificationProviderRepresentation> cibaNotificationProviders;
    private List<CibaDeadLetterHookNotificationProviderRepresentation> cibaDeadLetterHookProviders;

    private Map<String, List<SmsProviderRepresentation>> smsProviders;

    private List<MFAProviderRepresentation> mfaProviders;

    private Map<String, List<String>> enums;

    private List<String> cibaKeyTypes;

    private Map<String, List<String>> cibaComparators;

    private boolean showSecuritySettings;

    private Set<String> supportedLocales;

    public List<String> getCibaKeyTypes() {
        return cibaKeyTypes;
    }

    public void setCibaKeyTypes(List<String> cibaKeyTypes) {
        this.cibaKeyTypes = cibaKeyTypes;
    }

    public Map<String, List<String>> getCibaComparators() {
        return cibaComparators;
    }

    public void setCibaComparators(Map<String, List<String>> cibaComparators) {
        this.cibaComparators = cibaComparators;
    }

    public SystemInfoRepresentation getSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(SystemInfoRepresentation systemInfo) {
        this.systemInfo = systemInfo;
    }

    public MemoryInfoRepresentation getMemoryInfo() {
        return memoryInfo;
    }

    public void setMemoryInfo(MemoryInfoRepresentation memoryInfo) {
        this.memoryInfo = memoryInfo;
    }

    public ProfileInfoRepresentation getProfileInfo() {
        return profileInfo;
    }

    public void setProfileInfo(ProfileInfoRepresentation profileInfo) {
        this.profileInfo = profileInfo;
    }

    public Map<String, List<ThemeInfoRepresentation>> getThemes() {
        return themes;
    }

    public void setThemes(Map<String, List<ThemeInfoRepresentation>> themes) {
        this.themes = themes;
    }

    public List<Map<String, String>> getSocialProviders() {
        return socialProviders;
    }

    public void setSocialProviders(List<Map<String, String>> socialProviders) {
        this.socialProviders = socialProviders;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public String getVariant() {
        return variant;
    }

    public void setDebuggerUrl(String debuggerUrl) {
        this.debuggerUrl = debuggerUrl;
    }

    public String getDebuggerUrl() {
        return debuggerUrl;
    }

    public List<Map<String, String>> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<Map<String, String>> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public List<Map<String, String>> getClientImporters() {
        return clientImporters;
    }

    public void setClientImporters(List<Map<String, String>> clientImporters) {
        this.clientImporters = clientImporters;
    }

    public Map<String, SpiInfoRepresentation> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, SpiInfoRepresentation> providers) {
        this.providers = providers;
    }

    public Map<String, List<ProtocolMapperTypeRepresentation>> getProtocolMapperTypes() {
        return protocolMapperTypes;
    }

    public void setProtocolMapperTypes(Map<String, List<ProtocolMapperTypeRepresentation>> protocolMapperTypes) {
        this.protocolMapperTypes = protocolMapperTypes;
    }

    public Map<String, List<ProtocolMapperRepresentation>> getBuiltinProtocolMappers() {
        return builtinProtocolMappers;
    }

    public void setBuiltinProtocolMappers(Map<String, List<ProtocolMapperRepresentation>> builtinProtocolMappers) {
        this.builtinProtocolMappers = builtinProtocolMappers;
    }

    public Map<String, List<String>> getEnums() {
        return enums;
    }

    public void setEnums(Map<String, List<String>> enums) {
        this.enums = enums;
    }

    public Map<String, List<ClientInstallationRepresentation>> getClientInstallations() {
        return clientInstallations;
    }

    public void setClientInstallations(Map<String, List<ClientInstallationRepresentation>> clientInstallations) {
        this.clientInstallations = clientInstallations;
    }

    public List<PasswordPolicyTypeRepresentation> getPasswordPolicies() {
        return passwordPolicies;
    }

    public void setPasswordPolicies(List<PasswordPolicyTypeRepresentation> passwordPolicies) {
        this.passwordPolicies = passwordPolicies;
    }

    public List<UsernamePolicyTypeRepresentation> getUsernamePolicies() {
        return usernamePolicies;
    }

    public void setUsernamePolicies(List<UsernamePolicyTypeRepresentation> usernamePolicies) {
        this.usernamePolicies = usernamePolicies;
    }

    public List<ActionPolicyTypeRepresentation> getActionPolicies() {
        return actionPolicies;
    }

    public void setActionPolicies(List<ActionPolicyTypeRepresentation> actionPolicies) {
        this.actionPolicies = actionPolicies;
    }

    public List<EventListenerRepresentation> getEventListeners() {
        return eventListeners;
    }

    public void setEventListeners(List<EventListenerRepresentation> eventListeners) {
        this.eventListeners = eventListeners;
    }

    public Map<String, List<ComponentTypeRepresentation>> getComponentTypes() {
        return componentTypes;
    }

    public void setComponentTypes(Map<String, List<ComponentTypeRepresentation>> componentTypes) {
        this.componentTypes = componentTypes;
    }

    public List<EmailProviderRepresentation> getEmailProviders() {
        return emailProviders;
    }

    public void setEmailProviders(List<EmailProviderRepresentation> emailProviders) {
        this.emailProviders = emailProviders;
    }

    public List<CibaAuthProviderRepresentation> getCibaAuthProviders() {
        return cibaAuthProviders;
    }

    public void setCibaAuthProviders(List<CibaAuthProviderRepresentation> cibaAuthProviders) {
        this.cibaAuthProviders = cibaAuthProviders;
    }

    public List<CibaNotificationProviderRepresentation> getCibaNotificationProviders() {
        return cibaNotificationProviders;
    }

    public void setCibaNotificationProviders(List<CibaNotificationProviderRepresentation> cibaNotificationProviders) {
        this.cibaNotificationProviders = cibaNotificationProviders;
    }

    public List<CibaDeadLetterHookNotificationProviderRepresentation> getCibaDeadLetterHookNotificationProviders() {
        return cibaDeadLetterHookProviders;
    }

    public void setCibaDeadLetterHookNotificationProviders(List<CibaDeadLetterHookNotificationProviderRepresentation> cibaDeadLetterHookNotificationProviders) {
        this.cibaDeadLetterHookProviders = cibaDeadLetterHookNotificationProviders;
    }

    public Map<String, List<SmsProviderRepresentation>> getSmsProviders() {
        return smsProviders;
    }

    public void setSmsProviders(Map<String, List<SmsProviderRepresentation>> smsProviders) {
        this.smsProviders = smsProviders;
    }

    public List<MFAProviderRepresentation> getMfaProviders() {
        return mfaProviders;
    }

    public void setMfaProviders(List<MFAProviderRepresentation> mfaProviders) {
        this.mfaProviders = mfaProviders;
    }

    public BrandRepresentation getBrandInfo() {
        return brandInfo;
    }

    public void setBrandInfo(BrandRepresentation brandInfo) {
        this.brandInfo = brandInfo;
    }

    public String getRootAccountName() {
        return rootAccountName;
    }

    public void setRootAccountName(String rootAccountName) {
        this.rootAccountName = rootAccountName;
    }

    public boolean isCibaFeatureEnabled() {
        return isCibaFeatureEnabled;
    }

    public void setCibaFeatureEnabled(boolean isCibaFeatureEnabled) {
        this.isCibaFeatureEnabled = isCibaFeatureEnabled;
    }

    public boolean isIWSEnabled() {
        return isIWSEnabled;
    }

    public void setIWSEnabled(boolean IWSEnabled) {
        isIWSEnabled = IWSEnabled;
    }

    public boolean isOIDCEnabled() {
        return isOIDCEnabled;
    }

    public void setOIDCEnabled(boolean OIDCEnabled) {
        isOIDCEnabled = OIDCEnabled;
    }

    public boolean isThirdPartyReqEnabled() {
        return thirdPartyReqEnabled;
    }

    public void setThirdPartyReqEnabled(boolean thirdPartyReqEnabled) {
        this.thirdPartyReqEnabled = thirdPartyReqEnabled;
    }

    public boolean isShowSecuritySettings() {
        return showSecuritySettings;
    }

    public void setShowSecuritySettings(boolean showSecuritySettings) {
        this.showSecuritySettings = showSecuritySettings;
    }

    public Set<String> getSupportedLocales() {
        return supportedLocales;
    }

    public void setSupportedLocales(Set<String> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }
}

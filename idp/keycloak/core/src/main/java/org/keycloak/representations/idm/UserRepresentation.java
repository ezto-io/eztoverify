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

package org.keycloak.representations.idm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.keycloak.json.StringListMapDeserializer;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRepresentation {

    protected String self; // link
    protected String id;
    protected String origin;
    protected String profPic;
    protected Boolean mfaEnabled;
    protected Long createdTimestamp;
    protected String username;
    protected Boolean enabled;
    protected Boolean totp;
    protected Boolean emailVerified;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String mobileNumber;
    protected String mobileNumberRegion;
    protected String federationLink;
    protected String serviceAccountClientId; // For rep, it points to clientId (not DB ID)
    protected boolean skipSendingEmail;
    protected String inviteWay; //otp,magiclink
    private String userInviteStatus;
    protected String inviteClientId;
    protected String accessUrl;
    protected boolean isAccountCreator;
    protected Long lastLoginTimestamp;
    private static final ArrayList<String> SINGLETON_AL = new ArrayList<>();

    @JsonDeserialize(using = StringListMapDeserializer.class)
    protected Map<String, List<String>> attributes;
    protected List<CredentialRepresentation> credentials;
    protected Set<String> disableableCredentialTypes;
    protected List<String> requiredActions;
    protected List<FederatedIdentityRepresentation> federatedIdentities;
    protected List<String> realmRoles;
    protected Map<String, List<String>> clientRoles;
    protected List<UserConsentRepresentation> clientConsents;
    protected Integer notBefore;

    @Deprecated
    protected Map<String, List<String>> applicationRoles;
    @Deprecated
    protected List<SocialLinkRepresentation> socialLinks;

    protected List<String> groups;
    private Map<String, Boolean> access;

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }


    public Boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(Boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    @Deprecated
    public Boolean isTotp() {
        return totp;
    }

    @Deprecated
    public void setTotp(Boolean totp) {
        this.totp = totp;
    }

    public Boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getMobileNumberRegion() {
        return mobileNumberRegion;
    }

    public void setMobileNumberRegion(String mobileNumberRegion) {
        this.mobileNumberRegion = mobileNumberRegion;
    }

    public Long getLastLoginTimestamp() {
        return lastLoginTimestamp;
    }

    public void setLastLoginTimestamp(Long lastLoginTimestamp) {
        this.lastLoginTimestamp = lastLoginTimestamp;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public UserRepresentation singleAttribute(String name, String value) {
        if (this.attributes == null) this.attributes=new HashMap<>();
        attributes.put(name, (value == null ? new ArrayList<String>() : Arrays.asList(value)));
        return this;
    }

    public String firstAttribute(String key) {
        return this.attributes == null ? null : this.attributes.get(key) == null ? null : this.attributes.get(key).isEmpty()? null : this.attributes.get(key).get(0);
    }

    public List<CredentialRepresentation> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialRepresentation> credentials) {
        this.credentials = credentials;
    }

    public List<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(List<String> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public List<FederatedIdentityRepresentation> getFederatedIdentities() {
        return federatedIdentities;
    }

    public void setFederatedIdentities(List<FederatedIdentityRepresentation> federatedIdentities) {
        this.federatedIdentities = federatedIdentities;
    }

    public List<SocialLinkRepresentation> getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(List<SocialLinkRepresentation> socialLinks) {
        this.socialLinks = socialLinks;
    }

    public List<String> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(List<String> realmRoles) {
        this.realmRoles = realmRoles;
    }

    public Map<String, List<String>> getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(Map<String, List<String>> clientRoles) {
        this.clientRoles = clientRoles;
    }

    public List<UserConsentRepresentation> getClientConsents() {
        return clientConsents;
    }

    public void setClientConsents(List<UserConsentRepresentation> clientConsents) {
        this.clientConsents = clientConsents;
    }

    public Integer getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Integer notBefore) {
        this.notBefore = notBefore;
    }

    @Deprecated
    public Map<String, List<String>> getApplicationRoles() {
        return applicationRoles;
    }

    public String getFederationLink() {
        return federationLink;
    }

    public void setFederationLink(String federationLink) {
        this.federationLink = federationLink;
    }

    public String getServiceAccountClientId() {
        return serviceAccountClientId;
    }

    public void setServiceAccountClientId(String serviceAccountClientId) {
        this.serviceAccountClientId = serviceAccountClientId;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    /**
     * Returns id of UserStorageProvider that loaded this user
     *
     * @return NULL if user stored locally
     */
    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Set<String> getDisableableCredentialTypes() {
        return disableableCredentialTypes;
    }

    public void setDisableableCredentialTypes(Set<String> disableableCredentialTypes) {
        this.disableableCredentialTypes = disableableCredentialTypes;
    }

    public Map<String, Boolean> getAccess() {
        return access;
    }

    public void setAccess(Map<String, Boolean> access) {
        this.access = access;
    }

    public Map<String, List<String>> toAttributes() {
        Map<String, List<String>> attrs = new HashMap<>();

        if (getAttributes() != null) attrs.putAll(getAttributes());

        if (getUsername() != null)
            attrs.put("username", Collections.singletonList(getUsername()));
        else
            attrs.remove("username");

        if (getEmail() != null)
            attrs.put("email", Collections.singletonList(getEmail()));
        else
            attrs.remove("email");

        if (getLastName() != null)
            attrs.put("lastName", Collections.singletonList(getLastName()));

        if (getFirstName() != null)
            attrs.put("firstName", Collections.singletonList(getFirstName()));

        // don't update mobileNumber if any one comes
        if(getMobileNumber() != null && getMobileNumberRegion() != null){
            attrs.put("mobileNumber", Collections.singletonList(getMobileNumber()));
            attrs.put("mobileNumberRegion", Collections.singletonList(getMobileNumberRegion()));
        } else {
            attrs.put("mobileNumber",SINGLETON_AL);
            attrs.put("mobileNumberRegion",SINGLETON_AL);
        }

        return attrs;
    }

    public String getProfPic() { return profPic;  }

    public void setProfPic(String profPic) { this.profPic = profPic; }

    public boolean isSkipSendingEmail() {
        return skipSendingEmail;
    }

    public void setSkipSendingEmail(boolean skipSendingEmail) {
        this.skipSendingEmail = skipSendingEmail;
    }

    public String getUserInviteStatus() {
        return userInviteStatus;
    }

    public void setUserInviteStatus(String userInviteStatus) {
        this.userInviteStatus = userInviteStatus;
    }

    public String getInviteClientId() {
        return inviteClientId;
    }

    public void setInviteClientId(String inviteClientId) {
        this.inviteClientId = inviteClientId;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public boolean isAccountCreator() {
        return isAccountCreator;
    }

    public void setAccountCreator(boolean accountCreator) {
        isAccountCreator = accountCreator;
    }
}


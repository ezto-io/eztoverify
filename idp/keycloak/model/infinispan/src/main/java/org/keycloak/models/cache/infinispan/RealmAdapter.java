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

package org.keycloak.models.cache.infinispan;

import com.qlikverify.apiConnector.spi.APIConnectorProvider;
import com.qlikverify.transactionState.spi.TransactionStateProvider;
import com.qlikverify.userTransactionStatus.spi.UserTransactionStatusProvider;
import com.qlikverify.transactionAttribute.spi.TransactionAttributeProvider;
import org.keycloak.Config;
import org.keycloak.common.Version;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedRealmModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.cache.infinispan.entities.CachedRealm;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdapter implements CachedRealmModel {
    private static final String OB_ADMIN_EMAIL_SETUP = "ob-admin-email-setup";
    private static final String OB_ADMIN_WHATSAPP_SETUP = "ob-admin-whatsapp-setup";
    private static final String OB_ADMIN_SMS_SETUP = "ob-admin-sms-setup";
    protected CachedRealm cached;
    protected RealmCacheSession cacheSession;
    protected volatile RealmModel updated;
    protected KeycloakSession session;
    private final Supplier<RealmModel> modelSupplier;

    public RealmAdapter(KeycloakSession session, CachedRealm cached, RealmCacheSession cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
        this.session = session;
        this.modelSupplier = this::getRealm;
    }

    @Override
    public RealmModel getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerRealmInvalidation(cached.getId(), cached.getName());
            updated = cacheSession.getRealmDelegate().getRealm(cached.getId());
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
        return updated;
    }

    protected volatile boolean invalidated;

    protected void invalidateFlag() {
        invalidated = true;

    }

    @Override
    public void invalidate() {
        invalidated = true;
        getDelegateForUpdate();
    }

    @Override
    public long getCacheTimestamp() {
        return cached.getCacheTimestamp();
    }

    @Override
    public ConcurrentHashMap getCachedWith() {
        return cached.getCachedWith();
    }

    protected boolean isUpdated() {
        if (updated != null)
            return true;
        if (!invalidated)
            return false;
        updated = cacheSession.getRealmDelegate().getRealm(cached.getId());
        if (updated == null)
            throw new IllegalStateException("Not found in database");
        return true;
    }

    @Override
    public String getId() {
        if (isUpdated())
            return updated.getId();
        return cached.getId();
    }

    @Override
    public String getName() {
        if (isUpdated())
            return updated.getName();
        return cached.getName();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        updated.setName(name);
    }

    @Override
    public String getDisplayName() {
        if (isUpdated())
            return updated.getDisplayName();
        return cached.getDisplayName();
    }

    @Override
    public void setDisplayName(String displayName) {
        getDelegateForUpdate();
        updated.setDisplayName(displayName);
    }

    @Override
    public String getDisplayNameHtml() {
        if (isUpdated())
            return updated.getDisplayNameHtml();
        return cached.getDisplayNameHtml();
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        getDelegateForUpdate();
        updated.setDisplayNameHtml(displayNameHtml);
    }

    @Override
    public boolean isEnabled() {
        if (isUpdated())
            return updated.isEnabled();
        return cached.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEnabled(enabled);
    }

    @Override
    public boolean isUserManagedAccessAllowed() {
        if (isUpdated())
            return updated.isEnabled();
        return cached.isAllowUserManagedAccess();
    }

    @Override
    public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {
        getDelegateForUpdate();
        updated.setUserManagedAccessAllowed(userManagedAccessAllowed);
    }

    @Override
    public SslRequired getSslRequired() {
        if (isUpdated())
            return updated.getSslRequired();
        return cached.getSslRequired();
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        getDelegateForUpdate();
        updated.setSslRequired(sslRequired);
    }

    @Override
    public boolean isRegistrationAllowed() {
        if (isUpdated())
            return updated.isRegistrationAllowed();
        return cached.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        getDelegateForUpdate();
        updated.setRegistrationAllowed(registrationAllowed);
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        if (isUpdated())
            return updated.isRegistrationEmailAsUsername();
        return cached.isRegistrationEmailAsUsername();
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        getDelegateForUpdate();
        updated.setRegistrationEmailAsUsername(registrationEmailAsUsername);
    }

    @Override
    public boolean isRememberMe() {
        if (isUpdated())
            return updated.isRememberMe();
        return cached.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        getDelegateForUpdate();
        updated.setRememberMe(rememberMe);
    }

    @Override
    public String getDefaultSignatureAlgorithm() {
        if (isUpdated())
            return updated.getDefaultSignatureAlgorithm();
        return cached.getDefaultSignatureAlgorithm();
    }

    @Override
    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        getDelegateForUpdate();
        updated.setDefaultSignatureAlgorithm(defaultSignatureAlgorithm);
    }

    @Override
    public boolean isBruteForceProtected() {
        if (isUpdated())
            return updated.isBruteForceProtected();
        return cached.isBruteForceProtected();
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        getDelegateForUpdate();
        updated.setBruteForceProtected(value);
    }

    @Override
    public boolean isPermanentLockout() {
        if (isUpdated())
            return updated.isPermanentLockout();
        return cached.isPermanentLockout();
    }

    @Override
    public void setPermanentLockout(final boolean val) {
        getDelegateForUpdate();
        updated.setPermanentLockout(val);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        if (isUpdated())
            return updated.getMaxFailureWaitSeconds();
        return cached.getMaxFailureWaitSeconds();
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        getDelegateForUpdate();
        updated.setMaxFailureWaitSeconds(val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        if (isUpdated())
            return updated.getWaitIncrementSeconds();
        return cached.getWaitIncrementSeconds();
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        getDelegateForUpdate();
        updated.setWaitIncrementSeconds(val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        if (isUpdated())
            return updated.getMinimumQuickLoginWaitSeconds();
        return cached.getMinimumQuickLoginWaitSeconds();
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        getDelegateForUpdate();
        updated.setMinimumQuickLoginWaitSeconds(val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        if (isUpdated())
            return updated.getQuickLoginCheckMilliSeconds();
        return cached.getQuickLoginCheckMilliSeconds();
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        getDelegateForUpdate();
        updated.setQuickLoginCheckMilliSeconds(val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        if (isUpdated())
            return updated.getMaxDeltaTimeSeconds();
        return cached.getMaxDeltaTimeSeconds();
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        getDelegateForUpdate();
        updated.setMaxDeltaTimeSeconds(val);
    }

    @Override
    public int getFailureFactor() {
        if (isUpdated())
            return updated.getFailureFactor();
        return cached.getFailureFactor();
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        getDelegateForUpdate();
        updated.setFailureFactor(failureFactor);
    }

    @Override
    public String getDefaultRegion() {
        if (isUpdated())
            return updated.getDefaultRegion();
        return cached.getDefaultRegion();
    }

    @Override
    public void setDefaultRegion(String region) {
        getDelegateForUpdate();
        updated.setDefaultRegion(region);
    }

    @Override
    public boolean isVerifyEmail() {
        if (isUpdated())
            return updated.isVerifyEmail();
        return cached.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        getDelegateForUpdate();
        updated.setVerifyEmail(verifyEmail);
    }

    @Override
    public boolean isLoginWithEmailAllowed() {
        if (isUpdated())
            return updated.isLoginWithEmailAllowed();
        return cached.isLoginWithEmailAllowed();
    }

    @Override
    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        getDelegateForUpdate();
        updated.setLoginWithEmailAllowed(loginWithEmailAllowed);
    }

    @Override
    public boolean isDuplicateEmailsAllowed() {
        if (isUpdated())
            return updated.isDuplicateEmailsAllowed();
        return cached.isDuplicateEmailsAllowed();
    }

    @Override
    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        getDelegateForUpdate();
        updated.setDuplicateEmailsAllowed(duplicateEmailsAllowed);
    }

    @Override
    public boolean isDuplicateMobNumAllowed() {
        if (isUpdated())
            return updated.isDuplicateMobNumAllowed();
        return cached.isDuplicateMobNumAllowed();
    }

    @Override
    public void setDuplicateMobNumAllowed(boolean duplicateMobNumAllowed) {
        getDelegateForUpdate();
        updated.setDuplicateMobNumAllowed(duplicateMobNumAllowed);
    }

    @Override
    public boolean isResetPasswordAllowed() {
        if (isUpdated())
            return updated.isResetPasswordAllowed();
        return cached.isResetPasswordAllowed();
    }

    @Override
    public void setUserInviteWay(String inviteWay) {
        getDelegateForUpdate();
        updated.setUserInviteWay(inviteWay);
    }

    @Override
    public String getUserInviteWay() {
        if (isUpdated())
            return updated.getUserInviteWay();
        return cached.getUserInviteWay();
    }

    @Override
    public void setUserInviteClientId(String clientId) {
        getDelegateForUpdate();
        updated.setUserInviteClientId(clientId);
    }

    @Override
    public String getUserInviteClientId() {
        if (isUpdated())
            return updated.getUserInviteClientId();
        return cached.getUserInviteClientId();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        getDelegateForUpdate();
        updated.setResetPasswordAllowed(resetPasswordAllowed);
    }

    @Override
    public boolean isEditUsernameAllowed() {
        if (isUpdated())
            return updated.isEditUsernameAllowed();
        return cached.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        getDelegateForUpdate();
        updated.setEditUsernameAllowed(editUsernameAllowed);
    }

    @Override
    public boolean isRevokeRefreshToken() {
        if (isUpdated())
            return updated.isRevokeRefreshToken();
        return cached.isRevokeRefreshToken();
    }

    @Override
    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        getDelegateForUpdate();
        updated.setRevokeRefreshToken(revokeRefreshToken);
    }

    @Override
    public int getRefreshTokenMaxReuse() {
        if (isUpdated())
            return updated.getRefreshTokenMaxReuse();
        return cached.getRefreshTokenMaxReuse();
    }

    @Override
    public void setRefreshTokenMaxReuse(int refreshTokenMaxReuse) {
        getDelegateForUpdate();
        updated.setRefreshTokenMaxReuse(refreshTokenMaxReuse);
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        if (isUpdated())
            return updated.getSsoSessionIdleTimeout();
        return cached.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        getDelegateForUpdate();
        updated.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        if (isUpdated())
            return updated.getSsoSessionMaxLifespan();
        return cached.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getSsoSessionIdleTimeoutRememberMe() {
        if (updated != null)
            return updated.getSsoSessionIdleTimeoutRememberMe();
        return cached.getSsoSessionIdleTimeoutRememberMe();
    }

    @Override
    public void setSsoSessionIdleTimeoutRememberMe(int seconds) {
        getDelegateForUpdate();
        updated.setSsoSessionIdleTimeoutRememberMe(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespanRememberMe() {
        if (updated != null)
            return updated.getSsoSessionMaxLifespanRememberMe();
        return cached.getSsoSessionMaxLifespanRememberMe();
    }

    @Override
    public void setSsoSessionMaxLifespanRememberMe(int seconds) {
        getDelegateForUpdate();
        updated.setSsoSessionMaxLifespanRememberMe(seconds);
    }

    @Override
    public int getOfflineSessionIdleTimeout() {
        if (isUpdated())
            return updated.getOfflineSessionIdleTimeout();
        return cached.getOfflineSessionIdleTimeout();
    }

    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        getDelegateForUpdate();
        updated.setOfflineSessionIdleTimeout(seconds);
    }

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    @Override
    public boolean isOfflineSessionMaxLifespanEnabled() {
        if (isUpdated())
            return updated.isOfflineSessionMaxLifespanEnabled();
        return cached.isOfflineSessionMaxLifespanEnabled();
    }

    @Override
    public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        getDelegateForUpdate();
        updated.setOfflineSessionMaxLifespanEnabled(offlineSessionMaxLifespanEnabled);
    }

    @Override
    public int getOfflineSessionMaxLifespan() {
        if (isUpdated())
            return updated.getOfflineSessionMaxLifespan();
        return cached.getOfflineSessionMaxLifespan();
    }

    @Override
    public void setOfflineSessionMaxLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setOfflineSessionMaxLifespan(seconds);
    }

    @Override
    public int getClientSessionIdleTimeout() {
        if (isUpdated())
            return updated.getClientSessionIdleTimeout();
        return cached.getClientSessionIdleTimeout();
    }

    @Override
    public void setClientSessionIdleTimeout(int seconds) {
        getDelegateForUpdate();
        updated.setClientSessionIdleTimeout(seconds);
    }

    @Override
    public int getClientSessionMaxLifespan() {
        if (isUpdated())
            return updated.getClientSessionMaxLifespan();
        return cached.getClientSessionMaxLifespan();
    }

    @Override
    public void setClientSessionMaxLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setClientSessionMaxLifespan(seconds);
    }

    @Override
    public int getClientOfflineSessionIdleTimeout() {
        if (isUpdated())
            return updated.getClientOfflineSessionIdleTimeout();
        return cached.getClientOfflineSessionIdleTimeout();
    }

    @Override
    public void setClientOfflineSessionIdleTimeout(int seconds) {
        getDelegateForUpdate();
        updated.setClientOfflineSessionIdleTimeout(seconds);
    }

    @Override
    public int getClientOfflineSessionMaxLifespan() {
        if (isUpdated())
            return updated.getClientOfflineSessionMaxLifespan();
        return cached.getClientOfflineSessionMaxLifespan();
    }

    @Override
    public void setClientOfflineSessionMaxLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setClientOfflineSessionMaxLifespan(seconds);
    }

    @Override
    public int getAccessTokenLifespan() {
        if (isUpdated())
            if (getMode().equalsIgnoreCase("bo")) {
                updated.setAccessTokenLifespan(660);
                return updated.getAccessTokenLifespan();
            }
        return cached.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(int seconds) {
        getDelegateForUpdate();
        if (getMode().equalsIgnoreCase("bo")) {
            getRealm().setAccessTokenLifespan(660);
            updated.setAccessTokenLifespan(660);
        } else {
            updated.setAccessTokenLifespan(seconds);
        }
    }

    @Override
    public int getAccessTokenLifespanForImplicitFlow() {
        if (isUpdated())
            return updated.getAccessTokenLifespanForImplicitFlow();
        return cached.getAccessTokenLifespanForImplicitFlow();
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        getDelegateForUpdate();
        updated.setAccessTokenLifespanForImplicitFlow(seconds);
    }

    @Override
    public int getAccessCodeLifespan() {
        if (isUpdated())
            return updated.getAccessCodeLifespan();
        return cached.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setAccessCodeLifespan(seconds);
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        if (isUpdated())
            return updated.getAccessCodeLifespanUserAction();
        return cached.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int seconds) {
        getDelegateForUpdate();
        updated.setAccessCodeLifespanUserAction(seconds);
    }

    @Override
    public Map<String, Integer> getUserActionTokenLifespans() {
        if (isUpdated())
            return updated.getUserActionTokenLifespans();
        return cached.getUserActionTokenLifespans();
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        if (isUpdated())
            return updated.getAccessCodeLifespanLogin();
        return cached.getAccessCodeLifespanLogin();
    }

    @Override
    public void setAccessCodeLifespanLogin(int seconds) {
        getDelegateForUpdate();
        updated.setAccessCodeLifespanLogin(seconds);
    }

    @Override
    public int getActionTokenGeneratedByAdminLifespan() {
        if (isUpdated())
            return updated.getActionTokenGeneratedByAdminLifespan();
        return cached.getActionTokenGeneratedByAdminLifespan();
    }

    @Override
    public void setActionTokenGeneratedByAdminLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setActionTokenGeneratedByAdminLifespan(seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan() {
        if (isUpdated())
            return updated.getActionTokenGeneratedByUserLifespan();
        return cached.getActionTokenGeneratedByUserLifespan();
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(int seconds) {
        getDelegateForUpdate();
        updated.setActionTokenGeneratedByUserLifespan(seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan(String actionTokenId) {
        if (isUpdated())
            return updated.getActionTokenGeneratedByUserLifespan(actionTokenId);
        return cached.getActionTokenGeneratedByUserLifespan(actionTokenId);
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(String actionTokenId, Integer seconds) {
        if (seconds != null) {
            getDelegateForUpdate();
            updated.setActionTokenGeneratedByUserLifespan(actionTokenId, seconds);
        }
    }

    @Override
    public boolean isEnabledOwnEmailProvider() {
        if (isUpdated()) {
            updateTenantOnBoardingDueToSubscriptionChange(OB_ADMIN_EMAIL_SETUP);
            return updated.isEnabledOwnEmailProvider();
        }
        return cached.isEnabledOwnEmailProvider();
    }

    @Override
    public void setOwnEmailProvider(boolean value) {
        getDelegateForUpdate();
        updated.setOwnEmailProvider(value);
    }

    @Override
    public boolean isEnabledOwnWhatsappProvider() {
        return true;
    }

    @Override
    public boolean isDNSEnabled() {
        if (this.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm()))
            return true;

        return false;
    }

    @Override
    public boolean isEnabledOwnSmsProvider() {
        if (isUpdated()) {
            updateTenantOnBoardingDueToSubscriptionChange(OB_ADMIN_SMS_SETUP);
            return updated.isEnabledOwnSmsProvider();
        }
        return cached.isEnabledOwnSmsProvider();
    }

    @Override
    public void setOwnSmsProvider(boolean value) {
        getDelegateForUpdate();
        updated.setOwnSmsProvider(value);
    }

    @Override
    public String getRealmLogo() {
        if (isUpdated()) return updated.getRealmLogo();
        return cached.getRealmLogo();
    }

    @Override
    public void setRealmLogo(String logoUrl) {
        getDelegateForUpdate();
        updated.setRealmLogo(logoUrl);
    }

    @Override
    public Stream<RequiredCredentialModel> getRequiredCredentialsStream() {
        if (isUpdated())
            return updated.getRequiredCredentialsStream();
        return cached.getRequiredCredentials().stream();
    }

    @Override
    public OAuth2DeviceConfig getOAuth2DeviceConfig() {
        if (isUpdated())
            return updated.getOAuth2DeviceConfig();
        return cached.getOAuth2DeviceConfig(modelSupplier);
    }

    @Override
    public CibaConfig getCibaPolicy() {
        if (isUpdated())
            return updated.getCibaPolicy();
        return cached.getCibaConfig(modelSupplier);
    }

    @Override
    public ParConfig getParPolicy() {
        if (isUpdated())
            return updated.getParPolicy();
        return cached.getParConfig(modelSupplier);
    }

    @Override
    public List<RequiredCredentialModel> getRequiredCredentials() {
        if (isUpdated())
            return updated.getRequiredCredentials();
        return cached.getRequiredCredentials();
    }

    @Override
    public void addRequiredCredential(String cred) {
        getDelegateForUpdate();
        updated.addRequiredCredential(cred);
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (isUpdated())
            return updated.getPasswordPolicy();
        return cached.getPasswordPolicy();
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        getDelegateForUpdate();
        updated.setPasswordPolicy(policy);
    }

    @Override
    public OTPPolicy getOTPPolicy() {
        if (isUpdated())
            return updated.getOTPPolicy();
        return cached.getOtpPolicy();
    }

    @Override
    public void setOTPPolicy(OTPPolicy policy) {
        getDelegateForUpdate();
        updated.setOTPPolicy(policy);

    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicy() {
        if (isUpdated())
            return updated.getWebAuthnPolicy();
        return cached.getWebAuthnPolicy();
    }

    @Override
    public void setWebAuthnPolicy(WebAuthnPolicy policy) {
        getDelegateForUpdate();
        updated.setWebAuthnPolicy(policy);
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicyPasswordless() {
        if (isUpdated())
            return updated.getWebAuthnPolicyPasswordless();
        return cached.getWebAuthnPasswordlessPolicy();
    }

    @Override
    public void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy) {
        getDelegateForUpdate();
        updated.setWebAuthnPolicyPasswordless(policy);
    }

    @Override
    public RoleModel getRoleById(String id) {
        if (isUpdated())
            return updated.getRoleById(id);
        return cacheSession.getRoleById(this, id);
    }

    @Override
    public Stream<GroupModel> getDefaultGroupsStream() {
        if (isUpdated())
            return updated.getDefaultGroupsStream();
        return cached.getDefaultGroups().stream().map(this::getGroupById);
    }

    @Override
    public void addDefaultGroup(GroupModel group) {
        getDelegateForUpdate();
        updated.addDefaultGroup(group);

    }

    @Override
    public void removeDefaultGroup(GroupModel group) {
        getDelegateForUpdate();
        updated.removeDefaultGroup(group);

    }

    @Override
    @Deprecated
    public Stream<String> getDefaultRolesStream() {
        if (isUpdated())
            return updated.getDefaultRolesStream();
        return getDefaultRole().getCompositesStream().filter(this::isRealmRole).map(RoleModel::getName);
    }

    private boolean isRealmRole(RoleModel role) {
        return !role.isClientRole();
    }

    @Override
    @Deprecated
    public void addDefaultRole(String name) {
        getDelegateForUpdate();
        updated.addDefaultRole(name);
    }

    @Override
    @Deprecated
    public void removeDefaultRoles(String... defaultRoles) {
        getDelegateForUpdate();
        updated.removeDefaultRoles(defaultRoles);

    }

    @Override
    public void addToDefaultRoles(RoleModel role) {
        getDelegateForUpdate();
        updated.addToDefaultRoles(role);
    }

    @Override
    public Stream<ClientModel> getClientsStream() {
        return cacheSession.getClientsStream(this);
    }

    @Override
    public Stream<ClientModel> getClientsStream(String orderBy) {
        return cacheSession.getClientsStream(this, orderBy);
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream() {
        return cacheSession.getAlwaysDisplayInConsoleClientsStream(this);
    }

    @Override
    public ClientModel addClient() {
        return cacheSession.addClient(this);
    }

    @Override
    public ClientModel addClientByClientId(String clientId) {
        return cacheSession.addClient(this, clientId);
    }

    @Override
    public ClientModel addClient(String name) {
        return cacheSession.addClient(this, name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        return cacheSession.addClient(this, id, clientId);
    }

    @Override
    public boolean removeClient(String id) {
        return cacheSession.removeClient(this, id);
    }

    @Override
    public ClientModel getClientById(String id) {
        if (isUpdated())
            return updated.getClientById(id);
        return cacheSession.getClientById(this, id);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        return cacheSession.getClientByClientId(this, clientId);
    }

    @Override
    public ClientModel getClientBySandboxClientId(String sandboxClientId) {
        return cacheSession.getClientBySandboxClientId(this, sandboxClientId);
    }

    @Override
    public Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults) {
        return cacheSession.searchClientsByClientIdStream(this, clientId, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientByClientNameStream(String clientName, Integer firstResult, Integer maxResults) {
        return cacheSession.searchClientsByClientNameStream(this, clientName, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientByAttributes(Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return null;
    }

   /* @Override
    public Stream<ClientModel> searchClientByClientNameStream(String clientName, Integer firstResult, Integer maxResults) {
        return cacheSession.searchClientsByClientNameStream(this, clientName, firstResult, maxResults);
    }*/

    @Override
    public Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults) {
        return cacheSession.getClientsStream(this, firstResult, maxResults);
    }

    @Override
    public Long getClientsCount() {
        return cacheSession.getClientsCount(this);
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        getDelegateForUpdate();
        updated.updateRequiredCredentials(creds);
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        if (isUpdated())
            return updated.getBrowserSecurityHeaders();
        return cached.getBrowserSecurityHeaders();
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        getDelegateForUpdate();
        updated.setBrowserSecurityHeaders(headers);

    }

    @Override
    public Map<String, String> getSmtpConfig() {
        if (isUpdated())
            return updated.getSmtpConfig();
        return cached.getSmtpConfig();
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        getDelegateForUpdate();
        updated.setSmtpConfig(smtpConfig);
    }

    @Override
    public void setSmsConfig(Map<String, String> smsConfig) {
        getDelegateForUpdate();
        updated.setSmsConfig(smsConfig);
    }

    @Override
    public Map<String, String> getSmsConfig() {
        Map<String, String> smsInternalConfig = getSmsRealmConfig();
        RealmModel masterRealm = cacheSession.getRealmByName(org.keycloak.Config.getAdminRealm());
        String shareFeatures = masterRealm.getAttribute("shareFeatures");
        if ((smsInternalConfig == null || smsInternalConfig.size() == 0) && shareFeatures != null
                && shareFeatures.contains("sms")) {
            boolean isMaster = isUpdated() ? updated.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm())
                    : cached.getName().equalsIgnoreCase(org.keycloak.Config.getAdminRealm());
            if (!isMaster && cacheSession != null) {
                RealmModel realmModel = cacheSession.getRealmByName(org.keycloak.Config.getAdminRealm());
                if (realmModel != null) {
                    smsInternalConfig = realmModel.getSmsConfig();
                }
            }
        }
        return smsInternalConfig;
    }

    @Override
    public Map<String, String> getSmsRealmConfig() {
        if (isUpdated())
            return updated.getSmsConfig();
        return cached.getSmsConfig();
    }

    @Override
    public Map<String, String> getSmsExternalConfig() {
        Map<String, String> smsConfig = getSmsConfig();
        if (!smsConfig.get("provider").equals("Config"))
            return null;

        String attributes = smsConfig.get("attributes");
        if (attributes == null)
            return null;

        try {
            return JsonSerialization.readValue(attributes, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProvidersStream() {
        if (isUpdated())
            return updated.getIdentityProvidersStream();
        return cached.getIdentityProviders().stream();
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        if (isUpdated())
            return updated.getIdentityProviderByAlias(alias);
        return getIdentityProvidersStream()
                .filter(model -> Objects.equals(model.getAlias(), alias))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel identityProvider) {
        getDelegateForUpdate();
        updated.addIdentityProvider(identityProvider);
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        getDelegateForUpdate();
        updated.updateIdentityProvider(identityProvider);
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        getDelegateForUpdate();
        updated.removeIdentityProviderByAlias(alias);
    }

    @Override
    public String getLoginTheme() {
        if (isUpdated())
            return updated.getLoginTheme();
        return cached.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        getDelegateForUpdate();
        updated.setLoginTheme(name);
    }

    @Override
    public String getAccountTheme() {
        if (isUpdated())
            return updated.getAccountTheme();
        return cached.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        getDelegateForUpdate();
        updated.setAccountTheme(name);
    }

    @Override
    public String getAdminTheme() {
        if (isUpdated())
            return updated.getAdminTheme();
        return cached.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        getDelegateForUpdate();
        updated.setAdminTheme(name);
    }

    @Override
    public String getEmailTheme() {
        if (isUpdated())
            return updated.getEmailTheme();
        return cached.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        getDelegateForUpdate();
        updated.setEmailTheme(name);
    }

    @Override
    public String getSSOTheme() {
        if (isUpdated())
            return updated.getSSOTheme();
        return cached.getSsoTheme();
    }

    @Override
    public void setSSOTheme(String name) {
        getDelegateForUpdate();
        updated.setSSOTheme(name);
    }

    @Override
    public String getMode() {
        if (isUpdated())
            return updated.getMode();
        return cached.getMode();
    }

    @Override
    public void setMode(String mode) {
        getDelegateForUpdate();
        updated.setMode(mode);
    }


    @Override
    public int getNotBefore() {
        if (isUpdated())
            return updated.getNotBefore();
        return cached.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        getDelegateForUpdate();
        updated.setNotBefore(notBefore);
    }

    @Override
    public boolean isEventsEnabled() {
        if (isUpdated())
            return updated.isEventsEnabled();
        return cached.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEventsEnabled(enabled);
    }

    @Override
    public long getEventsExpiration() {
        if (isUpdated())
            return updated.getEventsExpiration();
        return cached.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(long expiration) {
        getDelegateForUpdate();
        updated.setEventsExpiration(expiration);
    }

    @Override
    public Stream<String> getEventsListenersStream() {
        if (isUpdated())
            return updated.getEventsListenersStream();
        return cached.getEventsListeners().stream();
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        getDelegateForUpdate();
        updated.setEventsListeners(listeners);
    }

    @Override
    public Stream<String> getEmailSenderStream() {
        if (isUpdated())
            return updated.getEmailSenderStream();
        return cached.getEmailSenders().stream();
    }

    @Override
    public void setEmailSenders(Set<String> listeners) {
        getDelegateForUpdate();
        updated.setEmailSenders(listeners);
    }

    @Override
    public Stream<String> getSmsSenderStream() {
        if (isUpdated())
            return updated.getSmsSenderStream();
        return cached.getSmsSenders().stream();
    }

    @Override
    public void setSmsSenders(Set<String> listeners) {
        getDelegateForUpdate();
        updated.setSmsSenders(listeners);
    }

    @Override
    public Stream<String> getMFAProvidersStream() {
        if (isUpdated())
            return updated.getMFAProvidersStream();
        return cached.getMfaProviders().stream();
    }

    @Override
    public void setMFAProviders(Set<String> listeners) {
        getDelegateForUpdate();
        updated.setMFAProviders(listeners);
    }

    @Override
    public Stream<String> getEnabledEventTypesStream() {
        if (isUpdated())
            return updated.getEnabledEventTypesStream();
        return cached.getEnabledEventTypes().stream();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        getDelegateForUpdate();
        updated.setEnabledEventTypes(enabledEventTypes);
    }

    @Override
    public boolean isAdminEventsEnabled() {
        if (isUpdated())
            return updated.isAdminEventsEnabled();
        return cached.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setAdminEventsEnabled(enabled);
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        if (isUpdated())
            return updated.isAdminEventsDetailsEnabled();
        return cached.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setAdminEventsDetailsEnabled(enabled);
    }

    @Override
    public ClientModel getMasterAdminClient() {
        return cached.getMasterAdminClient() == null ? null
                : cacheSession.getRealmByName(Config.getAdminRealm()).getClientById(cached.getMasterAdminClient());
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        getDelegateForUpdate();
        updated.setMasterAdminClient(client);
    }

    @Override
    public void setDefaultRole(RoleModel role) {
        getDelegateForUpdate();
        updated.setDefaultRole(role);
    }

    @Override
    public RoleModel getDefaultRole() {
        return cached.getDefaultRoleId() == null ? null : cacheSession.getRoleById(this, cached.getDefaultRoleId());
    }

    @Override
    public RoleModel getRole(String name) {
        return cacheSession.getRealmRole(this, name);
    }

    @Override
    public Stream<RoleModel> getRolesStream() {
        return cacheSession.getRealmRolesStream(this);
    }

    @Override
    public Stream<RoleModel> getRolesStream(Integer first, Integer max) {
        return cacheSession.getRealmRolesStream(this, first, max);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return cacheSession.searchForRolesStream(this, search, first, max);
    }

    @Override
    public RoleModel addRole(String name) {
        return cacheSession.addRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return cacheSession.addRealmRole(this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return cacheSession.removeRole(role);
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        if (isUpdated())
            return updated.isIdentityFederationEnabled();
        return cached.isIdentityFederationEnabled();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RealmModel))
            return false;

        RealmModel that = (RealmModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean isInternationalizationEnabled() {
        if (isUpdated())
            return updated.isInternationalizationEnabled();
        return cached.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setInternationalizationEnabled(enabled);
    }

    @Override
    public String getDefaultLocale() {
        if (isUpdated())
            return updated.getDefaultLocale();
        return cached.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        updated.setDefaultLocale(locale);
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream() {
        if (isUpdated())
            return updated.getIdentityProviderMappersStream();
        return cached.getIdentityProviderMapperSet().stream();
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias) {
        if (isUpdated())
            return updated.getIdentityProviderMappersByAliasStream(brokerAlias);
        Set<IdentityProviderMapperModel> mappings = new HashSet<>(
                cached.getIdentityProviderMappers().getList(brokerAlias));
        return mappings.stream();
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        getDelegateForUpdate();
        return updated.addIdentityProviderMapper(model);
    }

    @Override
    public void removeIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        getDelegateForUpdate();
        updated.removeIdentityProviderMapper(mapping);
    }

    @Override
    public void updateIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        getDelegateForUpdate();
        updated.updateIdentityProviderMapper(mapping);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        if (isUpdated())
            return updated.getIdentityProviderMapperById(id);
        for (List<IdentityProviderMapperModel> models : cached.getIdentityProviderMappers().values()) {
            for (IdentityProviderMapperModel model : models) {
                if (model.getId().equals(id))
                    return model;
            }
        }
        return null;
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String alias, String name) {
        if (isUpdated())
            return updated.getIdentityProviderMapperByName(alias, name);
        List<IdentityProviderMapperModel> models = cached.getIdentityProviderMappers().getList(alias);
        if (models == null)
            return null;
        for (IdentityProviderMapperModel model : models) {
            if (model.getName().equals(name))
                return model;
        }
        return null;
    }

    @Override
    public AuthenticationFlowModel getBrowserFlow() {
        if (isUpdated())
            return updated.getBrowserFlow();
        return cached.getBrowserFlow();
    }

    @Override
    public void setBrowserFlow(AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setBrowserFlow(flow);

    }

    @Override
    public AuthenticationFlowModel getRegistrationFlow() {
        if (isUpdated())
            return updated.getRegistrationFlow();
        return cached.getRegistrationFlow();
    }

    @Override
    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setRegistrationFlow(flow);

    }

    @Override
    public AuthenticationFlowModel getDirectGrantFlow() {
        if (isUpdated())
            return updated.getDirectGrantFlow();
        return cached.getDirectGrantFlow();
    }

    @Override
    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setDirectGrantFlow(flow);

    }

    @Override
    public AuthenticationFlowModel getResetCredentialsFlow() {
        if (isUpdated())
            return updated.getResetCredentialsFlow();
        return cached.getResetCredentialsFlow();
    }

    @Override
    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setResetCredentialsFlow(flow);

    }

    @Override
    public AuthenticationFlowModel getClientAuthenticationFlow() {
        if (isUpdated())
            return updated.getClientAuthenticationFlow();
        return cached.getClientAuthenticationFlow();
    }

    @Override
    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setClientAuthenticationFlow(flow);
    }

    @Override
    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        if (isUpdated())
            return updated.getDockerAuthenticationFlow();
        return cached.getDockerAuthenticationFlow();
    }

    @Override
    public void setDockerAuthenticationFlow(final AuthenticationFlowModel flow) {
        getDelegateForUpdate();
        updated.setDockerAuthenticationFlow(flow);
    }

    @Override
    public Stream<AuthenticationFlowModel> getAuthenticationFlowsStream() {
        if (isUpdated())
            return updated.getAuthenticationFlowsStream();
        return cached.getAuthenticationFlowList().stream();
    }

    @Override
    public AuthenticationFlowModel getFlowByAlias(String alias) {
        return getAuthenticationFlowsStream()
                .filter(flow -> flow.getAlias().equalsIgnoreCase(alias))
                .findFirst()
                .orElse(null);
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
        return getAuthenticatorConfigsStream()
                .filter(config -> Objects.equals(config.getAlias(), alias))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Stream<EventConfigModel> getEventConfigsStream() {
        if (isUpdated())
            return updated.getEventConfigsStream();
        return cached.getEventConfigs().values().stream();
    }

    @Override
    public EventConfigModel addEventConfig(EventConfigModel model) {
        getDelegateForUpdate();
        return updated.addEventConfig(model);
    }

    @Override
    public void updateEventConfig(EventConfigModel model) {
        getDelegateForUpdate();
        updated.updateEventConfig(model);
    }

    @Override
    public void removeEventConfig(EventConfigModel model) {
        getDelegateForUpdate();
        updated.removeEventConfig(model);
    }

    @Override
    public EventConfigModel getEventConfigByProviderId(String id) {
        if (isUpdated())
            return updated.getEventConfigByProviderId(id);
        return cached.getEventConfigs().get(id);
    }

    @Override
    public Stream<MarketplaceConfigModel> getMarketplaceConfigsStream() {
        if (isUpdated())
            return updated.getMarketplaceConfigsStream();
        return cached.getMarketPlaceConfigs().values().stream();
    }

    @Override
    public MarketplaceConfigModel addMarketplaceConfig(MarketplaceConfigModel model) {
        getDelegateForUpdate();
        return updated.addMarketplaceConfig(model);
    }

    @Override
    public void updateMarketplaceConfig(MarketplaceConfigModel model) {
        getDelegateForUpdate();
        updated.updateMarketplaceConfig(model);
    }

    @Override
    public void removeMarketplaceConfig(MarketplaceConfigModel model) {
        getDelegateForUpdate();
        updated.removeMarketplaceConfig(model);
    }

    @Override
    public Stream<MarketplaceConfigModel> getMarketplaceConfigByProviderType(String type) {
        if (isUpdated())
            return updated.getMarketplaceConfigsStream().filter(x -> x.getProviderType().equalsIgnoreCase(type));
        return cached.getMarketPlaceConfigs().values().stream().filter(x -> x.getProviderType().equalsIgnoreCase(type));
    }

    @Override
    public Stream<SmsConfigModel> getSmsConfigsStream() {
        if (isUpdated())
            return updated.getSmsConfigsStream();
        return cached.getSmsConfigs().values().stream();
    }

    @Override
    public SmsConfigModel addSmsConfig(SmsConfigModel model) {
        getDelegateForUpdate();
        return updated.addSmsConfig(model);
    }

    @Override
    public void updateSmsConfig(SmsConfigModel model) {
        getDelegateForUpdate();
        updated.updateSmsConfig(model);
    }

    @Override
    public void removeSmsConfig(SmsConfigModel model) {
        getDelegateForUpdate();
        updated.removeSmsConfig(model);
    }

    @Override
    public SmsConfigModel getSmsConfigByProviderId(String id) {
        if (isUpdated())
            return updated.getSmsConfigByProviderId(id);
        return cached.getSmsConfigs().get(id);
    }

    @Override
    public String getSubscription() {
        if (isUpdated())
            return updated.getSubscription();
        return cached.getSubscription();
    }

    @Override
    public void addSubscription(String model) {
        getDelegateForUpdate();
        updated.addSubscription(model);
    }

    @Override
    public Map<String, String> getSubscriptionUsage() {
        if (isUpdated())
            return updated.getSubscriptionUsage();
        return cached.getSubscriptionUsage();
    }

    @Override
    public void updateSubscription(Map<String, String> model) {
        getDelegateForUpdate();
        updated.updateSubscription(model);
    }

    @Override
    public Stream<ReportModel> getReports() {
        if (isUpdated())
            return updated.getReports();
        return cached.getReports().stream();
    }

    @Override
    public ReportModel addReport(ReportModel model) {
        getDelegateForUpdate();
        return updated.addReport(model);
    }

    @Override
    public ReportModel getReportById(String id) {
        if (isUpdated())
            return updated.getReportById(id);
        return cached.getReports().stream().filter(x -> x.getId().equals(id)).findFirst().get();
    }

    @Override
    public void updateReport(ReportModel model) {
        getDelegateForUpdate();
        updated.updateReport(model);
    }

    @Override
    public Stream<MgmtAccessModel> getMgmtAccessKeys() {
        if (isUpdated())
            return updated.getMgmtAccessKeys();
        return cached.getMgmtAccessModels().stream();
    }

    @Override
    public MgmtAccessModel addMgmtAccess(MgmtAccessModel model) {
        getDelegateForUpdate();
        return updated.addMgmtAccess(model);
    }

    @Override
    public MgmtAccessModel getMgmtAccessById(String id) {
        if (isUpdated())
            return updated.getMgmtAccessById(id);
        Optional<MgmtAccessModel> optionalMgmtAccessModel = cached.getMgmtAccessModels().stream()
                .filter(x -> x.getId().equals(id)).findFirst();
        return optionalMgmtAccessModel.orElse(null);
    }

    @Override
    public MgmtAccessModel updateMgmtAccess(MgmtAccessModel model) {
        getDelegateForUpdate();
        return updated.updateMgmtAccess(model);
    }

    @Override
    public MgmtAccessModel rotateMgmtAccess(MgmtAccessModel model) {
        getDelegateForUpdate();
        return updated.rotateMgmtAccess(model);
    }

    @Override
    public void removeMgmtAccess(String id) {
        getDelegateForUpdate();
        updated.removeMgmtAccess(id);
    }

    @Override
    public Stream<EmailConfigModel> getEmailConfigsStream() {
        if (isUpdated())
            return updated.getEmailConfigsStream();
        return cached.getEmailConfigs().values().stream();
    }

    @Override
    public EmailConfigModel addEmailConfig(EmailConfigModel model) {
        getDelegateForUpdate();
        return updated.addEmailConfig(model);
    }

    @Override
    public void updateEmailConfig(EmailConfigModel model) {
        getDelegateForUpdate();
        updated.updateEmailConfig(model);
    }

    @Override
    public void removeEmailConfig(EmailConfigModel model) {
        getDelegateForUpdate();
        updated.removeEmailConfig(model);
    }

    @Override
    public EmailConfigModel getEmailConfigByProviderId(String id) {
        if (isUpdated())
            return updated.getEmailConfigByProviderId(id);
        return cached.getEmailConfigs().get(id);
    }

    @Override
    public Stream<MFAConfigModel> getMFAConfigsStream() {
        if (isUpdated())
            return updated.getMFAConfigsStream();
        return cached.getMfaConfigs().values().stream();
    }

    @Override
    public MFAConfigModel addMFAConfig(MFAConfigModel model) {
        getDelegateForUpdate();
        return updated.addMFAConfig(model);
    }

    @Override
    public void updateMFAConfig(MFAConfigModel model) {
        getDelegateForUpdate();
        updated.updateMFAConfig(model);
    }

    @Override
    public void removeMFAConfig(MFAConfigModel model) {
        getDelegateForUpdate();
        updated.removeMFAConfig(model);
    }

    @Override
    public MFAConfigModel getMFAConfigByProviderId(String id) {
        if (isUpdated())
            return updated.getMFAConfigByProviderId(id);
        return cached.getMfaConfigs().get(id);
    }


    @Override
    public Stream<CibaAuthConfigModel> getCibaConfigsStream() {
        return updated.getCibaConfigsStream();
    }

    @Override
    public CibaAuthConfigModel addCibaAuthConfig(CibaAuthConfigModel model) {
        getDelegateForUpdate();
        return updated.addCibaAuthConfig(model);
    }

    @Override
    public void updateCibaAuthConfig(CibaAuthConfigModel model) {
        getDelegateForUpdate();
        updated.updateCibaAuthConfig(model);
        if(updated != null) {
            cached.getCibaAuthConfigs().removeIf(x-> Objects.equals(x.getClientId(), model.getClientId()));
        }
        cached.getCibaAuthConfigs().add(model);
    }

    @Override
    public void removeCibaAuthConfig(CibaAuthConfigModel model) {
        getDelegateForUpdate();
        updated.removeCibaAuthConfig(model);
    }

    @Override
    public CibaAuthConfigModel getCibaAuthConfigByProviderId(String id, String clientId, String realmId) {
        Optional<CibaAuthConfigModel> optData = cached.getCibaAuthConfigs().stream().filter(x -> x.getCibaAuthProviderId().equals(id) && x.getClientId().equals(clientId)).findFirst();
        if (optData.isPresent())
            return optData.get();

        if (isUpdated()) return updated.getCibaAuthConfigByProviderId(id, clientId, realmId);
        return cached.getCibaAuthConfigByProviderId(id, clientId, realmId);
    }

    @Override
    public CibaAuthConfigModel getCibaAuthConfigByClientId(String clientId, String realmId) {
        Optional<CibaAuthConfigModel> optData = cached.getCibaAuthConfigs().stream().filter(x -> x.getClientId().equals(clientId)).findFirst();
        if (optData.isPresent())
            return optData.get();

        if (isUpdated()) return updated.getCibaAuthConfigByClientId(clientId, realmId);
        return cached.getCibaAuthConfigByClientId(clientId, realmId);
    }

    @Override
    public Stream<CibaNotificationConfigModel> getCibaNotificationConfigsStream() {
        return updated.getCibaNotificationConfigsStream();
    }

    @Override
    public CibaNotificationConfigModel addCibaNotificationConfig(CibaNotificationConfigModel model) {
        getDelegateForUpdate();
        return updated.addCibaNotificationConfig(model);
    }

    @Override
    public void updateCibaNotificationConfig(CibaNotificationConfigModel model) {
        getDelegateForUpdate();
        updated.updateCibaNotificationConfig(model);
    }

    @Override
    public void removeCibaNotificationConfig(CibaNotificationConfigModel model) {
        getDelegateForUpdate();
        updated.removeCibaNotificationConfig(model);
    }

    @Override
    public CibaNotificationConfigModel getCibaNotificationConfigByProviderId(String id, String clientId, String realmId) {
        Optional<CibaNotificationConfigModel> optData = cached.getCibaNotificationConfigs().stream().filter(x -> x.getCibaNotificationProviderId().equals(id) && x.getClientId().equals(clientId)).findFirst();
        if (optData.isPresent())
            return optData.get();

        if (isUpdated()) updated.getCibaNotificationConfigByProviderId(id, clientId, realmId);
        return cached.getCibaNotificationConfigByProviderId(id, clientId, realmId);
    }

    @Override
    public CibaNotificationConfigModel getCibaNotificationConfigByClientId(String clientId, String realmId) {
        Optional<CibaNotificationConfigModel> optData = cached.getCibaNotificationConfigs().stream().filter(x -> x.getClientId().equals(clientId)).findFirst();
        if (optData.isPresent())
            return optData.get();

        if (isUpdated()) updated.getCibaNotificationConfigByClientId(clientId, realmId);
        return cached.getCibaNotificationConfigByClientId(clientId, realmId);
    }

    @Override
    public Stream<CibaDeadLetterHookConfigModel> getCibaDeadLetterHookNotificationConfigsStream() {
        return updated.getCibaDeadLetterHookNotificationConfigsStream();
    }

    @Override
    public CibaDeadLetterHookConfigModel addCibaResultHookNotificationConfig(CibaDeadLetterHookConfigModel model) {
        getDelegateForUpdate();
        return updated.addCibaResultHookNotificationConfig(model);
    }

    @Override
    public void updateCibaResultHookNotificationConfig(CibaDeadLetterHookConfigModel model) {
        getDelegateForUpdate();
        updated.updateCibaResultHookNotificationConfig(model);
    }

    @Override
    public void removeDeadLetterHookNotificationConfig(CibaDeadLetterHookConfigModel model) {
        getDelegateForUpdate();
        updated.removeDeadLetterHookNotificationConfig(model);
    }

    @Override
    public CibaDeadLetterHookConfigModel getDeadLetterHookNotificationConfigByProviderId(String id, String clientId, String realmId) {
        Optional<CibaDeadLetterHookConfigModel> optData = cached.getCibaDeadLetterHookNotificationConfigs().stream().filter(x -> x.getCibaResultHookProviderId().equals(id) && x.getClientId().equals(clientId)).findFirst();
        if (optData.isPresent())
            return optData.get();

        if (isUpdated()) updated.getCibaNotificationConfigByProviderId(id, clientId, realmId);
        return cached.getCibaResultHookNotificationConfigByProviderId(id, clientId, realmId);
    }

    @Override
    public CibaDeadLetterHookConfigModel getDeadLetterHookNotificationConfigByClientId(String clientId, String realmId) {
        Optional<CibaDeadLetterHookConfigModel> optData = cached.getCibaDeadLetterHookNotificationConfigs().stream().filter(x -> x.getClientId().equals(clientId)).findFirst();
        if (optData.isPresent())
            return optData.get();

        if (isUpdated()) updated.getDeadLetterHookNotificationConfigByClientId(clientId, realmId);
        return cached.getCibaResultHookNotificationConfigByClientId(clientId, realmId);
    }

    @Override
    public CibaStateModel addCibaState(CibaStateModel model) {
        return session.getProvider(TransactionStateProvider.class).addCibaState(model);
    }

    @Override
    public void removeCibaState(CibaStateModel model) {
        session.getProvider(TransactionStateProvider.class).removeCibaState(model);
    }

    @Override
    public CibaStateModel getCibaStateById(String cibaStateId) {
        return session.getProvider(TransactionStateProvider.class).getCibaStateById(cibaStateId);
    }

    @Override
    public TrxModel addAttribute(String trxId, String name, String value) {
        return session.getProvider(TransactionAttributeProvider.class).addAttribute(trxId, name, value);
    }

    @Override
    public void addTrxAttributes(String trxId, Map<String, String> claims) {
        session.getProvider(TransactionAttributeProvider.class).addAttributes(trxId,claims);
    }
    @Override
    public void removeTrxAttributes(String trxId, List<String> claimsToBeRemoved) {
        session.getProvider(TransactionAttributeProvider.class).removeAttributes(trxId, claimsToBeRemoved);
    }


    @Override
    public UserTransactionModel addTransactionStatus(String trxId, String userId, String status, Long expiresAt) {
        return session.getProvider(UserTransactionStatusProvider.class)
                .addTransactionStatus(session.getContext().getRealm().getId(), trxId, userId, status, expiresAt);
    }

    @Override
    public UserTransactionModel getUserTransactionDetails(String trxId, String userId) {
        return session.getProvider(UserTransactionStatusProvider.class).getUserTransactionDetails(trxId, userId);
    }

    @Override
    public List<ProviderModel> getProviders(String realmId, Integer firstResult, Integer max, String search) {
        return session.getProvider(APIConnectorProvider.class)
                .getProviderDetails(realmId, firstResult, max, search);
    }

    @Override
    public List<VerifierModel> getVerifiers(String realmId, Integer firstResult, Integer max, String search, String sortBy) {
        return session.getProvider(APIConnectorProvider.class)
                .getVerifierDetails(realmId, firstResult, max, search, sortBy
                );
    }

    @Override
    public List<APIPriorityModel> getVerifierProviderDetails(String realmId, Integer firstResult, Integer max, String search,String category, String sortBy) {
        return session.getProvider(APIConnectorProvider.class).getVerifierProviderDetails(realmId, firstResult, max, search, category, sortBy);
    }

    @Override
    public List<String> getVerifierCategories(String realmId) {
        return session.getProvider(APIConnectorProvider.class).getVerifierCategories(realmId);
    }

    @Override
    public List<String> getAllVerifiersName(String verifierName) {
        return session.getProvider(APIConnectorProvider.class).getAllVerifiersName(verifierName);
    }

    @Override
    public APIPriorityModel getVerifierAPIDetails(String verifierName, String realmId) {
        return session.getProvider(APIConnectorProvider.class).getVerifierAPIDetails(verifierName, realmId);
    }

    @Override
    public APIVerifierConfigModel getVerifierConfigs(String verifierName, String providerName) {
        return session.getProvider(APIConnectorProvider.class).getVerifierConfigs(verifierName, providerName);
    }

    @Override
    public List<APIVerifierConfigModel> getAPIVerifierConfigModels() {
        return session.getProvider(APIConnectorProvider.class).getAPIVerifierConfigsModels();
    }

    @Override
    public List<String> getClaims(String verification, String provider) {
        return session.getProvider(APIConnectorProvider.class)
                .getClaims(verification, provider);
    }

    @Override
    public void updateTransactionStatus(String trxId, String userId, String status) {
        session.getProvider(UserTransactionStatusProvider.class)
                .updateTransactionStatus(session.getContext().getRealm().getId(), trxId, userId, status);
    }

    @Override
    public void removeUserTransactionStatus(String userId) {
        session.getProvider(UserTransactionStatusProvider.class)
                .removeUserTransactionStatus(session.getContext().getRealm().getId(), userId);
    }

    @Override
    public List<UserTransactionModel> getUserTransactionStatus(String userId) {
        return session.getProvider(UserTransactionStatusProvider.class)
                .getUserTransactionStatus(session.getContext().getRealm().getId(),userId);
    }

    @Override
    public void removeTrxAttribute(String trxId, String name) {
        session.getProvider(TransactionAttributeProvider.class).removeTrxAttribute(trxId, name);
    }

    @Override
    public List<TermsAndConditionsModel> getTermsAndConditionsByVersion(String alias, String version) {
        //TODO: fix me
//        getDelegateForUpdate();
//        return updated.getTermsAndConditionsByVersion(alias, version);
        if(isUpdated())
            return updated.getTermsAndConditionsByVersion(alias, version);
        return cached.getTermsAndConditionsByVersion(alias, version);
    }

    @Override
    public List<TermsAndConditionsModel> getTermsAndConditionsVersions(String alias) {
        //TODO: fix me
        // below is the changes done for todo
        if(isUpdated())
            return updated.getTermsAndConditionsVersions(alias);
        return cached.getTermsAndConditionsVersions(alias);
    }

    @Override
    public TermsAndConditionsModel addTermsAndConditions(String alias, String realmId, String id, String version, String type, String content) {
        getDelegateForUpdate();
        return updated.addTermsAndConditions(alias, realmId, id, version, type, content);
    }

    @Override
    public void removeTermsAndConditionsVersions(String alias) {
        getDelegateForUpdate();
        updated.removeTermsAndConditionsVersions(alias);
    }

    @Override
    public void removeTermsAndConditionsVersionByVersion(String alias, String version) {
        getDelegateForUpdate();
        updated.removeTermsAndConditionsVersionByVersion(alias, version);
    }

    @Override
    public void updateTermsAndConditionsVersion(String id, String type, String content, String alias, String version) {
        getDelegateForUpdate();
        updated.updateTermsAndConditionsVersion(id, type, content, alias, version);
    }

    @Override
    public List<TermsAndConditionsModel> getTermsAndConditions(String realmId) {
        //TODO: fix me
//        getDelegateForUpdate();
        if(isUpdated())
            return updated.getTermsAndConditions(realmId);
        return cached.getTermsAndConditions(realmId);
    }

    @Override
    public void removeTrx(String trxId) {
        session.getProvider(TransactionAttributeProvider.class).removeTrx(trxId);
    }

    @Override
    public TrxModel getTrx(String trxId) {
        return session.getProvider(TransactionAttributeProvider.class).getTrx(trxId);
    }

    @Override
    public String getTrxValue(String trxId, String name) {
        return session.getProvider(TransactionAttributeProvider.class).getTrxValue(trxId, name);
    }

    @Override
    public boolean hasTrxValueAttribute(String trxId, String name) {
        return session.getProvider(TransactionAttributeProvider.class).hasTrxValueAttribute(trxId, name);
    }

    @Override
    public boolean hasTrxValueAttribute(String trxId, String name,String expectedValue) {
        return session.getProvider(TransactionAttributeProvider.class).hasTrxValueAttribute(trxId, name, expectedValue);
    }

    @Override
    public WebDeviceModel addWebDevice(WebDeviceModel model) {
        getDelegateForUpdate();
        return updated.addWebDevice(model);
    }

    @Override
    public void removeWebDevice(WebDeviceModel model) {
        getDelegateForUpdate();
        updated.removeWebDevice(model);
    }

    @Override
    public WebDeviceModel getWebDeviceById(String deviceId) {
        //TODO: fix me
        getDelegateForUpdate();
        return updated.getWebDeviceById(deviceId);
    }

    @Override
    public Stream<UserSyncConfigModel> getUserSyncConfigsStream() {
        if (isUpdated()) return updated.getUserSyncConfigsStream();
        return cached.getUserSyncConfigs().stream();
    }

    @Override
    public UserSyncConfigModel addUserSyncConfigModel(UserSyncConfigModel model) {
        getDelegateForUpdate();
        return updated.addUserSyncConfigModel(model);
    }

    @Override
    public void updateUserSyncConfigModel(UserSyncConfigModel model) {
        getDelegateForUpdate();
        updated.updateUserSyncConfigModel(model);
    }

    @Override
    public void removeUserSyncConfigModel(UserSyncConfigModel model) {
        getDelegateForUpdate();
        updated.removeUserSyncConfigModel(model);
    }

    @Override
    public UserSyncConfigModel getUserSyncConfigByProviderId(String id) {
        //TODO: fix me
//        getDelegateForUpdate();
        if(isUpdated())
            return updated.getUserSyncConfigByProviderId(id);
        return cached.getUserSyncConfigs().stream().filter(x -> x.getProviderId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public Stream<ActionModel> getActionStream() {
        if (isUpdated())
            return updated.getActionStream();
        return cached.getActionModelList().stream();
    }

    @Override
    public Stream<EztovrfyMarketplaceTemplateModel> getEztovrfyMarketplaceTemplateStream() {
        if (isUpdated())
            return updated.getEztovrfyMarketplaceTemplateStream();
        return cached.getEztovrfyMarketplaceTemplateModelList().stream();
    }

    @Override
    public List<EztovrfyMarketplaceTemplateModel> searchEztovrfyMarketplaceTemplateParams(String workflowName, String templateTag) {
        getDelegateForUpdate();
        return updated.searchEztovrfyMarketplaceTemplateParams(workflowName, templateTag);
    }
    @Override
    public ActionModel addAction(ActionModel model) {
        getDelegateForUpdate();
        return updated.addAction(model);
    }

    @Override
    public EztovrfyMarketplaceTemplateModel addEztovrfyMarketplaceTemplate(EztovrfyMarketplaceTemplateModel model) {
        getDelegateForUpdate();
        return updated.addEztovrfyMarketplaceTemplate(model);
    }

    @Override
    public boolean removeEztovrfyMarketplaceTemplate(EztovrfyMarketplaceTemplateModel model) {
        getDelegateForUpdate();
        return updated.removeEztovrfyMarketplaceTemplate(model);
    }

    @Override
    public Stream<EztovrfyMarketplaceCategoryModel> getEztovrfyMarketplaceCategoriesStream() {
        if (isUpdated())
            return updated.getEztovrfyMarketplaceCategoriesStream();
        return cached.getEztovrfyMarketplaceCategoryModelListList().stream();
    }

    @Override
    public EztovrfyMarketplaceCategoryModel addEztovrfyMarketplaceCategory(EztovrfyMarketplaceCategoryModel model) {
        getDelegateForUpdate();
        return updated.addEztovrfyMarketplaceCategory(model);
    }

    @Override
    public boolean removeEztovrfyMarketplaceCategory(EztovrfyMarketplaceCategoryModel model) {
        getDelegateForUpdate();
        return updated.removeEztovrfyMarketplaceCategory(model);
    }

    @Override
    public void updateAction(ActionModel model) {
        getDelegateForUpdate();
        updated.updateAction(model);
    }

    @Override
    public void removeAction(ActionModel model) {
        getDelegateForUpdate();
        updated.removeAction(model);
    }

    @Override
    public ActionModel getActionById(String id) {
        //TODO: fix me
//        getDelegateForUpdate();
        if(isUpdated())
            return updated.getActionById(id);
        return cached.getActionModelList().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
//        return updated.getActionById(id);
    }

    @Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream(String actionId) {
        if (isUpdated()) return updated.getRequiredActionProvidersStream(actionId);
        return cached.getRequiredActionProviderList().stream().filter(x -> x.getActionId().equals(actionId));
    }

    @Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream() {
        if (isUpdated()) return updated.getRequiredActionProvidersStream();
        return cached.getRequiredActionProviderList().stream();
    }


    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        getDelegateForUpdate();
        return updated.addAuthenticationFlow(model);
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        if (isUpdated())
            return updated.getAuthenticationFlowById(id);
        return cached.getAuthenticationFlows().get(id);
    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model, boolean force) {
        getDelegateForUpdate();
        updated.removeAuthenticationFlow(model, force);

    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        getDelegateForUpdate();
        updated.updateAuthenticationFlow(model);

    }

    @Override
    public Stream<AuthenticationExecutionModel> getAuthenticationExecutionsStream(String flowId) {
        if (isUpdated())
            return updated.getAuthenticationExecutionsStream(flowId);
        return cached.getAuthenticationExecutions().get(flowId).stream();
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        if (isUpdated())
            return updated.getAuthenticationExecutionById(id);
        return cached.getExecutionsById().get(id);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        if (isUpdated())
            return updated.getAuthenticationExecutionByFlowId(flowId);
        return cached.getAuthenticationExecutionByFlowId(flowId);
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        getDelegateForUpdate();
        return updated.addAuthenticatorExecution(model);
    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        getDelegateForUpdate();
        updated.updateAuthenticatorExecution(model);

    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        getDelegateForUpdate();
        updated.removeAuthenticatorExecution(model);

    }

    @Override
    public Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream() {
        if (isUpdated())
            return updated.getAuthenticatorConfigsStream();
        return cached.getAuthenticatorConfigs().values().stream();
    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        getDelegateForUpdate();
        return updated.addAuthenticatorConfig(model);
    }

    @Override
    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        getDelegateForUpdate();
        updated.updateAuthenticatorConfig(model);

    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        getDelegateForUpdate();
        updated.removeAuthenticatorConfig(model);

    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        if (isUpdated())
            return updated.getAuthenticatorConfigById(id);
        return cached.getAuthenticatorConfigs().get(id);
    }

    /*@Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream() {
        if (isUpdated())
            return updated.getRequiredActionProvidersStream();
        return cached.getRequiredActionProviderList().stream();
    }*/

    @Override
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        getDelegateForUpdate();
        return updated.addRequiredActionProvider(model);
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        getDelegateForUpdate();
        updated.updateRequiredActionProvider(model);

    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        getDelegateForUpdate();
        updated.removeRequiredActionProvider(model);

    }

    @Override
    public void removeRequiredActionProviders(String actionId) {
        getDelegateForUpdate();
        updated.removeRequiredActionProviders(actionId);
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        //TODO: fix me
//        getDelegateForUpdate();
        if(isUpdated())
            return updated.getRequiredActionProviderById(id);
        return cached.getRequiredActionProviderById(id);
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        //TODO: fix me
//        getDelegateForUpdate();
        if(isUpdated())
            return updated.getRequiredActionProviderByAlias(alias);
        return cached.getRequiredActionProviderByAlias(alias);
    }

    @Override
    public GroupModel createGroup(String id, String name, GroupModel toParent) {
        return cacheSession.createGroup(this, id, name, toParent);
    }

    @Override
    public void moveGroup(GroupModel group, GroupModel toParent) {
        cacheSession.moveGroup(this, group, toParent);
    }

    @Override
    public GroupModel getGroupById(String id) {
        return cacheSession.getGroupById(this, id);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return cacheSession.getGroupsStream(this);
    }

    @Override
    public Long getGroupsCount(Boolean onlyTopGroups) {
        return cacheSession.getGroupsCount(this, onlyTopGroups);
    }

    @Override
    public Long getGroupsCountByNameContaining(String search) {
        return cacheSession.getGroupsCountByNameContaining(this, search);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream() {
        return cacheSession.getTopLevelGroupsStream(this);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max) {
        return cacheSession.getTopLevelGroupsStream(this, first, max);
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(String search, Integer first, Integer max) {
        return cacheSession.searchForGroupByNameStream(this, search, first, max);
    }

    @Override
    public boolean removeGroup(GroupModel group) {
        return cacheSession.removeGroup(this, group);
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream() {
        if (isUpdated())
            return updated.getClientScopesStream();
        return cached.getClientScopes().stream().map(scope -> {
            ClientScopeModel model = cacheSession.getClientScopeById(this, scope);
            if (model == null) {
                throw new IllegalStateException("Cached clientScope not found: " + scope);
            }
            return model;
        });
    }

    @Override
    public ClientScopeModel addClientScope(String name) {
        RealmModel realm = getDelegateForUpdate();
        ClientScopeModel clientScope = updated.addClientScope(name);
        cacheSession.registerClientScopeInvalidation(clientScope.getId(), realm.getId());
        return clientScope;
    }

    @Override
    public ClientScopeModel addClientScope(String id, String name) {
        RealmModel realm = getDelegateForUpdate();
        ClientScopeModel clientScope = updated.addClientScope(id, name);
        cacheSession.registerClientScopeInvalidation(clientScope.getId(), realm.getId());
        return clientScope;
    }

    @Override
    public boolean removeClientScope(String id) {
        RealmModel realm = getDelegateForUpdate();
        cacheSession.registerClientScopeInvalidation(id, realm.getId());
        return updated.removeClientScope(id);
    }

    @Override
    public ClientScopeModel getClientScopeById(String id) {
        if (isUpdated())
            return updated.getClientScopeById(id);
        return cacheSession.getClientScopeById(this, id);
    }

    @Override
    public void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        getDelegateForUpdate();
        updated.addDefaultClientScope(clientScope, defaultScope);
    }

    @Override
    public void removeDefaultClientScope(ClientScopeModel clientScope) {
        getDelegateForUpdate();
        updated.removeDefaultClientScope(clientScope);
    }

    @Override
    public Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope) {
        if (isUpdated()) return updated.getDefaultClientScopesStream(defaultScope);
        List<String> clientScopeIds = defaultScope ? cached.getDefaultDefaultClientScopes() : cached.getOptionalDefaultClientScopes();
        return clientScopeIds.stream()
                .map(scope -> cacheSession.getClientScopeById(scope, this))
                .filter(Objects::nonNull);
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        getDelegateForUpdate();
        executeEvictions(model);
        return updated.addComponentModel(model);
    }

    @Override
    public ComponentModel importComponentModel(ComponentModel model) {
        getDelegateForUpdate();
        executeEvictions(model);
        return updated.importComponentModel(model);
    }

    public void executeEvictions(ComponentModel model) {
        if (model == null)
            return;

        // if user cache is disabled this is null
        UserCache userCache = session.userCache();
        if (userCache != null) {
            // If not realm component, check to see if it is a user storage provider child
            // component (i.e. LDAP mapper)
            if (model.getParentId() != null && !model.getParentId().equals(getId())) {
                ComponentModel parent = getComponent(model.getParentId());
                if (parent != null && UserStorageProvider.class.getName().equals(parent.getProviderType())) {
                    userCache.evict(this);
                }
                return;
            }

            // invalidate entire user cache if we're dealing with user storage SPI
            if (UserStorageProvider.class.getName().equals(model.getProviderType())) {
                userCache.evict(this);
            }
        }

        // invalidate entire realm if we're dealing with client storage SPI
        // entire realm because of client roles, client lists, and clients
        if (ClientStorageProvider.class.getName().equals(model.getProviderType())) {
            cacheSession.evictRealmOnRemoval(this);
        }
    }

    @Override
    public void updateComponent(ComponentModel component) {
        getDelegateForUpdate();
        executeEvictions(component);
        updated.updateComponent(component);

    }

    @Override
    public void removeComponent(ComponentModel component) {
        getDelegateForUpdate();
        executeEvictions(component);
        updated.removeComponent(component);

    }

    @Override
    public void removeComponents(String parentId) {
        getDelegateForUpdate();
        updated.removeComponents(parentId);

    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId, String providerType) {
        if (isUpdated())
            return updated.getComponentsStream(parentId, providerType);
        return cached.getComponentsByParentAndType().getList(parentId + providerType).stream();
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId) {
        if (isUpdated())
            return updated.getComponentsStream(parentId);
        return cached.getComponentsByParent().getList(parentId).stream();
    }

    @Override
    public Stream<ComponentModel> getComponentsStream() {
        if (isUpdated())
            return updated.getComponentsStream();
        return cached.getComponents().values().stream();
    }

    @Override
    public ComponentModel getComponent(String id) {
        if (isUpdated())
            return updated.getComponent(id);
        return cached.getComponents().get(id);
    }

    @Override
    public void setAttribute(String name, String value) {
        getDelegateForUpdate();
        updated.setAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, Boolean value) {
        getDelegateForUpdate();
        updated.setAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, Integer value) {
        getDelegateForUpdate();
        updated.setAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, Long value) {
        getDelegateForUpdate();
        updated.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        getDelegateForUpdate();
        updated.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        if (isUpdated())
            return updated.getAttribute(name);
        return cached.getAttribute(name);
    }

    @Override
    public Integer getAttribute(String name, Integer defaultValue) {
        if (isUpdated())
            return updated.getAttribute(name, defaultValue);
        return cached.getAttribute(name, defaultValue);
    }

    @Override
    public Long getAttribute(String name, Long defaultValue) {
        if (isUpdated())
            return updated.getAttribute(name, defaultValue);
        return cached.getAttribute(name, defaultValue);
    }

    @Override
    public Boolean getAttribute(String name, Boolean defaultValue) {
        if (isUpdated())
            return updated.getAttribute(name, defaultValue);
        return cached.getAttribute(name, defaultValue);
    }

    @Override
    public Map<String, String> getAttributes() {
        if (isUpdated())
            return updated.getAttributes();
        return cached.getAttributes();
    }

    @Override
    public void createOrUpdateRealmLocalizationTexts(String locale, Map<String, String> localizationTexts) {
        getDelegateForUpdate();
        updated.createOrUpdateRealmLocalizationTexts(locale, localizationTexts);
    }

    @Override
    public boolean removeRealmLocalizationTexts(String locale) {
        getDelegateForUpdate();
        return updated.removeRealmLocalizationTexts(locale);
    }

    @Override
    public Map<String, Map<String, String>> getRealmLocalizationTexts() {
        if (isUpdated())
            return updated.getRealmLocalizationTexts();
        return cached.getRealmLocalizationTexts();
    }

    @Override
    public Map<String, String> getRealmLocalizationTextsByLocale(String locale) {
        if (isUpdated())
            return updated.getRealmLocalizationTextsByLocale(locale);

        Map<String, String> localizationTexts = Collections.emptyMap();
        if (cached.getRealmLocalizationTexts() != null && cached.getRealmLocalizationTexts().containsKey(locale)) {
            localizationTexts = cached.getRealmLocalizationTexts().get(locale);
        }
        return Collections.unmodifiableMap(localizationTexts);
    }

    private RealmModel getRealm() {
        return cacheSession.getRealmDelegate().getRealm(cached.getId());
    }

    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(int expiration, int count) {
        getDelegateForUpdate();
        return updated.createClientInitialAccessModel(expiration, count);
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(String id) {
        return getDelegateForUpdate().getClientInitialAccessModel(id);
    }

    @Override
    public void removeClientInitialAccessModel(String id) {
        getDelegateForUpdate().removeClientInitialAccessModel(id);
    }

    @Override
    public Stream<ClientInitialAccessModel> getClientInitialAccesses() {
        return getDelegateForUpdate().getClientInitialAccesses();
    }

    @Override
    public void decreaseRemainingCount(ClientInitialAccessModel clientInitialAccess) {
        getDelegateForUpdate().decreaseRemainingCount(clientInitialAccess);
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }

    private void updateTenantOnBoardingDueToSubscriptionChange(String changedProviderId) {
        String customSubscription = session.getContext().getRealm().getAttribute("custom_subscription");
        String onboardingCompletedSteps = session.getContext().getRealm().getAttribute("onboarding-steps-completed");

        if (customSubscription != null && !customSubscription.contains(changedProviderId)) {
            if (onboardingCompletedSteps == null) {
                session.getContext().getRealm().setAttribute("custom_subscription", customSubscription + "," + changedProviderId);
            } else if (onboardingCompletedSteps != null && !onboardingCompletedSteps.contains(changedProviderId)) {
                session.getContext().getRealm().setAttribute("custom_subscription", customSubscription + "," + changedProviderId);
            }
        } else if (customSubscription == null) {
            if (onboardingCompletedSteps == null) {
                session.getContext().getRealm().setAttribute("custom_subscription", changedProviderId);
            } else if (onboardingCompletedSteps != null && !onboardingCompletedSteps.contains(changedProviderId)) {
                session.getContext().getRealm().setAttribute("custom_subscription", changedProviderId);
            }
        }
        session.getContext().getRealm().setAttribute("obSubscriptionChange", true);
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        //TODO: fix me
//        getDelegateForUpdate();
        if(isUpdated())
            return updated.getAuthenticationFlowBindingOverride(binding);
        return cached.getAuthFlowBindings().get(binding);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        //TODO: fix me
//        getDelegateForUpdate();
        if(isUpdated())
            return updated.getAuthenticationFlowBindingOverrides();
        return cached.getAuthFlowBindings();
    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String binding) {
        getDelegateForUpdate();
        updated.removeAuthenticationFlowBindingOverride(binding);
    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        getDelegateForUpdate();
        updated.setAuthenticationFlowBindingOverride(binding, flowId);
    }

    @Override
    public Boolean getApplicationConnections(String connId) {
        if (isUpdated())
            return updated.getApplicationConnections(connId);
        return cached.getAppConnections().get(connId);
    }

    @Override
    public Map<String, Boolean> getApplicationsConnections() {
        if (isUpdated())
            return updated.getApplicationsConnections();
        Map<String, Boolean> copy = new HashMap<>();
        copy.putAll(cached.getAppConnections());
        return copy;
    }

    @Override
    public void removeApplicationConnections(String connId) {
        getDelegateForUpdate();
        updated.removeApplicationConnections(connId);
    }

    @Override
    public void setApplicationConnections(String connId, Boolean connEnabled) {
        getDelegateForUpdate();
        updated.setApplicationConnections(connId, connEnabled);
    }

    @Override
    public Map<String, Boolean> getMFAConnections() {
        if (isUpdated())
            return updated.getMFAConnections();
        Map<String, Boolean> copy = new HashMap<>();
        copy.putAll(cached.getMfaConnections());
        return copy;
    }

    @Override
    public void removeMFAConnections(String connId) {
        getDelegateForUpdate();
        updated.removeMFAConnections(connId);
    }

    @Override
    public void setMFAConnections(String connId, Boolean connEnabled) {
        getDelegateForUpdate();
        updated.setMFAConnections(connId, connEnabled);
    }

    @Override
    public Stream<String> getSupportedLocalesStream() {
        if (isUpdated())
            return updated.getSupportedLocalesStream();
        return cached.getSupportedLocales().stream();
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        getDelegateForUpdate();
        updated.setSupportedLocales(locales);
    }

    @Override
    public Stream<String> getPreferredLoginLocalesStream() {
        if (isUpdated())
            return updated.getPreferredLoginLocalesStream();
        return cached.getPreferredLoginLocales().stream();
    }

    @Override
    public void setPreferredLoginLocales(Set<String> locales) {
        getDelegateForUpdate();
        updated.setPreferredLoginLocales(locales);
    }

    @Override
    public Boolean isSandboxMode() {
        if (isUpdated())
            return updated.isSandboxMode();
        return cached.isSandboxMode();
    }

    @Override
    public void setSandboxMode(Boolean sandboxMode) {
        getDelegateForUpdate();
        updated.setSandboxMode(sandboxMode);
    }

    @Override
    public Stream<CustomDomainMappingModel> getCustomDomainMappings() {
        if (isUpdated())
            return updated.getCustomDomainMappings();
        return cached.getCustomDomainMappingModels().stream();
    }

    @Override
    public CustomDomainMappingModel getCustomDomainMappingByDomain(String domain) {
        if (isUpdated())
            return updated.getCustomDomainMappingByDomain(domain);
        return cached.getCustomDomainMappingByDomain(domain);
    }

    @Override
    public CustomDomainMappingModel updateCustomDomainMapping(CustomDomainMappingModel customDomainMappingModel) {
        getDelegateForUpdate();
        return updated.updateCustomDomainMapping(customDomainMappingModel);
    }

    @Override
    public void removeCustomDomainMapping(String domain) {
        getDelegateForUpdate();
        updated.removeCustomDomainMapping(domain);
    }
}

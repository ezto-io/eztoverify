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

package org.keycloak.models.jpa;

import com.qlikverify.apiConnector.spi.APIConnectorProvider;
import com.qlikverify.transactionAttribute.spi.TransactionAttributeProvider;
import com.qlikverify.transactionState.spi.TransactionStateProvider;
import com.qlikverify.userTransactionStatus.spi.UserTransactionStatusProvider;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.*;
import org.keycloak.models.utils.ComponentUtil;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.JsonSerialization;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static org.keycloak.models.utils.KeycloakModelUtils.generateRandomId;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdapter implements RealmModel, JpaModel<RealmEntity> {
    protected static final Logger logger = Logger.getLogger(RealmAdapter.class);
    protected RealmEntity realm;
    protected EntityManager em;
    protected KeycloakSession session;

    @Override
    public Long getClientsCount() {
        return session.clients().getClientsCount(this);
    }

    private PasswordPolicy passwordPolicy;
    private OTPPolicy otpPolicy;

    public RealmAdapter(KeycloakSession session, EntityManager em, RealmEntity realm) {
        this.session = session;
        this.em = em;
        this.realm = realm;
    }

    public RealmEntity getEntity() {
        return realm;
    }

    @Override
    public String getId() {
        return realm.getId();
    }

    @Override
    public String getName() {
        return realm.getName();
    }

    @Override
    public void setName(String name) {
        realm.setName(name);
        em.flush();
    }

    @Override
    public String getDisplayName() {
        return getAttribute(RealmAttributes.DISPLAY_NAME);
    }

    @Override
    public void setDisplayName(String displayName) {
        setAttribute(RealmAttributes.DISPLAY_NAME, displayName);
    }

    @Override
    public String getDisplayNameHtml() {
        return getAttribute(RealmAttributes.DISPLAY_NAME_HTML);
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        setAttribute(RealmAttributes.DISPLAY_NAME_HTML, displayNameHtml);
    }

    @Override
    public boolean isEnabled() {
        return realm.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        realm.setEnabled(enabled);
        em.flush();
    }

    @Override
    public SslRequired getSslRequired() {
        return realm.getSslRequired() != null ? SslRequired.valueOf(realm.getSslRequired()) : null;
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        realm.setSslRequired(sslRequired.name());
        em.flush();
    }

    @Override
    public boolean isUserManagedAccessAllowed() {
        return realm.isAllowUserManagedAccess();
    }

    @Override
    public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {
        realm.setAllowUserManagedAccess(userManagedAccessAllowed);
        em.flush();
    }

    @Override
    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        realm.setRegistrationAllowed(registrationAllowed);
        em.flush();
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        return realm.isRegistrationEmailAsUsername();
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        realm.setRegistrationEmailAsUsername(registrationEmailAsUsername);
        if (registrationEmailAsUsername) realm.setDuplicateEmailsAllowed(false);
        em.flush();
    }

    @Override
    public boolean isRememberMe() {
        return realm.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        realm.setRememberMe(rememberMe);
        em.flush();
    }

    @Override
    public void setAttribute(String name, String value) {
        for (RealmAttributeEntity attr : realm.getAttributes()) {
            if (attr.getName().equals(name)) {
                attr.setValue(value);
                return;
            }
        }
        RealmAttributeEntity attr = new RealmAttributeEntity();
        attr.setName(name);
        attr.setValue(value);
        attr.setRealm(realm);
        em.persist(attr);
        realm.getAttributes().add(attr);
    }

    @Override
    public void setAttribute(String name, Boolean value) {
        setAttribute(name, value.toString());
    }

    @Override
    public void setAttribute(String name, Integer value) {
        setAttribute(name, value.toString());
    }

    @Override
    public void setAttribute(String name, Long value) {
        setAttribute(name, value.toString());
    }

    @Override
    public void removeAttribute(String name) {
        Iterator<RealmAttributeEntity> it = realm.getAttributes().iterator();
        while (it.hasNext()) {
            RealmAttributeEntity attr = it.next();
            if (attr.getName().equals(name)) {
                it.remove();
                em.remove(attr);
            }
        }
    }

    @Override
    public String getAttribute(String name) {
        for (RealmAttributeEntity attr : realm.getAttributes()) {
            if (attr.getName().equals(name)) {
                return attr.getValue();
            }
        }
        return null;
    }

    @Override
    public Integer getAttribute(String name, Integer defaultValue) {
        String v = getAttribute(name);
        return v != null ? Integer.parseInt(v) : defaultValue;

    }

    @Override
    public Long getAttribute(String name, Long defaultValue) {
        String v = getAttribute(name);
        return v != null ? Long.parseLong(v) : defaultValue;

    }

    @Override
    public Boolean getAttribute(String name, Boolean defaultValue) {
        String v = getAttribute(name);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;

    }

    @Override
    public Map<String, String> getAttributes() {
        // should always return a copy
        Map<String, String> result = new HashMap<String, String>();
        for (RealmAttributeEntity attr : realm.getAttributes()) {
            result.put(attr.getName(), attr.getValue());
        }
        return result;
    }

    @Override
    public String getDefaultSignatureAlgorithm() {
        return getAttribute("defaultSignatureAlgorithm");
    }

    @Override
    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        setAttribute("defaultSignatureAlgorithm", defaultSignatureAlgorithm);
    }

    @Override
    public boolean isBruteForceProtected() {
        return getAttribute("bruteForceProtected", false);
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        setAttribute("bruteForceProtected", value);
    }

    @Override
    public boolean isPermanentLockout() {
        return getAttribute("permanentLockout", false);
    }

    @Override
    public void setPermanentLockout(final boolean val) {
        setAttribute("permanentLockout", val);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return getAttribute("maxFailureWaitSeconds", 0);
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        setAttribute("maxFailureWaitSeconds", val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        return getAttribute("waitIncrementSeconds", 0);
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        setAttribute("waitIncrementSeconds", val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return getAttribute("quickLoginCheckMilliSeconds", 0L);
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        setAttribute("quickLoginCheckMilliSeconds", val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return getAttribute("minimumQuickLoginWaitSeconds", 0);
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        setAttribute("minimumQuickLoginWaitSeconds", val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        return getAttribute("maxDeltaTimeSeconds", 0);
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        setAttribute("maxDeltaTimeSeconds", val);
    }

    @Override
    public int getFailureFactor() {
        return getAttribute("failureFactor", 0);
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        setAttribute("failureFactor", failureFactor);
    }

    @Override
    public String getDefaultRegion() {
        return getAttribute("defaultRegion");
    }

    @Override
    public void setDefaultRegion(String region) {
        setAttribute("defaultRegion", region);
    }

    @Override
    public boolean isVerifyEmail() {
        return realm.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        realm.setVerifyEmail(verifyEmail);
        em.flush();
    }

    @Override
    public boolean isLoginWithEmailAllowed() {
        return realm.isLoginWithEmailAllowed();
    }

    @Override
    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        realm.setLoginWithEmailAllowed(loginWithEmailAllowed);
        if (loginWithEmailAllowed) realm.setDuplicateEmailsAllowed(false);
        em.flush();
    }

    @Override
    public boolean isDuplicateEmailsAllowed() {
        return realm.isDuplicateEmailsAllowed();
    }

    @Override
    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        realm.setDuplicateEmailsAllowed(duplicateEmailsAllowed);
        if (duplicateEmailsAllowed) {
            realm.setLoginWithEmailAllowed(false);
            realm.setRegistrationEmailAsUsername(false);
        }
        em.flush();
    }

    @Override
    public boolean isDuplicateMobNumAllowed() {
        return realm.isDuplicateMobNumAllowed();
    }

    @Override
    public void setDuplicateMobNumAllowed(boolean duplicateMobNumAllowed) {
        realm.setDuplicateMobNumAllowed(duplicateMobNumAllowed);
        em.flush();
    }

    @Override
    public boolean isResetPasswordAllowed() {
        return realm.isResetPasswordAllowed();
    }

    @Override
    public void setUserInviteWay(String inviteWay) {
        realm.setUserInviteWay(inviteWay);
        em.flush();
    }

    @Override
    public String getUserInviteWay() {
        return realm.getUserInviteWay();
    }

    @Override
    public void setUserInviteClientId(String clientId) {
        realm.setUserInviteClientId(clientId);
        em.flush();
    }

    @Override
    public String getUserInviteClientId() {
        return realm.getUserInviteClientId();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        realm.setResetPasswordAllowed(resetPasswordAllowed);
        em.flush();
    }

    @Override
    public boolean isEditUsernameAllowed() {
        return realm.isEditUsernameAllowed();
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        realm.setEditUsernameAllowed(editUsernameAllowed);
        em.flush();
    }

    @Override
    public int getNotBefore() {
        return realm.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        realm.setNotBefore(notBefore);
    }

    @Override
    public boolean isRevokeRefreshToken() {
        return realm.isRevokeRefreshToken();
    }

    @Override
    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        realm.setRevokeRefreshToken(revokeRefreshToken);
    }

    @Override
    public int getRefreshTokenMaxReuse() {
        return realm.getRefreshTokenMaxReuse();
    }

    @Override
    public void setRefreshTokenMaxReuse(int revokeRefreshTokenReuseCount) {
        realm.setRefreshTokenMaxReuse(revokeRefreshTokenReuseCount);
    }

    @Override
    public int getAccessTokenLifespan() {
        return realm.getAccessTokenLifespan();
    }

    @Override
    public void setAccessTokenLifespan(int tokenLifespan) {
        realm.setAccessTokenLifespan(tokenLifespan);
        em.flush();
    }

    @Override
    public int getAccessTokenLifespanForImplicitFlow() {
        return realm.getAccessTokenLifespanForImplicitFlow();
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        realm.setAccessTokenLifespanForImplicitFlow(seconds);
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        return realm.getSsoSessionIdleTimeout();
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        realm.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        return realm.getSsoSessionMaxLifespan();
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        realm.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getSsoSessionIdleTimeoutRememberMe() {
        return realm.getSsoSessionIdleTimeoutRememberMe();
    }

    @Override
    public void setSsoSessionIdleTimeoutRememberMe(int seconds) {
        realm.setSsoSessionIdleTimeoutRememberMe(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespanRememberMe() {
        return realm.getSsoSessionMaxLifespanRememberMe();
    }

    @Override
    public void setSsoSessionMaxLifespanRememberMe(int seconds) {
        realm.setSsoSessionMaxLifespanRememberMe(seconds);
    }

    @Override
    public int getOfflineSessionIdleTimeout() {
        return realm.getOfflineSessionIdleTimeout();
    }

    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        realm.setOfflineSessionIdleTimeout(seconds);
    }

    // KEYCLOAK-7688 Offline Session Max for Offline Token
    @Override
    public boolean isOfflineSessionMaxLifespanEnabled() {
        return getAttribute(RealmAttributes.OFFLINE_SESSION_MAX_LIFESPAN_ENABLED, false);
    }

    @Override
    public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        setAttribute(RealmAttributes.OFFLINE_SESSION_MAX_LIFESPAN_ENABLED, offlineSessionMaxLifespanEnabled);
    }

    @Override
    public int getOfflineSessionMaxLifespan() {
        return getAttribute(RealmAttributes.OFFLINE_SESSION_MAX_LIFESPAN, Constants.DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN);
    }

    @Override
    public void setOfflineSessionMaxLifespan(int seconds) {
        setAttribute(RealmAttributes.OFFLINE_SESSION_MAX_LIFESPAN, seconds);
    }

    @Override
    public int getClientSessionIdleTimeout() {
        return getAttribute(RealmAttributes.CLIENT_SESSION_IDLE_TIMEOUT, 0);
    }

    @Override
    public void setClientSessionIdleTimeout(int seconds) {
        setAttribute(RealmAttributes.CLIENT_SESSION_IDLE_TIMEOUT, seconds);
    }

    @Override
    public int getClientSessionMaxLifespan() {
        return getAttribute(RealmAttributes.CLIENT_SESSION_MAX_LIFESPAN, 0);
    }

    @Override
    public void setClientSessionMaxLifespan(int seconds) {
        setAttribute(RealmAttributes.CLIENT_SESSION_MAX_LIFESPAN, seconds);
    }

    @Override
    public int getClientOfflineSessionIdleTimeout() {
        return getAttribute(RealmAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT, 0);
    }

    @Override
    public void setClientOfflineSessionIdleTimeout(int seconds) {
        setAttribute(RealmAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT, seconds);
    }

    @Override
    public int getClientOfflineSessionMaxLifespan() {
        return getAttribute(RealmAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN, 0);
    }

    @Override
    public void setClientOfflineSessionMaxLifespan(int seconds) {
        setAttribute(RealmAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN, seconds);
    }

    @Override
    public int getAccessCodeLifespan() {
        return realm.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int accessCodeLifespan) {
        realm.setAccessCodeLifespan(accessCodeLifespan);
        em.flush();
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        return realm.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        realm.setAccessCodeLifespanUserAction(accessCodeLifespanUserAction);
        em.flush();
    }

    @Override
    public OAuth2DeviceConfig getOAuth2DeviceConfig() {
        return new OAuth2DeviceConfig(this);
    }

    @Override
    public CibaConfig getCibaPolicy() {
        return new CibaConfig(this);
    }

    @Override
    public ParConfig getParPolicy() {
        return new ParConfig(this);
    }

    @Override
    public Map<String, Integer> getUserActionTokenLifespans() {

        Map<String, Integer> userActionTokens = new HashMap<>();

        getAttributes().entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> nonNull(entry.getValue()))
                .filter(entry -> entry.getKey().startsWith(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "."))
                .forEach(entry -> userActionTokens.put(entry.getKey().substring(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN.length() + 1), Integer.valueOf(entry.getValue())));

        return Collections.unmodifiableMap(userActionTokens);
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        return realm.getAccessCodeLifespanLogin();
    }

    @Override
    public void setAccessCodeLifespanLogin(int accessCodeLifespanLogin) {
        realm.setAccessCodeLifespanLogin(accessCodeLifespanLogin);
        em.flush();
    }

    @Override
    public int getActionTokenGeneratedByAdminLifespan() {
        return getAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_ADMIN_LIFESPAN, 12 * 60 * 60);
    }

    @Override
    public void setActionTokenGeneratedByAdminLifespan(int actionTokenGeneratedByAdminLifespan) {
        setAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_ADMIN_LIFESPAN, actionTokenGeneratedByAdminLifespan);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan() {
        return getAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN, getAccessCodeLifespanUserAction());
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(int actionTokenGeneratedByUserLifespan) {
        setAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN, actionTokenGeneratedByUserLifespan);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan(String actionTokenId) {
        if (actionTokenId == null || getAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenId) == null) {
            return getActionTokenGeneratedByUserLifespan();
        }
        return getAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenId, getAccessCodeLifespanUserAction());
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(String actionTokenId, Integer actionTokenGeneratedByUserLifespan) {
        if (actionTokenGeneratedByUserLifespan != null)
            setAttribute(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenId, actionTokenGeneratedByUserLifespan);
    }

    @Override
    public boolean isEnabledOwnEmailProvider() {
        return getAttribute("enabledOwnEmailProvider", false);
    }

    @Override
    public void setOwnEmailProvider(boolean value) {
        setAttribute("enabledOwnEmailProvider", value);
    }

    @Override
    public boolean isEnabledOwnWhatsappProvider() {
        return true;
    }

    @Override
    public boolean isDNSEnabled() {
        return false;
    }

    @Override
    public boolean isEnabledOwnSmsProvider() {
        return getAttribute("enabledOwnSmsProvider", false);
    }

    @Override
    public void setOwnSmsProvider(boolean value) {
        setAttribute("enabledOwnSmsProvider", value);
    }

    @Override
    public String getRealmLogo() {
        return getAttribute("realmLogo");
    }

    @Override
    public void setRealmLogo(String logoUrl) {
        setAttribute("realmLogo", logoUrl);
    }

    protected RequiredCredentialModel initRequiredCredentialModel(String type) {
        RequiredCredentialModel model = RequiredCredentialModel.BUILT_IN.get(type);
        if (model == null) {
            throw new RuntimeException("Unknown credential type " + type);
        }
        return model;
    }

    @Override
    public void addRequiredCredential(String type) {
        RequiredCredentialModel model = initRequiredCredentialModel(type);
        addRequiredCredential(model);
        em.flush();
    }

    public void addRequiredCredential(RequiredCredentialModel model) {
        RequiredCredentialEntity entity = new RequiredCredentialEntity();
        entity.setRealm(realm);
        entity.setInput(model.isInput());
        entity.setSecret(model.isSecret());
        entity.setType(model.getType());
        entity.setFormLabel(model.getFormLabel());
        em.persist(entity);
        realm.getRequiredCredentials().add(entity);
        em.flush();
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        Collection<RequiredCredentialEntity> relationships = realm.getRequiredCredentials();
        if (relationships == null) relationships = new ArrayList<RequiredCredentialEntity>();

        Set<String> already = new HashSet<String>();
        List<RequiredCredentialEntity> remove = new ArrayList<RequiredCredentialEntity>();
        for (RequiredCredentialEntity rel : relationships) {
            if (!creds.contains(rel.getType())) {
                remove.add(rel);
            } else {
                already.add(rel.getType());
            }
        }
        for (RequiredCredentialEntity entity : remove) {
            relationships.remove(entity);
            em.remove(entity);
        }
        for (String cred : creds) {
            if (!already.contains(cred)) {
                addRequiredCredential(cred);
            }
        }
        em.flush();
    }


    @Override
    public Stream<RequiredCredentialModel> getRequiredCredentialsStream() {
        return realm.getRequiredCredentials().stream().map(this::toRequiredCredentialModel);
    }

    @Override
    @Deprecated
    public Stream<String> getDefaultRolesStream() {
        return getDefaultRole().getCompositesStream().filter(this::isRealmRole).map(RoleModel::getName);
    }

    private boolean isRealmRole(RoleModel role) {
        return !role.isClientRole();
    }

    @Override
    @Deprecated
    public void addDefaultRole(String name) {
        getDefaultRole().addCompositeRole(getOrAddRoleId(name));
    }

    private RoleModel getOrAddRoleId(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            role = addRole(name);
        }
        return role;
    }

    @Override
    @Deprecated
    public void removeDefaultRoles(String... defaultRoles) {
        for (String defaultRole : defaultRoles) {
            getDefaultRole().removeCompositeRole(getRole(defaultRole));
        }
    }

    @Override
    public Stream<GroupModel> getDefaultGroupsStream() {
        return realm.getDefaultGroupIds().stream().map(this::getGroupById);
    }

    @Override
    public void addDefaultGroup(GroupModel group) {
        Collection<String> groupsIds = realm.getDefaultGroupIds();
        if (groupsIds.contains(group.getId())) return;

        groupsIds.add(group.getId());
        em.flush();
    }

    @Override
    public void removeDefaultGroup(GroupModel group) {
        Collection<String> groupIds = realm.getDefaultGroupIds();

        if (groupIds.remove(group.getId())) {
            em.flush();
        }
    }

    @Override
    public Stream<ClientModel> getClientsStream() {
        return session.clients().getClientsStream(this);
    }

    @Override
    public Stream<ClientModel> getClientsStream(String orderBy) {
        return session.clients().getClientsStream(this, orderBy);
    }

    @Override
    public Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults) {
        return session.clients().getClientsStream(this, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream() {
        return session.clients().getAlwaysDisplayInConsoleClientsStream(this);
    }

    @Override
    public ClientModel addClient() {
        return session.clients().addClient(this);
    }

    @Override
    public ClientModel addClientByClientId(String clientId) {
        return session.clients().addClient(this, clientId);
    }

    @Override
    public ClientModel addClient(String name) {
        return session.clients().addClient(this, name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        return session.clients().addClient(this, id, clientId);
    }

    @Override
    public boolean removeClient(String id) {
        if (id == null) return false;
        ClientModel client = getClientById(id);
        if (client == null) return false;
        return session.clients().removeClient(this, id);
    }

    @Override
    public ClientModel getClientById(String id) {
        return session.clients().getClientById(this, id);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        return session.clients().getClientByClientId(this, clientId);
    }

    @Override
    public ClientModel getClientBySandboxClientId(String sandboxClientId) {
        return session.clients().getClientBySandboxClientId(this, sandboxClientId);
    }

    @Override
    public Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults) {
        return session.clients().searchClientsByClientIdStream(this, clientId, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientByClientNameStream(String clientName, Integer firstResult, Integer maxResults) {
        return session.clients().searchClientsByClientNameStream(this, clientName, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientByAttributes(Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return session.clients().searchClientsByAttributes(this, attributes, firstResult, maxResults);
    }

    private static final String BROWSER_HEADER_PREFIX = "_browser_header.";

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        Map<String, String> attributes = getAttributes();
        if (attributes.isEmpty()) return Collections.EMPTY_MAP;
        Map<String, String> headers = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().startsWith(BROWSER_HEADER_PREFIX)) {
                headers.put(entry.getKey().substring(BROWSER_HEADER_PREFIX.length()), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            setAttribute(BROWSER_HEADER_PREFIX + entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        Map<String, String> config = new HashMap<String, String>();
        config.putAll(realm.getSmtpConfig());
        return Collections.unmodifiableMap(config);
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        realm.setSmtpConfig(smtpConfig);
        em.flush();
    }

    @Override
    public void setSmsConfig(Map<String, String> smsConfig) {
        realm.setSmsConfig(smsConfig);
        em.flush();
    }

    @Override
    public Map<String, String> getSmsConfig() {
        Map<String, String> config = new HashMap<String, String>();
        config.putAll(realm.getSmsConfig());
        return Collections.unmodifiableMap(config);
    }

    @Override
    public Map<String, String> getSmsRealmConfig() {
        return getSmsConfig();
    }

    @Override
    public Map<String, String> getSmsExternalConfig() {
        Map<String, String> smsConfig = getSmsConfig();
        if (!smsConfig.get("provider").equals("Config"))
            return null;

        String attributes = smsConfig.get("attributes");
        if (attributes == null) return null;

        try {
            return JsonSerialization.readValue(attributes, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RoleModel getRole(String name) {
        return session.roles().getRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String name) {
        return session.roles().addRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return session.roles().addRealmRole(this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return session.roles().removeRole(role);
    }

    @Override
    public Stream<RoleModel> getRolesStream() {
        return session.roles().getRealmRolesStream(this);
    }

    @Override
    public Stream<RoleModel> getRolesStream(Integer first, Integer max) {
        return session.roles().getRealmRolesStream(this, first, max);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return session.roles().searchForRolesStream(this, search, first, max);
    }

    @Override
    public RoleModel getRoleById(String id) {
        return session.roles().getRoleById(this, id);
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (passwordPolicy == null) {
            passwordPolicy = PasswordPolicy.parse(session, realm.getPasswordPolicy());
        }
        return passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        this.passwordPolicy = policy;
        realm.setPasswordPolicy(policy.toString());
        em.flush();
    }

    @Override
    public OTPPolicy getOTPPolicy() {
        if (otpPolicy == null) {
            otpPolicy = new OTPPolicy();
            otpPolicy.setDigits(realm.getOtpPolicyDigits());
            otpPolicy.setAlgorithm(realm.getOtpPolicyAlgorithm());
            otpPolicy.setInitialCounter(realm.getOtpPolicyInitialCounter());
            otpPolicy.setLookAheadWindow(realm.getOtpPolicyLookAheadWindow());
            otpPolicy.setType(realm.getOtpPolicyType());
            otpPolicy.setPeriod(realm.getOtpPolicyPeriod());
        }
        return otpPolicy;
    }

    @Override
    public void setOTPPolicy(OTPPolicy policy) {
        realm.setOtpPolicyAlgorithm(policy.getAlgorithm());
        realm.setOtpPolicyDigits(policy.getDigits());
        realm.setOtpPolicyInitialCounter(policy.getInitialCounter());
        realm.setOtpPolicyLookAheadWindow(policy.getLookAheadWindow());
        realm.setOtpPolicyType(policy.getType());
        realm.setOtpPolicyPeriod(policy.getPeriod());
        em.flush();
    }


    // WebAuthn

    @Override
    public WebAuthnPolicy getWebAuthnPolicy() {
        return getWebAuthnPolicy("");
    }

    @Override
    public void setWebAuthnPolicy(WebAuthnPolicy policy) {
        setWebAuthnPolicy(policy, "");
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicyPasswordless() {
        // We will use some prefix for attributes related to passwordless WebAuthn policy
        return getWebAuthnPolicy(Constants.WEBAUTHN_PASSWORDLESS_PREFIX);
    }

    @Override
    public void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy) {
        // We will use some prefix for attributes related to passwordless WebAuthn policy
        setWebAuthnPolicy(policy, Constants.WEBAUTHN_PASSWORDLESS_PREFIX);
    }


    private WebAuthnPolicy getWebAuthnPolicy(String attributePrefix) {
        WebAuthnPolicy policy = new WebAuthnPolicy();

        // mandatory parameters
        String rpEntityName = getAttribute(RealmAttributes.WEBAUTHN_POLICY_RP_ENTITY_NAME + attributePrefix);
        if (rpEntityName == null || rpEntityName.isEmpty())
            rpEntityName = Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME;
        policy.setRpEntityName(rpEntityName);

        String signatureAlgorithmsString = getAttribute(RealmAttributes.WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS + attributePrefix);
        if (signatureAlgorithmsString == null || signatureAlgorithmsString.isEmpty())
            signatureAlgorithmsString = Constants.DEFAULT_WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS;
        List<String> signatureAlgorithms = Arrays.asList(signatureAlgorithmsString.split(","));
        policy.setSignatureAlgorithm(signatureAlgorithms);

        // optional parameters
        String rpId = getAttribute(RealmAttributes.WEBAUTHN_POLICY_RP_ID + attributePrefix);
        if (rpId == null || rpId.isEmpty()) rpId = "";
        policy.setRpId(rpId);

        String attestationConveyancePreference = getAttribute(RealmAttributes.WEBAUTHN_POLICY_ATTESTATION_CONVEYANCE_PREFERENCE + attributePrefix);
        if (attestationConveyancePreference == null || attestationConveyancePreference.isEmpty())
            attestationConveyancePreference = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        policy.setAttestationConveyancePreference(attestationConveyancePreference);

        String authenticatorAttachment = getAttribute(RealmAttributes.WEBAUTHN_POLICY_AUTHENTICATOR_ATTACHMENT + attributePrefix);
        if (authenticatorAttachment == null || authenticatorAttachment.isEmpty())
            authenticatorAttachment = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        policy.setAuthenticatorAttachment(authenticatorAttachment);

        String requireResidentKey = getAttribute(RealmAttributes.WEBAUTHN_POLICY_REQUIRE_RESIDENT_KEY + attributePrefix);
        if (requireResidentKey == null || requireResidentKey.isEmpty())
            requireResidentKey = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        policy.setRequireResidentKey(requireResidentKey);

        String userVerificationRequirement = getAttribute(RealmAttributes.WEBAUTHN_POLICY_USER_VERIFICATION_REQUIREMENT + attributePrefix);
        if (userVerificationRequirement == null || userVerificationRequirement.isEmpty())
            userVerificationRequirement = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        policy.setUserVerificationRequirement(userVerificationRequirement);

        String createTime = getAttribute(RealmAttributes.WEBAUTHN_POLICY_CREATE_TIMEOUT + attributePrefix);
        if (createTime != null) policy.setCreateTimeout(Integer.parseInt(createTime));
        else policy.setCreateTimeout(0);

        String avoidSameAuthenticatorRegister = getAttribute(RealmAttributes.WEBAUTHN_POLICY_AVOID_SAME_AUTHENTICATOR_REGISTER + attributePrefix);
        if (avoidSameAuthenticatorRegister != null)
            policy.setAvoidSameAuthenticatorRegister(Boolean.parseBoolean(avoidSameAuthenticatorRegister));

        String acceptableAaguidsString = getAttribute(RealmAttributes.WEBAUTHN_POLICY_ACCEPTABLE_AAGUIDS + attributePrefix);
        List<String> acceptableAaguids = new ArrayList<>();
        if (acceptableAaguidsString != null && !acceptableAaguidsString.isEmpty())
            acceptableAaguids = Arrays.asList(acceptableAaguidsString.split(","));
        policy.setAcceptableAaguids(acceptableAaguids);

        return policy;
    }


    private void setWebAuthnPolicy(WebAuthnPolicy policy, String attributePrefix) {
        // mandatory parameters
        String rpEntityName = policy.getRpEntityName();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_RP_ENTITY_NAME + attributePrefix, rpEntityName);

        List<String> signatureAlgorithms = policy.getSignatureAlgorithm();
        String signatureAlgorithmsString = String.join(",", signatureAlgorithms);
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS + attributePrefix, signatureAlgorithmsString);

        // optional parameters
        String rpId = policy.getRpId();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_RP_ID + attributePrefix, rpId);

        String attestationConveyancePreference = policy.getAttestationConveyancePreference();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_ATTESTATION_CONVEYANCE_PREFERENCE + attributePrefix, attestationConveyancePreference);

        String authenticatorAttachment = policy.getAuthenticatorAttachment();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_AUTHENTICATOR_ATTACHMENT + attributePrefix, authenticatorAttachment);

        String requireResidentKey = policy.getRequireResidentKey();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_REQUIRE_RESIDENT_KEY + attributePrefix, requireResidentKey);

        String userVerificationRequirement = policy.getUserVerificationRequirement();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_USER_VERIFICATION_REQUIREMENT + attributePrefix, userVerificationRequirement);

        int createTime = policy.getCreateTimeout();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_CREATE_TIMEOUT + attributePrefix, Integer.toString(createTime));

        boolean avoidSameAuthenticatorRegister = policy.isAvoidSameAuthenticatorRegister();
        setAttribute(RealmAttributes.WEBAUTHN_POLICY_AVOID_SAME_AUTHENTICATOR_REGISTER + attributePrefix, Boolean.toString(avoidSameAuthenticatorRegister));

        List<String> acceptableAaguids = policy.getAcceptableAaguids();
        if (acceptableAaguids != null && !acceptableAaguids.isEmpty()) {
            String acceptableAaguidsString = String.join(",", acceptableAaguids);
            setAttribute(RealmAttributes.WEBAUTHN_POLICY_ACCEPTABLE_AAGUIDS + attributePrefix, acceptableAaguidsString);
        } else {
            removeAttribute(RealmAttributes.WEBAUTHN_POLICY_ACCEPTABLE_AAGUIDS + attributePrefix);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RealmModel)) return false;

        RealmModel that = (RealmModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String getLoginTheme() {
        return realm.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        realm.setLoginTheme(name);
        em.flush();
    }

    @Override
    public String getAccountTheme() {
        return realm.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        realm.setAccountTheme(name);
        em.flush();
    }

    @Override
    public String getAdminTheme() {
        return realm.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        realm.setAdminTheme(name);
        em.flush();
    }

    @Override
    public String getEmailTheme() {
        return realm.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        realm.setEmailTheme(name);
        em.flush();
    }

    @Override
    public String getSSOTheme() {
        return realm.getSsoTheme();
    }

    @Override
    public void setSSOTheme(String name) {
        realm.setSsoTheme(name);
        em.flush();
    }

    @Override
    public String getMode() {
        return realm.getMode();
    }

    @Override
    public void setMode(String mode) {
        realm.setMode(mode);
        em.flush();
    }

    @Override
    public boolean isEventsEnabled() {
        return realm.isEventsEnabled();
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        realm.setEventsEnabled(enabled);
        em.flush();
    }

    @Override
    public long getEventsExpiration() {
        return realm.getEventsExpiration();
    }

    @Override
    public void setEventsExpiration(long expiration) {
        realm.setEventsExpiration(expiration);
        em.flush();
    }

    @Override
    public Stream<String> getEventsListenersStream() {
        return realm.getEventsListeners().stream();
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        realm.setEventsListeners(listeners);
        em.flush();
    }

    @Override
    public Stream<String> getEmailSenderStream() {
        return realm.getEmailSenders().stream();
    }

    @Override
    public void setEmailSenders(Set<String> listeners) {
        realm.setEmailSenders(listeners);
        em.flush();
    }

    @Override
    public Stream<String> getSmsSenderStream() {
        return realm.getSmsSenders().stream();
    }

    @Override
    public void setSmsSenders(Set<String> listeners) {
        realm.setSmsSenders(listeners);
        em.flush();
    }

    @Override
    public Stream<String> getMFAProvidersStream() {
        return realm.getMfaProviders().stream();
    }

    @Override
    public void setMFAProviders(Set<String> listeners) {
        realm.setMfaProviders(listeners);
        em.flush();
    }

    @Override
    public Stream<String> getEnabledEventTypesStream() {
        return realm.getEnabledEventTypes().stream();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        realm.setEnabledEventTypes(enabledEventTypes);
        em.flush();
    }

    @Override
    public boolean isAdminEventsEnabled() {
        return realm.isAdminEventsEnabled();
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        realm.setAdminEventsEnabled(enabled);
        em.flush();
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        return realm.isAdminEventsDetailsEnabled();
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        realm.setAdminEventsDetailsEnabled(enabled);
        em.flush();
    }

    @Override
    public ClientModel getMasterAdminClient() {
        String masterAdminClientId = realm.getMasterAdminClient();
        if (masterAdminClientId == null) {
            return null;
        }
        RealmModel masterRealm = getName().equals(Config.getAdminRealm())
                ? this
                : session.realms().getRealmByName(Config.getAdminRealm());
        return session.clients().getClientById(masterRealm, masterAdminClientId);
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        String appEntityId = client != null ? em.getReference(ClientEntity.class, client.getId()).getId() : null;
        realm.setMasterAdminClient(appEntityId);
        em.flush();
    }

    @Override
    public void setDefaultRole(RoleModel role) {
        realm.setDefaultRoleId(role.getId());
    }

    @Override
    public RoleModel getDefaultRole() {
        if (realm.getDefaultRoleId() == null) {
            return null;
        }
        return session.roles().getRoleById(this, realm.getDefaultRoleId());
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProvidersStream() {
        List<IdentityProviderEntity> list = realm.getIdentityProviders();
        return list.stream().map(this::entityToModel);
    }

    private IdentityProviderModel entityToModel(IdentityProviderEntity entity) {
        IdentityProviderModel identityProviderModel = new IdentityProviderModel();
        identityProviderModel.setProviderId(entity.getProviderId());
        identityProviderModel.setAlias(entity.getAlias());
        identityProviderModel.setDisplayName(entity.getDisplayName());

        identityProviderModel.setInternalId(entity.getInternalId());
        Map<String, String> config = entity.getConfig();
        Map<String, String> copy = new HashMap<>();
        copy.putAll(config);
        identityProviderModel.setConfig(copy);
        identityProviderModel.setEnabled(entity.isEnabled());
        identityProviderModel.setLinkOnly(entity.isLinkOnly());
        identityProviderModel.setTrustEmail(entity.isTrustEmail());
        identityProviderModel.setAuthenticateByDefault(entity.isAuthenticateByDefault());
        identityProviderModel.setFirstBrokerLoginFlowId(entity.getFirstBrokerLoginFlowId());
        identityProviderModel.setPostBrokerLoginFlowId(entity.getPostBrokerLoginFlowId());
        identityProviderModel.setEmailSenderId(entity.getEmailSenderId());
        identityProviderModel.setInBuilt(entity.isInBuilt());
        identityProviderModel.setStoreToken(entity.isStoreToken());
        identityProviderModel.setAddReadTokenRoleOnCreate(entity.isAddReadTokenRoleOnCreate());
        return identityProviderModel;
    }

    private ActionModel entityToModel(ActionEntity entity) {
        ActionModel actionModel = new ActionModel();
        actionModel.setId(entity.getId());
        actionModel.setAlias(entity.getAlias());
        actionModel.setRealmId(entity.getRealm());
        actionModel.setModifiedTimestamp(entity.getModifiedTimestamp());
        actionModel.setInBuilt(entity.isInBuilt());
        actionModel.setActionType(entity.getActionType());
        actionModel.setActionVariables(entity.getActionVariables());
        actionModel.setIsSubWorkflow(entity.isSubWorkflow());
        actionModel.setMarketPlace(entity.isMarketplace());
        return actionModel;
    }

    private EztovrfyMarketplaceTemplateModel entityToModel(EztovrfyMarketplaceTemplateEntity entity) {
        EztovrfyMarketplaceTemplateModel eztovrfyMarketplaceTemplateModel = new EztovrfyMarketplaceTemplateModel();
        eztovrfyMarketplaceTemplateModel.setId(entity.getId());
        eztovrfyMarketplaceTemplateModel.setRealmId(entity.getRealmId());
        eztovrfyMarketplaceTemplateModel.setWorkflowName(entity.getWorkflowName());
        eztovrfyMarketplaceTemplateModel.setWorkflowDescription(entity.getWorkflowDescription());
        eztovrfyMarketplaceTemplateModel.setImageURL(entity.getImageURL());
        eztovrfyMarketplaceTemplateModel.setWorkflowCategory(entity.getWorkflowCategory());
        try {
            eztovrfyMarketplaceTemplateModel.setTemplateTag(JsonSerialization.readValue(entity.getTemplateTag(), List.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        eztovrfyMarketplaceTemplateModel.setCountry(entity.getCountry());
        eztovrfyMarketplaceTemplateModel.setModifiedTimestamp(entity.getModifiedTimestamp());
        eztovrfyMarketplaceTemplateModel.setPublished(entity.isPublished());
        eztovrfyMarketplaceTemplateModel.setWorkflowId(entity.getWorkflowId());
        eztovrfyMarketplaceTemplateModel.setWorkflowJson(entity.getWorkflowJson());
        return eztovrfyMarketplaceTemplateModel;
    }

    private EztovrfyMarketplaceCategoryModel entityToModel(EztovrfyMarketplaceCategoryEntity entity) {
        EztovrfyMarketplaceCategoryModel model = new EztovrfyMarketplaceCategoryModel();
        model.setId(entity.getId());
        model.setRealmId(entity.getRealmId());
        model.setCategory(entity.getCategory());
        return model;
    }

    private MarketplaceConfigModel entityToModel(MarketplaceConfigEntity entity) {
        MarketplaceConfigModel actionModel = new MarketplaceConfigModel();
        actionModel.setId(entity.getId());
        actionModel.setProviderType(entity.getProviderType());
        actionModel.setProviderId(entity.getProviderId());
        actionModel.setRealmId(entity.getRealm().getId());
        return actionModel;
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        return getIdentityProvidersStream()
                .filter(model -> Objects.equals(model.getAlias(), alias))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel identityProvider) {
        IdentityProviderEntity entity = new IdentityProviderEntity();

        if (identityProvider.getInternalId() == null) {
            entity.setInternalId(KeycloakModelUtils.generateId());
        } else {
            entity.setInternalId(identityProvider.getInternalId());
        }
        entity.setAlias(identityProvider.getAlias());
        entity.setDisplayName(identityProvider.getDisplayName());
        entity.setProviderId(identityProvider.getProviderId());
        entity.setEnabled(identityProvider.isEnabled());
        entity.setStoreToken(identityProvider.isStoreToken());
        entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
        entity.setTrustEmail(identityProvider.isTrustEmail());
        entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
        entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
        entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
        entity.setEmailSenderId(identityProvider.getEmailSenderId());
        entity.setInBuilt(identityProvider.isInBuilt());
        entity.setConfig(identityProvider.getConfig());
        entity.setLinkOnly(identityProvider.isLinkOnly());

        realm.addIdentityProvider(entity);

        identityProvider.setInternalId(entity.getInternalId());

        em.persist(entity);
        em.flush();
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        for (IdentityProviderEntity entity : realm.getIdentityProviders()) {
            if (entity.getAlias().equals(alias)) {

                IdentityProviderModel model = entityToModel(entity);
                em.remove(entity);
                em.flush();

                session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderRemovedEvent() {

                    @Override
                    public RealmModel getRealm() {
                        return RealmAdapter.this;
                    }

                    @Override
                    public IdentityProviderModel getRemovedIdentityProvider() {
                        return model;
                    }

                    @Override
                    public KeycloakSession getKeycloakSession() {
                        return session;
                    }
                });

            }
        }
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        for (IdentityProviderEntity entity : this.realm.getIdentityProviders()) {
            if (entity.getInternalId().equals(identityProvider.getInternalId())) {
                entity.setAlias(identityProvider.getAlias());
                entity.setDisplayName(identityProvider.getDisplayName());
                entity.setEnabled(identityProvider.isEnabled());
                entity.setTrustEmail(identityProvider.isTrustEmail());
                entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
                entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
                entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
                entity.setEmailSenderId(identityProvider.getEmailSenderId());
                entity.setInBuilt(identityProvider.isInBuilt());
                entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
                entity.setStoreToken(identityProvider.isStoreToken());
                entity.setConfig(identityProvider.getConfig());
                entity.setLinkOnly(identityProvider.isLinkOnly());
            }
        }

        em.flush();

        session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderUpdatedEvent() {

            @Override
            public RealmModel getRealm() {
                return RealmAdapter.this;
            }

            @Override
            public IdentityProviderModel getUpdatedIdentityProvider() {
                return identityProvider;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        return !this.realm.getIdentityProviders().isEmpty();
    }

    @Override
    public boolean isInternationalizationEnabled() {
        return realm.isInternationalizationEnabled();
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        realm.setInternationalizationEnabled(enabled);
        em.flush();
    }

    @Override
    public Stream<String> getSupportedLocalesStream() {
        return realm.getSupportedLocales().stream();
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        realm.setSupportedLocales(locales);
        em.flush();
    }

    @Override
    public String getDefaultLocale() {
        return realm.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        realm.setDefaultLocale(locale);
        em.flush();
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream() {
        return realm.getIdentityProviderMappers().stream().map(this::entityToModel);
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias) {
        return realm.getIdentityProviderMappers().stream()
                .filter(e -> Objects.equals(e.getIdentityProviderAlias(), brokerAlias))
                .map(this::entityToModel);
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        if (getIdentityProviderMapperByName(model.getIdentityProviderAlias(), model.getName()) != null) {
            throw new RuntimeException("identity provider mapper name must be unique per identity provider");
        }
        String id = KeycloakModelUtils.generateId();
        IdentityProviderMapperEntity entity = new IdentityProviderMapperEntity();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setIdentityProviderAlias(model.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(model.getIdentityProviderMapper());
        entity.setRealm(this.realm);
        entity.setConfig(model.getConfig());

        em.persist(entity);
        this.realm.getIdentityProviderMappers().add(entity);
        return entityToModel(entity);
    }

    protected IdentityProviderMapperEntity getIdentityProviderMapperEntity(String id) {
        for (IdentityProviderMapperEntity entity : this.realm.getIdentityProviderMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }

    protected IdentityProviderMapperEntity getIdentityProviderMapperEntityByName(String alias, String name) {
        for (IdentityProviderMapperEntity entity : this.realm.getIdentityProviderMappers()) {
            if (entity.getIdentityProviderAlias().equals(alias) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }

    @Override
    public void removeIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        IdentityProviderMapperEntity toDelete = getIdentityProviderMapperEntity(mapping.getId());
        if (toDelete != null) {
            this.realm.getIdentityProviderMappers().remove(toDelete);
            em.remove(toDelete);
        }

    }

    @Override
    public void updateIdentityProviderMapper(IdentityProviderMapperModel mapping) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntity(mapping.getId());
        entity.setIdentityProviderAlias(mapping.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(mapping.getIdentityProviderMapper());
        if (entity.getConfig() == null) {
            entity.setConfig(mapping.getConfig());
        } else {
            entity.getConfig().clear();
            if (mapping.getConfig() != null) {
                entity.getConfig().putAll(mapping.getConfig());
            }
        }
        em.flush();

    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntity(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String alias, String name) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntityByName(alias, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected IdentityProviderMapperModel entityToModel(IdentityProviderMapperEntity entity) {
        IdentityProviderMapperModel mapping = new IdentityProviderMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setIdentityProviderAlias(entity.getIdentityProviderAlias());
        mapping.setIdentityProviderMapper(entity.getIdentityProviderMapper());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }

    @Override
    public AuthenticationFlowModel getBrowserFlow() {
        String flowId = realm.getBrowserFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setBrowserFlow(AuthenticationFlowModel flow) {
        realm.setBrowserFlow(flow.getId());

    }

    @Override
    public AuthenticationFlowModel getRegistrationFlow() {
        String flowId = realm.getRegistrationFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        realm.setRegistrationFlow(flow.getId());

    }

    @Override
    public AuthenticationFlowModel getDirectGrantFlow() {
        String flowId = realm.getDirectGrantFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        realm.setDirectGrantFlow(flow.getId());

    }

    @Override
    public AuthenticationFlowModel getResetCredentialsFlow() {
        String flowId = realm.getResetCredentialsFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        realm.setResetCredentialsFlow(flow.getId());
    }

    public AuthenticationFlowModel getClientAuthenticationFlow() {
        String flowId = realm.getClientAuthenticationFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        realm.setClientAuthenticationFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        String flowId = realm.getDockerAuthenticationFlow();
        if (flowId == null) return null;
        return getAuthenticationFlowById(flowId);
    }

    @Override
    public void setDockerAuthenticationFlow(AuthenticationFlowModel flow) {
        realm.setDockerAuthenticationFlow(flow.getId());
    }

    @Override
    public Stream<AuthenticationFlowModel> getAuthenticationFlowsStream() {
        return realm.getAuthenticationFlows().stream().map(this::entityToModel);
    }

    @Override
    public AuthenticationFlowModel getFlowByAlias(String alias) {
        return realm.getAuthenticationFlows().stream()
                .filter(flow -> Objects.equals(flow.getAlias(), alias))
                .findFirst()
                .map(this::entityToModel)
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
        return realm.getEventConfigs().stream().map(this::entityToModel);
    }

    @Override
    public EventConfigModel addEventConfig(EventConfigModel model) {
        EventConfigEntity auth = new EventConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setProviderId(model.getEventProviderId());
        auth.setRealm(realm);
        auth.setConfig(model.getConfig());
        realm.getEventConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void updateEventConfig(EventConfigModel model) {
        EventConfigEntity entity = getEventConfigEntity(model.getEventProviderId(), false);
        if (entity == null) return;
        entity.setProviderId(model.getEventProviderId());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();
    }

    @Override
    public void removeEventConfig(EventConfigModel model) {
        EventConfigEntity entity = getEventConfigEntity(model.getEventProviderId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public EventConfigModel getEventConfigByProviderId(String id) {
        EventConfigEntity entity = getEventConfigEntity(id, true);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public Stream<MarketplaceConfigModel> getMarketplaceConfigsStream() {
        return realm.getMarketplaceConfigs().stream().map(this::entityToModel);
    }

    @Override
    public MarketplaceConfigModel addMarketplaceConfig(MarketplaceConfigModel model) {
        MarketplaceConfigEntity auth = new MarketplaceConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setProviderId(model.getProviderId());
        auth.setRealm(realm);
        auth.setProviderType(model.getProviderType());
        realm.getMarketplaceConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void updateMarketplaceConfig(MarketplaceConfigModel model) {
        MarketplaceConfigEntity entity = getMarketplaceConfigEntity(model.getProviderId(), false);
        if (entity == null) return;
        entity.setProviderId(model.getProviderId());
        entity.setProviderType(model.getProviderType());
        em.flush();
    }

    @Override
    public void removeMarketplaceConfig(MarketplaceConfigModel model) {
        MarketplaceConfigEntity entity = getMarketplaceConfigEntity(model.getProviderId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public Stream<MarketplaceConfigModel> getMarketplaceConfigByProviderType(String type) {
        return getMarketplaceConfigEntityByType(type).map(this::entityToModel);
    }


    @Override
    public Stream<SmsConfigModel> getSmsConfigsStream() {
        return realm.getSmsConfigs().stream().map(this::entityToModel);
    }

    @Override
    public SmsConfigModel addSmsConfig(SmsConfigModel model) {
        SmsConfigEntity auth = new SmsConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setProviderId(model.getSmsProviderId());
        auth.setRealm(realm);
        auth.setConfig(model.getConfig());
        realm.getSmsConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void updateSmsConfig(SmsConfigModel model) {
        SmsConfigEntity entity = getSmsConfigEntity(model.getSmsProviderId(), false);
        if (entity == null) return;
        entity.setProviderId(model.getSmsProviderId());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();
    }

    @Override
    public void removeSmsConfig(SmsConfigModel model) {
        SmsConfigEntity entity = getSmsConfigEntity(model.getSmsProviderId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public SmsConfigModel getSmsConfigByProviderId(String id) {
        SmsConfigEntity entity = getSmsConfigEntity(id, true);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public String getSubscription() {
        return realm.getSubscription();
    }

    @Override
    public void addSubscription(String model) {
        realm.setSubscription(model);
        em.flush();
    }


    @Override
    public Map<String, String> getSubscriptionUsage() {
        Map<String, String> config = new HashMap<String, String>();
        config.putAll(realm.getSubscriptionUsage());
        return Collections.unmodifiableMap(config);
    }

    @Override
    public void updateSubscription(Map<String, String> model) {
        realm.setSubscriptionUsage(model);
        em.flush();
    }

    @Override
    public Stream<ReportModel> getReports() {
        return realm.getReports().stream().map(this::entityToModel);
    }

    @Override
    public ReportModel addReport(ReportModel model) {
        ReportEntity reportEntity = new ReportEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        reportEntity.setId(id);
        reportEntity.setRealmId(realm.getId());
        reportEntity.setReportType(model.getReportType());
        reportEntity.setFileName(model.getFileName());
        reportEntity.setRequestedBy(model.getRequestedBy());
        reportEntity.setCreatedDate(Time.currentTimeMillis());
        reportEntity.setStatus(model.getStatus());
        realm.getReports().add(reportEntity);
        em.persist(reportEntity);
        model.setId(reportEntity.getId());
        return model;
    }

    @Override
    public ReportModel getReportById(String id) {
        ReportEntity entity = getReportEntity(id, true);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public void updateReport(ReportModel model) {
        ReportEntity entity = getReportEntity(model.getId(), false);
        if (entity == null) return;
        entity.setStatus(model.getStatus());
        entity.setFileName(model.getFileName());
        entity.setCreatedDate(model.getCreatedDate());
        em.flush();
    }

    @Override
    public Stream<MgmtAccessModel> getMgmtAccessKeys() {
        return realm.getMgmtAccessEntities().stream().map(this::entityToModel);
    }

    @Override
    public MgmtAccessModel addMgmtAccess(MgmtAccessModel model) {
        MgmtAccessEntity entity = new MgmtAccessEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setRealmId(model.getRealmId());
        entity.setName(model.getName());
        long expiryDate = model.getExpiryDate();
        if(expiryDate == 0) {
            expiryDate = Time.addDays(System.currentTimeMillis(),365); // default here added 365 days
        }
        entity.setExpiryDate(expiryDate);
        entity.setRequestedBy(model.getRequestedBy());
        long createdDate = Time.currentTimeMillis();
        entity.setCreatedDate(createdDate);
        entity.setLastUpdatedDate(createdDate);
        entity.setLastUsedDate(model.getLastUsedDate());
        String secret = model.getSecret();
        if(secret == null)
        {
            String clientSecret =  generateRandomId(64);
            UserCredentialModel userSecret = UserCredentialModel.generateSecret(clientSecret);
            secret = userSecret.getChallengeResponse();
        }
        entity.setSecret(secret);
        String sandboxSecret = model.getSandboxSecret();
        if(sandboxSecret == null)
        {
            String clientSecret =  generateRandomId(64);
            UserCredentialModel userSecret = UserCredentialModel.generateSecret(clientSecret);
            sandboxSecret = userSecret.getChallengeResponse();
        }
        entity.setSandboxSecret(sandboxSecret);
        entity.setEnabled(model.isEnabled());
        entity.setSandboxMode(model.isSandboxMode());
        entity.setPermissions(model.getPermissions());
        realm.getMgmtAccessEntities().add(entity);
        em.persist(entity);
        return entityToModel(entity);
    }

    @Override
    public MgmtAccessModel getMgmtAccessById(String id) {
        MgmtAccessEntity entity = getMgmtAccessEntity(id, true);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public void removeMgmtAccess(String id) {
        MgmtAccessEntity entity  = getMgmtAccessEntity(id, true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public MgmtAccessModel updateMgmtAccess(MgmtAccessModel model) {
        MgmtAccessEntity entity = getMgmtAccessEntity(model.getId(), false);
        if (entity == null) return null;
        if(model.getLastUsedDate() > 0)
            entity.setLastUsedDate(model.getLastUsedDate());
        entity.setName(model.getName());
        entity.setLastUpdatedDate(Time.currentTimeMillis());
        entity.setEnabled(model.isEnabled());
        entity.setExpiryDate(model.getExpiryDate());
        entity.setPermissions(model.getPermissions());
        entity.setSandboxMode(model.isSandboxMode());
        entity.setSandboxSecret(model.getSandboxSecret());
        em.flush();
        return entityToModel(entity);
    }

    @Override
    public MgmtAccessModel rotateMgmtAccess(MgmtAccessModel model) {
        MgmtAccessEntity entity = getMgmtAccessEntity(model.getId(), false);
        if (entity == null) return null;
        String clientSecret =  generateRandomId(64);
        UserCredentialModel userSecret = UserCredentialModel.generateSecret(clientSecret);
        String secret = userSecret.getChallengeResponse();
        if(entity.isSandboxMode()){
            entity.setSandboxSecret(secret);
        } else{
            entity.setSecret(secret);
        }
        entity.setLastUpdatedDate(Time.currentTimeMillis());
        em.flush();
        return entityToModel(entity);
    }




    @Override
    public Stream<EmailConfigModel> getEmailConfigsStream() {
        return realm.getEmailConfigs().stream().map(this::entityToModel);
    }

    @Override
    public EmailConfigModel addEmailConfig(EmailConfigModel model) {
        EmailConfigEntity auth = new EmailConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setProviderId(model.getEmailProviderId());
        auth.setRealm(realm);
        auth.setConfig(model.getConfig());
        realm.getEmailConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void updateEmailConfig(EmailConfigModel model) {
        EmailConfigEntity entity = getEmailConfigEntity(model.getEmailProviderId(), false);
        if (entity == null) return;
        entity.setProviderId(model.getEmailProviderId());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();
    }

    @Override
    public void removeEmailConfig(EmailConfigModel model) {
        EmailConfigEntity entity = getEmailConfigEntity(model.getEmailProviderId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public EmailConfigModel getEmailConfigByProviderId(String id) {
        EmailConfigEntity entity = getEmailConfigEntity(id, true);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public Stream<MFAConfigModel> getMFAConfigsStream() {
        return realm.getMfaConfigs().stream().map(this::entityToModel);
    }

    @Override
    public MFAConfigModel addMFAConfig(MFAConfigModel model) {
        MFAConfigEntity auth = new MFAConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setProviderId(model.getMfaProviderId());
        auth.setRealm(realm);
        auth.setConfig(model.getConfig());
        realm.getMfaConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void updateMFAConfig(MFAConfigModel model) {
        MFAConfigEntity entity = getMFAConfigEntity(model.getMfaProviderId(), false);
        if (entity == null) return;
        entity.setProviderId(model.getMfaProviderId());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();
    }

    @Override
    public void removeMFAConfig(MFAConfigModel model) {
        MFAConfigEntity entity = getMFAConfigEntity(model.getMfaProviderId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public MFAConfigModel getMFAConfigByProviderId(String id) {
        MFAConfigEntity entity = getMFAConfigEntity(id, true);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public Stream<CibaAuthConfigModel> getCibaConfigsStream() {
        return realm.getCibaAuthConfigs().stream().map(this::entityToModel);
    }

    @Override
    public CibaAuthConfigModel addCibaAuthConfig(CibaAuthConfigModel model) {
        CibaAuthConfigEntity auth = new CibaAuthConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setProviderId(model.getCibaAuthProviderId());
        auth.setRealm(realm.getId());
        auth.setClientId(model.getClientId());
        auth.setConfig(model.getConfig());
        realm.getCibaAuthConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void updateCibaAuthConfig(CibaAuthConfigModel model) {
        CibaAuthConfigEntity entity = getCibaAuthConfigEntity(model.getCibaAuthProviderId(), model.getClientId(), model.getRealmId());
        if (entity == null) return;
        entity.setProviderId(model.getCibaAuthProviderId());
        entity.setClientId(model.getClientId());
        entity.setRealm(model.getRealmId());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();
    }

    @Override
    public void removeCibaAuthConfig(CibaAuthConfigModel model) {
        CibaAuthConfigEntity entity = getCibaAuthConfigEntity(model.getCibaAuthProviderId(), model.getClientId(), model.getRealmId());
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public CibaAuthConfigModel getCibaAuthConfigByProviderId(String id, String clientId, String realmId) {
        CibaAuthConfigEntity entity = getCibaAuthConfigEntity(id, clientId, realmId);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public CibaAuthConfigModel getCibaAuthConfigByClientId(String clientId, String realmId) {
        CibaAuthConfigEntity entity = getCibaAuthConfigEntity(clientId, realmId);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public Stream<CibaNotificationConfigModel> getCibaNotificationConfigsStream() {
        return realm.getCibaNotificationConfigs().stream().map(this::entityToModel);
    }

    @Override
    public CibaNotificationConfigModel addCibaNotificationConfig(CibaNotificationConfigModel model) {
        CibaNotificationConfigEntity auth = new CibaNotificationConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setProviderId(model.getCibaNotificationProviderId());
        auth.setRealm(realm.getId());
        auth.setClientId(model.getClientId());
        auth.setConfig(model.getConfig());
        realm.getCibaNotificationConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void updateCibaNotificationConfig(CibaNotificationConfigModel model) {
        CibaNotificationConfigEntity entity = getCibaNotificationConfigEntity(model.getClientId(), model.getRealmId());
        if (entity == null) return;
        entity.setProviderId(model.getCibaNotificationProviderId());
        entity.setClientId(model.getClientId());
        entity.setRealm(model.getRealmId());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();
    }

    @Override
    public void removeCibaNotificationConfig(CibaNotificationConfigModel model) {
        CibaNotificationConfigEntity entity = getCibaNotificationConfigEntity(model.getCibaNotificationProviderId(), model.getClientId(), model.getRealmId());
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public CibaNotificationConfigModel getCibaNotificationConfigByProviderId(String id, String clientId, String realmId) {
        CibaNotificationConfigEntity entity = getCibaNotificationConfigEntity(id, clientId, realmId);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public CibaNotificationConfigModel getCibaNotificationConfigByClientId(String clientId, String realmId) {
        CibaNotificationConfigEntity entity = getCibaNotificationConfigEntity(clientId, realmId);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public Stream<CibaDeadLetterHookConfigModel> getCibaDeadLetterHookNotificationConfigsStream() {
        return realm.getCibaDeadLetterHookNotificationConfigs().stream().map(this::entityToModel);
    }

    @Override
    public CibaDeadLetterHookConfigModel addCibaResultHookNotificationConfig(CibaDeadLetterHookConfigModel model) {
        CibaDeadLetterHookConfigEntity auth = new CibaDeadLetterHookConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setProviderId(model.getCibaResultHookProviderId());
        auth.setRealm(realm.getId());
        auth.setClientId(model.getClientId());
        auth.setConfig(model.getConfig());
        realm.getCibaDeadLetterHookNotificationConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void updateCibaResultHookNotificationConfig(CibaDeadLetterHookConfigModel model) {
        CibaDeadLetterHookConfigEntity entity = getCibaDeadLetterHookNotificationConfigEntity(model.getClientId(), model.getRealmId());
        if (entity == null) return;
        entity.setProviderId(model.getCibaResultHookProviderId());
        entity.setClientId(model.getClientId());
        entity.setRealm(model.getRealmId());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();
    }

    @Override
    public void removeDeadLetterHookNotificationConfig(CibaDeadLetterHookConfigModel model) {
        CibaDeadLetterHookConfigEntity entity = getCibaDeadLetterHookNotificationConfigEntity(model.getCibaResultHookProviderId(), model.getClientId(), model.getRealmId());
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public CibaDeadLetterHookConfigModel getDeadLetterHookNotificationConfigByProviderId(String id, String clientId, String realmId) {
        CibaDeadLetterHookConfigEntity entity = getCibaDeadLetterHookNotificationConfigEntity(id, clientId, realmId);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public CibaDeadLetterHookConfigModel getDeadLetterHookNotificationConfigByClientId(String clientId, String realmId) {
        CibaDeadLetterHookConfigEntity entity = getCibaDeadLetterHookNotificationConfigEntity(clientId, realmId);
        if (entity == null) return null;
        return entityToModel(entity);
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
        session.getProvider(TransactionAttributeProvider.class).addAttributes(trxId, claims);
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
    public void removeUserTransactionStatus(String userId) {
        session.getProvider(UserTransactionStatusProvider.class)
                .removeUserTransactionStatus(session.getContext().getRealm().getId(), userId);
    }

    @Override
    public void updateTransactionStatus(String trxId, String userId, String status) {
        session.getProvider(UserTransactionStatusProvider.class)
                .updateTransactionStatus(session.getContext().getRealm().getId(), trxId, userId, status);
    }

    @Override
    public UserTransactionModel getUserTransactionDetails(String trxId, String userId) {
        return session.getProvider(UserTransactionStatusProvider.class).getUserTransactionDetails(trxId, userId);
    }

    @Override
    public List<ProviderModel> getProviders(String realmId, Integer firstResult, Integer max, String search) {
        return session.getProvider(APIConnectorProvider.class).getProviderDetails(realmId, firstResult, max, search);
    }

    @Override
    public List<VerifierModel> getVerifiers(String realmId, Integer firstResult, Integer max, String search, String sortBy) {
        return session.getProvider(APIConnectorProvider.class).getVerifierDetails(realmId, firstResult, max, search, sortBy);
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
        return session.getProvider(APIConnectorProvider.class).getClaims(verification, provider);
    }

    @Override
    public List<UserTransactionModel> getUserTransactionStatus(String userId) {
        return session.getProvider(UserTransactionStatusProvider.class)
                .getUserTransactionStatus(session.getContext().getRealm().getId(),userId);
    }

    private List<UserTransactionEntity> getUserTransactionEntityByUserIdAndRealmId(String userId) {
        TypedQuery<UserTransactionEntity> query = em.createNamedQuery("findByUserIdAndRealmId", UserTransactionEntity.class);
        query.setParameter("userId", userId);
        query.setParameter("realmId", this.realm.getId());

        List<UserTransactionEntity> entity = query.getResultList().stream().collect(Collectors.toList());
        return entity;
    }


    private UserTransactionEntity addUserTransactionToEntity(String trxId, String userId, String status, Long expiresAt) {
        UserTransactionEntity userTransactionEntity = new UserTransactionEntity();
        userTransactionEntity.setRealmId(this.realm.getId());
        userTransactionEntity.setId(KeycloakModelUtils.generateId());
        userTransactionEntity.setTrxId(trxId);
        userTransactionEntity.setStatus(status);
        userTransactionEntity.setUserId(userId);
        userTransactionEntity.setExpiresAt(expiresAt);
        em.persist(userTransactionEntity);

        return userTransactionEntity;
    }

    private UserTransactionEntity getUserTransactionEntity(String trxId, String userId) {
        TypedQuery<UserTransactionEntity> query = em.createNamedQuery("findByTrxAndUserId", UserTransactionEntity.class);
        query.setParameter("trxId", trxId);
        query.setParameter("userId", userId);
        UserTransactionEntity userTransactionEntity = !query.getResultList().isEmpty() ? query.getSingleResult() : null;
        if (userTransactionEntity == null) {
            return null;
        }
        return userTransactionEntity;
    }

    private TrxAttributeEntity persistAttributeValue(String name, String value, String trxId) {
        TrxAttributeEntity attr = new TrxAttributeEntity();
        attr.setId(KeycloakModelUtils.generateId());
        attr.setName(name);
        attr.setValue(value);
        attr.setTrxId(trxId);
        em.persist(attr);
        return attr;
    }

    @Override
    public void removeTrxAttribute(String trxId, String name) {
        session.getProvider(TransactionAttributeProvider.class).removeTrxAttribute(trxId, name);
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
        WebDeviceEntity auth = new WebDeviceEntity();
        String id = (model.getDeviceId() == null) ? KeycloakModelUtils.generateId() : model.getDeviceId();
        auth.setDeviceId(id);
        auth.setDeviceData(model.getDeviceData());
        auth.setClientId(model.getClientId());
        auth.setRealmId(model.getRealmId());
        em.persist(auth);
        model.setDeviceId(auth.getDeviceId());
        return model;
    }

    @Override
    public void removeWebDevice(WebDeviceModel model) {
        WebDeviceEntity entity = getWebDeviceEntity(model.getDeviceId(), false);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public WebDeviceModel getWebDeviceById(String deviceId) {
        WebDeviceEntity entity = getWebDeviceEntity(deviceId, true);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public Stream<UserSyncConfigModel> getUserSyncConfigsStream() {
        return realm.getUserSyncConfigs().stream().map(this::entityToModel);
    }

    @Override
    public UserSyncConfigModel addUserSyncConfigModel(UserSyncConfigModel model) {
        UserSyncConfigEntity auth = new UserSyncConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setName(model.getName());
        auth.setRealm(realm);
        auth.setEnrollmentOption(model.getEnrollmentOption());
        auth.setProviderId(model.getProviderId());
        auth.setConfig(model.getConfig());
        realm.getUserSyncConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void updateUserSyncConfigModel(UserSyncConfigModel model) {
        UserSyncConfigEntity entity = getUserSyncConfigEntity(model.getId(), false);
        if (entity == null) return;

        if (model.getEnrollmentOption() != null) {
            entity.setEnrollmentOption(model.getEnrollmentOption());
        }

        if (model.getProviderId() != null) {
            entity.setProviderId(model.getProviderId());
        }

        if (model.getId() != null) {
            entity.setId(model.getId());
        }

        if (model.getName() != null) {
            entity.setName(model.getName());
        }
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();
    }

    @Override
    public void removeUserSyncConfigModel(UserSyncConfigModel model) {
        UserSyncConfigEntity entity = getUserSyncConfigEntity(model.getId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public UserSyncConfigModel getUserSyncConfigByProviderId(String id) {
        UserSyncConfigEntity entity = getUserSyncConfigEntity(id, true);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public Stream<ActionModel> getActionStream() {
        return realm.getActions().stream()
                .map(this::entityToModel);
    }

    @Override
    public Stream<EztovrfyMarketplaceTemplateModel> getEztovrfyMarketplaceTemplateStream() {
        return realm.getEztovrfyMarketplaceTemplates().stream()
                .map(this::entityToModel);
    }

    @Override
    public List<EztovrfyMarketplaceTemplateModel> searchEztovrfyMarketplaceTemplateParams(String workflowName, String templatetag) {
        TypedQuery<EztovrfyMarketplaceTemplateEntity> query = em.createNamedQuery("searchEztovrfyMarketplaceTemplateByName", EztovrfyMarketplaceTemplateEntity.class);
        query.setParameter("name", workflowName);
        query.setParameter("tag", templatetag);
        List<EztovrfyMarketplaceTemplateEntity> entities = query.getResultList();
        List<EztovrfyMarketplaceTemplateModel> models = new ArrayList<>();
        entities.forEach(x-> models.add(entityToModel(x)));
        return models;
    }

    @Override
    public ActionModel addAction(ActionModel model) {
        ActionEntity auth = new ActionEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setAlias(model.getAlias());
        auth.setSubWorkflow(model.getIsSubWorkflow());
        auth.setRealm(realm.getId());
        auth.setModifiedTimestamp(System.currentTimeMillis());
        auth.setInBuilt(model.isInBuilt());
        auth.setActionType(model.getActionType());
        auth.setActionVariables(model.getActionVariables());
        auth.setMarketplace(model.getMarketPlace());
        realm.getActions().add(auth);
        em.persist(auth);
        em.flush();
        model.setId(auth.getId());
        return model;
    }

    @Override
    public EztovrfyMarketplaceTemplateModel addEztovrfyMarketplaceTemplate(EztovrfyMarketplaceTemplateModel model) {
        EztovrfyMarketplaceTemplateEntity templateEntity = new EztovrfyMarketplaceTemplateEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        templateEntity.setId(id);
        templateEntity.setRealmId(realm.getId());
        templateEntity.setWorkflowName(model.getWorkflowName());
        templateEntity.setWorkflowCategory(model.getWorkflowCategory());
        templateEntity.setImageURL(model.getImageURL());
        templateEntity.setWorkflowDescription(model.getWorkflowDescription());
        templateEntity.setWorkflowJson(model.getWorkflowJson());
        try {
            templateEntity.setTemplateTag(JsonSerialization.writeValueAsString(model.getTemplateTag()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        templateEntity.setModifiedTimestamp(System.currentTimeMillis());
        templateEntity.setWorkflowId(model.getWorkflowId());
        templateEntity.setPublished(model.isPublished());
        templateEntity.setCountry(model.getCountry());
        realm.getEztovrfyMarketplaceTemplates().add(templateEntity);
        em.persist(templateEntity);
        em.flush();
        model.setId(templateEntity.getId());
        return model;
    }

    @Override
    public boolean removeEztovrfyMarketplaceTemplate(EztovrfyMarketplaceTemplateModel model) {
        Optional<EztovrfyMarketplaceTemplateEntity> optionalEntity = realm.getEztovrfyMarketplaceTemplates().stream().filter(x-> x.getId().equals(model.getId())).findFirst();
        if(!optionalEntity.isPresent())
            return false;
        EztovrfyMarketplaceTemplateEntity templateEntity = optionalEntity.get();
        realm.getEztovrfyMarketplaceTemplates().remove(templateEntity);
        em.remove(templateEntity);
        em.flush();
        model.setId(templateEntity.getId());
        return true;
    }

    @Override
    public Stream<EztovrfyMarketplaceCategoryModel> getEztovrfyMarketplaceCategoriesStream() {
        return realm.getEztovrfyMarketplaceCategories().stream()
                .map(this::entityToModel);
    }

    @Override
    public EztovrfyMarketplaceCategoryModel addEztovrfyMarketplaceCategory(EztovrfyMarketplaceCategoryModel model) {
        EztovrfyMarketplaceCategoryEntity templateEntity = new EztovrfyMarketplaceCategoryEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        templateEntity.setId(id);
        templateEntity.setRealmId(realm.getId());
        templateEntity.setCategory(model.getCategory());
        realm.getEztovrfyMarketplaceCategories().add(templateEntity);
        em.persist(templateEntity);
        em.flush();
        model.setId(templateEntity.getId());
        return model;
    }

    @Override
    public boolean removeEztovrfyMarketplaceCategory(EztovrfyMarketplaceCategoryModel model) {
        Optional<EztovrfyMarketplaceCategoryEntity> optionalEntity = realm.getEztovrfyMarketplaceCategories().stream().filter(x-> x.getCategory().equals(model.getCategory())).findFirst();
        if(!optionalEntity.isPresent())
            return false;
        EztovrfyMarketplaceCategoryEntity templateEntity = optionalEntity.get();
        realm.getEztovrfyMarketplaceCategories().remove(templateEntity);
        em.remove(templateEntity);
        em.flush();
        model.setId(templateEntity.getId());
        return true;
    }

    @Override
    public void updateAction(ActionModel model) {
        ActionEntity entity = getActionEntity(model.getId(), false);
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        entity.setModifiedTimestamp(model.getModifiedTimestamp());
        entity.setInBuilt(model.isInBuilt());
        entity.setActionVariables(model.getActionVariables());
        em.flush();
    }

    @Override
    public void removeAction(ActionModel model) {
        ActionEntity entity = getActionEntity(model.getId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public ActionModel getActionById(String id) {
        ActionEntity entity = getActionEntity(id, false);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected AuthenticationFlowModel entityToModel(AuthenticationFlowEntity entity) {
        AuthenticationFlowModel model = new AuthenticationFlowModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        model.setProviderId(entity.getProviderId());
        model.setDescription(entity.getDescription());
        model.setBuiltIn(entity.isBuiltIn());
        model.setHidden(entity.isHidden());
        model.setInstalled(entity.isInstalled());
        model.setTemplateType(entity.getTemplateType());
        model.setTemplateId(entity.getTemplateId());
        model.setTemplateAlias(entity.getTemplateAlias());
        model.setTopLevel(entity.isTopLevel());
        return model;
    }

    public UserSyncConfigModel entityToModel(UserSyncConfigEntity entity) {
        UserSyncConfigModel model = new UserSyncConfigModel();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setEnrollmentOption(entity.getEnrollmentOption());
        model.setProviderId(entity.getProviderId());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String id) {
        AuthenticationFlowEntity entity = getAuthenticationFlowEntity(id, false);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model, boolean force) {
        if (KeycloakModelUtils.isFlowUsed(this, model) && !force) {
            throw new ModelException("Cannot remove authentication flow, it is currently in use");
        }
        AuthenticationFlowEntity entity = getAuthenticationFlowEntity(model.getId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity entity = getAuthenticationFlowEntity(model.getId(), false);
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        entity.setDescription(model.getDescription());
        entity.setProviderId(model.getProviderId());
        entity.setBuiltIn(model.isBuiltIn());
        entity.setInstalled(model.isInstalled());
        entity.setTemplateId(model.getTemplateId());
        entity.setTemplateAlias(model.getTemplateAlias());
        entity.setTemplateType(model.getTemplateType());
        entity.setTopLevel(model.isTopLevel());
    }

    private AuthenticationFlowEntity getAuthenticationFlowEntity(String id, boolean readForRemove) {
        AuthenticationFlowEntity entity = readForRemove
                ? em.find(AuthenticationFlowEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(AuthenticationFlowEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().equals(getEntity())) return null;
        return entity;
    }

    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        AuthenticationFlowEntity entity = new AuthenticationFlowEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setAlias(model.getAlias());
        entity.setDescription(model.getDescription());
        entity.setProviderId(model.getProviderId());
        entity.setBuiltIn(model.isBuiltIn());
        entity.setInstalled(model.isInstalled());
        entity.setHidden(model.isHidden());
        entity.setTemplateId(model.getTemplateId());
        entity.setTemplateAlias(model.getTemplateAlias());
        entity.setTemplateType(model.getTemplateType());
        entity.setTopLevel(model.isTopLevel());
        entity.setRealm(realm);
        realm.getAuthenticationFlows().add(entity);
        em.persist(entity);
        model.setId(entity.getId());
        return model;
    }

    @Override
    public Stream<AuthenticationExecutionModel> getAuthenticationExecutionsStream(String flowId) {
        AuthenticationFlowEntity flow = em.getReference(AuthenticationFlowEntity.class, flowId);
        return flow.getExecutions().stream()
                .filter(e -> getId().equals(e.getRealm().getId()))
                .map(this::entityToModel)
                .sorted(AuthenticationExecutionModel.ExecutionComparator.SINGLETON);
    }

    public AuthenticationExecutionModel entityToModel(AuthenticationExecutionEntity entity) {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(entity.getId());
        model.setRequirement(entity.getRequirement());
        model.setPriority(entity.getPriority());
        model.setAuthenticator(entity.getAuthenticator());
        model.setFlowId(entity.getFlowId());
        model.setHidden(entity.isHidden());
        model.setParentFlow(entity.getParentFlow().getId());
        model.setAuthenticatorFlow(entity.isAutheticatorFlow());
        model.setAuthenticatorConfig(entity.getAuthenticatorConfig());
        return model;
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        AuthenticationExecutionEntity entity = getAuthenticationExecution(id, false);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        TypedQuery<AuthenticationExecutionEntity> query = em.createNamedQuery("authenticationFlowExecution", AuthenticationExecutionEntity.class)
                .setParameter("flowId", flowId);
        if (query.getResultList().isEmpty()) {
            return null;
        }
        AuthenticationExecutionEntity authenticationFlowExecution = query.getResultList().get(0);
        return entityToModel(authenticationFlowExecution);
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = new AuthenticationExecutionEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setFlowId(model.getFlowId());
        entity.setRequirement(model.getRequirement());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
        entity.setHidden(model.isHidden());
        AuthenticationFlowEntity flow = em.find(AuthenticationFlowEntity.class, model.getParentFlow());
        entity.setParentFlow(flow);
        flow.getExecutions().add(entity);
        entity.setRealm(realm);
        entity.setAutheticatorFlow(model.isAuthenticatorFlow());
        em.persist(entity);
        model.setId(entity.getId());
        return model;

    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = getAuthenticationExecution(model.getId(), false);
        if (entity == null) return;
        entity.setAutheticatorFlow(model.isAuthenticatorFlow());
        entity.setAuthenticator(model.getAuthenticator());
        entity.setPriority(model.getPriority());
        entity.setRequirement(model.getRequirement());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
        entity.setFlowId(model.getFlowId());
        if (model.getParentFlow() != null) {
            AuthenticationFlowEntity flow = em.find(AuthenticationFlowEntity.class, model.getParentFlow());
            entity.setParentFlow(flow);
        }
        em.flush();
    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        AuthenticationExecutionEntity entity = getAuthenticationExecution(model.getId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();

    }

    private AuthenticationExecutionEntity getAuthenticationExecution(String id, boolean readForRemove) {
        AuthenticationExecutionEntity entity = readForRemove
                ? em.find(AuthenticationExecutionEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(AuthenticationExecutionEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().equals(getEntity())) return null;
        return entity;
    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity auth = new AuthenticatorConfigEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setAlias(model.getAlias());
        auth.setRealm(realm);
        auth.setConfig(model.getConfig());
        realm.getAuthenticatorConfigs().add(auth);
        em.persist(auth);
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity entity = getAuthenticatorConfigEntity(model.getId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();

    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        AuthenticatorConfigEntity entity = getAuthenticatorConfigEntity(id, false);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    public AuthenticatorConfigModel entityToModel(AuthenticatorConfigEntity entity) {
        AuthenticatorConfigModel model = new AuthenticatorConfigModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    public EventConfigModel entityToModel(EventConfigEntity entity) {
        EventConfigModel model = new EventConfigModel();
        model.setId(entity.getId());
        model.setEventProviderId(entity.getProviderId());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    public ReportModel entityToModel(ReportEntity entity) {
        ReportModel model = new ReportModel();
        model.setId(entity.getId());
        model.setRealmId(entity.getRealmId());
        model.setFileName(entity.getFileName());
        model.setReportType(entity.getReportType());
        model.setRequestedBy(entity.getRequestedBy());
        model.setCreatedDate(entity.getCreatedDate());
        model.setStatus(entity.getStatus());
        return model;
    }

    public MgmtAccessModel entityToModel(MgmtAccessEntity entity) {
        MgmtAccessModel model = new MgmtAccessModel();
        model.setId(entity.getId());
        model.setRealmId(entity.getRealmId());
        model.setName(entity.getName());
        model.setExpiryDate(entity.getExpiryDate());
        model.setRequestedBy(entity.getRequestedBy());
        model.setCreatedDate(entity.getCreatedDate());
        model.setLastUsedDate(entity.getLastUsedDate());
        model.setLastUpdatedDate(entity.getLastUpdatedDate());
        model.setSecret(entity.getSecret());
        model.setSandboxSecret(entity.getSandboxSecret());
        model.setEnabled(entity.isEnabled());
        model.setPermissions(entity.getPermissions());
        model.setSandboxMode(entity.isSandboxMode());
        return model;
    }

    public EmailConfigModel entityToModel(EmailConfigEntity entity) {
        EmailConfigModel model = new EmailConfigModel();
        model.setId(entity.getId());
        model.setEmailProviderId(entity.getProviderId());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    public SmsConfigModel entityToModel(SmsConfigEntity entity) {
        SmsConfigModel model = new SmsConfigModel();
        model.setId(entity.getId());
        model.setSmsProviderId(entity.getProviderId());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    public MFAConfigModel entityToModel(MFAConfigEntity entity) {
        MFAConfigModel model = new MFAConfigModel();
        model.setId(entity.getId());
        model.setMfaProviderId(entity.getProviderId());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    public CibaAuthConfigModel entityToModel(CibaAuthConfigEntity entity) {
        CibaAuthConfigModel model = new CibaAuthConfigModel();
        model.setId(entity.getId());
        model.setCibaAuthProviderId(entity.getProviderId());
        model.setClientId(entity.getClientId());
        model.setRealmId(entity.getRealm());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    public CibaNotificationConfigModel entityToModel(CibaNotificationConfigEntity entity) {
        CibaNotificationConfigModel model = new CibaNotificationConfigModel();
        model.setId(entity.getId());
        model.setCibaNotificationProviderId(entity.getProviderId());
        model.setClientId(entity.getClientId());
        model.setRealmId(entity.getRealm());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    public CibaDeadLetterHookConfigModel entityToModel(CibaDeadLetterHookConfigEntity entity) {
        CibaDeadLetterHookConfigModel model = new CibaDeadLetterHookConfigModel();
        model.setId(entity.getId());
        model.setCibaResultHookProviderId(entity.getProviderId());
        model.setClientId(entity.getClientId());
        model.setRealmId(entity.getRealm());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    public CibaStateModel entityToModel(CibaStateEntity entity) {
        CibaStateModel model = new CibaStateModel();
        model.setCibaStateId(entity.getCibaStateId());
        model.setCibaToken(entity.getCibaToken());
        model.setUserId(entity.getUserId());
        model.setRequestId(entity.getRequestId());
        return model;
    }

    public WebDeviceModel entityToModel(WebDeviceEntity entity) {
        WebDeviceModel model = new WebDeviceModel();
        model.setDeviceId(entity.getDeviceId());
        model.setClientId(entity.getClientId());
        model.setDeviceData(entity.getDeviceData());
        model.setRealmId(entity.getRealmId());
        return model;
    }

    public TrxModel entityToModel(List<TrxAttributeEntity> entity) {
        if (entity == null)
            return null;
        TrxModel model = new TrxModel();
        TrxAttributeEntity attributeEntity = entity.get(0);
        model.setTrxId(attributeEntity.getTrxId());
        Map<String, String> attributes = new HashMap<>();
        for (TrxAttributeEntity en :
                entity) {
            attributes.put(en.getName(), en.getValue());
        }
        model.setAttributes(attributes);
        model.setCreatedAt(attributeEntity.getCreatedTimestamp());
        return model;
    }

    public UserTransactionModel transactionEntityToModel(UserTransactionEntity entity) {
        if (entity == null)
            return null;
        UserTransactionModel model = new UserTransactionModel();
        model.setTrxId(entity.getTrxId());
        model.setUserId(entity.getUserId());
        model.setStatus(entity.getStatus());
        model.setCreatedAt(entity.getCreatedTimestamp());
        model.setExpiresAt(entity.getExpiresAt());
        return model;
    }

    @Override
    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        AuthenticatorConfigEntity entity = getAuthenticatorConfigEntity(model.getId(), false);
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();

    }

    private AuthenticatorConfigEntity getAuthenticatorConfigEntity(String id, boolean readForRemove) {
        AuthenticatorConfigEntity entity = readForRemove
                ? em.find(AuthenticatorConfigEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(AuthenticatorConfigEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().equals(getEntity())) return null;
        return entity;
    }

    private EventConfigEntity getEventConfigEntity(String id, boolean readForRemove) {
        TypedQuery<EventConfigEntity> query = em.createNamedQuery("findByProviderAndRealm", EventConfigEntity.class);
        query.setParameter("providerId", id);

        Optional<EventConfigEntity> entity = query.getResultList().stream().filter(x -> x.getRealm().getId().equals(realm.getId())).findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private UserSyncConfigEntity getUserSyncConfigEntity(String id, boolean readForRemove) {
        TypedQuery<UserSyncConfigEntity> query = em.createNamedQuery("findByUserSyncById", UserSyncConfigEntity.class);
        query.setParameter("id", id);
        query.setParameter("realm", realm.getId());

        Optional<UserSyncConfigEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private MarketplaceConfigEntity getMarketplaceConfigEntity(String id, boolean readForRemove) {
        TypedQuery<MarketplaceConfigEntity> query = em.createNamedQuery("findByMarketplaceAndRealm", MarketplaceConfigEntity.class);
        query.setParameter("providerId", id);
        query.setParameter("realm", realm.getId());

        Optional<MarketplaceConfigEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private Stream<MarketplaceConfigEntity> getMarketplaceConfigEntityByType(String type) {
        TypedQuery<MarketplaceConfigEntity> query = em.createNamedQuery("findByMarketplaceTypeAndRealm", MarketplaceConfigEntity.class);
        query.setParameter("providerType", type);
        query.setParameter("realm", realm.getId());

        return query.getResultList().stream();
    }


    private ReportEntity getReportEntity(String id, boolean readForRemove) {
        TypedQuery<ReportEntity> query = em.createNamedQuery("getReportById", ReportEntity.class);
        query.setParameter("id", id);

        Optional<ReportEntity> entity = query.getResultList().stream().filter(x -> x.getRealmId().equals(realm.getId())).findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private MgmtAccessEntity getMgmtAccessEntity(String id, boolean readForRemove) {
        TypedQuery<MgmtAccessEntity> query = em.createNamedQuery("getMgmtAccessById", MgmtAccessEntity.class);
        query.setParameter("id", id);
        query.setParameter("realmId", realm.getId());

        Optional<MgmtAccessEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }


    private EmailConfigEntity getEmailConfigEntity(String id, boolean readForRemove) {
        TypedQuery<EmailConfigEntity> query = em.createNamedQuery("findByEmailProviderAndRealm", EmailConfigEntity.class);
        query.setParameter("providerId", id);

        Optional<EmailConfigEntity> entity = query.getResultList().stream().filter(x -> x.getRealm().getId().equals(realm.getId())).findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private SmsConfigEntity getSmsConfigEntity(String id, boolean readForRemove) {
        TypedQuery<SmsConfigEntity> query = em.createNamedQuery("findBySmsProviderAndRealm", SmsConfigEntity.class);
        query.setParameter("providerId", id);

        Optional<SmsConfigEntity> entity = query.getResultList().stream().filter(x -> x.getRealm().getId().equals(realm.getId())).findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private MFAConfigEntity getMFAConfigEntity(String id, boolean readForRemove) {
        TypedQuery<MFAConfigEntity> query = em.createNamedQuery("findByMFAProviderAndRealm", MFAConfigEntity.class);
        query.setParameter("providerId", id);

        Optional<MFAConfigEntity> entity = query.getResultList().stream().filter(x -> x.getRealm().getId().equals(realm.getId())).findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private CibaNotificationConfigEntity getCibaNotificationConfigEntity(String providerId, String clientId, String realmId) {
        TypedQuery<CibaNotificationConfigEntity> query = em.createNamedQuery("findByCibaNotProviderAndRealm", CibaNotificationConfigEntity.class);
        query.setParameter("providerId", providerId);
        query.setParameter("clientId", clientId);
        query.setParameter("realmId", realmId);
        Optional<CibaNotificationConfigEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private CibaDeadLetterHookConfigEntity getCibaDeadLetterHookNotificationConfigEntity(String providerId, String clientId, String realmId) {
        TypedQuery<CibaDeadLetterHookConfigEntity> query = em.createNamedQuery("findByCibaDeadLetterHookNotificationProviderAndRealm", CibaDeadLetterHookConfigEntity.class);
        query.setParameter("providerId", providerId);
        query.setParameter("clientId", clientId);
        query.setParameter("realmId", realmId);
        Optional<CibaDeadLetterHookConfigEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private CibaNotificationConfigEntity getCibaNotificationConfigEntity(String clientId, String realmId) {
        TypedQuery<CibaNotificationConfigEntity> query = em.createNamedQuery("findByCibaNotProviderByClient", CibaNotificationConfigEntity.class);
        query.setParameter("clientId", clientId);
        query.setParameter("realmId", realmId);
        Optional<CibaNotificationConfigEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private CibaDeadLetterHookConfigEntity getCibaDeadLetterHookNotificationConfigEntity(String clientId, String realmId) {
        TypedQuery<CibaDeadLetterHookConfigEntity> query = em.createNamedQuery("findByCibaDeadLetterHookNotificationProviderByClient", CibaDeadLetterHookConfigEntity.class);
        query.setParameter("clientId", clientId);
        query.setParameter("realmId", realmId);
        Optional<CibaDeadLetterHookConfigEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private CibaAuthConfigEntity getCibaAuthConfigEntity(String providerId, String clientId, String realmId) {
        TypedQuery<CibaAuthConfigEntity> query = em.createNamedQuery("findByCibaProviderAndRealm", CibaAuthConfigEntity.class);
        query.setParameter("providerId", providerId);
        query.setParameter("clientId", clientId);
        query.setParameter("realmId", realmId);

        Optional<CibaAuthConfigEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private CibaAuthConfigEntity getCibaAuthConfigEntity(String clientId, String realmId) {
        TypedQuery<CibaAuthConfigEntity> query = em.createNamedQuery("findByCibaProviderByClient", CibaAuthConfigEntity.class);
        query.setParameter("clientId", clientId);
        query.setParameter("realmId", realmId);

        Optional<CibaAuthConfigEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private CibaStateEntity getCibaStateEntity(String id, boolean readForRemove) {
        TypedQuery<CibaStateEntity> query = em.createNamedQuery("findByCibaState", CibaStateEntity.class);
        query.setParameter("cibaStateId", id);

        Optional<CibaStateEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    private WebDeviceEntity getWebDeviceEntity(String id, boolean readForRemove) {
        TypedQuery<WebDeviceEntity> query = em.createNamedQuery("findByWebDeviceId", WebDeviceEntity.class);
        query.setParameter("deviceId", id);

        Optional<WebDeviceEntity> entity = query.getResultList().stream().findFirst();
        if (!entity.isPresent()) return null;
        return entity.get();
    }

    @Override
    public Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream() {
        return realm.getAuthenticatorConfigs().stream().map(this::entityToModel);
    }

    @Override
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity auth = new RequiredActionProviderEntity();
        String id = (model.getId() == null) ? KeycloakModelUtils.generateId() : model.getId();
        auth.setId(id);
        auth.setPolicyId(model.getPolicyId());
        auth.setPolicyType(model.getPolicyType() == null ? "action" : model.getPolicyType());
        auth.setActionId(model.getActionId());
        auth.setAlias(model.getAlias());
        auth.setName(model.getName());
        auth.setRealm(realm);
        auth.setProviderId(model.getProviderId());
        auth.setConfig(model.getConfig());
        auth.setEnabled(model.isEnabled());
        auth.setValidator(model.isValidator());
        auth.setParentId(model.getParentId());
        auth.setDirection(model.getDirection());
        auth.setDefaultAction(model.isDefaultAction());
        auth.setValidatorStepId(model.getValidatorStepId());
        auth.setPriority(model.getPriority());
        auth.setSkippable(model.isSkippable());
        realm.getRequiredActionProviders().add(auth);
        em.persist(auth);
        em.flush();
        model.setId(auth.getId());
        return model;
    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity entity = getRequiredProviderEntity(model.getId(), true);
        if (entity == null) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public void removeRequiredActionProviders(String actionId) {
        em.createNamedQuery("deleteRequiredActionProviderByRealm")
                .setParameter("realm", getEntity())
                .setParameter("actionId", actionId).executeUpdate();
        em.flush();
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        RequiredActionProviderEntity entity = getRequiredProviderEntity(id, false);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public List<TermsAndConditionsModel> getTermsAndConditionsByVersion(String alias, String version) {
        TypedQuery<TermsAndConditionsVersionEntity> query = em.createNamedQuery("findTermsAndConditionsByVersion", TermsAndConditionsVersionEntity.class);
        query.setParameter("termsAndConditionId", alias);
        query.setParameter("version", version);
        List<TermsAndConditionsVersionEntity> entity = query.getResultList().stream().collect(Collectors.toList());
        return termsAndConditionsEntityToModel(entity);
    }

    @Override
    public List<TermsAndConditionsModel> getTermsAndConditionsVersions(String alias) {
        TypedQuery<TermsAndConditionsVersionEntity> query = em.createNamedQuery("findTermsAndConditionsVersion", TermsAndConditionsVersionEntity.class);
        query.setParameter("termsAndConditionId", alias);
        List<TermsAndConditionsVersionEntity> entity = query.getResultList().stream().collect(Collectors.toList());
        return termsAndConditionsEntityToModel(entity);
    }

    @Override
    public TermsAndConditionsModel addTermsAndConditions(String alias, String realmId, String id, String version, String type, String content) {
        TermsAndConditionsVersionEntity termsAndConditionsVersionEntity = addTermsAndConditionsToEntity(alias, realmId, id, version, type, content);
        TermsAndConditionsModel model = new TermsAndConditionsModel();
        model.setRequiredActionId(termsAndConditionsVersionEntity.getRequiredActionId());
        model.setId(termsAndConditionsVersionEntity.getId());
        model.setRealmId(termsAndConditionsVersionEntity.getRealmId());
        model.setType(termsAndConditionsVersionEntity.getType());
        model.setVersion(termsAndConditionsVersionEntity.getVersion());
        model.setContent(termsAndConditionsVersionEntity.getContent());
        return model;
    }

    @Override
    public void removeTermsAndConditionsVersions(String requiredActionId) {
        Query query = em.createNamedQuery("removeTermsAndConditionsVersions");
        query.setParameter("termsAndConditionId", requiredActionId);

        query.executeUpdate();
    }

    @Override
    public void removeTermsAndConditionsVersionByVersion(String requiredActionId, String version) {
        Query query = em.createNamedQuery("removeTermsAndConditionsVersionByVersion");
        query.setParameter("version", version);
        query.setParameter("termsAndConditionId", requiredActionId);

        query.executeUpdate();
    }

    @Override
    public void updateTermsAndConditionsVersion(String id, String type, String content, String requiredActionId, String version) {
        Query query = em.createNamedQuery("updateTermsAndConditionsVersions");
        query.setParameter("id", id);
        query.setParameter("type", type);
        query.setParameter("content", content);
        query.setParameter("termsAndConditionId", requiredActionId);
        query.setParameter("version", version);

        query.executeUpdate();
    }

    @Override
    public List<TermsAndConditionsModel> getTermsAndConditions(String realmId) {
        TypedQuery<TermsAndConditionsVersionEntity> query = em.createNamedQuery("findTermsAndConditions", TermsAndConditionsVersionEntity.class);
        query.setParameter("realmId", realmId);
        List<TermsAndConditionsVersionEntity> entity = query.getResultList().stream().collect(Collectors.toList());
        return termsAndConditionsEntityToModel(entity);
    }

    private List<TermsAndConditionsModel> termsAndConditionsEntityToModel(List<TermsAndConditionsVersionEntity> entity) {
        List<TermsAndConditionsModel> termsandConditions = new ArrayList<>();
        entity.stream().forEach(x -> {
            TermsAndConditionsModel model = new TermsAndConditionsModel();
            model.setId(x.getId());
            model.setContent(x.getContent());
            model.setType(x.getType());
            model.setRealmId(x.getRealmId());
            model.setRequiredActionId(x.getRequiredActionId());
            model.setVersion(x.getVersion());
            termsandConditions.add(model);
        });
        return termsandConditions;
    }

    public TermsAndConditionsVersionEntity addTermsAndConditionsToEntity(String requiredActionId, String realmId, String id, String version, String type, String content){
        TermsAndConditionsVersionEntity termsAndConditionsVersionEntity = new TermsAndConditionsVersionEntity();
        termsAndConditionsVersionEntity.setRequiredActionId(requiredActionId);
        termsAndConditionsVersionEntity.setRealmId(realmId);
        termsAndConditionsVersionEntity.setId(id);
        termsAndConditionsVersionEntity.setVersion(version);
        termsAndConditionsVersionEntity.setType(type);
        termsAndConditionsVersionEntity.setContent(content);
        em.persist(termsAndConditionsVersionEntity);

        return termsAndConditionsVersionEntity;
    }

    public RequiredActionProviderModel entityToModel(RequiredActionProviderEntity entity) {
        String policyType = entity.getPolicyType();
        if (policyType == null) {
            if (realm.getMode().equals("bo")) {
                policyType = "ciba";
            } else {
                policyType = "action";
            }
        }
        RequiredActionProviderModel model = new RequiredActionProviderModel();
        model.setId(entity.getId());
        model.setProviderId(entity.getProviderId());
        model.setAlias(entity.getAlias());
        model.setPolicyId(entity.getPolicyId());
        model.setPolicyType(policyType);
        model.setEnabled(entity.isEnabled());
        model.setValidator(entity.isValidator());
        model.setSkippable(entity.isSkippable());
        model.setDefaultAction(entity.isDefaultAction());
        model.setParentId(entity.getParentId());
        model.setDirection(entity.getDirection());
        model.setValidatorStepId(entity.getValidatorStepId());
        model.setPriority(entity.getPriority());
        model.setName(entity.getName());
        model.setDescription(entity.getDescription());
        model.setActionId(entity.getActionId());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        RequiredActionProviderEntity entity = getRequiredProviderEntity(model.getId(), false);
        if (entity == null) return;
        entity.setAlias(model.getAlias());
        entity.setDescription(model.getDescription());
        entity.setProviderId(model.getProviderId());
        entity.setEnabled(model.isEnabled());
        entity.setSkippable(model.isSkippable());
        entity.setValidator(model.isValidator());
        entity.setParentId(model.getParentId());
        entity.setDirection(model.getDirection());
        entity.setValidatorStepId(model.getValidatorStepId());
        entity.setDefaultAction(model.isDefaultAction());
        entity.setPriority(model.getPriority());
        entity.setPolicyId(model.getPolicyId());
        entity.setPolicyType(model.getPolicyType());
        entity.setActionId(model.getActionId());
        entity.setName(model.getName());
        if (entity.getConfig() == null) {
            entity.setConfig(model.getConfig());
        } else {
            entity.getConfig().clear();
            if (model.getConfig() != null) {
                entity.getConfig().putAll(model.getConfig());
            }
        }
        em.flush();

    }

    @Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream(String id) {
        return realm.getRequiredActionProviders().stream()
                .filter(x -> x.getActionId() != null && x.getActionId().equals(id))
                .map(this::entityToModel)
                .sorted(RequiredActionProviderModel.RequiredActionComparator.SINGLETON);
    }

    @Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream() {
        return realm.getRequiredActionProviders().stream()
                .map(this::entityToModel)
                .sorted(RequiredActionProviderModel.RequiredActionComparator.SINGLETON);
    }

    private RequiredActionProviderEntity getRequiredProviderEntity(String id, boolean readForRemove) {
        RequiredActionProviderEntity entity = readForRemove
                ? em.find(RequiredActionProviderEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(RequiredActionProviderEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().equals(getEntity())) return null;
        return entity;
    }

    private ActionEntity getActionEntity(String id, boolean readForRemove) {
        ActionEntity entity = readForRemove
                ? em.find(ActionEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(ActionEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().equals(getEntity().getId())) return null;
        return entity;
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        return realm.getRequiredActionProviders().stream()
                .map(this::entityToModel)
                .filter(action -> Objects.equals(action.getAlias(), alias))
                .findFirst()
                .orElse(null);
    }

    @Override
    public GroupModel createGroup(String id, String name, GroupModel toParent) {
        return session.groups().createGroup(this, id, name, toParent);
    }

    @Override
    public void moveGroup(GroupModel group, GroupModel toParent) {
        session.groups().moveGroup(this, group, toParent);
    }

    @Override
    public GroupModel getGroupById(String id) {
        return session.groups().getGroupById(this, id);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return session.groups().getGroupsStream(this);
    }

    @Override
    public Long getGroupsCount(Boolean onlyTopGroups) {
        return session.groups().getGroupsCount(this, onlyTopGroups);
    }

    @Override
    public Long getGroupsCountByNameContaining(String search) {
        return session.groups().getGroupsCountByNameContaining(this, search);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream() {
        return session.groups().getTopLevelGroupsStream(this);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max) {
        return session.groups().getTopLevelGroupsStream(this, first, max);
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(String search, Integer first, Integer max) {
        return session.groups().searchForGroupByNameStream(this, search, first, max);
    }

    @Override
    public boolean removeGroup(GroupModel group) {
        return session.groups().removeGroup(this, group);
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream() {
        return session.clientScopes().getClientScopesStream(this);
    }

    @Override
    public ClientScopeModel addClientScope(String name) {
        return session.clientScopes().addClientScope(this, name);
    }

    @Override
    public ClientScopeModel addClientScope(String id, String name) {
        return session.clientScopes().addClientScope(this, id, name);
    }

    @Override
    public boolean removeClientScope(String id) {
        return session.clientScopes().removeClientScope(this, id);
    }

    @Override
    public ClientScopeModel getClientScopeById(String id) {
        return session.clientScopes().getClientScopeById(this, id);
    }

    @Override
    public void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        DefaultClientScopeRealmMappingEntity entity = new DefaultClientScopeRealmMappingEntity();
        entity.setClientScopeId(clientScope.getId());
        entity.setRealm(getEntity());
        entity.setDefaultScope(defaultScope);
        em.persist(entity);
        em.flush();
        em.detach(entity);
    }

    @Override
    public void removeDefaultClientScope(ClientScopeModel clientScope) {
        int numRemoved = em.createNamedQuery("deleteDefaultClientScopeRealmMapping")
                .setParameter("clientScopeId", clientScope.getId())
                .setParameter("realm", getEntity())
                .executeUpdate();
        em.flush();
    }

    @Override
    public void createOrUpdateRealmLocalizationTexts(String locale, Map<String, String> localizationTexts) {

    }

    @Override
    public Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope) {
        TypedQuery<String> query = em.createNamedQuery("defaultClientScopeRealmMappingIdsByRealm", String.class);
        query.setParameter("realm", getEntity());
        query.setParameter("defaultScope", defaultScope);
        return closing(query.getResultStream().map(this::getClientScopeById).filter(Objects::nonNull));
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        model = importComponentModel(model);
        ComponentUtil.notifyCreated(session, this, model);

        return model;
    }

    /**
     * This just exists for testing purposes
     */
    public static final String COMPONENT_PROVIDER_EXISTS_DISABLED = "component.provider.exists.disabled";

    @Override
    public ComponentModel importComponentModel(ComponentModel model) {
        ComponentFactory componentFactory = null;
        try {
            componentFactory = ComponentUtil.getComponentFactory(session, model);
            if (componentFactory == null && System.getProperty(COMPONENT_PROVIDER_EXISTS_DISABLED) == null) {
                throw new IllegalArgumentException("Invalid component type");
            }
            componentFactory.validateConfiguration(session, this, model);
        } catch (Exception e) {
            if (System.getProperty(COMPONENT_PROVIDER_EXISTS_DISABLED) == null) {
                throw e;
            }

        }


        ComponentEntity c = new ComponentEntity();
        if (model.getId() == null) {
            c.setId(KeycloakModelUtils.generateId());
        } else {
            c.setId(model.getId());
        }
        c.setName(model.getName());
        c.setParentId(model.getParentId());
        if (model.getParentId() == null) {
            c.setParentId(this.getId());
            model.setParentId(this.getId());
        }
        c.setProviderType(model.getProviderType());
        c.setProviderId(model.getProviderId());
        c.setSubType(model.getSubType());
        c.setRealm(realm);
        em.persist(c);
        realm.getComponents().add(c);
        setConfig(model, c);
        model.setId(c.getId());
        return model;
    }

    protected void setConfig(ComponentModel model, ComponentEntity c) {
        c.getComponentConfigs().clear();
        for (String key : model.getConfig().keySet()) {
            List<String> vals = model.getConfig().get(key);
            if (vals == null) {
                continue;
            }
            for (String val : vals) {
                ComponentConfigEntity config = new ComponentConfigEntity();
                config.setId(KeycloakModelUtils.generateId());
                config.setName(key);
                config.setValue(val);
                config.setComponent(c);
                c.getComponentConfigs().add(config);
            }
        }
    }

    @Override
    public void updateComponent(ComponentModel component) {
        ComponentUtil.getComponentFactory(session, component).validateConfiguration(session, this, component);

        ComponentEntity c = getComponentEntity(component.getId());
        if (c == null) return;
        ComponentModel old = entityToModel(c);
        c.setName(component.getName());
        c.setProviderId(component.getProviderId());
        c.setProviderType(component.getProviderType());
        c.setParentId(component.getParentId());
        c.setSubType(component.getSubType());
        setConfig(component, c);
        ComponentUtil.notifyUpdated(session, this, old, component);


    }

    @Override
    public void removeComponent(ComponentModel component) {
        ComponentEntity c = getComponentEntity(component.getId());
        if (c == null) return;
        session.users().preRemove(this, component);
        ComponentUtil.notifyPreRemove(session, this, component);
        removeComponents(component.getId());
        getEntity().getComponents().remove(c);
    }

    @Override
    public void removeComponents(String parentId) {
        Predicate<ComponentEntity> sameParent = c -> Objects.equals(parentId, c.getParentId());

        getEntity().getComponents().stream()
                .filter(sameParent)
                .map(this::entityToModel)
                .forEach((ComponentModel c) -> {
                    session.users().preRemove(this, c);
                    ComponentUtil.notifyPreRemove(session, this, c);
                });

        getEntity().getComponents().removeIf(sameParent);
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId, final String providerType) {
        if (parentId == null) parentId = getId();
        final String parent = parentId;

        Stream<ComponentEntity> componentStream = realm.getComponents().stream()
                .filter(c -> Objects.equals(parent, c.getParentId()));

        if (providerType != null) {
            componentStream = componentStream.filter(c -> Objects.equals(providerType, c.getProviderType()));
        }
        return componentStream.map(this::entityToModel);
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(final String parentId) {
        return getComponentsStream(parentId, null);
    }

    protected ComponentModel entityToModel(ComponentEntity c) {
        ComponentModel model = new ComponentModel();
        model.setId(c.getId());
        model.setName(c.getName());
        model.setProviderType(c.getProviderType());
        model.setProviderId(c.getProviderId());
        model.setSubType(c.getSubType());
        model.setParentId(c.getParentId());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        for (ComponentConfigEntity configEntity : c.getComponentConfigs()) {
            config.add(configEntity.getName(), configEntity.getValue());
        }
        model.setConfig(config);
        return model;
    }

    private RequiredCredentialModel toRequiredCredentialModel(RequiredCredentialEntity entity) {
        RequiredCredentialModel model = new RequiredCredentialModel();
        model.setFormLabel(entity.getFormLabel());
        model.setType(entity.getType());
        model.setSecret(entity.isSecret());
        model.setInput(entity.isInput());
        return model;
    }

    @Override
    public Stream<ComponentModel> getComponentsStream() {
        return realm.getComponents().stream().map(this::entityToModel);
    }

    @Override
    public ComponentModel getComponent(String id) {
        ComponentEntity c = getComponentEntity(id);
        return c == null ? null : entityToModel(c);
    }

    private ComponentEntity getComponentEntity(String id) {
        ComponentEntity c = em.find(ComponentEntity.class, id);
        if (c == null) return null;
        if (!c.getRealm().equals(getEntity())) return null;
        return c;
    }

    @Override
    public boolean removeRealmLocalizationTexts(String locale) {
        if (locale == null) return false;
        if (realm.getRealmLocalizationTexts().containsKey(locale)) {
            em.remove(realm.getRealmLocalizationTexts().get(locale));
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Map<String, String>> getRealmLocalizationTexts() {
        Map<String, Map<String, String>> localizationTexts = new HashMap<>();
        realm.getRealmLocalizationTexts().forEach((locale, localizationTextsEntity) -> {
            localizationTexts.put(localizationTextsEntity.getLocale(), localizationTextsEntity.getTexts());
        });
        return localizationTexts;
    }

    @Override
    public Map<String, String> getRealmLocalizationTextsByLocale(String locale) {
        if (realm.getRealmLocalizationTexts().containsKey(locale)) {
            return realm.getRealmLocalizationTexts().get(locale).getTexts();
        }
        return Collections.emptyMap();
    }

    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(int expiration, int count) {
        RealmEntity realmEntity = em.find(RealmEntity.class, realm.getId());

        ClientInitialAccessEntity entity = new ClientInitialAccessEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealm(realmEntity);

        entity.setCount(count);
        entity.setRemainingCount(count);

        int currentTime = Time.currentTime();
        entity.setTimestamp(currentTime);
        entity.setExpiration(expiration);

        em.persist(entity);

        return entityToModel(entity);
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(String id) {
        ClientInitialAccessEntity entity = em.find(ClientInitialAccessEntity.class, id);
        if (entity == null) return null;
        if (!entity.getRealm().getId().equals(realm.getId())) return null;
        return entityToModel(entity);
    }

    @Override
    public void removeClientInitialAccessModel(String id) {
        ClientInitialAccessEntity entity = em.find(ClientInitialAccessEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) return;
        if (!entity.getRealm().getId().equals(realm.getId())) return;
        em.remove(entity);
        em.flush();
    }

    @Override
    public Stream<ClientInitialAccessModel> getClientInitialAccesses() {
        RealmEntity realmEntity = em.find(RealmEntity.class, realm.getId());

        TypedQuery<ClientInitialAccessEntity> query = em.createNamedQuery("findClientInitialAccessByRealm", ClientInitialAccessEntity.class);
        query.setParameter("realm", realmEntity);
        return closing(query.getResultStream().map(this::entityToModel));
    }

    @Override
    public void decreaseRemainingCount(ClientInitialAccessModel clientInitialAccess) {
        em.createNamedQuery("decreaseClientInitialAccessRemainingCount")
                .setParameter("id", clientInitialAccess.getId())
                .executeUpdate();
    }

    private ClientInitialAccessModel entityToModel(ClientInitialAccessEntity entity) {
        ClientInitialAccessModel model = new ClientInitialAccessModel();
        model.setId(entity.getId());
        model.setCount(entity.getCount());
        model.setRemainingCount(entity.getRemainingCount());
        model.setExpiration(entity.getExpiration());
        model.setTimestamp(entity.getTimestamp());
        return model;
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        return realm.getAuthFlowBindings().get(binding);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        Map<String, String> config = new HashMap<String, String>();
        config.putAll(realm.getAuthFlowBindings());
        return Collections.unmodifiableMap(config);
    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String binding) {
        realm.getAuthFlowBindings().remove(binding);
    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        realm.getAuthFlowBindings().put(binding, flowId);
    }

    @Override
    public Boolean getApplicationConnections(String connId) {
        return realm.getAppConnections().get(connId);
    }

    @Override
    public Map<String, Boolean> getApplicationsConnections() {
        Map<String, Boolean> copy = new HashMap<>();
        copy.putAll(realm.getAppConnections());
        return copy;
    }

    @Override
    public void removeApplicationConnections(String connId) {
        realm.getAppConnections().remove(connId);
    }

    @Override
    public void setApplicationConnections(String connId, Boolean connEnabled) {
        realm.getAppConnections().put(connId, connEnabled);
    }

    @Override
    public Map<String, Boolean> getMFAConnections() {
        Map<String, Boolean> copy = new HashMap<>();
        copy.putAll(realm.getMfaConnections());
        return copy;
    }

    @Override
    public void removeMFAConnections(String connId) {
        realm.getMfaConnections().remove(connId);
    }

    @Override
    public void setMFAConnections(String connId, Boolean connEnabled) {
        realm.getMfaConnections().put(connId, connEnabled);
    }

    @Override
    public Stream<String> getPreferredLoginLocalesStream() {
        return realm.getPreferredLoginLocales().stream();
    }

    @Override
    public void setPreferredLoginLocales(Set<String> locales) {
        realm.setPreferredLoginLocales(locales);
        em.flush();
    }

    @Override
    public Boolean isSandboxMode() {
        return realm.getWorkspaceSandboxMode();
    }

    @Override
    public void setSandboxMode(Boolean sandboxMode) {
        realm.setWorkspaceSandboxMode(sandboxMode);
    }

    @Override
    public Stream<CustomDomainMappingModel> getCustomDomainMappings() {
        return realm.getCustomDomainMappings().stream().map(this::toCustomDomainMappingModel);
    }

    @Override
    public CustomDomainMappingModel getCustomDomainMappingByDomain(String domain) {
        Optional<CustomDomainMappingModel> customDomainMappingModel = realm.getCustomDomainMappings().stream()
                .filter(customDomainMappingsEntity -> customDomainMappingsEntity.getCustomDomain().equalsIgnoreCase(domain))
                .map(this::toCustomDomainMappingModel).findAny();
        return customDomainMappingModel.orElse(null);
    }

    @Override
    public CustomDomainMappingModel updateCustomDomainMapping(CustomDomainMappingModel customDomainMappingModel) {
        Optional<CustomDomainMappingsEntity> customDomainMappingEntities = realm.getCustomDomainMappings().stream()
                .filter(customDomainMappingsEntity -> customDomainMappingsEntity.getCustomDomain().equalsIgnoreCase(customDomainMappingModel.getCustomDomain())).findAny();

        CustomDomainMappingsEntity customDomainMappingsEntity = customDomainMappingEntities.orElse(null);
        if (customDomainMappingsEntity == null) customDomainMappingsEntity = new CustomDomainMappingsEntity();

        customDomainMappingsEntity.setCustomDomain(customDomainMappingModel.getCustomDomain());
        customDomainMappingsEntity.setARecordName(customDomainMappingModel.getARecordName());
        customDomainMappingsEntity.setARecordValue(customDomainMappingModel.getARecordValue());
        customDomainMappingsEntity.setARecordVerified(customDomainMappingModel.getARecordVerified() != null ? customDomainMappingModel.getARecordVerified() : false);
        customDomainMappingsEntity.setTxtRecordName(customDomainMappingModel.getTxtRecordName());
        customDomainMappingsEntity.setTxtRecordValue(customDomainMappingModel.getTxtRecordValue());
        customDomainMappingsEntity.setTxtRecordVerified(customDomainMappingModel.getTxtRecordVerified() != null ? customDomainMappingModel.getTxtRecordVerified() : false);
        customDomainMappingsEntity.setSetupCompleted(customDomainMappingModel.isSetupCompleted() != null ? customDomainMappingModel.isSetupCompleted() : false);
        customDomainMappingsEntity.setActive(customDomainMappingModel.isActive() != null ? customDomainMappingModel.isActive() : false);
        customDomainMappingsEntity.setIsPrimary(customDomainMappingModel.isPrimary() != null ? customDomainMappingModel.isPrimary() : false);
        customDomainMappingsEntity.setRealm(realm);

        em.persist(customDomainMappingsEntity);
        em.flush();

        return toCustomDomainMappingModel(customDomainMappingsEntity);
    }

    @Override
    public void removeCustomDomainMapping(String domain) {
        Optional<CustomDomainMappingsEntity> customDomainMappingsEntity = realm.getCustomDomainMappings().stream().filter(entity -> entity.getCustomDomain().equalsIgnoreCase(domain)).findAny();
        CustomDomainMappingsEntity entity = customDomainMappingsEntity.orElse(null);
        if (entity == null) return;
        em.remove(entity);
    }

    private CustomDomainMappingModel toCustomDomainMappingModel(CustomDomainMappingsEntity entity) {
        CustomDomainMappingModel model = new CustomDomainMappingModel();
        model.setId(entity.getId());
        model.setARecord(entity.getaRecordName());
        model.setARecordVerified(entity.isARecordVerified());
        model.setTxtRecordName(entity.getTxtRecordName());
        model.setTxtRecordVerified(entity.isTxtRecordVerified());
        model.setCustomDomain(entity.getCustomDomain());
        model.setSetupCompleted(entity.isSetupCompleted());
        model.setActive(entity.isActive());
        model.setARecordValue(entity.getARecordValue());
        model.setTxtRecordValue(entity.getTxtRecordValue());
        model.setIsPrimary(entity.isPrimary());
        return model;
    }
}

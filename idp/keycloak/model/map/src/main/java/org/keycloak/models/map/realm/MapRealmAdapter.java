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
package org.keycloak.models.map.realm;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.*;
import org.keycloak.models.map.common.TimeAdapter;
import org.keycloak.models.map.realm.entity.*;
import org.keycloak.models.utils.ComponentUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

public class MapRealmAdapter extends AbstractRealmModel<MapRealmEntity> implements RealmModel {

    private static final Logger LOG = Logger.getLogger(MapRealmAdapter.class);
    private static final String ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN = "actionTokenGeneratedByUserLifespan";
    private static final String DEFAULT_SIGNATURE_ALGORITHM = "defaultSignatureAlgorithm";
    private static final String BRUTE_FORCE_PROTECTED = "bruteForceProtected";
    private static final String PERMANENT_LOCKOUT = "permanentLockout";
    private static final String MAX_FAILURE_WAIT_SECONDS = "maxFailureWaitSeconds";
    private static final String WAIT_INCREMENT_SECONDS = "waitIncrementSeconds";
    private static final String QUICK_LOGIN_CHECK_MILLISECONDS = "quickLoginCheckMilliSeconds";
    private static final String MINIMUM_QUICK_LOGIN_WAIT_SECONDS = "minimumQuickLoginWaitSeconds";
    private static final String MAX_DELTA_SECONDS = "maxDeltaTimeSeconds";
    private static final String FAILURE_FACTOR = "failureFactor";

    private PasswordPolicy passwordPolicy;

    public MapRealmAdapter(KeycloakSession session, MapRealmEntity entity) {
        super(session, entity);
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public String getDisplayName() {
        return entity.getDisplayName();
    }

    @Override
    public void setDisplayName(String displayName) {
        entity.setDisplayName(displayName);
    }

    @Override
    public String getDisplayNameHtml() {
        return entity.getDisplayNameHtml();
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        entity.setDisplayNameHtml(displayNameHtml);
    }

    @Override
    public boolean isEnabled() {
        Boolean enabled = entity.isEnabled();
        return enabled == null ? false : enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    @Override
    public SslRequired getSslRequired() {
        String sslRequired = entity.getSslRequired();
        return sslRequired == null ? null : SslRequired.valueOf(sslRequired);
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        entity.setSslRequired(sslRequired.name());
    }

    @Override
    public boolean isRegistrationAllowed() {
        Boolean is = entity.isRegistrationAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        entity.setRegistrationAllowed(registrationAllowed);
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        Boolean is = entity.isRegistrationEmailAsUsername();
        return is == null ? false : is;
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        entity.setRegistrationEmailAsUsername(registrationEmailAsUsername);
    }

    @Override
    public boolean isRememberMe() {
        Boolean is = entity.isRememberMe();
        return is == null ? false : is;
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        entity.setRememberMe(rememberMe);
    }

    @Override
    public boolean isEditUsernameAllowed() {
        Boolean is = entity.isEditUsernameAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        entity.setEditUsernameAllowed(editUsernameAllowed);
    }

    @Override
    public boolean isUserManagedAccessAllowed() {
        Boolean is = entity.isAllowUserManagedAccess();
        return is == null ? false : is;
    }

    @Override
    public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {
        entity.setAllowUserManagedAccess(userManagedAccessAllowed);
    }

    @Override
    public void setAttribute(String name, String value) {
        entity.setAttribute(name, Collections.singletonList(value));
    }

    @Override
    public void removeAttribute(String name) {
        entity.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        List<String> attribute = entity.getAttribute(name);
        if (attribute == null || attribute.isEmpty()) return null;
        return attribute.get(0);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, List<String>> attrs = entity.getAttributes();

        return attrs == null || attrs.isEmpty() ? Collections.emptyMap() : attrs.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                    entry -> {
                        if (entry.getValue().isEmpty()) {
                            return null;
                        } else if (entry.getValue().size() > 1) {
                            // This could be caused by an inconsistency in the storage, a programming error,
                            // or a downgrade from a future version of Keycloak that already supports multi-valued attributes.
                            // The caller will not see the other values, and when this entity is later updated, the additional values be will lost.
                            LOG.warnf("Realm '%s' has attribute '%s' with %d values, retrieving only the first", getId(), entry.getKey(),
                                    entry.getValue().size());
                        }
                        return entry.getValue().get(0);
                    })
        );
    }

    @Override
    public boolean isVerifyEmail() {
        Boolean is = entity.isVerifyEmail();
        return is == null ? false : is;
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        entity.setVerifyEmail(verifyEmail);
    }

    @Override
    public boolean isLoginWithEmailAllowed() {
        Boolean is = entity.isLoginWithEmailAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        entity.setLoginWithEmailAllowed(loginWithEmailAllowed);
    }

    @Override
    public boolean isDuplicateEmailsAllowed() {
        Boolean is = entity.isDuplicateEmailsAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        entity.setDuplicateEmailsAllowed(duplicateEmailsAllowed);
    }

    @Override
    public boolean isDuplicateMobNumAllowed() {
        Boolean is = entity.isDuplicateMobNumAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setDuplicateMobNumAllowed(boolean duplicateMobNumAllowed) {
        entity.setDuplicateEmailsAllowed(duplicateMobNumAllowed);
    }


    @Override
    public boolean isResetPasswordAllowed() {
        Boolean is = entity.isResetPasswordAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setUserInviteWay(String inviteWay) {

    }

    @Override
    public String getUserInviteWay() {
        return null;
    }

    @Override
    public void setUserInviteClientId(String clientId) {

    }

    @Override
    public String getUserInviteClientId() {
        return null;
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        entity.setResetPasswordAllowed(resetPasswordAllowed);
    }

    @Override
    public boolean isRevokeRefreshToken() {
        Boolean is = entity.isRevokeRefreshToken();
        return is == null ? false : is;
    }

    @Override
    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        entity.setRevokeRefreshToken(revokeRefreshToken);
    }

    @Override
    public int getRefreshTokenMaxReuse() {
        Integer i = entity.getRefreshTokenMaxReuse();
        return i == null ? 0 : i;
    }

    @Override
    public void setRefreshTokenMaxReuse(int revokeRefreshTokenCount) {
        entity.setRefreshTokenMaxReuse(revokeRefreshTokenCount);
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        Integer i = entity.getSsoSessionIdleTimeout();
        return i == null ? 0 : i;
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        entity.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        Integer i = entity.getSsoSessionMaxLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        entity.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getSsoSessionIdleTimeoutRememberMe() {
        Integer i = entity.getSsoSessionIdleTimeoutRememberMe();
        return i == null ? 0 : i;
    }

    @Override
    public void setSsoSessionIdleTimeoutRememberMe(int seconds) {
        entity.setSsoSessionIdleTimeoutRememberMe(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespanRememberMe() {
        Integer i = entity.getSsoSessionMaxLifespanRememberMe();
        return i == null ? 0 : i;
    }

    @Override
    public void setSsoSessionMaxLifespanRememberMe(int seconds) {
        entity.setSsoSessionMaxLifespanRememberMe(seconds);
    }

    @Override
    public int getOfflineSessionIdleTimeout() {
        Integer i = entity.getOfflineSessionIdleTimeout();
        return i == null ? 0 : i;
    }

    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        entity.setOfflineSessionIdleTimeout(seconds);
    }

    @Override
    public int getAccessTokenLifespan() {
        Integer i = entity.getAccessTokenLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public int getClientSessionIdleTimeout() {
        Integer i = entity.getClientSessionIdleTimeout();
        return i == null ? 0 : i;
    }

    @Override
    public void setClientSessionIdleTimeout(int seconds) {
        entity.setClientSessionIdleTimeout(seconds);
    }

    @Override
    public int getClientSessionMaxLifespan() {
        Integer i = entity.getClientSessionMaxLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setClientSessionMaxLifespan(int seconds) {
        entity.setClientSessionMaxLifespan(seconds);
    }

    @Override
    public int getClientOfflineSessionIdleTimeout() {
        Integer i = entity.getClientOfflineSessionIdleTimeout();
        return i == null ? 0 : i;
    }

    @Override
    public void setClientOfflineSessionIdleTimeout(int seconds) {
        entity.setClientOfflineSessionIdleTimeout(seconds);
    }

    @Override
    public int getClientOfflineSessionMaxLifespan() {
        Integer i = entity.getClientOfflineSessionMaxLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setClientOfflineSessionMaxLifespan(int seconds) {
        entity.setClientOfflineSessionMaxLifespan(seconds);
    }

    @Override
    public void setAccessTokenLifespan(int seconds) {
        entity.setAccessTokenLifespan(seconds);
    }

    @Override
    public int getAccessTokenLifespanForImplicitFlow() {
        Integer i = entity.getAccessTokenLifespanForImplicitFlow();
        return i == null ? 0 : i;
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        entity.setAccessTokenLifespanForImplicitFlow(seconds);
    }

    @Override
    public int getAccessCodeLifespan() {
        Integer i = entity.getAccessCodeLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setAccessCodeLifespan(int seconds) {
        entity.setAccessCodeLifespan(seconds);
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        Integer i = entity.getAccessCodeLifespanUserAction();
        return i == null ? 0 : i;
    }

    @Override
    public void setAccessCodeLifespanUserAction(int seconds) {
        entity.setAccessCodeLifespanUserAction(seconds);
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        Integer i = entity.getAccessCodeLifespanLogin();
        return i == null ? 0 : i;
    }

    @Override
    public void setAccessCodeLifespanLogin(int seconds) {
        entity.setAccessCodeLifespanLogin(seconds);
    }

    @Override
    public int getActionTokenGeneratedByAdminLifespan() {
        Integer i = entity.getActionTokenGeneratedByAdminLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setActionTokenGeneratedByAdminLifespan(int seconds) {
        entity.setActionTokenGeneratedByAdminLifespan(seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan() {
        return getAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN, getAccessCodeLifespanUserAction());
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(int seconds) {
        setAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN, seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan(String actionTokenType) {
        return getAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenType, getAccessCodeLifespanUserAction());
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(String actionTokenType, Integer seconds) {
        if (actionTokenType != null && ! actionTokenType.isEmpty() && seconds != null) {
            setAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenType, seconds);
        }
    }

    @Override
    public boolean isEnabledOwnEmailProvider() {
        return false;
    }

    @Override
    public void setOwnEmailProvider(boolean value) {

    }

    @Override
    public boolean isEnabledOwnWhatsappProvider() {
        return false;
    }

    @Override
    public boolean isDNSEnabled() {
        return false;
    }

    @Override
    public boolean isEnabledOwnSmsProvider() {
        return false;
    }

    @Override
    public void setOwnSmsProvider(boolean value) {

    }

    @Override
    public String getRealmLogo() {
        return null;
    }

    @Override
    public void setRealmLogo(String logoUrl) {

    }

    @Override
    public Map<String, Integer> getUserActionTokenLifespans() {
        Map<String, List<String>> attrs = entity.getAttributes();
        if (attrs == null || attrs.isEmpty()) return Collections.emptyMap();

        Map<String, Integer> tokenLifespans = attrs.entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> nonNull(entry.getValue()) && ! entry.getValue().isEmpty())
                .filter(entry -> entry.getKey().startsWith(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "."))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().substring(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN.length() + 1),
                        entry -> Integer.valueOf(entry.getValue().get(0))));

        return Collections.unmodifiableMap(tokenLifespans);
    }

    @Override
    public Stream<RequiredCredentialModel> getRequiredCredentialsStream() {
        Set<MapRequiredCredentialEntity> rCEs = entity.getRequiredCredentials();
        return rCEs == null ? Stream.empty() : rCEs.stream().map(MapRequiredCredentialEntity::toModel);
    }

    @Override
    public void addRequiredCredential(String cred) {
        RequiredCredentialModel model = RequiredCredentialModel.BUILT_IN.get(cred);
        if (model == null) {
            throw new RuntimeException("Unknown credential type " + cred);
        }
        entity.addRequiredCredential(MapRequiredCredentialEntity.fromModel(model));
    }

    @Override
    public void updateRequiredCredentials(Set<String> credentials) {
        Set<MapRequiredCredentialEntity> requiredCredentialEntities = entity.getRequiredCredentials();
        Consumer<MapRequiredCredentialEntity> updateCredentialFnc = e -> {
            Optional<MapRequiredCredentialEntity> existingEntity = requiredCredentialEntities.stream()
                    .filter(existing -> Objects.equals(e.getType(), existing.getType()))
                    .findFirst();

            if (existingEntity.isPresent()) {
                updateRequiredCredential(existingEntity.get(), e);
            } else {
                entity.addRequiredCredential(e);
            }
        };

        credentials.stream()
                .map(RequiredCredentialModel.BUILT_IN::get)
                .peek(c -> { if (c == null) throw new RuntimeException("Unknown credential type " + c.getType()); })
                .map(MapRequiredCredentialEntity::fromModel)
                .forEach(updateCredentialFnc);
    }

    private void updateRequiredCredential(MapRequiredCredentialEntity existing, MapRequiredCredentialEntity newValue) {
        existing.setFormLabel(newValue.getFormLabel());
        existing.setInput(newValue.isInput());
        existing.setSecret(newValue.isSecret());
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (passwordPolicy == null) {
            passwordPolicy = PasswordPolicy.parse(session, entity.getPasswordPolicy());
        }
        return passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        this.passwordPolicy = policy;
        entity.setPasswordPolicy(policy.toString());
    }

    @Override
    public OTPPolicy getOTPPolicy() {
        MapOTPPolicyEntity policy = entity.getOTPPolicy();
        return policy == null ? OTPPolicy.DEFAULT_POLICY : MapOTPPolicyEntity.toModel(policy);
    }

    @Override
    public void setOTPPolicy(OTPPolicy policy) {
        entity.setOTPPolicy(MapOTPPolicyEntity.fromModel(policy));
    }

    @Override
    public RoleModel getRoleById(String id) {
        return session.roles().getRoleById(this, id);
    }

    @Override
    public Stream<GroupModel> getDefaultGroupsStream() {
        Set<String> gIds = entity.getDefaultGroupIds();
        return gIds == null ? Stream.empty() : gIds.stream().map(this::getGroupById);
    }

    @Override
    public void addDefaultGroup(GroupModel group) {
        entity.addDefaultGroupId(group.getId());
    }

    @Override
    public void removeDefaultGroup(GroupModel group) {
        entity.removeDefaultGroupId(group.getId());
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
    public Long getClientsCount() {
        return session.clients().getClientsCount(this);
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream() {
        return session.clients().getAlwaysDisplayInConsoleClientsStream(this);
    }

    @Override
    public ClientModel addClient() {
        return null;
    }

    @Override
    public ClientModel addClientByClientId(String clientId) {
        return session.clients().addClient(this,clientId);
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

    @Override
    public Map<String, String> getSmtpConfig() {
        Map<String, String> sC = entity.getSmtpConfig();
        return sC == null ? Collections.emptyMap() : Collections.unmodifiableMap(sC);
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        entity.setSmtpConfig(smtpConfig);
    }

    @Override
    public void setSmsConfig(Map<String, String> smsConfig) {

    }

    @Override
    public Map<String, String> getSmsConfig() {
        return null;
    }

    @Override
    public Map<String, String> getSmsRealmConfig() {
        return null;
    }

    @Override
    public Map<String, String> getSmsExternalConfig() {
        return null;
    }

    @Override
    public AuthenticationFlowModel getBrowserFlow() {
        return getAuthenticationFlowById(entity.getBrowserFlow());
    }

    @Override
    public void setBrowserFlow(AuthenticationFlowModel flow) {
        entity.setBrowserFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getRegistrationFlow() {
        return getAuthenticationFlowById(entity.getRegistrationFlow());
    }

    @Override
    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        entity.setRegistrationFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getDirectGrantFlow() {
        return getAuthenticationFlowById(entity.getDirectGrantFlow());
    }

    @Override
    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        entity.setDirectGrantFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getResetCredentialsFlow() {
        return getAuthenticationFlowById(entity.getResetCredentialsFlow());
    }

    @Override
    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        entity.setResetCredentialsFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getClientAuthenticationFlow() {
        return getAuthenticationFlowById(entity.getClientAuthenticationFlow());
    }

    @Override
    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        entity.setClientAuthenticationFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        return getAuthenticationFlowById(entity.getDockerAuthenticationFlow());
    }

    @Override
    public void setDockerAuthenticationFlow(AuthenticationFlowModel flow) {
        entity.setDockerAuthenticationFlow(flow.getId());
    }

    @Override
    public Stream<AuthenticationFlowModel> getAuthenticationFlowsStream() {
        Set<MapAuthenticationFlowEntity> afs = entity.getAuthenticationFlows();
        return afs == null ? Stream.empty() : afs.stream().map(MapAuthenticationFlowEntity::toModel);
    }

    @Override
    public AuthenticationFlowModel getFlowByAlias(String alias) {
        Set<MapAuthenticationFlowEntity> afs = entity.getAuthenticationFlows();
        return afs == null ? null : afs.stream()
                .filter(flow -> Objects.equals(flow.getAlias(), alias))
                .findFirst()
                .map(MapAuthenticationFlowEntity::toModel)
                .orElse(null);
    }

    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        if (entity.getAuthenticationFlow(model.getId()).isPresent()) {
            throw new ModelDuplicateException("An AuthenticationFlow with given id already exists");
        }

        MapAuthenticationFlowEntity authenticationFlowEntity = MapAuthenticationFlowEntity.fromModel(model);
        entity.addAuthenticationFlow(authenticationFlowEntity);

        return MapAuthenticationFlowEntity.toModel(authenticationFlowEntity);
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String flowId) {
        if (flowId == null) return null;
        return entity.getAuthenticationFlow(flowId).map(MapAuthenticationFlowEntity::toModel).orElse(null);
    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model, boolean force) {

    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        entity.getAuthenticationFlow(model.getId())
                .ifPresent(existing -> {
                    existing.setAlias(model.getAlias());
                    existing.setDescription(model.getDescription());
                    existing.setProviderId(model.getProviderId());
                    existing.setBuiltIn(model.isBuiltIn());
                    existing.setTopLevel(model.isTopLevel());
                });
    }

    @Override
    public Stream<AuthenticationExecutionModel> getAuthenticationExecutionsStream(String flowId) {
        Set<MapAuthenticationExecutionEntity> aee = entity.getAuthenticationExecutions();
        return aee == null ? Stream.empty() : aee.stream()
                .filter(execution -> Objects.equals(flowId, execution.getParentFlowId()))
                .map(MapAuthenticationExecutionEntity::toModel)
                .sorted(AuthenticationExecutionModel.ExecutionComparator.SINGLETON);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        if (id == null) return null;
        return entity.getAuthenticationExecution(id).map(MapAuthenticationExecutionEntity::toModel).orElse(null);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        Set<MapAuthenticationExecutionEntity> aee = entity.getAuthenticationExecutions();
        return aee == null ? null : aee.stream()
                .filter(execution -> Objects.equals(flowId, execution.getFlowId()))
                .findAny()
                .map(MapAuthenticationExecutionEntity::toModel)
                .orElse(null);
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        if (entity.getAuthenticationExecution(model.getId()).isPresent()) {
            throw new ModelDuplicateException("An RequiredActionProvider with given id already exists");
        }
        MapAuthenticationExecutionEntity executionEntity = MapAuthenticationExecutionEntity.fromModel(model);
        entity.addAuthenticationExecution(executionEntity);
        return MapAuthenticationExecutionEntity.toModel(executionEntity);
    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        entity.getAuthenticationExecution(model.getId())
                .ifPresent(existing -> {
                    existing.setAuthenticator(model.getAuthenticator());
                    existing.setAuthenticatorConfig(model.getAuthenticatorConfig());
                    existing.setFlowId(model.getFlowId());
                    existing.setParentFlowId(model.getParentFlow());
                    existing.setRequirement(model.getRequirement());
                    existing.setAutheticatorFlow(model.isAuthenticatorFlow());
                    existing.setPriority(model.getPriority());
                });
    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        entity.removeAuthenticationExecution(model.getId());
    }

    @Override
    public Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream() {
        Set<MapAuthenticatorConfigEntity> acs = entity.getAuthenticatorConfigs();
        return acs == null ? Stream.empty() : acs.stream().map(MapAuthenticatorConfigEntity::toModel);
    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        MapAuthenticatorConfigEntity authenticatorConfig = MapAuthenticatorConfigEntity.fromModel(model);
        entity.addAuthenticatorConfig(authenticatorConfig);
        model.setId(authenticatorConfig.getId());
        return model;
    }

    @Override
    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        entity.getAuthenticatorConfig(model.getId())
                        .ifPresent(oldAC -> {
                            oldAC.setAlias(model.getAlias());
                            oldAC.setConfig(model.getConfig());
                        });
    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        entity.removeAuthenticatorConfig(model.getId());
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        if (id == null) return null;
        return entity.getAuthenticatorConfig(id).map(MapAuthenticatorConfigEntity::toModel).orElse(null);
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
        Set<MapAuthenticatorConfigEntity> acs = entity.getAuthenticatorConfigs();
        return acs == null ? null : acs.stream()
                .filter(config -> Objects.equals(config.getAlias(), alias))
                .findFirst()
                .map(MapAuthenticatorConfigEntity::toModel)
                .orElse(null);
    }

    @Override
    public Stream<EventConfigModel> getEventConfigsStream() {
        return null;
    }

    @Override
    public EventConfigModel addEventConfig(EventConfigModel model) {
        return null;
    }

    @Override
    public void updateEventConfig(EventConfigModel model) {

    }

    @Override
    public void removeEventConfig(EventConfigModel model) {

    }

    @Override
    public EventConfigModel getEventConfigByProviderId(String id) {
        return null;
    }

    @Override
    public Stream<MarketplaceConfigModel> getMarketplaceConfigsStream() {
        return null;
    }

    @Override
    public MarketplaceConfigModel addMarketplaceConfig(MarketplaceConfigModel model) {
        return null;
    }

    @Override
    public void updateMarketplaceConfig(MarketplaceConfigModel model) {

    }

    @Override
    public void removeMarketplaceConfig(MarketplaceConfigModel model) {

    }

    @Override
    public Stream<MarketplaceConfigModel> getMarketplaceConfigByProviderType(String type) {
        return null;
    }

    @Override
    public Stream<SmsConfigModel> getSmsConfigsStream() {
        return null;
    }

    @Override
    public SmsConfigModel addSmsConfig(SmsConfigModel model) {
        return null;
    }

    @Override
    public void updateSmsConfig(SmsConfigModel model) {

    }

    @Override
    public void removeSmsConfig(SmsConfigModel model) {

    }

    @Override
    public SmsConfigModel getSmsConfigByProviderId(String id) {
        return null;
    }

    @Override
    public String getSubscription() {
        return null;
    }

    @Override
    public void addSubscription(String model) {

    }

    @Override
    public Map<String, String> getSubscriptionUsage() {
        return null;
    }

    @Override
    public void updateSubscription(Map<String, String> model) {

    }

    @Override
    public Stream<ReportModel> getReports() {
        return null;
    }

    @Override
    public ReportModel addReport(ReportModel model) {
        return null;
    }

    @Override
    public ReportModel getReportById(String id) {
        return null;
    }

    @Override
    public void updateReport(ReportModel model) {

    }

    @Override
    public Stream<MgmtAccessModel> getMgmtAccessKeys() {
        return null;
    }

    @Override
    public MgmtAccessModel addMgmtAccess(MgmtAccessModel model) {
        return null;
    }

    @Override
    public MgmtAccessModel getMgmtAccessById(String id) {
        return null;
    }

    @Override
    public MgmtAccessModel updateMgmtAccess(MgmtAccessModel model) {
        return null;
    }

    @Override
    public MgmtAccessModel rotateMgmtAccess(MgmtAccessModel model) {
        return null;
    }


    @Override
    public void removeMgmtAccess(String id) {
        return;
    }

    @Override
    public Stream<EmailConfigModel> getEmailConfigsStream() {
        return null;
    }

    @Override
    public EmailConfigModel addEmailConfig(EmailConfigModel model) {
        return null;
    }

    @Override
    public void updateEmailConfig(EmailConfigModel model) {

    }

    @Override
    public void removeEmailConfig(EmailConfigModel model) {

    }

    @Override
    public EmailConfigModel getEmailConfigByProviderId(String id) {
        return null;
    }

    @Override
    public Stream<MFAConfigModel> getMFAConfigsStream() {
        return null;
    }

    @Override
    public MFAConfigModel addMFAConfig(MFAConfigModel model) {
        return null;
    }

    @Override
    public void updateMFAConfig(MFAConfigModel model) {

    }

    @Override
    public void removeMFAConfig(MFAConfigModel model) {

    }

    @Override
    public MFAConfigModel getMFAConfigByProviderId(String id) {
        return null;
    }

    @Override
    public Stream<CibaAuthConfigModel> getCibaConfigsStream() {
        return null;
    }

    @Override
    public CibaAuthConfigModel addCibaAuthConfig(CibaAuthConfigModel model) {
        return null;
    }

    @Override
    public void updateCibaAuthConfig(CibaAuthConfigModel model) {

    }

    @Override
    public void removeCibaAuthConfig(CibaAuthConfigModel model) {

    }

    @Override
    public CibaAuthConfigModel getCibaAuthConfigByProviderId(String id, String clientId, String realmId) {
        return null;
    }

    @Override
    public CibaAuthConfigModel getCibaAuthConfigByClientId(String clientId, String realmId) {
        return null;
    }

    @Override
    public Stream<UserSyncConfigModel> getUserSyncConfigsStream() {
        return null;
    }

    @Override
    public UserSyncConfigModel addUserSyncConfigModel(UserSyncConfigModel model) {
        return null;
    }

    @Override
    public void updateUserSyncConfigModel(UserSyncConfigModel model) {
       
    }

    @Override
    public void removeUserSyncConfigModel(UserSyncConfigModel model) {
         
    }

    @Override
    public UserSyncConfigModel getUserSyncConfigByProviderId(String id) {
        return null;
    }


    @Override
    public Stream<CibaNotificationConfigModel> getCibaNotificationConfigsStream() {
        return null;
    }

    @Override
    public CibaNotificationConfigModel addCibaNotificationConfig(CibaNotificationConfigModel model) {
        return null;
    }

    @Override
    public void updateCibaNotificationConfig(CibaNotificationConfigModel model) {

    }

    @Override
    public void removeCibaNotificationConfig(CibaNotificationConfigModel model) {

    }

    @Override
    public CibaNotificationConfigModel getCibaNotificationConfigByProviderId(String id, String clientId, String realmId) {
        return null;
    }

    @Override
    public CibaNotificationConfigModel getCibaNotificationConfigByClientId(String clientId, String realmId) {
        return null;
    }

    @Override
    public Stream<CibaDeadLetterHookConfigModel> getCibaDeadLetterHookNotificationConfigsStream() {
        return null;
    }

    @Override
    public CibaDeadLetterHookConfigModel addCibaResultHookNotificationConfig(CibaDeadLetterHookConfigModel model) {
        return null;
    }

    @Override
    public void updateCibaResultHookNotificationConfig(CibaDeadLetterHookConfigModel model) {

    }

    @Override
    public void removeDeadLetterHookNotificationConfig(CibaDeadLetterHookConfigModel model) {

    }

    @Override
    public CibaDeadLetterHookConfigModel getDeadLetterHookNotificationConfigByProviderId(String id, String clientId, String realmId) {
        return null;
    }

    @Override
    public CibaDeadLetterHookConfigModel getDeadLetterHookNotificationConfigByClientId(String clientId, String realmId) {
        return null;
    }


    @Override
    public CibaStateModel addCibaState(CibaStateModel model) {
        return null;
    }

    @Override
    public void removeCibaState(CibaStateModel model) {

    }

    @Override
    public CibaStateModel getCibaStateById(String cibaStateId) {
        return null;
    }

    @Override
    public TrxModel addAttribute(String trxId, String name, String value) {
        return null;
    }

    @Override
    public void addTrxAttributes(String trxId, Map<String, String> claims) {
        //
    }

    @Override
    public void removeTrxAttributes(String trxId, List<String> claimsToBeRemoved) {
        //
    }


    @Override
    public UserTransactionModel addTransactionStatus(String trxId, String userId, String status, Long expiresAt) {
        return null;
    }

    @Override
    public UserTransactionModel getUserTransactionDetails(String trxId, String userId) {
        return null;
    }
    @Override
    public List<ProviderModel> getProviders(String realmId, Integer firstResult, Integer max, String search) {
        return null;
    }

    @Override
    public List<String> getClaims(String verification, String provider) {
        return null;
    }

    @Override
    public List<VerifierModel> getVerifiers(String realmId, Integer firstResult, Integer max, String search, String sortBy) {
        return null;
    }

    @Override
    public List<APIPriorityModel> getVerifierProviderDetails(String realmId, Integer firstResult, Integer max, String search, String category, String sortBy) {
        return null;
    }

    @Override
    public List<String> getVerifierCategories(String realmId) {
        return null;
    }

    @Override
    public List<String> getAllVerifiersName(String verifierName) {
        return null;
    }

    @Override
    public APIPriorityModel getVerifierAPIDetails(String verifierName, String realmId) {
        return null;
    }

    @Override
    public APIVerifierConfigModel getVerifierConfigs(String verifierName, String providerName) {
        return null;
    }

    @Override
    public List<APIVerifierConfigModel> getAPIVerifierConfigModels() {
        return null;
    }

    @Override
    public void updateTransactionStatus(String trxId, String userId, String status) {

    }

    @Override
    public List<UserTransactionModel> getUserTransactionStatus(String userId) {

        return null;
    }

    @Override
    public void removeUserTransactionStatus(String userId) {

    }


    @Override
    public void removeTrxAttribute(String trxId, String name) {

    }

    @Override
    public List<TermsAndConditionsModel> getTermsAndConditionsByVersion(String alias, String version) {
        return null;
    }

    @Override
    public List<TermsAndConditionsModel> getTermsAndConditionsVersions(String alias) {
        return null;
    }

    @Override
    public TermsAndConditionsModel addTermsAndConditions(String alias, String realmId, String id, String version, String type, String content) {
        return null;
    }

    @Override
    public void removeTermsAndConditionsVersions(String alias) {

    }

    @Override
    public void removeTermsAndConditionsVersionByVersion(String alias, String version) {

    }

    @Override
    public void updateTermsAndConditionsVersion(String id, String type, String content, String alias, String version) {

    }

    @Override
    public List<TermsAndConditionsModel> getTermsAndConditions(String realmId) {
        return null;
    }

    @Override
    public void removeTrx(String trxId) {

    }

    @Override
    public TrxModel getTrx(String trxId) {
        return null;
    }

    @Override
    public String getTrxValue(String trxId, String name)
    {
        return null;
    }

    @Override
    public boolean hasTrxValueAttribute(String trxId, String name) {
        return false;
    }

    @Override
    public boolean hasTrxValueAttribute(String trxId, String name,String expectedValue) {
        return false;
    }

    @Override
    public WebDeviceModel addWebDevice(WebDeviceModel model) {
        return null;
    }

    @Override
    public void removeWebDevice(WebDeviceModel model) {

    }

    @Override
    public WebDeviceModel getWebDeviceById(String deviceId) {
        return null;
    }

    @Override
    public Stream<ActionModel> getActionStream() {
        return null;
    }

    @Override
    public Stream<EztovrfyMarketplaceTemplateModel> getEztovrfyMarketplaceTemplateStream() {
        return null;
    }

    @Override
    public List<EztovrfyMarketplaceTemplateModel> searchEztovrfyMarketplaceTemplateParams(String workflowName, String templateTag) {
        return null;
    }

    @Override
    public ActionModel addAction(ActionModel model) {
        return null;
    }

    @Override
    public EztovrfyMarketplaceTemplateModel addEztovrfyMarketplaceTemplate(EztovrfyMarketplaceTemplateModel model) {
        return null;
    }

    @Override
    public boolean removeEztovrfyMarketplaceTemplate(EztovrfyMarketplaceTemplateModel model) {
        return false;
    }

    @Override
    public Stream<EztovrfyMarketplaceCategoryModel> getEztovrfyMarketplaceCategoriesStream() {
        return null;
    }

    @Override
    public EztovrfyMarketplaceCategoryModel addEztovrfyMarketplaceCategory(EztovrfyMarketplaceCategoryModel model) {
        return null;
    }

    @Override
    public boolean removeEztovrfyMarketplaceCategory(EztovrfyMarketplaceCategoryModel model) {
        return false;
    }

    @Override
    public void updateAction(ActionModel model) {

    }

    @Override
    public void removeAction(ActionModel model) {

    }

    @Override
    public ActionModel getActionById(String id) {
        return null;
    }

    @Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream(String actionId) {
        return null;
    }

    @Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream() {
        return null;
    }

    @Override
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        MapRequiredActionProviderEntity requiredActionProvider = MapRequiredActionProviderEntity.fromModel(model);
        entity.addRequiredActionProvider(requiredActionProvider);

        return MapRequiredActionProviderEntity.toModel(requiredActionProvider);
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        entity.getRequiredActionProvider(model.getId())
                        .ifPresent(oldRAP -> {
                            oldRAP.setAlias(model.getAlias());
                            oldRAP.setName(model.getName());
                            oldRAP.setProviderId(model.getProviderId());
                            oldRAP.setPriority(model.getPriority());
                            oldRAP.setEnabled(model.isEnabled());
                            oldRAP.setDefaultAction(model.isDefaultAction());
                            oldRAP.setConfig(model.getConfig());
                        });
    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        entity.removeRequiredActionProvider(model.getId());
    }

    @Override
    public void removeRequiredActionProviders(String actionId) {

    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        if (id == null) return null;

        return entity.getRequiredActionProvider(id).map(MapRequiredActionProviderEntity::toModel).orElse(null);
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        Set<MapRequiredActionProviderEntity> raps = entity.getRequiredActionProviders();
        return raps == null ? null : raps.stream()
                .filter(actionProvider -> Objects.equals(actionProvider.getAlias(), alias))
                .findFirst()
                .map(MapRequiredActionProviderEntity::toModel)
                .orElse(null);
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProvidersStream() {
        Set<MapIdentityProviderEntity> ips = entity.getIdentityProviders();
        return ips == null ? Stream.empty() : ips.stream().map(MapIdentityProviderEntity::toModel);
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        Set<MapIdentityProviderEntity> ips = entity.getIdentityProviders();
        return ips == null ? null : ips.stream()
                .filter(identityProvider -> Objects.equals(identityProvider.getAlias(), alias))
                .findFirst()
                .map(MapIdentityProviderEntity::toModel)
                .orElse(null);
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel model) {
        entity.addIdentityProvider(MapIdentityProviderEntity.fromModel(model));
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        IdentityProviderModel model = getIdentityProviderByAlias(alias);
        entity.removeIdentityProvider(model.getInternalId());

        // TODO: Sending an event should be extracted to store layer
        session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderRemovedEvent() {

            @Override
            public RealmModel getRealm() {
                return MapRealmAdapter.this;
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
        // TODO: ^^^^^^^ Up to here
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        Set<MapIdentityProviderEntity> ips = entity.getIdentityProviders();
        if (ips != null) {
            ips.stream()
                    .filter(ip -> Objects.equals(ip.getId(), identityProvider.getInternalId()))
                    .findFirst()
                    .ifPresent(oldPS -> {
                        oldPS.setAlias(identityProvider.getAlias());
                        oldPS.setDisplayName(identityProvider.getDisplayName());
                        oldPS.setProviderId(identityProvider.getProviderId());
                        oldPS.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
                        oldPS.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
                        oldPS.setEnabled(identityProvider.isEnabled());
                        oldPS.setTrustEmail(identityProvider.isTrustEmail());
                        oldPS.setStoreToken(identityProvider.isStoreToken());
                        oldPS.setLinkOnly(identityProvider.isLinkOnly());
                        oldPS.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
                        oldPS.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
                        oldPS.setConfig(identityProvider.getConfig() == null ? null : new HashMap<>(identityProvider.getConfig()));
                    });

            // TODO: Sending an event should be extracted to store layer
            session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderUpdatedEvent() {

                @Override
                public RealmModel getRealm() {
                    return MapRealmAdapter.this;
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
            // TODO: ^^^^^^^ Up to here
        }
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream() {
        Set<MapIdentityProviderMapperEntity> ipms = entity.getIdentityProviderMappers();
        return ipms == null ? Stream.empty() : ipms.stream().map(MapIdentityProviderMapperEntity::toModel);
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias) {
        Set<MapIdentityProviderMapperEntity> ipms = entity.getIdentityProviderMappers();
        return ipms == null ? Stream.empty() : ipms.stream()
                .filter(mapper -> Objects.equals(mapper.getIdentityProviderAlias(), brokerAlias))
                .map(MapIdentityProviderMapperEntity::toModel);
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        MapIdentityProviderMapperEntity identityProviderMapper = MapIdentityProviderMapperEntity.fromModel(model);

        if (entity.getIdentityProviderMapper(model.getId()).isPresent()) {
            throw new ModelDuplicateException("An IdentityProviderMapper with given id already exists");
        }

        entity.addIdentityProviderMapper(identityProviderMapper);

        return MapIdentityProviderMapperEntity.toModel(identityProviderMapper);
    }

    @Override
    public void removeIdentityProviderMapper(IdentityProviderMapperModel model) {
        entity.removeIdentityProviderMapper(model.getId());
    }

    @Override
    public void updateIdentityProviderMapper(IdentityProviderMapperModel model) {
        entity.getIdentityProviderMapper(model.getId())
                        .ifPresent(oldIPM -> {
                            oldIPM.setName(model.getName());
                            oldIPM.setIdentityProviderAlias(model.getIdentityProviderAlias());
                            oldIPM.setIdentityProviderMapper(model.getIdentityProviderMapper());
                            oldIPM.setConfig(model.getConfig());
                        });
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        if (id == null) return null;
        return entity.getIdentityProviderMapper(id).map(MapIdentityProviderMapperEntity::toModel).orElse(null);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String brokerAlias, String name) {
        Set<MapIdentityProviderMapperEntity> ipms = entity.getIdentityProviderMappers();
        return ipms == null ? null : ipms.stream()
                .filter(identityProviderMapper -> Objects.equals(identityProviderMapper.getIdentityProviderAlias(), brokerAlias)
                        && Objects.equals(identityProviderMapper.getName(), name))
                .findFirst()
                .map(MapIdentityProviderMapperEntity::toModel)
                .orElse(null);
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        model = importComponentModel(model);
        ComponentUtil.notifyCreated(session, this, model);
        return model;
    }

    /**
     * Copied from jpa RealmAdapter: This just exists for testing purposes
     */
    private static final String COMPONENT_PROVIDER_EXISTS_DISABLED = "component.provider.exists.disabled";

    @Override
    public ComponentModel importComponentModel(ComponentModel model) {
        try {
            ComponentFactory componentFactory = ComponentUtil.getComponentFactory(session, model);
            if (componentFactory == null && System.getProperty(COMPONENT_PROVIDER_EXISTS_DISABLED) == null) {
                throw new IllegalArgumentException("Invalid component type");
            }
            componentFactory.validateConfiguration(session, this, model);
        } catch (IllegalArgumentException | ComponentValidationException e) {
            if (System.getProperty(COMPONENT_PROVIDER_EXISTS_DISABLED) == null) {
                throw e;
            }
        }

        if (entity.getComponent(model.getId()).isPresent()) {
            throw new ModelDuplicateException("A Component with given id already exists");
        }

        MapComponentEntity component = MapComponentEntity.fromModel(model);
        if (model.getParentId() == null) {
            component.setParentId(getId());
        }
        entity.addComponent(component);

        return MapComponentEntity.toModel(component);
    }

    @Override
    public void updateComponent(ComponentModel component) {
        ComponentUtil.getComponentFactory(session, component).validateConfiguration(session, this, component);
        entity.getComponent(component.getId())
                        .ifPresent(existing -> {
                            ComponentModel oldModel = MapComponentEntity.toModel(existing);
                            updateComponent(existing, component);
                            ComponentUtil.notifyUpdated(session, this, oldModel, component);
                        });
    }

    private static void updateComponent(MapComponentEntity oldValue, ComponentModel newValue) {
        oldValue.setName(newValue.getName());
        oldValue.setProviderId(newValue.getProviderId());
        oldValue.setProviderType(newValue.getProviderType());
        oldValue.setSubType(newValue.getSubType());
        oldValue.setParentId(newValue.getParentId());
        oldValue.setConfig(newValue.getConfig());
    }

    @Override
    public void removeComponent(ComponentModel component) {
        if (!entity.getComponent(component.getId()).isPresent()) return;

        session.users().preRemove(this, component);
        ComponentUtil.notifyPreRemove(session, this, component);
        removeComponents(component.getId());
        entity.removeComponent(component.getId());
    }

    @Override
    public void removeComponents(String parentId) {
        Set<MapComponentEntity> components = entity.getComponents();
        if (components == null || components.isEmpty()) return;
        components.stream()
            .filter(c -> Objects.equals(parentId, c.getParentId()))
            .map(MapComponentEntity::toModel)
            .collect(Collectors.toSet())  // This is necessary to read out all the components before removing them
            .forEach(c -> {
                session.users().preRemove(this, c);
                ComponentUtil.notifyPreRemove(session, this, c);
                entity.removeComponent(c.getId());
            });
    }

    @Override
    public Stream<ComponentModel> getComponentsStream() {
        Set<MapComponentEntity> components = entity.getComponents();
        return components == null ? Stream.empty() : components.stream().map(MapComponentEntity::toModel);
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId) {
        Set<MapComponentEntity> components = entity.getComponents();
        return components == null ? Stream.empty() : components.stream()
                .filter(c -> Objects.equals(parentId, c.getParentId()))
                .map(MapComponentEntity::toModel);
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId, String providerType) {
        Set<MapComponentEntity> components = entity.getComponents();
        return components == null ? Stream.empty() : components.stream()
                .filter(c -> Objects.equals(parentId, c.getParentId()))
                .filter(c -> Objects.equals(providerType, c.getProviderType()))
                .map(MapComponentEntity::toModel);
    }

    @Override
    public ComponentModel getComponent(String id) {
        return entity.getComponent(id).map(MapComponentEntity::toModel).orElse(null);
    }

    @Override
    public String getLoginTheme() {
        return entity.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        entity.setLoginTheme(name);
    }

    @Override
    public String getAccountTheme() {
        return entity.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        entity.setAccountTheme(name);
    }

    @Override
    public String getAdminTheme() {
        return entity.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        entity.setAdminTheme(name);
    }

    @Override
    public String getEmailTheme() {
        return entity.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        entity.setEmailTheme(name);
    }

    @Override
    public String getSSOTheme() {
        return null;
    }

    @Override
    public void setSSOTheme(String name) {

    }

    @Override
    public String getMode() {
        return null;
    }

    @Override
    public void setMode(String mode) {

    }

    @Override
    public int getNotBefore() {
        Long notBefore = entity.getNotBefore();
        return notBefore == null ? 0 : TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(notBefore);
    }

    @Override
    public void setNotBefore(int notBefore) {
        entity.setNotBefore(TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(notBefore));
    }

    @Override
    public boolean isEventsEnabled() {
        Boolean is = entity.isEventsEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        entity.setEventsEnabled(enabled);
    }

    @Override
    public long getEventsExpiration() {
        Long i = entity.getEventsExpiration();
        return i == null ? 0 : i;
    }

    @Override
    public void setEventsExpiration(long expiration) {
        entity.setEventsExpiration(expiration);
    }

    @Override
    public Stream<String> getEventsListenersStream() {
        Set<String> eLs = entity.getEventsListeners();
        return eLs == null ? Stream.empty() : eLs.stream();
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        entity.setEventsListeners(listeners);
    }

    @Override
    public Stream<String> getEmailSenderStream() {
        return null;
    }

    @Override
    public void setEmailSenders(Set<String> listeners) {

    }

    @Override
    public Stream<String> getSmsSenderStream() {
        return null;
    }

    @Override
    public void setSmsSenders(Set<String> listeners) {

    }

    @Override
    public Stream<String> getMFAProvidersStream() {
        return null;
    }

    @Override
    public void setMFAProviders(Set<String> listeners) {

    }

    @Override
    public Stream<String> getEnabledEventTypesStream() {
        Set<String> eETs = entity.getEnabledEventTypes();
        return eETs == null ? Stream.empty() : eETs.stream();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        entity.setEnabledEventTypes(enabledEventTypes);
    }

    @Override
    public boolean isAdminEventsEnabled() {
        Boolean is = entity.isAdminEventsEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        entity.setAdminEventsEnabled(enabled);
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        Boolean is = entity.isAdminEventsDetailsEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        entity.setAdminEventsDetailsEnabled(enabled);
    }

    @Override
    public ClientModel getMasterAdminClient() {
        String masterAdminClientId = entity.getMasterAdminClient();
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
        String id = client == null ? null : client.getId();
        entity.setMasterAdminClient(id);
    }

    @Override
    public RoleModel getDefaultRole() {
        return session.roles().getRoleById(this, entity.getDefaultRoleId());
    }

    @Override
    public void setDefaultRole(RoleModel role) {
        entity.setDefaultRoleId(role.getId());
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        Set<MapIdentityProviderEntity> ips = entity.getIdentityProviders();
        return ips != null && ips.stream().findAny().isPresent();
    }

    @Override
    public boolean isInternationalizationEnabled() {
        Boolean is = entity.isInternationalizationEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        entity.setInternationalizationEnabled(enabled);
    }

    @Override
    public String getDefaultLocale() {
        return entity.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        entity.setDefaultLocale(locale);
    }

    @Override
    public GroupModel createGroup(String id, String name, GroupModel toParent) {
        return session.groups().createGroup(this, id, name, toParent);
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
    public void moveGroup(GroupModel group, GroupModel toParent) {
        session.groups().moveGroup(this, group, toParent);
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
        if (defaultScope) {
            entity.addDefaultClientScopeId(clientScope.getId());
        } else {
            entity.addOptionalClientScopeId(clientScope.getId());
        }
    }

    @Override
    public void removeDefaultClientScope(ClientScopeModel clientScope) {
        Boolean removedDefault = entity.removeDefaultClientScopeId(clientScope.getId());
        if (removedDefault == null || !removedDefault) {
            entity.removeOptionalClientScopeId(clientScope.getId());
        }
    }

    @Override
    public Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope) {
        Set<String> csIds = defaultScope ? entity.getDefaultClientScopeIds() : entity.getOptionalClientScopeIds();
        return csIds == null ? Stream.empty() : csIds.stream().map(this::getClientScopeById);
    }

    @Override
    public void createOrUpdateRealmLocalizationTexts(String locale, Map<String, String> localizationTexts) {
        Map<String, Map<String, String>> realmLocalizationTexts = entity.getLocalizationTexts();

        if (realmLocalizationTexts != null && realmLocalizationTexts.containsKey(locale)) {
            Map<String, String> currentTexts = new HashMap<>(realmLocalizationTexts.get(locale));
            currentTexts.putAll(localizationTexts);
            entity.setLocalizationText(locale, currentTexts);
        } else {
            entity.setLocalizationText(locale, localizationTexts);
        }
    }

    @Override
    public boolean removeRealmLocalizationTexts(String locale) {
        if (locale == null) return false;
        return entity.removeLocalizationText(locale);
    }

    @Override
    public Map<String, Map<String, String>> getRealmLocalizationTexts() {
        Map<String, Map<String, String>> localizationTexts = entity.getLocalizationTexts();
        return localizationTexts == null ? Collections.emptyMap() : localizationTexts;
    }

    @Override
    public Map<String, String> getRealmLocalizationTextsByLocale(String locale) {
        Map<String, String> lT = entity.getLocalizationText(locale);
        return lT == null ? Collections.emptyMap() : lT;
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
    public Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        return session.roles().getRealmRolesStream(this, firstResult, maxResults);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return session.roles().searchForRolesStream(this, search, first, max);
    }

    @Override
    @Deprecated
    public Stream<String> getDefaultRolesStream() {
        return getDefaultRole().getCompositesStream().filter(this::isRealmRole).map(RoleModel::getName);
    }

    private boolean isRealmRole(RoleModel role) {
        return ! role.isClientRole();
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
    public boolean isBruteForceProtected() {
        return getAttribute(BRUTE_FORCE_PROTECTED, false);
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        setAttribute(BRUTE_FORCE_PROTECTED, value);
    }

    @Override
    public boolean isPermanentLockout() {
        return getAttribute(PERMANENT_LOCKOUT, false);
    }

    @Override
    public void setPermanentLockout(final boolean val) {
        setAttribute(PERMANENT_LOCKOUT, val);
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return getAttribute(MAX_FAILURE_WAIT_SECONDS, 0);
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        setAttribute(MAX_FAILURE_WAIT_SECONDS, val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        return getAttribute(WAIT_INCREMENT_SECONDS, 0);
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        setAttribute(WAIT_INCREMENT_SECONDS, val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return getAttribute(MINIMUM_QUICK_LOGIN_WAIT_SECONDS, 0);
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        setAttribute(MINIMUM_QUICK_LOGIN_WAIT_SECONDS, val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return getAttribute(QUICK_LOGIN_CHECK_MILLISECONDS, 0L);
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        setAttribute(QUICK_LOGIN_CHECK_MILLISECONDS, val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        return getAttribute(MAX_DELTA_SECONDS, 0);
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        setAttribute(MAX_DELTA_SECONDS, val);
    }

    @Override
    public int getFailureFactor() {
        return getAttribute(FAILURE_FACTOR, 0);
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        setAttribute(FAILURE_FACTOR, failureFactor);
    }

    @Override
    public String getDefaultRegion() {
        return null;
    }

    @Override
    public void setDefaultRegion(String region) {

    }

    @Override
    public String getDefaultSignatureAlgorithm() {
        return getAttribute(DEFAULT_SIGNATURE_ALGORITHM);
    }

    @Override
    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        setAttribute(DEFAULT_SIGNATURE_ALGORITHM, defaultSignatureAlgorithm);
    }

    @Override
    public boolean isOfflineSessionMaxLifespanEnabled() {
        Boolean is = entity.isOfflineSessionMaxLifespanEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        entity.setOfflineSessionMaxLifespanEnabled(offlineSessionMaxLifespanEnabled);
    }

    @Override
    public int getOfflineSessionMaxLifespan() {
        Integer i = entity.getOfflineSessionMaxLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setOfflineSessionMaxLifespan(int seconds) {
        entity.setOfflineSessionMaxLifespan(seconds);
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicy() {
        MapWebAuthnPolicyEntity policy = entity.getWebAuthnPolicy();
        if (policy == null) policy = MapWebAuthnPolicyEntity.defaultWebAuthnPolicy();
        return MapWebAuthnPolicyEntity.toModel(policy);
    }

    @Override
    public void setWebAuthnPolicy(WebAuthnPolicy policy) {
        entity.setWebAuthnPolicy(MapWebAuthnPolicyEntity.fromModel(policy));
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicyPasswordless() {
        MapWebAuthnPolicyEntity policy = entity.getWebAuthnPolicyPasswordless();
        if (policy == null) policy = MapWebAuthnPolicyEntity.defaultWebAuthnPolicy();
        return MapWebAuthnPolicyEntity.toModel(policy);
    }

    @Override
    public void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy) {
        entity.setWebAuthnPolicyPasswordless(MapWebAuthnPolicyEntity.fromModel(policy));
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        Map<String, String> bSH = entity.getBrowserSecurityHeaders();
        return bSH == null ? Collections.emptyMap() : Collections.unmodifiableMap(bSH);
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        entity.setBrowserSecurityHeaders(headers);
    }

    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(int expiration, int count) {
        MapClientInitialAccessEntity clientInitialAccess = MapClientInitialAccessEntity.createEntity(expiration, count);
        entity.addClientInitialAccess(clientInitialAccess);
        return MapClientInitialAccessEntity.toModel(clientInitialAccess);
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(String id) {
        return entity.getClientInitialAccess(id).map(MapClientInitialAccessEntity::toModel).orElse(null);
    }

    @Override
    public void removeClientInitialAccessModel(String id) {
        entity.removeClientInitialAccess(id);
    }

    @Override
    public Stream<ClientInitialAccessModel> getClientInitialAccesses() {
        Set<MapClientInitialAccessEntity> cias = entity.getClientInitialAccesses();
        return cias == null ? Stream.empty() : cias.stream().map(MapClientInitialAccessEntity::toModel);
    }

    @Override
    public void decreaseRemainingCount(ClientInitialAccessModel model) {
        entity.getClientInitialAccess(model.getId())
                        .ifPresent(cia -> cia.setRemainingCount(model.getRemainingCount() - 1));
    }

    @Override
    public OAuth2DeviceConfig getOAuth2DeviceConfig() {
        return new OAuth2DeviceConfig(this);
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }

    public CibaConfig getCibaPolicy() {
        return new CibaConfig(this);
    }

    public ParConfig getParPolicy() {
        return new ParConfig(this);
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        return null;
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return null;
    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String binding) {

    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {

    }

    @Override
    public Boolean getApplicationConnections(String connId) {
        return null;
    }

    @Override
    public Map<String, Boolean> getApplicationsConnections() {
        return null;
    }

    @Override
    public void removeApplicationConnections(String connId) {

    }

    @Override
    public void setApplicationConnections(String connId, Boolean connEnabled) {

    }

    @Override
    public Map<String, Boolean> getMFAConnections() {
        return null;
    }

    @Override
    public void removeMFAConnections(String connId) {

    }

    @Override
    public void setMFAConnections(String connId, Boolean connEnabled) {

    }

    @Override
    public Stream<String> getSupportedLocalesStream() {
        Set<String> sLs = entity.getSupportedLocales();
        return sLs == null ? Stream.empty() : sLs.stream();
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        entity.setSupportedLocales(locales);
    }

    @Override
    public Stream<String> getPreferredLoginLocalesStream() {
       return null;
    }

    @Override
    public void setPreferredLoginLocales(Set<String> locales) {
        //NOP
    }

    @Override
    public Boolean isSandboxMode() {
        return false;
    }

    @Override
    public void setSandboxMode(Boolean sandboxMode) {
        //NOP
    }

    @Override
    public Stream<CustomDomainMappingModel> getCustomDomainMappings() {
        return Stream.empty();
    }

    @Override
    public CustomDomainMappingModel getCustomDomainMappingByDomain(String domain) {
        return null;
    }

    @Override
    public CustomDomainMappingModel updateCustomDomainMapping(CustomDomainMappingModel customDomainMappingModel) {
        return null;
    }

    @Override
    public void removeCustomDomainMapping(String domain) {
        return;
    }


}

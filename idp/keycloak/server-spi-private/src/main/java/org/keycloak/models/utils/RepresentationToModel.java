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

package org.keycloak.models.utils;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.keys.KeyProvider;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.migration.migrators.MigrateTo8_0_0;
import org.keycloak.migration.migrators.MigrationUtils;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.OTPCredentialData;
import org.keycloak.models.credential.dto.OTPSecretData;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.policy.password.PasswordPolicyNotMetException;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.UserSyncConnectionRepresentation;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;
import org.keycloak.validation.ValidationUtil;

import static org.keycloak.models.Constants.CIBA;
import static org.keycloak.models.utils.ModelToRepresentation.getLogoAbsURL;
import static org.keycloak.protocol.saml.util.ArtifactBindingUtils.computeArtifactBindingIdentifierString;

public class RepresentationToModel {

    private static Logger logger = Logger.getLogger(RepresentationToModel.class);
    public static final String OIDC = "openid-connect";

    public static OTPPolicy toPolicy(RealmRepresentation rep) {
        OTPPolicy policy = new OTPPolicy();
        if (rep.getOtpPolicyType() != null) policy.setType(rep.getOtpPolicyType());
        if (rep.getOtpPolicyLookAheadWindow() != null) policy.setLookAheadWindow(rep.getOtpPolicyLookAheadWindow());
        if (rep.getOtpPolicyInitialCounter() != null) policy.setInitialCounter(rep.getOtpPolicyInitialCounter());
        if (rep.getOtpPolicyAlgorithm() != null) policy.setAlgorithm(rep.getOtpPolicyAlgorithm());
        if (rep.getOtpPolicyDigits() != null) policy.setDigits(rep.getOtpPolicyDigits());
        if (rep.getOtpPolicyPeriod() != null) policy.setPeriod(rep.getOtpPolicyPeriod());
        return policy;

    }

    public static String getMode() {
        if (Version.MODE.contains("wiam"))
            return "wiam";
        if (Version.MODE.contains("ciam"))
            return "ciam";
        if (Version.MODE.contains("bo"))
            return "bo";

        return "wiam";
    }

    public static void importRealm(KeycloakSession session, RealmRepresentation rep, RealmModel newRealm, boolean skipUserDependent) {
        convertDeprecatedSocialProviders(rep);
        convertDeprecatedApplications(session, rep);
        convertDeprecatedClientTemplates(rep);

        newRealm.setName(rep.getRealm());
        if (rep.getDisplayName() != null) newRealm.setDisplayName(rep.getDisplayName());
        if (rep.getDisplayNameHtml() != null) newRealm.setDisplayNameHtml(rep.getDisplayNameHtml());
        if (rep.isEnabled() != null) newRealm.setEnabled(rep.isEnabled());
        if (rep.getRealmLogo() != null) {
            newRealm.setRealmLogo(rep.getRealmLogo());
            //update the Url to thirdParty Providers
            session.rootBrand().onTenantBrandImageUpdate(newRealm, newRealm.getRealmLogo());
        }
        if (rep.getDefaultRegion() != null) newRealm.setDefaultRegion(rep.getDefaultRegion());
        if (rep.isUserManagedAccessAllowed() != null)
            newRealm.setUserManagedAccessAllowed(rep.isUserManagedAccessAllowed());
        if (rep.isBruteForceProtected() != null) newRealm.setBruteForceProtected(rep.isBruteForceProtected());
        if (rep.isPermanentLockout() != null) newRealm.setPermanentLockout(rep.isPermanentLockout());
        if (rep.getMaxFailureWaitSeconds() != null) newRealm.setMaxFailureWaitSeconds(rep.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null)
            newRealm.setMinimumQuickLoginWaitSeconds(rep.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) newRealm.setWaitIncrementSeconds(rep.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null)
            newRealm.setQuickLoginCheckMilliSeconds(rep.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) newRealm.setMaxDeltaTimeSeconds(rep.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) newRealm.setFailureFactor(rep.getFailureFactor());
        if (rep.isEnabledOwnEmailProvider() != null) newRealm.setOwnEmailProvider(rep.isEnabledOwnEmailProvider());
        if (rep.isEnabledOwnSmsProvider() != null) newRealm.setOwnSmsProvider(rep.isEnabledOwnSmsProvider());
        if (rep.isEventsEnabled() != null) newRealm.setEventsEnabled(rep.isEventsEnabled());
        if (rep.getEnabledEventTypes() != null)
            newRealm.setEnabledEventTypes(new HashSet<>(rep.getEnabledEventTypes()));
        if (rep.getEventsExpiration() != null) newRealm.setEventsExpiration(rep.getEventsExpiration());
        if (rep.getEventsListeners() != null) newRealm.setEventsListeners(new HashSet<>(rep.getEventsListeners()));
        if (rep.isAdminEventsEnabled() != null) newRealm.setAdminEventsEnabled(rep.isAdminEventsEnabled());
        if (rep.isAdminEventsDetailsEnabled() != null)
            newRealm.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());

        if (rep.getNotBefore() != null) newRealm.setNotBefore(rep.getNotBefore());

        if (rep.getDefaultSignatureAlgorithm() != null)
            newRealm.setDefaultSignatureAlgorithm(rep.getDefaultSignatureAlgorithm());
        else newRealm.setDefaultSignatureAlgorithm(Constants.DEFAULT_SIGNATURE_ALGORITHM);

        if (rep.getRevokeRefreshToken() != null) newRealm.setRevokeRefreshToken(rep.getRevokeRefreshToken());
        else newRealm.setRevokeRefreshToken(false);

        if (rep.getRefreshTokenMaxReuse() != null) newRealm.setRefreshTokenMaxReuse(rep.getRefreshTokenMaxReuse());
        else newRealm.setRefreshTokenMaxReuse(0);

        if (rep.getAccessTokenLifespan() != null) newRealm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        else {
            if (getMode() == "bo") {
                newRealm.setAccessTokenLifespan(660);
            } else {
                newRealm.setAccessTokenLifespan(300);
            }
        }


        if (rep.getAccessTokenLifespanForImplicitFlow() != null)
            newRealm.setAccessTokenLifespanForImplicitFlow(rep.getAccessTokenLifespanForImplicitFlow());
        else
            newRealm.setAccessTokenLifespanForImplicitFlow(Constants.DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT);

        if (rep.getSsoSessionIdleTimeout() != null) newRealm.setSsoSessionIdleTimeout(rep.getSsoSessionIdleTimeout());
        else newRealm.setSsoSessionIdleTimeout(1800);
        if (rep.getSsoSessionMaxLifespan() != null) newRealm.setSsoSessionMaxLifespan(rep.getSsoSessionMaxLifespan());
        else newRealm.setSsoSessionMaxLifespan(36000);
        if (rep.getSsoSessionMaxLifespanRememberMe() != null)
            newRealm.setSsoSessionMaxLifespanRememberMe(rep.getSsoSessionMaxLifespanRememberMe());
        if (rep.getSsoSessionIdleTimeoutRememberMe() != null)
            newRealm.setSsoSessionIdleTimeoutRememberMe(rep.getSsoSessionIdleTimeoutRememberMe());
        if (rep.getOfflineSessionIdleTimeout() != null)
            newRealm.setOfflineSessionIdleTimeout(rep.getOfflineSessionIdleTimeout());
        else newRealm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);

        // KEYCLOAK-7688 Offline Session Max for Offline Token
        if (rep.getOfflineSessionMaxLifespanEnabled() != null)
            newRealm.setOfflineSessionMaxLifespanEnabled(rep.getOfflineSessionMaxLifespanEnabled());
        else newRealm.setOfflineSessionMaxLifespanEnabled(false);

        if (rep.getOfflineSessionMaxLifespan() != null)
            newRealm.setOfflineSessionMaxLifespan(rep.getOfflineSessionMaxLifespan());
        else newRealm.setOfflineSessionMaxLifespan(Constants.DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN);

        if (rep.getClientSessionIdleTimeout() != null)
            newRealm.setClientSessionIdleTimeout(rep.getClientSessionIdleTimeout());
        if (rep.getClientSessionMaxLifespan() != null)
            newRealm.setClientSessionMaxLifespan(rep.getClientSessionMaxLifespan());

        if (rep.getClientOfflineSessionIdleTimeout() != null)
            newRealm.setClientOfflineSessionIdleTimeout(rep.getClientOfflineSessionIdleTimeout());
        if (rep.getClientOfflineSessionMaxLifespan() != null)
            newRealm.setClientOfflineSessionMaxLifespan(rep.getClientOfflineSessionMaxLifespan());

        if (rep.getAccessCodeLifespan() != null) newRealm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        else newRealm.setAccessCodeLifespan(60);

        if (rep.getAccessCodeLifespanUserAction() != null)
            newRealm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        else newRealm.setAccessCodeLifespanUserAction(300);

        if (rep.getAccessCodeLifespanLogin() != null)
            newRealm.setAccessCodeLifespanLogin(rep.getAccessCodeLifespanLogin());
        else newRealm.setAccessCodeLifespanLogin(1800);

        if (rep.getActionTokenGeneratedByAdminLifespan() != null)
            newRealm.setActionTokenGeneratedByAdminLifespan(rep.getActionTokenGeneratedByAdminLifespan());
        else newRealm.setActionTokenGeneratedByAdminLifespan(12 * 60 * 60);

        if (rep.getActionTokenGeneratedByUserLifespan() != null)
            newRealm.setActionTokenGeneratedByUserLifespan(rep.getActionTokenGeneratedByUserLifespan());
        else newRealm.setActionTokenGeneratedByUserLifespan(newRealm.getAccessCodeLifespanUserAction());

        // OAuth 2.0 Device Authorization Grant
        OAuth2DeviceConfig deviceConfig = newRealm.getOAuth2DeviceConfig();

        deviceConfig.setOAuth2DeviceCodeLifespan(rep.getOAuth2DeviceCodeLifespan());
        deviceConfig.setOAuth2DevicePollingInterval(rep.getOAuth2DevicePollingInterval());

        if (rep.getSslRequired() != null)
            newRealm.setSslRequired(SslRequired.valueOf(rep.getSslRequired().toUpperCase()));
        if (rep.isRegistrationAllowed() != null) newRealm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isRegistrationEmailAsUsername() != null)
            newRealm.setRegistrationEmailAsUsername(rep.isRegistrationEmailAsUsername());
        if (rep.isRememberMe() != null) newRealm.setRememberMe(rep.isRememberMe());
        if (rep.isVerifyEmail() != null) newRealm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isLoginWithEmailAllowed() != null) newRealm.setLoginWithEmailAllowed(rep.isLoginWithEmailAllowed());
        if (rep.isDuplicateEmailsAllowed() != null) newRealm.setDuplicateEmailsAllowed(rep.isDuplicateEmailsAllowed());
        if (rep.getDuplicateMobNumAllowed() != null)
            newRealm.setDuplicateMobNumAllowed(rep.getDuplicateMobNumAllowed());
        if (rep.isResetPasswordAllowed() != null) newRealm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.getUserInviteWay() != null) newRealm.setUserInviteWay(rep.getUserInviteWay());
        if (rep.getUserInviteClientId() != null) newRealm.setUserInviteClientId(rep.getUserInviteClientId());
        if (rep.isEditUsernameAllowed() != null) newRealm.setEditUsernameAllowed(rep.isEditUsernameAllowed());
        if (rep.getLoginTheme() != null) newRealm.setLoginTheme(rep.getLoginTheme());
        if (rep.getAccountTheme() != null) newRealm.setAccountTheme(rep.getAccountTheme());
        if (rep.getAdminTheme() != null) newRealm.setAdminTheme(rep.getAdminTheme());
        if (rep.getEmailTheme() != null) newRealm.setEmailTheme(rep.getEmailTheme());
        if (rep.getSsoTheme() != null) newRealm.setSSOTheme(rep.getSsoTheme());
        if (rep.getMode() != null) newRealm.setMode(rep.getMode());
        if (rep.getMode() == null)
            newRealm.setMode(getMode());

        // todo remove this stuff as its all deprecated
        if (rep.getRequiredCredentials() != null) {
            for (String requiredCred : rep.getRequiredCredentials()) {
                newRealm.addRequiredCredential(requiredCred);
            }
        } else {
            newRealm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        }

        if (rep.getPasswordPolicy() != null)
            newRealm.setPasswordPolicy(PasswordPolicy.parse(session, rep.getPasswordPolicy()));
        if (rep.getOtpPolicyType() != null) newRealm.setOTPPolicy(toPolicy(rep));
        else newRealm.setOTPPolicy(OTPPolicy.DEFAULT_POLICY);

        WebAuthnPolicy webAuthnPolicy = getWebAuthnPolicyTwoFactor(rep);
        newRealm.setWebAuthnPolicy(webAuthnPolicy);

        webAuthnPolicy = getWebAuthnPolicyPasswordless(rep);
        newRealm.setWebAuthnPolicyPasswordless(webAuthnPolicy);

        updateCibaSettings(rep, newRealm);

        updateParSettings(rep, newRealm);

        Map<String, String> mappedFlows = importAuthenticationFlows(session, newRealm, rep);
        /*if (rep.getRequiredActions() != null) {
            for (RequiredActionProviderRepresentation action : rep.getRequiredActions()) {
                RequiredActionProviderModel model = toModel(action);

                MigrationUtils.updateOTPRequiredAction(model);

                newRealm.addRequiredActionProvider(model);
            }
            DefaultRequiredActions.addDeleteAccountAction(newRealm);
        } else {
            DefaultRequiredActions.addActions(newRealm);
        }*/

        importIdentityProviders(rep, newRealm, session);
        importIdentityProviderMappers(rep, newRealm);

        Map<String, ClientScopeModel> clientScopes = new HashMap<>();
        if (rep.getClientScopes() != null) {
            clientScopes = createClientScopes(session, rep.getClientScopes(), newRealm);
        }
        if (rep.getDefaultDefaultClientScopes() != null) {
            for (String clientScopeName : rep.getDefaultDefaultClientScopes()) {
                ClientScopeModel clientScope = clientScopes.get(clientScopeName);
                if (clientScope != null) {
                    newRealm.addDefaultClientScope(clientScope, true);
                } else {
                    logger.warnf("Referenced client scope '%s' doesn't exist", clientScopeName);
                }
            }
        }
        if (rep.getDefaultOptionalClientScopes() != null) {
            for (String clientScopeName : rep.getDefaultOptionalClientScopes()) {
                ClientScopeModel clientScope = clientScopes.get(clientScopeName);
                if (clientScope != null) {
                    newRealm.addDefaultClientScope(clientScope, false);
                } else {
                    logger.warnf("Referenced client scope '%s' doesn't exist", clientScopeName);
                }
            }
        }

        Map<String, ClientModel> createdClients = new HashMap<>();
        if (rep.getClients() != null) {
            createdClients = createClients(session, rep, newRealm, mappedFlows);
        }

        importRoles(rep.getRoles(), newRealm);

        // Setup realm default roles
        if (rep.getDefaultRoles() != null) {
            for (String roleString : rep.getDefaultRoles()) {
                newRealm.addDefaultRole(roleString.trim());
            }
        }
        // Setup client default roles
        if (rep.getClients() != null) {
            for (ClientRepresentation resourceRep : rep.getClients()) {
                if (resourceRep.getDefaultRoles() != null) {
                    ClientModel clientModel = createdClients.computeIfAbsent(resourceRep.getClientId(), k -> newRealm.getClientByClientId(resourceRep.getClientId()));
                    clientModel.updateDefaultRoles(resourceRep.getDefaultRoles());
                    createdClients.put(clientModel.getClientId(), clientModel);
                }
            }
        }

        // Now that all possible roles and clients are created, create scope mappings

        if (rep.getClientScopeMappings() != null) {

            for (Map.Entry<String, List<ScopeMappingRepresentation>> entry : rep.getClientScopeMappings().entrySet()) {
                ClientModel app = createdClients.computeIfAbsent(entry.getKey(), k -> newRealm.getClientByClientId(entry.getKey()));
                if (app == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                createClientScopeMappings(newRealm, app, entry.getValue());
            }
        }

        if (rep.getScopeMappings() != null) {
            Map<String, RoleModel> roleModelMap = newRealm.getRolesStream().collect(Collectors.toMap(RoleModel::getId, Function.identity()));

            for (ScopeMappingRepresentation scope : rep.getScopeMappings()) {
                ScopeContainerModel scopeContainer = getScopeContainerHavingScope(newRealm, scope);
                for (String roleString : scope.getRoles()) {
                    final String roleStringTrimmed = roleString.trim();
                    RoleModel role = roleModelMap.computeIfAbsent(roleStringTrimmed, k -> newRealm.getRole(roleStringTrimmed));
                    if (role == null) {
                        role = newRealm.addRole(roleString);
                        roleModelMap.put(role.getId(), role);
                    }
                    scopeContainer.addScopeMapping(role);
                }
            }
        }

        if (rep.getSmtpServer() != null) {
            newRealm.setSmtpConfig(new HashMap(rep.getSmtpServer()));
        }

        if (rep.getSmsServer() != null) {
            newRealm.setSmsConfig(new HashMap(rep.getSmsServer()));
        }

        if (rep.getBrowserSecurityHeaders() != null) {
            newRealm.setBrowserSecurityHeaders(rep.getBrowserSecurityHeaders());
        } else {
            newRealm.setBrowserSecurityHeaders(BrowserSecurityHeaders.realmDefaultHeaders);
        }

        if (rep.getComponents() != null) {
            MultivaluedHashMap<String, ComponentExportRepresentation> components = rep.getComponents();
            String parentId = newRealm.getId();
            importComponents(newRealm, components, parentId);
        }
        importUserFederationProvidersAndMappers(session, rep, newRealm);


        if (rep.getGroups() != null) {
            importGroups(newRealm, rep);
            if (rep.getDefaultGroups() != null) {
                for (String path : rep.getDefaultGroups()) {
                    GroupModel found = KeycloakModelUtils.findGroupByPath(newRealm, path);
                    if (found == null) throw new RuntimeException("default group in realm rep doesn't exist: " + path);
                    newRealm.addDefaultGroup(found);
                }
            }
        }


        // create users and their role mappings and social mappings

        if (rep.getUsers() != null) {
            for (UserRepresentation userRep : rep.getUsers()) {
                createUser(session, newRealm, userRep);
            }
        }

        if (rep.getFederatedUsers() != null) {
            for (UserRepresentation userRep : rep.getFederatedUsers()) {
                importFederatedUser(session, newRealm, userRep);
            }
        }

        if (!skipUserDependent) {
            importRealmAuthorizationSettings(rep, newRealm, session);
        }

        if (rep.isInternationalizationEnabled() != null) {
            newRealm.setInternationalizationEnabled(rep.isInternationalizationEnabled());
        }
        if (rep.getSupportedLocales() != null) {
            newRealm.setSupportedLocales(new HashSet<String>(rep.getSupportedLocales()));
        }

        if (rep.getPreferredLoginLocales() != null) {
            newRealm.setPreferredLoginLocales(new HashSet<String>(rep.getPreferredLoginLocales()));
        }

        if (rep.getDefaultLocale() != null) {
            newRealm.setDefaultLocale(rep.getDefaultLocale());
        }

        // import attributes

        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> attr : rep.getAttributes().entrySet()) {
                newRealm.setAttribute(attr.getKey(), attr.getValue());
            }
        }

        if (newRealm.getComponentsStream(newRealm.getId(), KeyProvider.class.getName()).count() == 0) {
            if (rep.getPrivateKey() != null) {
                DefaultKeyProviders.createProviders(newRealm, rep.getPrivateKey(), rep.getCertificate());
            } else {
                DefaultKeyProviders.createProviders(newRealm);
            }
        }
    }


    public static WebAuthnPolicy getWebAuthnPolicyTwoFactor(RealmRepresentation rep) {
        WebAuthnPolicy webAuthnPolicy = new WebAuthnPolicy();

        String webAuthnPolicyRpEntityName = rep.getWebAuthnPolicyRpEntityName();
        if (webAuthnPolicyRpEntityName == null || webAuthnPolicyRpEntityName.isEmpty())
            webAuthnPolicyRpEntityName = Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME;
        webAuthnPolicy.setRpEntityName(webAuthnPolicyRpEntityName);

        List<String> webAuthnPolicySignatureAlgorithms = rep.getWebAuthnPolicySignatureAlgorithms();
        if (webAuthnPolicySignatureAlgorithms == null || webAuthnPolicySignatureAlgorithms.isEmpty())
            webAuthnPolicySignatureAlgorithms = Arrays.asList(Constants.DEFAULT_WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS.split(","));
        webAuthnPolicy.setSignatureAlgorithm(webAuthnPolicySignatureAlgorithms);

        String webAuthnPolicyRpId = rep.getWebAuthnPolicyRpId();
        if (webAuthnPolicyRpId == null || webAuthnPolicyRpId.isEmpty())
            webAuthnPolicyRpId = "";
        webAuthnPolicy.setRpId(webAuthnPolicyRpId);

        String webAuthnPolicyAttestationConveyancePreference = rep.getWebAuthnPolicyAttestationConveyancePreference();
        if (webAuthnPolicyAttestationConveyancePreference == null || webAuthnPolicyAttestationConveyancePreference.isEmpty())
            webAuthnPolicyAttestationConveyancePreference = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setAttestationConveyancePreference(webAuthnPolicyAttestationConveyancePreference);

        String webAuthnPolicyAuthenticatorAttachment = rep.getWebAuthnPolicyAuthenticatorAttachment();
        if (webAuthnPolicyAuthenticatorAttachment == null || webAuthnPolicyAuthenticatorAttachment.isEmpty())
            webAuthnPolicyAuthenticatorAttachment = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setAuthenticatorAttachment(webAuthnPolicyAuthenticatorAttachment);

        String webAuthnPolicyRequireResidentKey = rep.getWebAuthnPolicyRequireResidentKey();
        if (webAuthnPolicyRequireResidentKey == null || webAuthnPolicyRequireResidentKey.isEmpty())
            webAuthnPolicyRequireResidentKey = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setRequireResidentKey(webAuthnPolicyRequireResidentKey);

        String webAuthnPolicyUserVerificationRequirement = rep.getWebAuthnPolicyUserVerificationRequirement();
        if (webAuthnPolicyUserVerificationRequirement == null || webAuthnPolicyUserVerificationRequirement.isEmpty())
            webAuthnPolicyUserVerificationRequirement = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setUserVerificationRequirement(webAuthnPolicyUserVerificationRequirement);

        Integer webAuthnPolicyCreateTimeout = rep.getWebAuthnPolicyCreateTimeout();
        if (webAuthnPolicyCreateTimeout != null) webAuthnPolicy.setCreateTimeout(webAuthnPolicyCreateTimeout);
        else webAuthnPolicy.setCreateTimeout(0);

        Boolean webAuthnPolicyAvoidSameAuthenticatorRegister = rep.isWebAuthnPolicyAvoidSameAuthenticatorRegister();
        if (webAuthnPolicyAvoidSameAuthenticatorRegister != null)
            webAuthnPolicy.setAvoidSameAuthenticatorRegister(webAuthnPolicyAvoidSameAuthenticatorRegister);

        List<String> webAuthnPolicyAcceptableAaguids = rep.getWebAuthnPolicyAcceptableAaguids();
        if (webAuthnPolicyAcceptableAaguids != null)
            webAuthnPolicy.setAcceptableAaguids(webAuthnPolicyAcceptableAaguids);

        return webAuthnPolicy;
    }


    private static WebAuthnPolicy getWebAuthnPolicyPasswordless(RealmRepresentation rep) {
        WebAuthnPolicy webAuthnPolicy = new WebAuthnPolicy();

        String webAuthnPolicyRpEntityName = rep.getWebAuthnPolicyPasswordlessRpEntityName();
        if (webAuthnPolicyRpEntityName == null || webAuthnPolicyRpEntityName.isEmpty())
            webAuthnPolicyRpEntityName = Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME;
        webAuthnPolicy.setRpEntityName(webAuthnPolicyRpEntityName);

        List<String> webAuthnPolicySignatureAlgorithms = rep.getWebAuthnPolicyPasswordlessSignatureAlgorithms();
        if (webAuthnPolicySignatureAlgorithms == null || webAuthnPolicySignatureAlgorithms.isEmpty())
            webAuthnPolicySignatureAlgorithms = Arrays.asList(Constants.DEFAULT_WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS.split(","));
        webAuthnPolicy.setSignatureAlgorithm(webAuthnPolicySignatureAlgorithms);

        String webAuthnPolicyRpId = rep.getWebAuthnPolicyPasswordlessRpId();
        if (webAuthnPolicyRpId == null || webAuthnPolicyRpId.isEmpty())
            webAuthnPolicyRpId = "";
        webAuthnPolicy.setRpId(webAuthnPolicyRpId);

        String webAuthnPolicyAttestationConveyancePreference = rep.getWebAuthnPolicyPasswordlessAttestationConveyancePreference();
        if (webAuthnPolicyAttestationConveyancePreference == null || webAuthnPolicyAttestationConveyancePreference.isEmpty())
            webAuthnPolicyAttestationConveyancePreference = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setAttestationConveyancePreference(webAuthnPolicyAttestationConveyancePreference);

        String webAuthnPolicyAuthenticatorAttachment = rep.getWebAuthnPolicyPasswordlessAuthenticatorAttachment();
        if (webAuthnPolicyAuthenticatorAttachment == null || webAuthnPolicyAuthenticatorAttachment.isEmpty())
            webAuthnPolicyAuthenticatorAttachment = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setAuthenticatorAttachment(webAuthnPolicyAuthenticatorAttachment);

        String webAuthnPolicyRequireResidentKey = rep.getWebAuthnPolicyPasswordlessRequireResidentKey();
        if (webAuthnPolicyRequireResidentKey == null || webAuthnPolicyRequireResidentKey.isEmpty())
            webAuthnPolicyRequireResidentKey = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setRequireResidentKey(webAuthnPolicyRequireResidentKey);

        String webAuthnPolicyUserVerificationRequirement = rep.getWebAuthnPolicyPasswordlessUserVerificationRequirement();
        if (webAuthnPolicyUserVerificationRequirement == null || webAuthnPolicyUserVerificationRequirement.isEmpty())
            webAuthnPolicyUserVerificationRequirement = Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED;
        webAuthnPolicy.setUserVerificationRequirement(webAuthnPolicyUserVerificationRequirement);

        Integer webAuthnPolicyCreateTimeout = rep.getWebAuthnPolicyPasswordlessCreateTimeout();
        if (webAuthnPolicyCreateTimeout != null) webAuthnPolicy.setCreateTimeout(webAuthnPolicyCreateTimeout);
        else webAuthnPolicy.setCreateTimeout(0);

        Boolean webAuthnPolicyAvoidSameAuthenticatorRegister = rep.isWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister();
        if (webAuthnPolicyAvoidSameAuthenticatorRegister != null)
            webAuthnPolicy.setAvoidSameAuthenticatorRegister(webAuthnPolicyAvoidSameAuthenticatorRegister);

        List<String> webAuthnPolicyAcceptableAaguids = rep.getWebAuthnPolicyPasswordlessAcceptableAaguids();
        if (webAuthnPolicyAcceptableAaguids != null)
            webAuthnPolicy.setAcceptableAaguids(webAuthnPolicyAcceptableAaguids);

        return webAuthnPolicy;
    }

    public static void importUserFederationProvidersAndMappers(KeycloakSession session, RealmRepresentation rep, RealmModel newRealm) {
        // providers to convert to component model
        Set<String> convertSet = new HashSet<>();
        convertSet.add(LDAPConstants.LDAP_PROVIDER);
        convertSet.add("kerberos");
        Map<String, String> mapperConvertSet = new HashMap<>();
        mapperConvertSet.put(LDAPConstants.LDAP_PROVIDER, "org.keycloak.storage.ldap.mappers.LDAPStorageMapper");


        Map<String, ComponentModel> userStorageModels = new HashMap<>();

        if (rep.getUserFederationProviders() != null) {
            for (UserFederationProviderRepresentation fedRep : rep.getUserFederationProviders()) {
                if (convertSet.contains(fedRep.getProviderName())) {
                    ComponentModel component = convertFedProviderToComponent(newRealm.getId(), fedRep);
                    userStorageModels.put(fedRep.getDisplayName(), newRealm.importComponentModel(component));
                }
            }
        }

        // This is for case, when you have hand-written JSON file with LDAP userFederationProvider, but WITHOUT any userFederationMappers configured. Default LDAP mappers need to be created in that case.
        Set<String> storageProvidersWhichShouldImportDefaultMappers = new HashSet<>(userStorageModels.keySet());

        if (rep.getUserFederationMappers() != null) {
            for (UserFederationMapperRepresentation representation : rep.getUserFederationMappers()) {
                if (userStorageModels.containsKey(representation.getFederationProviderDisplayName())) {
                    ComponentModel parent = userStorageModels.get(representation.getFederationProviderDisplayName());
                    String newMapperType = mapperConvertSet.get(parent.getProviderId());
                    ComponentModel mapper = convertFedMapperToComponent(newRealm, parent, representation, newMapperType);
                    newRealm.importComponentModel(mapper);


                    storageProvidersWhichShouldImportDefaultMappers.remove(representation.getFederationProviderDisplayName());

                }
            }
        }

        for (String providerDisplayName : storageProvidersWhichShouldImportDefaultMappers) {
            ComponentUtil.notifyCreated(session, newRealm, userStorageModels.get(providerDisplayName));
        }
    }

    protected static void importComponents(RealmModel newRealm, MultivaluedHashMap<String, ComponentExportRepresentation> components, String parentId) {
        for (Map.Entry<String, List<ComponentExportRepresentation>> entry : components.entrySet()) {
            String providerType = entry.getKey();
            for (ComponentExportRepresentation compRep : entry.getValue()) {
                ComponentModel component = new ComponentModel();
                component.setId(compRep.getId());
                component.setName(compRep.getName());
                component.setConfig(compRep.getConfig());
                component.setProviderType(providerType);
                component.setProviderId(compRep.getProviderId());
                component.setSubType(compRep.getSubType());
                component.setParentId(parentId);
                component = newRealm.importComponentModel(component);
                if (compRep.getSubComponents() != null) {
                    importComponents(newRealm, compRep.getSubComponents(), component.getId());
                }
            }
        }
    }

    public static void importRoles(RolesRepresentation realmRoles, RealmModel realm) {
        if (realmRoles == null) return;

        if (realmRoles.getRealm() != null) { // realm roles
            for (RoleRepresentation roleRep : realmRoles.getRealm()) {
                if (!realm.getDefaultRole().getName().equals(roleRep.getName())) { // default role was already imported
                    createRole(realm, roleRep);
                }
            }
        }
        if (realmRoles.getClient() != null) {
            for (Map.Entry<String, List<RoleRepresentation>> entry : realmRoles.getClient().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + entry.getKey());
                }
                for (RoleRepresentation roleRep : entry.getValue()) {
                    // Application role may already exists (for example if it is defaultRole)
                    RoleModel role = roleRep.getId() != null ? client.addRole(roleRep.getId(), roleRep.getName()) : client.addRole(roleRep.getName());
                    role.setDescription(roleRep.getDescription());
                    if (roleRep.getAttributes() != null) {
                        roleRep.getAttributes().forEach((key, value) -> role.setAttribute(key, value));
                    }
                }
            }
        }
        // now that all roles are created, re-iterate and set up composites
        if (realmRoles.getRealm() != null) { // realm roles
            for (RoleRepresentation roleRep : realmRoles.getRealm()) {
                RoleModel role = realm.getRole(roleRep.getName());
                addComposites(role, roleRep, realm);
            }
        }
        if (realmRoles.getClient() != null) {
            for (Map.Entry<String, List<RoleRepresentation>> entry : realmRoles.getClient().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + entry.getKey());
                }
                for (RoleRepresentation roleRep : entry.getValue()) {
                    RoleModel role = client.getRole(roleRep.getName());
                    addComposites(role, roleRep, realm);
                }
            }
        }
    }

    public static void importGroups(RealmModel realm, RealmRepresentation rep) {
        List<GroupRepresentation> groups = rep.getGroups();
        if (groups == null) return;

        GroupModel parent = null;
        for (GroupRepresentation group : groups) {
            importGroup(realm, parent, group);
        }
    }

    public static void importGroup(RealmModel realm, GroupModel parent, GroupRepresentation group) {
        GroupModel newGroup = realm.createGroup(group.getId(), group.getName(), parent);
        if (group.getAttributes() != null) {
            for (Map.Entry<String, List<String>> attr : group.getAttributes().entrySet()) {
                newGroup.setAttribute(attr.getKey(), attr.getValue());
            }
        }

        if (group.getRealmRoles() != null) {
            for (String roleString : group.getRealmRoles()) {
                RoleModel role = realm.getRole(roleString.trim());
                if (role == null) {
                    role = realm.addRole(roleString.trim());
                }
                newGroup.grantRole(role);
            }
        }
        if (group.getClientRoles() != null) {
            for (Map.Entry<String, List<String>> entry : group.getClientRoles().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                List<String> roleNames = entry.getValue();
                for (String roleName : roleNames) {
                    RoleModel role = client.getRole(roleName.trim());
                    if (role == null) {
                        role = client.addRole(roleName.trim());
                    }
                    newGroup.grantRole(role);

                }
            }
        }
        if (group.getSubGroups() != null) {
            for (GroupRepresentation subGroup : group.getSubGroups()) {
                importGroup(realm, newGroup, subGroup);
            }
        }
    }

    public static Map<String, String> importAuthenticationFlows(KeycloakSession session, RealmModel newRealm, RealmRepresentation rep) {
        Map<String, String> mappedFlows = new HashMap<>();
        if (rep.getAuthenticationFlows() == null) {
            // assume this is an old version being imported
            DefaultAuthenticationFlows.migrateFlows(newRealm, session);
        } else {
            if (rep.getAuthenticatorConfig() != null) {
                for (AuthenticatorConfigRepresentation configRep : rep.getAuthenticatorConfig()) {
                    if (configRep.getAlias() == null) {
                        // this can happen only during import json files from keycloak 3.4.0 and older
                        throw new IllegalStateException("Provided realm contains authenticator config with null alias. "
                                + "It should be resolved by adding alias to the authenticator config before exporting the realm.");
                    }
                    AuthenticatorConfigModel model = toModel(configRep);
                    newRealm.addAuthenticatorConfig(model);
                }
            }
            if (rep.getAuthenticationFlows() != null) {
                for (AuthenticationFlowRepresentation flowRep : rep.getAuthenticationFlows()) {
                    AuthenticationFlowModel model = toModel(flowRep);
                    // make sure new id is generated for new AuthenticationFlowModel instance
                    String previousId = model.getId();
                    model.setId(null);
                    model = newRealm.addAuthenticationFlow(model);
                    // store the mapped ids so that clients can reference the correct flow when importing the authenticationFlowBindingOverrides
                    mappedFlows.put(previousId, model.getId());
                }
                for (AuthenticationFlowRepresentation flowRep : rep.getAuthenticationFlows()) {
                    AuthenticationFlowModel model = newRealm.getFlowByAlias(flowRep.getAlias());
                    for (AuthenticationExecutionExportRepresentation exeRep : flowRep.getAuthenticationExecutions()) {
                        AuthenticationExecutionModel execution = toModel(newRealm, model, exeRep);
                        newRealm.addAuthenticatorExecution(execution);
                    }
                }
            }
        }
        if (rep.getBrowserFlow() == null) {
            AuthenticationFlowModel defaultFlow = newRealm.getFlowByAlias(DefaultAuthenticationFlows.BROWSER_FLOW);
            if (defaultFlow != null) {
                newRealm.setBrowserFlow(defaultFlow);
            }
        } else {
            newRealm.setBrowserFlow(newRealm.getFlowByAlias(rep.getBrowserFlow()));
        }
        if (rep.getRegistrationFlow() == null) {
            AuthenticationFlowModel defaultFlow = newRealm.getFlowByAlias(DefaultAuthenticationFlows.REGISTRATION_FLOW);
            if (defaultFlow != null) {
                newRealm.setRegistrationFlow(defaultFlow);
            }
        } else {
            newRealm.setRegistrationFlow(newRealm.getFlowByAlias(rep.getRegistrationFlow()));
        }
        if (rep.getDirectGrantFlow() == null) {
            AuthenticationFlowModel defaultFlow = newRealm.getFlowByAlias(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW);
            if (defaultFlow != null) {
                newRealm.setDirectGrantFlow(defaultFlow);
            }
        } else {
            newRealm.setDirectGrantFlow(newRealm.getFlowByAlias(rep.getDirectGrantFlow()));
        }

        // reset credentials + client flow needs to be more defensive as they were added later (in 1.5 )
        if (rep.getResetCredentialsFlow() == null) {
            AuthenticationFlowModel resetFlow = newRealm.getFlowByAlias(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW);
            if (resetFlow == null) {
                DefaultAuthenticationFlows.resetCredentialsFlow(newRealm);
            } else {
                newRealm.setResetCredentialsFlow(resetFlow);
            }
        } else {
            newRealm.setResetCredentialsFlow(newRealm.getFlowByAlias(rep.getResetCredentialsFlow()));
        }
        if (rep.getClientAuthenticationFlow() == null) {
            AuthenticationFlowModel clientFlow = newRealm.getFlowByAlias(DefaultAuthenticationFlows.CLIENT_AUTHENTICATION_FLOW);
            if (clientFlow == null) {
                DefaultAuthenticationFlows.clientAuthFlow(newRealm);
            } else {
                newRealm.setClientAuthenticationFlow(clientFlow);
            }
        } else {
            newRealm.setClientAuthenticationFlow(newRealm.getFlowByAlias(rep.getClientAuthenticationFlow()));
        }

        // Added in 1.7
        if (newRealm.getFlowByAlias(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW) == null) {
            DefaultAuthenticationFlows.firstBrokerLoginFlow(newRealm, true);
        }

        // Added in 2.2
        String defaultProvider = null;
        if (rep.getIdentityProviders() != null) {
            for (IdentityProviderRepresentation i : rep.getIdentityProviders()) {
                if (i.isEnabled() && i.isAuthenticateByDefault()) {
                    defaultProvider = i.getProviderId();
                    break;
                }
            }
        }

        // Added in 3.2
        if (rep.getDockerAuthenticationFlow() == null) {
            AuthenticationFlowModel dockerAuthenticationFlow = newRealm.getFlowByAlias(DefaultAuthenticationFlows.DOCKER_AUTH);
            if (dockerAuthenticationFlow == null) {
                DefaultAuthenticationFlows.dockerAuthenticationFlow(newRealm);
            } else {
                newRealm.setDockerAuthenticationFlow(dockerAuthenticationFlow);
            }
        } else {
            newRealm.setDockerAuthenticationFlow(newRealm.getFlowByAlias(rep.getDockerAuthenticationFlow()));
        }

        DefaultAuthenticationFlows.addIdentityProviderAuthenticator(newRealm, defaultProvider);

        return mappedFlows;
    }

    private static void convertDeprecatedSocialProviders(RealmRepresentation rep) {
        if (rep.isSocial() != null && rep.isSocial() && rep.getSocialProviders() != null && !rep.getSocialProviders().isEmpty() && rep.getIdentityProviders() == null) {
            Boolean updateProfileFirstLogin = rep.isUpdateProfileOnInitialSocialLogin() != null && rep.isUpdateProfileOnInitialSocialLogin();
            if (rep.getSocialProviders() != null) {

                logger.warn("Using deprecated 'social' configuration in JSON representation. It will be removed in future versions");
                List<IdentityProviderRepresentation> identityProviders = new LinkedList<>();
                for (String k : rep.getSocialProviders().keySet()) {
                    if (k.endsWith(".key")) {
                        String providerId = k.split("\\.")[0];
                        String key = rep.getSocialProviders().get(k);
                        String secret = rep.getSocialProviders().get(k.replace(".key", ".secret"));

                        IdentityProviderRepresentation identityProvider = new IdentityProviderRepresentation();
                        identityProvider.setAlias(providerId);
                        identityProvider.setProviderId(providerId);
                        identityProvider.setEnabled(true);
                        identityProvider.setLinkOnly(false);
                        identityProvider.setUpdateProfileFirstLogin(updateProfileFirstLogin);

                        Map<String, String> config = new HashMap<>();
                        config.put("clientId", key);
                        config.put("clientSecret", secret);
                        identityProvider.setConfig(config);

                        identityProviders.add(identityProvider);
                    }
                }
                rep.setIdentityProviders(identityProviders);
            }
        }
    }

    private static void convertDeprecatedSocialProviders(UserRepresentation user) {
        if (user.getSocialLinks() != null && !user.getSocialLinks().isEmpty() && user.getFederatedIdentities() == null) {

            logger.warnf("Using deprecated 'socialLinks' configuration in JSON representation for user '%s'. It will be removed in future versions", user.getUsername());
            List<FederatedIdentityRepresentation> federatedIdentities = new LinkedList<>();
            for (SocialLinkRepresentation social : user.getSocialLinks()) {
                FederatedIdentityRepresentation federatedIdentity = new FederatedIdentityRepresentation();
                federatedIdentity.setIdentityProvider(social.getSocialProvider());
                federatedIdentity.setUserId(social.getSocialUserId());
                federatedIdentity.setUserName(social.getSocialUsername());
                federatedIdentities.add(federatedIdentity);
            }
            user.setFederatedIdentities(federatedIdentities);
        }

        user.setSocialLinks(null);
    }

    private static void convertDeprecatedApplications(KeycloakSession session, RealmRepresentation realm) {
        if (realm.getApplications() != null || realm.getOauthClients() != null) {
            if (realm.getClients() == null) {
                realm.setClients(new LinkedList<ClientRepresentation>());
            }

            List<ApplicationRepresentation> clients = new LinkedList<>();
            if (realm.getApplications() != null) {
                clients.addAll(realm.getApplications());
            }
            if (realm.getOauthClients() != null) {
                clients.addAll(realm.getOauthClients());
            }

            for (ApplicationRepresentation app : clients) {
                app.setClientId(app.getName());
                app.setName(null);

                if (app instanceof OAuthClientRepresentation) {
                    app.setConsentRequired(true);
                    app.setFullScopeAllowed(false);
                }

                if (app.getProtocolMappers() == null && app.getClaims() != null) {
                    long mask = getClaimsMask(app.getClaims());
                    List<ProtocolMapperRepresentation> convertedProtocolMappers = session.getProvider(MigrationProvider.class).getMappersForClaimMask(mask);
                    app.setProtocolMappers(convertedProtocolMappers);
                    app.setClaims(null);
                }

                realm.getClients().add(app);
            }
        }

        if (realm.getApplicationScopeMappings() != null && realm.getClientScopeMappings() == null) {
            realm.setClientScopeMappings(realm.getApplicationScopeMappings());
        }

        if (realm.getRoles() != null && realm.getRoles().getApplication() != null && realm.getRoles().getClient() == null) {
            realm.getRoles().setClient(realm.getRoles().getApplication());
        }

        if (realm.getUsers() != null) {
            for (UserRepresentation user : realm.getUsers()) {
                if (user.getApplicationRoles() != null && user.getClientRoles() == null) {
                    user.setClientRoles(user.getApplicationRoles());
                }
            }
        }

        if (realm.getRoles() != null && realm.getRoles().getRealm() != null) {
            for (RoleRepresentation role : realm.getRoles().getRealm()) {
                if (role.getComposites() != null && role.getComposites().getApplication() != null && role.getComposites().getClient() == null) {
                    role.getComposites().setClient(role.getComposites().getApplication());
                }
            }
        }

        if (realm.getRoles() != null && realm.getRoles().getClient() != null) {
            for (Map.Entry<String, List<RoleRepresentation>> clientRoles : realm.getRoles().getClient().entrySet()) {
                for (RoleRepresentation role : clientRoles.getValue()) {
                    if (role.getComposites() != null && role.getComposites().getApplication() != null && role.getComposites().getClient() == null) {
                        role.getComposites().setClient(role.getComposites().getApplication());
                    }
                }
            }
        }
    }

    private static void convertDeprecatedClientTemplates(RealmRepresentation realm) {
        if (realm.getClientTemplates() != null) {

            logger.warnf("Using deprecated 'clientTemplates' configuration in JSON representation for realm '%s'. It will be removed in future versions", realm.getRealm());

            List<ClientScopeRepresentation> clientScopes = new LinkedList<>();
            for (ClientTemplateRepresentation template : realm.getClientTemplates()) {
                ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
                scopeRep.setId(template.getId());
                scopeRep.setName(template.getName());
                scopeRep.setProtocol(template.getProtocol());
                scopeRep.setDescription(template.getDescription());
                scopeRep.setAttributes(template.getAttributes());
                scopeRep.setProtocolMappers(template.getProtocolMappers());

                clientScopes.add(scopeRep);
            }

            realm.setClientScopes(clientScopes);
        }
    }

    private static void convertDeprecatedCredentialsFormat(UserRepresentation user) {
        if (user.getCredentials() != null) {
            for (CredentialRepresentation cred : user.getCredentials()) {
                try {
                    if ((cred.getCredentialData() == null || cred.getSecretData() == null) && cred.getValue() == null) {
                        logger.warnf("Using deprecated 'credentials' format in JSON representation for user '%s'. It will be removed in future versions", user.getUsername());

                        if (PasswordCredentialModel.TYPE.equals(cred.getType()) || PasswordCredentialModel.PASSWORD_HISTORY.equals(cred.getType())) {
                            PasswordCredentialData credentialData = new PasswordCredentialData(cred.getHashIterations(), cred.getAlgorithm());
                            cred.setCredentialData(JsonSerialization.writeValueAsString(credentialData));
                            // Created this manually to avoid conversion from Base64 and back
                            cred.setSecretData("{\"value\":\"" + cred.getHashedSaltedValue() + "\",\"salt\":\"" + cred.getSalt() + "\"}");
                            cred.setPriority(10);
                        } else if (OTPCredentialModel.TOTP.equals(cred.getType()) || OTPCredentialModel.HOTP.equals(cred.getType())) {
                            OTPCredentialData credentialData = new OTPCredentialData(cred.getType(), cred.getDigits(), cred.getCounter(), cred.getPeriod(), cred.getAlgorithm());
                            OTPSecretData secretData = new OTPSecretData(cred.getHashedSaltedValue());
                            cred.setCredentialData(JsonSerialization.writeValueAsString(credentialData));
                            cred.setSecretData(JsonSerialization.writeValueAsString(secretData));
                            cred.setPriority(20);
                            cred.setType(OTPCredentialModel.TYPE);
                        }
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
    }

    private static void convertDeprecatedDefaultRoles(RealmRepresentation rep, RealmModel newRealm) {
        if (rep.getDefaultRole() == null) {

            // Setup realm default roles
            if (rep.getDefaultRoles() != null) {
                rep.getDefaultRoles().stream()
                        .map(String::trim)
                        .map(name -> getOrAddRealmRole(newRealm, name))
                        .forEach(role -> newRealm.getDefaultRole().addCompositeRole(role));
            }

            // Setup client default roles
            if (rep.getClients() != null) {
                for (ClientRepresentation clientRep : rep.getClients()) {
                    if (clientRep.getDefaultRoles() != null) {
                        Arrays.stream(clientRep.getDefaultRoles())
                                .map(String::trim)
                                .map(name -> getOrAddClientRole(newRealm.getClientById(clientRep.getId()), name))
                                .forEach(role -> newRealm.getDefaultRole().addCompositeRole(role));
                    }
                }
            }
        }
    }

    private static RoleModel getOrAddRealmRole(RealmModel realm, String name) {
        RoleModel role = realm.getRole(name);
        if (role == null) {
            role = realm.addRole(name);
        }
        return role;
    }

    private static RoleModel getOrAddClientRole(ClientModel client, String name) {
        RoleModel role = client.getRole(name);
        if (role == null) {
            role = client.addRole(name);
        }
        return role;
    }

    public static void renameRealm(RealmModel realm, String name) {
        if (name.equals(realm.getName())) return;

        String oldName = realm.getName();

        ClientModel masterApp = realm.getMasterAdminClient();
        masterApp.setClientId(KeycloakModelUtils.getMasterRealmAdminApplicationClientId(name));
        realm.setName(name);

        ClientModel adminClient = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (adminClient != null) {
            if (adminClient.getBaseUrl() != null) {
                adminClient.setBaseUrl(adminClient.getBaseUrl().replace("/admin/" + oldName + "/", "/admin/" + name + "/"));
            }
            Set<String> adminRedirectUris = new HashSet<>();
            for (String r : adminClient.getRedirectUris()) {
                adminRedirectUris.add(replace(r, "/admin/" + oldName + "/", "/admin/" + name + "/"));
            }
            adminClient.setRedirectUris(adminRedirectUris);
        }

        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient != null) {
            if (accountClient.getBaseUrl() != null) {
                accountClient.setBaseUrl(accountClient.getBaseUrl().replace("/realms/" + oldName + "/", "/realms/" + name + "/"));
            }
            Set<String> accountRedirectUris = new HashSet<>();
            for (String r : accountClient.getRedirectUris()) {
                accountRedirectUris.add(replace(r, "/realms/" + oldName + "/", "/realms/" + name + "/"));
            }
            accountClient.setRedirectUris(accountRedirectUris);
        }
    }

    private static String replace(String url, String target, String replacement) {
        return url != null ? url.replace(target, replacement) : null;
    }

    private static String validateTokenTimings(RealmRepresentation rep, RealmModel realm){
        if(rep.getSsoSessionIdleTimeoutRememberMe() > realm.getSsoSessionIdleTimeout())
            return "SSO session idle remember me timeout cannot be greater than SSO session idle timeout";
        if(rep.getSsoSessionMaxLifespanRememberMe() >= realm.getSsoSessionMaxLifespan())
            return "SSO session max remember me timeout cannot be greater than SSO session max timeout";
        if(rep.getAccessTokenLifespan() >= realm.getSsoSessionMaxLifespan())
            return "Access Token Lifespan timeout cannot be greater than SSO session max timeout";
        if(rep.getAccessTokenLifespanForImplicitFlow() >= realm.getSsoSessionMaxLifespan())
            return "Access Token Lifespan for implicit flow timeout cannot be greater than SSO session max timeout";
        return null;
    }

    public static void updateRealm(RealmRepresentation rep, RealmModel realm, KeycloakSession session) {
        // if (rep.getRealm() != null) {
        //     renameRealm(realm, rep.getRealm());
        // }

        if (!Boolean.parseBoolean(rep.getAttributesOrEmpty().get("userProfileEnabled"))) {
            UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
            provider.setConfiguration(null);
        }

        String error = validateTokenTimings(rep, realm);
        if(StringUtil.isNotBlank(error)){
            throw new RuntimeException(error);
        }

        // Import attributes first, so the stuff saved directly on representation (displayName, bruteForce etc) has bigger priority
        if (rep.getAttributes() != null) {
            Set<String> attrsToRemove = new HashSet<>(realm.getAttributes().keySet());
            attrsToRemove.removeAll(rep.getAttributes().keySet());

            for (Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                realm.setAttribute(entry.getKey(), entry.getValue());
            }

            for (String attr : attrsToRemove) {
                realm.removeAttribute(attr);
            }
        }
        //Dashboard locale,Admin locale
        if (rep.getDashboardLocale() != null) realm.setAttribute("dashboardLocale", rep.getDashboardLocale());
        if (rep.getLoginLocale() != null) realm.setAttribute("loginLocale", rep.getLoginLocale());
        if (rep.getProfileLocale() != null) realm.setAttribute("profileLocale", rep.getProfileLocale());

        if (rep.getDisplayName() != null) realm.setDisplayName(rep.getDisplayName());
        if (rep.getDisplayNameHtml() != null) realm.setDisplayNameHtml(rep.getDisplayNameHtml());
        if (rep.isEnabled() != null && !(rep.getId() == org.keycloak.Config.getAdminRealm() || rep.getRealm() == org.keycloak.Config.getAdminRealm()))
            realm.setEnabled(rep.isEnabled());
        if (rep.getDefaultRegion() != null) realm.setDefaultRegion(rep.getDefaultRegion());
        if (rep.isUserManagedAccessAllowed() != null)
            realm.setUserManagedAccessAllowed(rep.isUserManagedAccessAllowed());
        if (rep.isBruteForceProtected() != null) realm.setBruteForceProtected(rep.isBruteForceProtected());
        if (rep.isPermanentLockout() != null) realm.setPermanentLockout(rep.isPermanentLockout());
        if (rep.getMaxFailureWaitSeconds() != null) realm.setMaxFailureWaitSeconds(rep.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null)
            realm.setMinimumQuickLoginWaitSeconds(rep.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) realm.setWaitIncrementSeconds(rep.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null)
            realm.setQuickLoginCheckMilliSeconds(rep.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) realm.setMaxDeltaTimeSeconds(rep.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) realm.setFailureFactor(rep.getFailureFactor());
        if (rep.isEnabledOwnEmailProvider() != null) realm.setOwnEmailProvider(rep.isEnabledOwnEmailProvider());
        if (rep.isEnabledOwnSmsProvider() != null) realm.setOwnSmsProvider(rep.isEnabledOwnSmsProvider());

        if (rep.isRegistrationAllowed() != null) realm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isRegistrationEmailAsUsername() != null)
            realm.setRegistrationEmailAsUsername(rep.isRegistrationEmailAsUsername());
        if (rep.isRememberMe() != null) realm.setRememberMe(rep.isRememberMe());
        if (rep.isVerifyEmail() != null) realm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isLoginWithEmailAllowed() != null) realm.setLoginWithEmailAllowed(rep.isLoginWithEmailAllowed());
        if (rep.isDuplicateEmailsAllowed() != null) realm.setDuplicateEmailsAllowed(rep.isDuplicateEmailsAllowed());
        if (rep.getDuplicateMobNumAllowed() != null) realm.setDuplicateMobNumAllowed(rep.getDuplicateMobNumAllowed());
        if (rep.isResetPasswordAllowed() != null) realm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.isEditUsernameAllowed() != null) realm.setEditUsernameAllowed(rep.isEditUsernameAllowed());
        if (rep.getSslRequired() != null) realm.setSslRequired(SslRequired.valueOf(rep.getSslRequired().toUpperCase()));
        if (rep.getAccessCodeLifespan() != null) realm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        if (rep.getAccessCodeLifespanUserAction() != null)
            realm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        if (rep.getAccessCodeLifespanLogin() != null)
            realm.setAccessCodeLifespanLogin(rep.getAccessCodeLifespanLogin());
        if (rep.getActionTokenGeneratedByAdminLifespan() != null)
            realm.setActionTokenGeneratedByAdminLifespan(rep.getActionTokenGeneratedByAdminLifespan());
        if (rep.getActionTokenGeneratedByUserLifespan() != null)
            realm.setActionTokenGeneratedByUserLifespan(rep.getActionTokenGeneratedByUserLifespan());
        if (rep.getUserInviteWay() != null)
            realm.setUserInviteWay(rep.getUserInviteWay());
        if (rep.getUserInviteClientId() != null) {
            realm.setUserInviteClientId(rep.getUserInviteClientId());
        }
        if(rep.getSandboxMode() != null){
            realm.setSandboxMode(rep.getSandboxMode());
        }

        OAuth2DeviceConfig deviceConfig = realm.getOAuth2DeviceConfig();

        deviceConfig.setOAuth2DeviceCodeLifespan(rep.getOAuth2DeviceCodeLifespan());
        deviceConfig.setOAuth2DevicePollingInterval(rep.getOAuth2DevicePollingInterval());

        if (rep.getNotBefore() != null) realm.setNotBefore(rep.getNotBefore());
        if (rep.getDefaultSignatureAlgorithm() != null)
            realm.setDefaultSignatureAlgorithm(rep.getDefaultSignatureAlgorithm());
        if (rep.getRevokeRefreshToken() != null) realm.setRevokeRefreshToken(rep.getRevokeRefreshToken());
        if (rep.getRefreshTokenMaxReuse() != null) realm.setRefreshTokenMaxReuse(rep.getRefreshTokenMaxReuse());
        if (rep.getAccessTokenLifespan() != null) realm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        if (rep.getAccessTokenLifespanForImplicitFlow() != null)
            realm.setAccessTokenLifespanForImplicitFlow(rep.getAccessTokenLifespanForImplicitFlow());
        if (rep.getSsoSessionIdleTimeout() != null) realm.setSsoSessionIdleTimeout(rep.getSsoSessionIdleTimeout());
        if (rep.getSsoSessionMaxLifespan() != null) realm.setSsoSessionMaxLifespan(rep.getSsoSessionMaxLifespan());
        if (rep.getSsoSessionIdleTimeoutRememberMe() != null)
            realm.setSsoSessionIdleTimeoutRememberMe(rep.getSsoSessionIdleTimeoutRememberMe());
        if (rep.getSsoSessionMaxLifespanRememberMe() != null)
            realm.setSsoSessionMaxLifespanRememberMe(rep.getSsoSessionMaxLifespanRememberMe());
        if (rep.getOfflineSessionIdleTimeout() != null)
            realm.setOfflineSessionIdleTimeout(rep.getOfflineSessionIdleTimeout());
        // KEYCLOAK-7688 Offline Session Max for Offline Token
        if (rep.getOfflineSessionMaxLifespanEnabled() != null)
            realm.setOfflineSessionMaxLifespanEnabled(rep.getOfflineSessionMaxLifespanEnabled());
        if (rep.getOfflineSessionMaxLifespan() != null)
            realm.setOfflineSessionMaxLifespan(rep.getOfflineSessionMaxLifespan());
        if (rep.getClientSessionIdleTimeout() != null)
            realm.setClientSessionIdleTimeout(rep.getClientSessionIdleTimeout());
        if (rep.getClientSessionMaxLifespan() != null)
            realm.setClientSessionMaxLifespan(rep.getClientSessionMaxLifespan());
        if (rep.getClientOfflineSessionIdleTimeout() != null)
            realm.setClientOfflineSessionIdleTimeout(rep.getClientOfflineSessionIdleTimeout());
        if (rep.getClientOfflineSessionMaxLifespan() != null)
            realm.setClientOfflineSessionMaxLifespan(rep.getClientOfflineSessionMaxLifespan());
        if (rep.getRequiredCredentials() != null) {
            realm.updateRequiredCredentials(rep.getRequiredCredentials());
        }
        if (rep.getLoginTheme() != null) realm.setLoginTheme(rep.getLoginTheme());
        if (rep.getAccountTheme() != null) realm.setAccountTheme(rep.getAccountTheme());
        if (rep.getAdminTheme() != null) realm.setAdminTheme(rep.getAdminTheme());
        if (rep.getEmailTheme() != null) realm.setEmailTheme(rep.getEmailTheme());
        if (rep.getSsoTheme() != null) realm.setSSOTheme(rep.getSsoTheme());
        if (rep.getMode() != null) realm.setMode(rep.getMode());
        if (rep.getMode() == null)
            realm.setMode(getMode());

        if (rep.isEventsEnabled() != null) realm.setEventsEnabled(rep.isEventsEnabled());
        if (rep.getEventsExpiration() != null) realm.setEventsExpiration(rep.getEventsExpiration());
        if (rep.getEventsListeners() != null) realm.setEventsListeners(new HashSet<>(rep.getEventsListeners()));
        if (rep.getEnabledEventTypes() != null) realm.setEnabledEventTypes(new HashSet<>(rep.getEnabledEventTypes()));

        if (rep.isAdminEventsEnabled() != null) realm.setAdminEventsEnabled(rep.isAdminEventsEnabled());
        if (rep.isAdminEventsDetailsEnabled() != null)
            realm.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());


        if (rep.getPasswordPolicy() != null)
            realm.setPasswordPolicy(PasswordPolicy.parse(session, rep.getPasswordPolicy()));
        if (rep.getOtpPolicyType() != null) realm.setOTPPolicy(toPolicy(rep));

        WebAuthnPolicy webAuthnPolicy = getWebAuthnPolicyTwoFactor(rep);
        realm.setWebAuthnPolicy(webAuthnPolicy);

        webAuthnPolicy = getWebAuthnPolicyPasswordless(rep);
        realm.setWebAuthnPolicyPasswordless(webAuthnPolicy);

        updateCibaSettings(rep, realm);
        updateParSettings(rep, realm);
        session.clientPolicy().updateRealmModelFromRepresentation(realm, rep);

        if (rep.getSmtpServer() != null) {
            Map<String, String> config = new HashMap(rep.getSmtpServer());
            if (rep.getSmtpServer().containsKey("password") && ComponentRepresentation.SECRET_VALUE.equals(rep.getSmtpServer().get("password"))) {
                String passwordValue = realm.getSmtpConfig() != null ? realm.getSmtpConfig().get("password") : null;
                config.put("password", passwordValue);
            }
            realm.setSmtpConfig(config);
        }

        if (rep.getSmsServer() != null) {
            Map<String, String> config = new HashMap(rep.getSmsServer());
            realm.setSmsConfig(config);
        }

        if (rep.getBrowserSecurityHeaders() != null) {
            realm.setBrowserSecurityHeaders(rep.getBrowserSecurityHeaders());
        }

        if (rep.isInternationalizationEnabled() != null) {
            realm.setInternationalizationEnabled(rep.isInternationalizationEnabled());
        }
        if (rep.getSupportedLocales() != null) {
            realm.setSupportedLocales(new HashSet<String>(rep.getSupportedLocales()));
        }

        if (rep.getPreferredLoginLocales() != null) {
            realm.setPreferredLoginLocales(new HashSet<String>(rep.getPreferredLoginLocales()));
        }
        if (rep.getDefaultLocale() != null) {
            realm.setDefaultLocale(rep.getDefaultLocale());
        }
        if (rep.getBrowserFlow() != null) {
            realm.setBrowserFlow(realm.getFlowByAlias(rep.getBrowserFlow()));
        }
        if (rep.getRegistrationFlow() != null) {
            realm.setRegistrationFlow(realm.getFlowByAlias(rep.getRegistrationFlow()));
        }
        if (rep.getDirectGrantFlow() != null) {
            realm.setDirectGrantFlow(realm.getFlowByAlias(rep.getDirectGrantFlow()));
        }
        if (rep.getResetCredentialsFlow() != null) {
            realm.setResetCredentialsFlow(realm.getFlowByAlias(rep.getResetCredentialsFlow()));
        }
        if (rep.getClientAuthenticationFlow() != null) {
            realm.setClientAuthenticationFlow(realm.getFlowByAlias(rep.getClientAuthenticationFlow()));
        }
        if (rep.getDockerAuthenticationFlow() != null) {
            realm.setDockerAuthenticationFlow(realm.getFlowByAlias(rep.getDockerAuthenticationFlow()));
        }

        if (rep.getRealmLogo() != null) {
            realm.setRealmLogo(getLogoAbsURL(rep.getRealmLogo()));
            //update the Url to thirdParty Providers
            session.rootBrand().onTenantBrandImageUpdate(realm, realm.getRealmLogo());
        }

        if (rep.getAuthenticationFlowBindingOverrides() != null) {
            for (Map.Entry<String, String> entry : rep.getAuthenticationFlowBindingOverrides().entrySet()) {
                if (entry.getValue() == null || entry.getValue().trim().equals("")) {
                    realm.removeAuthenticationFlowBindingOverride(entry.getKey());
                } else {
                    String flowId = entry.getValue();
                    if (entry.getKey().equalsIgnoreCase("action")) {
                        if (realm.getActionById(flowId) == null) {
                            throw new RuntimeException("Unable to resolve action flow binding override for: " + entry.getKey());
                        }
                        realm.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    } else if (entry.getKey().equalsIgnoreCase("ciba_auth")) {
                        realm.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    } else {
                        if (realm.getAuthenticationFlowById(flowId) == null) {
                            throw new RuntimeException("Unable to resolve auth flow binding override for: " + entry.getKey());
                        }
                        realm.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    }

                }
            }
        }

        if (rep.getAppConnections() != null) {
            for (Map.Entry<String, Boolean> entry : rep.getAppConnections().entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                } else {
                    String connId = entry.getKey();
                    if (checkIfConnectionPresent(realm, connId) == null) {
                        throw new RuntimeException("Unable to resolve Social Connection for: " + entry.getKey());
                    } else {
                        realm.setApplicationConnections(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        if (rep.getMfaConnections() != null) {
            for (Map.Entry<String, Boolean> entry : rep.getMfaConnections().entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                } else {
                    String connId = entry.getKey();
                    if (checkIfMFAPresent(realm, connId) == null) {
                        realm.removeMFAConnections(entry.getKey());
                    } else {
                        realm.setMFAConnections(entry.getKey(), entry.getValue());
                    }
                }
            }
        }


    }

    private static void updateCibaSettings(RealmRepresentation rep, RealmModel realm) {
        Map<String, String> newAttributes = rep.getAttributesOrEmpty();
        CibaConfig cibaPolicy = realm.getCibaPolicy();

        cibaPolicy.setBackchannelTokenDeliveryMode(newAttributes.get(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE));
        cibaPolicy.setExpiresIn(newAttributes.get(CibaConfig.CIBA_EXPIRES_IN));
        cibaPolicy.setPoolingInterval(newAttributes.get(CibaConfig.CIBA_INTERVAL));
        cibaPolicy.setAuthRequestedUserHint(newAttributes.get(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT));
    }

    private static void updateParSettings(RealmRepresentation rep, RealmModel realm) {
        Map<String, String> newAttributes = rep.getAttributesOrEmpty();
        ParConfig parPolicy = realm.getParPolicy();

        parPolicy.setRequestUriLifespan(newAttributes.get(ParConfig.PAR_REQUEST_URI_LIFESPAN));
    }

    // Basic realm stuff


    public static ComponentModel convertFedProviderToComponent(String realmId, UserFederationProviderRepresentation fedModel) {
        UserStorageProviderModel model = new UserStorageProviderModel();
        model.setId(fedModel.getId());
        model.setName(fedModel.getDisplayName());
        model.setParentId(realmId);
        model.setProviderId(fedModel.getProviderName());
        model.setProviderType(UserStorageProvider.class.getName());
        model.setFullSyncPeriod(fedModel.getFullSyncPeriod());
        model.setPriority(fedModel.getPriority());
        model.setChangedSyncPeriod(fedModel.getChangedSyncPeriod());
        model.setLastSync(fedModel.getLastSync());
        if (fedModel.getConfig() != null) {
            for (Map.Entry<String, String> entry : fedModel.getConfig().entrySet()) {
                model.getConfig().putSingle(entry.getKey(), entry.getValue());
            }
        }
        return model;
    }

    public static ComponentModel convertFedMapperToComponent(RealmModel realm, ComponentModel parent, UserFederationMapperRepresentation rep, String newMapperType) {
        ComponentModel mapper = new ComponentModel();
        mapper.setId(rep.getId());
        mapper.setName(rep.getName());
        mapper.setProviderId(rep.getFederationMapperType());
        mapper.setProviderType(newMapperType);
        mapper.setParentId(parent.getId());
        if (rep.getConfig() != null) {
            for (Map.Entry<String, String> entry : rep.getConfig().entrySet()) {
                mapper.getConfig().putSingle(entry.getKey(), entry.getValue());
            }
        }
        return mapper;
    }


    // Roles

    public static RoleModel createRole(RealmModel newRealm, RoleRepresentation roleRep) {
        RoleModel role = roleRep.getId() != null ? newRealm.addRole(roleRep.getId(), roleRep.getName()) : newRealm.addRole(roleRep.getName());
        if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
        if (roleRep.getAttributes() != null) {
            for (Map.Entry<String, List<String>> attribute : roleRep.getAttributes().entrySet()) {
                role.setAttribute(attribute.getKey(), attribute.getValue());
            }
        }
        return role;
    }

    private static void addComposites(RoleModel role, RoleRepresentation roleRep, RealmModel realm) {
        if (roleRep.getComposites() == null) return;
        if (roleRep.getComposites().getRealm() != null) {
            for (String roleStr : roleRep.getComposites().getRealm()) {
                RoleModel realmRole = realm.getRole(roleStr);
                if (realmRole == null) throw new RuntimeException("Unable to find composite realm role: " + roleStr);
                role.addCompositeRole(realmRole);
            }
        }
        if (roleRep.getComposites().getClient() != null) {
            for (Map.Entry<String, List<String>> entry : roleRep.getComposites().getClient().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + roleRep.getName());
                }
                for (String roleStr : entry.getValue()) {
                    RoleModel clientRole = client.getRole(roleStr);
                    if (clientRole == null)
                        throw new RuntimeException("Unable to find composite client role: " + roleStr);
                    role.addCompositeRole(clientRole);
                }
            }

        }

    }

    // CLIENTS

    private static Map<String, ClientModel> createClients(KeycloakSession session, RealmRepresentation rep, RealmModel realm, Map<String, String> mappedFlows) {
        Map<String, ClientModel> appMap = new HashMap<String, ClientModel>();
        for (ClientRepresentation resourceRep : rep.getClients()) {
            ClientModel app = createClient(session, realm, resourceRep, false, mappedFlows);
            appMap.put(app.getClientId(), app);

            ValidationUtil.validateClient(session, app, false, r -> {
                throw new RuntimeException("Invalid client " + app.getClientId() + ": " + r.getAllErrorsAsString());
            });
        }
        return appMap;
    }

    /**
     * Does not create scope or role mappings!
     *
     * @param realm
     * @param resourceRep
     * @return
     */
    public static ClientModel createClient(KeycloakSession session, RealmModel realm, ClientRepresentation resourceRep, boolean addDefaultRoles) {
        return createClient(session, realm, resourceRep, addDefaultRoles, null);
    }

    private static ClientModel createClient(KeycloakSession session, RealmModel realm, ClientRepresentation resourceRep, boolean addDefaultRoles, Map<String, String> mappedFlows) {
        logger.debugv("Create client: {0}", resourceRep.getClientId());

        ClientModel client = resourceRep.getId() != null ? realm.addClient(resourceRep.getId(), resourceRep.getClientId()) : resourceRep.getClientId() != null
                ? realm.addClient(resourceRep.getClientId()) : realm.addClient();
        if (resourceRep.getName() != null) client.setName(resourceRep.getName());
        if (resourceRep.getDescription() != null) client.setDescription(resourceRep.getDescription());
        if (resourceRep.isEnabled() != null) client.setEnabled(resourceRep.isEnabled());
        if (resourceRep.isAlwaysDisplayInConsole() != null)
            client.setAlwaysDisplayInConsole(resourceRep.isAlwaysDisplayInConsole());
        client.setManagementUrl(resourceRep.getAdminUrl());
        if (resourceRep.isSurrogateAuthRequired() != null)
            client.setSurrogateAuthRequired(resourceRep.isSurrogateAuthRequired());
        if (resourceRep.getRootUrl() != null) client.setRootUrl(resourceRep.getRootUrl());
        if (resourceRep.getBaseUrl() != null) client.setBaseUrl(resourceRep.getBaseUrl());
        if (resourceRep.isBearerOnly() != null) client.setBearerOnly(resourceRep.isBearerOnly());
        if (resourceRep.isConsentRequired() != null) client.setConsentRequired(resourceRep.isConsentRequired());

        // Backwards compatibility only
        if (resourceRep.isDirectGrantsOnly() != null) {
            logger.warn("Using deprecated 'directGrantsOnly' configuration in JSON representation. It will be removed in future versions");
            client.setStandardFlowEnabled(!resourceRep.isDirectGrantsOnly());
            client.setDirectAccessGrantsEnabled(resourceRep.isDirectGrantsOnly());
        }

        if (resourceRep.isStandardFlowEnabled() != null)
            client.setStandardFlowEnabled(resourceRep.isStandardFlowEnabled());
        if (resourceRep.isImplicitFlowEnabled() != null)
            client.setImplicitFlowEnabled(resourceRep.isImplicitFlowEnabled());
        if (resourceRep.isDirectAccessGrantsEnabled() != null)
            client.setDirectAccessGrantsEnabled(resourceRep.isDirectAccessGrantsEnabled());
        if (resourceRep.isServiceAccountsEnabled() != null)
            client.setServiceAccountsEnabled(resourceRep.isServiceAccountsEnabled());

        if (resourceRep.isPublicClient() != null) client.setPublicClient(resourceRep.isPublicClient());
        if (resourceRep.isFrontchannelLogout() != null)
            client.setFrontchannelLogout(resourceRep.isFrontchannelLogout());

        // set defaults to openid-connect if no protocol specified
        if (resourceRep.getProtocol() != null) {
            client.setProtocol(resourceRep.getProtocol());
        } else {
            client.setProtocol(OIDC);
        }
        if (resourceRep.getNodeReRegistrationTimeout() != null) {
            client.setNodeReRegistrationTimeout(resourceRep.getNodeReRegistrationTimeout());
        } else {
            client.setNodeReRegistrationTimeout(-1);
        }

        if (resourceRep.getNotBefore() != null) {
            client.setNotBefore(resourceRep.getNotBefore());
        }

        if (resourceRep.getClientAuthenticatorType() != null) {
            client.setClientAuthenticatorType(resourceRep.getClientAuthenticatorType());
        } else {
            client.setClientAuthenticatorType(KeycloakModelUtils.getDefaultClientAuthenticatorType());
        }

        client.setSecret(resourceRep.getSecret());
        if (client.getSecret() == null) {
            KeycloakModelUtils.generateSecret(client);
        }

        client.setSandboxSecret(resourceRep.getSandboxSecret());
        if (client.getSandboxSecret() == null) {
            KeycloakModelUtils.generateSandboxSecret(client);
        }

        client.setSandboxClientId(resourceRep.getSandboxClientId());
        if(client.getSandboxClientId() == null){
            client.setSandboxClientId(KeycloakModelUtils.generateId());
        }

        if (resourceRep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : resourceRep.getAttributes().entrySet()) {
                client.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        if (resourceRep.getAttributes() != null && resourceRep.getAttributes().containsKey("login_theme")) {
            client.setAttribute("login_theme", resourceRep.getAttributes().get("login_theme"));
        } else {
            client.setAttribute("login_theme", "default");
        }

        client.setAttribute("inviteOnly", "false");

        if ("saml".equals(resourceRep.getProtocol())
                && (resourceRep.getAttributes() == null
                || !resourceRep.getAttributes().containsKey("saml.artifact.binding.identifier"))) {
            client.setAttribute("saml.artifact.binding.identifier",
                    computeArtifactBindingIdentifierString(client.getClientId()));
        }

        if (resourceRep.getAuthenticationFlowBindingOverrides() != null && !resourceRep.getAppType().equals(CIBA)) {
            for (Map.Entry<String, String> entry : resourceRep.getAuthenticationFlowBindingOverrides().entrySet()) {
                if (entry.getValue() == null || entry.getValue().trim().equals("")) {
                    continue;
                } else {
                    String flowId = entry.getValue();
                    // check if flow id was mapped when the flows were imported
                    if (mappedFlows != null && mappedFlows.containsKey(flowId)) {
                        flowId = mappedFlows.get(flowId);
                    }
                    if (entry.getKey().equalsIgnoreCase("verification-action")) {
                        if (client.getRealm().getActionById(flowId) == null) {
                            throw new RuntimeException("Unable to resolve verification action flow binding override for: " + entry.getKey());
                        }
                        client.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    } else if (entry.getKey().equalsIgnoreCase("action")) {
                        if (client.getRealm().getActionById(flowId) == null) {
                            throw new RuntimeException("Unable to resolve verification action flow binding override for: " + entry.getKey());
                        }
                        client.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    } else if (entry.getKey().equalsIgnoreCase("onboarding-action")) {
                        if (client.getRealm().getActionById(flowId) == null) {
                            throw new RuntimeException("Unable to resolve onboarding flow binding override for: " + entry.getKey());
                        }
                        client.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    } else if (entry.getKey().equalsIgnoreCase("ciba_auth")) {
                        client.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    } else {
                        if (client.getRealm().getAuthenticationFlowById(flowId) == null) {
                            throw new RuntimeException("Unable to resolve auth flow binding override for: " + entry.getKey());
                        }
                        client.setAuthenticationFlowBindingOverride(entry.getKey(), flowId);
                    }
                }
            }
        }
        if (resourceRep.getAppConnections() != null) {
            for (Map.Entry<String, Boolean> entry : resourceRep.getAppConnections().entrySet()) {
                if (entry.getValue() == null || !entry.getValue().equals(Boolean.TRUE) || !entry.getValue().equals(Boolean.FALSE)) {
                    continue;
                } else {
                    String connId = entry.getKey();
                    if (checkIfConnectionPresent(realm, connId) == null) {
                        throw new RuntimeException("Unable to resolve Social Connection for: " + entry.getKey());
                    } else {
                        client.setApplicationConnections(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        if (resourceRep.getMfaConnections() != null) {
            for (Map.Entry<String, Boolean> entry : resourceRep.getMfaConnections().entrySet()) {
                if (entry.getValue() == null || !entry.getValue().equals(Boolean.TRUE) || !entry.getValue().equals(Boolean.FALSE)) {
                    continue;
                } else {
                    String connId = entry.getKey();
                    if (checkIfMFAPresent(realm, connId) == null) {
                        throw new RuntimeException("Unable to resolve MFA Connection for: " + entry.getKey());
                    } else {
                        client.setMFAConnections(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        addRedirectUrisAndSetupWebOrigins(client, resourceRep);

        if (resourceRep.getRegisteredNodes() != null) {
            for (Map.Entry<String, Integer> entry : resourceRep.getRegisteredNodes().entrySet()) {
                client.registerNode(entry.getKey(), entry.getValue());
            }
        }

        if (addDefaultRoles && resourceRep.getDefaultRoles() != null) {
            client.updateDefaultRoles(resourceRep.getDefaultRoles());
        }


        if (resourceRep.getProtocolMappers() != null) {
            // first, remove all default/built in mappers
            client.getProtocolMappersStream().collect(Collectors.toList()).forEach(client::removeProtocolMapper);

            for (ProtocolMapperRepresentation mapper : resourceRep.getProtocolMappers()) {
                client.addProtocolMapper(toModel(mapper));
            }

            MigrationUtils.updateProtocolMappers(client);

        }

        if (resourceRep.getClientTemplate() != null) {
            String clientTemplateName = KeycloakModelUtils.convertClientScopeName(resourceRep.getClientTemplate());
            addClientScopeToClient(realm, client, clientTemplateName, true);
        }

        if (resourceRep.getDefaultClientScopes() != null || resourceRep.getOptionalClientScopes() != null) {
            // First remove all default/built in client scopes
            for (ClientScopeModel clientScope : client.getClientScopes(true, false).values()) {
                client.removeClientScope(clientScope);
            }

            // First remove all default/built in client scopes
            for (ClientScopeModel clientScope : client.getClientScopes(false, false).values()) {
                client.removeClientScope(clientScope);
            }
        }

        if (resourceRep.getDefaultClientScopes() != null) {
            for (String clientScopeName : resourceRep.getDefaultClientScopes()) {
                addClientScopeToClient(realm, client, clientScopeName, true);
            }
        }
        if (resourceRep.getOptionalClientScopes() != null) {
            for (String clientScopeName : resourceRep.getOptionalClientScopes()) {
                addClientScopeToClient(realm, client, clientScopeName, false);
            }
        }

        if (resourceRep.isFullScopeAllowed() != null) {
            client.setFullScopeAllowed(resourceRep.isFullScopeAllowed());
        } else {
            client.setFullScopeAllowed(!client.isConsentRequired());
        }

        if (resourceRep.getMfaPolicyId() != null) {
            client.setMfaPolicyId(resourceRep.getMfaPolicyId());
            client.setMfaPolicyType(resourceRep.getMfaPolicyType());
        }

        client.setLastUpdatedTimestamp(System.currentTimeMillis());

        client.updateClient();
        resourceRep.setId(client.getId());

        if (resourceRep.getAppType() != null) {
            switch (resourceRep.getAppType()) {
                case Constants.HIGH_SECURE:
                    client.setStandardFlowEnabled(true);
                    client.setImplicitFlowEnabled(false);
                    client.setDirectAccessGrantsEnabled(false);
                    client.setServiceAccountsEnabled(false);
                    client.setBearerOnly(false);
                    break;
                case Constants.REST_API:
                    client.setDirectAccessGrantsEnabled(true);
                    client.setStandardFlowEnabled(false);
                    client.setImplicitFlowEnabled(false);
                    client.setServiceAccountsEnabled(false);
                    client.setBearerOnly(false);
                    break;
                case Constants.M2M:
                    client.setServiceAccountsEnabled(true);
                    client.setDirectAccessGrantsEnabled(false);
                    client.setStandardFlowEnabled(false);
                    client.setImplicitFlowEnabled(false);
                    client.setPublicClient(false);
                    client.setBearerOnly(false);
                    break;
                case Constants.BROWSER_BASED:
                case Constants.IWS:
                    client.setImplicitFlowEnabled(true);
                    client.setStandardFlowEnabled(false);
                    client.setDirectAccessGrantsEnabled(false);
                    client.setServiceAccountsEnabled(false);
                    client.setBearerOnly(false);
                    break;
                case CIBA:
                    client.setStandardFlowEnabled(true);
                    client.setImplicitFlowEnabled(false);
                    client.setDirectAccessGrantsEnabled(false);
                    client.setServiceAccountsEnabled(false);
                    client.setBearerOnly(false);
                    client.setAttribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, "true");
                    client.setPublicClient(false);
                    client.setSandboxMode(resourceRep.getSandboxMode());
                    break;
            }
        }

        return client;
    }

    private static void addClientScopeToClient(RealmModel realm, ClientModel client, String clientScopeName, boolean defaultScope) {
        ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(realm, clientScopeName);
        if (clientScope != null) {
            client.addClientScope(clientScope, defaultScope);
        } else {
            logger.warnf("Referenced client scope '%s' doesn't exist. Ignoring", clientScopeName);
        }
    }

    public static void updateClient(ClientRepresentation rep, ClientModel resource) {
        if (rep.getName() != null)
            resource.setName(rep.getName());
        if (rep.getDescription() != null) resource.setDescription(rep.getDescription());
        if (rep.isEnabled() != null) resource.setEnabled(rep.isEnabled());
        if (rep.isAlwaysDisplayInConsole() != null) resource.setAlwaysDisplayInConsole(rep.isAlwaysDisplayInConsole());
        if (rep.isBearerOnly() != null) resource.setBearerOnly(rep.isBearerOnly());
        if (rep.isConsentRequired() != null) resource.setConsentRequired(rep.isConsentRequired());
        if (rep.isStandardFlowEnabled() != null) resource.setStandardFlowEnabled(rep.isStandardFlowEnabled());
        if (rep.isImplicitFlowEnabled() != null) resource.setImplicitFlowEnabled(rep.isImplicitFlowEnabled());
        if (rep.isDirectAccessGrantsEnabled() != null)
            resource.setDirectAccessGrantsEnabled(rep.isDirectAccessGrantsEnabled());
        if (rep.isServiceAccountsEnabled() != null) resource.setServiceAccountsEnabled(rep.isServiceAccountsEnabled());
        if (rep.isPublicClient() != null) resource.setPublicClient(rep.isPublicClient());
        if (rep.isFullScopeAllowed() != null) resource.setFullScopeAllowed(rep.isFullScopeAllowed());
        if (rep.isFrontchannelLogout() != null) resource.setFrontchannelLogout(rep.isFrontchannelLogout());
        if (rep.getRootUrl() != null) resource.setRootUrl(rep.getRootUrl());
        if (rep.getAdminUrl() != null) resource.setManagementUrl(rep.getAdminUrl());
        if (rep.getBaseUrl() != null) resource.setBaseUrl(rep.getBaseUrl());
        if (rep.isSurrogateAuthRequired() != null) resource.setSurrogateAuthRequired(rep.isSurrogateAuthRequired());
        if (rep.getNodeReRegistrationTimeout() != null)
            resource.setNodeReRegistrationTimeout(rep.getNodeReRegistrationTimeout());
        if (rep.getClientAuthenticatorType() != null)
            resource.setClientAuthenticatorType(rep.getClientAuthenticatorType());

        if (rep.getRegistrationAllowed() != null) resource.setRegistrationAllowed(rep.getRegistrationAllowed());
        if (rep.getResetPasswordAllowed() != null) resource.setResetPasswordAllowed(rep.getResetPasswordAllowed());
        if (rep.getRememberMe() != null) resource.setRememberMe(rep.getRememberMe());

        if (rep.getProtocol() != null) resource.setProtocol(rep.getProtocol());
        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                resource.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : removeEmptyString(rep.getAttributes()).entrySet()) {
                resource.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        if ("saml".equals(rep.getProtocol())
                && (rep.getAttributes() == null
                || !rep.getAttributes().containsKey("saml.artifact.binding.identifier"))) {
            resource.setAttribute("saml.artifact.binding.identifier", computeArtifactBindingIdentifierString(rep.getClientId()));
        }

        if (rep.getAuthenticationFlowBindingOverrides() != null) {
            for (Map.Entry<String, String> entry : rep.getAuthenticationFlowBindingOverrides().entrySet()) {
                if (entry.getValue() == null || entry.getValue().trim().equals("")) {
                    resource.removeAuthenticationFlowBindingOverride(entry.getKey());
                } else {
                    String flowId = entry.getValue();
                    if (entry.getKey().equalsIgnoreCase("verification-action")) {
                        if (resource.getRealm().getActionById(flowId) == null) {
                            throw new RuntimeException("Unable to resolve verification action flow binding override for: " + entry.getKey());
                        }
                        resource.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    } else if (entry.getKey().equalsIgnoreCase("action")) {
                        if (resource.getRealm().getActionById(flowId) == null) {
                            // since verify-action and onboarding-action is only used in bo mode, and action is set during triggering a transaction
                            if(!resource.getRealm().getMode().equals("bo")) {
                                throw new RuntimeException("Unable to resolve verification action flow binding override for: " + entry.getKey());
                            }
                            else {
                                resource.removeAuthenticationFlowBindingOverride(entry.getKey());
                            }
                        }
                        resource.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    } else if (entry.getKey().equalsIgnoreCase("onboarding-action")) {
                        if (resource.getRealm().getActionById(flowId) == null) {
                            throw new RuntimeException("Unable to resolve onboarding flow binding override for: " + entry.getKey());
                        }
                        resource.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    } else if (entry.getKey().equalsIgnoreCase("ciba_auth")) {
                        resource.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    } else {
                        if (resource.getRealm().getAuthenticationFlowById(flowId) == null) {
                            throw new RuntimeException("Unable to resolve auth flow binding override for: " + entry.getKey());
                        }
                        resource.setAuthenticationFlowBindingOverride(entry.getKey(), entry.getValue());
                    }

                }
            }
        }

        if (rep.getAppConnections() != null) {
            for (Map.Entry<String, Boolean> entry : rep.getAppConnections().entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                } else {
                    String connId = entry.getKey();
                    if (checkIfConnectionPresent(resource.getRealm(), connId) == null) {
                        throw new RuntimeException("Unable to resolve Social Connection for: " + entry.getKey());
                    } else {
                        resource.setApplicationConnections(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        if (rep.getMfaConnections() != null) {
            for (Map.Entry<String, Boolean> entry : rep.getMfaConnections().entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                } else {
                    String connId = entry.getKey();
                    if (checkIfMFAPresent(resource.getRealm(), connId) == null) {
                        throw new RuntimeException("Unable to resolve Social Connection for: " + entry.getKey());
                    } else {
                        resource.setMFAConnections(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        if (rep.getAppType() != null) {
            switch (rep.getAppType()) {
                case Constants.HIGH_SECURE:
                    resource.setStandardFlowEnabled(true);
                    resource.setImplicitFlowEnabled(false);
                    resource.setDirectAccessGrantsEnabled(false);
                    resource.setServiceAccountsEnabled(false);
                    resource.setBearerOnly(false);
                    break;
                case Constants.REST_API:
                    resource.setDirectAccessGrantsEnabled(true);
                    resource.setStandardFlowEnabled(false);
                    resource.setImplicitFlowEnabled(false);
                    resource.setServiceAccountsEnabled(false);
                    resource.setBearerOnly(false);
                    break;
                case Constants.M2M:
                    resource.setServiceAccountsEnabled(true);
                    resource.setDirectAccessGrantsEnabled(false);
                    resource.setStandardFlowEnabled(false);
                    resource.setImplicitFlowEnabled(false);
                    resource.setPublicClient(false);
                    resource.setBearerOnly(false);
                    break;
                case Constants.BROWSER_BASED:
                case Constants.IWS:
                    resource.setImplicitFlowEnabled(true);
                    resource.setStandardFlowEnabled(false);
                    resource.setDirectAccessGrantsEnabled(false);
                    resource.setServiceAccountsEnabled(false);
                    resource.setBearerOnly(false);
                    break;
                case CIBA:
                    resource.setStandardFlowEnabled(true);
                    resource.setImplicitFlowEnabled(false);
                    resource.setDirectAccessGrantsEnabled(false);
                    resource.setServiceAccountsEnabled(false);
                    resource.setBearerOnly(false);
                    resource.setAttribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, "true");
                    resource.setPublicClient(false);
                    resource.setSandboxMode(rep.getSandboxMode());
                    break;
            }
        }
        //Here we set the attribute type to identity it as a IWS app, but default it will be browser based app
        if (rep.getAppType().equals(Constants.IWS)) {
            resource.setAttribute("iws.enable", "true");
            if (!rep.getAttributes().containsKey("iws.credtype")) {
                resource.setAttribute("iws.credtype", "iws_user_set_pass");
            }
            resource.setAttribute("vault", "true");
            resource.setAttribute("sso.show", "true");
        }

        if (rep.getNotBefore() != null) {
            resource.setNotBefore(rep.getNotBefore());
        }
        if (rep.getDefaultRoles() != null) {
            resource.updateDefaultRoles(rep.getDefaultRoles());
        }

        addRedirectUrisAndSetupWebOrigins(resource, rep);

        if (rep.getRegisteredNodes() != null) {
            for (Map.Entry<String, Integer> entry : rep.getRegisteredNodes().entrySet()) {
                resource.registerNode(entry.getKey(), entry.getValue());
            }
        }

        if (rep.getMfaPolicyId() != null) {
            resource.setMfaPolicyId(rep.getMfaPolicyId());
            resource.setMfaPolicyType(rep.getMfaPolicyType());
        }

        if (resource.isPublicClient() || resource.isBearerOnly()) {
            resource.setSecret(null);
        } else {
            String currentSecret = resource.getSecret();
            String newSecret = rep.getSecret();

            if (newSecret == null && currentSecret == null) {
                KeycloakModelUtils.generateSecret(resource);
            } else if (newSecret != null) {
                resource.setSecret(newSecret);
            }
        }
        resource.setOverrideAuthentication(rep.getOverrideAuthentication());
        resource.setLastUpdatedTimestamp(System.currentTimeMillis());
        resource.updateClient();
    }


    private static IdentityProviderModel checkIfConnectionPresent(RealmModel realm, String connId) {
        return realm.getIdentityProvidersStream()
                .filter(model -> Objects.equals(model.getProviderId(), connId))
                .findFirst()
                .orElse(null);
    }

    private static String checkIfMFAPresent(RealmModel realm, String connId) {
        if (connId.equals("is_mandatory")) {
            return "is_mandatory";
        }
        return realm.getMFAProvidersStream()
                .filter(model -> model.equals(connId))
                .findFirst()
                .orElse(null);
    }

    public static void addRedirectUrisAndSetupWebOrigins(ClientModel client, ClientRepresentation rep) {
        if (rep.getRedirectUris() != null && rep.getRedirectUris().size() > 0) {
            Set<String> redirectUris = new HashSet<String>();
            Set<String> origins = new HashSet<String>();
            for (String redirectUri : rep.getRedirectUris()) {
                //to check if client is inBuilt
                if (client.getAttribute("isInBuilt") != null && client.getAttribute("isInBuilt").equalsIgnoreCase("true")) {
                    redirectUris.add(redirectUri);
                } else {
                    if (redirectUri.startsWith("http") || redirectUri.endsWith("*") || redirectUri.startsWith("winlogon://")) {

                        redirectUris.add(redirectUri);

                        //we set the webOrigins internally from redirectUri
                        String origin = UriUtils.getOrigin(redirectUri);
                        origins.add(origin);
                    } else {
                        if (rep.getAppType().equalsIgnoreCase("saml") && rep.getProtocol().equalsIgnoreCase("saml")) {
                            redirectUris.clear();
                            origins.clear();
                        } else if (rep.getAppType().equals(Constants.IWS)) {
                            throw new RuntimeException("invalid-app-login-url");
                        } else {
                            throw new RuntimeException("invalid-redirect-url");
                        }
                    }
                }
            }
            if (redirectUris.size() > 0) {
                client.setRedirectUris(redirectUris);
            } else {
                if (rep.getAppType().equals(Constants.IWS))
                    throw new RuntimeException("invalid-app-login-url");
                if (!rep.getAppType().equalsIgnoreCase("saml") && !rep.getProtocol().equalsIgnoreCase("saml")) {
                    throw new RuntimeException("invalid-redirect-url");
                }

            }

            if (origins.size() > 0) {
                client.setWebOrigins(origins);
            }
        }
    }

    public static void updateClientProtocolMappers(ClientRepresentation rep, ClientModel resource) {

        if (rep.getProtocolMappers() != null) {
            Map<String, ProtocolMapperModel> existingProtocolMappers =
                    resource.getProtocolMappersStream().collect(Collectors.toMap(mapper ->
                            generateProtocolNameKey(mapper.getProtocol(), mapper.getName()), Function.identity()));


            for (ProtocolMapperRepresentation protocolMapperRepresentation : rep.getProtocolMappers()) {
                String protocolNameKey = generateProtocolNameKey(protocolMapperRepresentation.getProtocol(), protocolMapperRepresentation.getName());
                ProtocolMapperModel existingMapper = existingProtocolMappers.get(protocolNameKey);
                if (existingMapper != null) {
                    ProtocolMapperModel updatedProtocolMapperModel = toModel(protocolMapperRepresentation);
                    updatedProtocolMapperModel.setId(existingMapper.getId());
                    resource.updateProtocolMapper(updatedProtocolMapperModel);

                    existingProtocolMappers.remove(protocolNameKey);

                } else {
                    resource.addProtocolMapper(toModel(protocolMapperRepresentation));
                }
            }

            for (Map.Entry<String, ProtocolMapperModel> entryToDelete : existingProtocolMappers.entrySet()) {
                resource.removeProtocolMapper(entryToDelete.getValue());
            }
        }
    }

    private static String generateProtocolNameKey(String protocol, String name) {
        return String.format("%s%%%s", protocol, name);
    }

    // CLIENT SCOPES

    private static Map<String, ClientScopeModel> createClientScopes(KeycloakSession session, List<ClientScopeRepresentation> clientScopes, RealmModel realm) {
        Map<String, ClientScopeModel> appMap = new HashMap<>();
        for (ClientScopeRepresentation resourceRep : clientScopes) {
            ClientScopeModel app = createClientScope(session, realm, resourceRep);
            appMap.put(app.getName(), app);
        }
        return appMap;
    }

    public static ClientScopeModel createClientScope(KeycloakSession session, RealmModel realm, ClientScopeRepresentation resourceRep) {
        logger.debugv("Create client scope: {0}", resourceRep.getName());

        ClientScopeModel clientScope = resourceRep.getId() != null ? realm.addClientScope(resourceRep.getId(), resourceRep.getName()) : realm.addClientScope(resourceRep.getName());
        if (resourceRep.getName() != null) clientScope.setName(resourceRep.getName());
        if (resourceRep.getDescription() != null) clientScope.setDescription(resourceRep.getDescription());
        if (resourceRep.getProtocol() != null) clientScope.setProtocol(resourceRep.getProtocol());
        if (resourceRep.getProtocolMappers() != null) {
            // first, remove all default/built in mappers
            clientScope.getProtocolMappersStream().collect(Collectors.toList()).forEach(clientScope::removeProtocolMapper);

            for (ProtocolMapperRepresentation mapper : resourceRep.getProtocolMappers()) {
                clientScope.addProtocolMapper(toModel(mapper));
            }
            MigrationUtils.updateProtocolMappers(clientScope);
        }

        if (resourceRep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : resourceRep.getAttributes().entrySet()) {
                clientScope.setAttribute(entry.getKey(), entry.getValue());
            }
        }


        return clientScope;
    }

    public static void updateClientScope(ClientScopeRepresentation rep, ClientScopeModel resource) {
        if (rep.getName() != null) resource.setName(rep.getName());
        if (rep.getDescription() != null) resource.setDescription(rep.getDescription());


        if (rep.getProtocol() != null) resource.setProtocol(rep.getProtocol());

        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                resource.setAttribute(entry.getKey(), entry.getValue());
            }
        }

    }

    public static long getClaimsMask(ClaimRepresentation rep) {
        long mask = ClaimMask.ALL;

        if (rep.getAddress()) {
            mask |= ClaimMask.ADDRESS;
        } else {
            mask &= ~ClaimMask.ADDRESS;
        }
        if (rep.getEmail()) {
            mask |= ClaimMask.EMAIL;
        } else {
            mask &= ~ClaimMask.EMAIL;
        }
        if (rep.getGender()) {
            mask |= ClaimMask.GENDER;
        } else {
            mask &= ~ClaimMask.GENDER;
        }
        if (rep.getLocale()) {
            mask |= ClaimMask.LOCALE;
        } else {
            mask &= ~ClaimMask.LOCALE;
        }
        if (rep.getName()) {
            mask |= ClaimMask.NAME;
        } else {
            mask &= ~ClaimMask.NAME;
        }
        if (rep.getPhone()) {
            mask |= ClaimMask.PHONE;
        } else {
            mask &= ~ClaimMask.PHONE;
        }
        if (rep.getPicture()) {
            mask |= ClaimMask.PICTURE;
        } else {
            mask &= ~ClaimMask.PICTURE;
        }
        if (rep.getProfile()) {
            mask |= ClaimMask.PROFILE;
        } else {
            mask &= ~ClaimMask.PROFILE;
        }
        if (rep.getUsername()) {
            mask |= ClaimMask.USERNAME;
        } else {
            mask &= ~ClaimMask.USERNAME;
        }
        if (rep.getWebsite()) {
            mask |= ClaimMask.WEBSITE;
        } else {
            mask &= ~ClaimMask.WEBSITE;
        }
        return mask;
    }

    // Scope mappings

    public static void createClientScopeMappings(RealmModel realm, ClientModel clientModel, List<ScopeMappingRepresentation> mappings) {
        for (ScopeMappingRepresentation mapping : mappings) {
            ScopeContainerModel scopeContainer = getScopeContainerHavingScope(realm, mapping);

            for (String roleString : mapping.getRoles()) {
                RoleModel role = clientModel.getRole(roleString.trim());
                if (role == null) {
                    role = clientModel.addRole(roleString.trim());
                }
                scopeContainer.addScopeMapping(role);
            }
        }
    }

    private static ScopeContainerModel getScopeContainerHavingScope(RealmModel realm, ScopeMappingRepresentation scope) {
        if (scope.getClient() != null) {
            ClientModel client = realm.getClientByClientId(scope.getClient());
            if (client == null) {
                throw new RuntimeException("Unknown client specification in scope mappings: " + scope.getClient());
            }
            return client;
        } else if (scope.getClientScope() != null) {
            ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(realm, scope.getClientScope());
            if (clientScope == null) {
                throw new RuntimeException("Unknown clientScope specification in scope mappings: " + scope.getClientScope());
            }
            return clientScope;
        } else if (scope.getClientTemplate() != null) { // Backwards compatibility
            String templateName = KeycloakModelUtils.convertClientScopeName(scope.getClientTemplate());
            ClientScopeModel clientTemplate = KeycloakModelUtils.getClientScopeByName(realm, templateName);
            if (clientTemplate == null) {
                throw new RuntimeException("Unknown clientScope specification in scope mappings: " + templateName);
            }
            return clientTemplate;
        } else {
            throw new RuntimeException("Either client or clientScope needs to be specified in scope mappings");
        }
    }

    // Users

    public static UserModel createUser(KeycloakSession session, RealmModel newRealm, UserRepresentation userRep) {
        convertDeprecatedSocialProviders(userRep);

        // Import users just to user storage. Don't federate
        UserModel user = session.userLocalStorage().addUser(newRealm, userRep.getId(), userRep.getUsername(), false, false);
        user.setEnabled(userRep.isEnabled() != null && userRep.isEnabled());
        user.setCreatedTimestamp(userRep.getCreatedTimestamp());
        user.setEmail(userRep.getEmail());
        if (userRep.isEmailVerified() != null) user.setEmailVerified(userRep.isEmailVerified());
        user.setFirstName(userRep.getFirstName());
        user.setLastName(userRep.getLastName());
        user.setMobileNumber(userRep.getMobileNumber());
        user.setMobileNumberRegion(userRep.getMobileNumberRegion());
        user.setFederationLink(userRep.getFederationLink());
        if (userRep.getAttributes() != null) {
            for (Map.Entry<String, List<String>> entry : userRep.getAttributes().entrySet()) {
                List<String> value = entry.getValue();
                if (value != null) {
                    user.setAttribute(entry.getKey(), new ArrayList<>(value));
                }
            }
        }
        if (userRep.getRequiredActions() != null) {
            for (String requiredAction : userRep.getRequiredActions()) {
                try {
                    user.addRequiredAction(UserModel.RequiredAction.valueOf(requiredAction.toUpperCase()));
                } catch (IllegalArgumentException iae) {
                    user.addRequiredAction(requiredAction);
                }
            }
        }
        createCredentials(userRep, session, newRealm, user, false);
        createFederatedIdentities(userRep, session, newRealm, user);
        createRoleMappings(userRep, user, newRealm);
        if (userRep.getClientConsents() != null) {
            for (UserConsentRepresentation consentRep : userRep.getClientConsents()) {
                UserConsentModel consentModel = toModel(newRealm, consentRep);
                session.users().addConsent(newRealm, user.getId(), consentModel);
            }
        }

        if (userRep.getNotBefore() != null) {
            session.users().setNotBeforeForUser(newRealm, user, userRep.getNotBefore());
        }

        if (userRep.getServiceAccountClientId() != null) {
            String clientId = userRep.getServiceAccountClientId();
            ClientModel client = newRealm.getClientByClientId(clientId);
            if (client == null) {
                throw new RuntimeException("Unable to find client specified for service account link. Client: " + clientId);
            }
            user.setServiceAccountClientLink(client.getId());
        }
        createGroups(userRep, newRealm, user);
        return user;
    }

    public static void createGroups(UserRepresentation userRep, RealmModel newRealm, UserModel user) {
        if (userRep.getGroups() != null) {
            for (String path : userRep.getGroups()) {
                GroupModel group = KeycloakModelUtils.findGroupByPath(newRealm, path);
                if (group == null) {
                    throw new RuntimeException("Unable to find group specified by path: " + path);

                }
                user.joinGroup(group);
            }
        }
    }

    public static void createFederatedIdentities(UserRepresentation userRep, KeycloakSession session, RealmModel realm, UserModel user) {
        if (userRep.getFederatedIdentities() != null) {
            for (FederatedIdentityRepresentation identity : userRep.getFederatedIdentities()) {
                FederatedIdentityModel mappingModel = new FederatedIdentityModel(identity.getIdentityProvider(), identity.getUserId(), identity.getUserName());
                session.users().addFederatedIdentity(realm, user, mappingModel);
            }
        }
    }

    public static void createCredentials(UserRepresentation userRep, KeycloakSession session, RealmModel realm, UserModel user, boolean adminRequest) {
        convertDeprecatedCredentialsFormat(userRep);
        if (userRep.getCredentials() != null) {
            for (CredentialRepresentation cred : userRep.getCredentials()) {
                if (cred.getId() != null && session.userCredentialManager().getStoredCredentialById(realm, user, cred.getId()) != null) {
                    continue;
                }
                if (cred.getValue() != null && !cred.getValue().isEmpty()) {
                    RealmModel origRealm = session.getContext().getRealm();
                    try {
                        session.getContext().setRealm(realm);
                        session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password(cred.getValue(), cred.getUserLabel(), cred.isTemporary()));
                    } catch (ModelException ex) {
                        throw new PasswordPolicyNotMetException(ex.getMessage(), user.getUsername(), ex);
                    } finally {
                        session.getContext().setRealm(origRealm);
                    }
                } else {
                    session.userCredentialManager().createCredentialThroughProvider(realm, user, toModel(cred));
                }
            }
        }
    }

    public static EventConfigModel toModel(EventConfigRepresentation rep) {
        EventConfigModel model = new EventConfigModel();
        model.setEventProviderId(rep.getEventProviderId());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static EmailConfigModel toModel(EmailConfigRepresentation rep) {
        EmailConfigModel model = new EmailConfigModel();
        model.setEmailProviderId(rep.getEmailProviderId());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static SmsConfigModel toModel(SmsConfigRepresentation rep) {
        SmsConfigModel model = new SmsConfigModel();
        model.setSmsProviderId(rep.getSmsProviderId());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static MFAConfigModel toModel(MFAConfigRepresentation rep) {
        MFAConfigModel model = new MFAConfigModel();
        model.setMfaProviderId(rep.getMfaProviderId());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static CibaAuthConfigModel toModel(CibaAuthConfigRepresentation rep) {
        CibaAuthConfigModel model = new CibaAuthConfigModel();
        model.setCibaAuthProviderId(rep.getProviderId());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static CibaNotificationConfigModel toModel(CibaNotificationConfigRepresentation rep) {
        CibaNotificationConfigModel model = new CibaNotificationConfigModel();
        model.setCibaNotificationProviderId(rep.getProviderId());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }


    public static CibaDeadLetterHookConfigModel toModel(DeadLetterHookNotificationConfigRepresentation rep) {
        CibaDeadLetterHookConfigModel model = new CibaDeadLetterHookConfigModel();
        model.setCibaResultHookProviderId(rep.getProviderId());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static CredentialModel toModel(CredentialRepresentation cred) {
        CredentialModel model = new CredentialModel();
        model.setCreatedDate(cred.getCreatedDate());
        model.setType(cred.getType());
        model.setUserLabel(cred.getUserLabel());
        model.setSecretData(cred.getSecretData());
        model.setCredentialData(cred.getCredentialData());
        model.setId(cred.getId());
        model.setEnabled(cred.isEnabled());
        return model;
    }

    public static PolicyModel toModel(RealmPolicyRepresentation rep) {
        PolicyModel model = new PolicyModel();

        model.setPolicyName(rep.getPolicyName());
        model.setPolicyType(rep.getPolicyType());
        model.setPolicyValue(rep.getPolicyValue());
        model.setRealmPolicy(rep.isRealmPolicy());

        return model;
    }

    // Role mappings

    public static void createRoleMappings(UserRepresentation userRep, UserModel user, RealmModel realm) {
        if (userRep.getRealmRoles() != null) {
            for (String roleString : userRep.getRealmRoles()) {
                RoleModel role = realm.getRole(roleString.trim());
                if (role == null) {
                    role = realm.addRole(roleString.trim());
                }
                user.grantRole(role);
            }
        }
        if (userRep.getClientRoles() != null) {
            for (Map.Entry<String, List<String>> entry : userRep.getClientRoles().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                createClientRoleMappings(client, user, entry.getValue());
            }
        }
    }

    public static void createClientRoleMappings(ClientModel clientModel, UserModel user, List<String> roleNames) {
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        for (String roleName : roleNames) {
            RoleModel role = clientModel.getRole(roleName.trim());
            if (role == null) {
                role = clientModel.addRole(roleName.trim());
            }
            user.grantRole(role);

        }
    }

    private static void importIdentityProviders(RealmRepresentation rep, RealmModel newRealm, KeycloakSession session) {
        if (rep.getIdentityProviders() != null) {
            for (IdentityProviderRepresentation representation : rep.getIdentityProviders()) {
                newRealm.addIdentityProvider(toModel(newRealm, representation, session));
            }
        }
    }

    private static void importIdentityProviderMappers(RealmRepresentation rep, RealmModel newRealm) {
        if (rep.getIdentityProviderMappers() != null) {
            for (IdentityProviderMapperRepresentation representation : rep.getIdentityProviderMappers()) {
                newRealm.addIdentityProviderMapper(toModel(representation));
            }
        }
    }

    public static IdentityProviderModel toModel(RealmModel realm, IdentityProviderRepresentation representation, KeycloakSession session) {
        IdentityProviderFactory providerFactory = (IdentityProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(
                IdentityProvider.class, representation.getProviderId());

        if (providerFactory == null) {
            providerFactory = (IdentityProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(
                    SocialIdentityProvider.class, representation.getProviderId());
        }

        if (providerFactory == null) {
            throw new IllegalArgumentException("Invalid identity provider id [" + representation.getProviderId() + "]");
        }

        IdentityProviderModel identityProviderModel = providerFactory.createConfig();

        identityProviderModel.setInternalId(representation.getInternalId());
        identityProviderModel.setAlias(representation.getAlias());
        identityProviderModel.setDisplayName(representation.getDisplayName());
        identityProviderModel.setProviderId(representation.getProviderId());
        identityProviderModel.setEnabled(representation.isEnabled());
        identityProviderModel.setLinkOnly(representation.isLinkOnly());
        identityProviderModel.setTrustEmail(representation.isTrustEmail());
        identityProviderModel.setAuthenticateByDefault(representation.isAuthenticateByDefault());
        identityProviderModel.setStoreToken(representation.isStoreToken());
        identityProviderModel.setAddReadTokenRoleOnCreate(representation.isAddReadTokenRoleOnCreate());
        identityProviderModel.setConfig(removeEmptyString(representation.getConfig()));

        String flowAlias = representation.getFirstBrokerLoginFlowAlias();
        if (flowAlias == null) {
            flowAlias = DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW;
        }

        AuthenticationFlowModel flowModel = realm.getFlowByAlias(flowAlias);
        if (flowModel == null) {
            throw new ModelException("No available authentication flow with alias: " + flowAlias);
        }
        identityProviderModel.setFirstBrokerLoginFlowId(flowModel.getId());

        flowAlias = representation.getPostBrokerLoginFlowAlias();
        if (flowAlias == null || flowAlias.trim().length() == 0) {
            identityProviderModel.setPostBrokerLoginFlowId(null);
        } else {
            flowModel = realm.getFlowByAlias(flowAlias);
            if (flowModel == null) {
                throw new ModelException("No available authentication flow with alias: " + flowAlias);
            }
            identityProviderModel.setPostBrokerLoginFlowId(flowModel.getId());
        }

        //To get the Email Sender from Social Connection Config
        //Purpose of EmailSender is for sending verification email to Link two Social Accounts with same email address.
        String emailSender = representation.getEmailSenderId();
        if (emailSender == null || emailSender.trim().length() == 0) {
            emailSender = "default";
        }
        identityProviderModel.setEmailSenderId(emailSender);
        identityProviderModel.setInBuilt(representation.isInBuilt());

        identityProviderModel.validate(realm);

        return identityProviderModel;
    }

    public static ProtocolMapperModel toModel(ProtocolMapperRepresentation rep) {
        ProtocolMapperModel model = new ProtocolMapperModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setProtocol(rep.getProtocol());
        model.setProtocolMapper(rep.getProtocolMapper());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static MgmtAccessModel toModel(MgmtAccessRepresentation rep,RealmModel realmModel) {
        MgmtAccessModel model = new MgmtAccessModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setPermissions(rep.getPermissions());
        model.setExpiryDate(rep.getExpiryDate());
        model.setCreatedDate(rep.getCreatedDate());
        model.setEnabled(rep.isEnabled());
        model.setRealmId(realmModel.getId());
        model.setRequestedBy(rep.getRequestedBy().getId());
        return model;
    }

    public static IdentityProviderMapperModel toModel(IdentityProviderMapperRepresentation rep) {
        IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setIdentityProviderAlias(rep.getIdentityProviderAlias());
        model.setIdentityProviderMapper(rep.getIdentityProviderMapper());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static UserConsentModel toModel(RealmModel newRealm, UserConsentRepresentation consentRep) {
        ClientModel client = newRealm.getClientByClientId(consentRep.getClientId());
        if (client == null) {
            throw new RuntimeException("Unable to find client consent mappings for client: " + consentRep.getClientId());
        }

        UserConsentModel consentModel = new UserConsentModel(client);
        consentModel.setCreatedDate(consentRep.getCreatedDate());
        consentModel.setLastUpdatedDate(consentRep.getLastUpdatedDate());

        if (consentRep.getGrantedClientScopes() != null) {
            for (String scopeName : consentRep.getGrantedClientScopes()) {
                ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(newRealm, scopeName);
                if (clientScope == null) {
                    throw new RuntimeException("Unable to find client scope referenced in consent mappings of user. Client scope name: " + scopeName);
                }
                consentModel.addGrantedClientScope(clientScope);
            }
        }

        // Backwards compatibility. If user had consent for "offline_access" role, we treat it as he has consent for "offline_access" client scope
        if (consentRep.getGrantedRealmRoles() != null) {
            if (consentRep.getGrantedRealmRoles().contains(OAuth2Constants.OFFLINE_ACCESS)) {
                ClientScopeModel offlineScope = client.getClientScopes(false, true).get(OAuth2Constants.OFFLINE_ACCESS);
                if (offlineScope == null) {
                    logger.warn("Unable to find offline_access scope referenced in grantedRoles of user");
                }
                consentModel.addGrantedClientScope(offlineScope);
            }
        }

        return consentModel;
    }

    public static AuthenticationFlowModel toModel(AuthenticationFlowRepresentation rep) {
        AuthenticationFlowModel model = new AuthenticationFlowModel();
        model.setId(rep.getId());
        model.setBuiltIn(rep.isBuiltIn());
        model.setHidden(rep.isHidden());
        model.setInstalled(rep.isInstalled());
        model.setTemplateId(rep.getTemplateId());
        model.setTemplateAlias(rep.getTemplateAlias());
        model.setTemplateType(rep.getTemplateType());
        model.setTopLevel(rep.isTopLevel());
        model.setProviderId(rep.getProviderId());
        model.setAlias(rep.getAlias());
        model.setDescription(rep.getDescription());
        return model;

    }

    private static AuthenticationExecutionModel toModel(RealmModel realm, AuthenticationFlowModel parentFlow, AuthenticationExecutionExportRepresentation rep) {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        if (rep.getAuthenticatorConfig() != null) {
            AuthenticatorConfigModel config = realm.getAuthenticatorConfigByAlias(rep.getAuthenticatorConfig());
            model.setAuthenticatorConfig(config.getId());
        }
        model.setAuthenticator(rep.getAuthenticator());
        model.setAuthenticatorFlow(rep.isAuthenticatorFlow());
        if (rep.getFlowAlias() != null) {
            AuthenticationFlowModel flow = realm.getFlowByAlias(rep.getFlowAlias());
            model.setFlowId(flow.getId());
        }
        model.setPriority(rep.getPriority());
        try {
            model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));
            model.setParentFlow(parentFlow.getId());
        } catch (IllegalArgumentException iae) {
            //retro-compatible for previous OPTIONAL being changed to CONDITIONAL
            if ("OPTIONAL".equals(rep.getRequirement())) {
                MigrateTo8_0_0.migrateOptionalAuthenticationExecution(realm, parentFlow, model, false);
            }
        }
        return model;
    }

    public static AuthenticationExecutionModel toModel(RealmModel realm, AuthenticationExecutionRepresentation rep) {
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(rep.getId());
        model.setFlowId(rep.getFlowId());

        model.setAuthenticator(rep.getAuthenticator());
        model.setHidden(rep.isHidden());
        model.setPriority(rep.getPriority());
        model.setParentFlow(rep.getParentFlow());
        model.setAuthenticatorFlow(rep.isAuthenticatorFlow());
        model.setRequirement(AuthenticationExecutionModel.Requirement.valueOf(rep.getRequirement()));

        if (rep.getAuthenticatorConfig() != null) {
            AuthenticatorConfigModel cfg = realm.getAuthenticatorConfigByAlias(rep.getAuthenticatorConfig());
            model.setAuthenticatorConfig(cfg.getId());
        }
        return model;
    }

    public static AuthenticatorConfigModel toModel(AuthenticatorConfigRepresentation rep) {
        AuthenticatorConfigModel model = new AuthenticatorConfigModel();
        model.setAlias(rep.getAlias());
        model.setConfig(removeEmptyString(rep.getConfig()));
        return model;
    }

    public static RequiredActionProviderModel toModel(RequiredActionProviderRepresentation rep) {
        RequiredActionProviderModel model = new RequiredActionProviderModel();
        model.setConfig(removeEmptyString(rep.getConfig()));
        model.setPriority(rep.getPriority());
        model.setDefaultAction(rep.isDefaultAction());
        model.setEnabled(rep.isEnabled());
        model.setProviderId(rep.getProviderId());
        model.setName(rep.getName());
        model.setAlias(rep.getAlias());
        model.setPolicyId(rep.getPolicyId());
        model.setPolicyType(rep.getPolicyType());
        return model;
    }

    public static ComponentModel toModel(KeycloakSession session, ComponentRepresentation rep) {
        ComponentModel model = new ComponentModel();
        model.setId(rep.getId());
        model.setParentId(rep.getParentId());
        model.setProviderType(rep.getProviderType());
        model.setProviderId(rep.getProviderId());
        model.setConfig(new MultivaluedHashMap<>());
        model.setName(rep.getName());
        model.setSubType(rep.getSubType());

        if (rep.getConfig() != null) {
            Set<String> keys = new HashSet<>(rep.getConfig().keySet());
            for (String k : keys) {
                List<String> values = rep.getConfig().get(k);
                if (values != null) {
                    ListIterator<String> itr = values.listIterator();
                    while (itr.hasNext()) {
                        String v = itr.next();
                        if (v == null || v.trim().isEmpty()) {
                            itr.remove();
                        }
                    }

                    if (!values.isEmpty()) {
                        model.getConfig().put(k, values);
                    }
                }
            }
        }

        return model;
    }

    public static void updateComponent(KeycloakSession session, ComponentRepresentation rep, ComponentModel component, boolean internal, boolean componentInUse) {
        if (rep.getName() != null) {
            component.setName(rep.getName());
        }

        if (rep.getParentId() != null) {
            component.setParentId(rep.getParentId());
        }

        if (rep.getProviderType() != null) {
            component.setProviderType(rep.getProviderType());
        }

        if (rep.getProviderId() != null) {
            component.setProviderId(rep.getProviderId());
        }

        if (rep.getSubType() != null) {
            component.setSubType(rep.getSubType());
        }

        Map<String, ProviderConfigProperty> providerConfiguration = null;
        if (!internal) {
            providerConfiguration = ComponentUtil.getComponentConfigProperties(session, component);
        }

        if (rep.getConfig() != null) {
            Set<String> keys = new HashSet<>(rep.getConfig().keySet());
            for (String k : keys) {
                if (componentInUse && (k.equalsIgnoreCase("active") || k.equalsIgnoreCase("enabled"))) {
                    if (componentInUse && !component.getConfig().containsKey(k)) {
                        component.getConfig().putSingle(k, "true");
                    }
                    if (component.getConfig().containsKey(k) && !rep.getConfig().get(k).equals(component.getConfig().get(k))) {
                        throw new RuntimeException("key-not-updated");
                    }
                }
                List<String> values = rep.getConfig().get(k);
                if (values == null || values.isEmpty() || values.get(0) == null || values.get(0).trim().isEmpty()) {
                    component.getConfig().remove(k);
                } else {
                    ListIterator<String> itr = values.listIterator();
                    while (itr.hasNext()) {
                        String v = itr.next();
                        if (v == null || v.trim().isEmpty() || v.equals(ComponentRepresentation.SECRET_VALUE)) {
                            itr.remove();
                        }
                    }

                    if (!values.isEmpty()) {
                        component.getConfig().put(k, values);
                    }
                }
            }
        }
    }

    public static void importRealmAuthorizationSettings(RealmRepresentation rep, RealmModel newRealm, KeycloakSession session) {
        if (rep.getClients() != null) {
            rep.getClients().forEach(clientRepresentation -> {
                ClientModel client = newRealm.getClientByClientId(clientRepresentation.getClientId());
                importAuthorizationSettings(clientRepresentation, client, session);
            });
        }
    }

    public static void importAuthorizationSettings(ClientRepresentation clientRepresentation, ClientModel client, KeycloakSession session) {
        if (Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION) && Boolean.TRUE.equals(clientRepresentation.getAuthorizationServicesEnabled())) {
            AuthorizationProviderFactory authorizationFactory = (AuthorizationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
            AuthorizationProvider authorization = authorizationFactory.create(session, client.getRealm());

            client.setServiceAccountsEnabled(true);
            client.setBearerOnly(false);
            client.setPublicClient(false);

            ResourceServerRepresentation rep = clientRepresentation.getAuthorizationSettings();

            if (rep == null) {
                rep = new ResourceServerRepresentation();
            }

            rep.setClientId(client.getId());

            toModel(rep, authorization, client);
        }
    }

    public static ResourceServer toModel(ResourceServerRepresentation rep, AuthorizationProvider authorization, ClientModel client) {
        ResourceServerStore resourceServerStore = authorization.getStoreFactory().getResourceServerStore();
        ResourceServer resourceServer;
        ResourceServer existing = resourceServerStore.findByClient(client);

        if (existing == null) {
            resourceServer = resourceServerStore.create(client);
            resourceServer.setAllowRemoteResourceManagement(true);
            resourceServer.setPolicyEnforcementMode(PolicyEnforcementMode.ENFORCING);
        } else {
            resourceServer = existing;
        }

        resourceServer.setPolicyEnforcementMode(rep.getPolicyEnforcementMode());
        resourceServer.setAllowRemoteResourceManagement(rep.isAllowRemoteResourceManagement());

        DecisionStrategy decisionStrategy = rep.getDecisionStrategy();

        if (decisionStrategy == null) {
            decisionStrategy = DecisionStrategy.UNANIMOUS;
        }

        resourceServer.setDecisionStrategy(decisionStrategy);

        for (ScopeRepresentation scope : rep.getScopes()) {
            toModel(scope, resourceServer, authorization);
        }

        KeycloakSession session = authorization.getKeycloakSession();
        RealmModel realm = authorization.getRealm();

        for (ResourceRepresentation resource : rep.getResources()) {
            ResourceOwnerRepresentation owner = resource.getOwner();

            if (owner == null) {
                owner = new ResourceOwnerRepresentation();
                owner.setId(resourceServer.getClientId());
                resource.setOwner(owner);
            } else if (owner.getName() != null) {
                UserModel user = session.users().getUserByUsername(realm, owner.getName());

                if (user != null) {
                    owner.setId(user.getId());
                }
            }

            toModel(resource, resourceServer, authorization);
        }

        importPolicies(authorization, resourceServer, rep.getPolicies(), null);

        return resourceServer;
    }

    private static Policy importPolicies(AuthorizationProvider authorization, ResourceServer resourceServer, List<PolicyRepresentation> policiesToImport, String parentPolicyName) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        for (PolicyRepresentation policyRepresentation : policiesToImport) {
            if (parentPolicyName != null && !parentPolicyName.equals(policyRepresentation.getName())) {
                continue;
            }

            Map<String, String> config = policyRepresentation.getConfig();
            String applyPolicies = config.get("applyPolicies");

            if (applyPolicies != null && !applyPolicies.isEmpty()) {
                PolicyStore policyStore = storeFactory.getPolicyStore();
                try {
                    List<String> policies = (List<String>) JsonSerialization.readValue(applyPolicies, List.class);
                    Set<String> policyIds = new HashSet<>();

                    for (String policyName : policies) {
                        Policy policy = policyStore.findByName(resourceServer, policyName);

                        if (policy == null) {
                            policy = policyStore.findById(resourceServer, policyName);
                        }

                        if (policy == null) {
                            policy = importPolicies(authorization, resourceServer, policiesToImport, policyName);
                            if (policy == null) {
                                throw new RuntimeException("Policy with name [" + policyName + "] not defined.");
                            }
                        }

                        policyIds.add(policy.getId());
                    }

                    config.put("applyPolicies", JsonSerialization.writeValueAsString(policyIds));
                } catch (Exception e) {
                    throw new RuntimeException("Error while importing policy [" + policyRepresentation.getName() + "].", e);
                }
            }

            PolicyStore policyStore = storeFactory.getPolicyStore();
            Policy policy = policyStore.findById(resourceServer, policyRepresentation.getId());

            if (policy == null) {
                policy = policyStore.findByName(resourceServer, policyRepresentation.getName());
            }

            if (policy == null) {
                policy = policyStore.create(resourceServer, policyRepresentation);
            } else {
                policy = toModel(policyRepresentation, authorization, policy);
            }

            if (parentPolicyName != null && parentPolicyName.equals(policyRepresentation.getName())) {
                return policy;
            }
        }

        return null;
    }

    public static Policy toModel(AbstractPolicyRepresentation representation, AuthorizationProvider authorization, Policy model) {
        model.setName(representation.getName());
        model.setDescription(representation.getDescription());
        model.setDecisionStrategy(representation.getDecisionStrategy());
        model.setLogic(representation.getLogic());

        Set resources = representation.getResources();
        Set scopes = representation.getScopes();
        Set policies = representation.getPolicies();

        if (representation instanceof PolicyRepresentation) {
            PolicyRepresentation policy = PolicyRepresentation.class.cast(representation);

            if (resources == null) {
                String resourcesConfig = policy.getConfig().get("resources");

                if (resourcesConfig != null) {
                    try {
                        resources = JsonSerialization.readValue(resourcesConfig, Set.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (scopes == null) {
                String scopesConfig = policy.getConfig().get("scopes");

                if (scopesConfig != null) {
                    try {
                        scopes = JsonSerialization.readValue(scopesConfig, Set.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (policies == null) {
                String policiesConfig = policy.getConfig().get("applyPolicies");

                if (policiesConfig != null) {
                    try {
                        policies = JsonSerialization.readValue(policiesConfig, Set.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            model.setConfig(policy.getConfig());
        }

        StoreFactory storeFactory = authorization.getStoreFactory();

        updateResources(resources, model, storeFactory);
        updateScopes(scopes, model, storeFactory);
        updateAssociatedPolicies(policies, model, storeFactory);

        PolicyProviderFactory provider = authorization.getProviderFactory(model.getType());

        if (provider == null) {
            throw new RuntimeException("Could find policy provider with type [" + model.getType() + "]");
        }

        if (representation instanceof PolicyRepresentation) {
            provider.onImport(model, PolicyRepresentation.class.cast(representation), authorization);
        } else if (representation.getId() == null) {
            provider.onCreate(model, representation, authorization);
        } else {
            provider.onUpdate(model, representation, authorization);
        }


        representation.setId(model.getId());

        return model;
    }

    private static void updateScopes(Set<String> scopeIds, Policy policy, StoreFactory storeFactory) {
        if (scopeIds != null) {
            if (scopeIds.isEmpty()) {
                for (Scope scope : new HashSet<Scope>(policy.getScopes())) {
                    policy.removeScope(scope);
                }
                return;
            }
            for (String scopeId : scopeIds) {
                boolean hasScope = false;

                for (Scope scopeModel : new HashSet<Scope>(policy.getScopes())) {
                    if (scopeModel.getId().equals(scopeId) || scopeModel.getName().equals(scopeId)) {
                        hasScope = true;
                    }
                }
                if (!hasScope) {
                    ResourceServer resourceServer = policy.getResourceServer();
                    Scope scope = storeFactory.getScopeStore().findById(resourceServer, scopeId);

                    if (scope == null) {
                        scope = storeFactory.getScopeStore().findByName(resourceServer, scopeId);
                        if (scope == null) {
                            throw new RuntimeException("Scope with id or name [" + scopeId + "] does not exist");
                        }
                    }

                    policy.addScope(scope);
                }
            }

            for (Scope scopeModel : new HashSet<Scope>(policy.getScopes())) {
                boolean hasScope = false;

                for (String scopeId : scopeIds) {
                    if (scopeModel.getId().equals(scopeId) || scopeModel.getName().equals(scopeId)) {
                        hasScope = true;
                    }
                }
                if (!hasScope) {
                    policy.removeScope(scopeModel);
                }
            }
        }

        policy.removeConfig("scopes");
    }

    private static void updateAssociatedPolicies(Set<String> policyIds, Policy policy, StoreFactory storeFactory) {
        ResourceServer resourceServer = policy.getResourceServer();

        if (policyIds != null) {
            if (policyIds.isEmpty()) {
                for (Policy associated : new HashSet<Policy>(policy.getAssociatedPolicies())) {
                    policy.removeAssociatedPolicy(associated);
                }
                return;
            }

            PolicyStore policyStore = storeFactory.getPolicyStore();

            for (String policyId : policyIds) {
                boolean hasPolicy = false;

                for (Policy policyModel : new HashSet<Policy>(policy.getAssociatedPolicies())) {
                    if (policyModel.getId().equals(policyId) || policyModel.getName().equals(policyId)) {
                        hasPolicy = true;
                    }
                }

                if (!hasPolicy) {
                    Policy associatedPolicy = policyStore.findById(resourceServer, policyId);

                    if (associatedPolicy == null) {
                        associatedPolicy = policyStore.findByName(resourceServer, policyId);
                        if (associatedPolicy == null) {
                            throw new RuntimeException("Policy with id or name [" + policyId + "] does not exist");
                        }
                    }

                    policy.addAssociatedPolicy(associatedPolicy);
                }
            }

            for (Policy policyModel : new HashSet<Policy>(policy.getAssociatedPolicies())) {
                boolean hasPolicy = false;

                for (String policyId : policyIds) {
                    if (policyModel.getId().equals(policyId) || policyModel.getName().equals(policyId)) {
                        hasPolicy = true;
                    }
                }
                if (!hasPolicy) {
                    policy.removeAssociatedPolicy(policyModel);
                }
            }
        }

        policy.removeConfig("applyPolicies");
    }

    private static void updateResources(Set<String> resourceIds, Policy policy, StoreFactory storeFactory) {
        if (resourceIds != null) {
            if (resourceIds.isEmpty()) {
                for (Resource resource : new HashSet<>(policy.getResources())) {
                    policy.removeResource(resource);
                }
            }
            for (String resourceId : resourceIds) {
                boolean hasResource = false;
                for (Resource resourceModel : new HashSet<>(policy.getResources())) {
                    if (resourceModel.getId().equals(resourceId) || resourceModel.getName().equals(resourceId)) {
                        hasResource = true;
                    }
                }
                if (!hasResource && !"".equals(resourceId)) {
                    Resource resource = storeFactory.getResourceStore().findById(policy.getResourceServer(), resourceId);

                    if (resource == null) {
                        resource = storeFactory.getResourceStore().findByName(policy.getResourceServer(), resourceId);
                        if (resource == null) {
                            throw new RuntimeException("Resource with id or name [" + resourceId + "] does not exist or is not owned by the resource server");
                        }
                    }

                    policy.addResource(resource);
                }
            }

            for (Resource resourceModel : new HashSet<>(policy.getResources())) {
                boolean hasResource = false;

                for (String resourceId : resourceIds) {
                    if (resourceModel.getId().equals(resourceId) || resourceModel.getName().equals(resourceId)) {
                        hasResource = true;
                    }
                }

                if (!hasResource) {
                    policy.removeResource(resourceModel);
                }
            }
        }

        policy.removeConfig("resources");
    }

    public static Resource toModel(ResourceRepresentation resource, ResourceServer resourceServer, AuthorizationProvider authorization) {
        ResourceStore resourceStore = authorization.getStoreFactory().getResourceStore();
        ResourceOwnerRepresentation owner = resource.getOwner();

        if (owner == null) {
            owner = new ResourceOwnerRepresentation();
            owner.setId(resourceServer.getClientId());
        }

        String ownerId = owner.getId();

        if (ownerId == null) {
            ownerId = resourceServer.getClientId();
        }

        if (!resourceServer.getClientId().equals(ownerId)) {
            RealmModel realm = authorization.getRealm();
            KeycloakSession keycloakSession = authorization.getKeycloakSession();
            UserProvider users = keycloakSession.users();
            UserModel ownerModel = users.getUserById(realm, ownerId);

            if (ownerModel == null) {
                ownerModel = users.getUserByUsername(realm, ownerId);
            }

            if (ownerModel == null) {
                throw new RuntimeException("Owner must be a valid username or user identifier. If the resource server, the client id or null.");
            }

            ownerId = ownerModel.getId();
        }

        Resource existing;

        if (resource.getId() != null) {
            existing = resourceStore.findById(resourceServer, resource.getId());
        } else {
            existing = resourceStore.findByName(resourceServer, resource.getName(), ownerId);
        }

        if (existing != null) {
            existing.setName(resource.getName());
            existing.setDisplayName(resource.getDisplayName());
            existing.setType(resource.getType());
            existing.updateUris(resource.getUris());
            existing.setIconUri(resource.getIconUri());
            existing.setOwnerManagedAccess(Boolean.TRUE.equals(resource.getOwnerManagedAccess()));
            existing.updateScopes(resource.getScopes().stream()
                    .map((ScopeRepresentation scope) -> toModel(scope, resourceServer, authorization, false))
                    .collect(Collectors.toSet()));
            Map<String, List<String>> attributes = resource.getAttributes();

            if (attributes != null) {
                Set<String> existingAttrNames = existing.getAttributes().keySet();

                for (String name : existingAttrNames) {
                    if (attributes.containsKey(name)) {
                        existing.setAttribute(name, attributes.get(name));
                        attributes.remove(name);
                    } else {
                        existing.removeAttribute(name);
                    }
                }

                for (String name : attributes.keySet()) {
                    existing.setAttribute(name, attributes.get(name));
                }
            }

            return existing;
        }

        Resource model = resourceStore.create(resourceServer, resource.getId(), resource.getName(), ownerId);

        model.setDisplayName(resource.getDisplayName());
        model.setType(resource.getType());
        model.updateUris(resource.getUris());
        model.setIconUri(resource.getIconUri());
        model.setOwnerManagedAccess(Boolean.TRUE.equals(resource.getOwnerManagedAccess()));

        Set<ScopeRepresentation> scopes = resource.getScopes();

        if (scopes != null) {
            model.updateScopes(scopes.stream().map(scope -> toModel(scope, resourceServer, authorization, false)).collect(Collectors.toSet()));
        }

        Map<String, List<String>> attributes = resource.getAttributes();

        if (attributes != null) {
            for (Entry<String, List<String>> entry : attributes.entrySet()) {
                model.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        resource.setId(model.getId());

        return model;
    }

    public static Scope toModel(ScopeRepresentation scope, ResourceServer resourceServer, AuthorizationProvider authorization) {
        return toModel(scope, resourceServer, authorization, true);
    }

    public static Scope toModel(ScopeRepresentation scope, ResourceServer resourceServer, AuthorizationProvider authorization, boolean updateIfExists) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        ScopeStore scopeStore = storeFactory.getScopeStore();
        Scope existing;

        if (scope.getId() != null) {
            existing = scopeStore.findById(resourceServer, scope.getId());
        } else {
            existing = scopeStore.findByName(resourceServer, scope.getName());
        }

        if (existing != null) {
            if (updateIfExists) {
                existing.setName(scope.getName());
                existing.setDisplayName(scope.getDisplayName());
                existing.setIconUri(scope.getIconUri());
            }
            return existing;
        }

        Scope model = scopeStore.create(resourceServer, scope.getId(), scope.getName());

        model.setDisplayName(scope.getDisplayName());
        model.setIconUri(scope.getIconUri());

        scope.setId(model.getId());

        return model;
    }

    public static PermissionTicket toModel(PermissionTicketRepresentation representation, ResourceServer resourceServer, AuthorizationProvider authorization) {
        PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();
        PermissionTicket ticket = ticketStore.findById(resourceServer, representation.getId());
        boolean granted = representation.isGranted();

        if (granted && !ticket.isGranted()) {
            ticket.setGrantedTimestamp(System.currentTimeMillis());
        } else if (!granted) {
            ticketStore.delete(ticket.getId());
        }

        return ticket;
    }

    public static void importFederatedUser(KeycloakSession session, RealmModel newRealm, UserRepresentation userRep) {
        UserFederatedStorageProvider federatedStorage = session.userFederatedStorage();
        if (userRep.getAttributes() != null) {
            for (Map.Entry<String, List<String>> entry : userRep.getAttributes().entrySet()) {
                String key = entry.getKey();
                List<String> value = entry.getValue();
                if (value != null) {
                    federatedStorage.setAttribute(newRealm, userRep.getId(), key, new LinkedList<>(value));
                }
            }
        }
        if (userRep.getRequiredActions() != null) {
            for (String action : userRep.getRequiredActions()) {
                federatedStorage.addRequiredAction(newRealm, userRep.getId(), action);
            }
        }
        if (userRep.getCredentials() != null) {
            for (CredentialRepresentation cred : userRep.getCredentials()) {
                federatedStorage.createCredential(newRealm, userRep.getId(), toModel(cred));
            }
        }
        createFederatedRoleMappings(federatedStorage, userRep, newRealm);

        if (userRep.getGroups() != null) {
            for (String path : userRep.getGroups()) {
                GroupModel group = KeycloakModelUtils.findGroupByPath(newRealm, path);
                if (group == null) {
                    throw new RuntimeException("Unable to find group specified by path: " + path);

                }
                federatedStorage.joinGroup(newRealm, userRep.getId(), group);
            }
        }

        if (userRep.getFederatedIdentities() != null) {
            for (FederatedIdentityRepresentation identity : userRep.getFederatedIdentities()) {
                FederatedIdentityModel mappingModel = new FederatedIdentityModel(identity.getIdentityProvider(), identity.getUserId(), identity.getUserName());
                federatedStorage.addFederatedIdentity(newRealm, userRep.getId(), mappingModel);
            }
        }
        if (userRep.getClientConsents() != null) {
            for (UserConsentRepresentation consentRep : userRep.getClientConsents()) {
                UserConsentModel consentModel = toModel(newRealm, consentRep);
                federatedStorage.addConsent(newRealm, userRep.getId(), consentModel);
            }
        }
        if (userRep.getNotBefore() != null) {
            federatedStorage.setNotBeforeForUser(newRealm, userRep.getId(), userRep.getNotBefore());
        }


    }

    public static void createFederatedRoleMappings(UserFederatedStorageProvider federatedStorage, UserRepresentation userRep, RealmModel realm) {
        if (userRep.getRealmRoles() != null) {
            for (String roleString : userRep.getRealmRoles()) {
                RoleModel role = realm.getRole(roleString.trim());
                if (role == null) {
                    role = realm.addRole(roleString.trim());
                }
                federatedStorage.grantRole(realm, userRep.getId(), role);
            }
        }
        if (userRep.getClientRoles() != null) {
            for (Map.Entry<String, List<String>> entry : userRep.getClientRoles().entrySet()) {
                ClientModel client = realm.getClientByClientId(entry.getKey());
                if (client == null) {
                    throw new RuntimeException("Unable to find client role mappings for client: " + entry.getKey());
                }
                createFederatedClientRoleMappings(federatedStorage, realm, client, userRep, entry.getValue());
            }
        }
    }

    public static void createFederatedClientRoleMappings(UserFederatedStorageProvider federatedStorage, RealmModel realm, ClientModel clientModel, UserRepresentation userRep, List<String> roleNames) {
        if (userRep == null) {
            throw new RuntimeException("User not found");
        }

        for (String roleName : roleNames) {
            RoleModel role = clientModel.getRole(roleName.trim());
            if (role == null) {
                role = clientModel.addRole(roleName.trim());
            }
            federatedStorage.grantRole(realm, userRep.getId(), role);

        }
    }

    public static Map<String, String> removeEmptyString(Map<String, String> map) {
        if (map == null) {
            return null;
        }

        Map<String, String> m = new HashMap<>(map);
        for (Iterator<Map.Entry<String, String>> itr = m.entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry<String, String> e = itr.next();
            if (e.getValue() == null || e.getValue().equals("")) {
                itr.remove();
            }
        }
        return m;
    }

    public static ResourceServer createResourceServer(ClientModel client, KeycloakSession session, boolean addDefaultRoles) {
        if ((client.isBearerOnly() || client.isPublicClient())
                && !(client.getClientId().equals(Config.getAdminRealm() + "-realm") || client.getClientId().equals(Constants.REALM_MANAGEMENT_CLIENT_ID))) {
            throw new RuntimeException("Only confidential clients are allowed to set authorization settings");
        }
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        UserModel serviceAccount = session.users().getServiceAccount(client);

        if (serviceAccount == null) {
            client.setServiceAccountsEnabled(true);
        }

        if (addDefaultRoles) {
            RoleModel umaProtectionRole = client.getRole(Constants.AUTHZ_UMA_PROTECTION);

            if (umaProtectionRole == null) {
                umaProtectionRole = client.addRole(Constants.AUTHZ_UMA_PROTECTION);
            }

            if (serviceAccount != null) {
                serviceAccount.grantRole(umaProtectionRole);
            }
        }

        ResourceServerRepresentation representation = new ResourceServerRepresentation();

        representation.setAllowRemoteResourceManagement(true);
        representation.setClientId(client.getId());

        return toModel(representation, authorization, client);
    }

    public static UserSyncConfigModel toModel(UserSyncConnectionRepresentation connection) {
        UserSyncConfigModel model = new UserSyncConfigModel();
        model.setId(connection.getProviderId());
        model.setEnrollmentOption(connection.getEnrollmentOption());
        model.setConfig(connection.getSyncConfig());
        model.setName(connection.getName());
        model.setProviderId(connection.getProviderId());

        return model;
    }
}

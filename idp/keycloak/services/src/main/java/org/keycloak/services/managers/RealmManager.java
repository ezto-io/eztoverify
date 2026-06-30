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
package org.keycloak.services.managers;

import com.qlikverify.billingAddress.spi.BillingAddressRepresentation;
import com.qlikverify.billingAddress.spi.BillingAddressService;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.branding.RootBrandProvider;
import org.keycloak.common.Profile;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.models.*;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.DefaultClientScopes;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AudienceResolveProtocolMapper;
import org.keycloak.representations.idm.*;
import org.keycloak.services.clientregistration.policy.DefaultClientRegistrationPolicies;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.utils.ReservedCharValidator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.keycloak.models.CibaConfig.CIBA_EXPIRES_IN;
import static org.keycloak.protocol.oidc.grants.ciba.AbstractCibaAuthorizer.CIBA_LINK;
import static org.keycloak.protocol.oidc.grants.ciba.AbstractCibaAuthorizer.CIBA_NOTIFY;

/**
 * Per request object
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmManager {

    protected KeycloakSession session;
    private AvatarProvider avatarProvider;
    protected FreeMarkerEmailTemplateProvider freeMarker;
    protected RealmProvider model;
    protected static final Logger logger = Logger.getLogger(RealmManager.class);

    public RealmManager(KeycloakSession session) {
        this.session = session;
        this.freeMarker = new FreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.model = session.realms();
    }

    public KeycloakSession getSession() {
        return session;
    }

    public RealmModel getKeycloakAdminstrationRealm() {
        return getRealmByName(Config.getAdminRealm());
    }

    public RealmModel getRealm(String id) {
        return model.getRealm(id);
    }

    public RealmModel getRealmByName(String name) {
        return model.getRealmByName(name);
    }

    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    public RealmModel createRealm(String id, String name) {
        return createRealm(id, name, RepresentationToModel.getMode(), null);
    }

    public RealmModel createRealm(String id, String name, String mode, String subscriptionType) {
        if (id == null || id.trim().isEmpty()) {
            id = KeycloakModelUtils.generateId();
        } else {
            ReservedCharValidator.validate(id);
        }
        String realmName = name.trim().toLowerCase().replace(" ", "-");
        ReservedCharValidator.validate(realmName);
        RealmModel realm = model.createRealm(id, realmName);
        realm.setMode(mode);
        realm.setName(realmName);
        realm.setDisplayName(name);

        // setup defaults
        setupRealmDefaults(realm);

        KeycloakModelUtils.setupDefaultRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realmName.toLowerCase());
        setupMasterAdminManagement(realm);
        setupRealmAdminManagement(realm);
        setupAccountManagement(realm);
        setupSSOManagement(realm);
        setupBrokerService(realm);
        setupAdminConsole(realm);
        setupAdminConsoleLocaleMapper(realm);
        setupAdminCli(realm);
        setupImpersonationService(realm);
        setupAuthenticationFlows(realm);
//        setupRequiredActions(realm);
        setupOfflineTokens(realm, null);
        createDefaultClientScopes(realm);
        setupAuthorizationServices(realm);
        setupClientRegistrations(realm);
        setupCreateRealmApp(realm);


        if (!realm.getName().equalsIgnoreCase(Config.getAdminRealm())) {
            //Upload Email Templates  to MinIO
            uploadEmailTemplatesToMinio(realm);
        }

        fireRealmPostCreate(realm);

        if (EnvironmentReader.getValue("VARIANT").equalsIgnoreCase("ezto auth") && subscriptionType != null && subscriptionType.equals("wiam_authentication")) {
            realm.setAttribute("subscriptionNeedsAuthenticationProduct", "true");
        }
        if(EnvironmentReader.getValue("VARIANT").equalsIgnoreCase("ezto verify")) {
            realm.setAttribute("result-version", "v1");
        }

        session.onBoarding().onTenantOnboarding(realm, null);

        setupEventsConfig(realm);
        return realm;
    }

    protected void setupAuthenticationFlows(RealmModel realm) {
        if (realm.getAuthenticationFlowsStream().count() == 0) DefaultAuthenticationFlows.addFlows(realm, session);
    }

   /* protected void setupRequiredActions(RealmModel realm) {
        if (realm.getRequiredActionProvidersStream().count() == 0) DefaultRequiredActions.addActions(realm);
    }*/

    private void setupOfflineTokens(RealmModel realm, RealmRepresentation realmRep) {
        RoleModel offlineRole = KeycloakModelUtils.setupOfflineRole(realm);

        if (realmRep != null && hasRealmRole(realmRep, Constants.OFFLINE_ACCESS_ROLE)) {
            // Case when realmRep had the offline_access role, but not the offline_access client scope. Need to manually remove the role
            List<RoleRepresentation> realmRoles = realmRep.getRoles().getRealm();
            for (RoleRepresentation role : realmRoles) {
                if (Constants.OFFLINE_ACCESS_ROLE.equals(role.getName())) {
                    realmRoles.remove(role);
                    break;
                }
            }
        }

        if (realmRep == null || !hasClientScope(realmRep, Constants.OFFLINE_ACCESS_ROLE)) {
            DefaultClientScopes.createOfflineAccessClientScope(realm, offlineRole);
        }
    }

    protected void createDefaultClientScopes(RealmModel realm) {
        DefaultClientScopes.createDefaultClientScopes(session, realm, true);
    }

    protected void setupAdminConsole(RealmModel realm) {
        ClientModel adminConsole = realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        if (adminConsole == null)
            adminConsole = KeycloakModelUtils.createClient(realm, Constants.ADMIN_CONSOLE_CLIENT_ID);
        adminConsole.setName("dashboard");

        adminConsole.setRootUrl(Constants.AUTH_ADMIN_URL_PROP);
        String baseUrl = "/admin/" + realm.getName() + "/console/";
        adminConsole.setBaseUrl(baseUrl);
        adminConsole.addRedirectUri(baseUrl + "*");
        adminConsole.setWebOrigins(Collections.singleton("+"));

        adminConsole.setEnabled(true);
        adminConsole.setOverrideAuthentication(true);
        adminConsole.setAlwaysDisplayInConsole(false);
        adminConsole.setPublicClient(true);
        adminConsole.setFullScopeAllowed(false);
        adminConsole.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        adminConsole.setResetPasswordAllowed(true);

        adminConsole.setAttribute(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD, "S256");

        adminConsole.setPublicClient(true);
        adminConsole.setStandardFlowEnabled(true);
        adminConsole.setImplicitFlowEnabled(false);
        adminConsole.setDirectAccessGrantsEnabled(false);
        adminConsole.setServiceAccountsEnabled(false);

        adminConsole.setAttribute("login_theme", "default");
        adminConsole.setAttribute("isInBuilt", "true");
        adminConsole.setAttribute("inviteOnly", "false");

        setupDefaultActionAndPolicies(realm, adminConsole);
    }

    private void setupDefaultActionAndPolicies(RealmModel realm, ClientModel adminConsole) {
        PolicyModel model = new PolicyModel();

        model.setPolicyName(Constants.ADMIN_CONSOLE_CLIENT_ID + "_temppwd_policy");
        model.setPolicyType("action");
        model.setPolicyValue("temppassword() and decision()");
        model.setRealmPolicy(false);
        model.setInBuilt(true);
        model = session.policies().addPolicy(realm, model);


        PolicyModel emailPolicyModel = new PolicyModel();

        emailPolicyModel.setPolicyName(Constants.ADMIN_CONSOLE_CLIENT_ID + "_email_policy");
        emailPolicyModel.setPolicyType("action");
        emailPolicyModel.setPolicyValue("isemailverified(false) and decision()");
        emailPolicyModel.setRealmPolicy(false);
        emailPolicyModel.setInBuilt(true);

        emailPolicyModel = session.policies().addPolicy(realm, emailPolicyModel);

        ActionModel actionModel = new ActionModel();
        actionModel.setAlias(Constants.ADMIN_CONSOLE_CLIENT_ID + "_action");
        actionModel.setInBuilt(true);
        actionModel = realm.addAction(actionModel);

        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setAlias(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setName(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setProviderId(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setDefaultAction(false);
        requiredAction.setPolicyId(model.getId());
        requiredAction.setPolicyType("action");
        requiredAction.setPriority(0);
        requiredAction.setEnabled(true);
        requiredAction.setActionId(actionModel.getId());
        realm.addRequiredActionProvider(requiredAction);

        RequiredActionProviderModel emailRA = new RequiredActionProviderModel();
        emailRA.setAlias(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setName(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setProviderId(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setDefaultAction(false);
        emailRA.setPolicyId(emailPolicyModel.getId());
        emailRA.setPolicyType("action");
        emailRA.setPriority(1);
        emailRA.setEnabled(true);
        emailRA.setActionId(actionModel.getId());
        realm.addRequiredActionProvider(emailRA);

        PolicyModel tenantOnboardingPloicy = new PolicyModel();

        tenantOnboardingPloicy.setPolicyName(Constants.ADMIN_CONSOLE_CLIENT_ID + "_tenant_onboarding_policy");
        tenantOnboardingPloicy.setPolicyType("action");
        tenantOnboardingPloicy.setPolicyValue("tenantOnboardingPolicy() and decision()");
        tenantOnboardingPloicy.setRealmPolicy(false);
        tenantOnboardingPloicy.setInBuilt(true);
        tenantOnboardingPloicy = session.policies().addPolicy(realm, tenantOnboardingPloicy);

        RequiredActionProviderModel tenantOnbaording = new RequiredActionProviderModel();
        tenantOnbaording.setAlias(UserModel.RequiredAction.TENANT_ONBOARDING.name());
        tenantOnbaording.setName(UserModel.RequiredAction.TENANT_ONBOARDING.name());
        tenantOnbaording.setProviderId(UserModel.RequiredAction.TENANT_ONBOARDING.name());
        tenantOnbaording.setDefaultAction(false);
        tenantOnbaording.setPolicyId(tenantOnboardingPloicy.getId());
        tenantOnbaording.setPolicyType("action");
        tenantOnbaording.setPriority(2);
        tenantOnbaording.setEnabled(true);
        tenantOnbaording.setActionId(actionModel.getId());
        realm.addRequiredActionProvider(tenantOnbaording);

        adminConsole.setAuthenticationFlowBindingOverride("action", actionModel.getId());
    }

    private void setupDefaultActionAndPoliciesSSO(RealmModel realm, ClientModel ssoClient, String clientId) {
        PolicyModel model = new PolicyModel();

        model.setPolicyName(clientId + "_temppwd_policy");
        model.setPolicyType("action");
        model.setPolicyValue("temppassword() and decision()");
        model.setRealmPolicy(false);
        model.setInBuilt(true);
        model = session.policies().addPolicy(realm, model);


        PolicyModel emailPolicyModel = new PolicyModel();

        emailPolicyModel.setPolicyName(clientId + "_email_policy");
        emailPolicyModel.setPolicyType("action");
        emailPolicyModel.setPolicyValue("isemailverified(false) and decision()");
        emailPolicyModel.setRealmPolicy(false);
        emailPolicyModel.setInBuilt(true);

        emailPolicyModel = session.policies().addPolicy(realm, emailPolicyModel);


        ActionModel actionModel = new ActionModel();
        actionModel.setAlias(clientId + "_action");
        actionModel.setInBuilt(true);
        actionModel = realm.addAction(actionModel);

        RequiredActionProviderModel requiredAction = new RequiredActionProviderModel();
        requiredAction.setAlias(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setName(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setProviderId(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        requiredAction.setDefaultAction(false);
        requiredAction.setPolicyId(model.getId());
        requiredAction.setPolicyType("action");
        requiredAction.setPriority(0);
        requiredAction.setEnabled(true);
        requiredAction.setActionId(actionModel.getId());
        realm.addRequiredActionProvider(requiredAction);

        RequiredActionProviderModel emailRA = new RequiredActionProviderModel();
        emailRA.setAlias(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setName(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setProviderId(UserModel.RequiredAction.VERIFY_EMAIL.name());
        emailRA.setDefaultAction(false);
        emailRA.setPolicyId(emailPolicyModel.getId());
        emailRA.setPolicyType(emailPolicyModel.getPolicyType());
        emailRA.setPriority(1);
        emailRA.setEnabled(true);
        emailRA.setActionId(actionModel.getId());
        realm.addRequiredActionProvider(emailRA);

        ssoClient.setAuthenticationFlowBindingOverride("action", actionModel.getId());
    }

    protected void setupAdminConsoleLocaleMapper(RealmModel realm) {
        ClientModel adminConsole = session.clients().getClientByClientId(realm, Constants.ADMIN_CONSOLE_CLIENT_ID);
        ProtocolMapperModel localeMapper = adminConsole.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, OIDCLoginProtocolFactory.LOCALE);

        if (localeMapper == null) {
            localeMapper = ProtocolMapperUtils.findLocaleMapper(session);
            if (localeMapper != null) {
                adminConsole.addProtocolMapper(localeMapper);
            }
        }
    }

    public void setupAdminCli(RealmModel realm) {
        ClientModel adminCli = realm.getClientByClientId(Constants.ADMIN_CLI_CLIENT_ID);
        if (adminCli == null) {
            adminCli = KeycloakModelUtils.createClient(realm, Constants.ADMIN_CLI_CLIENT_ID);
            adminCli.setName("${client_" + Constants.ADMIN_CLI_CLIENT_ID + "}");
            adminCli.setEnabled(true);
            adminCli.setAlwaysDisplayInConsole(false);
            adminCli.setPublicClient(true);
            adminCli.setFullScopeAllowed(false);
            adminCli.setStandardFlowEnabled(false);
            adminCli.setDirectAccessGrantsEnabled(true);
            adminCli.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        }

    }

    public void addQueryCompositeRoles(ClientModel realmAccess) {
        RoleModel queryClients = realmAccess.getRole(AdminRoles.QUERY_CLIENTS);
        RoleModel queryUsers = realmAccess.getRole(AdminRoles.QUERY_USERS);
        RoleModel queryGroups = realmAccess.getRole(AdminRoles.QUERY_GROUPS);

        RoleModel viewClients = realmAccess.getRole(AdminRoles.VIEW_CLIENTS);
        viewClients.addCompositeRole(queryClients);
        RoleModel viewUsers = realmAccess.getRole(AdminRoles.VIEW_USERS);
        viewUsers.addCompositeRole(queryUsers);
        viewUsers.addCompositeRole(queryGroups);
    }


    public String getRealmAdminClientId(RealmModel realm) {
        return Constants.REALM_MANAGEMENT_CLIENT_ID;
    }

    public String getRealmAdminClientId(RealmRepresentation realm) {
        return Constants.REALM_MANAGEMENT_CLIENT_ID;
    }

    protected void setupRealmDefaults(RealmModel realm) {
        realm.setBrowserSecurityHeaders(BrowserSecurityHeaders.realmDefaultHeaders);

        RootBrandProvider rootBrandProvider = session.rootBrand();
        realm.setAdminTheme(rootBrandProvider.getAdminTheme());
        realm.setLoginTheme(rootBrandProvider.getLoginTheme());
        realm.setEmailTheme(rootBrandProvider.getEmailTheme());
        realm.setAccountTheme(rootBrandProvider.getAccountTheme());
        realm.setSSOTheme(rootBrandProvider.getSSOTheme());

        session.loginBranding().createBranding(realm, "default", true, getDefaultTheme(session, realm));
        // brute force
        realm.setBruteForceProtected(true);
        realm.setPermanentLockout(false);
        realm.setMaxFailureWaitSeconds(900);
        realm.setMinimumQuickLoginWaitSeconds(60);
        realm.setWaitIncrementSeconds(60);
        realm.setQuickLoginCheckMilliSeconds(1000);
        realm.setMaxDeltaTimeSeconds(60 * 60 * 12); // 12 hours
        realm.setFailureFactor(30);
        realm.setSslRequired(SslRequired.EXTERNAL);
        realm.setOTPPolicy(OTPPolicy.DEFAULT_POLICY);
        realm.setLoginWithEmailAllowed(true);

        // set default event listeners
        List<String> defaultEventListenersTobeAdded = new ArrayList<>();
        Collections.addAll(defaultEventListenersTobeAdded, "jboss-logging", "email", "azure-sync", "google-ws-sync", "duo-ws-sync", "okta-ws-sync", "black-listing-listener",
                "report-listener", "workflows-event-listener", "user-last-login-listener", "ez-dead-letter-hook-listener");

        Set<String> eventListeners = new HashSet<String>();

        session.getKeycloakSessionFactory().getProviderFactoriesStream(EventListenerProvider.class)
                .filter(factory -> defaultEventListenersTobeAdded.contains(factory.getId()))
                .map(EventListenerProviderFactory.class::cast)
                .forEach(factory -> {
                    if (Arrays.asList(factory.supportedModes()).contains(realm.getMode())) {
                        eventListeners.add(factory.getId());
                    }
                });

        realm.setEventsListeners(eventListeners);
    }


    public static Map<String, String> getDefaultTheme(KeycloakSession session, RealmModel realmModel) {
        Map<String, String> defaultBrandConfig = new HashMap<>();
        defaultBrandConfig.put("showHeader", "true");
        defaultBrandConfig.put("bodyBgImageOpacity", "0.25");
        defaultBrandConfig.put("headerBgColor", "#edeef1");
        defaultBrandConfig.put("headerMenuItems", "https://ezto.io/verify|Home##https://support.ezto.io|Contact Support");
        defaultBrandConfig.put("headerLogoLink", "https://ezto.io/verify");
        defaultBrandConfig.put("themeLayout", "Center");
        defaultBrandConfig.put("themeLayoutOneLine", "One step away from all your Digital Identity needs");
        defaultBrandConfig.put("themeLayoutFeatures", "Join us##Make OnboardingProvider and Login experience awesome##Quick Start your process with industry standard templates");
        defaultBrandConfig.put("themeLayoutBgType", "Text");
        defaultBrandConfig.put("themeLayoutBgColor", "#000000");
        defaultBrandConfig.put("themeLayoutTextColor", "#FFFFFF");
        defaultBrandConfig.put("textBorderColor", "#edeef1");
        defaultBrandConfig.put("formBgColor", "#FFFFFF");
        defaultBrandConfig.put("formBorderColor", "#FFFFFF");
        defaultBrandConfig.put("formBorderWidth", "1");
        defaultBrandConfig.put("formBorderRadius", "10");
        defaultBrandConfig.put("buttonBorderRadius", "8");
        defaultBrandConfig.put("buttonTextColor", "#FFFFFF");
        defaultBrandConfig.put("bodyFont", "Inter");
        defaultBrandConfig.put("fontPreference", "default");
        defaultBrandConfig.put("customFontPreference", "link");
        defaultBrandConfig.put("showLogoInForm", "true");
        defaultBrandConfig.put("logoHeight", "38");
        defaultBrandConfig.put("logoPosition", "logo-left");
        defaultBrandConfig.put("logoPositionValue", "0|0|0|0");
        defaultBrandConfig.put("logoPositionValueType", "px");
        defaultBrandConfig.put("bodyBgType", "Image");
        defaultBrandConfig.put("bodyBgColor", "#000000");
        defaultBrandConfig.put("showFooter", "true");
        defaultBrandConfig.put("textBoxTextColor", "#0b2238");
        defaultBrandConfig.put("formTextColor", "#0b2238");
        defaultBrandConfig.put("textBoxBgColor", "#ffffff");
        if (realmModel.getMode().equalsIgnoreCase("bo")) {
            defaultBrandConfig.put("buttonBgColor", "#1C64EB");
            defaultBrandConfig.put("textActiveColor", "#165df5");
        } else if (realmModel.getMode().equalsIgnoreCase("ciam")) {
            defaultBrandConfig.put("buttonBgColor", "#323232");
            defaultBrandConfig.put("textActiveColor", "#000000");
        } else if (realmModel.getMode().equalsIgnoreCase("wiam")) {
            defaultBrandConfig.put("buttonBgColor", "#4CA355");
            defaultBrandConfig.put("textActiveColor", "#418b4a");
        }
        int year = Calendar.getInstance().get(Calendar.YEAR);
        defaultBrandConfig.put("footerText", "© " + String.valueOf(year) + " " + EnvironmentReader.getValue("VARIANT", "ezto verify") + ". All Rights Reserved");
        defaultBrandConfig.put("footerTextColor", "#6C7A87");
        return defaultBrandConfig;
    }

    //Function used to set QVBranding in Subscription
    public static void isQvBrandingDisabled(KeycloakSession session, RealmModel realmModel, Map<String, String> brandConfig) {
        if (session != null && realmModel != null) {
            brandConfig.put("removeBranding", "false");
        }
    }

    public boolean removeRealm(RealmModel realm) {

        ClientModel masterAdminClient = realm.getMasterAdminClient();
        boolean removed = model.removeRealm(realm.getId());
        if (removed) {
            if (masterAdminClient != null) {
                session.clients().removeClient(getKeycloakAdminstrationRealm(), masterAdminClient.getId());
            }

            UserSessionProvider sessions = session.sessions();
            if (sessions != null) {
                sessions.onRealmRemoved(realm);
            }

            UserSessionPersisterProvider sessionsPersister = session.getProvider(UserSessionPersisterProvider.class);
            if (sessionsPersister != null) {
                sessionsPersister.onRealmRemoved(realm);
            }

            AuthenticationSessionProvider authSessions = session.authenticationSessions();
            if (authSessions != null) {
                authSessions.onRealmRemoved(realm);
            }

            // Refresh periodic sync tasks for configured storageProviders
            UserStorageSyncManager storageSync = new UserStorageSyncManager();
            realm.getUserStorageProvidersStream()
                    .forEachOrdered(provider -> storageSync.notifyToRefreshPeriodicSync(session, realm, provider, true));

            //Remove the Realm Data from MINIO
            AvatarProvider avatarProvider = session.getProvider(AvatarProvider.class, "avatar-storage-minio");
            if (!avatarProvider.removeRealmData(realm.getName())) {
                logger.error("Something went wrong on deleting Realm Data for the realm " + realm.getName() + " from Minio");
            }

            //Trigger a DNS Removal Request if any set
            if (realm.getCustomDomainMappings().filter(x -> x.isActive()).count() > 0) {
                realm.getCustomDomainMappings().filter(x -> x.isActive()).forEach(domain -> triggerDNSRemovalReq(session, realm, domain.getCustomDomain()));
            }
        }
        return removed;
    }

    public void updateRealmEventsConfig(RealmEventsConfigRepresentation rep, RealmModel realm) {
        realm.setEventsEnabled(rep.isEventsEnabled());
        realm.setEventsExpiration(rep.getEventsExpiration() != null ? rep.getEventsExpiration() : 0);
        if (rep.getEventsListeners() != null) {
            realm.setEventsListeners(new HashSet<>(rep.getEventsListeners()));
        }
        if (rep.getEnabledEventTypes() != null) {
            if (!rep.getEnabledEventTypes().contains(Constants.defaultEvts)) {
                rep.getEnabledEventTypes().addAll(Constants.defaultEvts);
            }
            realm.setEnabledEventTypes(new HashSet<>(rep.getEnabledEventTypes()));
        }
        if (rep.isAdminEventsEnabled() != null) {
            realm.setAdminEventsEnabled(rep.isAdminEventsEnabled());
        }
        if (rep.isAdminEventsDetailsEnabled() != null) {
            realm.setAdminEventsDetailsEnabled(rep.isAdminEventsDetailsEnabled());
        }
    }

    public void setupEventsConfig(RealmModel realm) {
        List<String> defEvents = new ArrayList<>();
        defEvents.add("LOGIN");
        defEvents.add("REGISTER");
        defEvents.add("LOGOUT");
        defEvents.add("LOGIN_ERROR");
        defEvents.add("REGISTER_ERROR");
        defEvents.add("LOGOUT_ERROR");
        defEvents.add(EventType.AZURE_SYNC_SUCCESS.name());
        defEvents.add(EventType.GOOGLE_WS_SYNC_SUCCESS.name());
        defEvents.add(EventType.DUO_WS_SYNC_SUCCESS.name());
        defEvents.add(EventType.OKTA_WS_SYNC_SUCCESS.name());

        realm.setEventsEnabled(true);
        realm.setAdminEventsEnabled(true);
        realm.setEnabledEventTypes(new HashSet<>(defEvents));
        setupDefaultAnamolyDetectionSetupConfig(realm);
    }

    private void setupDefaultAnamolyDetectionSetupConfig(RealmModel realmModel) {
        Map<String, String> config = new HashMap<>();
        config.put("DIFF_BROWSER_TRIGGER", "true");
        config.put("DIFF_IP_TRIGGER", "true");
        config.put("DIFF_OS_TRIGGER", "false");

        Optional<EmailConfigModel> emailProvider = realmModel.getEmailConfigsStream().findFirst();
        emailProvider.ifPresent(emailConfigModel -> config.put("email-sender", emailConfigModel.getEmailProviderId()));
        EventConfigRepresentation rep = new EventConfigRepresentation();
        EventConfigModel exists = realmModel.getEventConfigByProviderId(Constants.EZTO_ANAMOLOY_DETECTION);
        if (exists == null) {
            List<String> eventListeners = realmModel.getEventsListenersStream().collect(Collectors.toList());
            if (!eventListeners.contains(Constants.EZTO_ANAMOLOY_DETECTION)) {
                eventListeners.add(Constants.EZTO_ANAMOLOY_DETECTION);
                realmModel.setEventsListeners(new HashSet<>(eventListeners));
            }

            rep.setConfig(config);
            rep.setEventProviderId(Constants.EZTO_ANAMOLOY_DETECTION);

            realmModel.addEventConfig(RepresentationToModel.toModel(rep));
            AdminEventBuilder adEvt = new AdminEventBuilder(realmModel, new AdminAuth(realmModel, null, null,
                    realmModel.getMode().equalsIgnoreCase("bo")
                            ? realmModel.getClientByClientId("dashboard")
                            : realmModel.getClientByClientId("sso")), session, null);
            adEvt.operation(OperationType.CREATE).resource(ResourceType.EVENT_CONFIG).representation(rep).success();
        }
    }

    public void updateRealmEmailConfig(RealmEmailConfigRepresentation rep, RealmModel realmModel) {
        if (rep.getEmailSenders() != null) {
            realmModel.setEmailSenders(new HashSet<>(rep.getEmailSenders()));
        }
    }

    public void updateRealmSmsConfig(RealmSmsConfigRepresentation rep, RealmModel realmModel) {
        if (rep.getSmsSenders() != null) {
            realmModel.setSmsSenders(new HashSet<>(rep.getSmsSenders()));
        }
    }

    public void updateRealmMFAConfig(RealmMFAConfigRepresentation rep, RealmModel realmModel) {
        if (rep.getMfaProviders() != null) {
            realmModel.setMFAProviders(new HashSet<>(rep.getMfaProviders()));
        }
    }

    public void setupMasterAdminManagement(RealmModel realm) {
        // Need to refresh masterApp for current realm
        String adminRealmId = Config.getAdminRealm();
        RealmModel adminRealm = model.getRealmByName(adminRealmId);
        ClientModel masterApp = adminRealm.getClientByClientId(KeycloakModelUtils.getMasterRealmAdminApplicationClientId(realm.getName()));
        if (masterApp == null) {
            createMasterAdminManagement(realm);
            return;
        }
        realm.setMasterAdminClient(masterApp);
    }

    private void createMasterAdminManagement(RealmModel realm) {
        RealmModel adminRealm;
        RoleModel adminRole;

        if (realm.getName().equals(Config.getAdminRealm())) {
            adminRealm = realm;

            adminRole = realm.addRole(AdminRoles.ADMIN);
            adminRole.setSingleAttribute("isInBuilt", "true");
            RoleModel createRealmRole = realm.addRole(AdminRoles.CREATE_REALM);
            createRealmRole.setSingleAttribute("isInBuilt", "true");
            adminRole.addCompositeRole(createRealmRole);
            //createRealmRole.setDescription("${role_" + AdminRoles.CREATE_REALM + "}");
        } else {
            adminRealm = model.getRealmByName(Config.getAdminRealm());
            adminRole = adminRealm.getRole(AdminRoles.ADMIN);
            adminRole.setSingleAttribute("isInBuilt", "true");
        }
        //adminRole.setDescription("${role_" + AdminRoles.ADMIN + "}");

        ClientModel realmAdminApp = KeycloakModelUtils.createClient(adminRealm, KeycloakModelUtils.getMasterRealmAdminApplicationClientId(realm.getName()));
        // No localized name for now
        realmAdminApp.setName(realm.getName() + " Workspace");

        realmAdminApp.setBearerOnly(true);
        realm.setMasterAdminClient(realmAdminApp);

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel role = realmAdminApp.addRole(r);
            role.setSingleAttribute("isInBuilt", "true");
            //role.setDescription("${role_" + r + "}");
            adminRole.addCompositeRole(role);
        }
        addQueryCompositeRoles(realmAdminApp);
    }

    private void checkMasterAdminManagementRoles(RealmModel realm) {
        RealmModel adminRealm = model.getRealmByName(Config.getAdminRealm());
        RoleModel adminRole = adminRealm.getRole(AdminRoles.ADMIN);

        ClientModel masterAdminClient = realm.getMasterAdminClient();
        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel found = masterAdminClient.getRole(r);
            if (found == null) {
                addAndSetAdminRole(r, masterAdminClient, adminRole);
            }
        }
        addQueryCompositeRoles(masterAdminClient);
    }


    private void setupRealmAdminManagement(RealmModel realm) {
        if (realm.getName().equals(Config.getAdminRealm())) {
            return;
        } // don't need to do this for master realm

        String realmAdminClientId = getRealmAdminClientId(realm);
        ClientModel realmAdminClient = realm.getClientByClientId(realmAdminClientId);
        if (realmAdminClient == null) {
            realmAdminClient = KeycloakModelUtils.createClient(realm, realmAdminClientId);
            realmAdminClient.setName("${client_" + realmAdminClientId + "}");
        }
        RoleModel adminRole = realmAdminClient.addRole(AdminRoles.REALM_ADMIN);
        adminRole.setSingleAttribute("isInBuilt", "true");
        //adminRole.setDescription("${role_" + AdminRoles.REALM_ADMIN + "}");
        realmAdminClient.setBearerOnly(true);
        realmAdminClient.setFullScopeAllowed(false);
        realmAdminClient.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            addAndSetAdminRole(r, realmAdminClient, adminRole);
        }
        addQueryCompositeRoles(realmAdminClient);
    }

    private void addAndSetAdminRole(String roleName, ClientModel parentClient, RoleModel parentRole) {
        RoleModel role = parentClient.addRole(roleName);
        role.setSingleAttribute("isInBuilt", "true");
        //role.setDescription("${role_" + roleName + "}");
        parentRole.addCompositeRole(role);
    }


    private void checkRealmAdminManagementRoles(RealmModel realm) {
        if (realm.getName().equals(Config.getAdminRealm())) {
            return;
        } // don't need to do this for master realm

        String realmAdminClientId = getRealmAdminClientId(realm);
        ClientModel realmAdminClient = realm.getClientByClientId(realmAdminClientId);
        RoleModel adminRole = realmAdminClient.getRole(AdminRoles.REALM_ADMIN);

        // if realm-admin role isn't in the realm model, create it
        if (adminRole == null) {
            adminRole = realmAdminClient.addRole(AdminRoles.REALM_ADMIN);
            adminRole.setSingleAttribute("isInBuilt", "true");
            //adminRole.setDescription("${role_" + AdminRoles.REALM_ADMIN + "}");
        }

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel found = realmAdminClient.getRole(r);
            if (found == null) {
                addAndSetAdminRole(r, realmAdminClient, adminRole);
            }
        }
        addQueryCompositeRoles(realmAdminClient);
    }


    private void setupAccountManagement(RealmModel realm) {
        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient == null) {
            accountClient = KeycloakModelUtils.createClient(realm, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            accountClient.setName("profile");
            accountClient.setEnabled(true);
            accountClient.setAlwaysDisplayInConsole(false);
            accountClient.setFullScopeAllowed(false);

            accountClient.setRootUrl(Constants.AUTH_BASE_URL_PROP);
            String baseUrl = "/realms/" + realm.getName() + "/account/";
            accountClient.setBaseUrl(baseUrl);
            accountClient.addRedirectUri(baseUrl + "*");

            accountClient.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            accountClient.setAttribute("login_theme", "default");
            accountClient.setAttribute("isInBuilt", "true");
            accountClient.setAttribute("inviteOnly", "false");

            for (String role : AccountRoles.DEFAULT) {
                accountClient.addDefaultRole(role);
                RoleModel roleModel = accountClient.getRole(role);
                roleModel.setSingleAttribute("isInBuilt", "true");
                //roleModel.setDescription("${role_" + role + "}");
                realm.addToDefaultRoles(roleModel);
            }
            RoleModel manageAccountLinks = accountClient.addRole(AccountRoles.MANAGE_ACCOUNT_LINKS);
            manageAccountLinks.setSingleAttribute("isInBuilt", "true");
            //manageAccountLinks.setDescription("${role_" + AccountRoles.MANAGE_ACCOUNT_LINKS + "}");
            RoleModel manageAccount = accountClient.getRole(AccountRoles.MANAGE_ACCOUNT);
            manageAccount.setSingleAttribute("isInBuilt", "true");
            manageAccount.addCompositeRole(manageAccountLinks);
            RoleModel viewAppRole = accountClient.addRole(AccountRoles.VIEW_APPLICATIONS);
            viewAppRole.setSingleAttribute("isInBuilt", "true");
            //viewAppRole.setDescription("${role_" + AccountRoles.VIEW_APPLICATIONS + "}");
            RoleModel viewConsentRole = accountClient.addRole(AccountRoles.VIEW_CONSENT);
            viewConsentRole.setSingleAttribute("isInBuilt", "true");
            //viewConsentRole.setDescription("${role_" + AccountRoles.VIEW_CONSENT + "}");
            RoleModel manageConsentRole = accountClient.addRole(AccountRoles.MANAGE_CONSENT);
            manageConsentRole.setSingleAttribute("isInBuilt", "true");
            // manageConsentRole.setDescription("${role_" + AccountRoles.MANAGE_CONSENT + "}");
            manageConsentRole.addCompositeRole(viewConsentRole);

            KeycloakModelUtils.setupDeleteAccount(accountClient);

            ClientModel accountConsoleClient = realm.getClientByClientId(Constants.ACCOUNT_CONSOLE_CLIENT_ID);
            if (accountConsoleClient == null) {
                accountConsoleClient = KeycloakModelUtils.createClient(realm, Constants.ACCOUNT_CONSOLE_CLIENT_ID);
                accountConsoleClient.setName("${client_" + Constants.ACCOUNT_CONSOLE_CLIENT_ID + "}");
                accountConsoleClient.setEnabled(true);
                accountConsoleClient.setAlwaysDisplayInConsole(false);
                accountConsoleClient.setFullScopeAllowed(false);
                accountConsoleClient.setPublicClient(true);
                accountConsoleClient.setDirectAccessGrantsEnabled(false);

                accountConsoleClient.setRootUrl(Constants.AUTH_BASE_URL_PROP);
                accountConsoleClient.setBaseUrl(baseUrl);
                accountConsoleClient.addRedirectUri(baseUrl + "*");

                accountConsoleClient.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

                accountConsoleClient.addScopeMapping(accountClient.getRole(AccountRoles.MANAGE_ACCOUNT));

                ProtocolMapperModel audienceMapper = new ProtocolMapperModel();
                audienceMapper.setName(OIDCLoginProtocolFactory.AUDIENCE_RESOLVE);
                audienceMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
                audienceMapper.setProtocolMapper(AudienceResolveProtocolMapper.PROVIDER_ID);

                accountConsoleClient.addProtocolMapper(audienceMapper);

                accountConsoleClient.setAttribute(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD, "S256");
            }
        }
    }

    private void setupSSOManagement(RealmModel realm) {
        if (!realm.getMode().equals("wiam"))
            return;

        ClientModel ssoClient = realm.getClientByClientId(Constants.SSO_MANAGEMENT_CLIENT_ID);
        if (ssoClient == null) {
            if (realm.getRole("user") == null) {
                RoleModel roleModel = realm.addRole("user");
                roleModel.setSingleAttribute("isInBuilt", "true");
                realm.addToDefaultRoles(roleModel);
            }
            ssoClient = KeycloakModelUtils.createClient(realm, Constants.SSO_MANAGEMENT_CLIENT_ID);
            ssoClient.setName("sso");
            ssoClient.setEnabled(true);
            ssoClient.setOverrideAuthentication(true);
            ssoClient.setAlwaysDisplayInConsole(false);
            ssoClient.setFullScopeAllowed(false);

            ssoClient.setRootUrl(Constants.AUTH_BASE_URL_PROP);
            String baseUrl = "/realms/" + realm.getName() + "/sso/";
            ssoClient.setBaseUrl(baseUrl);
            ssoClient.addRedirectUri(baseUrl + "*");

            ssoClient.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            ssoClient.setAttribute("login_theme", "default");
            ssoClient.setAttribute("inviteOnly", "true");
            realm.setUserInviteClientId(ssoClient.getClientId());
            ssoClient.setAttribute("isInBuilt", "true");

            setupDefaultActionAndPoliciesSSO(realm, ssoClient, Constants.SSO_MANAGEMENT_CLIENT_ID);
        }
    }

    public void setupImpersonationService(RealmModel realm) {
        ImpersonationConstants.setupImpersonationService(session, realm);
    }

    public void setupBrokerService(RealmModel realm) {
        ClientModel client = realm.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
        if (client == null) {
            client = KeycloakModelUtils.createManagementClient(realm, Constants.BROKER_SERVICE_CLIENT_ID);
            client.setEnabled(true);
            client.setAlwaysDisplayInConsole(false);
            client.setName("${client_" + Constants.BROKER_SERVICE_CLIENT_ID + "}");
            client.setFullScopeAllowed(false);
            client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            for (String role : Constants.BROKER_SERVICE_ROLES) {
                RoleModel roleModel = client.addRole(role);
                roleModel.setSingleAttribute("isInBuilt", "true");
                //roleModel.setDescription("${role_" + role.toLowerCase().replaceAll("_", "-") + "}");
            }
        }
    }

    public RealmModel importRealm(RealmRepresentation rep) {
        return importRealm(rep, false);
    }

    public void createDefaultApiApplication(RealmModel realm) {
        try {
            ClientModel clientModel = realm.addClient();
            clientModel.setSandboxMode(false);
            clientModel.setAuthenticationFlowBindingOverride("ciba_auth", "api-authorizer");
            clientModel.setName("default" + "_API");
            clientModel.setClientId(clientModel.getName());
            clientModel.setSandboxMode(false);
            clientModel.setAuthenticationFlowBindingOverride("ciba_auth", "api-authorizer");
            clientModel.setProtocol("openid-connect");
            clientModel.setClientAuthenticatorType(KeycloakModelUtils.getDefaultClientAuthenticatorType());
            KeycloakModelUtils.generateSecret(clientModel);
            clientModel.setStandardFlowEnabled(true);
            clientModel.setImplicitFlowEnabled(false);
            clientModel.setDirectAccessGrantsEnabled(false);
            clientModel.setServiceAccountsEnabled(false);
            clientModel.setBearerOnly(false);
            clientModel.setAttribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, "true");
            clientModel.setPublicClient(false);
            clientModel.setSandboxMode(false);
            clientModel.setAttribute("login_theme", "default");
            clientModel.setAttribute("isInBuilt", "true");
            clientModel.setAttribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, "true");
            CibaAuthConfigModel configModel = new CibaAuthConfigModel();
            configModel.setClientId(clientModel.getClientId());
            configModel.setId(UUID.randomUUID().toString());
            configModel.setRealmId(realm.getId());
            configModel.setCibaAuthProviderId("ciba-default-managed-auth");
            configModel.setConfig(new HashMap<String, String>() {{
                put("verify-expiry-time", "300s");
                put(CIBA_NOTIFY,CIBA_LINK);
            }});
            realm.addCibaAuthConfig(configModel);
            clientModel.setAttribute(CIBA_EXPIRES_IN, "300s");
        } catch (Exception e){
            logger.error("failed to create default api application");
            throw new RuntimeException();
        }
    }


    /**
     * if "skipUserDependent" is true, then import of any models, which needs users already imported in DB, will be skipped. For example authorization
     */
    public RealmModel importRealm(RealmRepresentation rep, boolean skipUserDependent) {
        String id = rep.getId();
        if (id == null || id.trim().isEmpty()) {
            id = KeycloakModelUtils.generateId();
        } else {
            ReservedCharValidator.validate(id);
        }
        RealmModel realm = model.createRealm(id, rep.getRealm());
        ReservedCharValidator.validate(rep.getRealm());
        realm.setName(rep.getRealm());

        if (rep.getBillingAddress() != null) {
            BillingAddressRepresentation billingAddressRepresentation = new BillingAddressRepresentation();
            billingAddressRepresentation.setLine1(rep.getBillingAddress().getLine1());
            billingAddressRepresentation.setLine2(rep.getBillingAddress().getLine2());
            billingAddressRepresentation.setCity(rep.getBillingAddress().getCity());
            billingAddressRepresentation.setState(rep.getBillingAddress().getState());
            billingAddressRepresentation.setCountry(rep.getBillingAddress().getCountry());
            billingAddressRepresentation.setPostal_code(rep.getBillingAddress().getPostal_code());
            realm.setDefaultRegion(rep.getBillingAddress().getCountry());

            session.getProvider(BillingAddressService.class).addBillingAddress(realm, billingAddressRepresentation);
        }
        // setup defaults
        if (realm.getMode() == null) {
            realm.setMode(rep.getMode());
            if (rep.getMode().equals("bo")) {
                realm.setSandboxMode(true);
            }
        }

        if (realm.getPreferredLoginLocalesStream() == null || !realm.getPreferredLoginLocalesStream().findAny().isPresent()) {
            if (rep.getPreferredLoginLocales() != null && !rep.getPreferredLoginLocales().isEmpty())
                realm.setPreferredLoginLocales(rep.getPreferredLoginLocales());
            else {
                HashSet<String> preferredLocales = new HashSet<>();
                preferredLocales.add("en");
                realm.setPreferredLoginLocales(preferredLocales);
            }
        }

        setupRealmDefaults(realm);

        if (rep.getDefaultRole() == null) {
            KeycloakModelUtils.setupDefaultRole(realm, determineDefaultRoleName(rep));
        } else {
            realm.setDefaultRole(RepresentationToModel.createRole(realm, rep.getDefaultRole()));
        }

        boolean postponeMasterClientSetup = postponeMasterClientSetup(rep);
        if (!postponeMasterClientSetup) {
            setupMasterAdminManagement(realm);
        }

        if (!hasRealmAdminManagementClient(rep)) setupRealmAdminManagement(realm);
        if (!hasAccountManagementClient(rep)) setupAccountManagement(realm);

        if (!hasSSOManagementClient(rep) && rep.getMode().equalsIgnoreCase("wiam")) setupSSOManagement(realm);

        if (rep.getMode().equalsIgnoreCase("bo")) {
            RoleModel roleModel = realm.addRole("user");
            roleModel.setSingleAttribute("isInBuilt", "true");
            realm.addToDefaultRoles(roleModel);
            realm.setAttribute("isPlatformWorkspace", "true");
            realm.setAttribute("isApiWorkspace", "true");
            createDefaultApiApplication(realm);

        }

        //if (!hasSSODesktopClient(rep)) setupSSODesktop(realm);

        boolean postponeImpersonationSetup = hasRealmAdminManagementClient(rep);
        if (!postponeImpersonationSetup) {
            setupImpersonationService(realm);
        }

        if (!hasBrokerClient(rep)) setupBrokerService(realm);
        if (!hasAdminConsoleClient(rep)) setupAdminConsole(realm);

        boolean postponeAdminCliSetup = false;
        if (!hasAdminCliClient(rep)) {
            postponeAdminCliSetup = hasRealmAdminManagementClient(rep);

            if (!postponeAdminCliSetup) {
                setupAdminCli(realm);
            }
        }

        if (!hasRealmRole(rep, Constants.OFFLINE_ACCESS_ROLE) || !hasClientScope(rep, Constants.OFFLINE_ACCESS_ROLE)) {
            setupOfflineTokens(realm, rep);
        }

        if (rep.getClientScopes() == null) {
            createDefaultClientScopes(realm);
        }

        if (EnvironmentReader.getValue("VARIANT").equalsIgnoreCase("ezto auth") && rep.getSubscriptionType() != null && rep.getSubscriptionType().equals("wiam_authentication")) {
            realm.setAttribute("subscriptionNeedsAuthenticationProduct", "true");
        }
        if(EnvironmentReader.getValue("VARIANT").equalsIgnoreCase("ezto verify")) {
            realm.setAttribute("result-version", "v1");
        }

        RepresentationToModel.importRealm(session, rep, realm, skipUserDependent);

        setupClientServiceAccountsAndAuthorizationOnImport(rep, skipUserDependent);

        setupAdminConsoleLocaleMapper(realm);

        if (postponeMasterClientSetup) {
            setupMasterAdminManagement(realm);
        }

        if (rep.getRoles() != null || hasRealmAdminManagementClient(rep)) {
            // Assert all admin roles are available once import took place. This is needed due to import from previous version where JSON file may not contain all admin roles
            checkMasterAdminManagementRoles(realm);
            checkRealmAdminManagementRoles(realm);
        }

        // Could happen when migrating from older version and I have exported JSON file, which contains "realm-management" client but not "impersonation" client
        // I need to postpone impersonation because it needs "realm-management" client and its roles set
        if (postponeImpersonationSetup) {
            setupImpersonationService(realm);
        }

        if (postponeAdminCliSetup) {
            setupAdminCli(realm);
        }

        setupAuthenticationFlows(realm);
        //setupRequiredActions(realm);

        if (!hasRealmRole(rep, AccountRoles.DELETE_ACCOUNT)) {
            KeycloakModelUtils.setupDeleteAccount(realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID));
        }

        // Refresh periodic sync tasks for configured storageProviders
        UserStorageSyncManager storageSync = new UserStorageSyncManager();
        realm.getUserStorageProvidersStream()
                .forEachOrdered(provider -> storageSync.notifyToRefreshPeriodicSync(session, realm, provider, false));

        setupAuthorizationServices(realm);
        setupClientRegistrations(realm);

        if (rep.getKeycloakVersion() != null) {
            MigrationModelManager.migrateImport(session, realm, rep, skipUserDependent);
        }

        session.clientPolicy().updateRealmModelFromRepresentation(realm, rep);

        fireRealmPostCreate(realm);

        session.onBoarding().onTenantOnboarding(realm, rep);
        setupEventsConfig(realm);
        return realm;
    }

    private String determineDefaultRoleName(RealmRepresentation rep) {
        String defaultRoleName = Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + rep.getRealm().toLowerCase();
        if (!hasRealmRole(rep, defaultRoleName)) {
            return defaultRoleName;
        } else {
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                defaultRoleName = Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + rep.getRealm().toLowerCase() + "-" + i;
                if (!hasRealmRole(rep, defaultRoleName)) {
                    return defaultRoleName;
                }
            }
        }
        return null;
    }

    private boolean postponeMasterClientSetup(RealmRepresentation rep) {
        if (!Config.getAdminRealm().equals(rep.getRealm())) {
            return false;
        }

        return hasRealmAdminManagementClient(rep);
    }

    private boolean hasRealmAdminManagementClient(RealmRepresentation rep) {
        String realmAdminClientId = Config.getAdminRealm().equals(rep.getRealm()) ? KeycloakModelUtils.getMasterRealmAdminApplicationClientId(rep.getRealm()) : getRealmAdminClientId(rep);
        return hasClient(rep, realmAdminClientId);
    }

    private boolean hasAccountManagementClient(RealmRepresentation rep) {
        return hasClient(rep, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
    }

    private boolean hasSSOManagementClient(RealmRepresentation rep) {
        return hasClient(rep, Constants.SSO_MANAGEMENT_CLIENT_ID);
    }

    private boolean hasSSODesktopClient(RealmRepresentation rep) {
        return hasClient(rep, Constants.SSO_DESKTOP_CLIENT_ID);
    }

    private boolean hasBrokerClient(RealmRepresentation rep) {
        return hasClient(rep, Constants.BROKER_SERVICE_CLIENT_ID);
    }

    private boolean hasAdminConsoleClient(RealmRepresentation rep) {
        return hasClient(rep, Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    private boolean hasAdminCliClient(RealmRepresentation rep) {
        return hasClient(rep, Constants.ADMIN_CLI_CLIENT_ID);
    }

    private boolean hasClient(RealmRepresentation rep, String clientId) {
        if (rep.getClients() != null) {
            for (ClientRepresentation clientRep : rep.getClients()) {
                if (clientRep.getClientId() != null && clientRep.getClientId().equals(clientId)) {
                    return true;
                }
            }
        }

        // TODO: Just for compatibility with old versions. Should be removed later...
        if (rep.getApplications() != null) {
            for (ApplicationRepresentation clientRep : rep.getApplications()) {
                if (clientRep.getName().equals(clientId)) {
                    return true;
                }
            }
        }
        if (rep.getOauthClients() != null) {
            for (OAuthClientRepresentation clientRep : rep.getOauthClients()) {
                if (clientRep.getName().equals(clientId)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasRealmRole(RealmRepresentation rep, String roleName) {
        if (rep.getRoles() == null || rep.getRoles().getRealm() == null) {
            return false;
        }

        for (RoleRepresentation role : rep.getRoles().getRealm()) {
            if (roleName.equals(role.getName())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasClientScope(RealmRepresentation rep, String clientScopeName) {
        if (rep.getClientScopes() == null) {
            return false;
        }

        for (ClientScopeRepresentation clientScope : rep.getClientScopes()) {
            if (clientScopeName.equals(clientScope.getName())) {
                return true;
            }
        }

        return false;
    }

    private void setupAuthorizationServices(RealmModel realm) {
        KeycloakModelUtils.setupAuthorizationServices(realm);
    }

    private void setupClientRegistrations(RealmModel realm) {
        DefaultClientRegistrationPolicies.addDefaultPolicies(realm);
    }

    protected void setupCreateRealmApp(RealmModel realm) {
        ClientModel createRealmApp = realm.getClientByClientId(Constants.CREATE_REALM_CLIENT_ID);
        if (createRealmApp == null) {
            createRealmApp = KeycloakModelUtils.createClient(realm, Constants.CREATE_REALM_CLIENT_ID);
            createRealmApp.setName("${client_" + Constants.CREATE_REALM_CLIENT_ID + "}");
            createRealmApp.setEnabled(true);
            createRealmApp.setAlwaysDisplayInConsole(false);
            createRealmApp.setPublicClient(true);
            createRealmApp.setFullScopeAllowed(false);
            createRealmApp.setStandardFlowEnabled(false);
            createRealmApp.setDirectAccessGrantsEnabled(true);
            createRealmApp.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        }
    }

    //Function used to get the files which are to be uploaded in MinIO and uploading on StartUp
    public void uploadEmailTemplatesToMinio(RealmModel realm) {
        Collection<String> templates;
        if (realm.getMode().equalsIgnoreCase("bo"))
            templates = Constants.boEmailTemplates;
        else
            templates = Constants.emailTemplates;
        for (String template : templates) {
            String htmlBody;
            htmlBody = freeMarker.processTemplate(realm, template);

            //Upload the htmlBody to minio
            InputStream emailHtmlTemp = new ByteArrayInputStream(htmlBody.getBytes());
            avatarProvider = session.getProvider(AvatarProvider.class, "avatar-storage-minio");
            avatarProvider.saveEmailTemplate(realm.getName(), removeExtension(template), emailHtmlTemp);
        }
    }

    //Function used to reset the files to default in Minio
    public void resetEmailTemplateInMinio(RealmModel realm, String templateName) {
        //getting htmlBody from default file
        String htmlBody = freeMarker.processTemplate(realm, templateName);
        if (htmlBody == null || htmlBody.trim().length() == 0) {
            throw new RuntimeException("Template not found with the name of " + templateName);
        }

        //Upload the htmlBody to minio
        InputStream emailHtmlTemp = new ByteArrayInputStream(htmlBody.getBytes());
        avatarProvider = session.getProvider(AvatarProvider.class, "avatar-storage-minio");
        avatarProvider.saveEmailTemplate(realm.getName(), removeExtension(templateName), emailHtmlTemp);
    }

    private void fireRealmPostCreate(RealmModel realm) {
        session.getKeycloakSessionFactory().publish(new RealmModel.RealmPostCreateEvent() {
            @Override
            public RealmModel getCreatedRealm() {
                return realm;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

    }

    public void setupClientServiceAccountsAndAuthorizationOnImport(RealmRepresentation rep, boolean skipUserDependent) {
        List<ClientRepresentation> clients = rep.getClients();
        // do not initialize services accounts or authorization if skipUserDependent
        // they need the users and should be done at the end in dir import
        if (clients != null && !skipUserDependent) {
            ClientManager clientManager = new ClientManager(this);

            for (ClientRepresentation client : clients) {
                RealmModel realmModel = this.getRealmByName(rep.getRealm());

                ClientModel clientModel = Optional.ofNullable(client.getId())
                        .map(realmModel::getClientById)
                        .orElseGet(() -> realmModel.getClientByClientId(client.getClientId()));

                if (clientModel == null) {
                    throw new RuntimeException("Cannot find provided client by dir import.");
                }

                UserModel serviceAccount = null;
                if (clientModel.isServiceAccountsEnabled()) {
                    serviceAccount = this.getSession().users().getServiceAccount(clientModel);
                    if (serviceAccount == null) {
                        // initialize the service account if the account is missing
                        clientManager.enableServiceAccount(clientModel);
                    }
                }

                if (Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION) && Boolean.TRUE.equals(client.getAuthorizationServicesEnabled())) {
                    // just create the default roles if the service account was missing in the import
                    RepresentationToModel.createResourceServer(clientModel, session, serviceAccount == null);
                    RepresentationToModel.importAuthorizationSettings(client, clientModel, session);
                }
            }
        }
    }

    public static boolean triggerDNSRemovalReq(KeycloakSession session, RealmModel realm, String domain) {
        AdminEventBuilder adEvt = new AdminEventBuilder(realm, new AdminAuth(session.getContext().getRealm(),
                null, null, realm.getClientByClientId("dashboard")), session,
                session.getContext().getConnection());

        Map<String, String> body = new HashMap<>();
        body.put("domain", domain);
        body.put("realm", realm.getName());

        org.keycloak.sms.utility.HttpUtility httpUtility = new org.keycloak.sms.utility.HttpUtility();
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            okhttp3.Response response = httpUtility.callRequest(javax.ws.rs.core.MediaType.APPLICATION_JSON, "POST",
                    EnvironmentReader.getValue("INGRESS_ENDPOINT") + "/delete", objectMapper.writeValueAsString(body)
                    , null);
            if (response.isSuccessful()) {
                adEvt.operation(OperationType.DNS).resource(ResourceType.DNS_REMOVAL_SUCCESS).representation("DNS removed successfully: " + domain).success();
                realm.removeCustomDomainMapping(domain);
                return true;
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error(e.getMessage());
        }
        adEvt.operation(OperationType.DNS).resource(ResourceType.DNS_REMOVAL_FAILURE).representation("DNS removal failed").success();
        return false;
    }
}

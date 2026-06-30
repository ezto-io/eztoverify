package com.qlikverify.marketplace.templates.apps;

import com.qlikverify.marketplace.TemplateConstants;
import com.qlikverify.marketplace.MessageBundler;
import com.qlikverify.marketplace.templates.apps.base.AppsMarketplaceTemplate;
import org.keycloak.AvatarConstants;
import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.locale.LocaleType;
import org.keycloak.marketplace.InstallResponse;
import org.keycloak.marketplace.MarketplaceException;
import org.keycloak.marketplace.ThirdPartyProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProvider;
import org.keycloak.marketplace.template.MarketplaceTemplateProviderFactory;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.managers.ClientManager;

import java.util.*;

import static com.qlikverify.marketplace.TemplateConstants.APPS_WIN_LOGON_TITLE;

public class WinLogonTemplate extends AppsMarketplaceTemplate implements MarketplaceTemplateProvider, MarketplaceTemplateProviderFactory {

    public static final String WINLOGON_ADMIN_ROLE = "winlogon-admin-role";
    public static final String WINLOGON_USER_ROLE = "winlogon-user-role";
    public static final String WINLOGON_USER_GROUP = "winlogon-user-group";
    public static final String WINLOGON_ADMIN_GROUP = "winlogon-admin-group";
    public static final String CLIENT_ID_WINLOGON_APP = "winlogon-app";
    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);
    private static final String MARKETPLACE = "/marketplace/";

    KeycloakSession session;

    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {
        RealmModel realmModel = session.getContext().getRealm();
        ClientModel accountClient = realmModel.getClientByClientId(CLIENT_ID_WINLOGON_APP);
        if (accountClient != null){
            return new InstallResponse(false, "winlogon-already-installed", "realms/" + session.getContext().getRealm().getName() + "/clients/type/high-secure");
        }else {

            ClientRepresentation rep = new ClientRepresentation();

            rep.setFullScopeAllowed(true);
            rep.setName(APPS_WIN_LOGON_TITLE);
            rep.setAppType("high_secure");
            rep.setProtocol("openid-connect");
            List<String> redirectUris = new ArrayList<>();
            redirectUris.add("https://winlogon/"+realmModel.getName());
            rep.setRedirectUris(redirectUris);
            Map<String, String> attributes = new HashMap<>();
            attributes.put("login_theme", "default");
            rep.setAttributes(attributes);
            rep.setClientId(CLIENT_ID_WINLOGON_APP);
            ClientModel clientModel = ClientManager.createClient(session, realmModel, rep, true);
            clientModel.setStandardFlowEnabled(true);
            clientModel.setImplicitFlowEnabled(false);
            clientModel.setDirectAccessGrantsEnabled(false);
            clientModel.setServiceAccountsEnabled(false);
            clientModel.setBearerOnly(false);
            clientModel.setPublicClient(false);
            clientModel.setAttribute("vault", "true");
            clientModel.setAttribute("logoUrl", getLogo());
            clientModel.setAttribute("hasInBuiltRoles", "true");
            clientModel.setAttribute("templateId",getId());

            String adminRoleName = WINLOGON_ADMIN_ROLE;
            if (realmModel.getRole(adminRoleName) == null) {
                realmModel.addRole(adminRoleName);
            }

            String userRoleName = WINLOGON_USER_ROLE;
            if (realmModel.getRole(userRoleName) == null) {
                realmModel.addRole(userRoleName);
            }

            Optional<GroupModel> groupModelOptional = realmModel.getGroupsStream().filter(x -> x.getName().equals(WINLOGON_USER_GROUP)).findFirst();
            if (!groupModelOptional.isPresent()) {
                RoleMapperModel roleMapperModel = realmModel.createGroup(WINLOGON_USER_GROUP);
                roleMapperModel.grantRole(realmModel.getRole(userRoleName));
            } else {
                RoleMapperModel roleMapperModel = groupModelOptional.get();
                roleMapperModel.grantRole(realmModel.getRole(userRoleName));
            }

            Optional<GroupModel> adminGroupModelOptional = realmModel.getGroupsStream().filter(x -> x.getName().equals(WINLOGON_ADMIN_GROUP)).findFirst();
            if (!adminGroupModelOptional.isPresent()) {
                RoleMapperModel roleMapperModel = realmModel.createGroup(WINLOGON_ADMIN_GROUP);
                roleMapperModel.grantRole(realmModel.getRole(adminRoleName));
            } else {
                RoleMapperModel roleMapperModel = adminGroupModelOptional.get();
                roleMapperModel.grantRole(realmModel.getRole(adminRoleName));
            }
        }

        return new InstallResponse(true, null, "realms/" + session.getContext().getRealm().getName() + "/clients/type/high-secure");
    }

    @Override
    public InstallResponse update(KeycloakSession session,ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        return new InstallResponse(true, null, null);
    }

    @Override
    public InstallResponse onCleanUp(RealmModel realmModel, ClientModel clientModel) {
        RoleModel userRoleModel = realmModel.getRole(WINLOGON_USER_ROLE);
        if (userRoleModel != null) {
            realmModel.removeRole(userRoleModel);
        }

        RoleModel adminRoleModel = realmModel.getRole(WINLOGON_ADMIN_ROLE);
        if (adminRoleModel != null) {
            realmModel.removeRole(adminRoleModel);
        }
        Optional<GroupModel> groupModelOptional = realmModel.getGroupsStream().filter(x -> x.getName().equals(WINLOGON_USER_GROUP)).findFirst();
        if (groupModelOptional.isPresent()) {
            realmModel.removeGroup(groupModelOptional.get());
        }

        Optional<GroupModel> adminGroupModelOptional = realmModel.getGroupsStream().filter(x -> x.getName().equals(WINLOGON_ADMIN_GROUP)).findFirst();
        if (adminGroupModelOptional.isPresent()) {
            realmModel.removeGroup(adminGroupModelOptional.get());
        }
        return new InstallResponse(true, null, null);
    }

    @Override
    public String getLogo() {
        return DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.APPS_WIN_LOGON_TEMP + "/logo.svg";
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return "Windows LogOn";
    }

    @Override
    public String getAuthor(KeycloakSession session) {
        return session == null ? TemplateConstants.QV_AUTHOR : session.rootBrand().getMasterDisplayName();
    }

    @Override
    public String getCategory() {
        return Constants.APPS;
    }

    @Override
    public String getDescription(KeycloakSession session) {
        Locale locale = session.getContext().resolveLocale(null, LocaleType.ADMIN);
        return MessageBundler.get(locale,TemplateConstants.APPS_WIN_LOGON_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.APPS_WIN_LOGON_TEMP + "/overview.md";

    }

    @Override
    public String getInstallationMdUrl() {
        return DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.APPS_WIN_LOGON_TEMP + "/Windows-LogOn.md";

    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[]{"windows", "window_desktop", "windows_login", "windows_logon"};

    }

    @Override
    public ConstraintResponse resolveConstraints(KeycloakSession session) {
        return null;
    }

    @Override
    public Map<String, Object> getMarketProviders(KeycloakSession session) {
        return new HashMap<>();
    }

    @Override
    public String installType() {
        return TemplateConstants.ONCE;
    }

    @Override
    public boolean isInstalled(KeycloakSession session) {
        return session.getContext().getRealm().getClientByClientId(CLIENT_ID_WINLOGON_APP) != null;
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return configProperties;
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config, boolean isUpdate) {
        return new ArrayList<>();
    }

    @Override
    public ThirdPartyProvider get3rdPartyProvider() {
        return null;
    }

    @Override
    public MarketplaceTemplateProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return "d8144a7f-ebcc-459b-bbc3-3e669d5a5041";
    }
}

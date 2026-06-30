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

import static com.qlikverify.marketplace.TemplateConstants.SSO_DESKTOP_TITLE;

public class SsoDesktopTemplate extends AppsMarketplaceTemplate implements MarketplaceTemplateProvider, MarketplaceTemplateProviderFactory {

    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);
    private static final String MARKETPLACE = "/marketplace/";
    private static final String ACTION = "action";

    KeycloakSession session;
    @Override
    public InstallResponse install(KeycloakSession session, Map<String, String> config) throws MarketplaceException {
        RealmModel realmModel = session.getContext().getRealm();
        ClientModel accountClient = realmModel.getClientByClientId(Constants.SSO_DESKTOP_CLIENT_ID);
        if (accountClient != null){
            return new InstallResponse(false, "sso-desktop-already-installed",
                    "realms/" + session.getContext().getRealm().getName() + "/clients/type/high-secure");
        }else {
            ClientRepresentation rep = new ClientRepresentation();
            rep.setName(SSO_DESKTOP_TITLE);
            rep.setAppType("high_secure");
            rep.setProtocol("openid-connect");
            List<String> redirectUris = new ArrayList<>();
            String desktopLoginRedirectUri = "https://ssodesktop/login/"
                    + realmModel.getName().toLowerCase();
            String desktopLogoutRedirectUri = "https://ssodesktop/logout/"
                    + realmModel.getName().toLowerCase();
            redirectUris.add(desktopLoginRedirectUri);
            redirectUris.add(desktopLogoutRedirectUri);
            redirectUris.add("https://oauth.pstmn.io/v1/callback");
            rep.setRedirectUris(redirectUris);
            Map<String, String> attributes = new HashMap<>();
            attributes.put("login_theme", "default");
            rep.setAttributes(attributes);
            rep.setClientId(Constants.SSO_DESKTOP_CLIENT_ID);
            ClientModel clientModel = ClientManager.createClient(session, realmModel, rep, true);

            clientModel.setImplicitFlowEnabled(false);
            clientModel.setStandardFlowEnabled(true);
            clientModel.setDirectAccessGrantsEnabled(false);
            clientModel.setServiceAccountsEnabled(false);
            clientModel.setBearerOnly(false);
            clientModel.setPublicClient(true);
            clientModel.setAttribute("logoUrl",getLogo());

            setupDefaultActionAndPoliciesSSO(realmModel, session, clientModel, Constants.SSO_DESKTOP_CLIENT_ID);
        }

        return new InstallResponse(true, null, "realms/" + session.getContext().getRealm().getName() + "/clients/type/high-secure");

    }

    @Override
    public InstallResponse update(KeycloakSession session,ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        return new InstallResponse(true, null, null);
    }

    @Override
    public InstallResponse onCleanUp(RealmModel realmModel, ClientModel clientModel) {
        return MarketplaceTemplateProvider.super.onCleanUp(realmModel, clientModel);
    }

    @Override
    public String getLogo() {
        return DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.APPS_SSO_DESKTOP_TEMP + "/logo.svg";
    }

    @Override
    public String getTitle(KeycloakSession session) {
        Locale locale = session.getContext().resolveLocale(null,LocaleType.ADMIN);
        return MessageBundler.get(locale,SSO_DESKTOP_TITLE);
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
        return MessageBundler.get(locale,TemplateConstants.APPS_SSO_DESKTOP_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.APPS_SSO_DESKTOP_TEMP + "/overview.md";
    }

    @Override
    public String getInstallationMdUrl() {
        return DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.APPS_SSO_DESKTOP_TEMP + "/SSO-Desktop.md";
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[] {"sso", "sso_desktop", "desktop_sso", "desktop"};
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
        return session.getContext().getRealm().getClientByClientId(Constants.SSO_DESKTOP_CLIENT_ID) != null;
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
        return "204a8ae7-8d55-4188-92eb-705f24a47ff1";
    }

    private void setupDefaultActionAndPoliciesSSO(RealmModel realm, KeycloakSession session,ClientModel adminConsole,
                                                  String clientId) {
        PolicyModel model = new PolicyModel();

        model.setPolicyName(clientId + "_temppwd_policy");
        model.setPolicyType(ACTION);
        model.setPolicyValue("temppassword() and decision()");
        model.setRealmPolicy(false);
        model.setInBuilt(true);
        model = session.policies().addPolicy(realm, model);


        PolicyModel emailPolicyModel = new PolicyModel();

        emailPolicyModel.setPolicyName(clientId + "_email_policy");
        emailPolicyModel.setPolicyType(ACTION);
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
        emailRA.setPriority(1);
        emailRA.setEnabled(true);
        emailRA.setActionId(actionModel.getId());
        realm.addRequiredActionProvider(emailRA);

        adminConsole.setAuthenticationFlowBindingOverride(ACTION, actionModel.getId());
    }
}

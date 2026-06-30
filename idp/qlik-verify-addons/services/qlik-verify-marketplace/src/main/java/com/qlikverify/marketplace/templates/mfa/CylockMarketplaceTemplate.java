package com.qlikverify.marketplace.templates.mfa;

import com.qlikverify.marketplace.TemplateConstants;
import com.qlikverify.marketplace.MessageBundler;
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

import java.util.*;

import static com.qlikverify.marketplace.TemplateConstants.MARKETPLACE;

public class CylockMarketplaceTemplate implements MarketplaceTemplateProvider, MarketplaceTemplateProviderFactory {

    private static final String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

    private static final String MFA_ID = "cylock-mfa";
    KeycloakSession session;
    @Override
    public InstallResponse install(KeycloakSession session, Map<String,String> config) throws MarketplaceException {
        RealmModel realmModel = session.getContext().getRealm();
        if (realmModel.getMarketplaceConfigsStream().filter(x -> x.getProviderId().equalsIgnoreCase(MFA_ID)).count() == 0) {
            MarketplaceConfigModel marketplaceConfigModel = new MarketplaceConfigModel();
            marketplaceConfigModel.setRealmId(session.getContext().getRealm().getId());
            marketplaceConfigModel.setProviderId(MFA_ID);
            marketplaceConfigModel.setProviderType(Constants.MFA);
            realmModel.addMarketplaceConfig(marketplaceConfigModel);
            return new InstallResponse(true,null,"realms/"+session.getContext().getRealm().getName()+"/connections/mfa/create");
        }
        return new InstallResponse(false,"mfa-template-already-installed","realms/"+session.getContext().getRealm().getName()+"/connections/mfa/create");
    }

    @Override
    public InstallResponse update(KeycloakSession session, ClientRepresentation rep, ClientModel clientModel, Map<String, String> config) throws MarketplaceException {
        return null;
    }

    @Override
    public String getLogo() {
        return DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.MFA_CYLOCK_TEMP + "/logo.svg";
    }

    @Override
    public String getTitle(KeycloakSession session) {
        return TemplateConstants.MFA_CYLOCK_TITLE;
    }

    @Override
    public String getAuthor(KeycloakSession session) {
        return session == null ? TemplateConstants.QV_AUTHOR : session.rootBrand().getMasterDisplayName();
    }

    @Override
    public String getCategory() {
        return Constants.MFA;
    }

    @Override
    public String getDescription(KeycloakSession session) {
        Locale locale = session.getContext().resolveLocale(null, LocaleType.ADMIN);
        return MessageBundler.get(locale,TemplateConstants.MFA_CYLOCK_DESC);
    }

    @Override
    public String getOverviewMdUrl() {
        return DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.MFA_CYLOCK_TEMP + "/overview.md";
    }

    @Override
    public String getInstallationMdUrl() {
        return DEFAULT_SERVER_URL + MARKETPLACE + TemplateConstants.MFA_CYLOCK_TEMP + "/installation.md";
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public String[] getKeywords() {
        return new String[0];
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
        return session.getContext().getRealm().getMarketplaceConfigsStream().filter(x -> x.getProviderId().equalsIgnoreCase(MFA_ID)).count() > 0;
    }

    @Override
    public boolean isRecommended() {
        return false;
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
        return "7f17eca0-0874-4e17-aeff-9d5345b3dc2e";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return new ArrayList<>();
    }

    @Override
    public List<String> validateConfig(KeycloakSession session, Map<String, String> config, boolean isUpdate) {
        return new ArrayList<>();
    }

    @Override
    public ThirdPartyProvider get3rdPartyProvider() {
        return new ThirdPartyProvider() {
            @Override
            public String getName() {
                return "Cybernexa";
            }

            @Override
            public String getWebsiteUrl() {
                return "https://cybernexa.com";
            }

            @Override
            public String getLogoUrl() {
                return "https://cybernexa.com/assets/images/logo.png";
            }

            @Override
            public String getPrivacyUrl() {
                return null;
            }

            @Override
            public String getSalesUrl() {
                return "https://cybernexa.com/pricing.html";
            }

            @Override
            public String getTermsUrl() {
                return null;
            }
        };
    }

}

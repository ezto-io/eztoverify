package com.qlikverify.services.theme;

import org.keycloak.Config;
import org.keycloak.EnvironmentReader;
import org.keycloak.branding.RootBrandProvider;
import org.keycloak.branding.RootBrandProviderFactory;
import org.keycloak.common.Version;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static com.qlikverify.services.theme.onboarding.Constants.*;

public class QlikVerifyBrandProvider implements RootBrandProvider, RootBrandProviderFactory {
    private static final List<String> whitelistedMarketplaceCategories = new ArrayList<>();
    KeycloakSession session;

    static {
        whitelistedMarketplaceCategories.add(Constants.SSO);
        whitelistedMarketplaceCategories.add(Constants.IWS_MARKETPLACE);
        whitelistedMarketplaceCategories.add(Constants.APPS);
    }

    @Override
    public QlikVerifyBrandProvider create(KeycloakSession session) {
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
    public String getId() {
        return "qlikverify-theme";
    }

    @Override
    public String getWelcomeTheme() {
        return QLIK_VERIFY;
    }

    @Override
    public String getSSOTheme() {
        return QLIK_VERIFY;
    }

    @Override
    public String getLoginTheme() {
        return QLIK_VERIFY;
    }

    @Override
    public String getAccountTheme() {
        return QLIK_VERIFY;
    }

    @Override
    public String getEmailTheme() {
        return QLIK_VERIFY;
    }

    @Override
    public String getAdminTheme() {
        return QLIK_VERIFY;
    }

    @Override
    public boolean isBrandingEnabled() {
        return true;
    }

    @Override
    public boolean isModeEnabled() {
        return true;
    }

    @Override
    public Map<String, String> getAdminBrandAttributes(RealmModel realmModel) {
        Map<String, String> brandingAttributes = new HashMap<>();
        if (realmModel.getMode().equals("bo")) {
            brandingAttributes.put(PRIMARY, BTN_PRIMARY_VALUE_BO);
            brandingAttributes.put(BTN_PRIMARY, BTN_PRIMARY_VALUE_BO);
            brandingAttributes.put(TEXT_PRIMARY, BTN_PRIMARY_VALUE_BO);
            brandingAttributes.put(BTN_PRIMARY_HOVER, "#164FC2");
            brandingAttributes.put(BTN_SECONDARY, BUTTON_SECONDARY_VALUE);
            brandingAttributes.put(BTN_SECONDARY_HOVER, BUTTON_HOVER_VALUE);
            brandingAttributes.put(PRIMARY_OPAQUE, "rgba(4,123,248,0.25)");
        } else if (realmModel.getMode().equals("ciam")) {
            brandingAttributes.put(PRIMARY, BTN_PRIMARY_VALUE_CIAM);
            brandingAttributes.put(BTN_PRIMARY, BTN_PRIMARY_VALUE_CIAM);
            brandingAttributes.put(TEXT_PRIMARY, BTN_PRIMARY_VALUE_CIAM);
            brandingAttributes.put(BTN_PRIMARY_HOVER, "#080808");
            brandingAttributes.put(BTN_SECONDARY, "#c0c0c0");
            brandingAttributes.put(BTN_SECONDARY_HOVER, "#8b8888");
            brandingAttributes.put(PRIMARY_OPAQUE, "rgba(50, 50, 50, 0.25)");
        } else if (realmModel.getMode().equals("wiam")){
            brandingAttributes.put(PRIMARY, BTN_PRIMARY_VALUE_WIAM);
            brandingAttributes.put(BTN_PRIMARY, BTN_PRIMARY_VALUE_WIAM);
            brandingAttributes.put(TEXT_PRIMARY, "#3d7f48");
            brandingAttributes.put(BTN_PRIMARY_HOVER, "#45914F");
            brandingAttributes.put(BTN_SECONDARY, BUTTON_SECONDARY_VALUE);
            brandingAttributes.put(BTN_SECONDARY_HOVER, BUTTON_HOVER_VALUE);
            brandingAttributes.put(PRIMARY_OPAQUE, "rgba(76, 163, 85, 0.25)");
        }
        brandingAttributes.put("btnDanger", "#f04141");
        brandingAttributes.put("btnDangerHover", "#dc3545");
        brandingAttributes.put("fontFamily", "Inter, sans-serif");
        brandingAttributes.put("bo-primary", "#a4243b");
        brandingAttributes.put("ciam-primary", "#0d1821");
        return brandingAttributes;
    }

    @Override
    public String getMasterDisplayName() {
        return EnvironmentReader.getValue("VARIANT", "ezto verify");
    }

    @Override
    public void onTenantBrandImageUpdate(RealmModel realmModel, String url) {
        // here no code required
    }

    @Override
    public String getLogoUrl() {
        if(session.getContext().getRealm().getMode().equals("bo")){
            return "default-verify-logo";
        }else{
            return "default-auth-logo";
        }
    }

    @Override
    public boolean isCibaFeatureEnabled(RealmModel realmModel) {
        return !((Version.OFFERING.equalsIgnoreCase(ON_PREMISE) && Version.DISABLED_FEATURES.contains("ciba"))
                || (Version.OFFERING.equalsIgnoreCase(CLOUD)
                        && (realmModel.getMode().equals("wiam") || realmModel.getMode().equals("ciam"))));
    }

    @Override
    public boolean isIWSFeatureEnabled(RealmModel realmModel) {
        return !((Version.OFFERING.equalsIgnoreCase(ON_PREMISE) && Version.DISABLED_FEATURES.contains("iws"))
                || (Version.OFFERING.equalsIgnoreCase(CLOUD) && realmModel.getMode().equals("ciam")
                        || realmModel.getMode().equals("bo")));
    }

    @Override
    public boolean isOIDCEnabled(RealmModel realmModel) {
        return !((Version.OFFERING.equalsIgnoreCase(ON_PREMISE) && Version.DISABLED_FEATURES.contains("oidc"))
                || (Version.OFFERING.equalsIgnoreCase(CLOUD) && realmModel.getMode().equals("bo")));
    }

    @Override
    public boolean isThirdPartyRequestEnabled() {
        return !(Version.OFFERING.equalsIgnoreCase(ON_PREMISE)
                && Version.DISABLED_FEATURES.contains("thirdpartyrequest"));
    }

    @Override
    public List<String> whitelistedMarketplace(RealmModel realmModel) {
        if (Version.OFFERING.equalsIgnoreCase(ON_PREMISE)) {
            return Version.WHITELIST_MARKETPLACE;
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> whitelistedAuthenticators(RealmModel realmModel) {
        return new ArrayList<>();
    }

    @Override
    public List<String> whitelistedMarketplaceCategories(RealmModel realmModel) {
        if (realmModel.getMode().equals("ciam"))
            return whitelistedMarketplaceCategories;

        return new ArrayList<>();
    }

    @Override
    public List<Enum> toExcludeEvtTypes() {
        return new ArrayList<>();
    }

    @Override
    public void close() {
        // NOP
    }
}

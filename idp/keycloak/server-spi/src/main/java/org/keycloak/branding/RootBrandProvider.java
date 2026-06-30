package org.keycloak.branding;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;

/**
 * Provider to provide whitelabeling details of respective customer
 */
public interface RootBrandProvider extends Provider {

    /**
     * Get Welcome theme
     * @return
     */
    String getWelcomeTheme();

    /**
     * Get SSO theme
     * @return
     */
    String getSSOTheme();

    /**
     * Get Login theme
     * @return
     */
    String getLoginTheme();

    /**
     * Get Account theme
     * @return
     */
    String getAccountTheme();

    /**
     * Get Email theme
     * @return
     */
    String getEmailTheme();

    /**
     * Get Admin theme, here it will be always qlik_verify. this is kept open for future options
     * @return
     */
    String getAdminTheme();

    /**
     * It will return to say if the brand support dynamic branding or not
     * @return
     */
    boolean isBrandingEnabled();

    /**
     * It will return to say if the realm has mode support to select ciam or wiam
     * @return
     */
    boolean isModeEnabled();

    /**
     * Returns admin dashboard branding attributes, this is supported only for qlik_verify brand and in case of customer specific changes this will be returned as null
     * Following are the keys supported for now
     * logoName
     * primaryColor
     * secondaryColor
     * buttonColor
     * textColor
     * fontFamily
     * @return
     */
    Map<String,String> getAdminBrandAttributes(RealmModel realmModel);

    /**
     * Returns the master tenant display name
     * @return
     */
    String getMasterDisplayName();

    /**
     * Updates the Tenant brand image to respective third party providers
     * @param realmModel
     * @param url
     */
    void onTenantBrandImageUpdate(RealmModel realmModel, String url);

    /**
     * Returns hosted logo url link
     * @return
     */
    String getLogoUrl();

    /**
     * It will return to say if the ciba feature supported or not
     * @return
     */
    boolean isCibaFeatureEnabled(RealmModel realmModel);

    /**
     * Return if the IWS Feature supported or not
     * @return
     */
    boolean isIWSFeatureEnabled(RealmModel realmModel);

    boolean isOIDCEnabled(RealmModel realmModel);

    /**
     * Return if the Request for 3rd party integration feature in marketplace
     * supported or not
     * @return
     */

    boolean isThirdPartyRequestEnabled();

    /**
     * Return the list of whitelisted Marketplace providers
     * @return
     */
    List<String> whitelistedMarketplace(RealmModel realmModel);
    List<String> whitelistedAuthenticators(RealmModel realmModel);
    List<String> whitelistedMarketplaceCategories(RealmModel realmModel);

    List<Enum> toExcludeEvtTypes();
}

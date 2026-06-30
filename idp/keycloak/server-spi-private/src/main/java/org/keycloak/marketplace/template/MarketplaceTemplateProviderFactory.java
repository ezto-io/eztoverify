package org.keycloak.marketplace.template;

import org.keycloak.events.models.ConstraintResponse;
import org.keycloak.marketplace.ThirdPartyProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.util.List;
import java.util.Map;

public interface MarketplaceTemplateProviderFactory extends ProviderFactory<MarketplaceTemplateProvider> {

    /**
     * Returns the template logo for the item in marketplace
     * @return
     */
    String getLogo();

    /**
     * Returns the template name which will be shown in ui
     * @return
     */
    String getTitle(KeycloakSession session);

    /**
     * Returns the author of template
     * @return
     */
    String getAuthor(KeycloakSession session);

    /**
     * Category of template, for user understanding
     * @return
     */
    String getCategory();

    /**
     * Description of template, for user understanding
     * @return
     */
    String getDescription(KeycloakSession session);
//    default String getDescription(KeycloakSession session){
//        return String.valueOf(Locale.ENGLISH);
//    }
    /**
     * Returns the OverView Markdown URL of template
     * @return
     */
    String getOverviewMdUrl();

    /**
     * Returns the Installation  Markdown URL of template
     * @return
     */
    String getInstallationMdUrl();

    /**
     * Return if the template is new, this will add badge to showcase
     * @return
     */
    boolean isNew();

    /**
     * Returns the list of keywords that this template shall be used
     * @return
     */
    String[] getKeywords();

    /**
     * Returns the list of constraints needs to be resolved to install template
     */
    ConstraintResponse resolveConstraints(KeycloakSession session);

    Map<String,Object> getMarketProviders(KeycloakSession session);

    String installType();

    boolean isInstalled(KeycloakSession session);

    boolean isRecommended();

    List<ProviderConfigProperty> getConfigProperties(KeycloakSession session);

    List<String> validateConfig(KeycloakSession session, Map<String, String> config, boolean isUpdate);


    /**
     * Retruns if any 3rd party providers are involved in the template
     * @return
     */
    ThirdPartyProvider get3rdPartyProvider();

    default boolean isImplemented(){
        return true;
    }

    default String buttonNameBeforeInstall(){
        return "install";
    }

    default String buttonNameAfterInstall(){
        return "installed";
    }

    default String[] serviceLocations(){
        return null;
    }

    default boolean arrange1to1Meetings(){
        return false;
    }

    default String meetingLink(){
        return "https://calendly.com/lokesh-ravi";
    }

    default boolean showTrialOption(RealmModel realmModel){
        return false;
    }

    default long showTrialDays(RealmModel realmModel){
        return 15;
    }

    default boolean isUnderTrial(RealmModel realmModel){
        return false;
    }
}

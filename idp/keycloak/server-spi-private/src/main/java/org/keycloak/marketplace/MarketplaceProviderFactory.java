package org.keycloak.marketplace;

import org.keycloak.provider.ProviderFactory;

import java.util.List;

public interface MarketplaceProviderFactory extends ProviderFactory<MarketplaceProvider> {
    /**
     * Returns the template name which will be shown in ui
     * @return
     */
    String getTemplateName();

    /**
     * Description of template, for user understanding
     * @return
     */
    String getTemplateDescription();


    /**
     * Returns gif or video link to show how the template will work in live
     * @return
     */
    String getHowItWorks();

    /**
     * Returns type of the template
     * @return
     */
    TemplateType getTemplateType();

    /**
     * Returns the json or content to build the flow
     * @return
     */
    FlowMaps getFlow();

    /**
     * Returns the list of keywords that this template shall be used
     * @return
     */
    List<String> getKeywords();

    /**
     * Return if the template is new, this will add badge to showcase
     * @return
     */
    boolean isNew();


}

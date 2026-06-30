package com.qlikverify.services.resources;

import com.qlikverify.services.eventlisteners.blacklisting.BlacklistingEventListenerProviderFactory;
import org.jboss.logging.Logger;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.EventConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import javax.ws.rs.*;
import java.util.*;

import static com.qlikverify.services.constants.Constants.*;

public class BlacklistingResource {

    //~ Static fields/initializers =========================================================================================================

    private static final Logger logger = Logger.getLogger(BlacklistingResource.class);
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminEventBuilder adminEvent;
    private static final String LOG_TAG = "EZTO:BLACKLISTING_RESOURCE: ";

    //~ Constructor ========================================================================================================================

    public BlacklistingResource(KeycloakSession session, RealmModel realm, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.adminEvent = adminEvent;
    }

    //~ API ================================================================================================================================

    @GET
    @Path("/")
    public List<ProviderConfigProperty> blacklistConfig(){
        logger.info(LOG_TAG + "enetered blacklistConfig()");
        List<ProviderConfigProperty> providerConfigProperties = new BlacklistingEventListenerProviderFactory().getConfigProperties(session);
        if(providerConfigProperties == null){
            logger.error(LOG_TAG + "blacklistConfig: provider config properties not found");
            return Collections.emptyList();
        }
        EventConfigModel eventConfigModel = realm.getEventConfigByProviderId(BLACKLISTING_PROVIDER_ID);
        if(eventConfigModel == null) {
            logger.error(LOG_TAG + "blacklistConfig: config model is null");
            return providerConfigProperties;
        }
        Map<String, String> eventConfig = eventConfigModel.getConfig();
        providerConfigProperties.forEach(x-> x.setDefaultValue(eventConfig != null ? eventConfig.get(x.getName()) : ""));
        return providerConfigProperties;
    }

    //~ ====================================================================================================================================

    @PUT
    @Path("/")
    public List<ProviderConfigProperty> updateBlacklistConfig(List<ProviderConfigProperty> config){
        logger.info(LOG_TAG + "entered updateBlacklistConfig()");
        if(config == null){
            logger.error(LOG_TAG + "updateBlacklistConfig: config not available to update");
            return Collections.emptyList();
        }

        EventConfigModel eventConfigModel = new EventConfigModel();
        Map<String, String> eventConfig = new HashMap<>();
        config.forEach(x -> eventConfig.put(x.getName(), x.getDefaultValue() != null ?  x.getDefaultValue().toString() : ""));
        eventConfigModel.setConfig(eventConfig);
        eventConfigModel.setEventProviderId(BLACKLISTING_PROVIDER_ID);

        Optional<ProviderConfigProperty> blacklistingEnabled = config.stream().filter(
                x-> x.getName().equalsIgnoreCase("bl-enable")
        ).findFirst();

        blacklistingEnabled.ifPresent(providerConfigProperty ->
                realm.setAttribute(BLACKLISTING_ENABLED, providerConfigProperty.getDefaultValue().toString())
        );

        if (realm.getEventConfigByProviderId(BLACKLISTING_PROVIDER_ID) == null)
            realm.addEventConfig(eventConfigModel);
        else
            realm.updateEventConfig(eventConfigModel);

        adminEvent.operation(OperationType.UPDATE)
                .resourcePath(session.getContext().getUri())
                .representation(List.of("black listing config updated ",realm.getEventConfigByProviderId(BLACKLISTING_PROVIDER_ID).getConfig()))
                .success();
        return config;
    }
}

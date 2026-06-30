package org.keycloak.protocol.oidc.grants.ciba.tutorial;

import org.keycloak.Config;
import org.keycloak.ciba.CibaNotificationProvider;
import org.keycloak.ciba.CibaTutorialProvider;
import org.keycloak.ciba.CibaTutorialProviderFactory;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CibaStep3Provider implements CibaTutorialProvider, CibaTutorialProviderFactory {
    KeycloakSession session;
    UserModel userModel;

    @Override
    public CibaTutorialProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "ciba-step3";
    }

    @Override
    public String getDisplayName() {
        return "Step 3";
    }

    @Override
    public CibaTutorialProvider setUserModel(UserModel userModel) {
        this.userModel = userModel;
        return this;
    }

    @Override
    public String getDescription() {
        return "Verify the result";
    }

    @Override
    public Map<String, Object> getCommand(ClientModel clientModel, String actionFlow) {
        Map<String, Object> result = new HashMap<>();
        RealmModel realm = session.getContext().getRealm();
        CibaConfig cibaConfig = realm.getCibaPolicy();
        String notProviderId = cibaConfig.getBackchannelTokenDeliveryMode(clientModel);
        CibaNotificationProvider cibaNotificationProvider = session.getProvider(CibaNotificationProvider.class, notProviderId);
        if (cibaNotificationProvider == null) {
            result.put("error", "No Result Notification way is set for the application");
            return result;
        }

        CibaNotificationConfigModel configModel = session.getContext().getRealm().getCibaNotificationConfigByProviderId(notProviderId, clientModel.getClientId(), realm.getId());
        if (configModel == null) {
            result.put("error", "Invalid config settings for selected result notification way");
            return result;
        }

        result.put("data", cibaNotificationProvider.getStep3(clientModel, userModel, configModel));
        return result;
    }

    @Override
    public boolean onExecute(Map<String, String> attributes) {
        return false;
    }

    @Override
    public boolean hasExecution() {
        return true;
    }

    @Override
    public int order() {
        return 2;
    }

    @Override
    public List<ProviderFactory> getConfig(KeycloakSession session) {
        return null;
    }

}

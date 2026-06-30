package org.keycloak.protocol.oidc.grants.ciba.tutorial;

import org.keycloak.Config;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.ciba.CibaTutorialProvider;
import org.keycloak.ciba.CibaTutorialProviderFactory;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CibaStep2Provider implements CibaTutorialProvider, CibaTutorialProviderFactory {
    KeycloakSession session;
    UserModel userModel;

    @Override
    public String getDisplayName() {
        return "Step 2";
    }

    @Override
    public CibaTutorialProvider setUserModel(UserModel userModel) {
        this.userModel = userModel;
        return this;
    }

    @Override
    public String getDescription() {
        return "Verify the identity";
    }

    @Override
    public Map<String, Object> getCommand(ClientModel clientModel, String actionFlow) {
        Map<String, Object> result = new HashMap<>();
        String cibaAuthorizer = clientModel.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        RealmModel realm = session.getContext().getRealm();
        CibaAuthProvider cibaAuthProvider = session.getProvider(CibaAuthProvider.class, cibaAuthorizer);
        if (cibaAuthProvider == null) {
            result.put("error", "No Authentication way is set for the application");
            return result;
        }
        CibaAuthConfigModel cibaAuthConfigModel = session.getContext().getRealm().getCibaAuthConfigByProviderId(cibaAuthorizer, clientModel.getClientId(), realm.getId());
        if (cibaAuthConfigModel == null) {
            result.put("error", "Invalid config settings for selected authentication way");
            return result;
        }

        result.put("data", cibaAuthProvider.getStep2(clientModel, userModel, cibaAuthConfigModel));
        return result;
    }

    @Override
    public boolean onExecute(Map<String, String> attributes) {
        return false;
    }

    @Override
    public boolean hasExecution() {
        return false;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public List<ProviderFactory> getConfig(KeycloakSession session) {
        return null;
    }

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
        return "ciba-step2";
    }

}

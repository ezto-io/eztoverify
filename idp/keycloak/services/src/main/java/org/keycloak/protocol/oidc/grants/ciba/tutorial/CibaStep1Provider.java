package org.keycloak.protocol.oidc.grants.ciba.tutorial;

import com.qlikverify.helpers.GeneralHelper;
import org.keycloak.Config;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.ciba.CibaTutorialProvider;
import org.keycloak.ciba.CibaTutorialProviderFactory;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.urls.UrlType;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CibaStep1Provider implements CibaTutorialProvider, CibaTutorialProviderFactory {
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
        return "ciba-step1";
    }

    @Override
    public String getDisplayName() {
        return "Step 1";
    }

    @Override
    public CibaTutorialProvider setUserModel(UserModel userModel) {
        this.userModel = userModel;
        return this;
    }

    @Override
    public String getDescription() {
        return "Trigger the request";
    }

    @Override
    public Map<String, Object> getCommand(ClientModel clientModel, String actionFlow) {
        Map<String, Object> result = new HashMap<>();
        String cibaAuthorizer = clientModel.getAuthenticationFlowBindingOverride(AuthenticationFlowBindings.CIBA_AUTHORIZER);
        RealmModel realm = session.getContext().getRealm();
        CibaAuthProvider cibaAuthProvider = session.getProvider(CibaAuthProvider.class, cibaAuthorizer);
        if(cibaAuthProvider == null)
        {
            result.put("error", "No Authentication way is set for the application");
            return result;
        }
        CibaAuthConfigModel cibaAuthConfigModel = session.getContext().getRealm().getCibaAuthConfigByProviderId(cibaAuthorizer, clientModel.getClientId(), realm.getId());
        if(cibaAuthConfigModel == null) {
            result.put("error", "Invalid config settings for selected authentication way");
            return result;
        }

        HashMap<String, String> data = new HashMap<>();

        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        String authUrl = CibaGrantType.authorizationUrl(backendUriInfo.getBaseUriBuilder()).build(session.getContext().getRealm().getName()).toString();
        String appId = clientModel.isSandboxMode() ? clientModel.getSandboxClientId() : clientModel.getClientId();
        String appSecret = clientModel.isSandboxMode() ? clientModel.getSandboxSecret() : clientModel.getSecret();

        // this is specific to testing workflow modules, the curl for api-authorizer would be generated in the frontend
        if (GeneralHelper.isV0Supported(realm)) {
            data.put("v0", "curl --location --request POST '" + authUrl + "' \n--header 'Content-Type:application/x-www-form-urlencoded' \n--data-urlencode 'app_id=" + appId + "' \n--data-urlencode 'app_secret=" + appSecret + "' \n--data-urlencode 'request="+cibaAuthProvider.getStep1(clientModel, userModel, cibaAuthConfigModel, actionFlow)+"'");
        }

        data.put("v1", "curl --location --request POST '" + authUrl + "' \n--header 'Content-Type:application/x-www-form-urlencoded' \n --header 'api-version: 1' \n --header 'Authorization: " + BasicAuthHelper.createHeader(appId,appSecret) + "' \n--data-urlencode 'request="+cibaAuthProvider.getStep1(clientModel, userModel, cibaAuthConfigModel, actionFlow)+"'");

        result.put("data", data);
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
        return 0;
    }

    @Override
    public List<ProviderFactory> getConfig(KeycloakSession session) {
        return null;
    }

}

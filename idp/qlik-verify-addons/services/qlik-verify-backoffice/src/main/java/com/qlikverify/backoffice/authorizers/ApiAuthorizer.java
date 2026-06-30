package com.qlikverify.backoffice.authorizers;

import com.qlikverify.backoffice.GeneralHelper;
import org.keycloak.Config;
import org.keycloak.ciba.AuthenticationChannelRequest;
import org.keycloak.ciba.CibaAuthProvider;
import org.keycloak.ciba.CibaAuthProviderFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.grants.ciba.AbstractCibaAuthorizer;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiAuthorizer  extends AbstractCibaAuthorizer implements CibaAuthProvider, CibaAuthProviderFactory {
    @Override
    public CibaAuthProvider setEventBuilder(EventBuilder eventBuilder) {
        this.event = eventBuilder;
        return this;
    }

    @Override
    public boolean onAuthorize(ClientModel clientModel, RealmModel realm, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, AuthenticationChannelRequest request, CibaStateModel cibaStateModel) {
        realm.addAttribute(cibaStateModel.getRequestId(),"transaction_is_api", String.valueOf(true));
        return true;
    }

    @Override
    public void onAuthCompleted(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, String callbackUrl, CibaStateModel cibaState, EventBuilder event, Map<String, Object> response) {

    }

    @Override
    public void onAuthFailed(ClientModel clientModel, UserModel userModel, String callbackUrl, CibaStateModel cibaState, EventBuilder event, Map<String, Object> response) {

    }

    @Override
    public boolean validateUserDetails(UserModel user, RealmModel realm, CibaAuthConfigModel cibaAuthConfigModel, String requestId) {
        return true;
    }

    @Override
    public String getStep1(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel, String actionFlow) {
        String data = "{}";

        return GeneralHelper.updateUserMetadata(session, clientModel, data, session.getContext().getRealm(), actionFlow);
    }

    @Override
    public String getStep2(ClientModel clientModel, UserModel userModel, CibaAuthConfigModel cibaAuthConfigModel) {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return "api-authorizer-title";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties(KeycloakSession session) {
        return new ArrayList<>();
    }

    @Override
    public CibaAuthProvider create(KeycloakSession session) {
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
    public boolean isTemplateSupported() {
        return false;
    }

    @Override
    public String getId() {
        return "api-authorizer";
    }
}
